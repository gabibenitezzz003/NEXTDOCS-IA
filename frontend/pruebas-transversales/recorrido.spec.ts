import { expect, test, type Page } from "@playwright/test";
import { ingresar, navegar, preparar } from "./entorno";

const CARPETA = "capturas-recorrido";

let paso = 0;

async function registrar(pagina: Page, nombre: string) {
  paso += 1;
  await pagina.screenshot({
    path: `${CARPETA}/${String(paso).padStart(2, "0")}-${nombre}.png`,
    animations: "disabled",
  });
}

test.describe.configure({ mode: "serial" });

test("P0-02 recorrido punta a punta: entrar, subir, clasificar, corregir, resolver y medir", async ({
  page,
}) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.emulateMedia({ reducedMotion: "reduce" });
  const control = await preparar(page, false);

  const resueltas: { id: string; resolucion: string }[] = [];

  await page.route("**/api/v1/documentos", async (ruta, siguiente) => {
    if (ruta.request().method() !== "POST") return siguiente();
    control.documentos.A = "Certificado de origen.pdf";
    return ruta.fulfill({
      json: {
        ...control.detalle.documento,
        id: "documento",
        nombre: "Certificado de origen.pdf",
        estado: "RECIBIDO",
      },
    });
  });

  await page.route("**/api/v1/excepciones/*/resolver", async (ruta) => {
    const camino = new URL(ruta.request().url()).pathname;
    resueltas.push({
      id: camino.split("/").at(-2) as string,
      resolucion: ruta.request().postDataJSON().resolucion,
    });
    return ruta.fulfill({ json: { estado: "RESUELTA" } });
  });

  await page.goto("/ingresar");
  await registrar(page, "ingreso");

  await ingresar(page, "A");
  await expect(page.getByRole("heading", { name: "Mi trabajo" })).toBeVisible();
  await registrar(page, "resumen");

  await navegar(page, "Documentos");
  await expect(page.getByRole("heading", { name: "Bandeja documental" })).toBeVisible();

  await page
    .getByLabel("Seleccionar un documento para cargar")
    .setInputFiles({
      name: "certificado-de-origen.pdf",
      mimeType: "application/pdf",
      buffer: Buffer.from("%PDF-1.4 recorrido de aceptacion"),
    });
  await expect(
    page.getByRole("button", { name: /^Abrir documento Certificado de origen\.pdf/ }),
  ).toBeVisible();
  await registrar(page, "bandeja-con-documento-subido");

  await expect(page.getByRole("table")).toContainText("FACTURA");

  await page.getByRole("button", { name: /^Abrir documento/ }).click();
  const visor = page.getByRole("dialog");
  await expect(visor.getByRole("tab", { name: /^Campos/ })).toBeVisible();
  await registrar(page, "visor-campos-extraidos");

  await visor.getByLabel("Razón social").fill("Empresa corregida a mano");
  await expect(visor.getByText(/1 campo\(s\) corregido\(s\) sin enviar/)).toBeVisible();
  await registrar(page, "visor-campo-corregido");

  await visor.getByLabel("Motivo de la decisión").fill("Razon social corregida en la revision");
  await visor.getByRole("button", { name: "Aprobar" }).click();

  const revision = control.peticiones.findLast(
    (peticion) => peticion.metodo === "POST" && peticion.ruta.endsWith("/revisiones"),
  );
  expect(revision?.cuerpo).toMatchObject({
    decision: "APROBAR",
    correcciones: { razonSocial: "Empresa corregida a mano" },
  });
  await registrar(page, "visor-decision-aprobada");

  await page.getByRole("button", { name: "Cerrar" }).first().click();
  await navegar(page, "Excepciones");
  await expect(page.getByRole("heading", { name: "Centro de excepciones" })).toBeVisible();
  await registrar(page, "excepciones-abiertas");

  await page.getByRole("button", { name: "Resolver" }).first().click();
  await page.getByLabel("Cómo se resolvió").fill("Se corrigio la razon social en la revision");
  await registrar(page, "excepcion-en-resolucion");
  await page.getByRole("button", { name: "Confirmar" }).click();

  await expect.poll(() => resueltas.length).toBe(1);
  expect(resueltas[0].resolucion).toBe("Se corrigio la razon social en la revision");
  await registrar(page, "excepcion-resuelta");

  await navegar(page, "Panel de control");
  const heroe = page.getByRole("region", { name: "Indicadores principales" });
  await expect(heroe.getByText("Documentos recibidos")).toBeVisible();
  await expect(heroe).toContainText("2");
  await registrar(page, "panel-indicadores");

  expect(control.inesperadas).toEqual([]);
  expect(control.errores).toEqual([]);
  expect(control.consola).toEqual([]);
});
