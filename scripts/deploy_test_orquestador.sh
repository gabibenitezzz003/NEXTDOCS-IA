#!/usr/bin/env bash

set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

printf '\n========================================\n'
printf 'TEST 1 - DRY RUN DEL ORQUESTADOR\n'
printf '========================================\n'

OUTPUT="$(./deploy.sh --dry-run 2>&1)"
STATUS=$?

if [ "$STATUS" -ne 0 ]; then
    printf '%s\n' "$OUTPUT"
    printf '%s\n' 'FAIL: deploy.sh --dry-run terminó con error.'
    exit 1
fi

if ! printf '%s\n' "$OUTPUT" | grep -q 'DEPLOY_DRY_RUN=1'; then
    printf '%s\n' "$OUTPUT"
    printf '%s\n' "FAIL: faltó 'DEPLOY_DRY_RUN=1'"
    exit 1
fi

if ! printf '%s\n' "$OUTPUT" | grep -q 'PLAN_ESTADO='; then
    printf '%s\n' "$OUTPUT"
    printf '%s\n' "FAIL: faltó 'PLAN_ESTADO='"
    exit 1
fi

if ! printf '%s\n' "$OUTPUT" | grep -q 'DEPLOY_FRONTEND='; then
    printf '%s\n' "$OUTPUT"
    printf '%s\n' "FAIL: faltó 'DEPLOY_FRONTEND='"
    exit 1
fi

if ! printf '%s\n' "$OUTPUT" | grep -q 'DEPLOY_BACKEND='; then
    printf '%s\n' "$OUTPUT"
    printf '%s\n' "FAIL: faltó 'DEPLOY_BACKEND='"
    exit 1
fi

if ! printf '%s\n' "$OUTPUT" | grep -q 'DEPLOY_DATABASE='; then
    printf '%s\n' "$OUTPUT"
    printf '%s\n' "FAIL: faltó 'DEPLOY_DATABASE='"
    exit 1
fi

if ! printf '%s\n' "$OUTPUT" | grep -q 'DEPLOY_COMPOSE='; then
    printf '%s\n' "$OUTPUT"
    printf '%s\n' "FAIL: faltó 'DEPLOY_COMPOSE='"
    exit 1
fi

if ! printf '%s\n' "$OUTPUT" | grep -q 'DEPLOY_NGINX='; then
    printf '%s\n' "$OUTPUT"
    printf '%s\n' "FAIL: faltó 'DEPLOY_NGINX='"
    exit 1
fi

printf 'PASS\n'

printf '\n========================================\n'
printf 'TODAS LAS PRUEBAS DEL ORQUESTADOR PASARON\n'
printf '========================================\n'
