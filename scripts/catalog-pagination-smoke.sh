#!/bin/bash
#
# Smoke test runtime de la PAGINATION du catalog (GET /products + GET /admin/products).
#
# Couvre :
#   - visibilité : listing public = ACTIVE only ; /admin/products = tous statuts
#   - read model : inStock dérivé (qty>0), categoryName/categorySlug joints
#   - filtre par catégorie (slug)
#   - tri whitelisté (name/price, asc/desc) + rejet 400 d'un champ/direction hors whitelist
#   - clamp du size (0 -> défaut 20, 400 -> plafond 100) + pagination (page, totalElements)
#   - sécurité /admin/** : 401 sans token, 403 CUSTOMER, 200 ADMIN
#
# Prérequis (identiques à catalog-security-smoke.sh) :
#   - l'appli tourne sur $BASE (profil dev)        : docker compose -f docker-compose.dev.yml up -d
#   - le conteneur Postgres est up                 : (idem)
#   - jq et docker disponibles dans le PATH
#
# La promotion ADMIN se fait en base (aucun endpoint ne crée d'ADMIN), puis on
# RE-signin pour un token portant le rôle ADMIN (rôle figé dans le JWT à l'émission).
#
# ⚠️ Le script VIDE les tables users/catalog au démarrage (repart d'une base propre).
# ⚠️ Quoting : chaque corps JSON est passé via variable (voir catalog-security-smoke.sh).
#
# Usage : bash scripts/catalog-pagination-smoke.sh
#
set -u

BASE="${BASE:-http://localhost:8080}"
PG_CONTAINER="${PG_CONTAINER:-the-shop-dev-postgres-1}"
PG_USER="${PG_USER:-the_shop_user}"
PG_DB="${PG_DB:-the_shop}"
PASS=0; FAIL=0
PW='supersecret123456'   # >= 16 caractères (politique signup)

# call METHOD PATH TOKEN BODY -> renvoie le code HTTP, écrit le corps dans /tmp/r.json
call() {
  local method=$1 path=$2 token=$3 body=$4
  local args=(-s -o /tmp/r.json -w "%{http_code}" -X "$method" "$BASE$path")
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$body" ]  && args+=(-H "Content-Type: application/json" -d "$body")
  curl "${args[@]}"
}
check()  { if [ "$3" = "$2" ]; then echo "  PASS  $1 -> $3"; PASS=$((PASS+1)); else echo "  FAIL  $1 -> got $3 expected $2"; FAIL=$((FAIL+1)); cat /tmp/r.json; echo; fi; }
checkv() { if [ "$3" = "$2" ]; then echo "  PASS  $1 = $3"; PASS=$((PASS+1)); else echo "  FAIL  $1 = $3 expected $2"; FAIL=$((FAIL+1)); fi; }
psql_exec() { docker exec "$PG_CONTAINER" psql -U "$PG_USER" -d "$PG_DB" -tAc "$1"; }

echo "############ 0. BASE PROPRE ############"
psql_exec "TRUNCATE users, refresh_tokens, products, product_stock, categories RESTART IDENTITY CASCADE;" >/dev/null
echo "  tables users/catalog vidées"

echo
echo "############ A. ROLES ############"
call POST /auth/signup '' "{\"email\":\"admin@test.com\",\"password\":\"$PW\",\"firstName\":\"Ad\",\"lastName\":\"Min\"}" >/dev/null
psql_exec "UPDATE users SET role='ADMIN' WHERE email='admin@test.com';" >/dev/null
call POST /auth/signin '' "{\"email\":\"admin@test.com\",\"password\":\"$PW\"}" >/dev/null
ADMIN=$(jq -r .accessToken /tmp/r.json)
call POST /auth/signup '' "{\"email\":\"cust@test.com\",\"password\":\"$PW\",\"firstName\":\"Cu\",\"lastName\":\"St\"}" >/dev/null
CUST=$(jq -r .accessToken /tmp/r.json)

echo
echo "############ B. SEED CATALOG ############"
call POST /categories "$ADMIN" '{"name":"Electronics","parentId":null}' >/dev/null
EID=$(jq -r .id /tmp/r.json); ESLUG=$(jq -r .slug /tmp/r.json)
call POST /categories "$ADMIN" '{"name":"Books","parentId":null}' >/dev/null
BID=$(jq -r .id /tmp/r.json); BSLUG=$(jq -r .slug /tmp/r.json)

mkprod() { # name price catId -> echoes slug
  call POST /products "$ADMIN" "{\"name\":\"$1\",\"description\":\"d\",\"categoryId\":\"$3\",\"price\":$2}" >/dev/null
  jq -r .slug /tmp/r.json
}
LAPTOP=$(mkprod "Laptop" 999.99 "$EID")
MOUSE=$(mkprod "Mouse" 19.99 "$EID")
KEYB=$(mkprod "Keyboard" 49.99 "$EID")
TABLET=$(mkprod "Tablet" 299.99 "$EID")   # reste DRAFT (jamais activé)
MONITOR=$(mkprod "Monitor" 199.99 "$EID") # sera INACTIVE
NOVEL=$(mkprod "Novel" 9.99 "$BID")

for s in "$LAPTOP" "$MOUSE" "$KEYB" "$MONITOR" "$NOVEL"; do call POST "/products/$s/activate" "$ADMIN" '' >/dev/null; done
call POST "/products/$LAPTOP/stock/increase" "$ADMIN" '{"amount":5}' >/dev/null
call POST "/products/$KEYB/stock/increase" "$ADMIN" '{"amount":10}' >/dev/null
call POST "/products/$NOVEL/stock/increase" "$ADMIN" '{"amount":3}' >/dev/null
# Mouse reste à 0 (inStock false). Monitor -> INACTIVE. Tablet reste DRAFT.
call POST "/products/$MONITOR/deactivate" "$ADMIN" '' >/dev/null

echo
echo "############ C. VISIBILITE (public = ACTIVE only) ############"
check "GET /products public 200" 200 "$(call GET /products '' '')"
checkv "public totalElements (4 ACTIVE)" 4 "$(jq -r .totalElements /tmp/r.json)"
checkv "public ne contient PAS Tablet(DRAFT)" 0 "$(jq -r '[.content[]|select(.name=="Tablet")]|length' /tmp/r.json)"
checkv "public ne contient PAS Monitor(INACTIVE)" 0 "$(jq -r '[.content[]|select(.name=="Monitor")]|length' /tmp/r.json)"

echo
echo "############ D. READ MODEL (inStock dérivé + jointure catégorie) ############"
checkv "Laptop inStock true" true "$(jq -r '.content[]|select(.name=="Laptop").inStock' /tmp/r.json)"
checkv "Mouse inStock false (qty 0)" false "$(jq -r '.content[]|select(.name=="Mouse").inStock' /tmp/r.json)"
checkv "Laptop categoryName joint" "Electronics" "$(jq -r '.content[]|select(.name=="Laptop").categoryName' /tmp/r.json)"

echo
echo "############ E. FILTRE CATEGORIE ############"
call GET "/products?category=$ESLUG" '' '' >/dev/null
checkv "category=electronics -> 3 ACTIVE" 3 "$(jq -r .totalElements /tmp/r.json)"
call GET "/products?category=$BSLUG" '' '' >/dev/null
checkv "category=books -> 1" 1 "$(jq -r .totalElements /tmp/r.json)"

echo
echo "############ F. TRI ############"
call GET "/products?sort=price,desc" '' '' >/dev/null
checkv "price desc 1er = Laptop" "Laptop" "$(jq -r '.content[0].name' /tmp/r.json)"
checkv "price desc dernier = Novel" "Novel" "$(jq -r '.content[-1].name' /tmp/r.json)"
call GET "/products?sort=price,asc" '' '' >/dev/null
checkv "price asc 1er = Novel" "Novel" "$(jq -r '.content[0].name' /tmp/r.json)"
call GET "/products?sort=name,asc" '' '' >/dev/null
checkv "name asc 1er = Keyboard" "Keyboard" "$(jq -r '.content[0].name' /tmp/r.json)"

echo
echo "############ G. TRI INVALIDE -> 400 (whitelist / anti-injection) ############"
check "sort=secret -> 400" 400 "$(call GET '/products?sort=secret,desc' '' '')"
check "sort=price,sideways -> 400" 400 "$(call GET '/products?sort=price,sideways' '' '')"

echo
echo "############ H. SIZE (clamp) ############"
call GET "/products?size=0" '' '' >/dev/null
checkv "size=0 -> défaut 20" 20 "$(jq -r .size /tmp/r.json)"
checkv "size=0 contenu = 4" 4 "$(jq -r '.content|length' /tmp/r.json)"
call GET "/products?size=20" '' '' >/dev/null
checkv "size=20 -> 20" 20 "$(jq -r .size /tmp/r.json)"
call GET "/products?size=400" '' '' >/dev/null
checkv "size=400 -> plafond 100" 100 "$(jq -r .size /tmp/r.json)"
call GET "/products?size=2" '' '' >/dev/null
checkv "size=2 -> 2 items" 2 "$(jq -r '.content|length' /tmp/r.json)"
checkv "size=2 totalElements toujours 4" 4 "$(jq -r .totalElements /tmp/r.json)"
checkv "size=2 page=0" 0 "$(jq -r .page /tmp/r.json)"

echo
echo "############ I. PAGINATION page 2 (tie-breaker stable) ############"
call GET "/products?size=2&page=1&sort=name,asc" '' '' >/dev/null
checkv "page1 size2 name asc 1er=Mouse" "Mouse" "$(jq -r '.content[0].name' /tmp/r.json)"
checkv "page=1" 1 "$(jq -r .page /tmp/r.json)"

echo
echo "############ J. SECURITE /admin/products ############"
check "admin sans token -> 401" 401 "$(call GET /admin/products '' '')"
check "admin CUSTOMER -> 403" 403 "$(call GET /admin/products "$CUST" '')"
check "admin ADMIN -> 200" 200 "$(call GET /admin/products "$ADMIN" '')"
checkv "admin voit TOUT (6 produits)" 6 "$(jq -r .totalElements /tmp/r.json)"
checkv "admin voit Tablet(DRAFT)" 1 "$(jq -r '[.content[]|select(.name=="Tablet")]|length' /tmp/r.json)"
checkv "admin voit Monitor(INACTIVE)" 1 "$(jq -r '[.content[]|select(.name=="Monitor")]|length' /tmp/r.json)"

echo
echo "############ RESULTAT : $PASS PASS / $FAIL FAIL ############"
[ "$FAIL" -eq 0 ]
