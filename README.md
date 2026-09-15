# NEXT DOC AI

> Inteligencia documental multi-tenant con motor de procesos documentales.
> *Tus documentos saben qué hacer después.*

Plataforma independiente de captura, extracción, validación, asociación, revisión y gobernanza documental, con un workflow microservicio para orquestar procesos documentales (COMEX, aprobaciones, terceros, partners).

No es un módulo de Follow: Follow, CIMA y Valid360.ai son **consumidores opcionales** vía conector, SSO y eventos.

> ### ¿Tomás el proyecto sin contexto?
> Empezá por **[docs/EMPEZAR_ACA.md](docs/EMPEZAR_ACA.md)** — traspaso oficial, reglas no negociables y trampas que ya costaron tiempo.

---

## Producción

Disponible en **https://nextdocsia.fenixgroup.tech** (AWS EC2, HTTPS con Let's Encrypt).

- Merge a `main` corre la suite de verificación y deploya automáticamente al server; el repo workflow dispara el deploy del core por `repository_dispatch`.
- nginx sirve el portal, rutea `/api/` al core y los prefijos de procesos al workflow, y redirige HTTP a HTTPS.
- Ingreso con usuario y clave por organización, o con **Google y Microsoft** (OAuth2/OIDC con `state` firmado, `nonce`, aprovisionamiento automático y vínculo por email). Configuración por tenant en `docs/LOGIN_SOCIAL_OAUTH.md`.

Swagger: `https://nextdocsia.fenixgroup.tech/swagger-ui.html` (core) y
`https://nextdocsia.fenixgroup.tech/workflow-swagger` (workflow).
Detalle en [`docs/DESPLIEGUE.md`](docs/DESPLIEGUE.md).

---

## Qué hay en este monorepo

```text
nextdocs-ai/
├── backend/           Spring Boot 3.4 · Java 21 — núcleo documental
├── frontend/          React 19 · TypeScript · Vite · Tailwind 4 — portal
├── infra/             Keycloak realm, identidad, nginx y compose de producción
├── docs/              Arquitectura, API, despliegue, TODOs, verificaciones
├── compose.yml        Infra + perfiles app/workflow/antivirus
└── README.md          Este archivo
```

El motor de procesos vive en el repositorio hermano `gabibenitezzz003/nextdocs-workflow` y se levanta desde acá a través del perfil `workflow` de `compose.yml`.

---

## Funcionalidades

- **Core documental**: ingesta, clasificación, extracción (Gemini/DeepSeek), validación por reglas versionadas, revisión humana, excepciones, auditoría completa, gobernanza, exportación, retención y costo por tenant.
- **Login social**: Google y Microsoft sobre la federación por tenant, con aprovisionamiento automático del usuario y vínculo por email a cuentas existentes.
- **Workflow runtime**: semántica real por tipo de nodo — temporizadores que avanzan solos, tareas externas con enlace de un solo uso, solicitud de documento validada, validación IA que bloquea, acciones API, notificaciones, decisiones y subprocesos.
- **Operación de procesos**: bandejas de instancias y tareas con filtros por estado, proceso y responsable; detalle con datos del proceso y timeline.
- **IA Supervisora**: motor de condiciones (umbral + operador), evaluación automática al completar tareas, hallazgos resolubles y pantalla de reglas.
- **SLA y KPIs**: SLA por plantilla heredable a tareas, editable desde el portal; KPIs de procesos visibles en la bandeja.
- **Colaboración externa**: cuentas externas y Secure Action Link con TTL, scope y usos máximos.
- **Partner Foundation**: organizaciones partner y delegación de scopes con expiración.
- **Marketplace**: modelo de instalación, overlay, provenance y términos comerciales.
- **Biblioteca COMEX**: 14 plantillas base multimodales (`EX-MAR-FCL`, `IM-AIR`, `EX-MM-ROAD-SEA`, etc.).
- **Portal**: alineado al Design System NEXT DOC AI v0.2 con tema claro/oscuro; bandejas, detalle de instancia, hallazgos, KPIs y reglas de la supervisora.

---

## Repositorios relacionados

| Repositorio | Rol | Rama principal |
|---|---|---|
| `gabibenitezzz003/NEXTDOCS-IA` | Core documental, portal y orquestación local | `main` |
| `gabibenitezzz003/nextdocs-workflow` | Microservicio de procesos (motor de ejecución) | `main` |
| `gabibenitezzz003/next-doc-ai-design-system` | Design System v0.2: catálogo, SDK y tokens | `main` |
| `gabibenitezzz003/docvance-ai` | Servicio auxiliar de IA documental | `next-ai` |
| `Follow-Hub/follow-backend` | Consumidor vía conector. **No es dependencia.** | `integraciones-pedidos` |
| `Follow-Hub/follow-front` | Consumidor embebido vía SSO. **No es dependencia.** | `Integraciones-front` |

---

## Stack

- **Backend:** Java 21, Spring Boot 3.4, Maven 3.9, PostgreSQL 16, Redis 7, MinIO, Flyway, Hibernate `validate`, OpenAPI/Swagger, Keycloak 26.
- **Workflow (microservicio):** Java 21, Spring Boot 3.4, Maven 3.9, PostgreSQL, Flyway, REST.
- **Frontend:** React 19, TypeScript 5.7, Vite, Tailwind CSS 4, React Router 7, TanStack Query, Axios.
- **QA:** JUnit 5, Mockito, AssertJ, Playwright, Docker Compose, GitHub Actions.
- **IA:** Gemini (`gemini-2.5-flash`), DeepSeek, proveedor `SIMULADO` para pruebas sin clave.

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
| Workflow API | `http://localhost:8091` | `Authorization: Bearer` del login del core |
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
npm run test:e2e       # smoke contra el stack real (requiere workflow y core corriendo)
npx playwright test --config playwright.procesos.config.ts
npx playwright test --config playwright.transversales.config.ts
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
| Identidad | `Tenant`, `Usuario`, `Rol`, `CuentaServicio`, `ProveedorIdentidad` |
| Plantillas documentales | `PlantillaDocumental`, `VersionPlantilla`, `CampoPlantilla`, `ReglaPlantilla` |
| Documental | `Documento`, `ArchivoDocumento`, `SegmentoDocumento`, `SeguimientoOriginalFisico` |
| Extracción | `EjecucionExtraccion`, `ValorExtraido` |
| Validación | `EjecucionValidacion`, `HallazgoValidacion` |
| Asociación | `CandidatoAsociacion` |
| Revisión | `RevisionDocumento`, `CambioCampoRevision`, `ExcepcionDocumental` |
| Gobernanza | `EventoAuditoria`, `PoliticaRetencion`, `SuscripcionWebhook`, `EntregaWebhook` |

---

## Workflow

El microservicio `workflow` vive en `gabibenitezzz003/nextdocs-workflow` (clone local en
`../workflow`), valida el JWT del core y expone:

- `POST/GET /api/v1/procesos` — definiciones, versiones, validación y publicación del grafo
- `POST/GET /api/v1/instancias` — instancias con filtros por estado y proceso
- `POST /api/v1/instancias/{id}/pausar|reanudar|bloquear|cancelar` — operación del ciclo de vida
- `GET /api/v1/tareas` — tareas con filtros por estado e instancia
- `POST /api/v1/tareas/{tareaId}/completar` — completar tarea con actor, decisión, datos y motivo
- `POST /api/v1/colaboracion-externa/...` — cuentas externas y enlaces seguros de un solo uso
- `GET/POST /api/v1/supervisora/...` — reglas con umbral y operador, evaluación automática, hallazgos resolubles
- `GET /api/v1/kpi-procesos` — KPI operativo de procesos (resumen y población)
- `GET/POST /api/v1/partners`, `/api/v1/marketplace` — fundaciones de partners e instalaciones

Semántica real por tipo de nodo, SLA por plantilla, supervisora con motor de condiciones y
bitácora completa por instancia. Detalle en el README del repo workflow.

Para levantar el fixture de plantillas COMEX:

```bash
NEXTDOCS_WORKFLOW_COMEX_HABILITADO=true docker compose --profile workflow up -d --build
```

---

## Documentación

- [`docs/EMPEZAR_ACA.md`](docs/EMPEZAR_ACA.md) — traspaso oficial y reglas del proyecto.
- [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md) — bounded contexts, flujos e invariantes.
- [`docs/API.md`](docs/API.md) — endpoints, permisos, errores, webhooks.
- [`docs/DESPLIEGUE.md`](docs/DESPLIEGUE.md) — compose, imágenes, CI y recuperación.
- [`docs/LOGIN_SOCIAL_OAUTH.md`](docs/LOGIN_SOCIAL_OAUTH.md) — registro de apps Google/Microsoft, configuración del proveedor por tenant y troubleshooting.
- [`docs/TODO_MVP0_COMERCIAL.md`](docs/TODO_MVP0_COMERCIAL.md) — plan de trabajo MVP0.
- [`docs/verificaciones/`](docs/verificaciones/) — evidencias de QA por P0.
