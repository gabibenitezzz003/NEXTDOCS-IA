#!/usr/bin/env bash

set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_SCRIPT="$ROOT_DIR/deploy.sh"
TEST_DIR="$(mktemp -d /tmp/nextdocs-deploy-baseline-test.XXXXXX)"

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

assert_not_contains() {
    local output="$1"
    local unexpected="$2"

    if printf '%s\n' "$output" | grep -Fxq "$unexpected"; then
        fail "no debía aparecer '$unexpected'"
    fi
}

[ -x "$DEPLOY_SCRIPT" ] || fail "deploy.sh no existe o no es ejecutable"

create_repo() {
    local repo="$1"

    mkdir -p "$repo"
    cd "$repo"

    git init -q
    git config user.name "NextDocs Test"
    git config user.email "nextdocs-test@example.invalid"

    printf 'base\n' > README.md

    git add README.md
    git commit -qm "base"

    git branch -M main

    git clone -q --bare "$repo" "$repo/origin.git"
    git remote add origin "$repo/origin.git"
    git fetch -q origin
}

run_baseline() {
    local repo="$1"
    local state_file="$2"

    "$DEPLOY_SCRIPT" \
        --test-baseline \
        --repo "$repo" \
        --state-file "$state_file"
}

printf '\n========================================\n'
printf 'TEST 1 - PRIMER DEPLOY\n'
printf '========================================\n'

REPO="$TEST_DIR/repo"
STATE_FILE="$TEST_DIR/deployed_commit"

create_repo "$REPO"

OUTPUT="$(run_baseline "$REPO" "$STATE_FILE")"

assert_contains "$OUTPUT" "BASELINE_NECESARIO=1"
assert_contains "$OUTPUT" "DEPLOY_FRONTEND=1"
assert_contains "$OUTPUT" "DEPLOY_BACKEND=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 2 - DEPLOY YA INICIALIZADO\n'
printf '========================================\n'

COMMIT="$(git -C "$REPO" rev-parse HEAD)"
printf '%s\n' "$COMMIT" > "$STATE_FILE"

OUTPUT="$(run_baseline "$REPO" "$STATE_FILE")"

assert_contains "$OUTPUT" "BASELINE_NECESARIO=0"
assert_not_contains "$OUTPUT" "DEPLOY_FRONTEND=1"
assert_not_contains "$OUTPUT" "DEPLOY_BACKEND=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 3 - ESTADO VACIO DEBE BLOQUEAR\n'
printf '========================================\n'

: > "$STATE_FILE"

if run_baseline "$REPO" "$STATE_FILE" >/tmp/nextdocs-baseline-empty.out 2>/tmp/nextdocs-baseline-empty.err; then
    fail "un estado vacío fue aceptado"
fi

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 4 - ESTADO INVALIDO DEBE BLOQUEAR\n'
printf '========================================\n'

printf '%s\n' 'commit-inexistente-0000000000000000000000000000000000000000' > "$STATE_FILE"

if run_baseline "$REPO" "$STATE_FILE" >/tmp/nextdocs-baseline-invalid.out 2>/tmp/nextdocs-baseline-invalid.err; then
    fail "un estado inválido fue aceptado"
fi

printf 'PASS\n'

printf '\n========================================\n'
printf 'TODAS LAS PRUEBAS DE BASELINE PASARON\n'
printf '========================================\n'
