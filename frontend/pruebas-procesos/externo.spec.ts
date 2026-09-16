import { test, expect, type Page } from "@playwright/test";

const ENLACE_ACTIVO = {
  estado: "ACTIVO",
  expiracion: "2026-10-01T12:00:00Z",
  actor: "proveedor@externo.test",
  nombrePaso: "Aprobación del cliente",
  tipoNodo: "TAREA_EXTERNA",
  nombreProceso: "Alta de proveedores",
  codigoProceso: "ALTA-PROV",
  vencimientoTarea: "2026-09-30T12:00:00Z",
  estadoTarea: "PENDIENTE",
};

async function prepararExterno(
  pagina: Page,
  enlace: Record<string, unknown> | null = ENLACE_ACTIVO,
  status = 200,
) {
  const control = { completadas: [] as unknown[] };
  await pagina.route("**/api/v1/**", async (ruta) => {
    const peticion = ruta.request();
    const camino = new URL(peticion.url()).pathname;
    if (camino.startsWith("/api/v1/colaboracion-externa/enlaces/")) {
      if (peticion.method() === "GET") {
        return enlace
          ? ruta.fulfill({ status, json: enlace })
          : ruta.fulfill({ status: 404, json: { mensaje: "No existe" } });
      }
      if (peticion.method() === "POST") {
        control.completadas.push(peticion.postDataJSON());
        return ruta.fulfill({
          status: 200,
          json: { id: "tarea", estado: "COMPLETADA" },
        });
      }
    }
    return ruta.fulfill({ status: 404, json: { mensaje: "Endpoint inesperado" } });
  });
  await pagina.goto("/externo/token-de-prueba");
  return control;
}

test("portal externo muestra el contexto de la tarea y permite aprobar", async ({
  page,
}) => {
  const control = await prepararExterno(page);
  await expect(
    page.getByRole("heading", { name: "Tarea externa" }),
  ).toBeVisible();
  await expect(page.getByText("Alta de proveedores")).toBeVisible();
  await expect(page.getByText("Aprobación del cliente")).toBeVisible();
  await page.getByRole("button", { name: "Aprobado", exact: true }).click();
  await page.getByRole("button", { name: "Confirmar" }).click();
  await expect(page.getByText("Respuesta enviada")).toBeVisible();
  expect(control.completadas).toEqual([
    { decision: "APROBADO", motivo: undefined, datos: undefined },
  ]);
});

test("rechazar exige motivo y lo envía", async ({ page }) => {
  const control = await prepararExterno(page);
  await page.getByRole("button", { name: "Rechazado", exact: true }).click();
  const confirmar = page.getByRole("button", { name: "Confirmar" });
  await expect(confirmar).toBeDisabled();
  await page
    .getByLabel("Motivo (obligatorio si rechaza)")
    .fill("Documentación incompleta");
  await expect(confirmar).toBeEnabled();
  await confirmar.click();
  await expect(page.getByText("Respuesta enviada")).toBeVisible();
  expect(control.completadas[0]).toMatchObject({
    decision: "RECHAZADO",
    motivo: "Documentación incompleta",
  });
});

test("enlace revocado no muestra el formulario", async ({ page }) => {
  await prepararExterno(page, { ...ENLACE_ACTIVO, estado: "REVOCADO" });
  await expect(
    page.getByText("Este enlace fue revocado por la organización."),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Aprobado", exact: true }),
  ).toHaveCount(0);
});

test("enlace inválido muestra aviso", async ({ page }) => {
  await prepararExterno(page, null);
  await expect(
    page.getByRole("heading", { name: "Enlace no disponible" }),
  ).toBeVisible();
});

test("tarea ya completada muestra la respuesta registrada", async ({ page }) => {
  await prepararExterno(page, {
    ...ENLACE_ACTIVO,
    estado: "USADO",
    estadoTarea: "COMPLETADA",
    decision: "APROBADO",
    completada: "2026-09-20T12:00:00Z",
  });
  await expect(page.getByText("Este enlace ya fue utilizado.")).toBeVisible();
  await expect(page.getByText(/Respuesta registrada/)).toBeVisible();
});

test("portal externo con documentos exige adjuntar para aprobar", async ({
  page,
}) => {
  const control = { completadas: [] as unknown[], subidas: [] as string[] };
  await page.route("**/api/v1/colaboracion-externa/enlaces/**", async (ruta) => {
    const peticion = ruta.request();
    const camino = new URL(peticion.url()).pathname;
    if (camino.endsWith("/documentos")) {
      control.subidas.push(peticion.headers()["content-type"] ?? "");
      return ruta.fulfill({
        status: 201,
        json: { id: "doc-77", nombre: "factura.pdf", estado: "RECIBIDO" },
      });
    }
    if (peticion.method() === "GET") {
      return ruta.fulfill({
        status: 200,
        json: {
          ...ENLACE_ACTIVO,
          documentosEsperados: ["FACTURA_COMERCIAL"],
        },
      });
    }
    if (peticion.method() === "POST") {
      control.completadas.push(peticion.postDataJSON());
      return ruta.fulfill({
        status: 200,
        json: { id: "tarea", estado: "COMPLETADA" },
      });
    }
    return ruta.fulfill({ status: 404, json: { mensaje: "No" } });
  });
  await page.goto("/externo/token-de-prueba");

  await expect(page.getByText("Documentos solicitados")).toBeVisible();
  await expect(page.getByText("FACTURA_COMERCIAL")).toBeVisible();

  await page.getByRole("button", { name: "Aprobado", exact: true }).click();
  const confirmar = page.getByRole("button", { name: "Confirmar" });
  await expect(confirmar).toBeDisabled();

  await page.locator('input[type="file"]').setInputFiles({
    name: "factura.pdf",
    mimeType: "application/pdf",
    buffer: Buffer.from("pdf"),
  });
  await expect(page.getByText("factura.pdf")).toBeVisible();
  await expect(confirmar).toBeEnabled();
  await confirmar.click();
  await expect(page.getByText("Respuesta enviada")).toBeVisible();
  expect(control.subidas).toHaveLength(1);
  expect(control.completadas[0]).toMatchObject({
    decision: "APROBADO",
    datos: { documentoId: "doc-77" },
  });
});
