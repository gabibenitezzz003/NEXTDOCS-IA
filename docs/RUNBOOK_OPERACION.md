# Runbook de operación

Lo necesario para operar NextDocs en producción: qué mirar, qué respaldar, qué hacer cuando algo
falla. Todos los números salen de mediciones reales sobre el entorno local, no de estimaciones.

## Línea base de latencia

Medida el 14/09/2026 con `scripts/baseline-carga.mjs`, 4 peticiones por segundo, 20 segundos por
escenario, sobre 73 documentos y 5 instancias de proceso.

| Escenario | Servicio | p50 | p95 | p99 |
|---|---|---|---|---|
| documentos: bandeja | core | 21 ms | 31 ms | 35 ms |
| documentos: resumen | core | 12 ms | 16 ms | 16 ms |
| excepciones: bandeja | core | 17 ms | 26 ms | 31 ms |
| kpi documental: resumen | core | 17 ms | 26 ms | 30 ms |
| plantillas: catálogo | core | 14 ms | 17 ms | 21 ms |
| procesos: definiciones | workflow | 16 ms | 25 ms | 28 ms |
| procesos: instancias | workflow | 24 ms | 33 ms | 50 ms |
| procesos: tareas | workflow | 10 ms | 12 ms | 13 ms |
| kpi procesos: resumen | workflow | 11 ms | 14 ms | 23 ms |
| kpi procesos: población | workflow | 9 ms | 11 ms | 14 ms |

El peor p95 es `procesos: instancias` con 33 ms, porque devuelve cada instancia con sus tareas y
eventos. Es el primero que va a degradarse cuando crezca el volumen.

**Estos números son un piso, no un techo.** Se midieron con pocos datos y sin concurrencia real de
escritura. Volver a correrlo con volumen de cliente antes de comprometer un SLA.

```bash
NEXTDOCS_TENANT=... NEXTDOCS_EMAIL=... NEXTDOCS_CLAVE=... \
  node scripts/baseline-carga.mjs --informe baseline.md
```

Desde el 16/09/2026 el core expone el histograma de `http.server.requests` con percentiles
(p50/p95/p99) y buckets SLO en `/actuator/metrics` y `/actuator/prometheus`, además de los gauges
`nextdocs.cola.extraccion.profundidad` y `nextdocs.cola.extraccion.reintentos` — el p95 ya no hay
que medirlo a mano, sale del scrape.

Medición puntual del 16/09/2026 sobre la base QA (114 documentos): `GET /documentos` sostenido
dio p50=10 ms y p95=13 ms, y la ráfaga por encima de 300 peticiones/minuto por usuario devolvió
429 como corresponde — el límite corta en vez de degradar.

## El límite de peticiones es más bajo de lo que parece

El core rechaza con **429** por encima de:

| Límite | Valor | Variable |
|---|---|---|
| Peticiones por minuto y por usuario | 300 (5 por segundo) | `NEXTDOCS_LIMITE_PRINCIPAL` |
| Peticiones por minuto y por tenant | 1200 (20 por segundo) | `NEXTDOCS_LIMITE_TENANT` |
| Ingestas por minuto y por tenant | 60 | `NEXTDOCS_LIMITE_INGESTA` |

Los 300 por usuario se alcanzan antes de lo que uno espera: un portal abierto con varias pestañas
refrescando puede rozarlo. Si aparecen 429 en la operación normal, el número a subir es
`NEXTDOCS_LIMITE_PRINCIPAL`, no el del tenant.

**El motor de procesos no tiene límite de peticiones.** Si se expone fuera de la red interna hay
que ponerle uno en el borde.

## Respaldo

`scripts/respaldo.sh` cubre las dos mitades: las bases PostgreSQL y los objetos de MinIO. Una sin
la otra no sirve — la base sin los archivos son filas apuntando a documentos que no existen.

```bash
scripts/respaldo.sh crear /respaldos/$(date -u +%Y-%m-%d)
scripts/respaldo.sh verificar /respaldos/$(date -u +%Y-%m-%d)
```

`verificar` levanta un PostgreSQL descartable, restaura ahí, y compara la cantidad de filas de
**cada tabla** contra la base viva; hace lo mismo con los objetos comparando checksums. No toca
producción. Sale con código 1 si algo no cuadra, así que sirve en cron.

Verificado el 14/09/2026: 44 tablas en `nextdocs`, 19 en `nextdocs_workflow` y 4516 objetos, sin
una sola diferencia. Y comprobado al revés: quitándole tres objetos al respaldo, la verificación
falla como debe.

**Un respaldo que nunca se restauró no es un respaldo.** Correr `verificar` con cada respaldo, no
una vez por año.

### Restaurar

```bash
scripts/respaldo.sh restaurar /respaldos/2026-09-14
docker compose --profile app --profile workflow restart
```

Es destructivo y pide escribir `RESTAURAR` para confirmar.

### Lo que el respaldo en caliente no garantiza

El empaquetado del volumen de MinIO se hace con el servicio andando. Los objetos de negocio salen
consistentes, pero los contadores internos de `.minio.sys` pueden quedar a medio escribir; MinIO
los regenera solo, y por eso `verificar` los excluye de la comparación.

Para un respaldo con garantía total hay que cortar la escritura durante el volcado, o pasar a
`mc mirror` a nivel aplicación cuando MinIO deje de ser de un solo nodo.

## Observabilidad

| Servicio | Endpoint | Estado |
|---|---|---|
| core | `/actuator/health` | público |
| core | `/actuator/prometheus` | autenticado: una cuenta de servicio lo lee con `X-Clave-Servicio` |
| workflow | `/actuator/health` | público |
| workflow | `/actuator/prometheus` | público, 126 métricas |

**El scraper lee el core con una cuenta de servicio.** `/actuator/**` no está en las rutas
públicas, así que devuelve 403 sin credencial. La salida es darle al scraper una cuenta de
servicio con un alcance mínimo: `X-Clave-Servicio` autentica a cualquier principal con
credencial, y las métricas no exponen datos del tenant. Se crea una sola vez:

```bash
curl -X POST "$BASE/api/v1/administracion/cuentas-servicio" \
  -H "Authorization: Bearer $TOKEN_ADMIN" -H "Content-Type: application/json" \
  -d '{"nombre":"scraper-prometheus","alcances":["documentos.leer"]}'
```

La clave se muestra una sola vez y va al `scrape_config` de Prometheus como cabecera
`X-Clave-Servicio`. Verificado el 16/09/2026: la cuenta lee las métricas, no puede publicar
eventos ni escribir fuera de su alcance, y al revocarla el acceso corta en el acto (403).

En producción el scrape va contra el **puerto interno** de la instancia
(`http://127.0.0.1:8090/actuator/prometheus` en el core, `:8091` en el workflow), no por el
dominio: nginx no rutea `/actuator` y devuelve el index.html del SPA. Las métricas quedan
dentro de la red interna, que es donde pertenecen.

En el workflow el endpoint existía en la configuración pero faltaba la dependencia
`micrometer-registry-prometheus`, así que devolvía 404: la configuración prometía algo que el
binario no podía dar. Corregido.

### Qué vigilar

| Señal | Umbral sugerido | Por qué |
|---|---|---|
| `http_server_requests` p95 | > 500 ms sostenido | La base medida está entre 9 y 33 ms |
| Respuestas 5xx | cualquiera sostenida | El catch-all devuelve 500 genérico: hay que ir al log |
| Respuestas 429 | crecimiento sostenido | Alguien está contra el límite por usuario |
| `hikaricp_connections_pending` | > 0 sostenido | El pool de base es el primer cuello |
| `nextdocs_cola_extraccion_profundidad` | creciendo sin bajar | La cola de extracción se trabó; el trabajador no drena |
| `nextdocs_cola_extraccion_reintentos` | creciendo sin bajar | Las extracciones fallan y se reintentan |
| Documentos en `PROCESANDO` | creciendo sin bajar | La cola de extracción se trabó |
| Excepciones `ABIERTA` | creciendo sin bajar | Nadie está revisando |
| Espacio del volumen de MinIO | > 80% | Los documentos no se borran solos |

## Rotación de secretos

Ninguno de estos valores vive en el repositorio.

| Secreto | Dónde | Al rotarlo |
|---|---|---|
| Clave de Gemini | `NEXTDOCS_GEMINI_CLAVE`, o por tenant en la base | Reiniciar el core si es la global. Las extracciones en curso fallan y se reintentan solas |
| Clave de DeepSeek | `NEXTDOCS_DEEPSEEK_CLAVE`, o por tenant | Igual que la anterior |
| Secreto JWT | `NEXTDOCS_JWT_SECRETO` | **Invalida todas las sesiones**: los usuarios vuelven a entrar. Hay que cambiarlo en el core y en el workflow **a la vez**, porque comparten el mismo valor |
| Clave de PostgreSQL | `NEXTDOCS_BD_CLAVE` | Cambiar en la base y en los dos servicios, después reiniciar |
| Claves de MinIO | `NEXTDOCS_S3_CLAVE_ACCESO`, `NEXTDOCS_S3_CLAVE_SECRETA` | Cambiar en MinIO y en el core |
| Secreto de webhooks | por tenant, en base | Se regenera desde la API; el receptor deja de validar las firmas viejas |

Las credenciales de los proveedores de IA se resuelven con `ResolvedorSecreto`, que acepta dos
formas: `env:NOMBRE_DE_VARIABLE` o `literal:valor`. Un tenant puede tener la suya en
`configuracion_proveedor.referencia_secreto`; si no la tiene, se usa la variable global. **Preferir
siempre `env:`**: con `literal:` la clave queda escrita en la base y aparece en cualquier respaldo.

El JWT es el único que corta sesiones. Los demás se pueden rotar en caliente con un reinicio
rodante.

**Pendiente:** la clave de Gemini que se usó durante el desarrollo sigue vigente. Rotarla antes de
que el sistema toque datos de un cliente.

## Incidentes frecuentes

### Los documentos se quedan en PROCESANDO

Mirar el log del core buscando `ProveedorNoDisponibleException`. Suele ser cuota del proveedor de
IA agotada: el sistema abre una excepción de tipo `CUOTA_PROVEEDOR` y reintenta. Si la cuota está
bien, revisar que el contenedor de antivirus responda — la ingesta escanea antes de extraer.

### Todo devuelve 401

El secreto JWT dejó de coincidir entre el core y el workflow. Los dos leen
`NEXTDOCS_JWT_SECRETO` y tiene que ser el mismo valor en ambos.

### El workflow arranca y se cae

Casi siempre es Flyway con `ddl-auto: validate`: una migración nueva que no corrió, o una entidad
que no coincide con el esquema. El log lo dice explícito. **Nunca editar una migración ya
aplicada**; agregar una nueva.

### Un indicador muestra número pero el detalle está vacío

Ya pasó dos veces por bugs distintos (un typo en el switch del drilldown y una relación cargada
fuera de transacción). Si vuelve a pasar, mirar el log del workflow: un
`LazyInitializationException` en `KpiProcesoService` apunta a un método sin `@Transactional`.

## Lo que falta para producción

- Límite de peticiones en el motor de procesos si se expone fuera de la red interna.
- Alertas configuradas de verdad contra los umbrales de arriba; hoy solo están las métricas.
- Repetir la línea base con volumen de cliente.
- Respaldo automático programado con retención, y el `verificar` corriendo con cada uno.
- Rotar la clave de Gemini.
