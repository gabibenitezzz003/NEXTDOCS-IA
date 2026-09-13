#!/usr/bin/env bash

set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_SCRIPT="$ROOT_DIR/deploy.sh"
TEST_DIR="$(mktemp -d /tmp/nextdocs-deploy-backend-test.XXXXXX)"

cleanup() {
    rm -rf "$TEST_DIR"
}

trap cleanup EXIT

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

[ -x "$DEPLOY_SCRIPT" ] || fail "deploy.sh no existe o no es ejecutable"

printf '\n========================================\n'
printf 'TEST 1 - CONFIGURACION PRODUCCION\n'
printf '========================================\n'

OUTPUT="$(
    "$DEPLOY_SCRIPT" \
        --test-backend \
        --compose-file "$TEST_DIR/compose.produccion.yml" \
        --env-file "$TEST_DIR/nextdocs.env" \
        --dry-run
)"

assert_contains "$OUTPUT" "COMPOSE_FILE=$TEST_DIR/compose.produccion.yml"
assert_contains "$OUTPUT" "ENV_FILE=$TEST_DIR/nextdocs.env"
assert_contains "$OUTPUT" "SERVICE=app"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 2 - NO USA COMPOSE LOCAL\n'
printf '========================================\n'

if printf '%s\n' "$OUTPUT" | grep -Fq "compose.yml"; then
    fail "se detectó compose.yml local"
fi

assert_contains "$OUTPUT" "PRODUCCION=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 3 - OPERACION DE BUILD\n'
printf '========================================\n'

assert_contains "$OUTPUT" "BUILD=1"
assert_contains "$OUTPUT" "UP=1"
assert_contains "$OUTPUT" "NO_DEPS=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 4 - SINCRONIZACION DE SERVICIO\n'
printf '========================================\n'

assert_contains "$OUTPUT" "RECREATE_SERVICE=app"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TODAS LAS PRUEBAS DE BACKEND PASARON\n'
printf '========================================\n'
