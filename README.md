# NEXT DOC AI

> Inteligencia documental y automatización de procesos.
> *Tus documentos saben qué hacer después.*

Plataforma **independiente** de captura, extracción, validación, asociación y gobernanza documental.
No es un módulo de Follow: Follow, CIMA y Valid360.ai son **consumidores opcionales** vía conector, SSO y eventos.

---

## Estado actual

| Área | Estado |
|---|---|
| Andamiaje del backend (Maven, Spring Boot 3.4, Java 21) | ✅ |
| Infraestructura local (PostgreSQL, Redis, MinIO) | ✅ |
| Enumeraciones del núcleo documental | ✅ |
| Entidades JPA del núcleo (27) | ✅ |
| Migración Flyway `V1` del esquema | ✅ |
| Configuración, seguridad JWT y multi-tenancy | ⏳ en curso |
| Repositorios, servicios y controladores REST | ⏳ pendiente |
| Proveedor IA abstracto + worker de extracción | ⏳ pendiente |
| Motor de reglas y validación | ⏳ pendiente |
| Outbox + webhooks HMAC | ⏳ pendiente |

---

## Decisiones de arquitectura tomadas

1. **Sistema desacoplado de Follow.** Ningún servicio del core importa modelos de Follow.
   Las referencias externas se guardan como `ReferenciaExterna` (`origen` / `tipoObjeto` / `idObjeto` / `tenantOrigen`).
   Follow entra únicamente como conector registrado en el Integration Hub.
2. **Monorepo con boundaries lógicos.** Un solo deployable al inicio (menos complejidad operativa),
   pero con contratos y capas de dominio desacopladas para poder extraer servicios cuando el volumen lo justifique.
3. **Stack moderno.** Spring Boot 3.4 + Java 21 + PostgreSQL 16, en lugar del Spring Boot 2.3 / Java 11 de Follow,
   que está fuera de soporte. Se conserva **la nomenclatura y la estructura de paquetes de Follow**.
4. **Multi-tenancy por columna discriminante.** Toda entidad de negocio tiene `tenant`.
   El aislamiento se refuerza en repositorio y servicio, nunca sólo en el controlador.
5. **Proveedor de IA abstracto.** `Gemini`, `DeepSeek`, `ABBYY` y `Simulado` detrás de un mismo contrato
   que devuelve un resultado canónico. Cambiar de proveedor no cambia el payload de negocio.
6. **Asincronía obligatoria.** La API guarda y encola; el worker es el único que llama al proveedor de IA.

---

## Convenciones de código

Estas reglas son **obligatorias** y replican las de `follow-backend`:

- **Todo en español**: nombres de clases, métodos, variables, columnas y tablas.
- **camelCase** en Java; `snake_case` en la base (resuelto por la estrategia de nombres de Hibernate).
- **Prohibido escribir comentarios en el código.** El nombre de la clase, del método y de la variable
  tienen que alcanzar. Si algo necesita explicación, se documenta en `docs/`.
- Paquetes: `config`, `clientes`, `convertidores`, `entidades`, `enumeraciones`, `errores`,
  `exceptions`, `filtros`, `interfaces`, `modelos`, `repositorios`, `restControladores`, `servicios`, `utiles`.
- Entidades con `@Data` de Lombok, `implements Serializable`, id `String` UUID.
- Campos de ciclo de vida: `alta` (creación) y `baja` (borrado lógico), igual que Follow.
- DTOs terminan en `Model`, `ReqModel` o `ResModel`. Servicios en `Service`. Controladores en `RestController`.
- Cada enumeración expone un `desde(String)` tolerante a mayúsculas/minúsculas.

---

## Estructura del repositorio

```
nextdocs-ai/
├── backend/                         Spring Boot 3.4 · Java 21
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/nextdocs/ai/
│       │   ├── NextDocsAiApplication.java
│       │   ├── clientes/            clientes HTTP de terceros
│       │   ├── config/              beans de configuración y properties
│       │   ├── convertidores/       entidad → modelo
│       │   ├── entidades/           entidades JPA
│       │   ├── enumeraciones/       enums de dominio
│       │   ├── errores/             manejador global y modelo de error
│       │   ├── exceptions/          excepciones de negocio
│       │   ├── filtros/             filtros de servlet (JWT, tenant, correlación)
│       │   ├── interfaces/          contratos de proveedores y conectores
│       │   ├── modelos/             DTOs de entrada y salida
│       │   ├── repositorios/        Spring Data JPA
│       │   ├── restControladores/   API REST v1
│       │   ├── servicios/           lógica de negocio
│       │   └── utiles/              utilidades transversales
│       └── resources/
│           ├── application.yml
│           └── db/migration/        migraciones Flyway
├── docs/                            documentación viva del proyecto
└── compose.yml                      PostgreSQL + Redis + MinIO
```

---

## Modelo de datos del núcleo

**Identidad y tenancy:** `Tenant`, `Usuario`, `Rol`, `CuentaServicio`

**Plantillas:** `PlantillaDocumental`, `VersionPlantilla`, `CampoPlantilla`, `ReglaPlantilla`

**Documental:** `Documento`, `ArchivoDocumento`, `SegmentoDocumento`, `SeguimientoOriginalFisico`

**Extracción:** `EjecucionExtraccion`, `ValorExtraido`

**Validación:** `EjecucionValidacion`, `HallazgoValidacion`

**Asociación:** `CandidatoAsociacion`

**Revisión humana:** `RevisionDocumento`, `CambioCampoRevision`, `ExcepcionDocumental`

**Gobernanza e integración:** `EventoAuditoria`, `EventoSalida`, `SuscripcionWebhook`, `EntregaWebhook`, `ConfiguracionProveedor`, `PoliticaRetencion`

### Máquina de estados del documento

```
RECIBIDO → PROCESANDO → EXTRAIDO → VALIDADO → APROBADO → CERRADO
                ↓            ↓          ↓          ↑
             DIVIDIDO    OBSERVADO ─────┴──────────┘
                             ↓
                         RECHAZADO
```

Reglas que no se negocian:
- La confianza de lectura **nunca** aprueba por sí sola. La decisión la toman las reglas.
- `NO_FIGURA` y `ILEGIBLE` son estados distintos y deben producir hallazgos distintos.
- Un documento con dos candidatos de asociación va a revisión humana; jamás se elige en silencio.
- Una plantilla publicada no se modifica: se crea una versión nueva.

---

## Cómo levantar el entorno

```bash
docker compose up -d                 # PostgreSQL 5434 · Redis 6381 · MinIO 9102/9101
cd backend && ./mvnw spring-boot:run # API en http://localhost:8090
```

Documentación de la API: `http://localhost:8090/swagger-ui.html`

---

## Próximos pasos

Orden de trabajo. Quien retome el proyecto arranca por el primero sin marcar.

- [x] Andamiaje Maven, `compose.yml` y `application.yml`
- [x] Enumeraciones del núcleo documental
- [x] Entidades JPA + migración Flyway `V1`
- [ ] `config/`: properties tipadas, `SecurityConfig`, `OpenApiConfig`, cliente S3, Redis
- [ ] `filtros/`: filtro JWT, filtro de contexto de tenant, filtro de correlación (`correlacionId` en MDC)
- [ ] `errores/` + `exceptions/`: `ErrorHandler` global con el mismo contrato de error que Follow
- [ ] `repositorios/`: uno por entidad, con consultas **siempre** filtradas por `tenant`
- [ ] `servicios/AutenticacionService`: login, refresh, cuentas de servicio con clave hasheada
- [ ] `servicios/AlmacenamientoService`: subida a S3/MinIO, checksum, URL firmada con TTL
- [ ] `servicios/IngestaDocumentalService`: idempotencia por `claveIdempotencia`, validación MIME real
      con Tika, límite de tamaño, cuarentena y encolado
- [ ] `interfaces/ProveedorDocumentalIa` + adaptadores `Simulado` y `Gemini`
- [ ] `servicios/ExtractorDocumentalService` y worker de cola con reintento exponencial, DLQ y
      circuit breaker para el 429 del proveedor
- [ ] `servicios/ValidacionDocumentalService`: motor de reglas versionadas por `VersionPlantilla`
- [ ] `servicios/SegmentacionDocumentalService`: split de PDF multi-documento con PDFBox
- [ ] `servicios/RevisionDocumentalService` y `ExcepcionService` con SLA y asignación
- [ ] `servicios/AuditoriaService`: registro de toda acción con actor, recurso, hash antes/después y correlación
- [ ] `servicios/EventoSalidaService`: outbox transaccional + despachador de webhooks firmados con HMAC
- [ ] `restControladores/`: `DocumentoRestController`, `PlantillaRestController`,
      `ExcepcionRestController`, `AutenticacionRestController`, `TenantRestController`
- [ ] Suite de QA de la Etapa 1 (`QA1-01` a `QA1-10` del documento N3)
- [ ] `FollowConnector` como adaptador aislado detrás de la interfaz de asociación
- [ ] Frontend del portal standalone
- [ ] Etapa 2: Workflow Definition/Runtime, Process Studio y Sentinel de consistencia

---

## Servicios relacionados

| Servicio | Rol | Repositorio |
|---|---|---|
| **NEXTDOCS-AI** | Core documental. Este repositorio. | `gabibenitezzz003/NEXTDOCS-IA` |
| **workflow** | Motor de procesos. Microservicio aparte, Etapa 2. | `Follow-Hub/workflow` rama `NEXT-DOCS-AI` |
| **docvance-ai** | Servicio auxiliar de IA documental. | `gabibenitezzz003/docvance-ai` rama `next-ai` |
| **follow-backend** | Consumidor vía conector. No es dependencia. | `Follow-Hub/follow-backend` |
| **follow-front** | Consumidor embebido vía SSO. No es dependencia. | `Follow-Hub/follow-front` |

---

## Documentación fuente

La especificación funcional y técnica vive en `dev/NEXT_DOC_AI_SRP084_V7_FINAL_20260823/`
(fuera de este repositorio). Los documentos que gobiernan las decisiones son:

- `03_ARQUITECTURA_FUNCIONAL_TECNICA_..._MICROSERVICIOS_V7` — arquitectura, contratos, eventos, modelo de datos
- `N1-SPR084-V7-..._PRD` — alcance de producto
- `N2-SPR084-V7-..._ARQUITECTURA_FLUJOS_ESCENARIOS` — flujos y escenarios
- `N3-SPR084-V7-..._ESPECIFICACION_DESARROLLO_QA_UX` — backlog, criterios de aceptación y QA
