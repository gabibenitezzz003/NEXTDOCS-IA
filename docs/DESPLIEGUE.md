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
| Workflow (profile `workflow`) | 8091 | 8091 |

Flyway corre al arrancar. Hibernate valida el esquema: si la imagen y la base no coinciden, el
proceso no queda `UP`.

El servicio `workflow` es el microservicio de procesos del MVP0 Comercial (repositorio hermano
`../workflow`). Arranca con `--profile workflow`; necesita el repo del workflow clonado al lado
del core porque el contexto de build apunta ahí. Usa la base `nextdocs_workflow` del mismo
PostgreSQL, que el servicio `postgres-init` crea de forma idempotente (también sobre volúmenes ya
existentes). Apagarlo no afecta a la API documental.

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

docker compose --profile workflow up -d --build    # microservicio de procesos (repo ../workflow)
curl -fsS http://localhost:8091/actuator/health
```

Compose pisa las URLs de `.env` que apuntan a `localhost`: adentro de la red Docker la base es
`postgres:5432`, Redis `redis:6379` y MinIO `http://minio:9000`. Los secretos (JWT, Gemini, claves
de MinIO) sí salen de `.env`.

Las URLs firmadas son la excepción: el `S3Client` habla con MinIO por la red interna, pero el
presigner firma con `NEXTDOCS_S3_ENDPOINT_PUBLICO` (`http://localhost:9102`) para que el navegador
del host resuelva el link. Si la variable queda vacía, el firmador cae al endpoint interno.

El frontend envía a Workflow el UUID real del tenant de la sesión como `X-Tenant-Id`. Para que
`/procesos` tenga datos en dev, el tenant demo del core nace con id fijo
(`NEXTDOCS_TENANT_DEMO_ID`, default `00000000-0000-4000-8000-000000000001`) y el fixture de
Workflow siembra `WFL-APROBACION-DEMO` bajo ese mismo tenant. Con una base dev ya existente el
tenant conserva su UUID original: hay que copiar ese id en `NEXTDOCS_WORKFLOW_FIXTURES_TENANT_ID`
del `.env` (`SELECT id FROM tenant WHERE codigo='demo'`) para que el fixture siembre ahí.

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

## AWS — producción en EC2

La instancia `nextdocs-ia-backend-prod` (`i-08d32d6d1248553aa`, Elastic IP `3.213.58.243`,
us-east-1) corre el stack productivo. El dominio público es `demo.mynextpipe.com`.

| Pieza | Dónde |
|---|---|
| Frontend | nginx host → `/var/www/nextdocs-ia` (site en `infra/nginx/nextdocs-ia.conf`) |
| Backend | `docker compose -f /etc/nextdocs-ia/compose.produccion.yml --env-file /etc/nextdocs-ia/nextdocs.env` — app en `127.0.0.1:8090`, Redis y ClamAV en red interna `nextdocs-interno` |
| Base | RDS `nextdocs-ia-prod` (PostgreSQL, mismo puerto lógico 5432) |
| Storage | S3 real: `nextdocs-{documentos,exportaciones,cuarentena}-178313340212-nextdocs`, por rol IAM `NextDocsIA-EC2-Role` (sin claves en env) |
| Perfil | `SPRING_PROFILES_ACTIVE=produccion`: el validador de arranque frena con defaults de desarrollo |

En producción `AlmacenamientoConfig` usa `DefaultCredentialsProvider` (rol IAM) y no pisa el
endpoint de S3; fuera de producción sigue MinIO con credenciales estáticas. Como no hay clave
secreta de S3, el validador no la exige y en cambio vigila `NEXTDOCS_BOOTSTRAP_SECRETO`.

### Primer tenant en producción

`NEXTDOCS_CREAR_TENANT_DEMO=false` en prod: el primer tenant se crea con
`POST /api/v1/bootstrap/tenant`, habilitado por `NEXTDOCS_BOOTSTRAP_HABILITADO` y protegido por
el header `X-Bootstrap-Secreto` (`NEXTDOCS_BOOTSTRAP_SECRETO`, mínimo 32 bytes). El endpoint
falla apenas existe un tenant activo y nginx sólo lo expone a `127.0.0.1`: hay que llamarlo
desde la propia instancia (SSH o `aws ssm`).

### Deploy automático

`deploy.sh` (raíz del repo) corre en el servidor sobre el clon `/opt/nextdocs-ia/app` y es
idempotente: diff entre el commit registrado en `/var/lib/nextdocs-ia/deployed_commit` y el
objetivo, clasifica los archivos y despliega sólo lo que cambió.

- Backend: preserva la imagen anterior como `nextdocs-ia-backup`, buildea, recrea `app`,
  espera `healthy` y restaura la imagen previa si falla. Flyway migra al arrancar.
- Frontend: build con contenedor `node:24-alpine` (`npm ci && npm run build`), copia del
  publicado en `/var/www/nextdocs-ia.prev` y `rsync --delete` a `/var/www/nextdocs-ia`.
- Cambios en `compose.yml` o `infra/nginx/` no se auto-aplican: quedan marcados para revisión
  manual (la config productiva vive en `/etc`, no en el clon).
- Concurrencia con `flock`; el estado sólo se registra si todo terminó bien. `--dry-run`
  muestra el plan sin tocar nada; `--todo` fuerza despliegue completo; `--ref` fija el commit.

El workflow `.github/workflows/deploy.yml` corre cuando `Verificar` termina en verde sobre
`main` (o manual con `workflow_dispatch`): snapshot de RDS, luego `ssm send-command` que ejecuta
`deploy.sh` como `ubuntu`. La autenticación es OIDC (`NextDocsIA-GitHub-Deploy`), sin claves AWS
en GitHub; el rol sólo puede `SendCommand` a esa instancia y crear snapshots de esa base.
El secreto del repo `AWS_DEPLOY_ROLE_ARN` contiene el ARN del rol.

SSH de operador: `ssh -i ~/.ssh/nextdocs-ia-prod.pem ubuntu@3.213.58.243` (el SG admite el 22
sólo desde IPs autorizadas). Alternativa sin puerto 22: `aws ssm start-session`.

### Workflow en producción

El servicio workflow (repo aparte `gabibenitezzz003/nextdocs-workflow`, puerto interno 8091) valida el mismo
JWT del core: `Authorization: Bearer` HS256 con `NEXTDOCS_JWT_SECRETO`, emisor `nextdocs-ai` y
claim `tenantId`. Con `NEXTDOCS_WORKFLOW_SEGURIDAD_JWT_HABILITADA=true` el header `X-Tenant-Id`
suelto ya no alcanza (401); el frontend manda el Bearer del core automáticamente.

`deploy.sh` lo maneja como unidad propia: clona/sincroniza `gabibenitezzz003/nextdocs-workflow` en
`/opt/nextdocs-ia/workflow`, compara contra `/var/lib/nextdocs-ia/deployed_workflow_commit`,
crea la base `nextdocs_workflow` en RDS si falta, buildea, recrea y espera `healthy` con
rollback a la imagen anterior (primera vez sin imagen previa: detiene el servicio). Si el
compose de producción no declara el servicio `workflow`, el paso se salta sin tocar nada.

Bloque a agregar en `/etc/nextdocs-ia/compose.produccion.yml` (contexto = el clon del repo
workflow al lado del del core):

```yaml
  workflow:
    build:
      context: /opt/nextdocs-ia/workflow
      dockerfile: Dockerfile
    container_name: nextdocs-ia-workflow
    restart: unless-stopped
    environment:
      NEXTDOCS_BD_URL: jdbc:postgresql://<rds-endpoint>:5432/nextdocs_workflow
      NEXTDOCS_BD_USUARIO: ${NEXTDOCS_BD_USUARIO}
      NEXTDOCS_BD_CLAVE: ${NEXTDOCS_BD_CLAVE}
      NEXTDOCS_WORKFLOW_SEGURIDAD_JWT_HABILITADA: "true"
      NEXTDOCS_JWT_SECRETO: ${NEXTDOCS_JWT_SECRETO}
      NEXTDOCS_JWT_EMISOR: nextdocs-ai
      NEXTDOCS_WORKFLOW_FIXTURES_HABILITADO: "false"
      NEXTDOCS_WORKFLOW_DOCUMENTAL_URL_BASE: http://app:8090
      NEXTDOCS_WORKFLOW_DOCUMENTAL_CLAVES: ${NEXTDOCS_WORKFLOW_DOCUMENTAL_CLAVES}
    ports:
      - "127.0.0.1:8091:8091"
    healthcheck:
      test: ["CMD-SHELL", "wget -qO- http://localhost:8091/actuator/health | grep -q UP"]
      interval: 30s
      timeout: 5s
      retries: 5
      start_period: 60s
    networks:
      - nextdocs-interno
```

#### Conector documental

Los nodos `SOLICITUD_DOCUMENTO` resuelven sus documentos contra el core. La autenticación va por
cuenta de servicio, que en el core pertenece a un tenant, así que la clave se configura **por
tenant**: `NEXTDOCS_WORKFLOW_DOCUMENTAL_CLAVES` es una lista `tenantId=clave` separada por comas.

Para dar de alta la cuenta y dejar la clave en el entorno, **parado en la instancia**:

```bash
ENV=/etc/nextdocs-ia/nextdocs.env TENANT=<codigo> EMAIL=<admin> CLAVE=<clave> \
  /opt/nextdocs-ia/app/scripts/alta-cuenta-motor.sh
```

`ENV` es obligatorio acá: por defecto el script escribe en el `.env` de la raíz del repo, que en
producción **no** es el archivo que lee compose (`--env-file /etc/nextdocs-ia/nextdocs.env`). Si se
omite, la clave queda en un archivo que nadie lee, y como se muestra una sola vez hay que dar de
alta otra cuenta.

`CORE` no hace falta: el default `http://localhost:8090` es donde escucha el core en la instancia.

La clave en claro se muestra una sola vez, por eso el script la guarda en lugar de imprimirla.

**Un tenant sin clave no resuelve documentos.** El nodo queda con `documentosEsperados` y sin
`documentosEncontrados`: la falta de configuración produce ausencia, nunca documentos inventados.
No hay conector simulado al que caer.

#### Firma electrónica (Documenso)

El nodo `FIRMA` crea un sobre en Documenso, expone el enlace de firma en la tarea
(`firmaEnlace`) y el webhook del proveedor cierra la tarea con `APROBADO`/`RECHAZADO`.
El circuito quedó verificado de punta a punta en producción el 16/09/2026.

Piezas, todas declaradas en `infra/compose.produccion.yml`:

- Servicio `documenso` con la imagen propia `documenso-firma:v2.18.0`, construida con
  `NEXT_PUBLIC_BASE_PATH=/firma` en tiempo de build: la imagen oficial no sirve la app
  bajo un subpath en runtime. Escucha en `127.0.0.1:3001` y nginx la publica en
  `https://<dominio>/firma/` (location propio, ver `infra/nginx/nextdocs-ia.conf`).
  El build emite los assets como `/firmaassets/*` (bug del base path sin barra final)
  y React Router pide `/__manifest` en raíz: nginx reescribe ambos hacia
  `/firma/assets/*` y `/firma/__manifest`. Sin esos dos location el visor de la página
  de firma queda colgado.
- Base `documenso` en el mismo RDS con usuario dedicado `documenso`; las migraciones
  las aplica la imagen al arrancar.
- Certificado de firma local `cert.p12` montado sólo lectura desde
  `/etc/nextdocs-ia/documenso/`; la frase va en `DOCUMENSO_FIRMA_FRASE`.
- `NEXT_PUBLIC_UPLOAD_TRANSPORT=database` y `NEXT_PRIVATE_JOBS_PROVIDER=local`: sin S3
  ni cola externa para el proveedor. SMTP queda configurado pero sin uso real: los
  sobres se crean con `distributionMethod: NONE`, así que Documenso no envía correos y
  el enlace lo entrega la tarea del workflow.

Variables del lado del workflow (`/etc/nextdocs-ia/nextdocs.env`):

- `NEXTDOCS_WORKFLOW_FIRMA_URL_BASE`: URL interna del proveedor
  (`http://nextdocs-ia-documenso:3000/firma`), la usa el adapter para la API v2.
- `NEXTDOCS_WORKFLOW_FIRMA_URL_PUBLICA`: URL que ve el firmante
  (`https://<dominio>/firma`), base del `firmaEnlace`.
- `NEXTDOCS_WORKFLOW_FIRMA_CLAVES`: lista `tenantId=apiKeyDeDocumenso` separada por
  comas, mismo mecanismo que `DOCUMENTAL_CLAVES`. **Un tenant sin entrada aquí no
  dispara firma**: la tarea queda sin `firmaEnlace`.
- `NEXTDOCS_WORKFLOW_FIRMA_WEBHOOK_SECRETO`: secreto compartido que el webhook exige
  en `X-Documenso-Secret`. Sin header o con valor incorrecto responde 401.

En Documenso hay que registrar el webhook apuntando a
`https://<dominio>/api/v1/firma/webhook` con los eventos `DOCUMENT_COMPLETED` y
`DOCUMENT_REJECTED` y el secreto anterior. Los eventos intermedios, duplicados y los de
tareas ya cerradas se ignoran.

**La cuenta de servicio necesita `documentos.escribir`.** El webhook descarga el PDF
firmado del proveedor y lo sube al core con la misma clave del conector documental.
Una cuenta creada sólo para leer (alcance `documentos.leer`) recibe 403 del core, el
webhook falla con `EVENTO_EXTERNO_FALLIDO`/502 y la tarea queda `PENDIENTE` aunque el
documento ya esté firmado. El alta es por tabla `cuenta_servicio_alcance` si la cuenta
ya existe, o dándole ambos alcances al crearla.

Documenso reintenta el webhook unas pocas veces y lo da por `FAILED` (tabla
`WebhookCall`). Para re-entregarlo se puede reenviar el `requestBody` registrado al
endpoint del workflow con el header `X-Documenso-Secret` correcto.

Requisitos en la instancia (una sola vez): clave de despliegue SSH de sólo lectura para
`gabibenitezzz003/nextdocs-workflow` en el usuario `ubuntu` (el `git clone` corre con ella), y
`NEXTDOCS_JWT_SECRETO` presente en `/etc/nextdocs-ia/nextdocs.env` con el mismo valor que usa
el core. nginx ya rutea `/api/v1/{procesos,instancias,tareas,kpi-procesos,partners,
marketplace,supervisora,colaboracion-externa,template-recommendations,
workflow-instances,firma}` a `127.0.0.1:8091` desde `infra/nginx/nextdocs-ia.conf`.
Cada ruta nueva del workflow exige sumarla a esa lista y recargar nginx en el
servidor (`sudo nginx -t && sudo systemctl reload nginx`): el archivo productivo
vive en `/etc` y no se auto-aplica con el deploy.

Disparadores: cada push a `main` de `gabibenitezzz003/nextdocs-workflow` llama `repository_dispatch`
(`workflow-actualizado`) sobre este repo vía `.github/workflows/despachar.yml` (secreto
`NEXTDOCS_DISPATCH_TOKEN` en ese repo), y el deploy normal del core también sincroniza el
workflow si cambió — un merge del core que no toca el workflow no lo rebuildeará.

#### Motor documental (nextdocs-documental)

Fork de `docvance-ai` en `gabibenitezzz003/nextdocs-documental`: mismo pipeline
(catálogo → extracción → emparejamiento → decisión) pero con código propio para evolucionar
sin tocar DocVance ni ia-docs de Follow. Corre como dos contenedores en producción:

- `documental-api` (puerto interno `4002→4000`) y `documental-worker` (misma imagen
  `nextdocs-documental:1.x`, comando `node apps/worker/dist/principal.js`). El deploy
  manual de imagen hoy es `docker build` local + `docker save | ssh docker load`.
- Base `nextdocs_documental` en el mismo RDS con usuario dedicado; `POSTGRES_SSL=true`
  es obligatorio (RDS rechaza conexiones sin TLS). Migraciones:
  `docker compose run --rm --no-deps documental-api node packages/db/dist/migrar.js`.
- Almacenamiento: el **mismo bucket** `NEXTDOCS_S3_BUCKET_DOCUMENTOS` con prefijo
  `documental/` (`ALMACENAMIENTO_PREFIJO`) y la cadena de credenciales AWS del rol de la
  instancia — sin endpoint ni claves cuando `ALMACENAMIENTO_ENDPOINT` está vacío.
- Redis compartido con el core: BullMQ nombra las claves por cola
  (`nextdocs-documental-procesamiento`), sin colisión.
- Alta de un inquilino (por tenant del core):
  `docker compose run --rm --no-deps -e ALTA_INQUILINO_ID=<uuidTenantCore>
   -e ALTA_INQUILINO_NOMBRE="<nombre>" -e NEXTDOCS_DOCUMENTAL_API_KEY=<ndk_...>
   documental-api node packages/db/dist/alta-inquilino.js` — crea el inquilino con las
  11 plantillas base y la clave (huella SHA-256). El inquilino Diagnostico
  (`40ec8745-84d2-4c81-a7f4-4839e0606270`) ya existe con este mismo ID del core.
- `PROVEEDOR_IA=gemini` con `GOOGLE_API_KEY` y `GEMINI_MODELO=gemini-3.6-flash`
  (el modelo `gemini-2.0-flash` que traía el .env.ejemplo está retirado por Google).
- Correo deshabilitado en producción (`CORREO_ANFITRION` vacío): la bandeja de salida
  no despacha e-mails. ARCA queda en `homologacion` sin certificado — sin efecto.
- La API es interna: escucha sólo en `127.0.0.1:4002` y `nextdocs-interno`, autenticación
  por `Authorization: Bearer ndk_*` (huella SHA-256 en `clave_api`).

### HTTPS

Pendiente hasta que `demo.mynextpipe.com` apunte a `3.213.58.243` por DNS: `certbot --nginx`
emite el certificado y reescribe el site. El 443 ya está abierto en el security group.

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
