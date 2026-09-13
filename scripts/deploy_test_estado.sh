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

cd "$TEST_DIR"

git init -q
git config user.name "NextDocs Test"
git config user.email "nextdocs-test@example.invalid"

printf 'base\n' > README.md

git add README.md
git commit -qm "base"

BASE_COMMIT="$(git rev-parse HEAD)"

git branch -M main

git clone -q --bare "$TEST_DIR" "$TEST_DIR/origin.git"

git remote add origin "$TEST_DIR/origin.git"
git fetch -q origin

git checkout -q -B main "$BASE_COMMIT"

git push -q origin main

git fetch -q origin

OUTPUT="$(
    "$DEPLOY_SCRIPT" \
        --test-estado \
        --repo "$TEST_DIR" \
        2>&1
)" || true

assert_contains "$OUTPUT" "RAMA=main"
assert_contains "$OUTPUT" "ORIGIN_MAIN=$BASE_COMMIT"
assert_contains "$OUTPUT" "HEAD=$BASE_COMMIT"

printf '\n========================================\n'
printf 'TEST ESTADO - MAIN SINCRONIZADO\n'
printf '========================================\n'
printf 'PASS\n'
printf '\n========================================\n'
printf 'PRUEBA ESTADO PASO\n'
printf '========================================\n'
