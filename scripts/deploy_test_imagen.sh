#!/usr/bin/env bash

set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_SCRIPT="$ROOT_DIR/deploy.sh"
TEST_DIR="$(mktemp -d /tmp/nextdocs-deploy-imagen-test.XXXXXX)"

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
printf 'TEST 1 - CONTENEDOR DE PRODUCCION\n'
printf '========================================\n'

OUTPUT="$(
    "$DEPLOY_SCRIPT" \
        --test-imagen \
        --container nextdocs-ia-app \
        --dry-run
)"

assert_contains "$OUTPUT" "CONTAINER=nextdocs-ia-app"
assert_contains "$OUTPUT" "CAPTURE_IMAGE_ID=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 2 - IMAGE ID OBLIGATORIO\n'
printf '========================================\n'

assert_contains "$OUTPUT" "IMAGE_ID_REQUIRED=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 3 - BACKUP IDENTIFICABLE\n'
printf '========================================\n'

assert_contains "$OUTPUT" "BACKUP_IMAGE_TAG=nextdocs-ia-backup"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 4 - BACKUP ANTES DE RECREAR\n'
printf '========================================\n'

assert_contains "$OUTPUT" "BACKUP_BEFORE_RECREATE=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 5 - NO MODIFICA PRODUCCION EN DRY RUN\n'
printf '========================================\n'

assert_contains "$OUTPUT" "DRY_RUN=1"
assert_contains "$OUTPUT" "EXECUTE_DOCKER=0"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TODAS LAS PRUEBAS DE IMAGEN PASARON\n'
printf '========================================\n'
