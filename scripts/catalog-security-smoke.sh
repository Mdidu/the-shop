#!/bin/bash
#
# Smoke test runtime du module catalog + des règles de sécurité (ADMIN/public).
#
# Couvre :
#   - les 16 endpoints fonctionnels (écritures ADMIN, lectures publiques)
#   - la matrice de sécurité : GET public→200, écriture sans token→401,
#     écriture CUSTOMER→403, écriture ADMIN→201, le tout en corps `ApiError`.
#
# Prérequis :
#   - l'appli tourne sur $BASE (profil dev)        : mvnw spring-boot:run
#   - le conteneur Postgres est up                 : docker compose -f docker-compose.dev.yml up -d postgres
#   - jq et docker disponibles dans le PATH
#
# La promotion ADMIN se fait en base (aucun endpoint ne crée d'ADMIN : User.create
# code en dur Role.CUSTOMER), puis on RE-signin pour obtenir un token portant le
# rôle ADMIN (le rôle est figé dans le JWT à l'émission).
#
# ⚠️ Le script VIDE les tables users/catalog au démarrage (repart d'une base propre).
#
# ⚠️ Convention de quoting : chaque corps JSON est stocké dans une VARIABLE puis
#    passé via "$VAR". Ne PAS inliner un body avec des guillemets échappés dans la
#    substitution $(call ...) : le shell le transmet alors comme une *chaîne* JSON,
#    que Jackson refuse de désérialiser (→ 500 trompeur).
#
# Usage : bash scripts/catalog-security-smoke.sh
#
set -u

BASE="${BASE:-http://localhost:8080}"
PG_CONTAINER="${PG_CONTAINER:-the-shop-dev-postgres-1}"
PG_USER="${PG_USER:-the_shop_user}"
PG_DB="${PG_DB:-the_shop}"
PASS=0; FAIL=0

# call METHOD PATH TOKEN BODY -> renvoie le code HTTP, écrit le corps dans /tmp/resp.json
call() {
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
psql_exec() { docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d "$PG_DB" -tAc "$1"; }

PW='supersecret123456'   # >= 16 caractères (politique signup)

echo "############ 0. BASE PROPRE ############"
psql_exec "TRUNCATE users, refresh_tokens, products, product_stock, categories RESTART IDENTITY CASCADE;" >/dev/null
echo "  tables users/catalog vidées"

echo
echo "############ A. MISE EN PLACE DES ROLES ############"
SIGNUP_ADMIN="{\"email\":\"admin@test.com\",\"password\":\"$PW\",\"firstName\":\"Ada\",\"lastName\":\"Min\"}"
SIGNIN_ADMIN="{\"email\":\"admin@test.com\",\"password\":\"$PW\"}"
SIGNUP_CUST="{\"email\":\"cust@test.com\",\"password\":\"$PW\",\"firstName\":\"Cus\",\"lastName\":\"Tomer\"}"

check "signup admin" 201 "$(call POST /auth/signup '' "$SIGNUP_ADMIN")"
psql_exec "UPDATE users SET role='ADMIN' WHERE email='admin@test.com';" >/dev/null
echo "  -> role passé à ADMIN en base, re-signin pour un token frais"
call POST /auth/signin '' "$SIGNIN_ADMIN" >/dev/null
ADMIN=$(jq -r .accessToken /tmp/resp.json)

check "signup customer" 201 "$(call POST /auth/signup '' "$SIGNUP_CUST")"
CUST=$(jq -r .accessToken /tmp/resp.json)

echo
echo "############ B. TEST FONCTIONNEL (ecritures=ADMIN, lectures=public) ############"
CAT_CREATE='{"name":"Electronics","parentId":null}'
check "POST /categories (admin)" 201 "$(call POST /categories "$ADMIN" "$CAT_CREATE")"
CAT_ID=$(jq -r .id /tmp/resp.json); CAT_SLUG=$(jq -r .slug /tmp/resp.json)

PROD_CREATE="{\"name\":\"Laptop X\",\"description\":\"un laptop\",\"categoryId\":\"$CAT_ID\",\"price\":999.99}"
check "POST /products (admin)" 201 "$(call POST /products "$ADMIN" "$PROD_CREATE")"
PSLUG=$(jq -r .slug /tmp/resp.json)

PROD_PATCH='{"price":899.99}'
STOCK_INC='{"amount":10}'
STOCK_DEC='{"amount":3}'
CAT_RENAME='{"name":"Electro Gadgets"}'
CAT_MOVE='{"parentId":null}'

check "GET /products/{slug} (public)"      200 "$(call GET   "/products/$PSLUG" '' '')"
check "GET /categories/{slug} (public)"    200 "$(call GET   "/categories/$CAT_SLUG" '' '')"
check "PATCH /products/{slug} (admin)"     200 "$(call PATCH "/products/$PSLUG" "$ADMIN" "$PROD_PATCH")"
check "POST activate (admin)"              200 "$(call POST  "/products/$PSLUG/activate" "$ADMIN" '')"
check "POST stock/increase (admin)"        200 "$(call POST  "/products/$PSLUG/stock/increase" "$ADMIN" "$STOCK_INC")"
check "POST stock/decrease (admin)"        200 "$(call POST  "/products/$PSLUG/stock/decrease" "$ADMIN" "$STOCK_DEC")"
check "GET stock (public)"                 200 "$(call GET   "/products/$PSLUG/stock" '' '')"
check "PATCH categories/rename (admin)"    200 "$(call PATCH "/categories/$CAT_SLUG/rename" "$ADMIN" "$CAT_RENAME")"
check "PATCH categories/move (admin)"      200 "$(call PATCH "/categories/$CAT_SLUG/move" "$ADMIN" "$CAT_MOVE")"
check "POST deactivate (admin)"            200 "$(call POST  "/products/$PSLUG/deactivate" "$ADMIN" '')"

echo
echo "############ C. MATRICE DE SECURITE ############"
WRITE_PROD="{\"name\":\"Hack\",\"categoryId\":\"$CAT_ID\",\"price\":1}"
WRITE_CAT='{"name":"Pirate"}'
STOCK_ONE='{"amount":1}'
PATCH_ONE='{"price":1}'
PROD_OK="{\"name\":\"Laptop Y\",\"description\":\"ok\",\"categoryId\":\"$CAT_ID\",\"price\":50}"

check "GET /products/{slug} sans token"    200 "$(call GET  "/products/$PSLUG" '' '')"

check "POST /products SANS token => 401"   401 "$(call POST /products '' "$WRITE_PROD")"
echo "  --- corps (doit etre un ApiError) ---"; jq -c . /tmp/resp.json

check "POST /products CUSTOMER   => 403"   403 "$(call POST /products "$CUST" "$WRITE_PROD")"
echo "  --- corps (doit etre un ApiError) ---"; jq -c . /tmp/resp.json

check "POST /categories CUSTOMER => 403"   403 "$(call POST /categories "$CUST" "$WRITE_CAT")"
check "POST stock/increase CUST  => 403"   403 "$(call POST "/products/$PSLUG/stock/increase" "$CUST" "$STOCK_ONE")"
check "PATCH /products CUSTOMER   => 403"   403 "$(call PATCH "/products/$PSLUG" "$CUST" "$PATCH_ONE")"
check "POST /products ADMIN      => 201"   201 "$(call POST /products "$ADMIN" "$PROD_OK")"

echo
echo "############ RESULTAT : $PASS PASS / $FAIL FAIL ############"
[ "$FAIL" -eq 0 ]
