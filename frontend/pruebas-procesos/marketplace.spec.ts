import { test, expect, type Page } from "@playwright/test";
import type {
  InstalacionMarketplace,
  PublicacionMarketplace,
} from "../src/api/procesos";
import { elegirEnDesplegable } from "../pruebas-transversales/desplegable";

function publicacionBase(): PublicacionMarketplace {
  return {
    id: "pub-1",
    definicionId: "def-origen",
    tenantId: "publisher-1",
    categoria: "comex",
    comercial: {
      nombreProceso: "Exportación marítima",
      codigoProceso: "COMEX-01",
      descripcionProceso: "Flujo completo de exportación FCL",
      versionNumero: 2,
      nodosTotal: 6,
      tiposNodo: ["INICIO", "FORMULARIO", "REVISION_HUMANA", "FIN"],
      precio: "USD 50",
      publicadorNombre: "Logística Sur",
    },
  };
}

interface ProcesoConVersiones {
  id: string;
  codigo: string;
  familia: string;
  nombre: string;
  versiones: { id: string; numero: number; estado: string }[];
}

function procesoPropio(): ProcesoConVersiones {
  return {
    id: "def-propia",
    codigo: "PROPIO",
    familia: "General",
    nombre: "Proceso propio",
    versiones: [{ id: "ver-propia-1", numero: 1, estado: "PUBLICADA" }],
  };
}

async function preparar(
  pagina: Page,
  publicaciones: PublicacionMarketplace[] = [],
  instalaciones: InstalacionMarketplace[] = [],
  propias: PublicacionMarketplace[] = [],
  procesos: ProcesoConVersiones[] = [],
) {
  const control = {
    publicaciones: [...publicaciones],
    instalaciones: [...instalaciones],
    propias: [...propias],
    instaladas: [] as unknown[],
    publicadas: [] as unknown[],
    retiradas: [] as string[],
    errorInstalar: false,
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
    if (camino === "/api/v1/procesos") return responder(procesos);
    if (
      camino === "/api/v1/marketplace/publicaciones" &&
      peticion.method() === "GET"
    )
      return responder(control.publicaciones);
    if (
      camino === "/api/v1/marketplace/publicaciones/mias" &&
      peticion.method() === "GET"
    )
      return responder(control.propias);
    if (
      camino === "/api/v1/marketplace/publicaciones" &&
      peticion.method() === "POST"
    ) {
      const cuerpo = peticion.postDataJSON();
      control.publicadas.push(cuerpo);
      const creada = { id: "pub-nueva", tenantId: "tenant-prueba", ...cuerpo };
      control.propias.push(creada);
      return responder(creada, 201);
    }
    if (
      camino.startsWith("/api/v1/marketplace/publicaciones/") &&
      peticion.method() === "DELETE"
    ) {
      const id = camino.split("/").pop() as string;
      control.retiradas.push(id);
      control.propias = control.propias.filter((pub) => pub.id !== id);
      return ruta.fulfill({ status: 204, body: "" });
    }
    if (
      camino === "/api/v1/marketplace/instalaciones" &&
      peticion.method() === "GET"
    )
      return responder(control.instalaciones);
    if (
      camino === "/api/v1/marketplace/instalaciones" &&
      peticion.method() === "POST"
    ) {
      if (control.errorInstalar)
        return responder({ mensaje: "Ya existe una instalacion" }, 409);
      const cuerpo = peticion.postDataJSON();
      control.instaladas.push(cuerpo);
      const creada = {
        id: `inst-${control.instalaciones.length + 1}`,
        definicionId: "def-origen",
        versionId: "ver-1",
        publicadorTenantId: "publisher-1",
        definicionLocalId: "def-local-1",
        ...cuerpo,
      };
      control.instalaciones.push(creada);
      return responder(creada, 201);
    }
    return responder({ mensaje: "Endpoint inesperado" }, 404);
  });
  await pagina.goto("/marketplace");
  return control;
}

test.describe("marketplace", () => {
  test("muestra el catalogo con datos comerciales enriquecidos", async ({
    page,
  }) => {
    await preparar(page, [publicacionBase()]);
    await expect(page.getByText("Exportación marítima")).toBeVisible();
    await expect(page.getByText("Logística Sur")).toBeVisible();
    await expect(page.getByText("USD 50")).toBeVisible();
    await expect(page.getByText("comex")).toBeVisible();
  });

  test("catalogo vacio muestra el estado vacio", async ({ page }) => {
    await preparar(page);
    await expect(page.getByText("El catálogo está vacío")).toBeVisible();
  });

  test("instala una plantilla con politica de actualizacion", async ({
    page,
  }) => {
    const control = await preparar(page, [publicacionBase()]);
    await page.getByRole("button", { name: "Instalar" }).click();
    await elegirEnDesplegable(
      page,
      "Política de actualización",
      "Actualizaciones mayores",
    );
    await page.getByRole("checkbox", { name: /Fijar en la versión/ }).check();
    await page
      .getByRole("button", { name: "Confirmar instalación" })
      .click();
    await expect(
      page.getByText("Plantilla instalada como copia propia en el workflow."),
    ).toBeVisible();
    expect(control.instaladas[0]).toMatchObject({
      publicacionId: "pub-1",
      pin: true,
      politicaActualizacion: "MAYOR",
    });
  });

  test("una publicacion ya instalada muestra la insignia y no instala", async ({
    page,
  }) => {
    await preparar(page, [publicacionBase()], [
      {
        id: "inst-1",
        definicionId: "def-origen",
        versionId: "ver-1",
        publicadorTenantId: "publisher-1",
        definicionLocalId: "def-local-1",
      },
    ]);
    await expect(page.getByText("Instalada")).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Instalar" }),
    ).toHaveCount(0);
  });

  test("muestra el error del servidor al instalar duplicada", async ({
    page,
  }) => {
    const control = await preparar(page, [publicacionBase()]);
    control.errorInstalar = true;
    await page.getByRole("button", { name: "Instalar" }).click();
    await page
      .getByRole("button", { name: "Confirmar instalación" })
      .click();
    await expect(page.getByText("Ya existe una instalacion")).toBeVisible();
  });

  test("lista instalaciones con enlace a la copia local", async ({ page }) => {
    await preparar(page, [], [
      {
        id: "inst-1",
        definicionId: "def-origen",
        versionId: "ver-1",
        publicadorTenantId: "publisher-1",
        definicionLocalId: "def-local-1",
        pin: true,
        politicaActualizacion: "MAYOR",
      },
    ]);
    await page.getByRole("button", { name: "Instalaciones" }).click();
    await expect(page.getByText("publisher-1")).toBeVisible();
    await expect(page.getByText("Fijada")).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Abrir en el workflow" }),
    ).toBeVisible();
  });

  test("publica una version propia en el marketplace", async ({ page }) => {
    const control = await preparar(page, [], [], [], [procesoPropio()]);
    await page.getByRole("button", { name: "Mis publicaciones" }).click();
    await page.getByRole("button", { name: "Publicar plantilla" }).click();
    await elegirEnDesplegable(
      page,
      "Versión publicada",
      "Proceso propio · v1",
    );
    await page.getByLabel("Categoría").fill("aduanas");
    await page.getByLabel("Precio").fill("USD 120");
    await page
      .getByRole("button", { name: "Publicar", exact: true })
      .click();
    await expect(
      page.getByText("Versión publicada en el marketplace."),
    ).toBeVisible();
    expect(control.publicadas[0]).toMatchObject({
      versionId: "ver-propia-1",
      categoria: "aduanas",
    });
    expect((control.publicadas[0] as { comercial?: { precio?: string } }).comercial?.precio).toBe("USD 120");
  });

  test("retira una publicacion propia", async ({ page }) => {
    const control = await preparar(page, [], [], [
      {
        id: "pub-propia",
        definicionId: "def-propia",
        tenantId: "tenant-prueba",
        categoria: "comex",
        comercial: { nombreProceso: "Proceso propio", versionNumero: 1 },
      },
    ]);
    await page.getByRole("button", { name: "Mis publicaciones" }).click();
    await expect(page.getByText("Proceso propio")).toBeVisible();
    await page.getByRole("button", { name: "Retirar" }).click();
    await expect(
      page.getByText("Publicación retirada del catálogo."),
    ).toBeVisible();
    expect(control.retiradas).toEqual(["pub-propia"]);
  });
});
