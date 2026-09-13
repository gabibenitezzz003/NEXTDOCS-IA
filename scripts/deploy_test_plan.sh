#!/usr/bin/env bash

set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_SCRIPT="$ROOT_DIR/deploy.sh"
TEST_DIR="$(mktemp -d /tmp/nextdocs-deploy-plan-test.XXXXXX)"

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
        fail "no debía existir '$unexpected'"
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

new_commit() {
    local message="$1"

    git add .
    git commit -qm "$message"
}

printf '\n========================================\n'
printf 'TEST 1 - SOLO DOCUMENTACION\n'
printf '========================================\n'

REPO="$TEST_DIR/docs"
create_repo "$REPO"

cd "$REPO"

printf 'documentacion\n' >> README.md
new_commit "docs"

OUTPUT="$(
    "$DEPLOY_SCRIPT" \
        --test-plan \
        --repo "$REPO" \
        --from "$(git rev-parse HEAD~1)" \
        --to "$(git rev-parse HEAD)"
)"

assert_contains "$OUTPUT" "DEPLOY_FRONTEND=0"
assert_contains "$OUTPUT" "DEPLOY_BACKEND=0"
assert_contains "$OUTPUT" "DEPLOY_DATABASE=0"
assert_contains "$OUTPUT" "DEPLOY_COMPOSE=0"
assert_contains "$OUTPUT" "DEPLOY_NGINX=0"
assert_contains "$OUTPUT" "DEPLOY_DOCUMENTACION=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 2 - SOLO FRONTEND\n'
printf '========================================\n'

REPO="$TEST_DIR/frontend"
create_repo "$REPO"

cd "$REPO"

mkdir -p frontend/src
printf 'frontend\n' > frontend/src/App.tsx
new_commit "frontend"

OUTPUT="$(
    "$DEPLOY_SCRIPT" \
        --test-plan \
        --repo "$REPO" \
        --from "$(git rev-parse HEAD~1)" \
        --to "$(git rev-parse HEAD)"
)"

assert_contains "$OUTPUT" "DEPLOY_FRONTEND=1"
assert_contains "$OUTPUT" "DEPLOY_BACKEND=0"
assert_contains "$OUTPUT" "DEPLOY_DATABASE=0"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 3 - SOLO BACKEND\n'
printf '========================================\n'

REPO="$TEST_DIR/backend"
create_repo "$REPO"

cd "$REPO"

mkdir -p backend/src/main/java
printf 'backend\n' > backend/src/main/java/Servicio.java
new_commit "backend"

OUTPUT="$(
    "$DEPLOY_SCRIPT" \
        --test-plan \
        --repo "$REPO" \
        --from "$(git rev-parse HEAD~1)" \
        --to "$(git rev-parse HEAD)"
)"

assert_contains "$OUTPUT" "DEPLOY_FRONTEND=0"
assert_contains "$OUTPUT" "DEPLOY_BACKEND=1"
assert_contains "$OUTPUT" "DEPLOY_DATABASE=0"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 4 - MIGRACION\n'
printf '========================================\n'

REPO="$TEST_DIR/migration"
create_repo "$REPO"

cd "$REPO"

mkdir -p backend/src/main/resources/db/migration
printf 'migration\n' > backend/src/main/resources/db/migration/V19__prueba.sql
new_commit "migration"

OUTPUT="$(
    "$DEPLOY_SCRIPT" \
        --test-plan \
        --repo "$REPO" \
        --from "$(git rev-parse HEAD~1)" \
        --to "$(git rev-parse HEAD)"
)"

assert_contains "$OUTPUT" "DEPLOY_FRONTEND=0"
assert_contains "$OUTPUT" "DEPLOY_BACKEND=1"
assert_contains "$OUTPUT" "DEPLOY_DATABASE=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 5 - FRONTEND + BACKEND\n'
printf '========================================\n'

REPO="$TEST_DIR/full"
create_repo "$REPO"

cd "$REPO"

mkdir -p frontend/src
mkdir -p backend/src/main/java

printf 'frontend\n' > frontend/src/App.tsx
printf 'backend\n' > backend/src/main/java/Servicio.java

new_commit "frontend y backend"

OUTPUT="$(
    "$DEPLOY_SCRIPT" \
        --test-plan \
        --repo "$REPO" \
        --from "$(git rev-parse HEAD~1)" \
        --to "$(git rev-parse HEAD)"
)"

assert_contains "$OUTPUT" "DEPLOY_FRONTEND=1"
assert_contains "$OUTPUT" "DEPLOY_BACKEND=1"
assert_contains "$OUTPUT" "DEPLOY_DATABASE=0"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 6 - COMPOSE REQUIERE REVISION\n'
printf '========================================\n'

REPO="$TEST_DIR/compose"
create_repo "$REPO"

cd "$REPO"

printf 'compose\n' > compose.yml
new_commit "compose"

OUTPUT="$(
    "$DEPLOY_SCRIPT" \
        --test-plan \
        --repo "$REPO" \
        --from "$(git rev-parse HEAD~1)" \
        --to "$(git rev-parse HEAD)"
)"

assert_contains "$OUTPUT" "DEPLOY_COMPOSE=1"
assert_contains "$OUTPUT" "REVISION_MANUAL=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 7 - NGINX REQUIERE REVISION\n'
printf '========================================\n'

REPO="$TEST_DIR/nginx"
create_repo "$REPO"

cd "$REPO"

mkdir -p infra/nginx
printf 'nginx\n' > infra/nginx/default.conf
new_commit "nginx"

OUTPUT="$(
    "$DEPLOY_SCRIPT" \
        --test-plan \
        --repo "$REPO" \
        --from "$(git rev-parse HEAD~1)" \
        --to "$(git rev-parse HEAD)"
)"

assert_contains "$OUTPUT" "DEPLOY_NGINX=1"
assert_contains "$OUTPUT" "REVISION_MANUAL=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TODAS LAS PRUEBAS DE PLAN PASARON\n'
printf '========================================\n'
