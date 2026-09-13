#!/usr/bin/env bash

set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_SCRIPT="$ROOT_DIR/deploy.sh"
TEST_DIR="$(mktemp -d /tmp/nextdocs-deploy-rollback-test.XXXXXX)"

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
printf 'TEST 1 - HEALTHCHECK OBLIGATORIO\n'
printf '========================================\n'

OUTPUT="$(
    "$DEPLOY_SCRIPT" \
        --test-rollback \
        --dry-run
)"

assert_contains "$OUTPUT" "HEALTHCHECK=1"
assert_contains "$OUTPUT" "HEALTHCHECK_SERVICE=app"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 2 - TIMEOUT HEALTHCHECK\n'
printf '========================================\n'

assert_contains "$OUTPUT" "HEALTHCHECK_TIMEOUT=180"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 3 - BACKUP DE IMAGEN ANTERIOR\n'
printf '========================================\n'

assert_contains "$OUTPUT" "BACKUP_OLD_IMAGE=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 4 - ROLLBACK SI FALLA HEALTHCHECK\n'
printf '========================================\n'

assert_contains "$OUTPUT" "ROLLBACK_ON_FAILURE=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 5 - ROLLBACK USA LA IMAGEN ANTERIOR\n'
printf '========================================\n'

assert_contains "$OUTPUT" "ROLLBACK_SOURCE=OLD_IMAGE"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 6 - VERIFICACION POST-ROLLBACK\n'
printf '========================================\n'

assert_contains "$OUTPUT" "VERIFY_ROLLBACK=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TODAS LAS PRUEBAS DE ROLLBACK PASARON\n'
printf '========================================\n'
