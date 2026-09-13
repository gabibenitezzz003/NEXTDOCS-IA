#!/usr/bin/env bash

set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_SCRIPT="$ROOT_DIR/deploy.sh"
TEST_DIR="$(mktemp -d /tmp/nextdocs-deploy-dirty-test.XXXXXX)"

cleanup() {
    rm -rf "$TEST_DIR"
}

trap cleanup EXIT

fail() {
    printf 'FAIL: %s\n' "$1" >&2
    exit 1
}

[ -x "$DEPLOY_SCRIPT" ] || fail "deploy.sh no existe o no es ejecutable"

cd "$TEST_DIR"

git init -q
git config user.name "NextDocs Test"
git config user.email "nextdocs-test@example.invalid"

printf 'base\n' > README.md

git add README.md
git commit -qm "base"

printf 'cambio sin commit\n' >> README.md

OUTPUT="$(
    "$DEPLOY_SCRIPT" \
        --test-arbol-limpio \
        --repo "$TEST_DIR" \
        2>&1
)" || true

printf '%s\n' "$OUTPUT" | grep -Fq "DIRTY=1" \
    || fail "deploy.sh no detectó el árbol sucio"

printf '\n========================================\n'
printf 'TEST DIRTY - ARBOL SUCIO\n'
printf '========================================\n'
printf 'PASS\n'
printf '\n========================================\n'
printf 'PRUEBA DIRTY PASO\n'
printf '========================================\n'
