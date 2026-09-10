# E04 · P0-05 · Colaboración externa

## Alcance

Implementación de colaboración externa en el microservicio workflow:

- `CuentaExterna` con email, nombre, estado y baja lógica.
- `EnlaceAccionSeguro` con token único, `tareaId`, `actor`, scopes, expiración, usos máximos y contador.
- Uso del enlace que completa la tarea asociada validando tenant, token, expiración, usos y scopes.

## Pruebas unitarias

Comando usado:

```bash
cd /home/gabibenitezzz/Escritorio/NEXT\ AI/workflow
mvn verify
```

Resultado:

```text
Tests run: 63, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Nuevo test: `ColaboracionExternaServiceTest`.

## Ajuste de estado en `EjecutorProcesoService`

Se corrigió `completarTarea` para permitir instancias en `ESPERANDO`, ya que `crearTarea` deja la instancia en ese estado. El control ahora es:

```java
if (instancia.getEstado() != EstadoInstanciaProceso.ACTIVA
        && instancia.getEstado() != EstadoInstanciaProceso.ESPERANDO) { ... }
```

Esto hace coherente el flujo real con el manejo de `ESPERANDO` que ya existía más adelante en el mismo método.

## Ejecución en Docker

### Health

```bash
curl -s http://localhost:8091/actuator/health
```

Respuesta:

```json
{"status":"UP","groups":["liveness","readiness"]}
```

### Flyway

Log de arranque:

```text
Migrating schema "public" to version "7 - colaboracion externa"
Successfully applied 1 migration to schema "public", now at version v7
```

### Flujo manual completo

Crear proceso con revisión humana (`REVISION_HUMANA`) en `tenant-colab`:

- `POST /api/v1/procesos` con `COLAB-001`
- `POST /api/v1/procesos/{definicionId}/versiones`
- `PUT /api/v1/procesos/versiones/{versionId}/grafo` con `asignadoA: externo@demo.com`
- `POST /api/v1/procesos/versiones/{versionId}/publicar`
- `POST /api/v1/instancias` para obtener `instanciaId`
- `GET /api/v1/tareas?instanciaId=...` para obtener `tareaId`

Crear cuenta externa:

```bash
curl -s -X POST -H "X-Tenant-Id: tenant-colab" -H "Content-Type: application/json" \
  http://localhost:8091/api/v1/colaboracion-externa/cuentas \
  -d '{"email":"externo@demo.com","nombre":"Externo Demo"}'
```

Respuesta:

```json
{"id":"c276c6f3-0ed5-4843-a936-607abbda59e5","email":"externo@demo.com","nombre":"Externo Demo","estado":"ACTIVA","alta":"..."}
```

Crear enlace seguro:

```bash
curl -s -X POST -H "X-Tenant-Id: tenant-colab" -H "Content-Type: application/json" \
  http://localhost:8091/api/v1/colaboracion-externa/enlaces \
  -d '{"tareaId":"b2d339e5-84dd-45ca-baf8-82e448cd947b","actor":"externo@demo.com","scopes":["COMPLETAR_TAREA"],"expiracion":"2026-12-31T23:59:59Z","usosMaximos":1}'
```

Respuesta:

```json
{"id":"2885f80e-0585-4d33-8a52-b64b2307885a","token":"f88554c4-af10-4055-8277-fd3f437c7920","tareaId":"b2d339e5-84dd-45ca-baf8-82e448cd947b","actor":"externo@demo.com","scopes":["COMPLETAR_TAREA"],"expiracion":"2026-12-31T23:59:59Z","usosMaximos":1,"usos":0,"estado":"ACTIVO","alta":"..."}
```

Usar el enlace:

```bash
curl -s -X POST -H "X-Tenant-Id: tenant-colab" -H "Content-Type: application/json" \
  http://localhost:8091/api/v1/colaboracion-externa/enlaces/f88554c4-af10-4055-8277-fd3f437c7920 \
  -d '{"decision":"APROBADO"}'
```

Respuesta:

```json
{"id":"b2d339e5-84dd-45ca-baf8-82e448cd947b","instanciaId":"7ef1a6e0-60de-4883-8639-a66ceb1152ac","nodoId":"revision","tipoNodo":"REVISION_HUMANA","estado":"COMPLETADA","asignadoA":"externo@demo.com","decision":"APROBADO","datos":{},"completada":"2026-09-10T13:25:28.455250828Z","completadaPor":"externo@demo.com"}
```

Verificar estado del enlace:

```bash
curl -s -H "X-Tenant-Id: tenant-colab" http://localhost:8091/api/v1/colaboracion-externa/enlaces
```

El enlace ahora muestra `usos: 1` y `estado: USADO`.

## Resultado

- `mvn verify`: **63 tests, 0 fallas**.
- Contenedor `nextdocs-workflow` en `UP`.
- Flyway en `V7`.
- Flujo end-to-end de colaboración externa funcional.
