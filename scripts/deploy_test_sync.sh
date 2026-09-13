#!/usr/bin/env bash

set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_SCRIPT="$ROOT_DIR/deploy.sh"
TEST_DIR="$(mktemp -d /tmp/nextdocs-deploy-sync-test.XXXXXX)"

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

git branch -M main

git clone -q --bare "$TEST_DIR" "$TEST_DIR/origin.git"
git remote add origin "$TEST_DIR/origin.git"
git push -q -u origin main
git fetch -q origin

BASE_COMMIT="$(git rev-parse HEAD)"

printf 'servidor atrasado\n' >> README.md
git add README.md
git commit -qm "commit remoto"

REMOTE_COMMIT="$(git rev-parse HEAD)"
git push -q origin main

git reset -q --hard "$BASE_COMMIT"
git fetch -q origin

OUTPUT="$(
    "$DEPLOY_SCRIPT" \
        --test-sincronizacion \
        --repo "$TEST_DIR" \
        2>&1
)" || true

assert_contains "$OUTPUT" "AHEAD=0"
assert_contains "$OUTPUT" "BEHIND=1"

printf '\n========================================\n'
printf 'TEST SYNC - SERVIDOR ATRASADO\n'
printf '========================================\n'
printf 'PASS\n'
printf '\n========================================\n'
printf 'PRUEBA SYNC PASO\n'
printf '========================================\n'
