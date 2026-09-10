# E08 · P0-11 · E2E y seguridad

## Alcance

Scaffold de E2E con Playwright en el frontend:

- `@playwright/test` agregado a `devDependencies`.
- `playwright.config.ts` con proyecto `api` apuntando a `http://localhost:8091`.
- `e2e/smoke.spec.ts` con tests de humo:
  - `GET /actuator/health` responde `UP`.
  - `GET /api/v1/procesos` requiere y acepta `X-Tenant-Id`.
- Scripts `test:e2e` y `test:e2e:ui` en `package.json`.
- Workflow de GitHub Actions `.github/workflows/e2e.yml` que levanta el stack workflow y ejecuta Playwright.

## Instalación y ejecución

Comandos usados:

```bash
cd /home/gabibenitezzz/Escritorio/NEXT\ AI/nextdocs-ai/frontend
npm install
npx playwright install chromium
npm run test:e2e
```

Resultado:

```text
Running 2 tests using 2 workers
  ✓ [api] › e2e/smoke.spec.ts:4:3 › workflow health y tenant isolation › health responde UP (49ms)
  ✓ [api] › e2e/smoke.spec.ts:11:3 › workflow health y tenant isolation › procesos requiere header de tenant (92ms)
  2 passed (473ms)
```

## CI

`.github/workflows/e2e.yml` ejecuta:

1. `docker compose --profile workflow up -d --build`.
2. Espera `actuator/health`.
3. `npm ci` en `frontend`.
4. `npx playwright install --with-deps chromium`.
5. `npm run test:e2e`.

## Resultado

- Playwright instalado y ejecutado con 2 tests pasados.
- Smoke valida health y tenant header.
- CI scaffold listo para extender con recorridos reales de Docs/Flow/tercero/partner cuando las UI screens existan.
