# E03 · P0-07 + P0-14 · Workflow KPI y Partner Foundation

## Alcance

Verificación conjunta de dos comerciales en el microservicio workflow:

- **P0-14**: KPI operativo de procesos (`/api/v1/kpi-procesos`).
- **P0-07**: Partner Foundation (`/api/v1/partners/organizaciones` y `/api/v1/partners/delegaciones`).

## Requisitos previos

- Infraestructura de workflow levantada con Docker Compose.
- Flyway aplicado hasta `V6`.
- Imagen `nextdocs-workflow:local` reconstruida con el estado del repo.

## Pruebas unitarias

Comando usado:

```bash
cd /home/gabibenitezzz/Escritorio/NEXT\ AI/workflow
mvn verify
```

Resultado:

```text
Tests run: 59, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Nuevos tests añadidos:

- `KpiProcesoServiceTest`
- `PartnerServiceTest`

## Pruebas de ejecución en Docker

### Health

```bash
curl -s http://localhost:8091/actuator/health
```

Respuesta:

```json
{"status":"UP","groups":["liveness","readiness"]}
```

### Flyway

El log de arranque del contenedor mostró:

```text
Migrating schema "public" to version "6 - partner foundation"
Successfully applied 1 migration to schema "public", now at version v6
```

### KPI resumen

```bash
curl -s -H "X-Tenant-Id: tenant-1" http://localhost:8091/api/v1/kpi-procesos
```

Devolvió `13` indicadores con `formula`, `fuente`, `tendencia`, `estadoFuente` y `drilldown` conforme al contrato V11.4.

### KPI drill-down (población)

```bash
curl -s -H "X-Tenant-Id: tenant-1" \
  'http://localhost:8091/api/v1/kpi-procesos/poblacion?indicador=instanciasCompletadas'
```

Devolvió población vacía para un tenant sin datos.

### Partner: creación y aislamiento de tenant

Crear `SOCIO-A` en `tenant-partner`:

```bash
curl -s -X POST -H "Content-Type: application/json" -H "X-Tenant-Id: tenant-partner" \
  http://localhost:8091/api/v1/partners/organizaciones \
  -d '{"codigo":"SOCIO-A","nombre":"Socio A","emailContacto":"a@socio.com"}'
```

Respuesta:

```json
{"id":"8b3072f6-8144-4f3a-866c-7b8150b089fc","codigo":"SOCIO-A","nombre":"Socio A","emailContacto":"a@socio.com","estado":"ACTIVA","alta":"2026-09-10T12:35:17.436433189Z","delegaciones":[]}
```

Crear `SOCIO-A` en `tenant-otro`:

```bash
curl -s -X POST -H "Content-Type: application/json" -H "X-Tenant-Id: tenant-otro" \
  http://localhost:8091/api/v1/partners/organizaciones \
  -d '{"codigo":"SOCIO-A","nombre":"Socio Otro","emailContacto":"otro@socio.com"}'
```

Respuesta distinto id, mismo código permitido por tenant.

Listar en `tenant-partner`:

```bash
curl -s -H "X-Tenant-Id: tenant-partner" http://localhost:8091/api/v1/partners/organizaciones
```

Respuesta:

```json
[{"id":"8b3072f6-8144-4f3a-866c-7b8150b089fc","codigo":"SOCIO-A","nombre":"Socio A","emailContacto":"a@socio.com","estado":"ACTIVA","alta":"2026-09-10T12:35:17.436433Z","delegaciones":[]}]
```

Solo devolvió la organización del tenant solicitado.

### Partner: idempotencia

Repetir `SOCIO-A` en `tenant-partner`:

```bash
curl -s -o /dev/null -w "%{http_code}" -X POST -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-partner" http://localhost:8091/api/v1/partners/organizaciones \
  -d '{"codigo":"SOCIO-A","nombre":"Otro","emailContacto":"a@socio.com"}'
```

HTTP `409`.

### Partner: delegación

```bash
curl -s -X POST -H "Content-Type: application/json" -H "X-Tenant-Id: tenant-partner" \
  http://localhost:8091/api/v1/partners/delegaciones \
  -d '{
    "partnerId":"8b3072f6-8144-4f3a-866c-7b8150b089fc",
    "tenantClienteId":"tenant-cliente-1",
    "scopes":["procesos.leer","procesos.escribir"],
    "expiracion":"2026-12-31T23:59:59Z",
    "aprobador":"admin@cliente.com"
  }'
```

Respuesta:

```json
{"id":"729bfc61-2a5c-41de-8cba-146a87bbe505","partnerId":"8b3072f6-8144-4f3a-866c-7b8150b089fc","tenantClienteId":"tenant-cliente-1","scopes":["procesos.leer","procesos.escribir"],"expiracion":"2026-12-31T23:59:59Z","aprobador":"admin@cliente.com","alta":"2026-09-10T12:36:03.573489607Z"}
```

## Evidencias levantadas

- `TODO_MVP0_COMERCIAL.md` actualizado:
  - `P0-07` completado.
  - `P0-14` completado.
- Imagen `nextdocs-workflow:local` reconstruida y funcionando.
- Migración `V6__partner_foundation.sql` aplicada.
- Endpoints `/api/v1/kpi-procesos` y `/api/v1/partners/*` operativos.
