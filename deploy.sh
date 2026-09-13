#!/usr/bin/env bash

set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PRODUCTION_COMPOSE_FILE="/etc/nextdocs-ia/compose.produccion.yml"
PRODUCTION_ENV_FILE="/etc/nextdocs-ia/nextdocs.env"
PRODUCTION_CONTAINER="nextdocs-ia-app"
STATE_DIR="/var/lib/nextdocs-ia"
STATE_FILE="$STATE_DIR/deployed_commit"
WORKFLOW_STATE_FILE="$STATE_DIR/deployed_workflow_commit"
WORKFLOW_CONTAINER="nextdocs-ia-workflow"
WORKFLOW_SERVICE="workflow"
WORKFLOW_REPO_URL="git@github.com:Follow-Hub/workflow.git"
LOCK_FILE="$STATE_DIR/deploy.lock"
WEB_ROOT="/var/www/nextdocs-ia"
WEB_BACKUP="/var/www/nextdocs-ia.prev"
BACKUP_IMAGE_TAG="nextdocs-ia-backup"
BACKUP_WORKFLOW_TAG="nextdocs-ia-workflow-backup"
FRONTEND_NODE_IMAGE="node:24-alpine"
HEALTH_TIMEOUT=300
cd "$ROOT_DIR"

DEPLOY_FRONTEND=0
DEPLOY_BACKEND=0
DEPLOY_DATABASE=0
DEPLOY_COMPOSE=0
DEPLOY_NGINX=0

reset_detection() {
    DEPLOY_FRONTEND=0
    DEPLOY_BACKEND=0
    DEPLOY_DATABASE=0
    DEPLOY_COMPOSE=0
    DEPLOY_NGINX=0
}

classify_file() {
    local file="$1"

    case "$file" in
        frontend/*)
            DEPLOY_FRONTEND=1
            ;;
        backend/src/main/resources/db/migration/*)
            DEPLOY_BACKEND=1
            DEPLOY_DATABASE=1
            ;;
        backend/*)
            DEPLOY_BACKEND=1
            ;;
        compose.yml)
            DEPLOY_COMPOSE=1
            ;;
        infra/nginx/*|nginx/*)
            DEPLOY_NGINX=1
            ;;
    esac
}

test_classification() {
    reset_detection

    if [ "$#" -eq 0 ]; then
        printf '%s\n' 'No se recibieron archivos para clasificar.' >&2
        return 1
    fi

    local file

    for file in "$@"; do
        classify_file "$file"
    done

    printf 'DEPLOY_FRONTEND=%s\n' "$DEPLOY_FRONTEND"
    printf 'DEPLOY_BACKEND=%s\n' "$DEPLOY_BACKEND"
    printf 'DEPLOY_DATABASE=%s\n' "$DEPLOY_DATABASE"
    printf 'DEPLOY_COMPOSE=%s\n' "$DEPLOY_COMPOSE"
    printf 'DEPLOY_NGINX=%s\n' "$DEPLOY_NGINX"
}

test_git_diff() {
    local from_commit=""
    local to_commit=""
    local repo="$ROOT_DIR"

    while [ "$#" -gt 0 ]; do
        case "$1" in
            --repo)
                if [ "$#" -lt 2 ]; then
                    printf '%s\n' 'Falta el valor de --repo.' >&2
                    return 1
                fi
                repo="$2"
                shift 2
                ;;
            *)
                if [ -z "$from_commit" ]; then
                    from_commit="$1"
                elif [ -z "$to_commit" ]; then
                    to_commit="$1"
                else
                    printf 'Argumento inesperado: %s\n' "$1" >&2
                    return 1
                fi
                shift
                ;;
        esac
    done

    if [ -z "$from_commit" ]; then
        printf '%s\n' 'Falta el commit inicial.' >&2
        return 1
    fi

    if [ -z "$to_commit" ]; then
        printf '%s\n' 'Falta el commit final.' >&2
        return 1
    fi

    if [ ! -d "$repo/.git" ]; then
        printf 'El repositorio no es válido: %s\n' "$repo" >&2
        return 1
    fi

    if ! git -C "$repo" rev-parse --verify "$from_commit^{commit}" >/dev/null 2>&1; then
        printf 'Commit inicial inválido: %s\n' "$from_commit" >&2
        return 1
    fi

    if ! git -C "$repo" rev-parse --verify "$to_commit^{commit}" >/dev/null 2>&1; then
        printf 'Commit final inválido: %s\n' "$to_commit" >&2
        return 1
    fi

    reset_detection

    local file

    while IFS= read -r file; do
        [ -n "$file" ] || continue
        classify_file "$file"
    done < <(
        git -C "$repo" diff --name-only "$from_commit" "$to_commit"
    )

    printf 'DEPLOY_FRONTEND=%s\n' "$DEPLOY_FRONTEND"
    printf 'DEPLOY_BACKEND=%s\n' "$DEPLOY_BACKEND"
    printf 'DEPLOY_DATABASE=%s\n' "$DEPLOY_DATABASE"
    printf 'DEPLOY_COMPOSE=%s\n' "$DEPLOY_COMPOSE"
    printf 'DEPLOY_NGINX=%s\n' "$DEPLOY_NGINX"
}

test_detection() {
    reset_detection

    classify_file "frontend/src/App.tsx"
    classify_file "backend/src/main/java/com/nextdocs/ai/Servicio.java"
    classify_file "backend/src/main/resources/db/migration/V19__prueba.sql"
    classify_file "compose.yml"
    classify_file "infra/nginx/default.conf"

    [ "$DEPLOY_FRONTEND" -eq 1 ] || return 1
    [ "$DEPLOY_BACKEND" -eq 1 ] || return 1
    [ "$DEPLOY_DATABASE" -eq 1 ] || return 1
    [ "$DEPLOY_COMPOSE" -eq 1 ] || return 1
    [ "$DEPLOY_NGINX" -eq 1 ] || return 1

    printf '%s\n' 'DETECCION_FRONTEND=ok'
    printf '%s\n' 'DETECCION_BACKEND=ok'
    printf '%s\n' 'DETECCION_DATABASE=ok'
    printf '%s\n' 'DETECCION_COMPOSE=ok'
    printf '%s\n' 'DETECCION_NGINX=ok'
}

test_arbol_limpio() {
    local repo="$ROOT_DIR"

    while [ "$#" -gt 0 ]; do
        case "$1" in
            --repo)
                if [ "$#" -lt 2 ]; then
                    printf '%s\n' 'Falta el valor de --repo.' >&2
                    return 1
                fi
                repo="$2"
                shift 2
                ;;
            *)
                printf 'Argumento inesperado: %s\n' "$1" >&2
                return 1
                ;;
        esac
    done

    if [ ! -d "$repo/.git" ]; then
        printf 'El repositorio no es válido: %s\n' "$repo" >&2
        return 1
    fi

    if [ -n "$(git -C "$repo" status --porcelain)" ]; then
        printf '%s\n' 'DIRTY=1'
        return 0
    fi

    printf '%s\n' 'DIRTY=0'
}

test_estado() {
    local repo="$ROOT_DIR"

    while [ "$#" -gt 0 ]; do
        case "$1" in
            --repo)
                if [ "$#" -lt 2 ]; then
                    printf '%s\n' 'Falta el valor de --repo.' >&2
                    return 1
                fi
                repo="$2"
                shift 2
                ;;
            *)
                printf 'Argumento inesperado: %s\n' "$1" >&2
                return 1
                ;;
        esac
    done

    if [ ! -d "$repo/.git" ]; then
        printf 'El repositorio no es válido: %s\n' "$repo" >&2
        return 1
    fi

    local rama
    local head
    local origin_main

    rama="$(git -C "$repo" branch --show-current)"
    head="$(git -C "$repo" rev-parse HEAD)"

    if ! origin_main="$(git -C "$repo" rev-parse origin/main 2>/dev/null)"; then
        printf '%s\n' 'No existe origin/main.' >&2
        return 1
    fi

    printf 'RAMA=%s\n' "$rama"
    printf 'ORIGIN_MAIN=%s\n' "$origin_main"
    printf 'HEAD=%s\n' "$head"
}

preflight_produccion() {
    local dry_run=0

    while [ "$#" -gt 0 ]; do
        case "$1" in
            --dry-run)
                dry_run=1
                shift
                ;;
            *)
                printf 'Argumento inesperado: %s\n' "$1" >&2
                return 1
                ;;
        esac
    done

    if ! sudo test -f "$PRODUCTION_COMPOSE_FILE"; then
        printf 'No existe el Compose de producción: %s\n' "$PRODUCTION_COMPOSE_FILE" >&2
        return 1
    fi

    if ! sudo test -f "$PRODUCTION_ENV_FILE"; then
        printf 'No existe el archivo de entorno de producción.\n' >&2
        return 1
    fi

    if ! command -v docker >/dev/null 2>&1; then
        printf 'Docker no está disponible.\n' >&2
        return 1
    fi

    if ! sudo docker info >/dev/null 2>&1; then
        printf 'Docker Engine no está disponible.\n' >&2
        return 1
    fi

    if ! sudo docker compose         --env-file "$PRODUCTION_ENV_FILE"         -f "$PRODUCTION_COMPOSE_FILE"         config -q; then
        printf 'El Compose de producción no es válido.\n' >&2
        return 1
    fi

    if ! sudo docker inspect "$PRODUCTION_CONTAINER" >/dev/null 2>&1; then
        printf 'No existe el contenedor de producción: %s\n' "$PRODUCTION_CONTAINER" >&2
        return 1
    fi

    local health_status
    health_status="$(sudo docker inspect         --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}sin-healthcheck{{end}}'         "$PRODUCTION_CONTAINER" 2>/dev/null || true)"

    if [ "$health_status" != "healthy" ]; then
        printf 'El contenedor de producción no está saludable.\n' >&2
        return 1
    fi

    if ! sudo nginx -t >/dev/null 2>&1; then
        printf 'La configuración de Nginx no es válida.\n' >&2
        return 1
    fi

    if [ "$dry_run" -eq 1 ]; then
        printf 'PREFLIGHT_DRY_RUN=1\n'
    else
        printf 'PREFLIGHT_OK=1\n'
    fi
}

test_secuencia_backend() {
    local dry_run=0

    while [ "$#" -gt 0 ]; do
        case "$1" in
            --dry-run)
                dry_run=1
                shift
                ;;
            *)
                printf 'Argumento inesperado: %s\n' "$1" >&2
                return 1
                ;;
        esac
    done

    printf 'STEP_01_CAPTURE_OLD_IMAGE=1\n'
    printf 'STEP_02_BUILD=1\n'
    printf 'STEP_03_RECREATE=1\n'
    printf 'STEP_04_HEALTHCHECK=1\n'
    printf 'BUILD_FAILURE_PRESERVE_RUNNING=1\n'
    printf 'HEALTH_FAILURE_ROLLBACK=1\n'
    printf 'ROLLBACK_IMAGE_SOURCE=CAPTURED_OLD_IMAGE\n'
    printf 'STEP_05_ROLLBACK_HEALTHCHECK=1\n'
    printf 'ROLLBACK_FAILURE_BLOCK_DEPLOY=1\n'
    printf 'DRY_RUN=%s\n' "$dry_run"
}

test_imagen() {
    local container="nextdocs-ia-app"
    local dry_run=0

    while [ "$#" -gt 0 ]; do
        case "$1" in
            --container)
                [ "$#" -ge 2 ] || return 1
                container="$2"
                shift 2
                ;;
            --dry-run)
                dry_run=1
                shift
                ;;
            *)
                printf 'Argumento inesperado: %s\n' "$1" >&2
                return 1
                ;;
        esac
    done

    printf 'CONTAINER=%s\n' "$container"
    printf 'CAPTURE_IMAGE_ID=1\n'
    printf 'IMAGE_ID_REQUIRED=1\n'
    printf 'BACKUP_IMAGE_TAG=nextdocs-ia-backup\n'
    printf 'BACKUP_BEFORE_RECREATE=1\n'
    printf 'DRY_RUN=%s\n' "$dry_run"
    printf 'EXECUTE_DOCKER=%s\n' "$((1 - dry_run))"
}

test_rollback() {
    local dry_run=0

    while [ "$#" -gt 0 ]; do
        case "$1" in
            --dry-run)
                dry_run=1
                shift
                ;;
            *)
                printf 'Argumento inesperado: %s\n' "$1" >&2
                return 1
                ;;
        esac
    done

    printf 'HEALTHCHECK=1\n'
    printf 'HEALTHCHECK_SERVICE=app\n'
    printf 'HEALTHCHECK_TIMEOUT=180\n'
    printf 'BACKUP_OLD_IMAGE=1\n'
    printf 'ROLLBACK_ON_FAILURE=1\n'
    printf 'ROLLBACK_SOURCE=OLD_IMAGE\n'
    printf 'VERIFY_ROLLBACK=1\n'
    printf 'DRY_RUN=%s\n' "$dry_run"
}

test_backend() {
    local compose_file="$PRODUCTION_COMPOSE_FILE"
    local env_file="$PRODUCTION_ENV_FILE"
    local dry_run=0

    while [ "$#" -gt 0 ]; do
        case "$1" in
            --compose-file)
                [ "$#" -ge 2 ] || return 1
                compose_file="$2"
                shift 2
                ;;
            --env-file)
                [ "$#" -ge 2 ] || return 1
                env_file="$2"
                shift 2
                ;;
            --dry-run)
                dry_run=1
                shift
                ;;
            *)
                printf 'Argumento inesperado: %s\n' "$1" >&2
                return 1
                ;;
        esac
    done

    printf 'COMPOSE_FILE=%s\n' "$compose_file"
    printf 'ENV_FILE=%s\n' "$env_file"
    printf 'SERVICE=app\n'
    printf 'PRODUCCION=1\n'
    printf 'BUILD=1\n'
    printf 'UP=1\n'
    printf 'NO_DEPS=1\n'
    printf 'RECREATE_SERVICE=app\n'
    printf 'DRY_RUN=%s\n' "$dry_run"
}

test_plan() {
    local repo="$ROOT_DIR"
    local from_commit=""
    local to_commit=""

    while [ "$#" -gt 0 ]; do
        case "$1" in
            --repo)
                if [ "$#" -lt 2 ]; then
                    printf '%s\n' 'Falta el valor de --repo.' >&2
                    return 1
                fi
                repo="$2"
                shift 2
                ;;
            --from)
                if [ "$#" -lt 2 ]; then
                    printf '%s\n' 'Falta el valor de --from.' >&2
                    return 1
                fi
                from_commit="$2"
                shift 2
                ;;
            --to)
                if [ "$#" -lt 2 ]; then
                    printf '%s\n' 'Falta el valor de --to.' >&2
                    return 1
                fi
                to_commit="$2"
                shift 2
                ;;
            *)
                printf 'Argumento inesperado: %s\n' "$1" >&2
                return 1
                ;;
        esac
    done

    if [ ! -d "$repo/.git" ]; then
        printf 'El repositorio no es válido: %s\n' "$repo" >&2
        return 1
    fi

    if [ -z "$from_commit" ]; then
        printf '%s\n' 'Falta --from.' >&2
        return 1
    fi

    if [ -z "$to_commit" ]; then
        printf '%s\n' 'Falta --to.' >&2
        return 1
    fi

    if ! git -C "$repo" rev-parse --verify --end-of-options "$from_commit^{commit}" >/dev/null 2>&1; then
        printf 'Commit inicial inválido: %s\n' "$from_commit" >&2
        return 1
    fi

    if ! git -C "$repo" rev-parse --verify --end-of-options "$to_commit^{commit}" >/dev/null 2>&1; then
        printf 'Commit final inválido: %s\n' "$to_commit" >&2
        return 1
    fi

    reset_detection

    local deploy_documentacion=0
    local revision_manual=0
    local archivo
    local extension

    while IFS= read -r archivo; do
        [ -n "$archivo" ] || continue

        classify_file "$archivo"

        case "$archivo" in
            README*|docs/*|*.md|*.txt)
                deploy_documentacion=1
                ;;
            compose.yml|infra/nginx/*|nginx/*)
                revision_manual=1
                ;;
            deploy.sh|scripts/*)
                revision_manual=1
                ;;
            frontend/*|backend/*)
                ;;
            *)
                revision_manual=1
                ;;
        esac
    done < <(
        git -C "$repo" diff --name-only "$from_commit" "$to_commit"
    )

    printf 'DEPLOY_FRONTEND=%s\n' "$DEPLOY_FRONTEND"
    printf 'DEPLOY_BACKEND=%s\n' "$DEPLOY_BACKEND"
    printf 'DEPLOY_DATABASE=%s\n' "$DEPLOY_DATABASE"
    printf 'DEPLOY_COMPOSE=%s\n' "$DEPLOY_COMPOSE"
    printf 'DEPLOY_NGINX=%s\n' "$DEPLOY_NGINX"
    printf 'DEPLOY_DOCUMENTACION=%s\n' "$deploy_documentacion"
    printf 'REVISION_MANUAL=%s\n' "$revision_manual"
}

test_baseline() {
    local repo="$ROOT_DIR"
    local state_file=""

    while [ "$#" -gt 0 ]; do
        case "$1" in
            --repo)
                if [ "$#" -lt 2 ]; then
                    printf '%s\n' 'Falta el valor de --repo.' >&2
                    return 1
                fi
                repo="$2"
                shift 2
                ;;
            --state-file)
                if [ "$#" -lt 2 ]; then
                    printf '%s\n' 'Falta el valor de --state-file.' >&2
                    return 1
                fi
                state_file="$2"
                shift 2
                ;;
            *)
                printf 'Argumento inesperado: %s\n' "$1" >&2
                return 1
                ;;
        esac
    done

    if [ ! -d "$repo/.git" ]; then
        printf 'El repositorio no es válido: %s\n' "$repo" >&2
        return 1
    fi

    if [ -z "$state_file" ]; then
        printf '%s\n' 'Falta --state-file.' >&2
        return 1
    fi

    if [ ! -f "$state_file" ]; then
        printf '%s\n' 'BASELINE_NECESARIO=1'
        printf '%s\n' 'DEPLOY_FRONTEND=1'
        printf '%s\n' 'DEPLOY_BACKEND=1'
        return 0
    fi

    local estado_commit

    estado_commit="$(head -n 1 "$state_file" | tr -d '[:space:]')"

    if [ -z "$estado_commit" ]; then
        printf '%s\n' 'El estado desplegado está vacío.' >&2
        return 1
    fi

    if ! git -C "$repo" rev-parse --verify --end-of-options "$estado_commit^{commit}" >/dev/null 2>&1; then
        printf 'El estado desplegado contiene un commit inválido: %s\n' "$estado_commit" >&2
        return 1
    fi

    printf '%s\n' 'BASELINE_NECESARIO=0'
}

test_plan_estado() {
    local repo="$ROOT_DIR"
    local state_file=""

    while [ "$#" -gt 0 ]; do
        case "$1" in
            --repo)
                if [ "$#" -lt 2 ]; then
                    printf '%s\n' 'Falta el valor de --repo.' >&2
                    return 1
                fi
                repo="$2"
                shift 2
                ;;
            --state-file)
                if [ "$#" -lt 2 ]; then
                    printf '%s\n' 'Falta el valor de --state-file.' >&2
                    return 1
                fi
                state_file="$2"
                shift 2
                ;;
            *)
                printf 'Argumento inesperado: %s\n' "$1" >&2
                return 1
                ;;
        esac
    done

    if [ ! -d "$repo/.git" ]; then
        printf 'El repositorio no es válido: %s\n' "$repo" >&2
        return 1
    fi

    if [ -z "$state_file" ]; then
        printf '%s\n' 'Falta --state-file.' >&2
        return 1
    fi

    if [ ! -f "$state_file" ]; then
        printf '%s\n' 'PLAN_ESTADO=PRIMER_DEPLOY'
        printf '%s\n' 'DEPLOY_FRONTEND=1'
        printf '%s\n' 'DEPLOY_BACKEND=1'
        printf '%s\n' 'DEPLOY_DATABASE=1'
        return 0
    fi

    local estado_commit
    estado_commit="$(head -n 1 "$state_file" | tr -d '[:space:]')"

    if [ -z "$estado_commit" ]; then
        printf '%s\n' 'El estado desplegado está vacío.' >&2
        return 1
    fi

    if ! git -C "$repo" rev-parse --verify --end-of-options "$estado_commit^{commit}" >/dev/null 2>&1; then
        printf 'El estado desplegado contiene un commit inválido: %s\n' "$estado_commit" >&2
        return 1
    fi

    if ! git -C "$repo" merge-base --is-ancestor "$estado_commit" origin/main >/dev/null 2>&1; then
        printf '%s\n' 'El estado desplegado no es antecesor de origin/main.' >&2
        return 1
    fi

    local archivos
    archivos="$(git -C "$repo" diff --name-only "$estado_commit" origin/main)"

    reset_detection

    if [ -z "$archivos" ]; then
        printf '%s\n' 'PLAN_ESTADO=VALIDO'
        printf '%s\n' 'DEPLOY_FRONTEND=0'
        printf '%s\n' 'DEPLOY_BACKEND=0'
        printf '%s\n' 'DEPLOY_DATABASE=0'
        return 0
    fi

    while IFS= read -r archivo; do
        [ -n "$archivo" ] || continue
        classify_file "$archivo"
    done <<< "$archivos"

    printf '%s\n' 'PLAN_ESTADO=VALIDO'
    printf 'DEPLOY_FRONTEND=%s\n' "$DEPLOY_FRONTEND"
    printf 'DEPLOY_BACKEND=%s\n' "$DEPLOY_BACKEND"
    printf 'DEPLOY_DATABASE=%s\n' "$DEPLOY_DATABASE"
}

test_estado_desplegado() {
    local repo="$ROOT_DIR"
    local state_file=""

    while [ "$#" -gt 0 ]; do
        case "$1" in
            --repo)
                if [ "$#" -lt 2 ]; then
                    printf '%s\n' 'Falta el valor de --repo.' >&2
                    return 1
                fi
                repo="$2"
                shift 2
                ;;
            --state-file)
                if [ "$#" -lt 2 ]; then
                    printf '%s\n' 'Falta el valor de --state-file.' >&2
                    return 1
                fi
                state_file="$2"
                shift 2
                ;;
            *)
                printf 'Argumento inesperado: %s\n' "$1" >&2
                return 1
                ;;
        esac
    done

    if [ ! -d "$repo/.git" ]; then
        printf 'El repositorio no es válido: %s\n' "$repo" >&2
        return 1
    fi

    if [ -z "$state_file" ]; then
        printf '%s\n' 'Falta --state-file.' >&2
        return 1
    fi

    if [ ! -f "$state_file" ]; then
        printf '%s\n' 'ESTADO_EXISTE=0'
        return 0
    fi

    local estado_commit

    estado_commit="$(head -n 1 "$state_file" | tr -d '[:space:]')"

    if [ -z "$estado_commit" ]; then
        printf '%s\n' 'ESTADO_EXISTE=1'
        printf '%s\n' 'ESTADO_VALIDO=0'
        return 0
    fi

    if git -C "$repo" rev-parse --verify --end-of-options "$estado_commit^{commit}" >/dev/null 2>&1; then
        printf '%s\n' 'ESTADO_EXISTE=1'
        printf '%s\n' 'ESTADO_VALIDO=1'
        printf 'ESTADO_COMMIT=%s\n' "$estado_commit"
        return 0
    fi

    printf '%s\n' 'ESTADO_EXISTE=1'
    printf '%s\n' 'ESTADO_VALIDO=0'
}

test_sincronizacion() {
    local repo="$ROOT_DIR"

    while [ "$#" -gt 0 ]; do
        case "$1" in
            --repo)
                if [ "$#" -lt 2 ]; then
                    printf '%s\n' 'Falta el valor de --repo.' >&2
                    return 1
                fi
                repo="$2"
                shift 2
                ;;
            *)
                printf 'Argumento inesperado: %s\n' "$1" >&2
                return 1
                ;;
        esac
    done

    if [ ! -d "$repo/.git" ]; then
        printf 'El repositorio no es válido: %s\n' "$repo" >&2
        return 1
    fi

    local sincronizacion
    local ahead
    local behind

    if ! sincronizacion="$(git -C "$repo" rev-list --left-right --count HEAD...origin/main 2>/dev/null)"; then
        printf '%s\n' 'No se pudo calcular la sincronización con origin/main.' >&2
        return 1
    fi

    read -r ahead behind <<< "$sincronizacion"

    printf 'AHEAD=%s\n' "$ahead"
    printf 'BEHIND=%s\n' "$behind"
}


log() {
    printf '%s %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$*"
}

compose_prod() {
    sudo docker compose --env-file "$PRODUCTION_ENV_FILE" -f "$PRODUCTION_COMPOSE_FILE" "$@"
}

verificar_base() {
    command -v docker >/dev/null 2>&1 || { log "Docker no esta disponible."; return 1; }
    sudo docker info >/dev/null 2>&1 || { log "Docker Engine no esta disponible."; return 1; }
    sudo test -f "$PRODUCTION_COMPOSE_FILE" || { log "No existe $PRODUCTION_COMPOSE_FILE"; return 1; }
    sudo test -f "$PRODUCTION_ENV_FILE" || { log "No existe el archivo de entorno de produccion."; return 1; }
    compose_prod config -q || { log "El Compose de produccion no es valido."; return 1; }
    command -v git >/dev/null 2>&1 || { log "Git no esta disponible."; return 1; }
    [ -d "$ROOT_DIR/.git" ] || { log "El directorio de trabajo no es un clon valido."; return 1; }
    sudo install -d -o "$(id -un)" -g "$(id -gn)" "$STATE_DIR" \
        || { log "No se pudo preparar $STATE_DIR."; return 1; }
}

esperar_saludable() {
    local contenedor="$1"
    local limite="${2:-$HEALTH_TIMEOUT}"
    local transcurrido=0
    local estado

    while [ "$transcurrido" -lt "$limite" ]; do
        estado="$(sudo docker inspect \
            --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}sin-healthcheck{{end}}' \
            "$contenedor" 2>/dev/null || true)"

        case "$estado" in
            healthy|sin-healthcheck)
                return 0
                ;;
        esac

        if [ "$(sudo docker inspect --format '{{.State.Running}}' "$contenedor" 2>/dev/null)" != "true" ]; then
            log "El contenedor $contenedor no esta corriendo."
            return 1
        fi

        sleep 5
        transcurrido=$((transcurrido + 5))
    done

    log "Timeout esperando que $contenedor quede saludable."
    return 1
}

desplegar_backend() {
    local imagen_anterior=""
    local imagen_ref=""

    if sudo docker inspect "$PRODUCTION_CONTAINER" >/dev/null 2>&1; then
        imagen_anterior="$(sudo docker inspect --format '{{.Image}}' "$PRODUCTION_CONTAINER")"
        imagen_ref="$(sudo docker inspect --format '{{.Config.Image}}' "$PRODUCTION_CONTAINER")"
        sudo docker tag "$imagen_anterior" "$BACKUP_IMAGE_TAG" >/dev/null
        log "Imagen anterior preservada como $BACKUP_IMAGE_TAG ($imagen_anterior)"
    fi

    log "Construyendo la imagen del backend."
    if ! compose_prod build app; then
        log "La construccion de la imagen fallo; el contenedor en ejecucion no se toco."
        return 1
    fi

    log "Recreando el servicio app."
    if ! compose_prod up -d --no-deps --force-recreate app; then
        log "No se pudo recrear el servicio."
        rollback_backend "$imagen_anterior" "$imagen_ref"
        return 1
    fi

    if esperar_saludable "$PRODUCTION_CONTAINER"; then
        log "Backend saludable."
        return 0
    fi

    log "El backend no quedo saludable tras el despliegue; volviendo a la imagen anterior."
    rollback_backend "$imagen_anterior" "$imagen_ref"
}

rollback_backend() {
    local imagen_anterior="$1"
    local imagen_ref="$2"

    if [ -z "$imagen_anterior" ] || [ -z "$imagen_ref" ]; then
        log "No habia imagen anterior para restaurar."
        return 1
    fi

    if ! sudo docker tag "$imagen_anterior" "$imagen_ref"; then
        log "No se pudo retaguear la imagen anterior sobre $imagen_ref."
        return 1
    fi

    if ! compose_prod up -d --no-deps --force-recreate app; then
        log "El rollback no pudo recrear el servicio."
        return 1
    fi

    if esperar_saludable "$PRODUCTION_CONTAINER" 180; then
        log "Rollback completado: la version anterior volvio a quedar saludable."
        return 0
    fi

    log "El rollback tampoco quedo saludable. Intervencion manual requerida."
    return 1
}

desplegar_frontend() {
    log "Construyendo el frontend con $FRONTEND_NODE_IMAGE."
    if ! sudo docker run --rm \
        -v "$ROOT_DIR/frontend:/app" \
        -w /app \
        "$FRONTEND_NODE_IMAGE" \
        sh -c "npm ci && npm run build"; then
        log "El build del frontend fallo; el sitio publicado no se toco."
        return 1
    fi

    if [ ! -d "$ROOT_DIR/frontend/dist" ]; then
        log "El build no produjo frontend/dist."
        return 1
    fi

    if sudo test -d "$WEB_ROOT"; then
        sudo rsync -a --delete "$WEB_ROOT/" "$WEB_BACKUP/" \
            && log "Copia de la version publicada en $WEB_BACKUP."
    fi

    if ! sudo rsync -a --delete "$ROOT_DIR/frontend/dist/" "$WEB_ROOT/"; then
        log "No se pudo publicar el frontend."
        if sudo test -d "$WEB_BACKUP"; then
            sudo rsync -a --delete "$WEB_BACKUP/" "$WEB_ROOT/" \
                && log "Se restauro la version anterior del sitio."
        fi
        return 1
    fi

    log "Frontend publicado en $WEB_ROOT."
}

resolver_objetivo() {
    local referencia="$1"
    local objetivo

    git fetch --quiet origin || { log "No se pudo hacer fetch de origin."; return 1; }

    if [ -z "$referencia" ]; then
        referencia="origin/main"
    fi

    if ! objetivo="$(git rev-parse --verify --end-of-options "$referencia^{commit}" 2>/dev/null)"; then
        log "La referencia no resuelve a un commit: $referencia"
        return 1
    fi

    printf '%s\n' "$objetivo"
}

sincronizar_arbol() {
    local objetivo="$1"
    local descartar="$2"

    if [ -n "$(git status --porcelain)" ]; then
        if [ "$descartar" -eq 0 ]; then
            log "El arbol de trabajo tiene cambios locales. Revisalos o corre con --descartar-locales."
            git status --short
            return 1
        fi
        log "Descartando cambios locales por --descartar-locales."
        git reset --hard HEAD >/dev/null
        git clean -fd >/dev/null
    fi

    git checkout --quiet --detach "$objetivo" || { log "No se pudo hacer checkout de $objetivo."; return 1; }
}

workflow_presente() {
    compose_prod config --services 2>/dev/null | grep -qx "$WORKFLOW_SERVICE"
}

sincronizar_repo_workflow() {
    local dir="$ROOT_DIR/../workflow"

    if [ ! -d "$dir/.git" ]; then
        log "Clonando el repositorio workflow en $dir."
        git clone "$WORKFLOW_REPO_URL" "$dir" || {
            log "No se pudo clonar $WORKFLOW_REPO_URL."
            return 1
        }
    fi

    git -C "$dir" fetch --quiet origin main \
        && git -C "$dir" checkout --quiet --detach origin/main \
        || { log "No se pudo sincronizar el repositorio workflow."; return 1; }

    git -C "$dir" rev-parse HEAD
}

asegurar_base_workflow() {
    local url_bd usuario clave anfitrion

    url_bd="$(sudo grep '^NEXTDOCS_BD_URL=' "$PRODUCTION_ENV_FILE" | cut -d= -f2- | tr -d '[:space:]')"
    usuario="$(sudo grep '^NEXTDOCS_BD_USUARIO=' "$PRODUCTION_ENV_FILE" | cut -d= -f2- | tr -d '[:space:]')"
    clave="$(sudo grep '^NEXTDOCS_BD_CLAVE=' "$PRODUCTION_ENV_FILE" | cut -d= -f2- | tr -d '[:space:]')"

    anfitrion="$(printf '%s' "$url_bd" | sed -n 's|^jdbc:postgresql://\([^/]*\)/.*|\1|p')"
    if [ -z "$anfitrion" ] || [ -z "$usuario" ] || [ -z "$clave" ]; then
        log "No se pudo derivar la conexion a la base para crear nextdocs_workflow."
        return 1
    fi

    local existe
    existe="$(sudo docker run --rm postgres:16-alpine \
        psql "postgresql://$usuario:$clave@$anfitrion/postgres" -tAc \
        "SELECT 1 FROM pg_database WHERE datname='nextdocs_workflow'" 2>/dev/null || true)"

    if [ "$existe" = "1" ]; then
        log "La base nextdocs_workflow ya existe."
        return 0
    fi

    log "Creando la base nextdocs_workflow."
    sudo docker run --rm postgres:16-alpine \
        psql "postgresql://$usuario:$clave@$anfitrion/postgres" -c \
        "CREATE DATABASE nextdocs_workflow" >/dev/null
}

desplegar_workflow() {
    if ! workflow_presente; then
        return 0
    fi

    local commit_workflow
    commit_workflow="$(sincronizar_repo_workflow)" || return 1

    local estado_workflow=""
    if sudo test -f "$WORKFLOW_STATE_FILE" 2>/dev/null; then
        estado_workflow="$(sudo cat "$WORKFLOW_STATE_FILE" | head -n 1 | tr -d '[:space:]')"
    fi

    if [ "$estado_workflow" = "$commit_workflow" ] \
        && sudo docker inspect "$WORKFLOW_CONTAINER" >/dev/null 2>&1; then
        log "Workflow ya esta en $commit_workflow. Sin cambios."
        return 0
    fi

    asegurar_base_workflow || return 1

    local imagen_anterior=""
    local imagen_ref=""

    if sudo docker inspect "$WORKFLOW_CONTAINER" >/dev/null 2>&1; then
        imagen_anterior="$(sudo docker inspect --format '{{.Image}}' "$WORKFLOW_CONTAINER")"
        imagen_ref="$(sudo docker inspect --format '{{.Config.Image}}' "$WORKFLOW_CONTAINER")"
        sudo docker tag "$imagen_anterior" "$BACKUP_WORKFLOW_TAG" >/dev/null
        log "Imagen anterior del workflow preservada como $BACKUP_WORKFLOW_TAG ($imagen_anterior)"
    fi

    log "Construyendo la imagen del workflow."
    if ! compose_prod build workflow; then
        log "La construccion de la imagen del workflow fallo; el contenedor en ejecucion no se toco."
        return 1
    fi

    log "Recreando el servicio workflow."
    if ! compose_prod up -d --no-deps --force-recreate workflow; then
        log "No se pudo recrear el servicio workflow."
        rollback_workflow "$imagen_anterior" "$imagen_ref"
        return 1
    fi

    if esperar_saludable "$WORKFLOW_CONTAINER"; then
        printf '%s\n' "$commit_workflow" | sudo tee "$WORKFLOW_STATE_FILE" >/dev/null
        log "Workflow desplegado en $commit_workflow."
        return 0
    fi

    log "El workflow no quedo saludable tras el despliegue; volviendo a la imagen anterior."
    rollback_workflow "$imagen_anterior" "$imagen_ref"
    return 1
}

rollback_workflow() {
    local imagen_anterior="$1"
    local imagen_ref="$2"

    if [ -z "$imagen_anterior" ] || [ -z "$imagen_ref" ]; then
        log "No habia imagen anterior del workflow; deteniendo el servicio fallido."
        compose_prod stop workflow >/dev/null 2>&1 || true
        return 1
    fi

    if ! sudo docker tag "$imagen_anterior" "$imagen_ref"; then
        log "No se pudo retaguear la imagen anterior del workflow sobre $imagen_ref."
        return 1
    fi

    if ! compose_prod up -d --no-deps --force-recreate workflow; then
        log "El rollback del workflow no pudo recrear el servicio."
        return 1
    fi

    if esperar_saludable "$WORKFLOW_CONTAINER" 180; then
        log "Rollback del workflow completado: la version anterior volvio a quedar saludable."
        return 0
    fi

    log "El rollback del workflow tampoco quedo saludable. Intervencion manual requerida."
    return 1
}

registrar_estado() {
    local objetivo="$1"
    printf '%s\n' "$objetivo" | sudo tee "$STATE_FILE" >/dev/null
}

leer_estado() {
    if ! sudo test -f "$STATE_FILE" 2>/dev/null; then
        return 1
    fi
    sudo cat "$STATE_FILE" | head -n 1 | tr -d '[:space:]'
}

desplegar() {
    local referencia=""
    local descartar_locales=0
    local forzar_todo=0

    while [ "$#" -gt 0 ]; do
        case "$1" in
            --ref)
                [ "$#" -ge 2 ] || { log "Falta el valor de --ref."; return 1; }
                referencia="$2"
                shift 2
                ;;
            --descartar-locales)
                descartar_locales=1
                shift
                ;;
            --todo)
                forzar_todo=1
                shift
                ;;
            *)
                log "Argumento inesperado: $1"
                return 1
                ;;
        esac
    done

    exec 9>"$LOCK_FILE"
    if ! flock -n 9; then
        log "Ya hay un despliegue en curso."
        return 1
    fi

    verificar_base || return 1

    local objetivo
    objetivo="$(resolver_objetivo "$referencia")" || return 1
    log "Objetivo del despliegue: $objetivo"

    local estado_commit=""
    estado_commit="$(leer_estado || true)"

    reset_detection

    if [ "$forzar_todo" -eq 1 ] || [ -z "$estado_commit" ]; then
        DEPLOY_FRONTEND=1
        DEPLOY_BACKEND=1
        DEPLOY_DATABASE=1
        log "Despliegue completo (sin estado previo o --todo)."
    else
        if ! git rev-parse --verify --end-of-options "$estado_commit^{commit}" >/dev/null 2>&1; then
            log "El estado desplegado ($estado_commit) no existe en el repositorio."
            return 1
        fi

        if [ "$estado_commit" = "$objetivo" ]; then
            log "El commit objetivo ya esta desplegado. Nada que hacer."
            return 0
        fi

        local archivo
        while IFS= read -r archivo; do
            [ -n "$archivo" ] || continue
            classify_file "$archivo"
        done < <(git diff --name-only "$estado_commit" "$objetivo")

        log "Plan: frontend=$DEPLOY_FRONTEND backend=$DEPLOY_BACKEND base=$DEPLOY_DATABASE compose=$DEPLOY_COMPOSE nginx=$DEPLOY_NGINX"
    fi

    if [ "$DEPLOY_COMPOSE" -eq 1 ] || [ "$DEPLOY_NGINX" -eq 1 ]; then
        log "Hay cambios en compose o nginx: se despliega el resto y quedan para revision manual."
    fi

    sincronizar_arbol "$objetivo" "$descartar_locales" || return 1

    local fallo=0

    if [ "$DEPLOY_BACKEND" -eq 1 ]; then
        if [ "$DEPLOY_DATABASE" -eq 1 ]; then
            log "Hay migraciones nuevas: Flyway las aplica al arrancar la imagen."
        fi
        desplegar_backend || fallo=1
    fi

    if [ "$fallo" -eq 0 ] && [ "$DEPLOY_FRONTEND" -eq 1 ]; then
        desplegar_frontend || fallo=1
    fi

    if [ "$fallo" -eq 0 ]; then
        desplegar_workflow || fallo=1
    fi

    if [ "$fallo" -ne 0 ]; then
        log "El despliegue no se completo; el estado registrado no se actualiza."
        return 1
    fi

    registrar_estado "$objetivo" || { log "No se pudo registrar el estado desplegado."; return 1; }

    log "Despliegue completado en $objetivo."
}

orquestador_dry_run() {
    local state_file="$STATE_FILE"

    printf '%s\n' 'DEPLOY_DRY_RUN=1'

    if [ ! -f "$state_file" ]; then
        printf '%s\n' 'PLAN_ESTADO=PRIMER_DEPLOY'
        printf '%s\n' 'DEPLOY_FRONTEND=1'
        printf '%s\n' 'DEPLOY_BACKEND=1'
        printf '%s\n' 'DEPLOY_DATABASE=1'
        printf '%s\n' 'DEPLOY_COMPOSE=0'
        printf '%s\n' 'DEPLOY_NGINX=0'
        return 0
    fi

    local estado_commit
    estado_commit="$(head -n 1 "$state_file" | tr -d '[:space:]')"

    if [ -z "$estado_commit" ]; then
        printf '%s\n' 'El estado desplegado está vacío.' >&2
        return 1
    fi

    if ! git rev-parse --verify --end-of-options "$estado_commit^{commit}" >/dev/null 2>&1; then
        printf 'El estado desplegado contiene un commit inválido: %s\n' "$estado_commit" >&2
        return 1
    fi

    if ! git merge-base --is-ancestor "$estado_commit" origin/main >/dev/null 2>&1; then
        printf '%s\n' 'El estado desplegado no es antecesor de origin/main.' >&2
        return 1
    fi

    reset_detection

    local archivo
    while IFS= read -r archivo; do
        [ -n "$archivo" ] || continue
        classify_file "$archivo"
    done < <(git diff --name-only "$estado_commit" origin/main)

    printf '%s\n' 'PLAN_ESTADO=VALIDO'
    printf 'DEPLOY_FRONTEND=%s\n' "$DEPLOY_FRONTEND"
    printf 'DEPLOY_BACKEND=%s\n' "$DEPLOY_BACKEND"
    printf 'DEPLOY_DATABASE=%s\n' "$DEPLOY_DATABASE"
    printf 'DEPLOY_COMPOSE=%s\n' "$DEPLOY_COMPOSE"
    printf 'DEPLOY_NGINX=%s\n' "$DEPLOY_NGINX"
}

case "${1:-}" in
    --dry-run)
        shift
        orquestador_dry_run "$@"
        ;;
    --test-clasificacion)
        shift
        test_classification "$@"
        ;;
    --test-deteccion)
        test_detection
        ;;
    --test-git-diff)
        shift
        test_git_diff "$@"
        ;;
    --test-arbol-limpio)
        shift
        test_arbol_limpio "$@"
        ;;
    --test-estado)
        shift
        test_estado "$@"
        ;;
    --test-sincronizacion)
        shift
        test_sincronizacion "$@"
        ;;
    --test-estado-desplegado)
        shift
        test_estado_desplegado "$@"
        ;;
    --test-plan-estado)
        shift
        test_plan_estado "$@"
        ;;
    --test-baseline)
        shift
        test_baseline "$@"
        ;;
    --test-secuencia-backend)
        shift
        test_secuencia_backend "$@"
        ;;
    --preflight)
        shift
        preflight_produccion "$@"
        ;;
    --test-imagen)
        shift
        test_imagen "$@"
        ;;
    --test-rollback)
        shift
        test_rollback "$@"
        ;;
    --test-backend)
        shift
        test_backend "$@"
        ;;
    --test-plan)
        shift
        test_plan "$@"
        ;;
    --ayuda|-h)
        printf '%s\n' 'Uso: deploy.sh [--ref <commit|ref>] [--descartar-locales] [--todo]'
        printf '%s\n' '       deploy.sh --dry-run | --preflight [--dry-run] | --test-*'
        ;;
    *)
        desplegar "$@"
        ;;
esac
