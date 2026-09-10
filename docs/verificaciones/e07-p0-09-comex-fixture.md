# E07 · P0-09 · Biblioteca COMEX

## Alcance

Fixture de biblioteca COMEX en el microservicio workflow:

- 14 plantillas base y multimodales.
- Grafo estandar COMEX: `INICIO -> SOLICITUD_DOCUMENTO -> VALIDACION_IA -> DECISION -> FIN` con rama a `REVISION_HUMANA`.
- Documentos base: `BL`, `PACKING_LIST`, `CERTIFICADO_ORIGEN`.
- Activación condicional con `nextdocs.workflow.comex.habilitado=true` y tenant `nextdocs.workflow.comex.tenant-id` (default `tenant-demo-comex`).

## Plantillas creadas

| Código | Nombre |
|---|---|
| EX-MAR-FCL | Export Maritimo FCL |
| EX-MAR-LCL | Export Maritimo LCL |
| EX-AIR | Export Aereo |
| EX-ROAD-FTL | Export Terrestre FTL |
| EX-ROAD-LTL | Export Terrestre LTL |
| IM-MAR-FCL | Import Maritimo FCL |
| IM-MAR-LCL | Import Maritimo LCL |
| IM-AIR | Import Aereo |
| IM-ROAD-FTL | Import Terrestre FTL |
| IM-ROAD-LTL | Import Terrestre LTL |
| EX-MM-ROAD-SEA | Export Multimodal Road-Sea |
| IM-MM-SEA-ROAD | Import Multimodal Sea-Road |
| EX-MM-ROAD-AIR | Export Multimodal Road-Air |
| IM-MM-AIR-ROAD | Import Multimodal Air-Road |

## Pruebas unitarias

Comando usado:

```bash
cd /home/gabibenitezzz/Escritorio/NEXT\ AI/workflow
mvn verify
```

Resultado:

```text
Tests run: 70, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Nuevo test: `ComexFixtureTest` (verifica que se creen 14 definiciones con version, grafo y publicacion).

## Ejecución en Docker

### Health

```bash
curl -s http://localhost:8091/actuator/health
```

Respuesta:

```json
{"status":"UP","groups":["liveness","readiness"]}
```

### Activación manual

Para levantar el fixture COMEX en un entorno:

```bash
export NEXTDOCS_WORKFLOW_COMEX_HABILITADO=true
export NEXTDOCS_WORKFLOW_COMEX_TENANT_ID=tenant-demo-comex
docker compose --profile workflow up -d
```

Luego `GET /api/v1/procesos` con `X-Tenant-Id: tenant-demo-comex` devuelve las 14 plantillas publicadas.

## Resultado

- `mvn verify`: **70 tests, 0 fallas**.
- Docker `nextdocs-workflow:local` rebuilt y `UP`.
- Fixture COMEX listo para activarse por feature flag.
