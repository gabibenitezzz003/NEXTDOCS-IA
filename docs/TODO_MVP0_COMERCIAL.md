# TO-DO — MVP0 Comercial

Plan de cierre del **MVP0 Comercial**, con baseline del 6 de septiembre de 2026. Este documento
reordena el plan maestro de [`TODO.md`](TODO.md) en función del rebaseline de producto definido en
`MVP/NEXT_DOC_AI_Analisis_Desvios_Cierre_MVP0_Comercial_2026-09-06.docx` y su anexo de biblioteca
COMEX. La especificación funcional completa vive fuera del repo (Base V7); acá queda el tablero
de trabajo.

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
| **A. Docs Core** | ✅ Completo: ingesta segura, clasificación automática, captura genérica, extracción Gemini/DeepSeek, validación, revisión, aprendizaje, excepciones, auditoría, API/webhooks | Validación E2E en navegador y dataset real (P0-02, P0-12) |
| **B. Workflow Core** | ❌ No empezado | Todo (P0-03). Ya existía como tareas 21–22 del plan viejo |
| **C. Studio guiado** | ❌ Las plantillas se sacaron del portal | Editor lista/timeline sobre el mismo JSON/schema del futuro Canvas (P0-04) |
| **D. Colaboración externa** | ⚠️ SSO/embed funciona (16) | External User con cuenta limitada + Secure Action Link (P0-05, viejas 23–24) |
| **E. IA Supervisora v0** | ❌ Sólo excepciones documentales | Controles configurables por plantilla: completitud, confidence, cross-doc, secuencia, SLA, versiones (P0-06) |
| **F. Biblioteca COMEX/Follow** | ❌ Catálogo sembrado argentino de 10 tipos documentales (remito, factura...); sin plantillas de proceso | 10 plantillas V7 + 4 multimodales + overlays + fixtures (P0-09) |
| **G. Partner Foundation** | ❌ No modelado | partner org, delegated grants, ownership/provenance/fork/install (P0-07, P0-08) |
| **H. Gobernanza & KPI** | ✅ Auditoría, reconstrucción de decisión, 11 KPI con drill-down, export con manifiesto, costo por tenant | KPI de procesos y SLA una vez exista Workflow |
| **I. Integración** | ✅ API REST, webhooks HMAC, SSO, FollowConnector de matching | Context Contract + Template Recommender + `subject_ref` genérico (P0-10) |

**Lo que ya está es la mitad difícil del MVP0:** Docs Core con 247 pruebas automáticas contra
infraestructura real, versionado de plantillas con quality gate, y la infra de despliegue. El
cierre es construir el eje de procesos sobre esa base sin desestabilizarla.

---

## 3. Tablero de tareas

Fuente: matriz de desvíos §12 del análisis. La columna "en el repo" ancla cada ítem al estado
actual del código.

### P0 — cierra el MVP0 Comercial

- [ ] **P0-01 · Baseline único y nomenclatura** · *Esfuerzo: S*
  Marcar los documentos V7 contradictorios como superseded-for-scope y adoptar la numeración
  MVP0–MVP4. Este documento es ese paso; falta la contraparte en la Base V7 (fuera del repo) y
  un pase de revista de `README.md` y `EMPEZAR_ACA.md` cuando cierre el P0-02.

- [ ] **P0-02 · Cerrar la validación del MVP0 técnico** · *Esfuerzo: S*
  Los cuatro puntos de `ESTADO_MVP0.md`: (1) recorrido humano punta a punta en el navegador,
  (2) confirmar que el visor muestra los datos de la captura genérica, (3) cargar 10–15
  documentos reales, (4) rotar la clave de Gemini. **Nada de lo demás importa si esto no sale
  bien: es la base sobre la que se vende.**

- [ ] **P0-03 · Workflow Definition + Runtime** · *Esfuerzo: L* · *Depende de: nada (arranca ya)*
  Definitions/versiones, instancias fijadas a versión, tareas, responsables, decisiones,
  timers/SLA, cierre y auditoría. Nodos del MVP0: START/END, DOCUMENT REQUEST, FORM, AI VALIDATE,
  HUMAN REVIEW, DECISION, EXTERNAL TASK, NOTIFICATION, TIMER/SLA, API/WEBHOOK ACTION, SUBPROCESS.
  Instancia iniciada en v1 sigue en v1 aunque se publique v2.
  *En el repo:* es la fusión de las viejas tareas 21 (nodos, aristas, versiones, validador de
  grafo) y 22 (tokens, ramas, joins, timers, reintentos). Va en el microservicio `workflow`, no
  en el core (regla de `EMPEZAR_ACA.md` §7: el núcleo documental no depende del motor de
  procesos, y apagar Workflow no puede afectar captura/extracción/validación).

- [ ] **P0-04 · Studio guiado** · *Esfuerzo: M/L* · *Depende de: P0-03*
  Editor lista/timeline con panel lateral de configuración y vista previa; crear desde cero o
  duplicar, definir pasos/documentos/roles/SLA/controles/notificaciones, probar y publicar con
  versionado. **Mismo JSON/schema de definición que usará el Canvas de MVP1** (no migrar después).
  *En el repo:* reutiliza el ciclo publish/versionado/quality gate ya construido para plantillas
  documentales (tarea 1/2 del plan viejo) como referencia de diseño.

- [ ] **P0-05 · External Collaboration** · *Esfuerzo: M* · *Depende de: P0-03*
  Cuenta externa limitada (sólo tareas/documentos/estado asignados) + Secure Action Link con
  scope y TTL para cargas/aprobaciones puntuales. Aislamiento cross-tenant verificado por test.
  *En el repo:* viejas tareas 23 (Task Service y Action Center) y 24 (Secure Action Links).

- [ ] **P0-06 · IA Supervisora v0** · *Esfuerzo: M/L* · *Depende de: P0-03*
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

- [ ] **P0-07 · Partner Foundation** · *Esfuerzo: M*
  `partner_organization`, `delegated_access_grant` (partner_user → client_tenant + scopes +
  expiración + aprobador), roles Partner Consultant y Partner Publisher. Delegación explícita por
  tenant/scope: sin superusuario transversal, sin visibilidad de otros clientes.
  *En el repo:* nuevo bounded context con tenant-filtering como el resto; migración nueva (V21+).

- [ ] **P0-08 · Template marketplace-ready** · *Esfuerzo: M* · *Depende de: P0-03, P0-07*
  `template_definition` (owner_type/owner_org_id/visibility), `template_version` (semver, manifest,
  hash), `template_provenance` (created_from/forked_from/overlay_of), `template_installation`
  (pin/update policy), `tenant_template_override`, `marketplace_listing` y `commercial_terms`
  **dormantes** en MVP0. Regla IP: una plantilla publicada no se copia ni sobreescribe; el cliente
  instala una versión y sus cambios son fork/overlay local; la actualización del publisher crea
  versión nueva y el cliente decide cuándo adoptar.
  *En el repo:* modelar esto desde el día uno en el dominio de plantillas de proceso; agregarlo
  después rompe trazabilidad y propiedad intelectual de los partners.

- [ ] **P0-09 · Biblioteca COMEX** · *Esfuerzo: M* · *Depende de: P0-03, P0-04*
  Las 10 plantillas base V7 (EX/IM × Mar FCL/LCL, Air, Road FTL/LTL) + 4 multimodales nuevas
  (EX-MM-ROAD-SEA, IM-MM-SEA-ROAD, EX-MM-ROAD-AIR, IM-MM-AIR-ROAD) + overlays (REEFER, DG, FOOD,
  OVERSIZE, CONT-RETURN, SP03-VOLUME, RECOLLECTION, POD/LASTMILE) + fixtures + role maps.
  Catálogo documental y manifest estándar según el anexo de `MVP/`. Los nombres/requisitos
  aduaneros se parametrizan por país; los IDs de tipos de carga Follow vienen por Connector, no
  se duplican maestros.

- [ ] **P0-10 · Follow Context/Template Recommender** · *Esfuerzo: M* · *Depende de: P0-09*
  Contrato canónico de contexto (scope/direction/mode/logistic_unit/cargo_profile/follow_operation/
  flags/subject_ref), `POST /template-recommendations`, `POST /workflow-instances`, eventos
  `document.*`/`task.*`/`workflow.completed`. Follow no hardcodea nombres de plantilla: envía
  contexto y NEXT DOC AI recomienda la composición.
  *En el repo:* `ConectorAsociacionInt` ya existe como el lugar de los adaptadores; el Connector
  es el que traduce IDs canónicos de Follow al manifest. Prohibido acoplar el core a tablas
  internas de Follow.

- [ ] **P0-11 · E2E y seguridad** · *Esfuerzo: M*
  Playwright cubriendo recorridos críticos de Docs + Flow + tercero + partner; cross-tenant,
  secure link y publicación versionada en CI. *En el repo:* ya hay un plan de QA con Playwright
  escrito y sin implementar (commits `e88ba76`/`b08d1b5`) — es el punto de partida.

- [ ] **P0-12 · Dataset real** · *Esfuerzo: M*
  Piloto COMEX + documentos variados por vertical; gold fields y reglas; variantes por
  emisor/formato y casos negativos. Para salida comercial se pide un corpus gold más amplio que
  los 10–15 documentos de la demo (§13 del análisis de desvíos). Incluye suite por plantilla del
  anexo: happy path, variantes layout, missing docs, low confidence, cross-doc mismatch, new
  version, external isolation, template version, overlay merge, follow mapping.

- [ ] **P0-13 · Load y operaciones** · *Esfuerzo: M*
  Baseline p95/throughput/colas/costo por documento y por proceso; backup/restore probado;
  observabilidad, alertas, runbooks y rotación de secretos. Sin esto no hay salida a producción.

### P1 — apenas cierre el MVP0 (hacia MVP1)

- [ ] **P1-01 · Email process intake** — reactivar el alias dedicado por tenant/proceso para
  COMEX. El código retirado se recupera de git (`0b681d3`, `a50dd7c`); la decisión de retirarlo
  se mantiene para el portal: entra como configuración de tenant, no como sección operativa.
- [ ] **P1-02 · Firma por adapter** — Legale/Docusign sin acoplar el core (vieja tarea 27).
- [ ] **P1-03 · Canvas avanzado** — grafo visual sobre el mismo schema del Studio guiado (vieja
  tarea 26 completa). MVP1.

---

## 4. Orden de ejecución sugerido

```
P0-01 ─┬─> P0-02 (validar lo que existe)
       └─> P0-03 (Workflow Definition + Runtime) ─┬─> P0-04 (Studio guiado) ──> P0-09 (COMEX) ──> P0-10 (Follow)
                                                   ├─> P0-05 (terceros)
                                                   └─> P0-06 (Supervisora v0)
P0-07 (Partner foundation) ──> P0-08 (marketplace-ready)
P0-11 (Playwright) ── cierra al final, cubriendo Docs + Flow + tercero + partner
P0-12 (dataset) y P0-13 (load/ops) ── en paralelo con el tramo final
```

**Primer sprint:** P0-01 + P0-02 son días, no semanas, y son la precondition de todo lo demás.
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

Checklist de cierre (§13 del análisis de desvíos):

- [ ] Un tenant nuevo opera standalone sin Follow: crea usuarios, sube documentos y ejecuta una plantilla de proceso.
- [ ] Un Process Admin duplica una plantilla, cambia responsables/documentos/SLA/controles, prueba y publica versión nueva sin soporte de desarrollo.
- [ ] Una instancia iniciada en v1 continúa en v1 aunque se publique v2; migración sólo explícita.
- [ ] Un tercero completa una tarea por cuenta limitada o Secure Action Link sin acceder a otro recurso/tenant.
- [ ] La IA Supervisora detecta documento faltante, baja confianza, inconsistencia cross-doc configurada y SLA vencido; crea acción auditada.
- [ ] Un partner con delegación explícita configura plantillas en un cliente sin visibilidad de otros clientes.
- [ ] Una plantilla conserva autor, owner, provenance, versión e instalación; un fork del cliente no altera el original del partner.
- [ ] Las 14 plantillas COMEX pasan validación estructural con fixtures; los overlays se componen sin duplicar lógica.
- [ ] Follow solicita recomendación por contexto y activa una instancia sin hardcodear nombre de plantilla.
- [ ] Playwright cubre los recorridos críticos; cross-tenant/secure link/publicación están en CI.
- [ ] Existe baseline de rendimiento y costo por documento/proceso; backup/restore y observabilidad probados.
- [ ] Datos reales de piloto producen salidas útiles o excepciones accionables; secretos de desarrollo rotados.

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
