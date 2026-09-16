# E14 · P0-15 · Acta de salida comercial (MVP0)

Estado al 16/09/2026. Mapea cada gate del MVP0 Comercial con su evidencia verificable. Un gate
**conforme** tiene prueba ejecutable o registro en el repo; uno **pendiente** indica la acción que
lo cierra.

## Gates

| Gate | Qué cubre | Estado | Evidencia |
|---|---|---|---|
| 0A | Baseline único y nomenclatura | Parcial | `TODO_MVP0_COMERCIAL.md` adopta MVP0–MVP4; falta la contraparte en la Base V7 (fuera del repo) |
| G-DEMO | Validación del MVP0 técnico | Parcial | `P0_02_VALIDACION.md`: recorrido automatizado hecho, captura genérica confirmada, 15/15 con Gemini real sobre dataset propio. Falta rotar la clave y corpus de cliente |
| 0B/0C | Workflow Definition + Runtime | Conforme | Motor con 15 tipos de nodo, fork/join real, disparadores, firma, simulador; 180 pruebas; QA end-to-end por API en `verificaciones` |
| 0D | Studio guiado + Colaboración externa | Conforme | Canvas completo con deshacer/minimapa/simulador; portal público `/externo/{token}` con carga de documentos; aislamiento verificado |
| 0E | IA Supervisora v0 | Conforme | `e06-p0-06-ia-supervisora.md` |
| 0F | Biblioteca COMEX | Conforme | `e07-p0-09-comex-fixture.md` |
| 0G | Load y operaciones | Parcial | Histograma p50/p95/p99, gauges de cola, scraper con cuenta de servicio, rate-limit verificado, respaldo/restauración ejercitados. Falta: alertas en Prometheus real, cron de respaldo, rotación de la clave |
| 0H | Release comercial | En curso | Este acta + runbook + rollback documentado. Faltan: demo final, clave rotada, corpus de cliente |

## QA ejecutado en el cierre

- **Motor paralelo**: fork/join por API real — dos ramas vivas, join que aguanta y libera, firma,
  cierre; los cuatro errores de diseño rechazados al guardar.
- **Colaboración externa**: enlace público con contexto, carga de documento por token verificada
  contra el repositorio, completar con `documentoId`, rechazo sin documento, estados terminales
  bloqueando cargas.
- **Notificaciones**: `NOTIFICACION` publicó `process.notification` al core; carga verificada.
- **Seguridad**: `integraciones.escribir` fuera de roles humanos; el endpoint exige cuenta de
  servicio; clave de servicio de alcance mínimo lee métricas y no puede publicar ni escribir;
  revocación corta en el acto.
- **Operación**: rate-limit devuelve 429 por encima del límite; pg_dump → pg_restore a base
  descartable con conteos idénticos y cero referencias huérfanas.

## Rollback

`deploy.sh` preserva la imagen previa como `nextdocs-ia-backup` y la restaura si el contenedor no
queda `healthy`. Restauración de datos: `scripts/respaldo.sh restaurar` (destructivo, pide
`RESTAURAR`). Detalle en `DESPLIEGUE.md` §Recuperación y `RUNBOOK_OPERACION.md` §Respaldo.

## Pendiente del operador (no de código)

- Rotar la clave de Gemini usada en desarrollo (procedimiento en §Rotación del runbook).
- Cargar documentación real de un cliente y repetir la línea base con ese volumen.
- Configurar alertas en el Prometheus desplegado contra los umbrales del runbook.
- Programar `respaldo.sh` con retención y `verificar` en cada corrida.
