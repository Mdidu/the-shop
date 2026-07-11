# The Shop — Order Management System

A production-grade order management system built with Java 21 and Spring Boot 4, designed to demonstrate clean backend architecture, rich domain modeling, and modern Java practices.

The scope is intentionally Amazon-like: simple on the surface, complex underneath. The goal is not to ship features fast — it's to ship them right.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4 |
| Persistence | PostgreSQL + Spring Data JPA |
| Cache | Redis |
| Messaging | Spring Events (→ Kafka in Phase 3) |
| Auth | JWT + Refresh Token |
| Testing | JUnit 5, Testcontainers, MockMvc |
| Infra | Docker, Docker Compose |

---

## Architecture

### Modular Monolith

The application is structured as a **modular monolith**: a single deployable unit with strict module boundaries. Each module owns its domain, its data, and its API surface. No module accesses another module's database tables directly — cross-module communication goes through explicit interfaces or domain events.

This architecture is chosen deliberately: it enables the discipline of microservices (bounded contexts, explicit contracts) without the operational overhead. Migrating a module to a standalone service later is a valid exit path.

```
com.exemple.the_shop
├── catalog/
├── cart/
├── order/
├── payment/
├── user/
└── notification/
```

### Hexagonal (Ports & Adapters)

Each module follows the same internal structure:

```
{module}/
├── domain/
│   ├── model/         # Entities & value objects — pure Java, no framework
│   ├── port/out/      # Outbound ports: interfaces the domain depends on (e.g. repositories)
│   └── *Exception     # Domain exceptions
├── application/       # Use cases / services + application DTOs
└── infrastructure/    # Adapters — the only layer that touches frameworks
    ├── persistence/   # JPA entities, Spring Data repos, mappers, port implementations
    ├── security/      # JWT util, authentication filter, Spring Security config
    └── web/           # Controllers, request DTOs, per-module exception handlers
```

**Dependency rule**: the domain knows nothing about the outside world. It defines the ports (interfaces) it needs; the infrastructure provides the adapters that implement them. Controllers (`infrastructure/web`) call the `application` layer — never the domain models directly.

Cross-cutting concerns that aren't tied to a single module live in a top-level `shared/` package: shared domain (value objects like `Money` and `Slug` in `shared/domain`) and shared web (the `ApiError` response body and the global exception handler in `shared/web`).

### Why Hexagonal?

The project initially aimed for a simpler layered approach, but converged on ports & adapters: isolating the domain behind explicit ports keeps Spring (Security, Data JPA, MVC) entirely in the infrastructure layer, and makes each module a self-contained template that's straightforward to replicate. The `user` module is that reference template.

---

## Modules

### `user`
Authentication and identity. Issues JWT access tokens and refresh tokens. Roles: `CUSTOMER`, `ADMIN`, `SELLER`.

### `catalog`
Products, categories, and stock levels. Stock is managed with **optimistic locking** to handle concurrent purchases without pessimistic locks degrading throughput.

### `cart`
Persisted shopping cart (one per user, not session-based). Created lazily on first `addItem`. The **product is the source of truth for price**: totals are recalculated from the live catalog price on every read. `cart_items.unit_price` is only a decorative "last known price" — the price is frozen for good at checkout (`order_items`), not in the cart. The cart reserves no stock; concurrency on the last unit is arbitrated at checkout.

### `order`
The richest module. An order moves through a strict state machine:

```
PENDING → CONFIRMED → PREPARING → SHIPPED → DELIVERED
                ↘ CANCELLED
                             DELIVERED → RETURN_REQUESTED → RETURNED
```

Transitions are explicit use cases. Invalid transitions throw domain exceptions. Each transition publishes a domain event consumed by other modules (payment, notification, catalog).

### `payment`
Simulated payment processor. Supports `PENDING → CAPTURED → REFUNDED`. All payment requests are **idempotent** via a client-provided idempotency key — replaying the same request returns the same result without side effects.

### `notification`
Reacts to domain events from other modules. Sends email notifications (simulated via logs in dev, real SMTP in prod) on order confirmation, shipment, and delivery.

---

## Key Technical Decisions

### Domain Events (in-memory → Kafka)

Phase 1 uses Spring's `ApplicationEventPublisher` — synchronous, in-process. The domain publishes events, the infrastructure dispatches them.

The constraint: **event publishers and handlers must never reference Spring's event API in the domain layer**. The domain defines its own event types (plain Java records). The infrastructure wires them to Spring.

This means migrating to Kafka in Phase 3 only touches the infrastructure layer. No domain code changes.

### CQRS (lightweight)

Write operations go through rich domain aggregates. Read operations (listings, order history, admin views) bypass the domain and hit optimized query projections directly. This avoids loading full aggregates just to render a table, and sidesteps the N+1 that fetching related data (category name, stock availability…) through aggregates would cause.

**Port placement is deliberately asymmetric between the two sides:**

| | Write side | Read side |
|---|---|---|
| Port location | `domain/port/out` | `application` |
| Returns | domain aggregate (`Product`) | flat read model DTO (`ProductListItem`) |
| Spring Data repo | `JpaRepository` (full CRUD) | `Repository` (only the query method) |

A write port belongs in the domain because it returns aggregates — domain depending on domain. A **read port does not**: it returns a projection shaped by the UI, which is an *application* concern, not a domain concept. Placing it in `domain/port/out` would force the domain to depend on an application DTO — an inverted dependency. So read ports live in `application`, next to the read model they return. This asymmetry is intentional, not an inconsistency: it follows the dependency rule rather than visual symmetry.

Concrete example — the catalog product listing (`GET /products`, `GET /admin/products`):
- `ProductListItem` (read model) + `ProductQueryRepository` (read port) live in `application`. The read model carries only what a list row renders (slug, name, price, category name/slug, an `inStock` boolean derived in SQL, status) — never the internal UUID, never the raw stock quantity.
- `ProductListingJpaRepository` (a Spring Data `Repository`, **not** `JpaRepository`, so the read side inherits no write methods) plus its adapter live in `infrastructure/persistence`.
- Spring's `Pageable` / `Page` / `Sort` are confined to that adapter. The layers above speak a home-grown `PageResponse<T>` and `ProductListQuery`, so no Spring Data pagination type leaks upward. Sortable fields are whitelisted (an enum), which is also the injection guard on the `ORDER BY`.

### Optimistic Locking on Stock

Stock decrements happen at order confirmation. Rather than locking rows, each stock record has a `@Version` field. On concurrent confirmations of the same product, one write succeeds and the others fail the version check — Hibernate raises `ObjectOptimisticLockingFailureException`, which the API surfaces as a **`409 Conflict`** (no deadlocks, predictable behavior under load). For now the client is responsible for retrying; an automatic server-side retry mechanism will be added later.

> **Implementation note** — because the persistence adapters reconstruct a *detached* entity in the mapper (`new XxxJpaEntity()`) on every `save`, the `@Version` value must be carried explicitly from the domain into the entity (`entity.setVersion(domain.getVersion())`). Otherwise the detached entity always claims version `0`, the merge silently breaks optimistic locking, and the second consecutive update on a row throws `StaleObjectStateException`. The same applies to `created_at` (carried via the mapper, with `@PrePersist` only filling it when null). If a module ever switches to loading-and-mutating the managed entity instead, this manual carrying becomes unnecessary.

### Idempotency

The payment endpoint and the "place order" endpoint accept an `Idempotency-Key` header. The server stores the result of the first execution and returns it on subsequent requests with the same key, making retries safe for clients.

### Modern Java

| Feature | Where used |
|---|---|
| `record` | DTOs, Value Objects (Money, Address, ProductRef) |
| `sealed class` | Order states, Payment states |
| `switch` expression + pattern matching | State transition logic |
| Virtual Threads (Project Loom) | Enabled globally — blocking I/O without thread pool tuning |
| `Optional` | Repository return types only, never as fields |
| Text blocks | SQL queries in repositories |

---

## Roadmap

### Phase 1 — Core
- [x] Project setup (Docker, DB migrations with Flyway, base config)
- [x] `user` module: registration, login, JWT *(code-complete, audité & testé manuellement via curl le 2026-06-24 — tests automatisés à venir)*
  - _Bugs runtime trouvés & corrigés au test manuel : `/auth/refresh` en 500 (refresh token opaque parsé comme un JWT + `created_at` perdu au merge → logique déplacée dans `AuthService.refresh`, mapper corrigé) ; `405` renvoyé en `500` (handler `HttpRequestMethodNotSupportedException` ajouté)._
  - [x] Entités JPA (`UserJpaEntity`, `RefreshTokenJpaEntity`, `Role`)
  - [x] Repositories JPA (`UserJpaRepository`, `RefreshTokenJpaRepository`)
  - [x] Ports domaine + modèle (`UserRepository`, `RefreshTokenRepository`, `RefreshToken`, `User`)
  - [x] Utilitaire JWT (`JwtUtil`, génération + validation HS256) + dépendance `jjwt`
  - [x] `RefreshTokenService` (issue / rotate / revoke / purge / validate)
  - [x] Adapters JPA des ports (`RefreshTokenRepositoryImpl`, `UserRepositoryImpl`)
  - [x] `AuthService` (signup / signin / signout)
    - **Décidé** : `signup` auto-connecte → retourne `AuthResponse` (access + refresh token).
    - **Décidé** : DTOs de requête (`SigninRequest`, `SignupRequest`) dans `infrastructure/web/` ; `AuthResponse` dans `application/`.
  - [x] Configuration Spring Security (`SecurityFilterChain`, stateless, `/auth/**` public)
  - [x] Filtre JWT (`JwtAuthenticationFilter`)
  - [x] Gestion d'erreurs (`ApiError` + `@RestControllerAdvice` global + par module) & validation `@Valid` des requêtes
  - [x] Controllers (`/auth/signup`, `/auth/signin`, `/auth/refresh`, `/auth/logout`)
  - [ ] Tests (domain + intégration Testcontainers + MockMvc)
  - _Note : `UserDetailsService` retiré volontairement — `signin` vérifie via `passwordEncoder.matches`. À réintroduire seulement en cas de statuts de compte / multi-providers._
- [ ] `catalog` module: products, categories, stock
  - **Décidé** : périmètre = CRUD complet produits + catégories + gestion du stock. Le décrément de stock à la commande reste dans le module `order`.
  - **Décidé** : écriture réservée au rôle `ADMIN` ; lecture publique.
  - **Décidé** : value object `Money` placé dans un package transverse `shared/domain` (domaine partagé). Le transverse web (`ApiError`, `GlobalExceptionHandler`) vit dans `shared/web`. _(Le package `common/` initialement envisagé n'a pas été créé : tout le transverse est regroupé sous `shared/`.)_
  - Ordre d'attaque (de l'intérieur vers l'extérieur, `Category` avant `Product`) :
    1. [x] `shared/` → VO `Money` (record immuable, `BigDecimal` mono-devise, factory `of(...)`, montant ≥ 0, échelle 2)
    2. [x] Domaine `Category` : modèle + VO `Slug` (shared) + enum `ProductStatus` + exceptions (`CategoryNotFoundException`, `SlugAlreadyUsedException` dans shared) + port `CategoryRepository` (`save`/`findById`/`findBySlug`)
    3. [x] Domaine `Product` + `ProductStock` : deux agrégats distincts, chacun son `version`
       - [x] Modèle `Product` (factory `create` → `DRAFT`/version 0 ; transitions immuables `activate`/`deactivate` ; édition immuable `updateName`/`updatePrice`/`updateDescription`/`updateCategory` ; nom non vide validé dans le constructeur ; **slug figé au renommage** — pas de recalcul, stabilité SEO/URL)
       - [x] Modèle `ProductStock` (invariant `quantity >= 0` dans le constructeur ; `productId` fourni par l'appelant ; mouvements `increase`/`decrease` immuables, delta strictement positif, `decrease` lève `InsufficientStockException`)
       - [x] Exceptions `ProductNotFoundException`, `IllegalProductStatusTransitionException`, `InsufficientStockException`
       - [x] Ports `ProductRepository` (`save`/`findById`/`findBySlug`/`findAllByCategoryId`) + `ProductStockRepository` (`save`/`findById`/`findByProductId`)
    4. [x] Persistence : `*JpaEntity` (`@Version`, `@PrePersist/@PreUpdate`), `*JpaRepository`, `*Mapper`, `*RepositoryImpl` — pour `Product` et `ProductStock`. Value objects (`Money`/`Slug`) déballés en primitives par le mapper, jamais exposés à JPA. Migration Flyway `V3__create_catalog.sql` (FK, `UNIQUE`, `CHECK`, index). Compile OK.
    5. [x] Application : `@Service` `ProductService` + `ProductStockService` + `CategoryService` avec méthodes `@Transactional` (pas de classes `UseCase` dédiées — on suit le gabarit `AuthService`).
       - **Décidé** : `createProduct` initialise le `ProductStock` (à 0) dans la **même transaction** que le produit.
       - **Décidé** : la vérif « catégorie existe » vit dans le service (→ `CategoryNotFoundException` métier) ; la FK base est le garde-fou de dernier rideau.
       - **Décidé** : cycle de vie `DRAFT → ACTIVE ⇄ INACTIVE`, réactivation autorisée, pas de hard delete.
       - **Décidé** : hiérarchie `Category` = arbre via `parent_id` ; `moveCategory` interdit le self-parent (domaine) et les cycles indirects (`assertNoCycle` remonte les ancêtres dans le service).
    6. [x] Web : `ProductController` (UN seul controller pour produit + stock, stock adressé par slug) + `CategoryController`, request DTOs `@Valid`, `CatalogExceptionHandler` (par module) + `IllegalArgumentException → 400` ajouté au `GlobalExceptionHandler` (slug d'URL mal formé).
    - [x] **Testé manuellement via curl le 2026-06-24** : les 16 endpoints (auth + catégories + produits + stock) validés de bout en bout sur base vierge. _Bug systémique de persistence trouvé & corrigé : toutes les opérations d'UPDATE plantaient (mappers reconstruisant une entité détachée sans recopier `created_at` ni `@Version` → `NOT NULL violation` et `StaleObjectStateException` au merge). Mappers Category/Product/ProductStock corrigés. Mapping d'exceptions complété : `IllegalProductStatusTransition`→409, `ObjectOptimisticLockingFailure`→409._
    7. [x] **Sécurité** : règles d'accès ajoutées au `SecurityConfig` (matchers par méthode HTTP, dans l'ordre) — `GET /products/**` & `GET /categories/**` publics, toute autre écriture catalog `hasRole("ADMIN")`, `anyRequest().authenticated()` en filet. Écriture = `ADMIN` seul (le rôle `SELLER` sera traité plus tard). Refus rendus en `ApiError` via `RestAuthenticationEntryPoint` (401) + `RestAccessDeniedHandler` (403) câblés sur `.exceptionHandling(...)` — nécessaires car le refus survient dans la chaîne de filtres, hors de portée du `@RestControllerAdvice`. _⚠️ Le slash initial des patterns (`/products/**`) est obligatoire : sans lui, le matcher ne couvre pas le path et l'écriture retombe sur `authenticated()` → faille fail-open silencieuse._
    - [x] **Testé manuellement via curl le 2026-06-26** (script `scripts/catalog-security-smoke.sh`, base vierge) : **21/21** — 16 endpoints fonctionnels (écritures `ADMIN`, lectures publiques) + matrice de sécurité (GET public→200, écriture sans token→401, écriture `CUSTOMER`→403, écriture `ADMIN`→201), corps `ApiError` vérifiés. _Promotion `ADMIN` par `UPDATE` en base (aucun endpoint ne crée d'`ADMIN`) + re-signin obligatoire (rôle figé dans le JWT). Piège Boot 4 / Jackson 3 levé : injecter `tools.jackson.databind.ObjectMapper` (le `com.fasterxml…` de jjwt n'a pas de bean)._
    8. [x] **Pagination** des listings produit (cf. CQRS / projections de lecture — read model, port de lecture en `application`, adapter confinant `Pageable`/`Page`, wrapper maison `PageResponse<T>`).
       - [x] `GET /products` (public, ACTIVE only) + `GET /admin/products` (tous statuts, `/admin/**` → `hasRole("ADMIN")`) — paginés, filtre par catégorie, tri whitelisté (`name`/`price`), `inStock` dérivé en SQL. **Testé via curl le 2026-06-27** (script `scripts/catalog-pagination-smoke.sh`, base vierge) : **30/30** — visibilité public/admin, filtre, tri (+ rejet 400 hors whitelist = garde anti-injection sur l'`ORDER BY`), clamp du `size` (0→20, 400→100), pagination + tie-breaker stable, sécurité 401/403/200. _3 risques JPQL levés au runtime : entity joins `ON`, `CASE` booléen, tri sur l'alias racine malgré les jointures._
       - **Décidé** : `GET /categories` **NON paginé**. Paginer un arbre de catégories (peu nombreuses, hiérarchiques) est artificiel — le besoin réel est de renvoyer l'arbre, pas une page plate. À reconsidérer seulement si le volume l'exige un jour.
- [ ] `cart` module: add/remove items, live pricing, checkout → order
  - **Décidé** : **un panier persistant par user** (pas de panier invité/session). Création = **lazy get-or-create au 1er `addItem`** (`findByUserId(u).orElseGet(create)`), **zéro couplage** avec l'inscription — un user sans panier est rattrapable (`GET /cart` → panier vide `200`), donc le cart n'est pas un invariant à créer au signup (rollback du compte parce que le cart plante = absurde).
  - **Décidé** : **le produit fait autorité sur le prix**. `GET /cart` relit le prix vivant du catalog (read model, JOIN) et recalcule le total à la lecture. `cart_items.unit_price` est **décoratif** (« dernier prix connu », utile pour un futur badge « prix changé »), PAS la source de vérité. Le prix ne se fige **définitivement qu'au checkout** (`order_items.unit_price`, module `order`) : le panier reflète le catalogue, la commande est le contrat.
  - **Décidé** : le panier **ne réserve aucun stock** (`carts`/`cart_items` n'ont ni lien `product_stock` ni `@Version`). La concurrence sur le dernier exemplaire est arbitrée au **checkout** (décrément + `@Version` côté `order`).
  - **Décidé** : **vrai agrégat `@OneToMany`** — `Cart` est la racine qui tient sa `List<CartItem>` ; `CartItem` n'a pas de repo propre. Contraste avec catalog (`Product`/`ProductStock` = deux agrégats distincts, d'où le style flat là-bas). C'est la 1re association JPA du projet.
  - Ordre d'attaque (bottom-up) :
    1. [x] Domaine `Cart` + `CartItem` : classes immuables, invariant `quantity > 0` dans le constructeur, factory `create`, transitions immuables **conservant l'`id`** de la ligne (`increaseQuantity`/`withQuantity` — clé pour qu'Hibernate reconnaisse la ligne au save). `addItem` merge par `productId` (traduit `UNIQUE(cart_id, product_id)` en règle métier). `removeItem`/`updateItemQuantity` → `CartItemNotFoundException` si absent. `isEmpty`.
    2. [x] Port `CartRepository` (`findByUserId`/`save`). Pas de `CartItemRepository` (CartItem n'est pas un aggregate root → un repo par racine).
    3. [x] Persistence : `CartJpaEntity` (`@OneToMany(cascade=ALL, orphanRemoval=true)` **unidirectionnel** + `@JoinColumn(name="cart_id", nullable=false)` — le `nullable=false` tue l'UPDATE parasite des unidirectionnelles), `CartItemJpaEntity` (pas de back-ref `cart`, fidèle au domaine), `CartJpaRepository`, `CartMapper`, `CartRepositoryImpl`. Timestamps générés en Java (`@PrePersist`/`@PreUpdate`). Compile OK.
       - **Stratégie `save` = load-and-mutate** (≠ « build fresh + merge » de catalog, inadapté à un agrégat `@OneToMany`/`orphanRemoval`) : `findById` absent → cart neuf → INSERT ; présent → **synchro par id** sur l'entity managée, en **mutant la collection en place** (`add`/`removeIf` sur le `PersistentBag` suivi) → id présent = UPDATE quantité ciblé (`created_at` préservé), id nouveau = INSERT, id disparu = DELETE orphelin. ⚠️ Impose que `CartService` soit `@Transactional` (dirty checking, flush au commit — l'entity managée serait détachée sinon).
    4. [ ] Application : `CartService` (`@Transactional`) + `CartResponse`/`CartItemResponse`. **À trancher** : comment `cart` **lit** `catalog` (prix + existence + statut `ACTIVE` du produit) → port dédié côté cart vs réutilisation d'un service/query catalog.
    5. [ ] Web : `CartController` + `CartExceptionHandler` (par module) + règles de sécurité.
- [ ] `order` module: state machine, place order use case
- [ ] `payment` module: simulated capture, idempotency
- [ ] `notification` module: event-driven email simulation
- [ ] Integration tests with Testcontainers

### Phase 2 — Enrichment
- [ ] Product reviews and ratings
- [ ] Order history with filtering and pagination (CQRS projections)
- [ ] Admin dashboard endpoints (sales metrics, stock alerts)
- [ ] PDF invoice generation on delivery
- [ ] Rate limiting on public endpoints
- [ ] Redis caching for catalog reads

### Phase 3 — Kafka Migration
- [ ] Replace `ApplicationEventPublisher` with Kafka producers/consumers
- [ ] Add consumer group and offset management
- [ ] Demonstrate zero domain changes — only infrastructure layer updated
- [ ] Dead letter queue for failed event processing

---

## Running Locally

### Prerequisites
- Docker & Docker Compose
- Java 21
- Maven (or use the `./mvnw` wrapper)

### Setup

```bash
# Copy and fill in the environment variables
cp .env.example .env
```

> **JWT_SECRET** — en dev, une valeur par défaut (non sécurisée) permet de lancer l'app sans rien configurer. En **prod, `JWT_SECRET` est obligatoire** (≥ 32 caractères) et le démarrage échoue s'il est absent. Génère-le avec `openssl rand -base64 48`.

### Start infrastructure (PostgreSQL, Redis, pgAdmin)

```bash
docker compose -f docker-compose.dev.yml up postgres redis pgadmin -d
```

### Run the application from the IDE or CLI

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Flyway runs automatically on startup and applies any pending migrations.

### Smoke test du module catalog (auth + sécurité)

Avec l'infra et l'application démarrées, le script rejoue de bout en bout les 16 endpoints du catalog et la matrice de sécurité (lecture publique / écriture `ADMIN`) :

```bash
bash scripts/catalog-security-smoke.sh
```

⚠️ Il **vide** les tables `users`/catalog au démarrage (repart d'une base propre). Nécessite `jq` et `docker`. Sortie : compteur `PASS/FAIL` + exit code 0 si tout est vert.

### Start everything including the backend in Docker

```bash
docker compose -f docker-compose.dev.yml up --build -d
```

---

## Service URLs

| Service | URL | Credentials |
|---|---|---|
| API | `http://localhost:8080` | — |
| pgAdmin | `http://localhost:5050` | see `.env` → `PGADMIN_EMAIL` / `PGADMIN_PASSWORD` |
| PostgreSQL | `localhost:5432` | see `.env` → `POSTGRES_USER` / `POSTGRES_PASSWORD` |
| Redis | `localhost:6379` | — |
| Debug (JDWP) | `localhost:5005` | connect from IDE |

### Connecting pgAdmin to the database

In pgAdmin: right-click **Servers → Register → Server**

| Field | Value |
|---|---|
| Host | `postgres` (Docker service name, not `localhost`) |
| Port | `5432` |
| Database | `the_shop` |
| Username | `the_shop_user` |
| Password | `changeme` |

Tables are under: **the_shop → Schemas → public → Tables**

---

## Database Schema

All tables use `UUID` primary keys. The UUID is generated by the **application** in the domain layer (factories call `UUID.randomUUID()`), so identity is a domain concern, not a database one. The SQL `DEFAULT gen_random_uuid()` is kept as a safety net but is never exercised in practice. Monetary values use `NUMERIC(10, 2)` — never `FLOAT`. All timestamps are `TIMESTAMP WITH TIME ZONE`.

### `user` module

**`users`**
| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `email` | VARCHAR UNIQUE | login identifier |
| `password_hash` | VARCHAR | BCrypt hash, never plain text |
| `role` | VARCHAR | `CUSTOMER`, `ADMIN`, `SELLER` |
| `created_at` / `updated_at` | TIMESTAMPTZ | |

**`refresh_tokens`**
| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `user_id` | UUID FK → users | cascade delete |
| `token` | VARCHAR UNIQUE | opaque token |
| `expires_at` | TIMESTAMPTZ | |
| `revoked` | BOOLEAN | explicit revocation before expiry |
| `created_at` | TIMESTAMPTZ | issued-at timestamp |

---

### `catalog` module

**`categories`**
| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `slug` | VARCHAR UNIQUE | URL-safe identifier |
| `parent_id` | UUID FK → categories | nullable, self-reference for subcategories |

**`products`**
| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `slug` | VARCHAR UNIQUE | URL-safe identifier |
| `price` | NUMERIC(10,2) | price at time of listing, not the cart price |
| `status` | VARCHAR | `DRAFT`, `ACTIVE`, `INACTIVE` — no physical delete |
| `version` | INTEGER | JPA `@Version` for optimistic locking on product edits |
| `category_id` | UUID FK → categories | |

**`product_stock`**
| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `product_id` | UUID FK UNIQUE → products | one-to-one |
| `quantity` | INTEGER ≥ 0 | enforced by CHECK constraint |
| `version` | INTEGER | separate `@Version` — stock lock doesn't block product edits |

Stock lives in its own table so that decrementing quantity during order confirmation only locks that row, not the entire product record.

---

### `cart` module

**`carts`**
| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `user_id` | UUID FK UNIQUE → users | one cart per user |

**`cart_items`**
| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `cart_id` | UUID FK → carts | cascade delete |
| `product_id` | UUID FK → products | |
| `quantity` | INTEGER > 0 | |
| `unit_price` | NUMERIC(10,2) | decorative "last known price" at time of addition — **not authoritative**; `GET /cart` re-reads the live product price. The price is frozen for good only at checkout (`order_items.unit_price`). |

`(cart_id, product_id)` has a UNIQUE constraint — adding the same product twice increases quantity, it does not create a second row.

---

### `order` module

**`orders`**
| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `user_id` | UUID FK → users | |
| `status` | VARCHAR | state machine: see below |
| `total_amount` | NUMERIC(10,2) | computed at placement, immutable |
| `shipping_street/city/zip/country` | VARCHAR | address embedded — a delivered order must never lose its address |

Order status transitions (enforced by CHECK constraint and domain logic):
```
PENDING → CONFIRMED → PREPARING → SHIPPED → DELIVERED
        ↘ CANCELLED                        ↘ RETURN_REQUESTED → RETURNED
```

**`order_items`**
| Column | Type | Notes |
|---|---|---|
| `product_name` | VARCHAR | **name snapshot** — product can be renamed after order |
| `unit_price` | NUMERIC(10,2) | **price snapshot** — product price can change after order |

**`order_status_history`**

Every status transition is recorded with `previous_status`, `new_status`, and `changed_at`. Full audit trail, no data is ever lost.

---

### `payment` module

**`payments`**
| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `order_id` | UUID FK → orders | |
| `idempotency_key` | VARCHAR UNIQUE | client-provided, prevents double-charge on retry |
| `status` | VARCHAR | `PENDING → CAPTURED → REFUNDED` |
| `amount` | NUMERIC(10,2) | |

---

## Testing Strategy

- **Domain unit tests** — pure Java, no Spring context. Cover state machine transitions, business rules, domain event publishing.
- **Application integration tests** — Spring context + Testcontainers (real PostgreSQL, real Redis). Cover full use case flows.
- **API tests** — MockMvc for controller layer. Validate request/response contracts and error handling.

No mocking of the database in integration tests. If the real database rejects it, the test should fail.
