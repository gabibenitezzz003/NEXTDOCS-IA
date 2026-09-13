import { test, expect } from "@playwright/test";
import {
  preparar,
  sesionControlada,
  esperaControlada,
  salir,
  ingresar,
  navegar,
} from "./entorno";

test("AT-01 refresh de una query anterior al logout no reemplaza credenciales nuevas", async ({
  page,
}) => {
  const control = await preparar(page);
  await page.goto("/documentos");
  await expect(
    page.getByRole("button", { name: /^Abrir documento/ }),
  ).toBeVisible();
  const espera = esperaControlada();
  let refrescando = false;
  await page.route("**/api/v1/autenticacion/refrescar", async (ruta) => {
    refrescando = true;
    await espera.promesa;
    await ruta.fulfill({ json: sesionControlada("A") });
  });
  let rechazada = false;
  await page.route("**/api/v1/documentos?**", async (ruta) => {
    if (!rechazada && ruta.request().headers().authorization === "Bearer A") {
      rechazada = true;
      return ruta.fulfill({
        status: 401,
        json: { mensaje: "Acceso expirado" },
      });
    }
    return ruta.fallback();
  });
  await page
    .getByLabel("Buscar documento", { exact: true })
    .fill("consulta pendiente");
  await page.getByLabel("Buscar documento", { exact: true }).press("Enter");
  await expect.poll(() => refrescando).toBe(true);
  await salir(page);
  await ingresar(page, "B");
  const respuesta = page.waitForResponse("**/api/v1/autenticacion/refrescar");
  espera.liberar();
  await respuesta;
  await expect
    .poll(() =>
      page.evaluate(() => sessionStorage.getItem("nextdocs.tokenRefresco")),
    )
    .toBe("B");
  await navegar(page, "Documentos");
  await expect(
    page.getByRole("button", { name: "Abrir documento Documento B.pdf" }),
  ).toBeVisible();
  await expect(page.locator("body")).not.toContainText("Documento A.pdf");
  expect(control.errores).toEqual([]);
});

test("AT-01 restauración descartada por desmontaje no revive una sesión terminada", async ({
  page,
}) => {
  await preparar(page);
  const espera = esperaControlada();
  let restauraciones = 0;
  await page.route("**/api/v1/autenticacion/refrescar", async (ruta) => {
    restauraciones++;
    if (restauraciones === 1) await espera.promesa;
    await ruta.fulfill({ json: sesionControlada("A") });
  });
  await page.goto("/documentos");
  await expect(
    page.getByRole("button", { name: /^Abrir documento/ }),
  ).toBeVisible();
  await salir(page);
  await ingresar(page, "B");
  const respuesta = page.waitForResponse("**/api/v1/autenticacion/refrescar");
  espera.liberar();
  await respuesta;
  await expect
    .poll(() =>
      page.evaluate(() => sessionStorage.getItem("nextdocs.tokenRefresco")),
    )
    .toBe("B");
  await navegar(page, "Documentos");
  await expect(
    page.getByRole("button", { name: "Abrir documento Documento B.pdf" }),
  ).toBeVisible();
});

test("AT-01 la sesión vigente conserva refresh y reintento de la query", async ({
  page,
}) => {
  const control = await preparar(page);
  await page.goto("/documentos");
  await expect(
    page.getByRole("button", { name: /^Abrir documento/ }),
  ).toBeVisible();
  control.documentos["A-renovado"] = "Documento tras refresh.pdf";
  await page.route("**/api/v1/autenticacion/refrescar", (ruta) =>
    ruta.fulfill({
      json: { ...sesionControlada("A"), tokenAcceso: "A-renovado" },
    }),
  );
  await page.route("**/api/v1/documentos?**", (ruta) =>
    ruta.request().headers().authorization === "Bearer A"
      ? ruta.fulfill({ status: 401, json: { mensaje: "Acceso expirado" } })
      : ruta.fallback(),
  );
  await page.getByLabel("Buscar documento", { exact: true }).fill("actualizar");
  await page.getByLabel("Buscar documento", { exact: true }).press("Enter");
  await expect(
    page.getByRole("button", {
      name: "Abrir documento Documento tras refresh.pdf",
    }),
  ).toBeVisible();
  expect(
    control.peticiones.filter((p) => p.ruta === "/api/v1/documentos").at(-1)
      ?.tenant,
  ).toBe("A-renovado");
  expect(control.errores).toEqual([]);
});
