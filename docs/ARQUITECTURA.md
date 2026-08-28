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
| Apagar el Workflow no rompe nada | La Etapa 2 todavía no existe; el núcleo documental no la referencia |

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
| Template Service | `PlantillaService`, `ValidadorPlantillaService`, `MaquinaEstadoPlantilla` | ✅ |
| Validation Service | `ValidacionDocumentalService` | ✅ |
| Matching Service | `AsociacionService`, `ConectorAsociacionInt`, `RegistroCircuitosService` | ✅ |
| Review / Exception | `RevisionDocumentalService`, `ExcepcionDocumentalService` | ✅ |
| Governance & Audit | `AuditoriaService`, entidades `EventoAuditoria` / `PoliticaRetencion` | ⚠️ auditoría lista, falta API de gobernanza |
| Event / Integration Hub | `EventoSalidaService`, `DespachadorEventosService` | ✅ |
| Follow Connector | `FollowConnector`, `FollowCliente` | ✅ |
| Workflow Definition/Runtime | — | ❌ Etapa 2 |

---

## Flujo del documento

```
POST /api/v1/documentos
        │
        ▼
IngestaDocumentalService
   · valida tamaño y extensión
   · detecta el MIME real con Tika (no confía en el header)
   · calcula sha256
   · resuelve idempotencia por Idempotency-Key (o por hash si no viene)
   · guarda en S3/MinIO, cuenta páginas con PDFBox
   · publica document.received en el outbox
   · encola en Redis DESPUÉS del commit
        │
        ▼  RECIBIDO
TrabajadorExtraccionService (@Scheduled sobre Redis)
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
   se escribe en la transacción de negocio; `DespachadorEventosService` lo entrega después.
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
| Secretos en el navegador | Ninguno. El front usa sesión; las integraciones usan `X-Clave-Servicio` server-to-server |
| Archivos | MIME real por contenido con Tika, no por extensión ni por header |
| Descarga del original | URL firmada con TTL de 15 min. El bucket no es público |
| Webhooks | Firma HMAC-SHA256 del cuerpo en `X-Nextdocs-Firma` |
| Trazabilidad | `correlacionId` en el MDC, en la respuesta, en cada evento y en cada registro de auditoría |

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
2. `factorCalibracionConfianza` es un ajuste lineal provisorio. La calibración real sale del
   dataset gold de la tarea 2, no de un número elegido a mano.
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
| Antivirus / antimalware en la ingesta | `SEC-04` del N3 no se cumple: un archivo malicioso llega al proveedor de IA |
| Segmentación de PDF multi-documento | `QA1-02`: un PDF con 10 remitos hoy entra como uno solo |
| Tests automatizados de los casos `QA1-01` a `QA1-10` | El `Definition of Done` del N3 los exige antes de release |
| Retención y legal hold ejecutándose | `GOV-02`: la política existe en el modelo pero nadie la aplica |
