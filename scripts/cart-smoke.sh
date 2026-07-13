#!/bin/bash
#
# Smoke test runtime du module cart (panier client).
#
# Couvre :
#   - le parcours nominal : panier vide -> add -> merge -> get -> update -> remove -> clear
#   - le recalcul du total (quantite * prix)
#   - les cas d'erreur metier : produit inexistant (404), produit inactif (409),
#     ligne absente (404), quantite invalide (400), sans token (401)
#
# Prerequis :
#   - l'appli tourne sur $BASE (profil dev)        : mvnw spring-boot:run
#   - le conteneur Postgres est up                 : docker compose -f docker-compose.dev.yml up -d postgres redis
#   - jq et docker disponibles dans le PATH
#
# Le panier n'a besoin que d'un user AUTHENTIFIE (regle .anyRequest().authenticated()),
# donc les operations tournent avec un token CUSTOMER. L'ADMIN ne sert qu'a creer
# le produit + son stock a mettre au panier.
#
# ⚠️ Le script VIDE les tables users/catalog/cart au demarrage.
#
# Usage : bash scripts/cart-smoke.sh
#
set -u

BASE="${BASE:-http://localhost:8080}"
PG_CONTAINER="${PG_CONTAINER:-the-shop-dev-postgres-1}"
PG_USER="${PG_USER:-the_shop_user}"
PG_DB="${PG_DB:-the_shop}"
PASS=0; FAIL=0
ZERO_UUID='00000000-0000-0000-0000-000000000000'

call() { # METHOD PATH TOKEN BODY -> code HTTP, corps dans /tmp/resp.json
  local method=$1 path=$2 token=$3 body=$4
  local args=(-s -o /tmp/resp.json -w "%{http_code}" -X "$method" "$BASE$path")
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$body" ]  && args+=(-H "Content-Type: application/json" -d "$body")
  curl "${args[@]}"
}
check() { # label expected actual
  if [ "$3" = "$2" ]; then echo "  PASS  $1 -> $3"; PASS=$((PASS+1));
  else echo "  FAIL  $1 -> got $3, expected $2"; FAIL=$((FAIL+1)); cat /tmp/resp.json; echo; fi
}
check_json() { # label jq-filter expected
  local actual; actual=$(jq -r "$2" /tmp/resp.json)
  if [ "$actual" = "$3" ]; then echo "  PASS  $1 -> $actual"; PASS=$((PASS+1));
  else echo "  FAIL  $1 -> got $actual, expected $3"; FAIL=$((FAIL+1)); cat /tmp/resp.json; echo; fi
}
psql_exec() { docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d "$PG_DB" -tAc "$1"; }

PW='supersecret123456'   # >= 16 caracteres (politique signup)

echo "############ 0. BASE PROPRE ############"
psql_exec "TRUNCATE users, refresh_tokens, products, product_stock, categories, carts, cart_items RESTART IDENTITY CASCADE;" >/dev/null
echo "  tables videes"

echo
echo "############ A. ROLES + CATALOGUE ############"
SIGNUP_ADMIN="{\"email\":\"admin@test.com\",\"password\":\"$PW\",\"firstName\":\"Ada\",\"lastName\":\"Min\"}"
SIGNIN_ADMIN="{\"email\":\"admin@test.com\",\"password\":\"$PW\"}"
SIGNUP_CUST="{\"email\":\"cust@test.com\",\"password\":\"$PW\",\"firstName\":\"Cus\",\"lastName\":\"Tomer\"}"

check "signup admin" 201 "$(call POST /auth/signup '' "$SIGNUP_ADMIN")"
psql_exec "UPDATE users SET role='ADMIN' WHERE email='admin@test.com';" >/dev/null
call POST /auth/signin '' "$SIGNIN_ADMIN" >/dev/null
ADMIN=$(jq -r .accessToken /tmp/resp.json)

check "signup customer" 201 "$(call POST /auth/signup '' "$SIGNUP_CUST")"
CUST=$(jq -r .accessToken /tmp/resp.json)

CAT_CREATE='{"name":"Electronics","parentId":null}'
check "POST /categories (admin)" 201 "$(call POST /categories "$ADMIN" "$CAT_CREATE")"
CAT_ID=$(jq -r .id /tmp/resp.json)

# Produit ACTIF (cree + active) a mettre au panier
ACTIVE_CREATE="{\"name\":\"Laptop X\",\"description\":\"un laptop\",\"categoryId\":\"$CAT_ID\",\"price\":100.00}"
check "POST /products actif (admin)" 201 "$(call POST /products "$ADMIN" "$ACTIVE_CREATE")"
ACTIVE_ID=$(jq -r .id /tmp/resp.json); ACTIVE_SLUG=$(jq -r .slug /tmp/resp.json)
check "activate produit (admin)" 200 "$(call POST "/products/$ACTIVE_SLUG/activate" "$ADMIN" '')"

# Produit INACTIF (cree, jamais active -> reste DRAFT -> isActive=false)
DRAFT_CREATE="{\"name\":\"Souris Y\",\"description\":\"une souris\",\"categoryId\":\"$CAT_ID\",\"price\":20.00}"
check "POST /products draft (admin)" 201 "$(call POST /products "$ADMIN" "$DRAFT_CREATE")"
DRAFT_ID=$(jq -r .id /tmp/resp.json)

echo
echo "############ B. PARCOURS NOMINAL (customer) ############"
check      "GET /cart (vide)"                 200 "$(call GET /cart "$CUST" '')"
check_json "  -> panier vide"                 '.items | length' 0
check_json "  -> total 0"                     '.total' 0

ADD2="{\"productId\":\"$ACTIVE_ID\",\"quantity\":2}"
ADD1="{\"productId\":\"$ACTIVE_ID\",\"quantity\":1}"
check      "POST /cart/items (qty 2)"         200 "$(call POST /cart/items "$CUST" "$ADD2")"
check_json "  -> 1 ligne"                     '.items | length' 1
check_json "  -> qty 2"                       '.items[0].quantity' 2

check      "POST /cart/items meme produit (qty 1 -> merge)" 200 "$(call POST /cart/items "$CUST" "$ADD1")"
check_json "  -> toujours 1 ligne"            '.items | length' 1
check_json "  -> qty mergee = 3"              '.items[0].quantity' 3
check_json "  -> total = 3 * 100"             '.total' 300.00

UPD5="{\"quantity\":5}"
check      "PUT /cart/items/{id} (qty 5)"     200 "$(call PUT "/cart/items/$ACTIVE_ID" "$CUST" "$UPD5")"
check_json "  -> qty 5"                        '.items[0].quantity' 5
check_json "  -> total = 5 * 100"              '.total' 500.00

check      "GET /cart"                         200 "$(call GET /cart "$CUST" '')"
check_json "  -> lineTotal = 500"              '.items[0].lineTotal' 500.00

check      "DELETE /cart/items/{id}"           200 "$(call DELETE "/cart/items/$ACTIVE_ID" "$CUST" '')"
check_json "  -> panier vide"                  '.items | length' 0

# re-add puis clear
call POST /cart/items "$CUST" "$ADD2" >/dev/null
check      "DELETE /cart (clear)"              200 "$(call DELETE /cart "$CUST" '')"
check_json "  -> panier vide"                  '.items | length' 0

echo
echo "############ C. CAS D'ERREUR ############"
ADD_MISSING="{\"productId\":\"$ZERO_UUID\",\"quantity\":1}"
ADD_DRAFT="{\"productId\":\"$DRAFT_ID\",\"quantity\":1}"
ADD_ZERO="{\"productId\":\"$ACTIVE_ID\",\"quantity\":0}"

check "POST produit inexistant => 404" 404 "$(call POST /cart/items "$CUST" "$ADD_MISSING")"
check "POST produit inactif => 409"    409 "$(call POST /cart/items "$CUST" "$ADD_DRAFT")"
check "POST quantite 0 => 400"         400 "$(call POST /cart/items "$CUST" "$ADD_ZERO")"
check "PUT ligne absente => 404"       404 "$(call PUT "/cart/items/$ACTIVE_ID" "$CUST" "$UPD5")"
check "DELETE ligne absente => 404"    404 "$(call DELETE "/cart/items/$ACTIVE_ID" "$CUST" '')"
check "GET /cart sans token => 401"    401 "$(call GET /cart '' '')"

echo
echo "############ RESULTAT : $PASS PASS / $FAIL FAIL ############"
[ "$FAIL" -eq 0 ]
