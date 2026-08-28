# Despliegue de NEXT DOC AI

Runbook de la Etapa 1. El criterio del N3 es `Compose/Helm/IaC + envs/health`: hoy el artefacto
reproducible es Compose + imagen Docker + CI. Helm queda para cuando haya más de un ambiente.

## Qué se despliega

Un solo proceso Java (`nextdocs-ai.jar`) y tres dependencias:

| Servicio | Puerto publicado | Puerto interno |
|---|---|---|
| API (`app`, profile `app`) | 8090 | 8090 |
| PostgreSQL 16 | 5434 | 5432 |
| Redis 7 | 6381 | 6379 |
| MinIO | 9102 API / 9101 consola | 9000 / 9101 |
| ClamAV | 3310 | 3310, profile `antivirus` |

Flyway corre al arrancar. Hibernate valida el esquema: si la imagen y la base no coinciden, el
proceso no queda `UP`.

## Máquina de desarrollo (infra sola)

Sigue siendo el camino de todos los días. El profile `app` **no** arranca:

```bash
cp .env.example .env
docker compose up -d
```

La API se corre con Maven (o el contenedor Maven si no hay JDK).

## Stack completo

```bash
cp .env.example .env          # JWT y Gemini si aplica
docker compose --profile app up -d --build
curl -fsS http://localhost:8090/actuator/health
```

Compose pisa las URLs de `.env` que apuntan a `localhost`: adentro de la red Docker la base es
`postgres:5432`, Redis `redis:6379` y MinIO `http://minio:9000`. Los secretos (JWT, Gemini, claves
de MinIO) sí salen de `.env`.

Listo cuando `/actuator/health` responde `{"status":"UP"}`. El tenant demo se crea si
`NEXTDOCS_CREAR_TENANT_DEMO=true` (default).

```
POST /api/v1/autenticacion/ingresar
{ "codigoTenant": "demo", "email": "admin@nextdocs.ai", "clave": "..." }
```

## Imagen

```bash
docker build -t nextdocs-ai:local backend
```

Multi-stage: Maven 3.9 / Java 21 compila el jar y la runtime es JRE 21 (usuario `nextdocs`, uid
10001). No lleva tests ni `.env`. Healthcheck interno: `GET /actuator/health`.

## CI

`.github/workflows/verificar.yml` en cada push/PR a `main`:

1. Service containers de Postgres, Redis y MinIO en los mismos puertos que el compose local.
2. `mvn -B verify` con `NEXTDOCS_PRUEBA_OBLIGATORIA=true`. Si la infra no está, **falla**; no se
   salta la suite como en un laptop sin Docker.
3. Construye la imagen (sin publicarla). El registry queda para cuando haya ambiente.

La CI no usa Testcontainers: es el mismo contrato que `PruebaIntegracion`.

## Producción (mínimo)

**Activá el perfil `produccion`.** Con `SPRING_PROFILES_ACTIVE=produccion` el arranque se **frena**
si la configuración no es apta, y lista todos los problemas juntos antes de abrir el puerto:

```
El perfil produccion esta activo pero la configuracion no es apta. Corregi lo siguiente:
  1. NEXTDOCS_JWT_SECRETO sigue siendo el valor de ejemplo del application.yml
  2. NEXTDOCS_CREAR_TENANT_DEMO esta en true y crearia un tenant con credenciales conocidas
  3. la clave secreta del object store es un valor de desarrollo conocido
  4. NEXTDOCS_ANTIVIRUS_MOTOR es PERMISIVO: los archivos no se analizan
```

No hay forma de arrancar en producción con los defaults de desarrollo. Si querés correr sin
antivirus a propósito, hay que decirlo explícito con `NEXTDOCS_ANTIVIRUS_PERMITIR_SIN_ANALISIS=true`.

Antes de exponer el puerto 8090:

1. `NEXTDOCS_JWT_SECRETO` de al menos 32 bytes, distinto del default del `application.yml`.
2. `NEXTDOCS_BD_CLAVE`, `NEXTDOCS_S3_CLAVE_SECRETA` y `NEXTDOCS_TENANT_DEMO_CLAVE` propias.
3. `NEXTDOCS_CREAR_TENANT_DEMO=false` salvo el primer boot.
4. `NEXTDOCS_ANTIVIRUS_MOTOR=CLAMAV` y `--profile antivirus` si el tenant exige `SEC-04`.
5. No publicar `/actuator/prometheus` ni `/actuator/metrics` a internet: piden autenticación, pero
   el scrape debe ir por red interna o cuenta de servicio.
6. Health público: `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`.

7. **Límite de uso**: `NEXTDOCS_LIMITE_ACTIVO=true` (por defecto). Ajustá
   `NEXTDOCS_LIMITE_PRINCIPAL`, `NEXTDOCS_LIMITE_TENANT` y `NEXTDOCS_LIMITE_INGESTA` al plan del
   cliente. El de ingesta es el que protege la cuota del proveedor de IA.
8. **Proveedor de respaldo**: configurá `DEEPSEEK` además de `GEMINI` en `configuracion_proveedor`
   con `respaldo = true`. Sin un segundo proveedor, un 429 sostenido de Google frena la extracción.

No hay Helm todavía. Un orchestrator puede usar la misma imagen y las mismas variables, con
liveness/readiness en esos paths.

## Recuperación

| Síntoma | Qué hacer |
|---|---|
| Health `DOWN` o el contenedor reinicia | `docker compose --profile app logs app --tail=200`. Casi siempre es Flyway/validate o S3/Redis inalcanzable |
| Cola de extracción quieta | El worker vive en el mismo proceso. `docker compose --profile app restart app` relee Redis |
| Webhook en DLQ | `POST /api/v1/integraciones/entregas/{id}/reintentar` (gobernanza.administrar) |
| MinIO vacío tras recrear volumen | `minio-init` corre otra vez al `up`; si el app ya estaba, `restart app` |
| Base restaurada de backup | Arrancar la **misma** versión de imagen que produjo esas migraciones; Flyway no baja de versión |

No hay “rebuild de projections”: el estado del documento está en PostgreSQL. Reprocesar un
documento puntual es `POST /api/v1/documentos/{id}/reprocesar`.
