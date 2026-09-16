import { test, expect, type Page } from "@playwright/test";
import type { DelegacionAcceso, OrganizacionPartner } from "../src/api/procesos";

function organizacionBase(): OrganizacionPartner {
  return {
    id: "org-1",
    codigo: "ACME-PARTNER",
    nombre: "Partner Acme",
    emailContacto: "hola@acme.test",
    estado: "ACTIVA",
  };
}

async function preparar(
  pagina: Page,
  organizaciones: OrganizacionPartner[] = [],
  delegaciones: DelegacionAcceso[] = [],
) {
  const control = {
    organizaciones: [...organizaciones],
    delegaciones: [...delegaciones],
    creadas: [] as unknown[],
    delegadas: [] as unknown[],
    eliminadas: [] as string[],
    revocadas: [] as string[],
    errorCrear: false,
  };
  await pagina.addInitScript(() =>
    sessionStorage.setItem("nextdocs.tokenRefresco", "controlado"),
  );
  await pagina.route("**/api/v1/**", async (ruta) => {
    const peticion = ruta.request();
    const camino = new URL(peticion.url()).pathname;
    const responder = (cuerpo: unknown, status = 200) =>
      ruta.fulfill({ status, json: cuerpo });
    if (camino.endsWith("/autenticacion/refrescar"))
      return responder({
        tokenAcceso: "controlado",
        tokenRefresco: "controlado",
        tenantId: "tenant-prueba",
        email: "persona@prueba.test",
        nombre: "Persona",
        nombreTenant: "Pruebas",
        permisos: ["tenant.administrar"],
      });
    if (camino === "/api/v1/partners/organizaciones" && peticion.method() === "GET")
      return responder(control.organizaciones);
    if (camino === "/api/v1/partners/organizaciones" && peticion.method() === "POST") {
      if (control.errorCrear)
        return responder({ mensaje: "El codigo ya existe" }, 409);
      const cuerpo = peticion.postDataJSON();
      control.creadas.push(cuerpo);
      const creada = {
        id: `org-${control.organizaciones.length + 1}`,
        estado: "ACTIVA",
        ...cuerpo,
      };
      control.organizaciones.push(creada);
      return responder(creada, 201);
    }
    if (
      camino.startsWith("/api/v1/partners/organizaciones/") &&
      peticion.method() === "DELETE"
    ) {
      const id = camino.split("/").pop() as string;
      control.eliminadas.push(id);
      control.organizaciones = control.organizaciones.map((org) =>
        org.id === id ? { ...org, estado: "INACTIVA" } : org,
      );
      return ruta.fulfill({ status: 204, body: "" });
    }
    if (camino === "/api/v1/partners/delegaciones" && peticion.method() === "GET")
      return responder(control.delegaciones);
    if (camino === "/api/v1/partners/delegaciones" && peticion.method() === "POST") {
      const cuerpo = peticion.postDataJSON();
      control.delegadas.push(cuerpo);
      const creada = { id: `del-${control.delegaciones.length + 1}`, ...cuerpo };
      control.delegaciones.push(creada);
      return responder(creada, 201);
    }
    if (
      camino.startsWith("/api/v1/partners/delegaciones/") &&
      peticion.method() === "DELETE"
    ) {
      const id = camino.split("/").pop() as string;
      control.revocadas.push(id);
      control.delegaciones = control.delegaciones.filter(
        (delegacion) => delegacion.id !== id,
      );
      return ruta.fulfill({ status: 204, body: "" });
    }
    return responder({ mensaje: "Endpoint inesperado" }, 404);
  });
  await pagina.goto("/partners");
  return control;
}

test.describe("partners", () => {
  test("lista organizaciones con estado y permite crear una", async ({ page }) => {
    const control = await preparar(page, [organizacionBase()]);
    await expect(page.getByText("Partner Acme")).toBeVisible();
    await expect(page.getByText("ACME-PARTNER")).toBeVisible();
    await page.getByRole("button", { name: "Nueva organización" }).click();
    await page.getByLabel("Código").fill("NUEVO-PARTNER");
    await page.getByLabel("Nombre").fill("Partner Nuevo");
    await page.getByRole("button", { name: "Guardar" }).click();
    await expect(page.getByText("Organización partner creada.")).toBeVisible();
    await expect(page.getByText("Partner Nuevo")).toBeVisible();
    expect(control.creadas[0]).toMatchObject({
      codigo: "NUEVO-PARTNER",
      nombre: "Partner Nuevo",
    });
  });

  test("muestra estado vacio sin organizaciones", async ({ page }) => {
    await preparar(page);
    await expect(page.getByText("Sin organizaciones partner")).toBeVisible();
  });

  test("desactiva una organizacion activa", async ({ page }) => {
    const control = await preparar(page, [organizacionBase()]);
    await page.getByRole("button", { name: "Desactivar" }).click();
    await expect(
      page.getByText("Organización partner desactivada."),
    ).toBeVisible();
    expect(control.eliminadas).toEqual(["org-1"]);
  });

  test("crea una delegacion sobre un tenant cliente", async ({ page }) => {
    const control = await preparar(page, [organizacionBase()]);
    await page
      .getByRole("button", { name: "Delegaciones" })
      .click();
    await page.getByRole("button", { name: "Nueva delegación" }).click();
    const comboPartner = page.getByRole("combobox", { name: "Partner" });
    await comboPartner.click();
    await page
      .getByRole("listbox", { name: "Partner" })
      .getByRole("option", { name: /Partner Acme/ })
      .click();
    await page.getByLabel("Tenant cliente").fill("tenant-cliente-9");
    await page.getByRole("checkbox", { name: "procesos.leer" }).check();
    await page.getByRole("button", { name: "Guardar" }).click();
    await expect(page.getByText("Delegación creada.")).toBeVisible();
    expect(control.delegadas[0]).toMatchObject({
      partnerId: "org-1",
      tenantClienteId: "tenant-cliente-9",
      scopes: ["procesos.leer"],
    });
  });

  test("revoca una delegacion existente", async ({ page }) => {
    const control = await preparar(page, [organizacionBase()], [
      {
        id: "del-1",
        partnerId: "org-1",
        tenantClienteId: "tenant-cliente-9",
        scopes: ["procesos.leer"],
      },
    ]);
    await page.getByRole("button", { name: "Delegaciones" }).click();
    await expect(page.getByText("tenant-cliente-9")).toBeVisible();
    await page.getByRole("button", { name: "Revocar" }).click();
    await expect(page.getByText("Delegación revocada.")).toBeVisible();
    expect(control.revocadas).toEqual(["del-1"]);
  });

  test("muestra el error del servidor al crear", async ({ page }) => {
    const control = await preparar(page);
    control.errorCrear = true;
    await page.getByRole("button", { name: "Nueva organización" }).click();
    await page.getByLabel("Código").fill("DUP");
    await page.getByLabel("Nombre").fill("Duplicado");
    await page.getByRole("button", { name: "Guardar" }).click();
    await expect(page.getByText("El codigo ya existe")).toBeVisible();
  });
});
