#!/usr/bin/env bash

set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_SCRIPT="$ROOT_DIR/deploy.sh"

fail() {
    printf 'FAIL: %s\n' "$1" >&2
    exit 1
}

assert_contains() {
    local output="$1"
    local expected="$2"

    printf '%s\n' "$output" | grep -Fxq "$expected" \
        || fail "faltó '$expected'"
}

assert_not_contains() {
    local output="$1"
    local unexpected="$2"

    if printf '%s\n' "$output" | grep -Fxq "$unexpected"; then
        fail "no debería existir '$unexpected'"
    fi
}

[ -x "$DEPLOY_SCRIPT" ] \
    || fail "deploy.sh no existe o no es ejecutable"

printf '\n========================================\n'
printf 'TEST 1 - SOLO FRONTEND\n'
printf '========================================\n'

OUTPUT="$("$DEPLOY_SCRIPT" --test-clasificacion \
    frontend/src/App.tsx)"

assert_contains "$OUTPUT" "DEPLOY_FRONTEND=1"
assert_contains "$OUTPUT" "DEPLOY_BACKEND=0"
assert_contains "$OUTPUT" "DEPLOY_DATABASE=0"
assert_contains "$OUTPUT" "DEPLOY_COMPOSE=0"
assert_contains "$OUTPUT" "DEPLOY_NGINX=0"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 2 - SOLO BACKEND\n'
printf '========================================\n'

OUTPUT="$("$DEPLOY_SCRIPT" --test-clasificacion \
    backend/src/main/java/com/nextdocs/ai/Servicio.java)"

assert_contains "$OUTPUT" "DEPLOY_FRONTEND=0"
assert_contains "$OUTPUT" "DEPLOY_BACKEND=1"
assert_contains "$OUTPUT" "DEPLOY_DATABASE=0"
assert_contains "$OUTPUT" "DEPLOY_COMPOSE=0"
assert_contains "$OUTPUT" "DEPLOY_NGINX=0"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 3 - MIGRACION\n'
printf '========================================\n'

OUTPUT="$("$DEPLOY_SCRIPT" --test-clasificacion \
    backend/src/main/resources/db/migration/V19__nueva_migracion.sql)"

assert_contains "$OUTPUT" "DEPLOY_FRONTEND=0"
assert_contains "$OUTPUT" "DEPLOY_BACKEND=1"
assert_contains "$OUTPUT" "DEPLOY_DATABASE=1"
assert_contains "$OUTPUT" "DEPLOY_COMPOSE=0"
assert_contains "$OUTPUT" "DEPLOY_NGINX=0"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 4 - COMPOSE\n'
printf '========================================\n'

OUTPUT="$("$DEPLOY_SCRIPT" --test-clasificacion \
    compose.yml)"

assert_contains "$OUTPUT" "DEPLOY_FRONTEND=0"
assert_contains "$OUTPUT" "DEPLOY_BACKEND=0"
assert_contains "$OUTPUT" "DEPLOY_DATABASE=0"
assert_contains "$OUTPUT" "DEPLOY_COMPOSE=1"
assert_contains "$OUTPUT" "DEPLOY_NGINX=0"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 5 - NGINX\n'
printf '========================================\n'

OUTPUT="$("$DEPLOY_SCRIPT" --test-clasificacion \
    infra/nginx/default.conf)"

assert_contains "$OUTPUT" "DEPLOY_FRONTEND=0"
assert_contains "$OUTPUT" "DEPLOY_BACKEND=0"
assert_contains "$OUTPUT" "DEPLOY_DATABASE=0"
assert_contains "$OUTPUT" "DEPLOY_COMPOSE=0"
assert_contains "$OUTPUT" "DEPLOY_NGINX=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 6 - DOCUMENTACION\n'
printf '========================================\n'

OUTPUT="$("$DEPLOY_SCRIPT" --test-clasificacion \
    README.md)"

assert_contains "$OUTPUT" "DEPLOY_FRONTEND=0"
assert_contains "$OUTPUT" "DEPLOY_BACKEND=0"
assert_contains "$OUTPUT" "DEPLOY_DATABASE=0"
assert_contains "$OUTPUT" "DEPLOY_COMPOSE=0"
assert_contains "$OUTPUT" "DEPLOY_NGINX=0"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TODAS LAS PRUEBAS PASARON\n'
printf '========================================\n'
