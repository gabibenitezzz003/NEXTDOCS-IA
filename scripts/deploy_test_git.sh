#!/usr/bin/env bash

set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_SCRIPT="$ROOT_DIR/deploy.sh"
TEST_DIR="$(mktemp -d /tmp/nextdocs-deploy-git-test.XXXXXX)"

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
        fail "no debería existir '$unexpected'"
    fi
}

[ -x "$DEPLOY_SCRIPT" ] \
    || fail "deploy.sh no existe o no es ejecutable"

cd "$TEST_DIR"

git init -q
git config user.name "NextDocs Test"
git config user.email "nextdocs-test@example.invalid"

mkdir -p frontend backend/src/main/java backend/src/main/resources/db/migration

printf 'base\n' > README.md
printf 'base\n' > frontend/App.tsx
printf 'base\n' > backend/src/main/java/App.java

git add .
git commit -qm "base"

BASE_COMMIT="$(git rev-parse HEAD)"

printf 'frontend cambiado\n' > frontend/App.tsx

git add frontend/App.tsx
git commit -qm "cambio frontend"

FRONTEND_COMMIT="$(git rev-parse HEAD)"

printf 'backend cambiado\n' > backend/src/main/java/App.java
printf 'create table prueba (id bigint);\n' \
    > backend/src/main/resources/db/migration/V19__prueba.sql

git add .
git commit -qm "cambio backend y migracion"

BACKEND_COMMIT="$(git rev-parse HEAD)"

printf 'documentacion\n' >> README.md

git add README.md
git commit -qm "documentacion"

DOCS_COMMIT="$(git rev-parse HEAD)"

printf '\n========================================\n'
printf 'TEST GIT 1 - FRONTEND\n'
printf '========================================\n'

OUTPUT="$(
    "$DEPLOY_SCRIPT" \
        --test-git-diff \
        "$BASE_COMMIT" \
        "$FRONTEND_COMMIT" \
        --repo "$TEST_DIR"
)"

assert_contains "$OUTPUT" "DEPLOY_FRONTEND=1"
assert_contains "$OUTPUT" "DEPLOY_BACKEND=0"
assert_contains "$OUTPUT" "DEPLOY_DATABASE=0"
assert_contains "$OUTPUT" "DEPLOY_COMPOSE=0"
assert_contains "$OUTPUT" "DEPLOY_NGINX=0"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST GIT 2 - BACKEND + MIGRACION\n'
printf '========================================\n'

OUTPUT="$(
    "$DEPLOY_SCRIPT" \
        --test-git-diff \
        "$FRONTEND_COMMIT" \
        "$BACKEND_COMMIT" \
        --repo "$TEST_DIR"
)"

assert_contains "$OUTPUT" "DEPLOY_FRONTEND=0"
assert_contains "$OUTPUT" "DEPLOY_BACKEND=1"
assert_contains "$OUTPUT" "DEPLOY_DATABASE=1"
assert_contains "$OUTPUT" "DEPLOY_COMPOSE=0"
assert_contains "$OUTPUT" "DEPLOY_NGINX=0"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST GIT 3 - SOLO DOCUMENTACION\n'
printf '========================================\n'

OUTPUT="$(
    "$DEPLOY_SCRIPT" \
        --test-git-diff \
        "$BACKEND_COMMIT" \
        "$DOCS_COMMIT" \
        --repo "$TEST_DIR"
)"

assert_contains "$OUTPUT" "DEPLOY_FRONTEND=0"
assert_contains "$OUTPUT" "DEPLOY_BACKEND=0"
assert_contains "$OUTPUT" "DEPLOY_DATABASE=0"
assert_contains "$OUTPUT" "DEPLOY_COMPOSE=0"
assert_contains "$OUTPUT" "DEPLOY_NGINX=0"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TODAS LAS PRUEBAS GIT PASARON\n'
printf '========================================\n'
