# TO-DO de NEXT DOC AI

Plan de trabajo hasta terminar el producto completo. **Este es el documento de referencia:
quien retome el proyecto arranca por la primera tarea sin marcar de la Fase 1.**

> Si es tu primer contacto con el proyecto, leé antes [`EMPEZAR_ACA.md`](EMPEZAR_ACA.md).

Cada tarea trae su criterio de aceptación referenciando los códigos de QA del documento
`N3-SPR084-V7-NEXT_DOC_AI_ESPECIFICACION_DESARROLLO_QA_UX`.

---

## Ya terminado

- [x] Andamiaje Maven · Spring Boot 3.4 · Java 21 · PostgreSQL 16 · Redis · MinIO
- [x] 27 enumeraciones, 27 entidades JPA, 26 repositorios, migración Flyway `V1`
- [x] Multi-tenancy en tres capas con aislamiento verificado (`SEC-01`)
- [x] Autenticación JWT y cuentas de servicio con clave hasheada
- [x] Ingesta idempotente con MIME real por contenido y conteo de páginas
- [x] Almacenamiento S3/MinIO con checksum y URL firmada con TTL
- [x] Máquina de estados del documento con transiciones validadas
- [x] Proveedor de IA abstracto, router con respaldo y worker con backoff exponencial
- [x] Motor de validación con reglas versionadas por plantilla
- [x] Revisión humana con correcciones, sobreescritura y motivo obligatorio
- [x] Centro de excepciones con SLA, prioridad y deduplicación
- [x] Auditoría de toda acción con actor, recurso y correlación
- [x] Outbox transaccional y despachador de webhooks firmados con HMAC
- [x] API REST v1 de autenticación, documentos y excepciones
- [x] **Tarea 1** — API de plantillas con ciclo de vida, versionado, publish y rollback
- [x] **Tarea 3** — adaptador Gemini real verificado contra `gemini-2.5-flash`
- [x] **Tarea 6** — matching con circuit breaker y `FollowConnector` aislado
- [x] **Tarea 5** — segmentación de PDF multi-documento con estrategias configurables
- [x] **Tarea 4** — antivirus con cuarentena verificado contra ClamAV real
- [x] **Tarea 11** — seguimiento del original físico con política por plantilla
- [x] **Tarea 13** — suite de QA automatizada, 50 unitarios y 80 de integración
- [x] **Tarea 7** — API de gobernanza, reconstrucción de la decisión y exportación de auditoría
- [x] **Tarea 8** — retención en ejecución con legal hold y prueba de borrado
- [x] **Tarea 9** — administración de tenant, usuarios, roles y cuentas de servicio
- [x] **Tarea 10** — API de suscripciones de webhook y monitor de integraciones
- [x] **Tarea 2** — prueba de plantilla y quality gate con dataset gold
- [x] **Tarea 12** — observabilidad y control de costos por tenant
- [x] **Tarea 14** — despliegue reproducible y pipeline de CI

---

## Fase 1 — Cerrar la Etapa 1 vendible

Es lo que falta para que NEXT DOC AI se pueda vender como producto documental standalone,
sin Workflow y sin Follow.

| # | Tarea | Por qué bloquea la venta |
|---|---|---|
| ~~1~~ | ~~API de plantillas con ciclo de vida completo~~ | ✅ **Terminada.** Ciclo, clonado, validador de publicación, rollback y bloqueo optimista |
| ~~2~~ | ~~Prueba de plantilla y quality gate con dataset gold~~ | ✅ **Terminada.** `GOV-04`: si la política exige el gate, no se publica sin una corrida gold aprobada; una versión que empeora queda bloqueada |
| ~~3~~ | ~~Adaptador Gemini real~~ | ✅ **Terminada.** `gemini-2.5-flash` con esquema estructurado, 10 tests contra servidor falso y verificación contra Gemini real |
| ~~4~~ | ~~Antivirus y cuarentena en la ingesta~~ | ✅ **Terminada.** `SEC-04` verificado con ClamAV 1.5.4 y un PDF con EICAR embebido |
| ~~5~~ | ~~Segmentación de PDF multi-documento~~ | ✅ **Terminada.** `QA1-02` verificado con un PDF real de 10 remitos |
| ~~6~~ | ~~Matching + `FollowConnector`~~ | ✅ **Terminada.** `QA1-05`, `QA1-06` y `QA-FOL-01` verificados end-to-end con Follow simulado |
| ~~7~~ | ~~API de gobernanza y exportación de auditoría~~ | ✅ **Terminada.** `GOV-01` verificado: un documento aprobado reconstruye proveedor, modelo, prompt, esquema, reglas, match y revisor, y declara qué falta cuando no puede |
| ~~8~~ | ~~Job de retención y legal hold~~ | ✅ **Terminada.** `GOV-02` verificado: el plazo corre desde el cierre, el legal hold bloquea la acción, y cada documento tratado deja prueba de borrado |
| ~~9~~ | ~~API de administración de tenant, usuarios y cuentas de servicio~~ | ✅ **Terminada.** Usuarios, roles propios, cuentas de servicio y tenant, con revocación inmediata y sin poder dejar el tenant sin administradores |
| ~~10~~ | ~~API de suscripciones de webhook y monitor de integraciones~~ | ✅ **Terminada.** Alta, pausa, prueba firmada, auto-pausa por fallos, DLQ y reintento manual. El secreto se muestra una sola vez |
| ~~11~~ | ~~Seguimiento del original físico~~ | ✅ **Terminada.** `QA1-08` verificado con las tres políticas |
| ~~12~~ | ~~Observabilidad y control de costos por tenant~~ | ✅ **Terminada.** Costo efectivo = inferencia del periodo / documentos `APROBADO` o `CERRADO`; presupuesto mensual opcional puede cortar la ingesta |
| ~~13~~ | ~~Suite de QA automatizada~~ | ✅ **Terminada.** 70 tests. Sin Testcontainers: usa la infra del compose, ver nota abajo |
| ~~14~~ | ~~Despliegue reproducible y pipeline de CI~~ | ✅ **Terminada.** Imagen multi-stage, `compose --profile app`, health y GitHub Actions con la misma infra que las IT |

**Orden sugerido:** ~~1~~ → ~~3~~ → ~~6~~ → ~~5~~ → ~~4~~ → ~~11~~ → ~~13~~ → ~~7~~ → ~~8~~ → ~~9~~ → ~~10~~ → ~~2~~ → ~~12~~ → ~~14~~

**Nota sobre Testcontainers.** No se pudo usar: Docker 29.7 exige API ≥ 1.40 y el `docker-java` que
trae Testcontainers 1.21.3 negocia 1.32, así que no encuentra el entorno Docker. En vez de eso los
tests de integración usan la infraestructura del `compose.yml` sobre una base `nextdocs_prueba`
separada, que se crea sola. Es lo mismo que va a hacer la CI con service containers, y quita una
dependencia frágil. Si en el futuro Testcontainers soporta esta versión de Docker, migrar es directo:
sólo cambia `PruebaIntegracion`.

**Dependencias:** 2 necesita 1 · 13 necesita 3, 4, 5, 6 y 11

**Aprendido en la tarea 3, aplicado en la 2:** la confianza que reporta Gemini es auto-reportada y
gruesa. El gold mide exactitud de campo (presencia + valor) y, si la corrida aprueba, escribe
`factorCalibracionConfianza` en la versión. La decisión de publicar no usa la confianza: usa el
umbral de exactitud y la comparación con la versión publicada.

---

## Fase 2 — Portal, canales y KPI (completar MVP 1)

| # | Tarea |
|---|---|
| 15 | Portal frontend standalone (bandeja, visor, Template Studio, Exception Center, gobernanza, monitor) |
| 16 | SSO y embed con Follow, CIMA y Valid360.ai por federación de identidad |
| 17 | Channel Gateway de email dedicado por tenant |
| 18 | Canal de ingesta por WhatsApp |
| 19 | Archive & Export Center |
| ~~20~~ | ~~Dashboard ejecutivo y panel de control de procesos~~ ✅ |

**Dependencias:** 15 necesita 1 y 9 · 20 necesita 12 y 15

La tarea 20 se construyó sobre lo que existe hoy: los KPI de proceso del ANEXO_H que dependen del
Workflow (Etapa 2) todavía no tienen de dónde salir, así que la unidad de agrupación es la
**plantilla**, no el proceso. Cuando entre la Fase 3 hay que sumar los KPI por etapa de workflow.

Con la Fase 2 cerrada, el **MVP 1** del resumen ejecutivo está completo:
portal propio, motor documental, SSO embebido, KPI y export básico.

---

## Fase 3 — Etapa 2: Workflow y Process Studio

| # | Tarea |
|---|---|
| 21 | Workflow Definition Service (nodos, aristas, versiones, validador de grafo) |
| 22 | Workflow Runtime Service (tokens, ramas, joins, timers, reintentos) |
| 23 | Task Service y Action Center |
| 24 | Secure Action Links para terceros |
| 25 | Notification Service |
| 26 | Process Studio con canvas y editor guiado |
| 27 | Signature Adapter desacoplado (Legale / Docusign) |

**Dependencias:** 22 necesita 21 · 23 necesita 22 · 26 necesita 21 y 22 · 27 necesita 22

Regla de la Etapa 2: **apagar el Workflow no puede afectar** captura, extracción, validación
ni integración documental. El núcleo no depende del motor de procesos.

---

## Fase 4 — Etapa 3: inteligencia continua

| # | Tarea |
|---|---|
| 28 | Process Facts y grafo de dependencias |
| 29 | Motor de reglas cross-document del Sentinel |
| 30 | Resolvedor de impacto incremental |
| 31 | Simulation Lab |
| 32 | Process Copilot |

**Dependencias:** 29 necesita 28 · 30 necesita 28, 29, 23 y 25 · 31 necesita 29

---

## Reglas que aplican a toda tarea

Del `Definition of Done` global del N3:

1. Código + migración + OpenAPI + test automatizado + observabilidad + runbook.
2. QA funcional con fixtures y **casos negativos**, no sólo el camino feliz.
3. Revisión de seguridad de tenant, secretos y subida de archivos.
4. Estados de UI: carga, vacío, error, reintento y permisos.
5. Toda feature se puede **deshabilitar por tenant** sin romper el core.
6. No se cambia un contrato de datos histórico sin migración y retrocompatibilidad.
7. **Ningún comentario en el código.** Lo que necesite explicación va a `docs/`.
8. Todo en español y camelCase, con la nomenclatura de `follow-backend`.

Y las invariantes de dominio que ninguna tarea puede romper:

- La confianza de lectura **nunca** aprueba por sí sola.
- `NO_FIGURA` e `ILEGIBLE` son estados distintos con hallazgos distintos.
- Dos candidatos de asociación van a revisión humana, jamás se elige en silencio.
- Una plantilla publicada no se modifica: se crea una versión nueva.
- Sobreescribir un hallazgo exige motivo, actor y queda auditado.
- Ningún servicio del core importa modelos de Follow.
