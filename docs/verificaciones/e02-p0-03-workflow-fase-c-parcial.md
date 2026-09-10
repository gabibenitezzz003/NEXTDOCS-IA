# E02 / P0-03 · Workflow Fase C · desvíos C1, C2, C3, C4, C5, C6, C7, C8, C9, C10, C11, C12

Fecha: sesión actual.
Entorno: microservicio `workflow` levantado con `docker compose --profile workflow up -d --build` (puerto 8091), base `nextdocs_workflow`, Flyway migrado a V5.

## Cambios realizados

### Migraciones aplicadas

`workflow/src/main/resources/db/migration/V3__idempotencia_y_bloqueo_optimista.sql`

- `instancia_proceso`: columnas `clave_idempotencia` e índice único parcial por tenant; columna `version` para bloqueo optimista.
- `tarea_proceso`: columna `version` para bloqueo optimista.

`workflow/src/main/resources/db/migration/V4__activaciones_logicas.sql`

- Tabla `activacion_proceso` con tenant, instancia, nodo, estado, snapshot JSONB e intento.
- Índices de unicidad parcial y por instancia.

### C1 · Idempotencia de inicio

- `InstanciaProceso` y `InstanciaProcesoModel` almacenan `claveIdempotencia`.
- `IniciarInstanciaReqModel` acepta `claveIdempotencia`.
- `InstanciaProcesoRestController` lee el header `Idempotency-Key` y lo asigna al request.
- `EjecutorProcesoService.iniciar()` busca una instancia previa por `(tenant, claveIdempotencia)`; si existe, retorna la existente sin crear otra.

### C2 · Activaciones lógicas por nodo

- Entidad `ActivacionProceso` y tabla `activacion_proceso` almacenan una activación por `(tenant, instancia, nodo)`.
- `ActivacionProcesoService` registra y confirma activaciones con snapshots JSON.
- `EjecutorProcesoService.avanzar()` consulta activaciones confirmadas antes de ejecutar un nodo.
  - Si ya existe, recupera el destino/decisión o la tarea ya creada sin duplicar efectos.
  - Si no existe, ejecuta el nodo, crea tarea/subproceso/decisión y registra la activación.
- `TareaProcesoRepository.buscarPorInstanciaYNodo` permite recuperar la tarea asociada a una activación existente.

### C5 · Rechazo de tarea con motivo obligatorio

- `EjecutorProcesoService.completarTarea()` rechaza cualquier decisión distinta de `APROBADO` cuando no se envía `motivo`.

### C9 · Actor autorizado en tarea

- `TareaProceso` ya almacena `asignadoA`.
- `EjecutorProcesoService.completarTarea()` ahora valida que, si una tarea tiene `asignadoA`, el actor del request coincida; si no coincide, lanza `El actor no esta autorizado para completar esta tarea`.
- Esto es el cierre defensivo posible hasta que se integre IAM/JWT real; la validación de grants y roles se delega a IAM sin bloquear el avance.

### C3/C4 · Decisiones deterministas

- `EvaluadorDecisionService.elegir()` ahora:
  - Detecta datos requeridos ausentes y bloquea la decisión (C3).
  - Lanza error cuando múltiples ramas condicionadas coinciden (C4).
  - Usa la rama por defecto solo si no falta ningún dato requerido.

### C6 · Bloqueo optimista

- `@Version` agregado a `InstanciaProceso` y `TareaProceso`.
- `ErrorHandler` convierte `ObjectOptimisticLockingFailureException` en `409 CONFLICT`.

### C7 · Catálogo MVP0 en publicación

- Nuevo `TipoNodoProcesoMvp0` con los nodos habilitados para MVP0: `INICIO`, `FIN`, `SOLICITUD_DOCUMENTO`, `FORMULARIO`, `REVISION_HUMANA`, `DECISION`, `TEMPORIZADOR`.
- `ValidadorGrafoService.validarPublicacion()` aplica la validación de catálogo; `DefinicionProcesoService.publicar()` la usa.
- `ValidadorGrafoService.validar()` sigue permitiendo nodos futuros en borrador.

### C8 · Estados ESPERANDO y BLOQUEADA

- `EstadoInstanciaProceso` ahora incluye `CREADA`, `ACTIVA`, `ESPERANDO`, `BLOQUEADA`, `COMPLETADA`, `CANCELADA`.
- `AccionInstancia` añade `INSTANCIA_BLOQUEADA` e `INSTANCIA_REANUDADA`.
- `EjecutorProcesoService.iniciar()` crea la instancia en `CREADA` y la pasa a `ACTIVA` antes de avanzar.
- Nuevos métodos `bloquear()` y `reanudar()`; controlador expone `POST /api/v1/instancias/{id}/bloquear` y `/reanudar`.
- Validaciones: no se puede bloquear/reanudar desde estados finales.

### C10 · Pausa con motivo, transiciones de espera y temporizador

- `EstadoInstanciaProceso` distingue `ESPERANDO` (espera de negocio o temporizador) de `BLOQUEADA` (bloqueo administrativo).
- `EjecutorProcesoService.iniciar()` y `avanzar()` colocan la instancia en `ESPERANDO` cuando el nodo actual genera una tarea o temporizador pendiente.
- `EjecutorProcesoService.pausar()` exige un motivo; rechaza con `ValidacionException` si el motivo está ausente. Registra evento `INSTANCIA_BLOQUEADA` y pasa a `BLOQUEADA`.
- `EjecutorProcesoService.reanudar()` permite volver a `ACTIVA` desde `BLOQUEADA` o `ESPERANDO`; registra `INSTANCIA_REANUDADA`.
- Temporizadores generan una tarea con `vencimiento` calculado a partir de `slaHoras`; la instancia queda `ESPERANDO` hasta que vence el temporizador.
- `marcarVencidas()` y `completarTemporizadorVencido()` solo avanzan si la instancia está `ACTIVA` o `ESPERANDO`; una instancia `BLOQUEADA` no avanza por vencimiento.

### C11 · Quality gate, fixtures y adaptadores

- Nuevo endpoint `POST /api/v1/procesos/versiones/{versionId}/validar`.
- `DefinicionProcesoService.validarCalidad()` ejecuta `ValidadorGrafoService.validar()` y `validarPublicacion()` en una versión borrador sin publicarla.
- Permite previsualizar errores de estructura y de catálogo MVP0 antes de publicar.
- Nuevo `ProcesoFixture` (`ApplicationRunner`, deshabilitado por defecto con `nextdocs.workflow.fixtures.habilitado=false`). Al habilitarse, si el tenant indicado no tiene el proceso `WFL-APROBACION-DEMO`, lo crea con grafo `INICIO → REVISION_HUMANA → DECISION → FIN|FIN`.
- Nuevos contratos de adaptadores desacoplados: `ConectorDocumentalInt` y `ConectorFirmaInt` en `com.nextdocs.workflow.interfaces`, con modelos `DocumentoWorkflowModel` y `SolicitudFirmaWorkflowModel`.
- Implementaciones simuladas de referencia: `ConectorDocumentalSimulado` y `ConectorFirmaSimulado` en `com.nextdocs.workflow.adaptadores`.
- El nodo `SOLICITUD_DOCUMENTO` usa `ConectorDocumentalInt` para traer metadatos de documentos esperados y los guarda en los datos de la tarea (`documentosEsperados` + `documentosEncontrados`).
- `ValidadorGrafoService` acepta que `SOLICITUD_DOCUMENTO` se configure con `tipoDocumento` o con una lista `identificadores`.

### C12 · Provenance mínimo

- Nuevo `FuenteProceso`: `STUDIO`, `CLONADO`, `IMPORTADO`.
- Migración `V5__provenance_proceso.sql`: agrega a `definicion_proceso` las columnas `autor`, `fuente`, `procedencia_definicion_id`, `procedencia_version_id`, `procedencia_tenant_id`; y a `version_proceso` la columna `fuente`.
- `DefinicionProceso`, `DefinicionProcesoModel`, `DefinicionProcesoReqModel` y `DefinicionProcesoConverter` exponen los campos de provenance.
- Nuevo endpoint `POST /api/v1/procesos/{definicionId}/clonar` que crea una nueva definición con `fuente = CLONADO` y referencias a la definición, versión y tenant de origen.

## Dockerfile del workflow

Se creó `/home/gabibenitezzz/Escritorio/NEXT AI/workflow/Dockerfile` para poder levantar el microservicio con `docker compose --profile workflow up -d --build`.

## Tests

`mvn verify` en el microservicio workflow:

```text
Tests run: 53, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

`mvn verify` en el core documental:

```text
Tests run: 174, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Nuevos tests:

- `iniciarConClaveIdempotenciaReutilizaLaInstanciaExistente`
- `completarTareaRechazadaSinMotivoLanzaError`
- `variasCondicionesCoincidenLanzanAmbiguedad`
- `faltaDatoRequeridoBloqueaLaDecision`
- `publicacionApruebaSoloNodosMvp0`
- `publicacionRechazaNodosNoMvp0`
- `iniciarTransicionaDeCreadaAActiva`
- `bloquearYReanudarCambianElEstado`
- `noSePuedeBloquearUnaInstanciaCompletada`
- `pausarRequiereMotivo`
- `pausarConMotivoBloqueaLaInstancia`
- `temporizadorPoneLaInstanciaEnEsperando`
- `completarTareaRequiereActorAsignado`
- `ConectorDocumentalSimuladoTest`
- `ConectorFirmaSimuladoTest`

## Recorrido manual por API

1. **Crear definición**: `POST /api/v1/procesos` → `WFL-DEMO-002` en tenant `tenant-demo-wfl`.
2. **Actualizar grafo** (MVP0): `PUT /api/v1/procesos/versiones/{id}/grafo` con `INICIO → REVISION_HUMANA → DECISION → FIN|RECHAZO`.
3. **Publicar**: `POST /api/v1/procesos/versiones/{id}/publicar` → estado `PUBLICADA` con hash.
4. **Iniciar con Idempotency-Key**: `POST /api/v1/instancias` header `Idempotency-Key: idem-wfl-001` → `201`, instancia `b6d6f6b2-...`.
5. **Repetir misma clave**: mismo header → retorna la misma instancia, ignorando el body distinto.
6. **Iniciar sin clave**: nueva instancia distinta.
7. **Completar tarea rechazada sin motivo**: `400` `"El rechazo de una tarea requiere un motivo"`.
8. **Completar tarea aprobada con `aprobado=SI`**: instancia pasa a `COMPLETADA`.
9. **Bloquear instancia activa**: `POST /api/v1/instancias/{id}/bloquear` → estado `BLOQUEADA`.
10. **Reanudar instancia bloqueada**: `POST /api/v1/instancias/{id}/reanudar` → estado `ACTIVA`.
11. **Pausar instancia activa sin motivo**: `400` con mensaje de validación.
12. **Pausar con motivo**: `POST /api/v1/instancias/{id}/pausar` con `motivo` → estado `BLOQUEADA` (o `ESPERANDO` si se especifica política de espera).
13. **Temporizador**: al iniciar un proceso con `TEMPORIZADOR`, la instancia pasa a `ESPERANDO` y genera una tarea con `vencimiento` calculado.
14. **Activaciones lógicas**: tras recorrer una instancia, la tabla `activacion_proceso` contiene una fila por nodo ejecutado con snapshots de destino o tarea; repetir el recorrido no duplica tareas ni efectos.
15. **Actor no autorizado**: completar una tarea cuyo `asignadoA` es otro actor devuelve `400` con `El actor no esta autorizado`.
16. **Clonar proceso**: `POST /api/v1/procesos/{definicionId}/clonar` crea una definición con provenance `CLONADO`.
17. **Validar versión**: `POST /api/v1/procesos/versiones/{versionId}/validar` devuelve la versión si pasa estructura y catálogo MVP0.
18. **Solicitud de documento**: proceso con nodo `SOLICITUD_DOCUMENTO` configurado con `identificadores` deja la instancia en `ESPERANDO` y la tarea contiene `documentosEsperados` y `documentosEncontrados` (metadatos simulados).
19. **Docker/Flyway**: `docker compose --profile workflow up -d --build` levanta el servicio; `GET /actuator/health` responde `{"status":"UP"}`; Flyway está en versión `5`.

## Archivos modificados

- `workflow/Dockerfile` (nuevo)
- `workflow/src/main/resources/db/migration/V3__idempotencia_y_bloqueo_optimista.sql` (nuevo)
- `workflow/src/main/resources/db/migration/V4__activaciones_logicas.sql` (nuevo)
- `workflow/src/main/resources/db/migration/V5__provenance_proceso.sql` (nuevo)
- `workflow/src/main/java/com/nextdocs/workflow/entidades/ActivacionProceso.java` (nuevo)
- `workflow/src/main/java/com/nextdocs/workflow/enumeraciones/EstadoActivacionProceso.java` (nuevo)
- `workflow/src/main/java/com/nextdocs/workflow/enumeraciones/FuenteProceso.java` (nuevo)
- `workflow/src/main/java/com/nextdocs/workflow/repositorios/ActivacionProcesoRepository.java` (nuevo)
- `workflow/src/main/java/com/nextdocs/workflow/servicios/ActivacionProcesoService.java` (nuevo)
- `workflow/src/main/java/com/nextdocs/workflow/enumeraciones/EstadoInstanciaProceso.java`
- `workflow/src/main/java/com/nextdocs/workflow/enumeraciones/AccionInstancia.java`
- `workflow/src/main/java/com/nextdocs/workflow/entidades/InstanciaProceso.java`
- `workflow/src/main/java/com/nextdocs/workflow/entidades/TareaProceso.java`
- `workflow/src/main/java/com/nextdocs/workflow/modelos/InstanciaProcesoModel.java`
- `workflow/src/main/java/com/nextdocs/workflow/modelos/TareaProcesoModel.java`
- `workflow/src/main/java/com/nextdocs/workflow/modelos/IniciarInstanciaReqModel.java`
- `workflow/src/main/java/com/nextdocs/workflow/repositorios/InstanciaProcesoRepository.java`
- `workflow/src/main/java/com/nextdocs/workflow/servicios/EjecutorProcesoService.java`
- `workflow/src/main/java/com/nextdocs/workflow/servicios/EvaluadorDecisionService.java`
- `workflow/src/main/java/com/nextdocs/workflow/servicios/ValidadorGrafoService.java`
- `workflow/src/main/java/com/nextdocs/workflow/servicios/DefinicionProcesoService.java`
- `workflow/src/main/java/com/nextdocs/workflow/servicios/TipoNodoProcesoMvp0.java` (nuevo)
- `workflow/src/main/java/com/nextdocs/workflow/restControladores/InstanciaProcesoRestController.java`
- `workflow/src/main/java/com/nextdocs/workflow/restControladores/DefinicionProcesoRestController.java`
- `workflow/src/main/java/com/nextdocs/workflow/convertidores/DefinicionProcesoConverter.java`
- `workflow/src/main/java/com/nextdocs/workflow/convertidores/VersionProcesoConverter.java`
- `workflow/src/main/java/com/nextdocs/workflow/entidades/DefinicionProceso.java`
- `workflow/src/main/java/com/nextdocs/workflow/entidades/VersionProceso.java`
- `workflow/src/main/java/com/nextdocs/workflow/modelos/DefinicionProcesoModel.java`
- `workflow/src/main/java/com/nextdocs/workflow/modelos/DefinicionProcesoReqModel.java`
- `workflow/src/main/java/com/nextdocs/workflow/modelos/VersionProcesoModel.java`
- `workflow/src/main/java/com/nextdocs/workflow/modelos/DocumentoWorkflowModel.java` (nuevo)
- `workflow/src/main/java/com/nextdocs/workflow/modelos/SolicitudFirmaWorkflowModel.java` (nuevo)
- `workflow/src/main/java/com/nextdocs/workflow/interfaces/ConectorDocumentalInt.java` (nuevo)
- `workflow/src/main/java/com/nextdocs/workflow/interfaces/ConectorFirmaInt.java` (nuevo)
- `workflow/src/main/java/com/nextdocs/workflow/adaptadores/ConectorDocumentalSimulado.java` (nuevo)
- `workflow/src/main/java/com/nextdocs/workflow/adaptadores/ConectorFirmaSimulado.java` (nuevo)
- `workflow/src/main/java/com/nextdocs/workflow/configuracion/ProcesoFixture.java` (nuevo)
- `workflow/src/main/java/com/nextdocs/workflow/configuracion/ProcesoFixturePropiedades.java` (nuevo)
- `workflow/src/main/java/com/nextdocs/workflow/errores/ErrorHandler.java`
- `workflow/src/test/java/com/nextdocs/workflow/servicios/EjecutorProcesoServiceTest.java`
- `workflow/src/test/java/com/nextdocs/workflow/servicios/EvaluadorDecisionTest.java`
- `workflow/src/test/java/com/nextdocs/workflow/servicios/ValidadorGrafoTest.java`

## Pendientes

- **C9 IAM real**: reemplazar la validación defensiva de `asignadoA` por grants/roles IAM/JWT cuando esté disponible.
- **C11 · Plantillas COMEX**: extender `ProcesoFixture` con las 14 bases de proceso COMEX cuando el catálogo documental esté listo.
- **C11/C12 · Adaptadores productivos**: reemplazar `ConectorDocumentalSimulado` y `ConectorFirmaSimulado` por conectores reales cuando haya endpoints de DOC/firma disponibles.
- Rotación de la clave Gemini en consola de Google cuando finalice toda la implementación.
