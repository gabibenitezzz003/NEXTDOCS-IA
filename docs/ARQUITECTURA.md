# Arquitectura de NEXT DOC AI

Documento vivo. Refleja lo construido, no lo planeado.
La especificación de origen es `03_ARQUITECTURA_FUNCIONAL_TECNICA_..._V7` y `N2` del paquete SPR084 V7.

---

## Principio rector

> NEXT DOC AI es independiente de Follow. Follow es una integración nativa **opcional**.
> La Etapa 1 funciona sin Workflow. La Etapa 2 es robotización opcional.

Consecuencias concretas en el código:

| Regla | Cómo se cumple hoy |
|---|---|
| Ningún servicio del core importa modelos de Follow | No existe ninguna dependencia a `com.logistica.*` en el `pom.xml` ni en los imports |
| Las referencias externas son opacas | `ReferenciaExterna` (`origen`/`tipoObjeto`/`idObjeto`/`tenantOrigen`) embebida en `Documento` y `CandidatoAsociacion` |
| El tenant es propio, no el de Follow | `Tenant`, `Usuario`, `Rol`, `CuentaServicio` viven en la base de NEXT DOC AI |
| Cambiar de proveedor de IA no cambia el contrato | `ProveedorDocumentalIaInt` devuelve siempre `ResultadoExtraccionModel` |
| Apagar el Workflow no rompe nada | El workflow es un microservicio aparte; el core no lo referencia. La comunicación es unidireccional workflow → core por `X-Clave-Servicio` |

---

## Bounded contexts

Un solo deployable, con fronteras lógicas. La decisión sigue el documento de arquitectura:
*"el objetivo no es crear veinte deployments desde el primer día, sino evitar dependencias lógicas
que impidan extraer servicios cuando el volumen lo justifique"*.

| Contexto | Clases principales | Estado |
|---|---|---|
| IAM / Tenant | `TenantService`, `AutenticacionService`, `CuentaServicioService`, `TokenService` | ✅ |
| Capture / Ingestion | `IngestaDocumentalService`, `InspectorArchivo`, `ColaExtraccionService` | ✅ |
| Document Repository | `AlmacenamientoService`, `DocumentoService`, `EstadoDocumentalService` | ✅ |
| Document AI Orchestrator | `ExtractorDocumentalService`, `RuteadorProveedorService`, `TrabajadorExtraccionService`, `ProveedorGeminiService` | ✅ |
| Template Service | `PlantillaService`, `ValidadorPlantillaService`, `QualityGateService`, `MaquinaEstadoPlantilla` | ✅ |
| Validation Service | `ValidacionDocumentalService` | ✅ |
| Matching Service | `AsociacionService`, `ConectorAsociacionInt`, `RegistroCircuitosService` | ✅ |
| Review / Exception | `RevisionDocumentalService`, `ExcepcionDocumentalService` | ✅ |
| Governance & Audit | `AuditoriaService`, `GobernanzaService`, entidades `EventoAuditoria` / `PoliticaRetencion` | ✅ |
| Event / Integration Hub | `EventoSalidaService`, `EntregaWebhookService`, `IntegracionService`, `DespachadorEventosService` | ✅ |
| Observability / Cost | `ObservabilidadService`, `PoliticaCostoTenant`, métricas Micrometer por tenant y proveedor | ✅ |
| Follow Connector | `FollowConnector`, `FollowCliente` | ✅ |
| Workflow Definition/Runtime | Microservicio separado `workflow/` (puerto 8091): `EjecutorProcesoService`, `DefinicionProcesoService`, `ColaboracionExternaService`, `DisparadorProcesoService`, `SimuladorGrafoService` | ✅ |

---

## Flujo del documento

```
POST /api/v1/documentos
        │
        ▼
IngestaDocumentalService
   · valida tamaño y extensión
   · detecta el MIME real con Tika (no confía en el header)
   · escanea con el antivirus; si está infectado va a cuarentena y NO se encola
   · calcula sha256
   · resuelve idempotencia por Idempotency-Key (o por hash si no viene)
   · guarda en S3/MinIO, cuenta páginas con PDFBox
   · publica document.received en el outbox
   · encola en Redis DESPUÉS del commit
        │
        ▼  RECIBIDO
TrabajadorExtraccionService (@Scheduled sobre Redis)
        │
        ▼
SegmentacionDocumentalService (sólo si la plantilla lo pide y el PDF tiene > 1 página)
   · calcula los tramos por páginas fijas o por patrón de texto
   · crea un hijo por tramo con su propio recorte en el object store
   · el padre pasa a DIVIDIDO y NO se extrae; cada hijo se encola aparte
        │
        ▼  PROCESANDO
ExtractorDocumentalService
   · arma el esquema desde CampoPlantilla de la VersionPlantilla
   · RuteadorProveedorService elige el adaptador
   · ante 429/5xx reintentable: prueba respaldo, si no reencola con backoff exponencial
   · persiste EjecucionExtraccion + ValorExtraido (confianza y presencia por campo)
        │
        ▼  EXTRAIDO
ValidacionDocumentalService
   · reglas de campo: requerido, ilegible, umbral de confianza, expresión regular
   · reglas versionadas: RANGO, VIGENCIA, COMPARACION_CAMPOS, CATALOGO, CONFIANZA_MINIMA
   · genera HallazgoValidacion con severidad
        │
        ├── hay BLOQUEANTE ────────► OBSERVADO + ExcepcionDocumental CRITICA
        └── el resto ──────────────► VALIDADO
                                        │
                                        ▼
AsociacionService
   · arma el contexto canónico con los valores extraídos
   · consulta cada conector activo detrás de su circuit breaker
   · un candidato claro se fija solo; dos o más van a revisión humana
   · un conector caído es CONECTOR_FALLIDO, distinto de SIN_CANDIDATOS
        │
        ├── validación OBSERVADO ──► OBSERVADO + excepción CALIDAD_LECTURA
        ├── asociación ambigua ────► OBSERVADO + excepción ASOCIACION
        ├── conector caído ────────► OBSERVADO + excepción CONECTOR
        └── todo limpio ───────────► APROBADO sólo si autoaprobable
                                                       │
                                              POST /revisiones (humano)
                                                       ▼
                                            APROBADO / RECHAZADO / reproceso
```

### Reglas que no se negocian

1. **La confianza nunca aprueba por sí sola.** `esAutoaprobable()` exige, además del umbral,
   que la validación haya dado `APROBADO` sin ningún hallazgo. La decisión la toman las reglas.
2. **`NO_FIGURA` ≠ `ILEGIBLE`.** Producen hallazgos distintos con severidad distinta.
   Un campo requerido ilegible es `BLOQUEANTE`; uno opcional ilegible es `REQUIERE_REVISION`.
3. **Sobreescribir un hallazgo exige motivo.** `RevisionDocumentalService.exigirMotivo()`.
4. **Las transiciones de estado son explícitas.** `MaquinaEstadoDocumento` rechaza cualquier salto
   no declarado con `TransicionInvalidaException` → HTTP 409.
5. **El evento se publica en la misma transacción que el cambio.** Patrón outbox: `EventoSalida`
   se escribe en la transacción de negocio; `EntregaWebhookService` lo entrega después
   (el `DespachadorEventosService` sólo orquesta el schedule).
6. **La cola se alimenta después del commit.** `TransactionSynchronization.afterCommit()`, para que
   el worker nunca lea un documento que todavía no existe.

---

## Multi-tenancy

Discriminador por columna. Toda entidad de negocio tiene `tenant`.

El aislamiento se aplica en **tres capas**:

1. **Token**: el JWT lleva `tenantId`; `FiltroAutenticacion` lo pone en el `PrincipalNextDocs`.
2. **Repositorio**: las consultas nombradas reciben `tenantId` como parámetro obligatorio
   (`buscarPorIdYTenant`, `listarPorTenant`, …). No hay un `findById` suelto en los caminos de lectura.
3. **Servicio**: `ContextoSeguridad.tenantId()` es la única fuente del tenant. Nunca llega por parámetro
   del cliente.

Verificado: un usuario del tenant B pidiendo un documento del tenant A recibe **404**, y su bandeja
devuelve 0 documentos.

---

## Seguridad

| Aspecto | Implementación |
|---|---|
| Sesión de usuario | JWT HS256 firmado, con `tenantId`, `codigoTenant` y permisos. Acceso 8 h, refresco 7 d |
| Integraciones | Cuenta de servicio con clave `ndai_...`; en base se guarda **sólo el sha256** |
| Autorización | `@PreAuthorize("hasAuthority('...')")` sobre permisos granulares de `Permiso` |
| Revocación | El filtro revalida en cada petición que el usuario siga activo y relee sus permisos. Un bloqueo no espera a que expire el token |
| Secretos en el navegador | Ninguno. El front usa sesión; las integraciones usan `X-Clave-Servicio` server-to-server |
| Archivos | MIME real por contenido con Tika, no por extensión ni por header |
| Antivirus | ClamAV por `INSTREAM` antes de almacenar; fail-closed por defecto; infectado va a bucket de cuarentena |
| Descarga del original | URL firmada con TTL de 15 min. El bucket no es público |
| Webhooks | Firma HMAC-SHA256 del cuerpo en `X-Nextdocs-Firma`. El secreto se muestra una sola vez. Las URLs deben ser HTTPS y no pueden apuntar a redes internas, salvo `nextdocs.webhooks.permitirLocalhost` para desarrollo. Tras `umbralPausa` fallos consecutivos la suscripcion se pausa sola |
| Trazabilidad | `correlacionId` en el MDC, en la respuesta, en cada evento y en cada registro de auditoría |
| Exportación de auditoría | CSV con neutralización de fórmulas, tope de 50 000 eventos y la propia exportación auditada |
| Llamadas del workflow | Sólo `X-Clave-Servicio` del tenant con alcances mínimos: `documentos.leer`/`documentos.escribir` (resolver y subir documentos) e `integraciones.escribir` (publicar `process.*` hacia webhooks: `process.notification`, `process.task_completed`, `process.workflow_completed`). Los enlaces externos del portal `/externo/{token}` nunca tocan el core directamente: el workflow hace de proxy |

---

## Reconstrucción de la decisión

`GOV-01` no pide un log: pide poder sentarse frente a un auditor con un documento aprobado y explicar
**por qué** se aprobó. `GobernanzaService.reconstruir` arma esa respuesta leyendo el rastro que las
demás etapas ya dejaban:

| Pregunta del auditor | De dónde sale |
|---|---|
| ¿Qué modelo lo leyó, con qué prompt y qué esquema? | `EjecucionExtraccion`: proveedor, modelo, `versionPrompt`, `versionEsquema` |
| ¿Contra qué reglas se validó? | `ReglaPlantilla` de la versión que el documento tiene congelada, no de la versión vigente hoy |
| ¿Qué encontró y con qué severidad? | `EjecucionValidacion` con sus `HallazgoValidacion` |
| ¿A qué objeto de negocio se asoció y por qué ese? | `CandidatoAsociacion` con puntaje, razones y el motivo de selección |
| ¿Quién lo aprobó y qué corrigió? | `RevisionDocumento` con sus `CambioCampoRevision` |
| ¿Quién más lo tocó? | `EventoAuditoria` del documento |

La pieza que no existía es `completa` / `faltantes`. Una reconstrucción que devuelve campos vacíos
sin decirlo es peor que ninguna: el auditor no distingue "no pasó" de "no lo guardamos". Por eso el
modelo declara explícitamente qué eslabón falta, y sólo exige revisor cuando el documento llegó a un
estado que **requiere** decisión humana y ninguna validación fue autoaprobada.

Consultar la trazabilidad es en sí un acceso a información sensible, así que queda auditado como
`AUDITORIA_CONSULTADA`.

---

## Ciclo de vida y retención

El escenario `S12` del N2 lo define en cuatro pasos: vence el plazo, se chequea el legal hold, se
borra o anonimiza, y queda un evento de evidencia. La implementación sigue ese orden literal en
`RetencionService.evaluar`.

Las decisiones que la especificación no fijaba y hubo que tomar:

| Decisión | Por qué |
|---|---|
| El plazo corre **desde el cierre**, no desde la recepción | Es el criterio contable estándar y el conservador: un documento en trámite no se borra por haber entrado hace mucho |
| Un documento sin política **no vence** | Borrar por omisión es el peor error posible acá. Aparece en el inventario como `cerradosSinPolitica` para que alguien decida |
| `ANONIMIZAR` purga sólo los campos `CONFIDENCIAL` y `PERSONAL` | Es el mínimo que cumple protección de datos sin destruir la trazabilidad del resto |
| La `presencia` de un valor anonimizado no cambia | `PRESENTE` describe qué había en el documento original. Convertirla en un estado de purga mezclaría dos cosas distintas y rompería la invariante `NO_FIGURA` ≠ `ILEGIBLE`. La purga se marca con el campo `anonimizado` |
| Ninguna acción borra la auditoría | El registro de qué se borró es justamente lo que hay que conservar |
| El ciclo no audita cada legal hold en cada pasada | Con un job horario eso inundaría la auditoría. El hold queda probado por el evento de activación más el contador del ciclo |

`retencionAplicada` es lo que evita que un documento se trate dos veces. Sin esa marca, una política
`ANONIMIZAR` volvería a procesar el mismo documento en cada ciclo para siempre.

---

## Identidad y gobierno del tenant

El JWT lleva los permisos, pero **no es la fuente de verdad**. `FiltroAutenticacion` revalida contra
la base, en cada petición, que el usuario siga activo, y relee sus roles. La alternativa —confiar en
el token— hace que bloquear a alguien no tenga efecto hasta que su token expire, ocho horas después.
Un bloqueo que tarda ocho horas no es un bloqueo. El costo es una consulta por petición, exactamente
la misma que las cuentas de servicio ya pagaban para autenticarse por hash.

La otra invariante es que **el tenant no puede quedarse sin administradores activos**. Bloquear, dar
de baja o degradar al último administrador se rechaza, porque el resultado sería un tenant que nadie
puede administrar y que sólo se recupera tocando la base. Por eso `UsuarioRepository.contarConPermiso`
cuenta administradores activos antes de cada una de esas tres operaciones, y no simplemente usuarios
con el rol `ADMINISTRADOR`: lo que importa es quién tiene efectivamente `tenant.administrar`, venga
del rol predefinido o de uno propio.

Una cuenta de servicio nunca puede tener `tenant.administrar`. Administrar el tenant es una acción de
persona, y una credencial de integración que puede crear usuarios es una vía de escalada.

---

## Resiliencia del proveedor de IA

El problema documentado en la auditoría del 23/08/2026 era que **11 de 15 excepciones eran 429 de Gemini**.
El diseño lo trata de frente:

```
ExtractorDocumentalService
  └── RuteadorProveedorService.resolverPrincipal(tenant)
        │  falla con ProveedorNoDisponibleException reintentable
        ▼
      resolverRespaldo(tenant, proveedorDescartado)
        │  no hay respaldo disponible
        ▼
      gestionarReintento()
        · backoff exponencial: 1 s → 2 s → 4 s → … tope 60 s
        · reencola en un sorted set de Redis con timestamp de vencimiento
        · agotados los intentos: ExcepcionDocumental CUOTA_PROVEEDOR bloqueante
        · el documento vuelve a RECIBIDO, nunca se pierde
```

Cuando un reproceso tiene éxito, `resolverAutomaticamente()` cierra la excepción de cuota
de forma idempotente.

---

## El proveedor de IA y la confianza

`ProveedorGeminiService` implementa `ProveedorDocumentalIaInt`. Construye el esquema estructurado
a partir de `CampoPlantilla`, lo envía con `responseMimeType: application/json` y `temperature: 0`,
y normaliza la respuesta al `ResultadoExtraccionModel` canónico.

La credencial **nunca** se guarda en base. `ConfiguracionProveedor.referenciaSecreto` guarda una
referencia, no el secreto: `env:NEXTDOCS_GEMINI_CLAVE`. `ResolvedorSecreto` la resuelve en runtime.

### Qué se aprendió midiendo contra Gemini real

La confianza que reporta el modelo **existe y varía**, pero es gruesa y auto-reportada:

| Documento | Confianzas devueltas |
|---|---|
| Remito limpio | todas `1.0` |
| Remito con texto degradado | `0.6`, `0.7`, `0.8`, `0.95`, `1.0` |

Conclusiones que se aplicaron al diseño:

1. Es una señal **ordinal útil** para detectar documentos malos, no una probabilidad calibrada.
   Por eso se guardan las dos: `confianzaProveedor` cruda para lineage y `confianza` calibrada
   para decidir.
2. `factorCalibracionConfianza` en la versión sale de la última corrida gold aprobada
   (exactitud / confianza media del proveedor). El factor en la config de Gemini queda como
   ajuste del adaptador, no como calibración de plantilla.
3. **La confianza no puede ser el control principal.** En un documento limpio todo da `1.0`, así que
   cualquier umbral se cumple. Lo que evita aprobar un documento inválido son las reglas.
4. En el documento degradado el modelo usó `ILEGIBLE` correctamente en vez de inventar un valor,
   que es la distinción que exige `QA1-07`.

### Manejo de fallos

| Respuesta de Gemini | Traducción | Reintentable |
|---|---|---|
| 429 | `CUOTA_EXCEDIDA` | Sí → respaldo, luego backoff exponencial |
| 401 / 403 | `CREDENCIAL_INVALIDA` | No → excepción bloqueante, no consume la cola |
| 5xx | proveedor no disponible | Sí |
| 4xx restantes | petición rechazada | No |
| Sin candidatos o JSON ilegible | respuesta inválida | Sí |
| `finishReason: SAFETY` | bloqueado por política | No |

Un campo que el modelo no devuelve se completa como `ILEGIBLE` con confianza `0` y una advertencia.
**Nunca** se completa como `NO_FIGURA`, porque no saber es distinto de saber que no está.

---

## Proveedor de respaldo

Hay dos adaptadores reales detrás de `ProveedorDocumentalIaInt`, y ambos construyen el prompt con
la **misma** `InstruccionExtraccion`. Eso es lo que hace que el contrato sea de verdad canónico:
cambiar de proveedor no cambia la semántica de `PRESENTE` / `NO_FIGURA` / `ILEGIBLE`.

| Proveedor | Entrada | Uso |
|---|---|---|
| `GEMINI` | el PDF completo, con visión | principal. Funciona con escaneados |
| `DEEPSEEK` | la **capa de texto** del PDF, API compatible con OpenAI | respaldo ante 429 de Gemini |

> **Limitación honesta del respaldo.** DeepSeek recibe texto, no imágenes. Un PDF escaneado sin capa
> de texto se rechaza con un error **no reintentable** que lo dice explícitamente, en vez de
> devolver campos vacíos que parecerían una extracción válida. Para que el respaldo cubra escaneados
> hay que apuntarlo a un endpoint con visión, o agregar OCR previo.

`RuteadorProveedorService` **falla al arrancar** si dos adaptadores declaran el mismo tipo. Es una
guarda contra una clase de bug que ya nos mordió: dos implementaciones compitiendo por el mismo
slot del mapa, donde una pisa a la otra en silencio.

---

## Persistencia

PostgreSQL 16. Esquema versionado con Flyway y **`ddl-auto: validate`**: si una entidad y la migración
divergen, la aplicación no arranca. No hay generación automática de esquema.

- Ids: `VARCHAR(36)` UUID generado por Hibernate.
- Timestamps: `Instant` sobre `TIMESTAMP(6) WITH TIME ZONE`, siempre UTC.
- Ciclo de vida: `alta` y `baja` (borrado lógico), igual que Follow.
- JSON: `jsonb` nativo vía `@JdbcTypeCode(SqlTypes.JSON)` en `configuracion`, `carga`, `detalle` y `parametros`.
- Índices pensados para las consultas reales de la bandeja: `(tenant_id, estado)`, `(tenant_id, alta)`,
  `(tenant_id, hash_contenido)`.

---

## Lo que falta y por qué importa

| Falta | Riesgo si no se hace |
|---|---|
| Portal frontend | El backend ya expone bandeja, plantillas, gobernanza, costo y un compose reproducible; falta el portal |
