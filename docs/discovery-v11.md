# E00 · Discovery V11 — Baseline de NEXT DOC AI

Documento generado por el Prompt 00 de descubrimiento del kit V11.  
Fecha: 2026-09-07 (baseline reportado) / descubrimiento ejecutado en la sesión actual.  
Repositorio core: `/home/gabibenitezzz/Escritorio/NEXT AI/nextdocs-ai`.  
Repositorio workflow: `/home/gabibenitezzz/Escritorio/NEXT AI/workflow`.  
Kit V11: `/home/gabibenitezzz/Escritorio/NEXT AI/nextdocs-ai/docs/implementation-v11/NEXT_DOC_AI_V11_4_IMPLEMENTACION`.

---

## 1. Resumen ejecutivo

El proyecto **NEXT DOC AI** es un monorepo con backend Spring Boot 3.4 / Java 21, frontend React 19 / TypeScript / Vite y un microservicio de workflow también en Spring Boot 3.4 / Java 21. El código y la documentación están en español, siguiendo la convención heredada de `follow-backend`.

El core documental está **implementado y relativamente maduro**: ingesta, clasificación automática, extracción con Gemini/DeepSeek/simulado, validación, revisión humana, excepciones, auditoría, retención, exportación, webhooks HMAC, SSO/embed, multi-tenancy, JWT y control de costos. El backend reporta 247 pruebas; en el repo se encontraron 26 clases de test Java (8 unitarias `*Test` y 18 de integración `*IT`), más helpers de prueba. El código Java del core consta de 336 archivos; el microservicio workflow consta de 53 archivos Java.

El microservicio **workflow** existe (`workflow/`), pero hay desvíos respecto a la especificación V11 del N2_WFL que deben cerrarse antes de considerar E04 cerrado (Fase C del `TODO_MVP0_COMERCIAL.md`). En particular: falta idempotencia de inicio, activación lógica persistida, rechazo con motivo obligatorio, bloqueo optimista y autenticación JWT real.

El frontend navegable actual cubre las pantallas core (login, resumen, panel, documentos, excepciones, tipos propuestos, procesos) pero no tiene tests automatizados ni la totalidad del UX-01…UX-38 propuesto por el kit.

**Hallazgo crítico:** el kit V11 reporta como implementado un microservicio Workflow completo, mientras que la documentación interna `docs/TODO_MVP0_COMERCIAL.md` (rebaseline 06/09/2026) lo marca como "No empezado" / "Con desvíos". La realidad del código es intermedia: hay definiciones, versiones, grafo JSONB, runtime, tareas, timers y bitácora, pero faltan reglas de idempotencia, asignación autorizada y autenticación. No se debe reconstruir lo que existe, sí cerrar los desvíos de la Fase C.

**Próximo corte vertical recomendado:** P0-02 (validar el core documental con recorrido humano + dataset real) en paralelo con la **Fase C de conformidad del workflow** (C1..C12), porque P0-03 (Workflow Definition + Runtime) es el bloque largo del MVP0 Comercial.

---

## 2. Kit V11 leído

Se leyeron los siguientes documentos del kit:

| Archivo | Contenido clave |
|---|---|
| `00_LEEME_PRIMERO.md` | Stack React + Java + PostgreSQL/AWS; MVP0 reportado: 247 pruebas, 13 controladores, 40 entidades, 20 migraciones; canales email/WhatsApp retirados; discrepancia COMEX catorce bases. |
| `00_LEEME_V11_2.md` | Regla de oro: no reconstruir capacidades backend que ya existen; Figma. |
| `00_LEEME_V11_4.md` | Preserva V11.2 byte a byte; UX/KPI/analytics nuevos; `/api/v1/kpi/*` como contrato vigente. |
| `02_N0/N0_ARQUITECTURA_IMPLEMENTACION.md` | Monolito modular; workers separados; Keycloak; MinIO emulador; transacciones outbox/inbox; seguridad transversal; expand/backfill/verify/contract. |
| `11_BASELINE/ESTADO_BACKEND_20260907.md` | Lista de implementado y pendiente MVP0; workflow reportado implementado; cierre pendiente incluye External Collaboration, IA Supervisora, Partner Foundation, COMEX, auth workflow, Playwright. |
| `11_BASELINE/backend_20260907/10-INVENTARIO-CODIGO-FUENTE.md` | Inventario detallado del baseline: 12+1 controladores, 44+ servicios, 40 entidades, 39 enums, 39 repos, 9 convertidores, ~82 modelos, 17 config, 3 clientes, 3 interfaces, 3 filtros, 9 excepciones, 15 utilidades, 2 builders, 20 migraciones; workflow con 3 controladores, 6 servicios, 5 entidades, etc. |
| `08_CIERRE/DECISIONES_PENDIENTES.md` | D01-D11: repo/ramas, motor AWS, framework Java, MVP0 ampliado, auth frontend, contratos Follow, catálogo COMEX, umbrales/carga, proveedores canales/firma/pagos, RPO/RTO, prompt maestro. |
| `08_CIERRE/QA_Y_GATES.md` | Tres tipos de evidencia; pirámide de pruebas; gates G-DEMO, 0A-0H, 1A-4A; pipeline CI. |
| `01_ROADMAP/PLAN_PROGRESIVO.md` | Secuencia E00→E14; dependencias; entregables por épica. |
| `01_ROADMAP/EPICAS_A_HISTORIAS.md` | Mapeo de 30 épicas a historias V11. |

---

## 3. Inventario backend core

### 3.1. Build tools y versiones fijadas

| Herramienta | Versión | Fuente |
|---|---|---|
| Java | 21 | `backend/pom.xml` property `<java.version>21</java.version>` |
| Spring Boot | 3.4.1 | `backend/pom.xml` parent `<version>3.4.1</version>` |
| Maven | 3.9.x por contenedor | `AGENTS.md` y `EMPEZAR_ACA.md`; no hay `mvnw` local |
| PostgreSQL | 16 | `compose.yml` image `postgres:16-alpine` |
| Redis | 7 | `compose.yml` image `redis:7-alpine` |
| MinIO | latest | `compose.yml` image `minio/minio:latest` |
| Keycloak | 26.0 | `compose.yml` image `quay.io/keycloak/keycloak:26.0` |
| jjwt | 0.12.6 | `backend/pom.xml` property `<jjwt.version>0.12.6</jjwt.version>` |
| AWS SDK S2 | 2.29.45 | `backend/pom.xml` property `<awssdk.version>2.29.45</awssdk.version>` |
| Tika | 3.0.0 | `backend/pom.xml` property `<tika.version>3.0.0</tika.version>` |
| PDFBox | 3.0.3 | `backend/pom.xml` direct version |
| springdoc-openapi | 2.7.0 | `backend/pom.xml` property `<springdoc.version>2.7.0</springdoc.version>` |
| Lombok | heredado del parent | `backend/pom.xml` `<optional>true</optional>` |

### 3.2. Controladores REST

| # | Clase | Ruta base |
|---|---|---|
| 1 | `AutenticacionRestController` | `/api/v1/autenticacion` |
| 2 | `DocumentoRestController` | `/api/v1/documentos` |
| 3 | `PlantillaRestController` | `/api/v1/plantillas` |
| 4 | `ExcepcionRestController` | `/api/v1/excepciones` |
| 5 | `OriginalFisicoRestController` | `/api/v1/originales-fisicos` |
| 6 | `GobernanzaRestController` | `/api/v1/gobernanza` |
| 7 | `IntegracionRestController` | `/api/v1/integraciones` |
| 8 | `ExportacionRestController` | `/api/v1/exportaciones` |
| 9 | `ObservabilidadRestController` | `/api/v1/observabilidad` |
| 10 | `KpiRestController` | `/api/v1/kpi` |
| 11 | `AdministracionRestController` | `/api/v1/administracion` |
| 12 | `FederacionRestController` | `/api/v1/federacion` |
| — | `ControladorRest` | base (sin rutas) |
| — | `ErrorHandler` | `@RestControllerAdvice` |

**Conteo:** 12 controladores con ruta + 1 base. El kit reporta 13; se confirma **confirmado por código** contando `ControladorRest` como artefacto de la capa REST (aunque no exponga endpoints).

### 3.3. Servicios

**Servicios top-level:** 45 clases (`AlmacenamientoService` … `VerificadorTokenIdpService`).  
**Subpaquetes:**

| Subpaquete | Clases |
|---|---|
| `servicios/antivirus` | `AntivirusClamAvService`, `AntivirusPermisivoService` (2) |
| `servicios/catalogo` | `CatalogoDocumentalBase` (1) |
| `servicios/conectores` | `FollowConnector` (1) |
| `servicios/jwt` | `TokenService` (1) |
| `servicios/proveedores` | `ConstructorSolicitudGemini`, `ContenidoNoConfiable`, `InstruccionClasificacion`, `InstruccionExtraccion`, `NormalizadorValor`, `ProveedorDeepSeekService`, `ProveedorGeminiService`, `ProveedorSimuladoService`, `RuteadorProveedorService` (9) |

**Total servicios principales:** 45 + 14 = **59** (incluye helpers de proveedor). El kit reportaba 44 + subpaquetes; la cifra real está en línea considerando helpers.

### 3.4. Entidades JPA

Se encontraron **40 entidades**, coincidentes con el baseline reportado:

`Tenant`, `Rol`, `Usuario`, `CuentaServicio`, `PlantillaDocumental`, `VersionPlantilla`, `CampoPlantilla`, `ReglaPlantilla`, `Documento`, `ArchivoDocumento`, `SegmentoDocumento`, `EjecucionExtraccion`, `ValorExtraido`, `EjecucionValidacion`, `HallazgoValidacion`, `CandidatoAsociacion`, `RevisionDocumento`, `CambioCampoRevision`, `ExcepcionDocumental`, `SeguimientoOriginalFisico`, `EventoAuditoria`, `EventoSalida`, `SuscripcionWebhook`, `EntregaWebhook`, `ConfiguracionProveedor`, `PoliticaRetencion`, `ConfiguracionConector`, `ConjuntoPruebaPlantilla`, `CasoPruebaPlantilla`, `EjecucionPruebaPlantilla`, `ResultadoCasoPrueba`, `PoliticaCostoTenant`, `LoteExportacion`, `ItemExportacion`, `AvisoAlmacenamiento`, `ProveedorIdentidad`, `CodigoEmbed`, `TipoPropuesto`, `CorreccionAprendida`, `ReferenciaExterna`.

### 3.5. Repositorios

**39 repositorios Spring Data JPA**, uno por cada entidad principal.

### 3.6. Enumeraciones

**39 enumeraciones**.

### 3.7. Convertidores, modelos y utilidades

| Categoría | Conteo |
|---|---|
| Convertidores | 9 |
| Modelos DTO/Req/Res | 82 |
| Configuración | 17 |
| Clientes HTTP | 3 (Gemini, DeepSeek, Follow) |
| Interfaces (puertos) | 3 (`ProveedorDocumentalIaInt`, `ConectorAsociacionInt`, `AntivirusInt`) |
| Filtros | 3 (`FiltroAutenticacion`, `FiltroCorrelacion`, `FiltroLimiteUso`) |
| Excepciones | 10 |
| Utilidades | 16 |
| Specification builders | 2 |

### 3.8. Migraciones Flyway

Se encontraron **20 migraciones** V1…V20 en `backend/src/main/resources/db/migration/`, coincidentes con el baseline:

V1__esquema_nucleo_documental.sql, V2__bloqueo_optimista_plantillas.sql, V3__conectores_asociacion.sql, V4__segmentacion_documental.sql, V5__escaneo_antivirus.sql, V6__original_fisico.sql, V7__gobernanza_auditoria.sql, V8__retencion_documental.sql, V9__monitor_webhooks.sql, V10__quality_gate_plantilla.sql, V11__observabilidad_costo_tenant.sql, V12__archive_export_center.sql, V13__permiso_exportar_roles_predefinidos.sql, V14__channel_gateway_correo.sql, V15__sso_embed_federado.sql, V16__canal_whatsapp.sql, V17__quitar_canales_entrada.sql, V18__clasificacion_automatica.sql, V19__captura_generica.sql, V20__aprendizaje_correcciones.sql.

### 3.9. Tests Java backend

| Tipo | Clases encontradas | Nota |
|---|---|---|
| Unitarios `*Test` | 8 | No requieren infra |
| Integración `*IT` | 18 | Requieren infra del compose; se saltan si no hay runtime (`PruebaIntegracion`) |
| Helpers de prueba | 5 | `ConectorPruebaService`, `FabricaDatosPrueba`, `PreparadorBuckets`, `PruebaIntegracion`, `ProveedorPruebaService` |

La cifra de **247 tests reportada** no fue reejecutada en esta sesión; se cuenta con 26 clases de test con múltiples métodos cada una. Ver sección 10 sobre verificaciones.

---

## 4. Inventario frontend

### 4.1. Stack y build

| Tecnología | Versión | Uso |
|---|---|---|
| React | 19.2.4 | UI |
| TypeScript | 5.7.2 | Tipado |
| Vite | 8.0.16 | Build / dev |
| React Router DOM | 7.1.1 | Navegación |
| TanStack Query | 5.62.0 | Estado servidor |
| Axios | 1.13.2 | HTTP |
| Tailwind CSS | 4.3.1 | Estilos |
| Prettier | 3.4.2 | Formateo |

Scripts:
- `npm run dev` — puerto 5175, proxy `/api` → `:8090`.
- `npm run build` — `tsc -b && vite build`.
- `npm run format` — Prettier.

### 4.2. Rutas y páginas

| Ruta | Página | UX-ID aproximado |
|---|---|---|
| `/ingresar` | `Ingresar.tsx` | UX-01 Login |
| `/resumen` | `Resumen.tsx` | UX-02 Dashboard |
| `/panel` | `Panel.tsx` | UX-03 Panel |
| `/documentos` | `Documentos.tsx` | UX-04 / UX-05 Lista documentos |
| `/excepciones` | `Excepciones.tsx` | UX-07 Bandeja excepciones |
| `/tipos-propuestos` | `TiposPropuestos.tsx` | UX-08 Gestión de tipos propuestos |
| `/procesos` | `Procesos.tsx` | UX-22 Procesos |
| (detalle no ruteado) | `VisorDocumento.tsx` | UX-06 Visor |

### 4.3. Componentes, contextos y API clientes

| Categoría | Archivos |
|---|---|
| Componentes compartidos | `Disposicion.tsx`, `Estados.tsx`, `Graficos.tsx`, `Iconos.tsx`, `Insignias.tsx`, `Interfaz.tsx`, `Marca.tsx` |
| Contextos | `ProveedorSesion.tsx` |
| API clientes | `cliente.ts`, `documentos.ts`, `excepciones.ts`, `kpi.ts`, `procesos.ts`, `tiposPropuestos.ts` |
| Tipos globales | `tipos/api.ts` |
| Entry points | `principal.tsx`, `Aplicacion.tsx` |

### 4.4. Tests frontend

No se encontraron archivos `*.test.{ts,tsx}` ni configuración de Playwright en el frontend del repo core. El kit y `TODO_MVP0_COMERCIAL.md` indican que los tests E2E son tarea pendiente (P0-11).

---

## 5. Inventario del microservicio workflow

### 5.1. Stack

- Java 21, Spring Boot 3.4.1, Maven por contenedor.
- PostgreSQL 16 (base `nextdocs_workflow` creada por `compose.yml`).
- Flyway con 2 migraciones.
- springdoc-openapi 2.7.0.
- **Autenticación:** provisionalmente `X-Tenant-Id` extraído del header (`ControladorRest.tenantId(HttpServletRequest)`); sin JWT ni Keycloak en este servicio. Esto es un **gap** vs el core.

### 5.2. Controladores REST

| Clase | Ruta base |
|---|---|
| `DefinicionProcesoRestController` | `/api/v1/procesos` |
| `InstanciaProcesoRestController` | `/api/v1/instancias` |
| `TareaProcesoRestController` | `/api/v1/tareas` |
| `ControladorRest` | base |
| `ErrorHandler` | `@RestControllerAdvice` |

Endpoints principales:
- `POST /api/v1/procesos` — crear definición.
- `GET /api/v1/procesos` — listar definiciones.
- `GET /api/v1/procesos/{definicionId}` — obtener definición.
- `GET /api/v1/procesos/{definicionId}/versiones` — listar versiones.
- `POST /api/v1/procesos/{definicionId}/versiones` — nueva versión.
- `PUT /api/v1/procesos/versiones/{versionId}/grafo` — actualizar grafo JSONB.
- `POST /api/v1/procesos/versiones/{versionId}/publicar` — publicar versión.
- `POST /api/v1/instancias` — iniciar instancia.
- `GET /api/v1/instancias` — listar instancias.
- `GET /api/v1/instancias/{instanciaId}` — obtener instancia.
- `POST /api/v1/instancias/{instanciaId}/cancelar` — cancelar instancia.
- `GET /api/v1/tareas` — listar tareas.
- `GET /api/v1/tareas/{tareaId}` — obtener tarea.
- `POST /api/v1/tareas/{tareaId}/completar` — completar tarea.

### 5.3. Servicios, entidades y migraciones

| Categoría | Conteo | Notas |
|---|---|---|
| Servicios | 7 | `DefinicionProcesoService`, `ValidadorGrafoService`, `EjecutorProcesoService`, `ConsultaProcesoService`, `EventoInstanciaService`, `EvaluadorDecisionService`, `ProgramadorVencimientos` |
| Entidades | 5 | `DefinicionProceso`, `VersionProceso`, `InstanciaProceso`, `TareaProceso`, `EventoInstancia` |
| Enumeraciones | 6 | `TipoNodoProceso`, `EstadoVersionProceso`, `VisibilidadProceso`, `EstadoInstanciaProceso`, `EstadoTareaProceso`, `AccionInstancia` |
| Repositorios | 5 | — |
| Convertidores | 5 | — |
| Modelos DTO | 13 | Incluye `WebErrorModel` |
| Migraciones | 2 | `V1__definiciones_proceso.sql`, `V2__runtime_procesos.sql` |
| Tests | 5 clases, 37 casos reportados | `EjecutorProcesoServiceTest`, `ValidadorGrafoTest`, `EvaluadorDecisionTest`, `HashGrafoTest`, `TipoNodoProcesoTest` |

### 5.4. Gaps respecto a N2_WFL.md y Fase C del TODO_MVP0_COMERCIAL

| ID | Desviación | Impacto | Estado |
|---|---|---|---|
| C1 | No hay clave de idempotencia en `iniciar` | Misma clave de inicio repetida crea instancias duplicadas | EXTENDER |
| C2 | No se registra activación lógica persistida por nodo | Replay/reinicio de workers puede duplicar tareas o efectos | EXTENDER |
| C3 | Decisión con fact requerido ausente cae a arista sin condición; debería bloquear | Semántica incorrecta de reglas deterministas | EXTENDER |
| C4 | Validador no rechaza decisiones con dos rutas `true` sin precedencia | Ambigüedad no controlada | EXTENDER |
| C5 | Rechazo de tarea no exige motivo | Auditoría incompleta | EXTENDER |
| C6 | Falta `@Version` en instancia/tarea | Sin bloqueo optimista en concurrencia | EXTENDER |
| C7 | Nodo no valida si capacidad está habilitada para MVP0 | Riesgo de publicar nodos futuros | EXTENDER |
| C8 | Estados `ESPERANDO` y `BLOQUEADA` del vocabulario V11 no están modelados | Diferencia semántica con N2_WFL | EXTENDER/NUEVO |
| C9 | Completar sin asignación ni delegación no devuelve DENEGADO | No hay IAM real en workflow | BLOQUEADO (depende E06) |
| C10 | Pausa sin motivo ni política de cómputo de plazo | SLA no gobernado | EXTENDER |
| C11 | No hay fixtures/quality gate de definición como en plantillas documentales | No se certifica antes de publicar | EXTENDER |
| C12 | Provenance mínimo (owner, tenant autor, fuente, versión origen) incompleto | Trazabilidad de partners débil | EXTENDER |

**Conclusión:** el workflow es **IMPLEMENTADO con desvíos**. No reconstruir; cerrar Fase C antes de agregar funcionalidades nuevas.

---

## 6. Comparación con baseline V11

| Métrica baseline | Valor V11 | Valor real repo | Estado |
|---|---|---|---|
| Pruebas automáticas | 247 reportadas | 26 clases de test (8 unit + 18 IT); cantidad de métodos no ejecutada | **Confirmado parcialmente por código; no verificado por ejecución** |
| Controladores REST | 13 | 12 + 1 base = 13 artefactos REST | **Confirmado por código** |
| Entidades JPA | 40 | 40 | **Confirmado por código** |
| Migraciones Flyway | 20 | 20 (V1..V20) | **Confirmado por código** |
| Servicios | 44 + subpaquetes | ~59 incluyendo helpers | Confirmado por código (mayor porque se cuentan helpers) |
| Workflow implementado | Sí (definiciones, runtime, tareas, timers, bitácora) | Existe pero con desvíos Fase C | **Confirmado parcialmente por código** |
| Frontend navegable | Prototipos V11.2/V11.4 | React funcional con ~8 rutas | **Confirmado por código** |
| Email/WhatsApp retirados | Sí | Migraciones V14/V16 + V17 de eliminación; sin tablas activas | **Confirmado por código** |

**Nota metodológica:** las cifras de V11 vienen del documento del usuario (00_LEEME_PRIMERO.md: "No se recibió el repositorio ni acceso AWS; no se ha verificado esa implementación"). En este discovery se confirmaron por lectura de código fuente, salvo el número exacto de 247 tests que requiere ejecutar `mvn verify`.

---

## 7. Funciones retiradas

El kit y `EMPEZAR_ACA.md` son explícitos: **los canales de entrada email y WhatsApp fueron construidos y luego retirados**. No se propone reactivarlos en el MVP0.

Evidencia en el código:
- Migraciones `V14__channel_gateway_correo.sql` y `V16__canal_whatsapp.sql`.
- Migración `V17__quitar_canales_entrada.sql`.
- No hay entidades ni servicios de correo/WhatsApp en el backend actual.
- El roadmap las reincorpora explícitamente en **MVP1 (E17)** como adapters opcionales, nunca como restauración automática.

**Compromiso:** cualquier propuesta futura de canales debe tratarse como E17/MVP1, con sandbox real y contrato; no reabrir código retirado en E00-E14.

---

## 8. Decisiones pendientes D01-D11

| ID | Decisión | Propuesta / fuente | Responsable | Estado en esta sesión | Bloquea |
|---|---|---|---|---|---|
| D01 | Repo, ramas, comandos, versiones reales | Preservar contenedor Maven; sin `mvnw` local; CI con Maven 3.9.9 | CTO | **Resuelto parcialmente** por `AGENTS.md` y `compose.yml` | Cambio sobre código real |
| D02 | Motor AWS, cuenta, región, red | PostgreSQL 16 local/emulado; RDS propuesto; sin cuenta AWS confirmada | Ops | **Pendiente / bloqueo** | Infra/migración productiva |
| D03 | Framework Java | Spring Boot 3.4.1 / Java 21 confirmado | CTO | **Resuelto** | Bootstrap backend nuevo |
| D04 | Ratificar MVP0 ampliado | G-DEMO separado de 0H; MVP0 Comercial = E00-E14 | PO | **Pendiente** (requiere ratificación PO) | Aceptación comercial |
| D05 | Auth frontend, issuer/audience | Keycloak 26.0 reportado; workflow aún usa `X-Tenant-Id` provisional | Security | **Parcialmente resuelto** (core) / **Pendiente** (workflow) | Integración login real |
| D06 | Contratos Follow/CIMA/Valid360 | `FollowConnector` existe pero endpoints/keys no suministrados | Integraciones | **Pendiente / bloqueo** | Certificación externa |
| D07 | Catálogo catorce bases COMEX | Discrepancia: diez bases direccionales + cuatro overlays vs catorce bases exigidas; ver `DELTA_COMEX_V11.md` | PO COMEX | **Pendiente / bloqueo** | Gate 0F |
| D08 | Umbrales, carga, presupuesto | Valores de prueba no son defaults vigentes | PO + Ops | **Pendiente** | Gate de capacidad/costo |
| D09 | Proveedor canales/firma/pagos | Elección no confirmada; usar puertos | PO + CTO | **Pendiente** | Gates MVP1/MVP2 externos |
| D10 | RPO/RTO, residencia, retención | Según contrato concreto, sin SLA inventado | Cliente + Ops | **Pendiente** | Gate 4A |
| D11 | Prompt maestro completo | Texto separado sólo tenía título; se usa estándar V4 y V11 | PO | **Pendiente** (evidencia textual) | Afirmar cumplimiento literal |

**Recomendación:** cerrar D01, D03 y D05-core en E00; dejar D02, D06, D07, D08, D09, D10, D11 con owners y fechas de seguimiento sin bloquear tareas independientes.

---

## 9. Próximo corte vertical recomendado

El plan maestro `TODO_MVP0_COMERCIAL.md` ya propone el orden correcto. El próximo corte vertical debe ser:

1. **P0-02 · Validar el core documental existente** (E01 · gate G-DEMO)
   - Recorrido humano punta a punta en el navegador.
   - Confirmar que el visor muestra datos de la captura genérica.
   - Cargar 10–15 documentos reales.
   - Rotar la clave de Gemini y revocar la anterior.
   - **Por qué primero:** si el core no funciona con datos reales, no importa el resto.

2. **Fase C · Conformidad V11 del workflow** (E02-E04 · gates 0B/0C)
   - Cerrar desvíos C1..C12 antes de agregar nodos o Canvas.
   - Empezar por C1 (idempotencia de inicio) y C6 (bloqueo optimista), porque afectan toda la arquitectura de instancias.
   - **Por qué en paralelo con P0-02:** P0-03 es el bloque largo del MVP0; no conviene esperar a que termine P0-02 para arrancarlo, pero tampoco iniciar sobre un runtime que contradice la spec.

3. **P0-07 · Partner Foundation** (E07 · gates 0B/0D)
   - Puede arrancar en paralelo con P0-02 y Fase C porque no depende del runtime de workflow.
   - Modelar `partner_organization`, `delegated_access_grant` y provenance desde el día uno.

**Lo que NO debe hacerse ahora:**
- Reconstruir canales email/WhatsApp (MVP1).
- Implementar Canvas completo o firma (MVP1).
- Agregar nodos no habilitados para MVP0 (firma, automatización general).
- Acoplar el core a tablas internas de Follow.

---

## 10. Verificaciones locales ejecutadas

Se ejecutaron verificaciones de compilación y conteo en la sesión actual. Los resultados completos, con los comandos exactos, se registran en `docs/verificaciones/e00-verificacion-local.md`.

Resumen de resultados:

|| Verificación | Resultado |
|---|---|---|
|| Archivos Java backend (core) | 336 |
|| Clases de test Java backend (8 `*Test` + 18 `*IT`) | 26 |
|| Archivos Java workflow | 53 |
|| Clases de test Java workflow | 5 |
|| Migraciones Flyway backend | 20 |
|| Migraciones Flyway workflow | 2 |
|| Controladores REST core | 12 + `ControladorRest` base |
|| `npm run build` (frontend) | **PASS** (`tsc -b && vite build`) |
|| `mvn compile -DskipTests` (core) | **PASS** |
|| `mvn compile -DskipTests` (workflow) | **PASS** |

**Observación:** los 247 tests reportados requieren ejecutar `mvn verify` con infraestructura levantada (PostgreSQL/Redis/MinIO/Keycloak). No se ejecutaron en esta verificación local.



---

## 11. Referencias cruzadas

- `docs/adr/0001-baseline.md` — ADR con decisiones de baseline.
- `docs/implementation-v11/matriz-requisito-implementacion-gap.md` — matriz de historias vs estado real.
- `docs/TODO_MVP0_COMERCIAL.md` — plan de cierre MVP0 Comercial (rebaseline 06/09/2026).
- `docs/EMPEZAR_ACA.md` — reglas no negociables y trampas de dominio.
- `docs/implementation-v11/NEXT_DOC_AI_V11_4_IMPLEMENTACION/11_BASELINE/backend_20260907/10-INVENTARIO-CODIGO-FUENTE.md` — inventario del kit.
