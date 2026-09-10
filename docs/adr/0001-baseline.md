# ADR 0001 · Baseline técnico del MVP0 Comercial de NEXT DOC AI

## Estado

Propuesto en E00. Aceptado por escrito requerido de PO/CTO para cierre del gate 0A.

## Contexto y problema

NEXT DOC AI es una plataforma de inteligencia documental que, al inicio de la iteración E00, ya cuenta con un backend funcional desarrollado en Spring Boot 3.4 / Java 21, un frontend React 19 / TypeScript / Vite y un microservicio de workflow en Spring Boot 3.4 / Java 21. Sin embargo, el punto de partida presenta ambigüedades:

- El kit V11 reporta un MVP0 con 247 pruebas, 13 controladores, 40 entidades y 20 migraciones, pero no se había reconciliado con el código real hasta esta sesión.
- El kit también reporta el microservicio workflow como implementado, mientras que la documentación interna del repo (`docs/TODO_MVP0_COMERCIAL.md`) lo califica como "No empezado" / "Con desvíos".
- Existen decisiones técnicas y comerciales pendientes (D01-D11) que deben resolverse o al menos documentarse antes de ejecutar cortes verticales.
- Algunas funciones construidas previamente (email y WhatsApp como canales de entrada) fueron retiradas del MVP0 y no deben reactivarse accidentalmente.

El problema es: ¿cómo definir un baseline único, verificable y gobernado que permita trabajar por épicas sin reconstruir lo que ya existe ni ignorar los gaps que bloquean el MVP0 Comercial?

## Decisiones tomadas

### DT-01 · Idioma y convenciones

**Decisión:** mantener todo el código y la documentación en español, con camelCase en Java/TypeScript y snake_case en la base de datos, siguiendo la estructura de paquetes heredada de `follow-backend`.

**Motivo:** regla no negociable de `AGENTS.md` y `EMPEZAR_ACA.md`. Cambiarlo ahora generaría inconsistencia masiva y perdería la trazabilidad con el equipo actual.

### DT-02 · Stack fijado

**Decisión:** fijar el stack técnico baseline:

- Backend core y workflow: Spring Boot 3.4.1, Java 21, Maven por contenedor.
- Base de datos: PostgreSQL 16 (local/emulado); RDS propuesto para AWS.
- Caché/coordinación: Redis 7 con semántica explícita.
- Almacenamiento de bytes: MinIO local / S3 AWS en producción.
- Identidad: Keycloak 26.0 en local; SSO federado en producción.
- Frontend: React 19, TypeScript 5.7, Vite 8, React Router 7, TanStack Query 5, Tailwind CSS 4, Axios.

**Motivo:** coincide con `pom.xml`, `package.json` y `compose.yml` actuales. No se actualizarán dependencias de producción por instrucciones genéricas.

### DT-03 · Monolito modular para el core documental

**Decisión:** conservar el backend core como monolito modular. No dividir en microservicios internos dentro del core.

**Motivo:** el código actual está estructurado en capas por dominio (`entidades`, `servicios`, `repositorios`, `restControladores`, etc.) y cumple la regla de que un módulo sólo muta sus tablas mediante su servicio. Dividir ahora sería reconstrucción innecesaria.

### DT-04 · Microservicio workflow separado

**Decisión:** mantener `workflow/` como microservicio separado del core documental, sin que servicios del core importen modelos de workflow.

**Motivo:** `EMPEZAR_ACA.md` §7 lo impone explícitamente. Apagar o degradar el motor de procesos no debe afectar ingesta, extracción, validación ni integración documental. Los adaptadores van detrás de interfaces propias (`ConectorAsociacionInt`).

### DT-05 · No reactivar canales retirados

**Decisión:** email y WhatsApp como canales de entrada quedan retirados del MVP0. Su eventual reincorporación será tratada como épica E17 de MVP1, con adapters y sandbox real.

**Motivo:** el kit y `TODO_MVP0_COMERCIAL.md` son explícitos. Las migraciones V14, V16 y V17 del core confirman la retirada. Reactivarlos en MVP0 rompería el alcance acordado.

### DT-06 · Baseline de datos sin editar migraciones aplicadas

**Decisión:** las 20 migraciones Flyway existentes (V1..V20) del core y las 2 del workflow no se editarán. Las correcciones y nuevos dominios se agregarán como V21+, con el patrón `INSERT ... WHERE NOT EXISTS` para permisos/roles como se hizo en V13.

**Motivo:** editar migraciones ya aplicadas rompe checksums en ambientes existentes y ya costó tiempo en el pasado (`EMPEZAR_ACA.md` §10).

### DT-07 · Servicios devuelven modelos, no entidades

**Decisión:** mantener la regla de que los servicios devuelven modelos DTO convertidos dentro de la transacción (`@Transactional`), nunca entidades JPA.

**Motivo:** evitar `LazyInitializationException` en el borde HTTP. Ya ocurrió tres veces y está documentado como trampa en `EMPEZAR_ACA.md` §1.

### DT-08 · Multi-tenancy en token → repositorio → servicio

**Decisión:** conservar el filtrado por `tenant_id` en repositorios y servicios. En workflow, el `X-Tenant-Id` provisional se mantiene como mecanismo temporal hasta que se resuelva D05 (auth JWT real en workflow).

**Motivo:** el core ya tiene esta invariante; el workflow aún no está integrado con Keycloak y no debe perder el aislamiento por tenant.

### DT-09 · Workflow: cerrar desvíos antes de extender

**Decisión:** antes de agregar nuevos nodos, Studio guiado, COMEX o Follow, se cerrarán los desvíos de la Fase C del `TODO_MVP0_COMERCIAL.md` (C1..C12).

**Motivo:** construir sobre un runtime que contradice la spec V11 generaría deuda técnica inmanejable. La prioridad es la conformidad, no la cantidad de nodos.

## Alternativas consideradas

| Alternativa | Pros | Contras | Decisión |
|---|---|---|---|
| Reconstruir el workflow dentro del core | Menos operación, una sola base | Rompe la regla de separación documental/procesos; apagar workflow afectaría ingesta | Rechazada |
| Migrar a microservicios internos del core (módulos Maven separados) | Alineación teórica con DDD | Reconstrucción masiva sin valor de negocio inmediato; 40 entidades y 59 servicios ya funcionan | Rechazada |
| Reactivar email/WhatsApp en MVP0 | Reutiliza migraciones V14/V16 | Contradice el rebaseline y el kit V11; requiere sandbox real no disponible | Rechazada |
| Actualizar Spring Boot a 3.5+ | Nuevas features y parches | No hay necesidad identificada; riesgo de romper Flyway/compatibilidad sin ganancia demostrable | Rechazada |
| Cambiar idioma a inglés para código | Mercado internacional | Rompe regla no negociable y convenciones del equipo; documentación puede ser bilingüe comercialmente | Rechazada |
| Reemplazar MinIO por S3 en desarrollo | Más parecido a prod | Sin cuenta AWS confirmada (D02 pendiente); MinIO es contrato S3 compatible | Rechazada para dev |

## Estado de cada módulo frente al MVP0

| Módulo | Código V11 | Estado baseline | Observación |
|---|---|---|---|
| DOC — Ingesta y extracción | HU-DOC-01..08 | **IMPLEMENTADO** | Documentos, archivos, segmentación, proveedores IA, antivirus, aprendizaje, correcciones. |
| STU — Plantillas documentales | HU-STU-01..02 | **IMPLEMENTADO** | Versionado, quality gate, bloqueo optimista, reglas. |
| WFL — Workflow Core | HU-WFL-01..08 | **EXTENDER** | Existe; cierra Fase C (idempotencia, activación, decisiones, asignación, JWT). |
| IAM — Identidad y acceso | HU-IAM-01..08 | **IMPLEMENTADO** | JWT, roles, permisos, cuentas de servicio; falta grants delegados de partners (E07). |
| GOV — Gobernanza y auditoría | HU-GOV-01..04 | **IMPLEMENTADO** | Auditoría, retención, legal hold, export, webhooks, costos. |
| REL — Release y operaciones | HU-REL-01..08 | **EXTENDER** | CI, compose, Dockerfile; faltan Playwright, dataset real, load/ops, runbook. |
| SUP — IA Supervisora v0 | HU-SUP-01..08 | **NUEVO** | No existe como capa de proceso; depende de WFL. |
| COM — COMEX/Follow | HU-COM-01..08 | **NUEVO/BLOQUEADO** | Catálogo sembrado argentino de 10 tipos; faltan 14 bases + overlays + Context Contract (D07, D06). |
| CAN — Process Canvas | HU-CAN-01..08 | **NUEVO** | MVP1; Studio guiado (E05) es el paso previo sin grafo visual completo. |
| PAR — Partner Workspace | HU-PAR-01..08 | **NUEVO** | E07 foundation y E16 workspace. |
| CHN — Canales y firma | HU-CHN-01..08 | **NUEVO** | MVP1; email/WhatsApp retirados, firma por adapter. |
| MKT — Marketplace | HU-MKT-01..08 | **NUEVO** | MVP2. |
| UPG — Updates y compatibilidad | HU-UPG-01..08 | **NUEVO** | MVP2. |
| SEN — Sentinel | HU-SEN-01..08 | **NUEVO** | MVP3. |
| COP — Simulation Lab / Copilot | HU-COP-01..08 | **NUEVO** | MVP3. |
| ENT/DR — Enterprise / DR | HU-ENT-01..08 / HU-DR-01..08 | **NUEVO/BLOQUEADO** | MVP4; depende de contrato y RPO/RTO (D10). |

## Consecuencias y riesgos

### Consecuencias positivas

- El trabajo puede comenzar inmediatamente sobre el core documental certificándolo (P0-02) mientras se cierra la conformidad del workflow (Fase C).
- No se desperdicia el código existente (~333 clases Java del core, 54 del workflow, 8 rutas frontend).
- El alcance queda claro: MVP0 Comercial = E00..E14, sin reactivar canales retirados.
- Las migraciones y el esquema de datos son estables; las extensiones van por V21+.

### Riesgos y mitigaciones

| Riesgo | Severidad | Mitigación |
|---|---|---|
| El workflow con desvíos sigue creciendo antes de cerrar Fase C | Alto | Gate 0C no se cierra hasta evidencia de C1..C12; regla de oro: no extender sobre spec incumplida. |
| Se confunde el core documental con el motor de procesos | Medio | ADR + reglas `.cursor` + separación de repos; ningún servicio del core importa modelos de workflow. |
| Se reintroducen canales retirados por presión de MVP | Medio | Documento explícito en ADR y matriz; cualquier canal nuevo pasa por E17 con sandbox real. |
| El frontend consume APIs inexistentes de COMEX/Partner antes de tener backend | Medio | Feature flags por MVP; UI no muestra datos ficticios de futuros. |
| Falta de autenticación JWT en workflow expone datos cross-tenant | Alto | D05 en curso; hasta entonces `X-Tenant-Id` no se considera seguro para producción. |
| AWS/RDS no está provisionado (D02) | Medio | Ambiente local con MinIO/PostgreSQL mantiene el contrato S3/SQL; despliegue productivo queda bloqueado hasta resolver D02. |
| Los 247 tests no se reejecutaron en esta sesión | Medio | Compilaciones (`mvn compile -DskipTests`) y build del frontend (`npm run build`) ejecutadas con éxito. Ejecutar `mvn verify` con infraestructura levantada y registrar resultados en `docs/verificaciones/`. |

## Vínculos

- `docs/discovery-v11.md` — descubrimiento completo y conteos reales.
- `docs/implementation-v11/matriz-requisito-implementacion-gap.md` — mapeo historia por historia.
- `docs/TODO_MVP0_COMERCIAL.md` — plan de cierre MVP0 Comercial.
- `docs/EMPEZAR_ACA.md` — reglas no negociables y trampas.
- `docs/implementation-v11/NEXT_DOC_AI_V11_4_IMPLEMENTACION_CURSOR_COMPLETA/00_LEEME_PRIMERO.md` — kit V11.
