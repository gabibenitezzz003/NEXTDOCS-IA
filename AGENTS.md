# AGENTS.md

Monorepo **NEXT DOC AI**: backend Spring Boot 3.4 / Java 21 (Maven) + frontend React 19 / TypeScript / Vite. Todo el código y la documentación están en **español** (regla no negociable). Antes de tocar nada, leé `docs/EMPEZAR_ACA.md` — es el traspaso oficial y lista reglas de dominio y trampas que ya costaron tiempo.

## Comandos

No hay wrapper Maven ni `mvn` instalado: los `./mvnw` del README son stale. Maven corre por contenedor.

```bash
# Infra (PostgreSQL 5434 · Redis 6381 · MinIO 9102/9101)
cp .env.example .env
docker compose up -d            # API NO arranca; se corre con Maven

# Backend (sin JDK local)
docker run --rm --network host --env-file ../.env \
  -v "$PWD":/app -v nextdocs-m2:/root/.m2 -w /app \
  maven:3.9-eclipse-temurin-21 mvn spring-boot:run

# Stack completo (imagen + infra)
docker compose --profile app up -d --build   # API en 8090

# Tests (todos): levantar la infra primero
docker compose up -d
docker run --rm --network host                               \
  -v "$PWD":/app -v nextdocs-m2:/root/.m2 -w /app            \
  maven:3.9-eclipse-temurin-21 mvn verify
```

- API: `http://localhost:8090` · Swagger: `/swagger-ui.html`
- Tenant demo de arranque: `admin@nextdocs.ai` / `nextdocs123` (se desactiva con `NEXTDOCS_CREAR_TENANT_DEMO=false`). Sin `NEXTDOCS_GEMINI_CLAVE` arranca igual con el proveedor `SIMULADO`.
- Frontend: `cd frontend && npm run dev` (puerto **5175**, proxy `/api` → `:8090`). Typecheck = `npm run build` (`tsc -b && vite build`). No hay lint; formatter = `npm run format` (prettier).

## Tests

- **Unitarios** (`*Test`): corren sin infra. De integración (`*IT`): requieren la infra del compose sobre la base `nextdocs_prueba`, que se crea sola. Si no hay infra se **saltan** (no fallan) con `Assumptions.abort` en `@BeforeAll` de `PruebaIntegracion`; la CI los fuerza con `NEXTDOCS_PRUEBA_OBLIGATORIA=true`. **Testcontainers no funciona** en este Docker (API ≥1.40 vs docker-java 1.32) — no intentar migrar.
- Un test solo, dentro del contenedor Maven: `mvn test -Dtest=ClaseTest` para unitarios, `mvn verify -Dit.test=ClaseIT` para integración.
- En la precondición de entorno, el chequeo va en un método de ciclo de vida (`@BeforeAll`), **nunca** en un bloque `static` del test — revienta en `ExceptionInInitializerError` y fallan todos.

## Convenciones (no negociables)

- Código 100% en español, camelCase; `snake_case` en la base. **Prohibido escribir comentarios** — lo que necesite explicación va a `docs/`. Estructura de paquetes heredada de `follow-backend` (`entidades`, `enumeraciones`, `repositorios`, `restControladores`, `servicios`, `convertidores`, `modelos`, `exceptions`, `utiles`, ...).
- DTOs terminan en `Model`/`ReqModel`/`ResModel`; servicios `Service`; controladores `RestController` (extienden `ControladorRest<T>`); contratos `Int`; convertidores `Converter`. Cada enum expone un `desde(String)` tolerante a mayúsculas.
- Los **servicios devuelven modelos, nunca entidades**: la conversión ocurre dentro del `@Transactional`. Un servicio que devuelve una entidad es un bug (`LazyInitializationException` en el borde HTTP).
- Los repositorios siempre filtran por tenant. Multi-tenancy en token → repositorio → servicio, no sólo en el controlador.
- Flyway corre al arrancar con `ddl-auto: validate`: mismas migraciones y entidades o el arranque no queda UP.

## Trampas de dominio (detalle en EMPEZAR_ACA.md)

- La confianza de lectura **nunca aprueba** sola; decide la regla.
- `NO_FIGURA` ≠ `ILEGIBLE` (producen hallazgos distintos).
- `OBLIGATORIO` verifica presencia, no valor: para exigir `true` usá `CATALOGO` con `{"valores":["true"]}`.
- Cierre sólo desde `APROBADO`, `RECHAZADO` o `DIVIDIDO` (el resto da 409).
- Plantilla `PUBLICADA` es inmutable: se clona a una versión nueva.
- Un permiso/constante **nuevo no se aplica a roles ya existentes**: requiere migración `INSERT ... WHERE NOT EXISTS` (ej. `V13__permiso_exportar_roles_predefinidos.sql`). Nunca editar una migración ya aplicada (rompe checksum).
- Parámetros opcionales de fecha en `@Query` nativos rompen con `Instant` nulo en PostgreSQL: usar `Specification` (el predicado se agrega sólo si hay valor) o valores por defecto.
- Legal hold y retención: borrar por omisión está prohibido; sin política de retención un documento no vence; la auditoría nunca se borra.

## Estado y docs

- Plan maestro: `docs/TODO.md` (códigos de QA del N3 como `QA1-03`/`SEC-04`; no inventar criterios de aceptación). Arquitectura: `docs/ARQUITECTURA.md`. API: `docs/API.md`. Despliegue: `docs/DESPLIEGUE.md`.
- Fase 1 completa (1–14) y Fase 2 completa (15, 16, 19, 20). Los canales de entrada (17 email, 18 WhatsApp) se construyeron y se **retiraron**: no son del MVP y el porqué está en `docs/TODO.md`. Siguiente: **21 (Workflow Definition Service)**.
- `.env` está ignorado y no se sube (verificar con `git check-ignore -v .env`). La API key de Gemini usada durante el desarrollo debe rotarse. Push por SSH (`git@github.com:gabibenitezzz003/NEXTDOCS-IA.git`).
- `workflow` y `docvance-ai` son microservicios aparte: no fusionarlos al core. Ningún servicio del core importa modelos de Follow — los adaptadores van detrás de `ConectorAsociacionInt`.