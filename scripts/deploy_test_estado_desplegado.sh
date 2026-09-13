#!/usr/bin/env bash

set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_SCRIPT="$ROOT_DIR/deploy.sh"
TEST_DIR="$(mktemp -d /tmp/nextdocs-deploy-state-test.XXXXXX)"

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

printf '\n========================================\n'
printf 'TEST 1 - ESTADO INEXISTENTE\n'
printf '========================================\n'

REPO="$TEST_DIR/repo"
create_repo "$REPO"

OUTPUT="$(
    "$DEPLOY_SCRIPT" \
        --test-estado-desplegado \
        --repo "$REPO" \
        --state-file "$TEST_DIR/no-existe"
)"

assert_contains "$OUTPUT" "ESTADO_EXISTE=0"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 2 - ESTADO VALIDO\n'
printf '========================================\n'

VALID_STATE="$TEST_DIR/estado-valido"
VALID_COMMIT="$(git -C "$REPO" rev-parse HEAD)"

printf '%s\n' "$VALID_COMMIT" > "$VALID_STATE"

OUTPUT="$(
    "$DEPLOY_SCRIPT" \
        --test-estado-desplegado \
        --repo "$REPO" \
        --state-file "$VALID_STATE"
)"

assert_contains "$OUTPUT" "ESTADO_EXISTE=1"
assert_contains "$OUTPUT" "ESTADO_VALIDO=1"
assert_contains "$OUTPUT" "ESTADO_COMMIT=$VALID_COMMIT"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 3 - ESTADO INVALIDO\n'
printf '========================================\n'

INVALID_STATE="$TEST_DIR/estado-invalido"

printf '%s\n' "0000000000000000000000000000000000000000" > "$INVALID_STATE"

OUTPUT="$(
    "$DEPLOY_SCRIPT" \
        --test-estado-desplegado \
        --repo "$REPO" \
        --state-file "$INVALID_STATE"
)"

assert_contains "$OUTPUT" "ESTADO_EXISTE=1"
assert_contains "$OUTPUT" "ESTADO_VALIDO=0"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TODAS LAS PRUEBAS DE ESTADO DESPLEGADO PASARON\n'
printf '========================================\n'
