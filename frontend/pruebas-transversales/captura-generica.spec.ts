import { expect, test, type Page } from "@playwright/test";
import { preparar } from "./entorno";
import type { ValorExtraido } from "../src/tipos/api";

const CAMPOS_GENERICOS: ValorExtraido[] = [
  {
    id: "v1",
    claveCampo: "tipoAparente",
    etiqueta: "Tipo aparente del documento",
    valorNormalizado: "Certificado de origen",
    presencia: "PRESENTE",
    confianza: 0.71,
  },
  {
    id: "v2",
    claveCampo: "fechaEmision",
    etiqueta: "Fecha del documento",
    valorNormalizado: "2026-03-14",
    presencia: "PRESENTE",
    confianza: 0.88,
  },
  {
    id: "v3",
    claveCampo: "numeroComprobante",
    etiqueta: "Numero o identificador",
    valorNormalizado: "CO-2026-00841",
    presencia: "PRESENTE",
    confianza: 0.64,
  },
  {
    id: "v4",
    claveCampo: "cuitEmisor",
    etiqueta: "CUIT de quien lo emite",
    valorNormalizado: "30-71234567-9",
    presencia: "PRESENTE",
    confianza: 0.55,
  },
  {
    id: "v5",
    claveCampo: "razonSocialEmisor",
    etiqueta: "Quien lo emite",
    valorNormalizado: "Camara de Comercio Exterior",
    presencia: "PRESENTE",
    confianza: 0.79,
  },
  {
    id: "v6",
    claveCampo: "importeTotal",
    etiqueta: "Importe total",
    presencia: "AUSENTE",
    confianza: 0,
  },
  {
    id: "v7",
    claveCampo: "resumen",
    etiqueta: "De que se trata",
    valorNormalizado: "Certifica el origen de la mercaderia exportada",
    presencia: "PRESENTE",
    confianza: 0.68,
  },
];

const MOTIVO =
  "El documento no corresponde a ningun tipo del catalogo, asi que se capturo con el esquema generico para que igual sea util y buscable.";

async function abrirDocumentoGenerico(pagina: Page, valores = CAMPOS_GENERICOS) {
  const control = await preparar(pagina);
  control.detalle.documento.codigoPlantilla = "GENERICO";
  control.detalle.documento.origenTipo = "GENERICO";
  control.detalle.documento.confianzaTipo = 0.31;
  control.detalle.documento.motivoTipo = MOTIVO;
  control.detalle.extraccion!.valores = valores;
  await pagina.goto("/documentos");
  await pagina.getByRole("button", { name: /^Abrir documento/ }).click();
  return control;
}

test("P0-02 el visor muestra los campos de un documento capturado con el esquema generico", async ({
  page,
}) => {
  await abrirDocumentoGenerico(page);
  const visor = page.getByRole("dialog");

  await expect(visor.getByRole("tab", { name: /^Campos \(7\)/ })).toBeVisible();

  for (const campo of CAMPOS_GENERICOS) {
    await expect(visor.getByText(campo.etiqueta!, { exact: true })).toBeVisible();
    await expect(visor.getByText(campo.claveCampo, { exact: true })).toBeVisible();
  }

  await expect(visor.getByLabel("Tipo aparente del documento")).toHaveValue(
    "Certificado de origen",
  );
  await expect(visor.getByLabel("Numero o identificador")).toHaveValue("CO-2026-00841");
  await expect(visor.getByLabel("CUIT de quien lo emite")).toHaveValue("30-71234567-9");
  await expect(visor.getByLabel("De que se trata")).toHaveValue(
    "Certifica el origen de la mercaderia exportada",
  );
  await expect(visor.getByLabel("Importe total")).toHaveValue("");
});

test("P0-02 el visor avisa que el documento entro por captura generica y explica por que", async ({
  page,
}) => {
  await abrirDocumentoGenerico(page);
  const visor = page.getByRole("dialog");

  await expect(visor.getByText("Captura genérica", { exact: true })).toBeVisible();
  await expect(visor.getByRole("note")).toContainText(
    "no correspondía a ningún tipo del catálogo",
  );
  await expect(visor.getByRole("note")).toContainText(MOTIVO);
});

test("P0-02 la bandeja marca los documentos que entraron por captura generica", async ({
  page,
}) => {
  await abrirDocumentoGenerico(page);
  await page.getByRole("button", { name: "Cerrar" }).first().click();

  await expect(
    page.getByRole("table").getByText("Captura genérica", { exact: true }),
  ).toBeVisible();
});

test("P0-02 un campo capturado con el esquema generico se puede corregir y viaja con la decision", async ({
  page,
}) => {
  const control = await abrirDocumentoGenerico(page);
  const visor = page.getByRole("dialog");

  await visor.getByLabel("Numero o identificador").fill("CO-2026-00842");
  await expect(visor.getByText(/1 campo\(s\) corregido\(s\) sin enviar/)).toBeVisible();

  await visor.getByLabel("Motivo de la decisión").fill("Numero corregido a mano");
  await visor.getByRole("button", { name: "Aprobar" }).click();

  await expect
    .poll(() =>
      control.peticiones.filter(
        (peticion) =>
          peticion.metodo === "POST" && peticion.ruta.endsWith("/revisiones"),
      ).length,
    )
    .toBe(1);

  const enviada = control.peticiones.findLast(
    (peticion) => peticion.metodo === "POST" && peticion.ruta.endsWith("/revisiones"),
  );
  expect(enviada?.cuerpo).toMatchObject({
    decision: "APROBAR",
    correcciones: { numeroComprobante: "CO-2026-00842" },
  });
});

test("P0-02 un documento generico sin campos lo dice y no aparenta estar roto", async ({
  page,
}) => {
  await abrirDocumentoGenerico(page, []);
  const visor = page.getByRole("dialog");

  await expect(visor.getByText("Sin campos extraídos", { exact: true })).toBeVisible();
  await expect(visor.getByRole("note")).toContainText(
    "no correspondía a ningún tipo del catálogo",
  );
});
