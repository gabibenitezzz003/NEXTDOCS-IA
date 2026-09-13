#!/usr/bin/env bash

set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_SCRIPT="$ROOT_DIR/deploy.sh"
TEST_DIR="$(mktemp -d /tmp/nextdocs-deploy-plan-estado-test.XXXXXX)"

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

run_plan() {
    local repo="$1"
    local state_file="$2"

    "$DEPLOY_SCRIPT" \
        --test-plan-estado \
        --repo "$repo" \
        --state-file "$state_file"
}

printf '\n========================================\n'
printf 'TEST 1 - ESTADO DESPLEGADO VALIDO SIN CAMBIOS\n'
printf '========================================\n'

REPO="$TEST_DIR/repo1"
STATE_FILE="$TEST_DIR/state1"

create_repo "$REPO"

COMMIT="$(git -C "$REPO" rev-parse HEAD)"
printf '%s\n' "$COMMIT" > "$STATE_FILE"

OUTPUT="$(run_plan "$REPO" "$STATE_FILE")"

assert_contains "$OUTPUT" "PLAN_ESTADO=VALIDO"
assert_contains "$OUTPUT" "DEPLOY_FRONTEND=0"
assert_contains "$OUTPUT" "DEPLOY_BACKEND=0"
assert_contains "$OUTPUT" "DEPLOY_DATABASE=0"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 2 - PRIMER DEPLOY SIN ESTADO\n'
printf '========================================\n'

REPO="$TEST_DIR/repo-primer-deploy"
STATE_FILE="$TEST_DIR/state-primer-deploy"

create_repo "$REPO"

OUTPUT="$(run_plan "$REPO" "$STATE_FILE")"

assert_contains "$OUTPUT" "PLAN_ESTADO=PRIMER_DEPLOY"
assert_contains "$OUTPUT" "DEPLOY_FRONTEND=1"
assert_contains "$OUTPUT" "DEPLOY_BACKEND=1"
assert_contains "$OUTPUT" "DEPLOY_DATABASE=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 2 - NUEVO COMMIT FRONTEND\n'
printf '========================================\n'

REPO="$TEST_DIR/repo2"
STATE_FILE="$TEST_DIR/state2"

create_repo "$REPO"

OLD_COMMIT="$(git -C "$REPO" rev-parse HEAD)"
printf '%s\n' "$OLD_COMMIT" > "$STATE_FILE"

mkdir -p "$REPO/frontend/src"
printf 'frontend\n' > "$REPO/frontend/src/test.tsx"
git -C "$REPO" add frontend/src/test.tsx
git -C "$REPO" commit -qm "frontend"

git -C "$REPO" push -q origin main
git -C "$REPO" fetch -q origin

OUTPUT="$(run_plan "$REPO" "$STATE_FILE")"

assert_contains "$OUTPUT" "PLAN_ESTADO=VALIDO"
assert_contains "$OUTPUT" "DEPLOY_FRONTEND=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 3 - NUEVO COMMIT BACKEND\n'
printf '========================================\n'

REPO="$TEST_DIR/repo3"
STATE_FILE="$TEST_DIR/state3"

create_repo "$REPO"

OLD_COMMIT="$(git -C "$REPO" rev-parse HEAD)"
printf '%s\n' "$OLD_COMMIT" > "$STATE_FILE"

mkdir -p "$REPO/backend/src"
printf 'backend\n' > "$REPO/backend/src/test.java"

git -C "$REPO" add backend/src/test.java
git -C "$REPO" commit -qm "backend"

git -C "$REPO" push -q origin main
git -C "$REPO" fetch -q origin

OUTPUT="$(run_plan "$REPO" "$STATE_FILE")"

assert_contains "$OUTPUT" "PLAN_ESTADO=VALIDO"
assert_contains "$OUTPUT" "DEPLOY_BACKEND=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 4 - MIGRACION\n'
printf '========================================\n'

REPO="$TEST_DIR/repo4"
STATE_FILE="$TEST_DIR/state4"

create_repo "$REPO"

OLD_COMMIT="$(git -C "$REPO" rev-parse HEAD)"
printf '%s\n' "$OLD_COMMIT" > "$STATE_FILE"

mkdir -p "$REPO/backend/src/main/resources/db/migration"
printf 'migration\n' > "$REPO/backend/src/main/resources/db/migration/V99__test.sql"

git -C "$REPO" add backend/src/main/resources/db/migration/V99__test.sql
git -C "$REPO" commit -qm "migration"

git -C "$REPO" push -q origin main
git -C "$REPO" fetch -q origin

OUTPUT="$(run_plan "$REPO" "$STATE_FILE")"

assert_contains "$OUTPUT" "PLAN_ESTADO=VALIDO"
assert_contains "$OUTPUT" "DEPLOY_BACKEND=1"
assert_contains "$OUTPUT" "DEPLOY_DATABASE=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 5 - DOCUMENTACION\n'
printf '========================================\n'

REPO="$TEST_DIR/repo5"
STATE_FILE="$TEST_DIR/state5"

create_repo "$REPO"

OLD_COMMIT="$(git -C "$REPO" rev-parse HEAD)"
printf '%s\n' "$OLD_COMMIT" > "$STATE_FILE"

printf 'docs\n' >> "$REPO/README.md"
git -C "$REPO" add README.md
git -C "$REPO" commit -qm "docs"

git -C "$REPO" push -q origin main
git -C "$REPO" fetch -q origin

OUTPUT="$(run_plan "$REPO" "$STATE_FILE")"

assert_contains "$OUTPUT" "PLAN_ESTADO=VALIDO"
assert_contains "$OUTPUT" "DEPLOY_FRONTEND=0"
assert_contains "$OUTPUT" "DEPLOY_BACKEND=0"
assert_contains "$OUTPUT" "DEPLOY_DATABASE=0"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TODAS LAS PRUEBAS DE PLAN DESDE ESTADO PASARON\n'
printf '========================================\n'
