#!/usr/bin/env bash

set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_SCRIPT="$ROOT_DIR/deploy.sh"
TEST_DIR="$(mktemp -d /tmp/nextdocs-deploy-sync-states-test.XXXXXX)"

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
    git push -q -u origin main
    git fetch -q origin
}

printf '\n========================================\n'
printf 'TEST 1 - SINCRONIZADO\n'
printf '========================================\n'

SYNC_REPO="$TEST_DIR/sincronizado"
create_repo "$SYNC_REPO"

OUTPUT="$(
    "$DEPLOY_SCRIPT" \
        --test-sincronizacion \
        --repo "$SYNC_REPO"
)"

assert_contains "$OUTPUT" "AHEAD=0"
assert_contains "$OUTPUT" "BEHIND=0"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 2 - SERVIDOR ATRASADO\n'
printf '========================================\n'

BEHIND_REPO="$TEST_DIR/atrasado"
create_repo "$BEHIND_REPO"

cd "$BEHIND_REPO"

BASE_COMMIT="$(git rev-parse HEAD)"

git clone -q "$BEHIND_REPO/origin.git" "$TEST_DIR/remoto-atrasado"

cd "$TEST_DIR/remoto-atrasado"

printf 'commit remoto\n' >> README.md
git add README.md
git commit -qm "commit remoto"
git push -q origin main

cd "$BEHIND_REPO"

git reset -q --hard "$BASE_COMMIT"
git fetch -q origin

OUTPUT="$(
    "$DEPLOY_SCRIPT" \
        --test-sincronizacion \
        --repo "$BEHIND_REPO"
)"

assert_contains "$OUTPUT" "AHEAD=0"
assert_contains "$OUTPUT" "BEHIND=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 3 - COMMITS LOCALES\n'
printf '========================================\n'

AHEAD_REPO="$TEST_DIR/ahead"
create_repo "$AHEAD_REPO"

cd "$AHEAD_REPO"

printf 'commit local\n' >> README.md
git add README.md
git commit -qm "commit local"

git fetch -q origin

OUTPUT="$(
    "$DEPLOY_SCRIPT" \
        --test-sincronizacion \
        --repo "$AHEAD_REPO"
)"

assert_contains "$OUTPUT" "AHEAD=1"
assert_contains "$OUTPUT" "BEHIND=0"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 4 - DIVERGENTE\n'
printf '========================================\n'

DIVERGED_REPO="$TEST_DIR/divergente"
create_repo "$DIVERGED_REPO"

cd "$DIVERGED_REPO"

BASE_COMMIT="$(git rev-parse HEAD)"

git clone -q "$DIVERGED_REPO/origin.git" "$TEST_DIR/remoto-divergente"

printf 'commit servidor\n' >> README.md
git add README.md
git commit -qm "commit servidor"

cd "$TEST_DIR/remoto-divergente"

printf 'commit remoto\n' >> README.md
git add README.md
git commit -qm "commit remoto"
git push -q origin main

cd "$DIVERGED_REPO"

git fetch -q origin

OUTPUT="$(
    "$DEPLOY_SCRIPT" \
        --test-sincronizacion \
        --repo "$DIVERGED_REPO"
)"

assert_contains "$OUTPUT" "AHEAD=1"
assert_contains "$OUTPUT" "BEHIND=1"

printf 'PASS\n'

printf '\n========================================\n'
printf 'TODAS LAS PRUEBAS DE ESTADOS PASARON\n'
printf '========================================\n'
