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

### Clean Architecture Layers

Each module follows the same internal structure:

```
{module}/
├── domain/          # Entities, Value Objects, Domain Events, interfaces
├── application/     # Use cases, commands, queries, event handlers
├── infrastructure/  # JPA repositories, external clients, event publishers
└── api/             # Controllers, request/response DTOs
```

**Dependency rule**: the domain knows nothing about the outside world. Infrastructure implements interfaces defined in the domain. The `api` layer calls the `application` layer — never the domain directly.

### Why Not Hexagonal?

Hexagonal architecture (ports & adapters) is a valid pattern but adds ceremony that obscures intent in a single-team project. The layered approach above enforces the same core constraint — the domain is isolated — with less indirection.

---

## Modules

### `user`
Authentication and identity. Issues JWT access tokens and refresh tokens. Roles: `CUSTOMER`, `ADMIN`, `SELLER`.

### `catalog`
Products, categories, and stock levels. Stock is managed with **optimistic locking** to handle concurrent purchases without pessimistic locks degrading throughput.

### `cart`
Persisted shopping cart (not session-based). Applies promotion rules at item level. Recalculates totals on every modification. Cart items hold a snapshot of the price at time of addition — not a live reference.

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

Write operations go through rich domain aggregates. Read operations (listings, order history, admin views) bypass the domain and hit optimized query projections directly. This avoids loading full aggregates just to render a table.

### Optimistic Locking on Stock

Stock decrements happen at order confirmation. Rather than locking rows, each stock record has a `@Version` field. Concurrent confirmations on the same product will result in one succeeding and others retrying — no deadlocks, predictable behavior under load.

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
- [ ] `user` module: registration, login, JWT
- [ ] `catalog` module: products, categories, stock
- [ ] `cart` module: add/remove items, price snapshot, promo rules
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

### Start infrastructure (PostgreSQL, Redis, pgAdmin)

```bash
docker compose -f docker-compose.dev.yml up postgres redis pgadmin -d
```

### Run the application from the IDE or CLI

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Flyway runs automatically on startup and applies any pending migrations.

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

All tables use `UUID` primary keys generated by PostgreSQL (`gen_random_uuid()` via the `pgcrypto` extension). Monetary values use `NUMERIC(10, 2)` — never `FLOAT`. All timestamps are `TIMESTAMP WITH TIME ZONE`.

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
| `unit_price` | NUMERIC(10,2) | **price snapshot at time of addition** — not the live product price |

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
