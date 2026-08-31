# Plan de implementación de QA integral con Playwright

> **Para trabajadores agénticos:** SUBHABILIDAD OBLIGATORIA: usar `superpowers:subagent-driven-development` (recomendada) o `superpowers:executing-plans` para implementar este plan tarea por tarea. Los pasos usan casillas (`- [ ]`) para seguir el avance.

**Objetivo:** Incorporar pruebas Playwright reproducibles que validen el portal contra la plataforma NEXT DOC AI completa y bloqueen regresiones en GitHub Actions.

**Arquitectura:** Playwright vivirá en el frontend y usará el navegador como cliente del sistema real. Docker Compose levantará PostgreSQL, Redis, MinIO, Keycloak y el backend; Playwright administrará Vite, preparará sesiones y datos aislados, y conservará evidencia de cada fallo.

**Tecnologías:** React 19, TypeScript 5.7, Vite 8, Playwright, Spring Boot 3.4, Java 21, PostgreSQL 16, Redis 7, MinIO, Keycloak 26, Docker Compose y GitHub Actions.

**Especificación:** `docs/superpowers/specs/2026-08-31-qa-integral-playwright-diseno.md`

## Restricciones globales

- Usar Playwright CLI; no usar `microsoft/playwright-github-action@v1`.
- Ejecutar las pruebas contra frontend, backend e infraestructura reales.
- Bloquear en `package-lock.json` la versión resuelta de Playwright.
- Mantener nombres propios, mensajes, pruebas y documentación en español.
- No incorporar comentarios dentro del código.
- No almacenar secretos ni credenciales productivas.
- Mantener aislamiento por tenant e independencia entre escenarios.
- Conservar reporte HTML, trazas, capturas y vídeos cuando una prueba falle.

---

### Tarea 1: Instalar y configurar Playwright

**Archivos:**

- Modificar: `frontend/package.json`
- Modificar: `frontend/package-lock.json`
- Crear: `frontend/playwright.config.ts`
- Crear: `frontend/pruebas-e2e/salud.spec.ts`
- Modificar: `frontend/.gitignore`

**Interfaces:**

- Consume: frontend Vite mediante `npm run dev`.
- Produce: comandos `qa:instalar`, `qa:e2e`, `qa:e2e:visible` y `qa:reporte`.

- [ ] **Paso 1: instalar Playwright y bloquear la dependencia**

```bash
cd frontend
npm install --save-dev @playwright/test
```

- [ ] **Paso 2: agregar los comandos de QA a `package.json`**

```json
"qa:instalar": "playwright install --with-deps chromium",
"qa:e2e": "playwright test",
"qa:e2e:visible": "playwright test --headed",
"qa:reporte": "playwright show-report"
```

- [ ] **Paso 3: escribir la primera prueba fallida en `pruebas-e2e/salud.spec.ts`**

```ts
import { expect, test } from "@playwright/test";

test("muestra el ingreso sin errores de ejecucion", async ({ page }) => {
  const errores: string[] = [];
  page.on("pageerror", (error) => errores.push(error.message));
  await page.goto("/ingresar");
  await expect(page.getByRole("heading", { name: "Ingresar al portal" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Ingresar" })).toBeVisible();
  expect(errores).toEqual([]);
});
```

- [ ] **Paso 4: ejecutar la prueba y comprobar el fallo por falta de configuración**

```bash
cd frontend
npx playwright test pruebas-e2e/salud.spec.ts
```

- [ ] **Paso 5: crear `playwright.config.ts`**

```ts
import { defineConfig, devices } from "@playwright/test";

const direccionPortal = process.env.NEXTDOCS_QA_PORTAL ?? "http://127.0.0.1:5175";

export default defineConfig({
  testDir: "./pruebas-e2e",
  fullyParallel: false,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [["list"], ["html", { outputFolder: "reporte-playwright", open: "never" }]],
  outputDir: "resultados-playwright",
  use: {
    baseURL: direccionPortal,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    video: "retain-on-failure",
  },
  projects: [{ name: "cromo", use: { ...devices["Desktop Chrome"] } }],
  webServer: {
    command: "npm run dev -- --host 127.0.0.1",
    url: direccionPortal,
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
    env: { NEXTDOCS_API: process.env.NEXTDOCS_QA_API ?? "http://127.0.0.1:8090" },
  },
});
```

- [ ] **Paso 6: ignorar artefactos generados**

Agregar a `frontend/.gitignore`:

```text
reporte-playwright/
resultados-playwright/
```

- [ ] **Paso 7: instalar Chromium y confirmar la prueba**

```bash
cd frontend
npx playwright install chromium
npm run qa:e2e -- pruebas-e2e/salud.spec.ts
```

Resultado esperado: una prueba aprobada.

- [ ] **Paso 8: confirmar el bloque**

```bash
git add frontend
git commit -m "Incorporar Playwright al portal"
```

---

### Tarea 2: Preparar el entorno completo y autenticar

**Archivos:**

- Crear: `compose.qa.yml`
- Crear: `infra/qa/esperar-servicio.sh`
- Crear: `frontend/pruebas-e2e/soporte/entorno.ts`
- Crear: `frontend/pruebas-e2e/soporte/sesion.ts`
- Crear: `frontend/pruebas-e2e/autenticacion.spec.ts`

**Interfaces:**

- Consume: `compose.yml` y `POST /api/v1/autenticacion/ingresar`.
- Produce: `datosQa(): DatosQa` e `ingresarComoAdministrador(page: Page): Promise<void>`.

- [ ] **Paso 1: escribir las pruebas de autenticación**

```ts
import { expect, test } from "@playwright/test";
import { datosQa } from "./soporte/entorno";

test("permite ingresar como administrador", async ({ page }) => {
  const datos = datosQa();
  await page.goto("/ingresar");
  await page.getByLabel("Organizacion").fill(datos.codigoTenant);
  await page.getByLabel("Email").fill(datos.emailAdministrador);
  await page.getByLabel("Clave").fill(datos.claveAdministrador);
  await page.getByRole("button", { name: "Ingresar" }).click();
  await expect(page).toHaveURL(/\/resumen$/);
  await expect(page.getByRole("link", { name: "Documentos" })).toBeVisible();
});

test("rechaza credenciales invalidas", async ({ page }) => {
  const datos = datosQa();
  await page.goto("/ingresar");
  await page.getByLabel("Organizacion").fill(datos.codigoTenant);
  await page.getByLabel("Email").fill(datos.emailAdministrador);
  await page.getByLabel("Clave").fill("clave-invalida");
  await page.getByRole("button", { name: "Ingresar" }).click();
  await expect(page.getByRole("alert")).toBeVisible();
  await expect(page).toHaveURL(/\/ingresar$/);
});
```

- [ ] **Paso 2: ejecutar y confirmar el fallo sin backend**

```bash
cd frontend
npm run qa:e2e -- pruebas-e2e/autenticacion.spec.ts
```

- [ ] **Paso 3: crear los datos de QA**

```ts
export interface DatosQa {
  codigoTenant: string;
  emailAdministrador: string;
  claveAdministrador: string;
}

export function datosQa(): DatosQa {
  return {
    codigoTenant: process.env.NEXTDOCS_QA_TENANT ?? "demo",
    emailAdministrador: process.env.NEXTDOCS_QA_EMAIL ?? "admin@nextdocs.ai",
    claveAdministrador: process.env.NEXTDOCS_QA_CLAVE ?? "nextdocs123",
  };
}
```

- [ ] **Paso 4: crear el ingreso reutilizable**

```ts
import type { Page } from "@playwright/test";
import { expect } from "@playwright/test";
import { datosQa } from "./entorno";

export async function ingresarComoAdministrador(page: Page): Promise<void> {
  const datos = datosQa();
  await page.goto("/ingresar");
  await page.getByLabel("Organizacion").fill(datos.codigoTenant);
  await page.getByLabel("Email").fill(datos.emailAdministrador);
  await page.getByLabel("Clave").fill(datos.claveAdministrador);
  await page.getByRole("button", { name: "Ingresar" }).click();
  await expect(page).toHaveURL(/\/resumen$/);
}
```

- [ ] **Paso 5: crear `compose.qa.yml`**

```yaml
services:
  app:
    environment:
      NEXTDOCS_CREAR_TENANT_DEMO: "true"
      NEXTDOCS_IA_PROVEEDOR: SIMULADO
      NEXTDOCS_ANTIVIRUS_MOTOR: PERMISIVO
      NEXTDOCS_LIMITE_ACTIVO: "false"
      NEXTDOCS_RETENCION_ACTIVA: "false"
      NEXTDOCS_WEBHOOK_DESPACHADOR: "false"
```

- [ ] **Paso 6: crear `infra/qa/esperar-servicio.sh`**

```bash
#!/usr/bin/env bash
set -euo pipefail

direccion="$1"
nombre="$2"
intentos="${3:-90}"

for ((intento = 1; intento <= intentos; intento++)); do
  if curl --fail --silent --show-error "$direccion" >/dev/null; then
    exit 0
  fi
  sleep 2
done

echo "$nombre no respondio en el tiempo esperado" >&2
exit 1
```

- [ ] **Paso 7: levantar y probar el sistema**

```bash
chmod +x infra/qa/esperar-servicio.sh
docker compose -f compose.yml -f compose.qa.yml --profile app up -d --build keycloak app
./infra/qa/esperar-servicio.sh http://127.0.0.1:8090/actuator/health backend
cd frontend
npm run qa:e2e -- pruebas-e2e/autenticacion.spec.ts
```

Resultado esperado: dos pruebas aprobadas contra el backend real.

- [ ] **Paso 8: confirmar el bloque**

```bash
git add compose.qa.yml infra/qa frontend/pruebas-e2e
git commit -m "Preparar el entorno integral de QA"
```

---

### Tarea 3: Cubrir los recorridos principales

**Archivos:**

- Crear: `frontend/pruebas-e2e/soporte/consola.ts`
- Crear: `frontend/pruebas-e2e/navegacion.spec.ts`
- Crear: `frontend/pruebas-e2e/documentos.spec.ts`
- Crear: `frontend/pruebas-e2e/excepciones.spec.ts`
- Crear: `frontend/pruebas-e2e/indicadores.spec.ts`

**Interfaces:**

- Consume: `ingresarComoAdministrador(page)` y rutas actuales.
- Produce: `vigilarErrores(page: Page): () => string[]` y cobertura de humo funcional.

- [ ] **Paso 1: crear el recolector de errores**

```ts
import type { Page } from "@playwright/test";

export function vigilarErrores(page: Page): () => string[] {
  const errores: string[] = [];
  page.on("pageerror", (error) => errores.push(error.message));
  page.on("console", (mensaje) => {
    if (mensaje.type() === "error") errores.push(mensaje.text());
  });
  return () => errores;
}
```

- [ ] **Paso 2: escribir navegación parametrizada**

La prueba debe ingresar una vez por caso, visitar las rutas y validar estos pares exactos:

```ts
const recorridos = [
  { ruta: "/resumen", titulo: "Resumen ejecutivo" },
  { ruta: "/documentos", titulo: "Bandeja documental" },
  { ruta: "/excepciones", titulo: "Centro de excepciones" },
  { ruta: "/panel", titulo: "Panel de control" },
];
```

Cada caso debe comprobar el encabezado visible y que el recolector de errores esté vacío.

- [ ] **Paso 3: escribir el ingreso documental real**

```ts
import { expect, test } from "@playwright/test";
import { ingresarComoAdministrador } from "./soporte/sesion";

const imagenPng = Buffer.from(
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=",
  "base64",
);

test("ingresa un documento y lo muestra en la bandeja", async ({ page }) => {
  await ingresarComoAdministrador(page);
  await page.getByRole("link", { name: "Documentos" }).click();
  await page.locator('input[type="file"]').setInputFiles({
    name: `documento-qa-${Date.now()}.png`,
    mimeType: "image/png",
    buffer: imagenPng,
  });
  await expect(page.getByRole("status")).toContainText("ingresado en estado");
  await expect(page.getByText(/documento-qa-/).first()).toBeVisible();
});
```

- [ ] **Paso 4: escribir excepciones e indicadores**

Las pruebas deben ingresar, comprobar `Centro de excepciones`, aceptar lista o estado vacío, comprobar `Panel de control`, cambiar el intervalo disponible y esperar una respuesta exitosa de `/api/v1/kpi`.

- [ ] **Paso 5: ejecutar y corregir solo defectos reproducidos**

```bash
cd frontend
npm run qa:e2e -- pruebas-e2e/navegacion.spec.ts pruebas-e2e/documentos.spec.ts pruebas-e2e/excepciones.spec.ts pruebas-e2e/indicadores.spec.ts
```

Resultado esperado: todos los escenarios aprobados. Cada defecto debe conservar una expectativa que falle antes de modificar el producto.

- [ ] **Paso 6: revisar cualquier TSX modificado con las reglas React**

Comprobar ausencia de cascadas evitables, estado derivado mediante efectos, componentes internos recreados y escuchas globales duplicadas.

- [ ] **Paso 7: confirmar el bloque**

```bash
git add frontend/pruebas-e2e frontend/src
git commit -m "Cubrir los recorridos principales del portal"
```

---

### Tarea 4: Integrar QA web en GitHub Actions

**Archivos:**

- Modificar: `.github/workflows/verificar.yml`

**Interfaces:**

- Consume: composición QA y `npm run qa:e2e`.
- Produce: trabajo `qa-web` y artefacto `evidencia-playwright`.

- [ ] **Paso 1: agregar el trabajo `qa-web`**

```yaml
  qa-web:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: "22"
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - name: Instalar dependencias del portal
        working-directory: frontend
        run: npm ci
      - name: Instalar Chromium para Playwright
        working-directory: frontend
        run: npx playwright install --with-deps chromium
      - name: Levantar la plataforma completa
        run: docker compose -f compose.yml -f compose.qa.yml --profile app up -d --build keycloak app
      - name: Esperar el backend
        run: ./infra/qa/esperar-servicio.sh http://127.0.0.1:8090/actuator/health backend
      - name: Ejecutar QA de extremo a extremo
        working-directory: frontend
        env:
          CI: "true"
        run: npm run qa:e2e
      - name: Publicar evidencia de Playwright
        if: ${{ always() }}
        uses: actions/upload-artifact@v4
        with:
          name: evidencia-playwright
          path: |
            frontend/reporte-playwright
            frontend/resultados-playwright
          if-no-files-found: ignore
          retention-days: 14
      - name: Mostrar registros ante un fallo
        if: ${{ failure() }}
        run: docker compose -f compose.yml -f compose.qa.yml logs --no-color app postgres redis minio keycloak
```

- [ ] **Paso 2: hacer depender `imagen` de ambos controles**

```yaml
needs: [pruebas, qa-web]
```

- [ ] **Paso 3: validar sintaxis y equivalencia local**

```bash
docker compose -f compose.yml -f compose.qa.yml config --quiet
cd frontend
npm ci
npm run build
npm run qa:e2e
```

- [ ] **Paso 4: confirmar el bloque**

```bash
git add .github/workflows/verificar.yml
git commit -m "Ejecutar Playwright en integracion continua"
```

---

### Tarea 5: Documentar, verificar y publicar

**Archivos:**

- Crear: `docs/QA_WEB.md`
- Modificar: `README.md`
- Modificar: `docs/TODO.md`
- Modificar: `docs/EMPEZAR_ACA.md`

**Interfaces:**

- Consume: comandos y arquitectura implementados.
- Produce: guía operativa, estado actualizado y entrega en `origin/main`.

- [ ] **Paso 1: crear `docs/QA_WEB.md`**

Documentar requisitos, instalación, levantado, ejecución, variables, evidencias, diagnóstico, matriz de recorridos y el motivo para no usar la acción obsoleta.

- [ ] **Paso 2: actualizar los documentos de entrada**

Agregar a `README.md` y `docs/EMPEZAR_ACA.md` el comando de QA y el enlace a la guía. Actualizar `docs/TODO.md` con el alcance comprobado de la tarea 15 sin declarar terminadas pantallas inexistentes.

- [ ] **Paso 3: ejecutar verificación completa fresca**

```bash
cd frontend
npm ci
npm run build
npm run qa:e2e
cd ../backend
docker run --rm --network host -v "$PWD":/app -v nextdocs-m2:/root/.m2 -w /app maven:3.9-eclipse-temurin-21 mvn verify
```

Resultado esperado: frontend compilado, Playwright aprobado y Maven Verify sin fallos ni pruebas omitidas.

- [ ] **Paso 4: revisar el repositorio**

```bash
git diff --check
git status --short
git log --oneline --decorate -8
```

- [ ] **Paso 5: confirmar la documentación y el plan**

```bash
git add docs/QA_WEB.md README.md docs/TODO.md docs/EMPEZAR_ACA.md docs/superpowers/plans/2026-08-31-qa-integral-playwright.md
git commit -m "Documentar el proceso integral de QA web"
```

- [ ] **Paso 6: sincronizar sin sobrescribir trabajo remoto**

```bash
git fetch origin
git status --short --branch
git log --oneline --left-right main...origin/main
```

Si existen commits remotos nuevos, integrarlos de forma no destructiva y repetir toda la verificación.

- [ ] **Paso 7: publicar y comprobar GitHub Actions**

```bash
git push origin main
gh run list --workflow verificar.yml --limit 1
gh run watch --exit-status
```

Resultado esperado: `main` coincide con `origin/main` y los trabajos `pruebas`, `qa-web` e `imagen` terminan correctamente. Ante un fallo, descargar la evidencia, reproducirlo localmente y publicar la corrección con su prueba.
