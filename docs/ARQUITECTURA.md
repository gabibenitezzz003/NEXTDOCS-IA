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
| Document AI Orchestrator | `ExtractorDocumentalService`, `RuteadorProveedorService`, `TrabajadorExtraccionService` | ✅ |
| Template Service | Entidades `PlantillaDocumental` / `VersionPlantilla` / `CampoPlantilla` / `ReglaPlantilla` | ⚠️ modelo listo, falta API |
| Validation Service | `ValidacionDocumentalService` | ✅ |
| Matching Service | `ConectorAsociacionInt`, entidad `CandidatoAsociacion` | ⚠️ contrato listo, falta implementación |
| Review / Exception | `RevisionDocumentalService`, `ExcepcionDocumentalService` | ✅ |
| Governance & Audit | `AuditoriaService`, entidades `EventoAuditoria` / `PoliticaRetencion` | ⚠️ auditoría lista, falta API de gobernanza |
| Event / Integration Hub | `EventoSalidaService`, `DespachadorEventosService` | ✅ |
| Follow Connector | — | ❌ Etapa siguiente |
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
        ├── hay REQUIERE_REVISION ─► VALIDADO ► OBSERVADO + ExcepcionDocumental ALTA
        └── sin hallazgos ─────────► VALIDADO ► APROBADO sólo si autoaprobable
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
| API de plantillas con `draft → test → publish → rollback` | Hoy las plantillas se cargan por SQL. Sin quality gate una versión mala llega a producción |
| Antivirus / antimalware en la ingesta | `SEC-04` del N3 no se cumple: un archivo malicioso llega al proveedor de IA |
| Segmentación de PDF multi-documento | `QA1-02`: un PDF con 10 remitos hoy entra como uno solo |
| `FollowConnector` detrás de `ConectorAsociacionInt` | Sin matching no hay `QA1-05` ni `QA1-06` |
| Adaptador Gemini real | Hoy sólo existe `SIMULADO`, que sirve para desarrollo y tests pero no para vender |
| Tests automatizados de los casos `QA1-01` a `QA1-10` | El `Definition of Done` del N3 los exige antes de release |
| Retención y legal hold ejecutándose | `GOV-02`: la política existe en el modelo pero nadie la aplica |
