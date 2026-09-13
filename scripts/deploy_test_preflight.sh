#!/usr/bin/env bash

set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_SCRIPT="$ROOT_DIR/deploy.sh"

fail=0

run_test() {
    local name="$1"
    shift

    printf '\n========================================\n'
    printf '%s\n' "$name"
    printf '========================================\n'

    if "$@"; then
        printf 'PASS\n'
    else
        printf 'FAIL\n'
        fail=1
    fi
}

test_preflight_exists() {
    grep -q -- '--preflight' "$DEPLOY_SCRIPT"
}

test_preflight_production_compose() {
    grep -q 'PRODUCTION_COMPOSE_FILE="/etc/nextdocs-ia/compose.produccion.yml"' "$DEPLOY_SCRIPT"
}

test_preflight_env_file() {
    grep -q 'PRODUCTION_ENV_FILE="/etc/nextdocs-ia/nextdocs.env"' "$DEPLOY_SCRIPT"
}

test_preflight_docker() {
    grep -q 'docker info' "$DEPLOY_SCRIPT"
}

test_preflight_compose_validation() {
    grep -q 'config -q' "$DEPLOY_SCRIPT"
}

test_preflight_container() {
    grep -q 'nextdocs-ia-app' "$DEPLOY_SCRIPT"
}

test_preflight_health() {
    grep -q 'health' "$DEPLOY_SCRIPT"
}

test_preflight_nginx() {
    grep -q 'nginx -t' "$DEPLOY_SCRIPT"
}

test_preflight_no_secret_output() {
    if grep -Eq 'cat[[:space:]]+.*nextdocs\.env|grep[[:space:]].*nextdocs\.env|echo[[:space:]].*NEXTDOCS_|printf[[:space:]].*NEXTDOCS_' "$DEPLOY_SCRIPT"; then
        return 1
    fi

    return 0
}

test_preflight_dry_run() {
    grep -q -- '--dry-run' "$DEPLOY_SCRIPT"
}

run_test "TEST 1 - PREFLIGHT IMPLEMENTADO" test_preflight_exists
run_test "TEST 2 - COMPOSE PRODUCCION" test_preflight_production_compose
run_test "TEST 3 - ENV PRODUCCION" test_preflight_env_file
run_test "TEST 4 - DOCKER DISPONIBLE" test_preflight_docker
run_test "TEST 5 - VALIDACION COMPOSE SIN SECRETOS" test_preflight_compose_validation
run_test "TEST 6 - CONTENEDOR PRODUCCION" test_preflight_container
run_test "TEST 7 - HEALTHCHECK" test_preflight_health
run_test "TEST 8 - NGINX" test_preflight_nginx
run_test "TEST 9 - NO EXPONE SECRETOS" test_preflight_no_secret_output
run_test "TEST 10 - DRY RUN" test_preflight_dry_run

printf '\n========================================\n'

if [ "$fail" -eq 0 ]; then
    printf 'TODAS LAS PRUEBAS DE PREFLIGHT PASARON\n'
    printf '========================================\n'
    exit 0
fi

printf 'HAY PRUEBAS DE PREFLIGHT FALLIDAS\n'
printf '========================================\n'
exit 1
