#!/usr/bin/env bash

set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

printf '\n========================================\n'
printf 'TEST 1 - DRY RUN NO EJECUTA OPERACIONES REALES\n'
printf '========================================\n'

if ! grep -q -- '--dry-run' deploy.sh; then
    printf '%s\n' 'FAIL: no existe --dry-run.'
    exit 1
fi

DRY_RUN_BLOCK="$(sed -n '/--dry-run)/,/;;/p' deploy.sh)"

if printf '%s\n' "$DRY_RUN_BLOCK" | grep -Eq 'docker|npm|nginx|deployed_commit'; then
    printf '%s\n' "$DRY_RUN_BLOCK"
    printf '%s\n' 'FAIL: el dispatcher de --dry-run contiene operaciones reales.'
    exit 1
fi

printf 'PASS\n'

printf '\n========================================\n'
printf 'TEST 2 - DRY RUN NO ESCRIBE ESTADO\n'
printf '========================================\n'

DRY_RUN_FUNCTION="$(sed -n '/orquestador_dry_run()/,/^}/p' deploy.sh)"

if printf '%s\n' "$DRY_RUN_FUNCTION" | grep -Eq '(^|[^a-zA-Z_])(>|\>\>|tee)[[:space:]]*.*deployed_commit'; then
    printf '%s\n' "$DRY_RUN_FUNCTION"
    printf '%s\n' 'FAIL: dry-run puede escribir deployed_commit.'
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
printf 'TODAS LAS PRUEBAS DE DRY RUN SEGURO PASARON\n'
printf '========================================\n'
