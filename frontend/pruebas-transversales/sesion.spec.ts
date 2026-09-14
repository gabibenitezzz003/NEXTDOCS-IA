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

test("AT-01 un 401 del motor de procesos tambien refresca y reintenta", async ({
  page,
}) => {
  const control = await preparar(page);
  await page.goto("/procesos");
  await expect(page.getByText("Proceso controlado")).toBeVisible();

  await page.route("**/api/v1/autenticacion/refrescar", (ruta) =>
    ruta.fulfill({
      json: { ...sesionControlada("A"), tokenAcceso: "A-renovado" },
    }),
  );
  await page.route("**/api/v1/instancias**", (ruta) =>
    ruta.request().headers().authorization === "Bearer A"
      ? ruta.fulfill({ status: 401, json: { mensaje: "Acceso expirado" } })
      : ruta.fallback(),
  );

  await page.getByRole("button", { name: "Instancias" }).click();

  await expect(page.getByText("Ocurrio un error inesperado")).toHaveCount(0);
  await expect(page.getByText(/La sesion no es valida/)).toHaveCount(0);
  await expect
    .poll(() =>
      control.peticiones
        .filter((p) => p.ruta === "/api/v1/instancias")
        .at(-1)?.tenant,
    )
    .toBe("A-renovado");
});

test("AT-01 un 401 irrecuperable del motor de procesos cierra la sesion, no deja paneles rotos", async ({
  page,
}) => {
  await preparar(page);
  await page.route("**/api/v1/autenticacion/refrescar", (ruta) =>
    ruta.fulfill({ status: 401, json: { mensaje: "Refresco vencido" } }),
  );
  await page.route("**/api/v1/procesos", (ruta) =>
    ruta.fulfill({ status: 401, body: "", headers: { "content-type": "text/plain" } }),
  );

  await page.goto("/procesos");

  await expect(page.getByLabel("Organización", { exact: true })).toBeVisible();
  await expect(page.getByText("Ocurrio un error inesperado")).toHaveCount(0);
});

test("un error del motor de procesos dice que paso en vez de un mensaje generico", async ({
  page,
}) => {
  await preparar(page);
  await page.route("**/api/v1/procesos", (ruta) =>
    ruta.fulfill({
      status: 502,
      body: "<html>bad gateway</html>",
      headers: { "content-type": "text/html" },
    }),
  );

  await page.goto("/procesos");

  const alerta = page.getByRole("alert").first();
  await expect(alerta).toContainText("Error del servidor");
  await expect(alerta).toContainText("El servicio no esta respondiendo (502)");
  await expect(page.getByText("Ocurrio un error inesperado")).toHaveCount(0);
  await expect(page.getByText(/No se pudieron cargar los/)).toHaveCount(0);
});
