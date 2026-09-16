# TO-DO — MVP0 Comercial

Plan de cierre del **MVP0 Comercial**. Este documento reordena el plan maestro de
[`TODO.md`](TODO.md) en función del rebaseline de producto. La especificación funcional completa
vive fuera del repo (Base V7); acá queda el tablero de trabajo.

---

## 0. Guía normativa: paquete V11 (precedencia máxima)

> **Rebaseline 07/09/2026.** El paquete **`NEXT_DOC_AI_V11_COMPLETA/`** (versionado en este repo)
> es la guía normativa de este tablero: **no se construye nada que contradiga la V11**. Los
> documentos `MVP/` (análisis de desvíos del 06/09) quedan como antecedente; si algo difiere,
> manda la V11.

- **N0-V11-SISTEMA**: 17 módulos, backlog **E00–E29**, precedencia N0 → N1 → N2 → N3.
- **MVP0 Comercial = épicas E00–E14**, gates **0A–0H** (más `G-DEMO` para el core existente).
- Cada N3 trae **32 casos CP/QA verificables** (módulo); la matriz `QA_MAESTRA_V11.json` lista los
  **544 casos** con precondición/acción/resultado; `BACKLOG_V11.json` las **136 historias**. Estado
  oficial de las matrices: `ESPECIFICACION_NO_EJECUTADA` — los casos son la especificación, no
  pruebas aprobadas.
- Regla de oro del paquete (RG-06): la evidencia de aceptación se obtiene **ejecutando** los casos.

### Mapeo de lo ya construido contra la V11

| Nuestro PR | Épica V11 | Gate | Estado |
|---|---|---|---|
| workflow #1 (Definition) | E02 Schema de proceso/template | 0B | Mayormente conforme |
| workflow #2 (Runtime) | E03 Dominio + E04 Runtime | 0B/0C | **Con desvíos** (ver Fase C) |
| #6 (compose/infra) | REL | 0G | Conforme |
| #7 (Studio guiado) | E05 Studio guiado | 0D | **Con desvíos** (ver Fase C) |

### Fase C — Conformidad V11 de lo ya construido (arranca antes que todo lo nuevo)

Desvíos detectados al contrastar el código contra `N3-MVP0-V11-WFL` y `N3-MVP0-V11-STU`:

- [x] **C1 · Idempotencia de inicio** (CP-WFL-04): misma clave de inicio repetida → una sola
  instancia con la misma referencia. Hoy no existe clave de idempotencia en `iniciar`.
- [x] **C2 · Activación lógica persistida** (RE-WFL-03, CP-WFL-09..12): registrar cada activación
  de nodo; replay/reinicio/concurrencia de workers no duplica tareas ni efectos confirmados.
- [x] **C3 · Decisión sin dato faltante** (CP-WFL-14, RE-STU-03): un fact requerido ausente
  **bloquea** la decisión ("ninguna ruta asumida"). Hoy caemos a la arista sin condición.
- [x] **C4 · Ambigüedad de condiciones** (CP-WFL-07): el validador rechaza decisiones con dos
  rutas true sin precedencia. Hoy sólo exigimos ≥2 salidas.
- [x] **C5 · Rechazo exige motivo** (CP-WFL-18): completar con decisión `RECHAZADO` y motivo
  vacío → no se guarda, la tarea queda abierta. Hoy el motivo es opcional.
- [x] **C6 · Bloqueo optimista** (CP-WFL-20): dos revisores sobre la misma revisión → uno gana,
  el otro recibe CONFLICTO. Falta `@Version` en instancia/tarea (el patrón ya existe en el core,
  migración `V2` de plantillas).
- [x] **C7 · Catálogo de nodos MVP0 acotado** (RE-WFL-02, CP-WFL-08): el MVP0 admite inicio,
  requisito documental, regla determinista, decisión exclusiva, tarea humana, espera/SLA y fin.
  Publicar con un nodo de capacidad no habilitada (p. ej. firma) → bloqueado.
- [x] **C8 · Estados del vocabulario V11** (N3.9): mapear `ESPERANDO` (tarea externa activa) y
  `BLOQUEADA` (regla bloqueante) sin renombrar datos por etiqueta.
- [x] **C9 · Actor autorizado en la tarea** (CP-WFL-19): completar sin asignación ni delegación →
  DENEGADO. Depende del IAM real (E06); con el placeholder `X-Tenant-Id` queda anotado, no cerrado.
- [x] **C10 · Pausa con motivo y reloj del tenant** (RE-WFL-06): pausar exige motivo y política
  explícita de cómputo del plazo; vencimientos con zona del tenant.
- [x] **C11 · Fixtures antes de publicar** (RE-STU-05): la versión candidata ejecuta fixtures
  representativos y casos negativos ligados al hash de la definición; una edición invalida la
  prueba anterior. El patrón existe en el core documental (quality gate); falta en el workflow.
- [x] **C12 · Provenance mínimo en la definición** (RE-STU-01): owner, tenant autor, fuente y
  versión origen desde la creación (se completa con E07).

Lo que la V11 confirma de nuestro diseño: inicio versionado con hash (RE-WFL-01 ✅), publicación
inmutable (RE-STU-06 ✅), instancia v1 sigue en v1 tras publicar v2 (CP-WFL-03 ✅), cancelación
invalida pendientes con motivo (CP-WFL-27 ✅), bitácora inalterable (RG-02 ✅).

---

**Precedencia (P0-01).** En alcance manda este documento. `TODO.md` queda como registro de lo
construido (Fases 1 y 2, "MVP0 técnico"): sus tareas 21–27 se reabsorben acá con numeración nueva;
sus Fases 3 y 4 se corresponden con los niveles MVP1–MVP4 de la nueva estructura.

---

## 1. La estructura de producto (cinco niveles)

| Nivel | Objetivo | Incluye |
|---|---|---|
| **MVP0 Comercial** | Vender y operar | IA Docs + Workflow Core + Studio guiado + terceros + IA Supervisora v0 + COMEX/Follow + foundation de partners |
| MVP1 Studio & Partners | Configurar y escalar | Process Canvas, Partner Workspace, canales, firma, experiencia avanzada |
| MVP2 Marketplace | Escalar ecosistema | Catálogo, certificación, licencias, compra/instalación, updates, revenue share |
| MVP3 IA Avanzada | Diferenciación | Sentinel, Simulation Lab, Process Copilot |
| MVP4 Enterprise | Grandes cuentas | Private cloud, DR, residencia, gobierno, SLA enterprise |

**Qué NO entra al MVP0:** Canvas completo, marketplace con cobro, Copilot, firma (P1). El MVP0
no es un BPM/RPA universal: procesos **documentales**, con adapters para acciones externas.

---

## 2. Punto de partida: qué hay hoy contra los nueve módulos del MVP0

El "MVP0 técnico" actual aporta los módulos documentales completos y verificados; la brecha está
en procesos, terceros, partners y contenido COMEX.

| Módulo | Estado hoy | Qué falta |
|---|---|---|
| **A. Docs Core** | ✅ Completo: ingesta segura, clasificación automática, captura genérica, extracción Gemini/DeepSeek, validación, revisión, aprendizaje, excepciones, auditoría, API/webhooks | Documentación real de un cliente (P0-02 punto 3) |
| **B. Workflow Core** | ✅ Motor completo: 15 tipos de nodo (los 12 del MVP0 + PARALELO/UNION/FIRMA), ejecución paralela real con join persistente e idempotente, disparadores por evento y por agenda, firma con documento exigido, notificaciones por webhook vía core, simulador sin persistencia, instancia fijada a versión, SLA de plantilla y de nodo | Conectores productivos e IAM real |
| **C. Studio guiado** | ✅ Canvas completo: paleta, panel por nodo, deshacer/rehacer, minimapa, avisos de diseño, simulador, duplicar plantilla | — |
| **D. Colaboración externa** | ✅ Cuenta externa, enlace de acción seguro (token, scopes, expiración, usos máximos) y **portal público `/externo/{token}`** con aprobar/rechazar y **carga de documentos** proxied al repositorio del tenant | — |
| **E. IA Supervisora v0** | ✅ Reglas configurables por plantilla con umbral y operador, hallazgos, bloqueo de instancia y pantalla de administración | Controles cross-doc y de secuencia — **fuera del MVP0** (§6) |
| **F. Biblioteca COMEX/Follow** | ✅ Catálogo argentino de 11 tipos documentales y plantillas de proceso COMEX con fixtures | Overlays por vertical |
| **G. Partner Foundation** | ✅ Partner org, delegated grants, ownership/provenance/fork/install + pantallas de partners y marketplace | Certificación comercial del marketplace — **fuera del MVP0** (§6) |
| **H. Gobernanza & KPI** | ✅ Auditoría, reconstrucción de decisión, 11 KPI documentales y 14 de proceso con drill-down, export con manifiesto, costo por tenant | — |
| **I. Integración** | ✅ API REST, webhooks HMAC, SSO, FollowConnector de matching | Context Contract + Template Recommender + `subject_ref` genérico (P0-10) |

**Lo que ya está es la mitad difícil del MVP0:** Docs Core con 121 pruebas unitarias y 175 de
integración contra infraestructura real, el motor de procesos con 180, 90+ pruebas de navegador,
versionado de plantillas con quality gate, y la infra de despliegue. El
cierre es construir el eje de procesos sobre esa base sin desestabilizarla.

---

## 3. Tablero de tareas

Fuente: matriz de desvíos §12 del análisis + **backlog V11** (épica/gate entre paréntesis). La
columna "en el repo" ancla cada ítem al estado actual del código.

### P0 — cierra el MVP0 Comercial

- [x] **P0-01 · Baseline único y nomenclatura** (E00 · gate 0A) · *Esfuerzo: S*
  Este documento es el baseline. Pase in-repo hecho el 16/09/2026: `EMPEZAR_ACA.md` ya remite
  a este tablero como plan maestro (el `TODO.md` viejo queda como registro de Fases 1–2), y
  `discovery-v11.md` y `frontend-figma.md` quedaron marcados superseded-for-scope
  (`implementation-v11/` vive fuera del control de versiones). Pendiente sólo la contraparte
  en la Base V7 (fuera del repo, acción del PO) y la aceptación formal del ADR 0001.

- [ ] **P0-02 · Cerrar la validación del MVP0 técnico** (E01 · gate G-DEMO) · *Esfuerzo: S*
  Tres de los cuatro puntos cerrados; detalle en [P0_02_VALIDACION.md](P0_02_VALIDACION.md).
  (1) Recorrido punta a punta automatizado con una captura por paso · **hecho**.
  (2) El visor muestra la captura genérica, con aviso propio y marca en la bandeja · **confirmado**.
  (3) 15 documentos cargados con Gemini 2.5 Flash real, 15/15 con el tipo correcto · **hecho con
  dataset propio**; falta documentación real de un cliente.
  (4) Rotar la clave de Gemini · **pendiente, es la única que bloquea el gate**.
  De acá salió el hallazgo de que la validación del dígito verificador del CUIT no existía.

- [x] **P0-03 · Workflow Definition + Runtime** (E02+E03+E04 · gates 0B/0C) · *Esfuerzo: L*
  Implementado en el microservicio `workflow`: 8 controladores, 48 endpoints, 16 entidades,
  11 migraciones y 107 pruebas. Los 12 nodos del MVP0 están en `TipoNodoProceso`
  (INICIO, FIN, SOLICITUD_DOCUMENTO, FORMULARIO, VALIDACION_IA, REVISION_HUMANA, DECISION,
  TAREA_EXTERNA, NOTIFICACION, TEMPORIZADOR, ACCION_API, SUBPROCESO). La instancia queda fijada
  a la versión con la que nació: publicar v2 no mueve las instancias de v1.
  El SLA se hereda de la plantilla y el nodo lo pisa si define el suyo.

- [x] **P0-04 · Studio guiado** (E05 · gate 0D) · *Esfuerzo: M/L* · *Depende de: P0-03*
  Canvas como superficie única de edición: paleta de los 15 tipos de nodo, conexión arrastrando,
  panel de configuración por nodo (incl. disparadores del INICIO, documentos esperados de
  TAREA_EXTERNA y documento requerido de FIRMA), avisos de diseño en el nodo y el panel,
  deshacer/rehacer con Ctrl+Z, minimapa navegable, guardar borrador, nueva versión, duplicar
  plantilla (`/clonar`), probar con instancia real, simular con datos de ejemplo y publicar con
  versionado.

- [x] **P0-05 · External Collaboration** (E06 · gate 0D) · *Esfuerzo: M* · *Depende de: P0-03*
  Cuenta externa + enlace de accion seguro con token, scopes, expiracion y usos maximos. Implementado en workflow.
  Cuenta externa limitada (sólo tareas/documentos/estado asignados) + Secure Action Link con
  scope y TTL para cargas/aprobaciones puntuales. Aislamiento cross-tenant verificado por test.
  *En el repo:* viejas tareas 23 (Task Service y Action Center) y 24 (Secure Action Links).

- [x] **P0-06 · IA Supervisora v0** (E08 · gate 0E) · *Esfuerzo: M/L* · *Depende de: P0-03*
  Reglas supervisora, hallazgos con accion/severidad/estado, evaluacion de instancia y resolucion. Capa de proceso implementada en workflow.
  Supervisor acotado por la plantilla, no agente autónomo. Controles: completitud, calidad IA
  (umbral de confidence), cross-document (identificadores/ítems/cantidades/Incoterm/parties),
  secuencia, SLA, duplicados/versiones, regla externa. Acciones configurables: solicitar /
  advertir / bloquear / review task + severidad.
  **Guardrail no negociable:** la IA propone; las reglas que bloquean o publican están versionadas
  y las aprueba un usuario autorizado. Todo queda auditado. La evolución a auto-propuestas es
  MVP3 (Sentinel), no acá.
  *En el repo:* el motor de hallazgos/severidades de `ValidacionDocumentalService` y el centro de
  excepciones con SLA ya existen para lo documental; la Supervisora v0 es la capa de proceso que
  reacciona a los mismos hechos.

- [x] **P0-07 · Partner Foundation** (E07 · gates 0B/0D) · *Esfuerzo: M*
  Organizaciones partner y delegaciones de scopes con expiración implementadas en el microservicio workflow.
  `partner_organization`, `delegated_access_grant` (partner_user → client_tenant + scopes +
  expiración + aprobador), roles Partner Consultant y Partner Publisher. Delegación explícita por
  tenant/scope: sin superusuario transversal, sin visibilidad de otros clientes.
  *En el repo:* nuevo bounded context con tenant-filtering como el resto; migración nueva (V21+).

- [x] **P0-08 · Template marketplace-ready** (E07 · gate 0B) · *Esfuerzo: M* · *Depende de: P0-03, P0-07*
  Tablas de instalacion, sobreescritura de overlays, publicacion y terminos comerciales; endpoints de instalacion y overlay. Modelado en workflow.
  `template_definition` (owner_type/owner_org_id/visibility), `template_version` (semver, manifest,
  hash), `template_provenance` (created_from/forked_from/overlay_of), `template_installation`
  (pin/update policy), `tenant_template_override`, `marketplace_listing` y `commercial_terms`
  **dormantes** en MVP0. Regla IP: una plantilla publicada no se copia ni sobreescribe; el cliente
  instala una versión y sus cambios son fork/overlay local; la actualización del publisher crea
  versión nueva y el cliente decide cuándo adoptar.
  *En el repo:* modelar esto desde el día uno en el dominio de plantillas de proceso; agregarlo
  después rompe trazabilidad y propiedad intelectual de los partners.

- [x] **P0-09 · Biblioteca COMEX** (E09 · gate 0F) · *Esfuerzo: M* · *Depende de: P0-03, P0-04*
  Fixture con 14 plantillas base (maritimo FCL/LCL, aereo, terrestre FTL/LTL) + 4 multimodales, grafo COMEX estandar y documentos base. Condicional via `nextdocs.workflow.comex.habilitado`.
  Las 10 plantillas base V7 (EX/IM × Mar FCL/LCL, Air, Road FTL/LTL) + 4 multimodales nuevas
  (EX-MM-ROAD-SEA, IM-MM-SEA-ROAD, EX-MM-ROAD-AIR, IM-MM-AIR-ROAD) + overlays (REEFER, DG, FOOD,
  OVERSIZE, CONT-RETURN, SP03-VOLUME, RECOLLECTION, POD/LASTMILE) + fixtures + role maps.
  Catálogo documental y manifest estándar según el anexo de `MVP/`. Los nombres/requisitos
  aduaneros se parametrizan por país; los IDs de tipos de carga Follow vienen por Connector, no
  se duplican maestros.

- [x] **P0-10 · Follow Context/Template Recommender** (E10 · gate 0F) · *Esfuerzo: M* · *Depende de: P0-09*
  Contrato canónico de contexto (scope/direction/mode/logistic_unit/cargo_profile/follow_operation/
  flags/subject_ref), `POST /template-recommendations`, `POST /workflow-instances`, eventos
  `document.*`/`task.*`/`workflow.completed`. Follow no hardcodea nombres de plantilla: envía
  contexto y NEXT DOC AI recomienda la composición.
  *En el repo:* `ConectorAsociacionInt` ya existe como el lugar de los adaptadores; el Connector
  es el que traduce IDs canónicos de Follow al manifest. Prohibido acoplar el core a tablas
  internas de Follow.

- [x] **P0-11 · E2E y seguridad** (E12 · gate 0G) · *Esfuerzo: M*
  Playwright instalado con proyecto `api`, config y smoke tests. Scaffolding listo para recorridos
  de Docs + Flow + tercero + partner; workflow CI en `.github/workflows/e2e.yml`.

- [x] **P0-12 · Dataset real** (E01/E09 · gate 0G) · *Esfuerzo: M*
  Suite `PilotoComexTest` con escenarios de plantilla COMEX: happy path, missing doc, low confidence,
  formato invalido, cross-doc mismatch montos, incoterm no permitido, documento vencido, nueva
  version requiere campo adicional. Core `mvn verify` OK.
  Se le suma el dataset cargable de 15 documentos con respuesta conocida
  (`scripts/generar-dataset.mjs` y `scripts/cargar-dataset.mjs`, ver
  [DATASET_PRUEBA.md](DATASET_PRUEBA.md)), corrido contra Gemini real con 15/15 de acierto.
  **Sigue faltando documentación real de un cliente**: todo lo anterior lo armamos nosotros.

- [~] **P0-13 · Load y operaciones** (E13 · gate 0G) · *Esfuerzo: M*
  Hecho y verificado: histograma de `http.server.requests` con p50/p95/p99 y SLO en el core,
  gauges de profundidad de la cola de extracción (`nextdocs.cola.extraccion.*`), scraper del core
  resuelto con cuenta de servicio de alcance mínimo (leer métricas sí, publicar eventos no,
  revocación inmediata), límite de peticiones verificado devolviendo 429, línea base medida
  (p95=13 ms en bandeja sobre QA) y respaldo/restauración ejercitados con pg_dump + pg_restore
  contra una base descartable con integridad referencial confirmada. Runbook actualizado.
  **Lo que queda es operativo, no de código:** alertas reales en un Prometheus desplegado, cron
  del `respaldo.sh` con retención y rotar la clave de Gemini (procedimiento ya en el runbook).

- [x] **P0-14 · KPI operativo de procesos** (E11 · gate 0H) — nueva en V11
  Endpoints `/api/v1/kpi-procesos` y `/api/v1/kpi-procesos/poblacion` con **14 indicadores** del
  workflow, visibles en la bandeja de instancias del portal con selector de 7/30/90 días y tabla
  de cuellos de botella. Población, fórmulas visibles y drill-down sobre `GOV`, con la misma
  invariante del `KpiIT` del core: el número es el tamaño exacto de su población.
  Esa invariante estaba rota para `tareasEnPlazo` por un typo en el switch del drilldown, que
  devolvía población vacía en silencio; corregido con una prueba que la exige para todos los
  indicadores de tareas.
- [~] **P0-15 · Release comercial** (E14 · gate 0H) — nueva en V11
  Demo de proceso completa, runbook, soporte, rollback y acta de salida (0A–0G con evidencia).
  El runbook está en [RUNBOOK_OPERACION.md](RUNBOOK_OPERACION.md), el rollback está documentado
  en `DESPLIEGUE.md` (imagen previa `nextdocs-ia-backup` + `respaldo.sh restaurar`) y el acta
  de salida con la evidencia de cada gate está en
  [verificaciones/e14-acta-salida.md](verificaciones/e14-acta-salida.md).
  **Lo que queda es del operador:** rotar la clave de Gemini, cargar documentación real de un
  cliente y cerrar el gate con esa demo final.

### P1 — apenas cierre el MVP0 (hacia MVP1)

- [ ] **P1-01 · Email process intake** — reactivar el alias dedicado por tenant/proceso para
  COMEX. El código retirado se recupera de git (`0b681d3`, `a50dd7c`); la decisión de retirarlo
  se mantiene para el portal: entra como configuración de tenant, no como sección operativa.
- [x] **P1-02 · Firma por adapter** — circuito real implementado: `ConectorFirmaInt` con
  adapter `DOCUMENSO` (API v2 envelopes), webhook `POST /api/v1/firma/webhook` autenticado
  por `X-Documenso-Secret` que completa/rechaza la tarea y archiva el PDF firmado en el
  core (workflow PR #29, monorepo PR #65). Pendiente: credenciales reales de Documenso y
  aplicar el conf de nginx en el servidor. Legale/Docusign quedan como adapters futuros
  si un cliente exige firma digital argentina.
- [ ] **P1-03 · Canvas avanzado** — grafo visual sobre el mismo schema del Studio guiado (vieja
  tarea 26 completa). MVP1.

---

## 4. Orden de ejecución sugerido

```
FASE C (conformidad V11 de lo ya construido) ── va primero: C1..C12
P0-01 ─┬─> P0-02 (validar lo que existe)
       └─> P0-03 (Workflow Definition + Runtime) ─┬─> P0-04 (Studio guiado) ──> P0-09 (COMEX) ──> P0-10 (Follow)
                                                   ├─> P0-05 (terceros)
                                                   └─> P0-06 (Supervisora v0)
P0-07 (Partner foundation) ──> P0-08 (marketplace-ready)
P0-11 (Playwright) ── cierra al final, cubriendo Docs + Flow + tercero + partner
P0-12 (dataset) y P0-13 (load/ops) ── en paralelo con el tramo final
```

**Primera entrega:** la Fase C. Nada de lo nuevo debe ampliarse sobre un runtime que contradice
la spec (la V11 es la guía, no el análisis de desvíos).
P0-03 es el bloque largo: arrancarlo apenas se valida lo existente. P0-07 no depende de P0-03 y
puede avanzar en paralelo.

**Cómo se reestructura respecto del plan viejo:** la Fase 3 de `TODO.md` (21–27) ya era el eje
correcto; el rebaseline lo que hace es (a) subirla al MVP0, (b) partirla en Definition/Runtime (03),
Studio sin canvas (04) y terceros (05), (c) agregar lo que el plan viejo no tenía —Supervisora v0,
partner foundation, marketplace-ready, COMEX, Follow Recommender, QA/datos/ops— y (d) empujar
firma y canvas a MVP1.

---

## 5. Reglas del repo que aplican a todo lo nuevo

Las convenciones de `EMPEZAR_ACA.md` siguen vigentes y son particularmente sensibles acá:

- **Workflow es microservicio aparte.** `workflow` y `docvance-ai` no se fusionan al core. Ningún
  servicio del core importa modelos de workflow; los adaptadores van detrás de interfaces propias
  (patrón `ConectorAsociacionInt`). Apagar el motor de procesos no puede romper captura,
  extracción, validación ni integración documental.
- **Migraciones Flyway nuevas** (V21+) para cada dominio nuevo; nunca editar una aplicada. Un
  permiso nuevo no se aplica solo a roles existentes: `INSERT ... WHERE NOT EXISTS` como `V13`.
- **Entidades → modelos en el borde HTTP:** los servicios devuelven modelos dentro del
  `@Transactional` (`LazyInitializationException`, pasó tres veces).
- **Repositorios siempre filtran por tenant**; la delegación de partner agrega una capa más de
  scoping (por tenant + por grant), no una excepción al filtrado.
- **Enums persistidos son contrato de datos:** ninguna constante nueva "provisional" que después
  haya que borrar.
- **Ninguna acción de retención borra auditoría; la auditoría del proceso cubre la misma
  invariante** (línea de decisión reconstruible: quién, cuándo, qué regla, qué versión).
- **Confianza de IA nunca aprueba sola** — la Supervisora v0 hereda la regla: umbral de
  confidence genera review task, jamás aprobación.
- **QA codes:** los criterios de aceptación vienen de la Base V7 (N3) y del análisis de desvíos;
  no se inventan.

---

## 6. Criterios de salida del MVP0 Comercial

### Qué entra y qué no

El checklist original (§13 del análisis de desvíos) describía un producto más ancho que el que
hoy es demostrable. Se acota el MVP0 al **núcleo documental más el motor de procesos**, que es lo
que está construido, verificado y se puede mostrar.

El criterio no es nuevo: P0-08 ya declaraba `marketplace_listing` y `commercial_terms`
**dormantes en MVP0** — modelados desde el día uno para no romper trazabilidad después, sin
superficie hasta que haga falta. Se aplica el mismo criterio a partners y colaboración externa,
que están en la misma situación: backend completo, cero pantallas.

| Fuera del MVP0 | Estado | Por qué se pospone |
|---|---|---|
| Marketplace de plantillas | tablas modeladas, dormantes | Ya estaba declarado así en P0-08 |
| Partners: pantalla de delegaciones | 7 endpoints, sin UI | Sin partners reales todavía; la delegación se configura por API |
| Colaboración externa: portal del tercero | 7 endpoints, sin UI | El enlace de acción seguro funciona; falta la pantalla que lo consume |
| Supervisora cross-document | motor de umbral sobre un dato | Comparar entre documentos es otro motor, no un ajuste |
| Duplicar plantilla desde el portal | `POST /procesos/{id}/clonar` sin consumir | Único hueco del Studio; entra si se decide cerrarlo antes |

Nada de esto se borra ni se esconde: el backend queda donde está, probado, y la superficie se
agrega cuando exista la demanda que la justifique. Lo que **no** se hace es dar por cumplido un
criterio porque el endpoint existe.

### Checklist acotado

- [x] Un documento entra por el portal, el sistema detecta su tipo, extrae los campos, valida y abre excepción accionable cuando algo no cierra.
- [x] El visor muestra los datos también cuando el documento cayó al esquema genérico, y lo señala.
- [x] Una corrección humana viaja con la decisión y queda auditada.
- [x] Un proceso se diseña en el Studio, se prueba y se publica con versionado.
- [x] Una instancia iniciada en v1 continúa en v1 aunque se publique v2.
- [x] Las tareas se resuelven desde la bandeja, con SLA heredado de la plantilla o propio del nodo.
- [x] La IA Supervisora evalúa reglas configurables por plantilla, abre hallazgos y bloquea la instancia cuando corresponde, con pantalla propia de administración.
- [x] Los indicadores de proceso son visibles y su drill-down devuelve la población exacta.
- [x] Follow solicita recomendación por contexto y recibe la plantilla sin hardcodear su nombre.
- [x] Playwright cubre los recorridos críticos del portal: 67 pruebas en tres suites.
- [x] Existe línea base de rendimiento; backup y restore verificados sobre las dos mitades.
- [ ] **Rotar la clave de Gemini usada en desarrollo.**
- [ ] **Cargar documentación real de un cliente y comprobar que produce salidas útiles o excepciones accionables.**

Quedan dos, y ninguno es desarrollo. El segundo es el único que todavía puede cambiar la
conclusión: todo lo cargado hasta ahora lo armamos nosotros.

### Lo que queda para producción, no para la demo

Del runbook: exponer las métricas del core al scraper sin romper la autenticación, límite de
peticiones en el motor de procesos si sale de la red interna, alertas configuradas contra los
umbrales, y respaldo automático programado con su verificación.

---

## 7. Riesgos a vigilar durante el cierre

| Riesgo | Control |
|---|---|
| Scope creep hacia BPM/RPA universal | Nodos limitados a procesos documentales; acciones externas por adapter |
| Partner con acceso global a todos sus clientes | Grants explícitos por tenant/scope/expiración, con aprobador |
| Cliente modifica plantilla de partner y se pierde el origen | Version pin + fork/overlay + provenance desde el día uno |
| IA Supervisora autónoma | Policy versionada; bloqueos y publicaciones gobernados; audit de todo |
| Explosión de plantillas COMEX | Base + overlays + subprocesos + Recommender, nunca una plantilla por combinación |
| Hardcode de tablas/IDs de Follow | Connector traduce a contrato canónico; el core no conoce maestros Follow |
| Demo ≠ producción | Gate de salida ampliado: E2E, load, dataset y ops son P0, no P1 |
