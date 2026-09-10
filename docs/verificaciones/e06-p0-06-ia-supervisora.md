# E06 · P0-06 · IA Supervisora v0

## Alcance

Capa de proceso de la IA Supervisora en el microservicio workflow:

- `ReglaSupervisora`: reglas versionables por tenant/plantilla con tipo, umbral, acción y severidad.
- `HallazgoProceso`: hallazgos generados sobre instancias de proceso con acción, severidad, estado y referencia.
- `IaSupervisoraService`: evalúa una instancia aplicando reglas, crea hallazgos y bloquea la instancia si la regla indica `BLOQUEAR` y el estado lo permite.
- Endpoints REST bajo `/api/v1/supervisora`.

## Pruebas unitarias

Comando usado:

```bash
cd /home/gabibenitezzz/Escritorio/NEXT\ AI/workflow
mvn verify
```

Resultado:

```text
Tests run: 69, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Nuevo test: `IaSupervisoraServiceTest`.

## Ajuste de tipo `umbral`

La primera imagen Docker falló por `scale has no meaning for SQL floating point types` porque el `umbral` estaba mapeado como `Double` con `precision` y `scale`. Se cambió a `BigDecimal` en entidad, modelo y DTO.

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
Migrating schema "public" to version "9 - supervisora"
Successfully applied 1 migration to schema "public", now at version v9
```

### Listado de reglas

```bash
curl -s -H "X-Tenant-Id: tenant-sup" http://localhost:8091/api/v1/supervisora/reglas
```

Respuesta:

```json
[]
```

## Resultado

- `mvn verify`: **69 tests, 0 fallas**.
- Flyway en `V9`.
- Servicio `nextdocs-workflow` en `UP`.
- Endpoints de supervisora operativos.
