#!/usr/bin/env bash

set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_SCRIPT="$ROOT_DIR/deploy.sh"
TEST_DIR="$(mktemp -d /tmp/nextdocs-deploy-secuencia-test.XXXXXX)"

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
printf 'TEST 1 - CAPTURA ANTES DEL BUILD\n'
printf '========================================\n'

OUTPUT="$(
    "$DEPLOY_SCRIPT" \
        --test-secuencia-backend \
        --dry-run
)"

assert_contains "$OUTPUT" "STEP_01_CAPTURE_OLD_IMAGE=1"
assert_contains "$OUTPUT" "STEP_02_BUILD=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 2 - BUILD ANTES DE RECREAR\n'
printf '========================================\n'

assert_contains "$OUTPUT" "STEP_03_RECREATE=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 3 - HEALTHCHECK POST-DEPLOY\n'
printf '========================================\n'

assert_contains "$OUTPUT" "STEP_04_HEALTHCHECK=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 4 - BUILD FALLIDO NO TOCA PRODUCCION\n'
printf '========================================\n'

assert_contains "$OUTPUT" "BUILD_FAILURE_PRESERVE_RUNNING=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 5 - HEALTHCHECK FALLIDO ACTIVA ROLLBACK\n'
printf '========================================\n'

assert_contains "$OUTPUT" "HEALTH_FAILURE_ROLLBACK=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 6 - ROLLBACK USA IMAGEN CAPTURADA\n'
printf '========================================\n'

assert_contains "$OUTPUT" "ROLLBACK_IMAGE_SOURCE=CAPTURED_OLD_IMAGE"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 7 - HEALTHCHECK DEL ROLLBACK\n'
printf '========================================\n'

assert_contains "$OUTPUT" "STEP_05_ROLLBACK_HEALTHCHECK=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 8 - ROLLBACK FALLIDO DETIENE DEPLOY\n'
printf '========================================\n'

assert_contains "$OUTPUT" "ROLLBACK_FAILURE_BLOCK_DEPLOY=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TODAS LAS PRUEBAS DE SECUENCIA PASARON\n'
printf '========================================\n'
