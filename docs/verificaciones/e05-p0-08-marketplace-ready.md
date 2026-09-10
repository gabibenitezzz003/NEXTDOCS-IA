# E05 · P0-08 · Template marketplace-ready

## Alcance

Modelado marketplace-ready en el microservicio workflow:

- `InstalacionPlantilla`: registro de una plantilla publicada instalada en un tenant cliente.
- `SobreescrituraPlantillaTenant`: overlays locales de un nodo para una instalación.
- `MarketplacePublicacion`: listado marketplace (dormante en MVP0).
- `TerminosComerciales`: condiciones comerciales por definición/version (dormante en MVP0).
- Endpoints `/api/v1/marketplace/instalaciones` y `/api/v1/marketplace/instalaciones/{instalacionId}/sobrescrituras`.

## Pruebas unitarias

Comando usado:

```bash
cd /home/gabibenitezzz/Escritorio/NEXT\ AI/workflow
mvn verify
```

Resultado:

```text
Tests run: 66, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Nuevo test: `MarketplaceServiceTest`.

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
Migrating schema "public" to version "8 - marketplace ready"
Successfully applied 1 migration to schema "public", now at version v8
```

### Marketplace listado vacío

```bash
curl -s -H "X-Tenant-Id: tenant-market" http://localhost:8091/api/v1/marketplace/instalaciones
```

Respuesta:

```json
[]
```

## Resultado

- `mvn verify`: **66 tests, 0 fallas**.
- Flyway en `V8`.
- Endpoints `marketplace` operativos.
