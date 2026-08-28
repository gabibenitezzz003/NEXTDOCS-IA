# NEXT DOC AI

> Inteligencia documental y automatización de procesos.
> *Tus documentos saben qué hacer después.*

Plataforma **independiente** de captura, extracción, validación, asociación y gobernanza documental.
No es un módulo de Follow: Follow, CIMA y Valid360.ai son **consumidores opcionales** vía conector, SSO y eventos.

> ### 👉 ¿Tomás el proyecto sin contexto? Empezá por **[docs/EMPEZAR_ACA.md](docs/EMPEZAR_ACA.md)**
> Son 5 minutos: cómo levantarlo, las reglas que no se negocian, las trampas que ya nos costaron
> tiempo y por dónde seguir.

---

## Estado actual

El núcleo documental de la Etapa 1 está **funcionando end-to-end y verificado contra infraestructura real**.

| Área | Estado |
|---|---|
| Andamiaje Maven · Spring Boot 3.4 · Java 21 · PostgreSQL 16 | ✅ |
| Infraestructura local (PostgreSQL, Redis, MinIO) | ✅ |
| 27 enumeraciones + 27 entidades JPA + migración Flyway `V1` | ✅ |
| Multi-tenancy con aislamiento verificado (`SEC-01`) | ✅ |
| Autenticación JWT + cuentas de servicio con clave hasheada | ✅ |
| Ingesta idempotente con MIME real (Tika) y conteo de páginas (PDFBox) | ✅ |
| Antivirus con cuarentena, ClamAV o permisivo, fail-closed por defecto | ✅ |
| Seguimiento del original físico con política por plantilla | ✅ |
| Almacenamiento S3/MinIO con checksum y URL firmada | ✅ |
| Máquina de estados del documento con transiciones validadas | ✅ |
| Proveedor de IA abstracto + adaptadores `GEMINI` y `SIMULADO` + router con respaldo | ✅ |
| Worker de extracción con backoff exponencial y reencolado | ✅ |
| Motor de validación con reglas versionadas por plantilla | ✅ |
| Revisión humana con correcciones, sobreescritura y motivo obligatorio | ✅ |
| Centro de excepciones con SLA, prioridad y deduplicación | ✅ |
| Auditoría de toda acción con actor, recurso y correlación | ✅ |
| Outbox transaccional + despachador de webhooks firmados con HMAC | ✅ |
| API REST v1 de documentos, excepciones y autenticación | ✅ |
| API de plantillas con ciclo de vida, versionado, publish y rollback | ✅ |
| Quality gate con dataset gold: no se publica una versión que empeora | ✅ |
| Adaptador Gemini real (`gemini-2.5-flash`) con esquema estructurado | ✅ |
| Segmentación de PDF multi-documento (páginas fijas o patrón) | ✅ |
| Matching con circuit breaker + `FollowConnector` aislado | ✅ |
| API de gobernanza: reconstrucción de la decisión y exportación de auditoría | ✅ |
| Retención en ejecución: plazo desde el cierre, legal hold y prueba de borrado | ✅ |
| Administración de tenant, usuarios, roles propios y cuentas de servicio | ✅ |
| API de webhooks: suscripciones, prueba firmada y monitor de entregas | ✅ |
| Costo por tenant: efectivo por documento correcto y presupuesto opcional | ✅ |
| Suite de QA automatizada: 57 unitarios + 104 de integración | ✅ |
| Imagen Docker, `compose --profile app` y CI en GitHub Actions | ✅ |

### Verificado con el sistema corriendo

```
POST /autenticacion/ingresar   → 200, 12 permisos del rol ADMINISTRADOR
POST /documentos               → 201 RECIBIDO
                                 worker → PROCESANDO → EXTRAIDO → VALIDADO → OBSERVADO
GET  /documentos/{id}/detalle  → extracción con confianza por campo,
                                 PRESENTE / ILEGIBLE distinguidos,
                                 2 hallazgos BLOQUEANTE por campos requeridos ilegibles,
                                 autoaprobado = false pese a confianza 0.92 en el resto
GET  /excepciones?estado=ABIERTA → 1 excepción VALIDACION/CRITICA con SLA de 4 h
POST /documentos/{id}/revisiones → OBSERVADO → APROBADO con 2 correcciones auditadas
POST .../revisiones sin motivo   → 400 "La decision requiere un motivo explicito"
Lectura cross-tenant             → 404, bandeja del intruso con 0 documentos
Sin token                        → 403

Plantillas
POST /plantillas                 → crea la plantilla con su versión 1 en BORRADOR
POST .../publicar sin campos     → 400 quality gate: "no tiene ningun campo definido"
POST .../validar                 → detecta reglas que apuntan a campos inexistentes
POST .../publicar                → v1 PUBLICADA, editable=false
POST .../campos sobre publicada  → 400 "Una version PUBLICADA es inmutable"
POST .../versiones (clonando v1) → v2 BORRADOR con 4 campos y 2 reglas clonados
POST .../publicar v2             → v1 pasa a DEPRECADA automáticamente
POST .../revertir/v1             → v1 vuelve a PUBLICADA, v2 a DEPRECADA
QA-WF-03                         → doc ingresado con v1 sigue en v1 y con esquema e1
                                   tras publicarse la v2; el doc nuevo usa v2 y esquema e2

Gemini real (gemini-2.5-flash)
PDF de remito → extrae los 5 campos correctamente, normaliza
                CUIT 30-71234567-4 → 30712345674
                fecha 14/08/2026   → 2026-08-14
                conformidad NO     → false
                576 tokens entrada, 323 salida, USD 0.00098, 6.5 s
PDF degradado → confianzas 0.6 a 0.95 y usa ILEGIBLE en vez de inventar (QA1-07)
429 / 403     → reintentable vs no reintentable, verificado con servidor falso
QA1-03        → remito sin conformidad con confianza 1.0 en TODOS los campos
                queda OBSERVADO, autoaprobado=false, porque lo decide la regla

Matching (Follow simulado, 4 escenarios end-to-end)
1 candidato   → se fija solo, sujeto = FOLLOW/PEDIDO/ped-1
2 candidatos  → QA1-06: NO elige, excepción ASOCIACION_AMBIGUA, sujeto vacío
                tras la selección humana → sujeto = ped-2, el resto descartado
Follow caído  → QA1-05: excepción CONECTOR con el HTTP 500, y el core sigue (QA-FOL-01)
0 candidatos  → sin excepción de conector: es un resultado distinto de "falló"

Segmentación (QA1-02, PDF real de 10 remitos)
padre         → DIVIDIDO con 10 segmentos, y CERO extracciones sobre el padre
10 hijos      → cada uno con su rango de páginas, su archivo propio en S3
                y su propio motivo de corte
extracción    → los 10 extrajeron SUS datos: remito 98471..98480,
                bultos 10..100, conformidad alternando true/false

Antivirus (SEC-04, ClamAV 1.5.4 real)
EICAR suelto  → 415 en el chequeo de MIME, ni llega al antivirus
PDF con EICAR → RECHAZADO, amenaza Eicar-Signature, bucket de cuarentena,
                excepción SEGURIDAD/CRITICA y CERO extracciones:
                el contenido malicioso nunca llegó a Gemini
PDF limpio    → LIMPIO por CLAMAV, sigue el pipeline normal

Original físico (QA1-08)
NO_REQUIERE          → no se crea seguimiento
REQUIERE_SEGUIMIENTO → documento CERRADO y el papel sigue PENDIENTE y visible
REQUIERE_PARA_CIERRE → cierre rechazado con 400 hasta recibir el papel,
                       después CERRADO; archivar sin recibir da 400

Gobernanza (GOV-01, GOV-02)
doc aprobado         → trazabilidad completa=true: proveedor, modelo prueba-v1,
                       prompt p1, esquema e1, regla REMITO_OBLIGATORIO,
                       match PEDIDO:ped-77 y el revisor que lo aprobó
doc sin extraer      → completa=false y faltantes declara extraccion, proveedor,
                       modelo y validacion: no devuelve huecos en silencio
doc autoaprobado     → completa=true sin revisor, porque nunca hubo decisión humana
trazabilidad ajena   → 404, no filtra nada del otro tenant
exportación CSV      → encabezados, respeta el filtro y queda auditada como
                       AUDITORIA_EXPORTADA con formato, cantidad y filtros
fórmulas en el CSV   → =, +, - y @ neutralizados con comilla simple
legal hold sin motivo → 400; con motivo queda auditado con actor y motivo
legal hold repetido   → 400 en vez de auditar un cambio que no ocurrió
política duplicada    → 409 sobre la misma clase del tenant

Retención (GOV-02, escenario S12)
cierre con política  → retenerHasta = cerrado + duracionDias
cierre sin política  → no vence nunca; queda en cerradosSinPolitica
vencido + legal hold → OMITIDA_POR_RETENCION_LEGAL: no borra, no marca,
                       el archivo sigue estando
levantar el hold     → el mismo documento pasa a APLICADA y se elimina
CONSERVAR            → marca tratado sin borrar nada
ANONIMIZAR           → borra el original, purga sólo el campo PERSONAL y
                       deja intacto el campo INTERNA
ELIMINAR             → borra el original y da de baja el documento;
                       la auditoría del documento sigue consultable
prueba de borrado    → RETENCION_APLICADA con clase, acción, hash del
                       contenido y checksum de cada archivo borrado
segunda pasada       → OMITIDA_YA_APLICADA, sale del listado de vencidos
ciclo completo       → elimina el tratable, cuenta el retenido y deja un
                       RETENCION_CICLO_EJECUTADO por tenant

Costo por tenant
2 docs, USD 0.50 c/u → uno APROBADO y uno OBSERVADO: efectivo = 1.00, no 0.50
sin correctos        → costoInferencia queda, efectivo es null
ALERTA al 40%        → alerta=true y la ingesta sigue
BLOQUEAR al tope     → el alta nueva recibe 400; el replay idempotente no
politica apagada     → aunque el numero se pase, no corta
tenant ajeno         → ve 0, no el gasto del otro

Administración (usuarios, roles, cuentas de servicio)
alta de usuario      → activo, con sus roles y permisos efectivos
email duplicado      → 409 dentro del tenant; el mismo email sí puede
                       existir en dos tenants distintos
bloquear al único admin  → 400 "el tenant quedaría sin administrador activo"
degradar al único admin  → 400 por la misma razón
con dos admins           → sí se puede bloquear al primero
auto-bloqueo             → 400, nadie se bloquea a sí mismo
rol predefinido      → 400 al modificarlo o borrarlo
rol propio con usuarios  → 400 hasta reasignarlos
permiso inventado    → 400 contra el catálogo de Permiso
clave de cuenta      → se devuelve una sola vez; en base sólo el sha256
cuenta con tenant.administrar → 400, es vía de escalada
cuenta revocada      → deja de autenticar de inmediato
clave propia         → exige la actual y rechaza repetirla
reset por admin      → auditado con el email de quien lo hizo
```

Hibernate arranca con `ddl-auto: validate`, así que el arranque limpio **prueba** que las entidades
y las migraciones Flyway coinciden exactamente.

### Tests

```bash
docker compose up -d      # PostgreSQL, Redis y MinIO
cd backend && ./mvnw verify
```

- **57 tests unitarios** — no necesitan nada levantado
- **104 tests de integración** (`*IT`) — usan la base `nextdocs_prueba`, que se crea sola

Los de integración cubren los casos del N3: `QA1-01` idempotencia, `QA1-02` split de 10 remitos,
`QA1-03` la confianza no aprueba, `QA1-04` cuota del proveedor, `QA1-05` timeout ≠ cero candidatos,
`QA1-06` dos candidatos van a revisión, `QA1-07` `ILEGIBLE` ≠ `NO_FIGURA`, `QA1-08` original físico,
`QA1-10` rollback de plantilla, `GOV-04` quality gate con gold, `QA-WF-03` el documento no cambia de versión, `QA-FOL-01` el núcleo
sigue sin conector, `SEC-01` aislamiento cross-tenant, `GOV-01` reconstrucción completa de la decisión
sobre un documento aprobado, `GOV-02` legal hold con motivo obligatorio y `GOV-03` overrides. El
monitor de webhooks verifica firma HMAC, auto-pausa por fallos y el aislamiento de suscripciones.
El costo por tenant verifica el efectivo sobre documentos correctos, el bloqueo de ingesta y que un
tenant ajeno vea cero.

Si la infraestructura no está levantada, los tests de integración se **saltan** con un mensaje claro
en vez de fallar.

---

## Decisiones de arquitectura

1. **Sistema desacoplado de Follow.** Ningún servicio del core importa modelos de Follow.
   Las referencias externas se guardan como `ReferenciaExterna` (`origen` / `tipoObjeto` / `idObjeto`).
2. **Monorepo con boundaries lógicos.** Un solo deployable, con contratos y capas de dominio
   desacopladas para poder extraer servicios cuando el volumen lo justifique.
3. **Stack moderno.** Spring Boot 3.4 + Java 21 en lugar del Spring Boot 2.3 / Java 11 de Follow,
   que está fuera de soporte. Se conserva **la nomenclatura y la estructura de paquetes de Follow**.
4. **Multi-tenancy en tres capas**: token → repositorio → servicio. Nunca sólo en el controlador.
5. **Proveedor de IA abstracto.** Cambiar de proveedor no cambia el payload de negocio.
6. **Asincronía obligatoria.** La API guarda y encola; el worker es el único que llama al proveedor.
7. **Los servicios devuelven modelos, no entidades.** La conversión ocurre dentro de la transacción.
   Esto evita `LazyInitializationException` en el borde HTTP.

Detalle completo en [docs/ARQUITECTURA.md](docs/ARQUITECTURA.md).

---

## Convenciones de código

Obligatorias. Replican las de `follow-backend`.

- **Todo en español**: clases, métodos, variables, columnas y tablas.
- **camelCase** en Java; `snake_case` en la base.
- **Prohibido escribir comentarios en el código.** El nombre de la clase, del método y de la variable
  tienen que alcanzar. Lo que necesite explicación va en `docs/`.
- Paquetes: `config`, `clientes`, `convertidores`, `entidades`, `enumeraciones`, `errores`,
  `exceptions`, `filtros`, `interfaces`, `modelos`, `repositorios`, `restControladores`,
  `servicios`, `specificationBuilder`, `utiles`.
- Entidades con `@Data`, `implements Serializable`, id `String` UUID, campos `alta` y `baja`.
- DTOs terminan en `Model`, `ReqModel` o `ResModel`. Servicios en `Service`. Controladores en
  `RestController`. Contratos en `Int`. Convertidores en `Converter`.
- Cada enumeración expone un `desde(String)` tolerante a mayúsculas y minúsculas.
- Los controladores extienden `ControladorRest<T>` y obtienen tenant y usuario de ahí.

---

## Estructura

```
nextdocs-ai/
├── backend/                         Spring Boot 3.4 · Java 21
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/nextdocs/ai/
│       │   ├── NextDocsAiApplication.java
│       │   ├── config/              properties tipadas, seguridad, S3, OpenAPI, arranque
│       │   ├── convertidores/       entidad → modelo
│       │   ├── entidades/           27 entidades JPA
│       │   ├── enumeraciones/       27 enums de dominio
│       │   ├── errores/             ErrorHandler global y WebErrorModel
│       │   ├── exceptions/          excepciones de negocio
│       │   ├── filtros/             correlación y autenticación
│       │   ├── interfaces/          ProveedorDocumentalIaInt, ConectorAsociacionInt
│       │   ├── modelos/             DTOs de entrada y salida
│       │   ├── repositorios/        26 repositorios, siempre filtrados por tenant
│       │   ├── restControladores/   API REST v1
│       │   ├── servicios/
│       │   │   ├── jwt/             TokenService
│       │   │   └── proveedores/     adaptadores de IA y router
│       │   ├── specificationBuilder/
│       │   └── utiles/              correlación, seguridad, hash, máquina de estados
│       └── resources/
│           ├── application.yml
│           └── db/migration/        V1 núcleo · … · V11 costo por tenant
├── backend/Dockerfile               imagen multi-stage (Maven 3.9 → JRE 21)
├── .github/workflows/verificar.yml  mvn verify + build de imagen
├── .env.example                     plantilla de variables; copiala a .env
├── frontend/                        Portal React 19 · TypeScript · Vite · Tailwind 4
│   ├── src/
│   │   ├── api/                     cliente axios con refresco y manejo de errores
│   │   ├── componentes/             marca, insignias, estados y disposicion
│   │   ├── contextos/               sesion
│   │   ├── paginas/                 resumen, bandeja, visor, excepciones, plantillas
│   │   └── tipos/                   contratos de la API
│   └── package.json
├── docs/
│   ├── EMPEZAR_ACA.md               traspaso: leer esto primero
│   ├── ARQUITECTURA.md              bounded contexts, flujos, reglas invariantes
│   ├── API.md                       endpoints, permisos, errores, webhooks
│   ├── DESPLIEGUE.md                compose, imagen, CI y recuperación
│   └── TODO.md                      plan de trabajo hasta terminar el producto
└── compose.yml                      infra siempre; API con --profile app
```

---

## Modelo de datos

**Identidad y tenancy:** `Tenant`, `Usuario`, `Rol`, `CuentaServicio`
**Plantillas:** `PlantillaDocumental`, `VersionPlantilla`, `CampoPlantilla`, `ReglaPlantilla`
**Documental:** `Documento`, `ArchivoDocumento`, `SegmentoDocumento`, `SeguimientoOriginalFisico`
**Extracción:** `EjecucionExtraccion`, `ValorExtraido`
**Validación:** `EjecucionValidacion`, `HallazgoValidacion`
**Asociación:** `CandidatoAsociacion`
**Revisión:** `RevisionDocumento`, `CambioCampoRevision`, `ExcepcionDocumental`
**Gobernanza e integración:** `EventoAuditoria`, `EventoSalida`, `SuscripcionWebhook`,
`EntregaWebhook`, `ConfiguracionProveedor`, `PoliticaRetencion`

### Máquina de estados

```
RECIBIDO → PROCESANDO → EXTRAIDO → VALIDADO → APROBADO → CERRADO
                ↓            ↓          ↓          ↑
             DIVIDIDO    OBSERVADO ─────┴──────────┘
                             ↓
                         RECHAZADO
```

Reglas invariantes:
- La confianza de lectura **nunca** aprueba por sí sola. La decisión la toman las reglas.
- `NO_FIGURA` e `ILEGIBLE` son estados distintos y producen hallazgos distintos.
- Dos candidatos de asociación van a revisión humana; jamás se elige en silencio.
- Una plantilla publicada no se modifica: se crea una versión nueva.
- Sobreescribir un hallazgo exige motivo, actor y queda auditado.

---

## Cómo levantar el entorno

```bash
cp .env.example .env          # completá NEXTDOCS_GEMINI_CLAVE
docker compose up -d
cd backend && ./mvnw spring-boot:run
```

Sin `NEXTDOCS_GEMINI_CLAVE` el sistema arranca igual y usa el proveedor `SIMULADO`.
La credencial **nunca** se guarda en base: `ConfiguracionProveedor.referenciaSecreto` guarda
`env:NEXTDOCS_GEMINI_CLAVE` y se resuelve en runtime.

- API: `http://localhost:8090`
- Swagger: `http://localhost:8090/swagger-ui.html`
- Consola MinIO: `http://localhost:9101` (`nextdocs` / `nextdocs123`)

Al arrancar se crea el tenant `demo` con el usuario `admin@nextdocs.ai` / `nextdocs123`.
Se desactiva con `NEXTDOCS_CREAR_TENANT_DEMO=false`.

> Sin JDK local se puede compilar y ejecutar con Docker:
> `docker run --rm --network host -v "$PWD":/app -v nextdocs-m2:/root/.m2 -w /app maven:3.9-eclipse-temurin-21 mvn spring-boot:run`

---

## Próximos pasos

El plan completo hasta terminar el producto está en **[docs/TODO.md](docs/TODO.md)**:
32 tareas en 4 fases, con criterios de aceptación y dependencias.

Quien retome el proyecto arranca por la **primera tarea sin marcar de la Fase 1**.

| Fase | Alcance | Tareas |
|---|---|---|
| **1** | Cerrar la Etapa 1 vendible (motor documental standalone) | 1–14 |
| **2** | Portal, canales y KPI → completa el MVP 1 | 15–20 |
| **3** | Etapa 2: Workflow Runtime y Process Studio | 21–27 |
| **4** | Etapa 3: Sentinel, Simulation Lab y Process Copilot | 28–32 |

Bloqueantes inmediatos para poder vender:

1. ~~API de plantillas con ciclo de vida y rollback~~ — **terminado**
2. ~~Adaptador Gemini real~~ — **terminado**, verificado contra Gemini de verdad
3. ~~Matching + `FollowConnector`~~ — **terminado**

Los tres bloqueantes de venta están cerrados. Lo que sigue es endurecer y completar:
seguimiento del original físico, gobernanza y la suite de QA automatizada.

---

## Servicios relacionados

| Servicio | Rol | Repositorio |
|---|---|---|
| **NEXTDOCS-AI** | Core documental. Este repositorio. | `gabibenitezzz003/NEXTDOCS-IA` |
| **workflow** | Motor de procesos. Microservicio aparte, Etapa 2. | `Follow-Hub/workflow` rama `NEXT-DOCS-AI` |
| **docvance-ai** | Servicio auxiliar de IA documental. | `gabibenitezzz003/docvance-ai` rama `next-ai` |
| **follow-backend** | Consumidor vía conector. **No es dependencia.** | `Follow-Hub/follow-backend` rama `integraciones-pedidos` |
| **follow-front** | Consumidor embebido vía SSO. **No es dependencia.** | `Follow-Hub/follow-front` rama `Integraciones-front` |

---

## Documentación fuente

La especificación funcional y técnica vive en `dev/NEXT_DOC_AI_SRP084_V7_FINAL_20260823/`
(fuera de este repositorio). Los documentos que gobiernan las decisiones:

- `03_ARQUITECTURA_FUNCIONAL_TECNICA_..._MICROSERVICIOS_V7` — arquitectura, contratos, eventos, datos
- `N1-SPR084-V7-..._PRD` — alcance de producto
- `N2-SPR084-V7-..._ARQUITECTURA_FLUJOS_ESCENARIOS` — flujos y escenarios
- `N3-SPR084-V7-..._ESPECIFICACION_DESARROLLO_QA_UX` — backlog, criterios de aceptación y QA
