#!/usr/bin/env bash

set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

printf '\n========================================\n'
printf 'TEST 1 - PREFLIGHT ANTES DEL PLAN\n'
printf '========================================\n'

PREFLIGHT_LINE="$(grep -n 'preflight_produccion' deploy.sh | head -n 1 | cut -d: -f1)"
PLAN_LINE="$(grep -n 'orquestador_dry_run' deploy.sh | head -n 1 | cut -d: -f1)"

if [ -z "$PREFLIGHT_LINE" ]; then
    printf '%s\n' 'FAIL: no se encontró preflight_produccion.'
    exit 1
fi

if [ -z "$PLAN_LINE" ]; then
    printf '%s\n' 'FAIL: no se encontró el orquestador.'
    exit 1
fi

if [ "$PREFLIGHT_LINE" -ge "$PLAN_LINE" ]; then
    printf 'preflight=%s plan=%s\n' "$PREFLIGHT_LINE" "$PLAN_LINE"
    printf '%s\n' 'FAIL: el preflight debe estar definido antes del orquestador.'
    exit 1
fi

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 2 - PREFLIGHT EXISTE EN EL FLUJO\n'
printf '========================================\n'

DISPATCHER="$(sed -n '/case "${1:-}" in/,$p' deploy.sh)"

if ! printf '%s\n' "$DISPATCHER" | grep -q 'preflight_produccion'; then
    printf '%s\n' 'FAIL: el dispatcher no contempla preflight.'
    exit 1
fi

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 3 - DRY RUN SIGUE FUNCIONANDO\n'
printf '========================================\n'

OUTPUT="$(./deploy.sh --dry-run 2>&1)"
STATUS=$?

if [ "$STATUS" -ne 0 ]; then
    printf '%s\n' "$OUTPUT"
    printf '%s\n' 'FAIL: --dry-run terminó con error.'
    exit 1
fi

if ! printf '%s\n' "$OUTPUT" | grep -q 'DEPLOY_DRY_RUN=1'; then
    printf '%s\n' "$OUTPUT"
    printf '%s\n' 'FAIL: falta DEPLOY_DRY_RUN=1.'
    exit 1
fi

printf 'PASS\n'

printf '\n========================================\n'
printf 'TODAS LAS PRUEBAS DE ORDEN PASARON\n'
printf '========================================\n'
