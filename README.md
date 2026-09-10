# NEXT DOC AI

> Inteligencia documental multi-tenant con motor de procesos documentales.
> *Tus documentos saben qué hacer después.*

Plataforma independiente de captura, extracción, validación, asociación, revisión y gobernanza documental, con un workflow microservicio para orquestar procesos documentales (COMEX, aprobaciones, terceros, partners).

No es un módulo de Follow: Follow, CIMA y Valid360.ai son **consumidores opcionales** vía conector, SSO y eventos.

> ### ¿Tomás el proyecto sin contexto?
> Empezá por **[docs/EMPEZAR_ACA.md](docs/EMPEZAR_ACA.md)** — traspaso oficial, reglas no negociables y trampas que ya costaron tiempo.

---

## Qué hay en este monorepo

```text
nextdocs-ai/
├── backend/           Spring Boot 3.4 · Java 21 — núcleo documental
├── frontend/          React 19 · TypeScript · Vite · Tailwind 4 — portal
├── infra/             Keycloak realm y configuración de identidad
├── docs/              Arquitectura, API, despliegue, TODOs, verificaciones
├── compose.yml        Infra + perfiles app/workflow/antivirus
└── README.md          Este archivo
```

El motor de procesos vive en el repositorio hermano `Follow-Hub/workflow` y se levanta desde acá a través del perfil `workflow` de `compose.yml`.

---

## Estado actual (MVP0 Comercial)

| Componente | Estado | Notas |
|---|---|---|
| **Core documental** | ✅ Cerrado y verificado | Ingesta, clasificación, extracción (Gemini/DeepSeek/SIMULADO), validación, revisión humana, excepciones, auditoría, gobernanza, exportación, retención, costo por tenant |
| **Workflow Core** | ✅ Cerrado y verificado | Definición de procesos, versiones, grafo, instancias, tareas, decisiones, temporizadores, estados V11 (`CREADA`, `ACTIVA`, `ESPERANDO`, `BLOQUEADA`, `COMPLETADA`, `CANCELADA`) |
| **P0-05 Colaboración externa** | ✅ Cerrado | Cuentas externas + Secure Action Link con TTL, scope y usos máximos |
| **P0-06 IA Supervisora v0** | ✅ Cerrado | Reglas, hallazgos, acciones/severidad, evaluación y resolución en instancias |
| **P0-07 Partner Foundation** | ✅ Cerrado | Organizaciones partner y delegación de scopes con expiración |
| **P0-08 Marketplace-ready** | ✅ Cerrado | Modelo de instalación, overlay, provenance y términos comerciales |
| **P0-09 Biblioteca COMEX** | ✅ Cerrado | Fixture con 14 plantillas base + multimodales (`EX-MAR-FCL`, `IM-AIR`, `EX-MM-ROAD-SEA`, etc.) |
| **P0-11 E2E y seguridad** | ✅ Scaffolding listo | Playwright instalado, smoke tests contra `/actuator/health` y `/api/v1/procesos`, CI `.github/workflows/e2e.yml` |
| **P0-12 Dataset real** | ✅ Suite lista | `PilotoComexTest` con 8 escenarios: happy path, missing doc, low confidence, formato inválido, cross-doc mismatch, incoterm no permitido, vigencia vencida, nueva versión |
| **P0-14 KPI operativo** | ✅ Cerrado | `/api/v1/kpi-procesos` con 13 indicadores de procesos |
| **P0-03/P0-04 Studio guiado** | ⚠️ Backend listo, UI en camino | El servicio de definición/runtime funciona; el editor visual de procesos entra con la fase UX |
| **P0-10 Follow Context/Template Recommender** | ❌ Pendiente | Contrato canónico de contexto y recomendador de plantillas |
| **P0-13 Load y operaciones** | ❌ Pendiente | Baseline de rendimiento, backup/restore, observabilidad y rotación de secretos |
| **P0-15 Release comercial** | ❌ Pendiente | Demo punta a punta, runbook y acta de salida |

Evidencias de cada gate en [`docs/verificaciones/`](docs/verificaciones/).

---

## Repositorios relacionados

| Repositorio | Rol | Rama principal |
|---|---|---|
| `gabibenitezzz003/NEXTDOCS-IA` | Core documental, portal y orquestación local | `main` |
| `Follow-Hub/workflow` | Microservicio de procesos | `main` |
| `gabibenitezzz003/docvance-ai` | Servicio auxiliar de IA documental | `next-ai` |
| `Follow-Hub/follow-backend` | Consumidor vía conector. **No es dependencia.** | `integraciones-pedidos` |
| `Follow-Hub/follow-front` | Consumidor embebido vía SSO. **No es dependencia.** | `Integraciones-front` |

---

## Stack

- **Backend:** Java 21, Spring Boot 3.4, Maven 3.9, PostgreSQL 16, Redis 7, MinIO, Flyway, Hibernate `validate`, OpenAPI/Swagger, Keycloak 26.
- **Workflow (microservicio):** Java 21, Spring Boot 3.4, Maven 3.9, PostgreSQL, Flyway, REST.
- **Frontend:** React 19, TypeScript 5.7, Vite, Tailwind CSS 4, React Router 7, TanStack Query, Axios.
- **QA:** JUnit 5, Mockito, AssertJ, Playwright, Docker Compose, GitHub Actions.
- **IA:** Gemini (`gemini-2.5-flash`), DeepSeek, proveedor `SIMULADO` para desarrollo sin clave.

---

## Arquitectura y reglas de oro

1. **Desacoplado de Follow.** El core no importa modelos de Follow. Las referencias externas se guardan como `ReferenciaExterna`.
2. **Multi-tenancy en tres capas:** token/contexto → repositorio → servicio. Nunca sólo en el controlador.
3. **Servicios devuelven modelos, no entidades.** La conversión ocurre dentro del `@Transactional`.
4. **Flyway con `ddl-auto: validate`.** Arranque limpio = migraciones y entidades coinciden.
5. **Asincronía por defecto.** La API guarda y encola; el worker llama al proveedor de IA.
6. **Proveedor de IA abstracto.** Cambiar Gemini/DeepSeek/SIMULADO no cambia el payload de negocio.
7. **Código 100% en español.** camelCase en Java, snake_case en base. Prohibido comentar el código; lo que necesite contexto va a `docs/`.
8. **Feature flags.** Funcionalidad futura detrás de flags que no llaman APIs inexistentes.

Detalle en [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md).

---

## Convenciones de código

- Paquetes: `configuracion`, `clientes`, `convertidores`, `entidades`, `enumeraciones`, `errores`, `exceptions`, `filtros`, `interfaces`, `modelos`, `repositorios`, `restControladores`, `servicios`, `utiles`.
- DTOs: `Model`, `ReqModel`, `ResModel`. Servicios: `Service`. Controladores: `RestController`. Contratos: `Int`. Convertidores: `Converter`.
- Entidades: `@Data`, `Serializable`, id `String` UUID, campos `alta` y `baja`.
- Cada enumeración expone `desde(String)` tolerante a mayúsculas/minúsculas.

---

## Cómo levantar el entorno

```bash
# 1. Variables de entorno
cp .env.example .env
# Editá .env y cargá NEXTDOCS_GEMINI_CLAVE si querés usar Gemini real.

# 2. Infra (PostgreSQL, Redis, MinIO, Keycloak)
docker compose up -d

# 3. Microservicio workflow (repo hermano ../workflow)
docker compose --profile workflow up -d --build

# 4. Núcleo documental
docker compose --profile app up -d --build

# 5. Portal
cd frontend
npm install
npm run dev
```

Sin `NEXTDOCS_GEMINI_CLAVE` el sistema arranca igual y usa el proveedor `SIMULADO`. La credencial nunca se guarda en base.

### Servicios y puertos

| Servicio | URL | Credenciales |
|---|---|---|
| Core API | `http://localhost:8090` | Swagger: `/swagger-ui.html` |
| Workflow API | `http://localhost:8091` | `X-Tenant-Id: tenant-demo` |
| Portal | `http://localhost:5175` | demo: `admin@nextdocs.ai` / `nextdocs123` |
| Keycloak | `http://localhost:8089` | `admin` / `admin` |
| MinIO console | `http://localhost:9101` | `nextdocs` / `nextdocs123` |
| PostgreSQL | `localhost:5434` | `nextdocs` / `nextdocs` |
| Redis | `localhost:6381` | - |

El tenant demo se crea al arrancar salvo que `NEXTDOCS_CREAR_TENANT_DEMO=false`.

---

## Tests

**No hay JDK ni Maven local.** Maven corre por contenedor.

### Core documental

```bash
cd backend
docker run --rm --network host \
  -v "$PWD":/app -v nextdocs-m2:/root/.m2 -w /app \
  maven:3.9-eclipse-temurin-21 mvn verify
```

### Workflow

```bash
cd ../workflow
docker run --rm --network host \
  -v "$PWD":/app -v nextdocs-m2:/root/.m2 -w /app \
  maven:3.9-eclipse-temurin-21 mvn verify
```

### Frontend

```bash
cd frontend
npm install
npm run build          # typecheck + build
npm run test:e2e     # requiere workflow y core corriendo
```

---

## Máquina de estados del documento

```
RECIBIDO → PROCESANDO → EXTRAIDO → VALIDADO → APROBADO → CERRADO
                ↓            ↓          ↓          ↑
             DIVIDIDO    OBSERVADO ─────┴───────────┘
                             ↓
                         RECHAZADO
```

- La confianza de lectura **nunca aprueba** sola. La decisión la toman las reglas.
- `NO_FIGURA` e `ILEGIBLE` son estados distintos.
- Una plantilla publicada no se modifica: se crea una versión nueva.
- Sobreescribir un hallazgo exige motivo, actor y queda auditado.

---

## Módulos principales del core

| Dominio | Entidades clave |
|---|---|
| Identidad | `Tenant`, `Usuario`, `Rol`, `CuentaServicio` |
| Plantillas documentales | `PlantillaDocumental`, `VersionPlantilla`, `CampoPlantilla`, `ReglaPlantilla` |
| Documental | `Documento`, `ArchivoDocumento`, `SegmentoDocumento`, `SeguimientoOriginalFisico` |
| Extracción | `EjecucionExtraccion`, `ValorExtraido` |
| Validación | `EjecucionValidacion`, `HallazgoValidacion` |
| Asociación | `CandidatoAsociacion` |
| Revisión | `RevisionDocumento`, `CambioCampoRevision`, `ExcepcionDocumental` |
| Gobernanza | `EventoAuditoria`, `PoliticaRetencion`, `SuscripcionWebhook`, `EntregaWebhook` |

---

## Workflow

El microservicio `workflow` vive en `../workflow` y expone:

- `POST /api/v1/procesos` — crear definición
- `GET/PUT /api/v1/procesos/{definicionId}/versiones` — versiones y grafo
- `POST /api/v1/procesos/versiones/{versionId}/publicar` — publicar versión
- `POST /api/v1/instancias` — iniciar instancia
- `GET /api/v1/tareas` — listar tareas
- `POST /api/v1/tareas/{tareaId}/completar` — completar tarea
- `POST /api/v1/colaboracion-externa/...` — cuentas externas y enlaces seguros
- `GET/POST /api/v1/supervisora/...` — reglas y hallazgos de la IA supervisora
- `GET /api/v1/kpi-procesos` — KPI operativo de procesos

Para levantar el fixture de plantillas COMEX:

```bash
docker compose --profile workflow up -d --build -e NEXTDOCS_WORKFLOW_COMEX_HABILITADO=true
```

---

## Próximos pasos

En orden de dependencia:

1. **UI/UX (`UX-01`..`UX-38`)** — editor de procesos (P0-04), bandeja de tareas, visor de hallazgos, panel de KPIs.
2. **P0-10 Follow Context/Template Recommender** — contrato canónico de contexto y recomendación de plantillas.
3. **P0-13 Load y operaciones** — baseline de rendimiento, backup/restore, observabilidad, alertas y runbooks.
4. **P0-15 Release comercial** — demo end-to-end, acta de salida y rotación de la API key de Gemini.

Plan completo en [`docs/TODO_MVP0_COMERCIAL.md`](docs/TODO_MVP0_COMERCIAL.md).

---

## Documentación

- [`docs/EMPEZAR_ACA.md`](docs/EMPEZAR_ACA.md) — traspaso oficial y reglas del proyecto.
- [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md) — bounded contexts, flujos e invariantes.
- [`docs/API.md`](docs/API.md) — endpoints, permisos, errores, webhooks.
- [`docs/DESPLIEGUE.md`](docs/DESPLIEGUE.md) — compose, imágenes, CI y recuperación.
- [`docs/TODO_MVP0_COMERCIAL.md`](docs/TODO_MVP0_COMERCIAL.md) — plan de trabajo MVP0.
- [`docs/verificaciones/`](docs/verificaciones/) — evidencias de QA por P0.
