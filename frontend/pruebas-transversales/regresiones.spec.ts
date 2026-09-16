import { test, expect } from "@playwright/test";
import {
  preparar,
  navegar,
  salir,
  ingresar,
  esperaControlada,
} from "./entorno";

for (const tenant of ["B", "A"]) {
  test(`AT-01 logout descarta documentos antes de responder la nueva sesión ${tenant}`, async ({
    page,
  }) => {
    const control = await preparar(page);
    await page.goto("/documentos");
    await expect(
      page.getByRole("button", { name: "Abrir documento Documento A.pdf" }),
    ).toBeVisible();
    await salir(page);
    control.documentos[tenant] = "Documento de sesión nueva.pdf";
    const espera = esperaControlada();
    control.demoraDocumentos = espera.promesa;
    await ingresar(page, tenant);
    await navegar(page, "Documentos");
    await expect(page.locator("body")).not.toContainText("Documento A.pdf");
    await expect
      .poll(
        () =>
          control.peticiones.filter((p) => p.ruta === "/api/v1/documentos")
            .length,
      )
      .toBe(2);
    await expect(page.locator("body")).not.toContainText("Documento A.pdf");
    espera.liberar();
    await expect(
      page.getByRole("button", {
        name: "Abrir documento Documento de sesión nueva.pdf",
      }),
    ).toBeVisible();
    expect(control.errores).toEqual([]);
  });
}

test("AT-01 una respuesta de documentos anterior al logout no repuebla la sesión nueva", async ({
  page,
}) => {
  const control = await preparar(page);
  const anterior = esperaControlada();
  control.demoraDocumentos = anterior.promesa;
  await page.goto("/documentos");
  await expect
    .poll(
      () =>
        control.peticiones.filter((p) => p.ruta === "/api/v1/documentos")
          .length,
    )
    .toBe(1);
  await salir(page);
  control.demoraDocumentos = null;
  await ingresar(page, "B");
  await navegar(page, "Documentos");
  await expect(
    page.getByRole("button", { name: "Abrir documento Documento B.pdf" }),
  ).toBeVisible();
  anterior.liberar();
  await navegar(page, "Mi trabajo");
  await navegar(page, "Documentos");
  await expect(page.getByText("Documento A.pdf", { exact: true })).toHaveCount(
    0,
  );
  await expect(
    page.getByRole("button", { name: "Abrir documento Documento B.pdf" }),
  ).toBeVisible();
});

test("AT-02 Resumen y Excepciones no comparten páginas de tamaños distintos", async ({
  page,
}) => {
  const control = await preparar(page);
  await page.goto("/resumen");
  await expect
    .poll(() =>
      control.peticiones.some(
        (p) => p.ruta === "/api/v1/excepciones" && p.parametros.tamano === "5",
      ),
    )
    .toBe(true);
  await navegar(page, "Excepciones");
  await expect(
    page.getByRole("button", { name: "Ver documento", exact: true }),
  ).toHaveCount(25);
  await expect(page.getByText("Evidencia 24", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Siguiente", exact: true }).click();
  await expect(page.getByText("Evidencia 25", { exact: true })).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Ver documento", exact: true }),
  ).toHaveCount(5);
  expect(
    control.peticiones
      .filter((p) => p.ruta === "/api/v1/excepciones")
      .map((p) => p.parametros),
  ).toEqual([
    { estado: "ABIERTA", pagina: "0", tamano: "5" },
    { estado: "ABIERTA", pagina: "0", tamano: "25" },
    { estado: "ABIERTA", pagina: "1", tamano: "25" },
  ]);
});

for (const ancho of [1440, 390]) {
  test(`AT-03 población contiene y restaura foco a ${ancho}`, async ({
    page,
  }) => {
    await page.setViewportSize({ width: ancho, height: 650 });
    const control = await preparar(page);
    await page.goto("/panel");
    const abrir = page
      .getByRole("button", { name: "Ver población", exact: true })
      .first();
    await abrir.focus();
    await page.keyboard.press("Enter");
    const modal = page.getByRole("dialog");
    await expect(modal).toBeVisible();
    await expect
      .poll(() => modal.evaluate((e) => e.contains(document.activeElement)))
      .toBe(true);
    for (let i = 0; i < 8; i++) {
      await page.keyboard.press(i < 4 ? "Tab" : "Shift+Tab");
      expect(
        await modal.evaluate((e) => e.contains(document.activeElement)),
      ).toBe(true);
    }
    await page.keyboard.press("Escape");
    await expect(modal).toHaveCount(0);
    await expect(abrir).toBeFocused();
    expect(control.consola).toEqual([]);
  });
}

for (const accion of [
  "Aprobar",
  "Observar",
  "Rechazar",
  "Reprocesar",
  "Cerrar documento",
  "Elegir",
]) {
  test(`AT-04 ${accion} bloquea campos, motivo y otras mutaciones`, async ({
    page,
  }) => {
    const control = await preparar(page);
    const espera = esperaControlada();
    control.demoraMutacion = espera.promesa;
    control.errorMutacion = true;
    await page.goto("/documentos");
    await page.getByRole("button", { name: /^Abrir documento/ }).click();
    const campo = page.getByRole("textbox", {
      name: "Razón social",
      exact: true,
    });
    await campo.fill("Edición enviada");
    await page
      .getByLabel("Motivo de la decisión", { exact: true })
      .fill("Motivo de revisión");
    if (accion === "Elegir")
      await page.getByRole("tab", { name: /^Asociación/ }).click();
    await page.getByRole("button", { name: accion, exact: true }).click();
    await expect
      .poll(
        () =>
          control.peticiones.filter(
            (p) => p.metodo === "POST" && !p.ruta.includes("autenticacion"),
          ).length,
      )
      .toBe(1);
    await page.getByRole("tab", { name: /^Campos/ }).click();
    await expect(campo).toBeDisabled();
    await expect(campo).toHaveValue("Edición enviada");
    await expect(
      page.getByLabel("Motivo de la decisión", { exact: true }),
    ).toBeDisabled();
    for (const nombre of [
      "Aprobar",
      "Observar",
      "Rechazar",
      "Reprocesar",
      "Cerrar documento",
    ])
      await expect(
        page.getByRole("button", { name: nombre, exact: true }),
      ).toBeDisabled();
    await page.getByRole("tab", { name: /^Asociación/ }).click();
    await expect(
      page.getByRole("button", { name: "Elegir", exact: true }),
    ).toBeDisabled();
    espera.liberar();
    await expect(page.getByRole("alert")).toContainText("Acción no disponible");
    await page.getByRole("tab", { name: /^Campos/ }).click();
    await expect(campo).toBeEnabled();
    await expect(campo).toHaveValue("Edición enviada");
    const peticion = control.peticiones.find((p) =>
      p.ruta.endsWith("/revisiones"),
    );
    if (peticion)
      expect(peticion.cuerpo).toEqual({
        decision: {
          Aprobar: "APROBAR",
          Observar: "OBSERVAR",
          Rechazar: "RECHAZAR",
        }[accion],
        motivo: "Motivo de revisión",
        correcciones: { razonSocial: "Edición enviada" },
      });
    expect(control.inesperadas).toEqual([]);
  });
}

for (const alto of [650, 844]) {
  test(`AT-05 metadata larga conserva área de revisión a 390x${alto}`, async ({
    page,
  }) => {
    await page.setViewportSize({ width: 390, height: alto });
    const control = await preparar(page);
    Object.assign(control.detalle.documento, {
      nombre: "Factura de exportación marítima y documentación complementaria "
        .repeat(4)
        .slice(0, 256),
      codigoPlantilla: "PLANTILLA_FACTURA_COMERCIAL_EXPORTACION_MARITIMA",
      sujetoOrigen: "CONECTOR_LOGISTICO_ORGANIZACION_DOCUMENTAL_OPERACIONES",
      sujetoTipoObjeto: "OPERACION_EXPORTACION_MARITIMA_DOCUMENTAL_COMERCIAL",
      sujetoIdObjeto: "OPERACION-".repeat(12),
    });
    await page.goto("/documentos");
    await page.getByRole("button", { name: /^Abrir documento/ }).click();
    const campo = page.getByRole("textbox", { name: "Razón social" });
    await expect(campo).toBeVisible();
    const area = page.getByRole("dialog").locator(".overscroll-contain");
    expect((await area.boundingBox())!.height).toBeGreaterThanOrEqual(200);
    await campo.scrollIntoViewIfNeeded();
    await expect(campo).toBeInViewport();
    const datos = page.getByText("Datos del documento", { exact: true });
    await datos.click();
    await expect(
      page.getByText(control.detalle.documento.nombre!, { exact: true }).last(),
    ).toBeVisible();
    await expect(
      page.getByText(control.detalle.documento.sujetoIdObjeto!, {
        exact: true,
      }),
    ).toBeVisible();
    await page
      .getByRole("button", { name: "Cerrar documento", exact: true })
      .scrollIntoViewIfNeeded();
    await expect(
      page.getByRole("button", { name: "Cerrar documento", exact: true }),
    ).toBeInViewport();
    expect(
      await page.evaluate(() => document.documentElement.scrollWidth),
    ).toBe(390);
  });
}

test("AT-06 decidir invalida KPI ya consultados", async ({ page }) => {
  const control = await preparar(page);
  await page.goto("/panel");
  await expect
    .poll(
      () =>
        control.peticiones.filter((p) => p.ruta === "/api/v1/kpi/resumen")
          .length,
    )
    .toBe(1);
  await navegar(page, "Documentos");
  await page.getByRole("button", { name: /^Abrir documento/ }).click();
  await page.getByRole("button", { name: "Aprobar", exact: true }).click();
  await expect(
    page.getByRole("status").filter({ hasText: "Documento APROBADO" }),
  ).toBeVisible();
  await page.keyboard.press("Escape");
  await navegar(page, "Panel de control");
  await expect
    .poll(
      () =>
        control.peticiones.filter((p) => p.ruta === "/api/v1/kpi/resumen")
          .length,
    )
    .toBe(2);
  expect(control.kpi).toBe(2);
});

for (const accion of ["Publicar versión 8", "Nueva versión"]) {
  test(`AT-06 ${accion} refresca la biblioteca`, async ({ page }) => {
    const control = await preparar(page);
    if (accion === "Nueva versión")
      control.proceso.versiones[0].estado = "PUBLICADA";
    await page.goto("/workflow");
    await page.getByRole("button", { name: "Abrir proceso" }).click();
    await page.getByRole("button", { name: accion, exact: true }).click();
    await expect
      .poll(() => control.peticiones.some((p) => p.metodo === "POST"))
      .toBe(true);
    await expect(
      page.getByRole("button", { name: accion, exact: true }),
    ).toHaveCount(0);
    await page.getByRole("button", { name: "Volver", exact: true }).click();
    await expect
      .poll(
        () =>
          control.peticiones.filter((p) => p.ruta === "/api/v1/procesos")
            .length,
      )
      .toBe(2);
    if (accion === "Publicar versión 8")
      await expect(
        page.getByText("Versión 8 publicada", { exact: true }),
      ).toBeVisible();
    else
      await expect(page.getByText("borrador", { exact: true })).toBeVisible();
  });
}

test("AT-07 labels informativos conservan contraste legible sobre lienzo", async ({
  page,
}) => {
  await preparar(page);
  await page.goto("/documentos");
  const contraste = await page
    .getByRole("main")
    .locator("header p")
    .first()
    .evaluate((e) => {
      const rgb = (color: string) =>
        color
          .match(/[\d.]+/g)!
          .slice(0, 3)
          .map(Number);
      const luminancia = (c: number[]) =>
        c
          .map((v) => v / 255)
          .map((v) => (v <= 0.04045 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4))
          .reduce((s, v, i) => s + v * [0.2126, 0.7152, 0.0722][i], 0);
      const texto = luminancia(rgb(getComputedStyle(e).color));
      // El lienzo se lee del tema activo en vez de fijarlo: así el umbral sigue
      // siendo real si cambian los tokens o se evalúa en tema oscuro.
      const fondo = luminancia(
        rgb(getComputedStyle(document.body).backgroundColor),
      );
      const [claro, oscuro] = fondo >= texto ? [fondo, texto] : [texto, fondo];
      return (claro + 0.05) / (oscuro + 0.05);
    });
  expect(contraste).toBeGreaterThanOrEqual(4.5);
});

test("AT-08 login compacto y error completo en móvil bajo", async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 650 });
  const control = await preparar(page, false);
  await page.goto("/ingresar");
  const organizacion = page.getByLabel("Organización", { exact: true });
  const descripcion = page.getByText(
    "Usá las credenciales de tu organización.",
    { exact: true },
  );
  const a = (await organizacion.boundingBox())!;
  const b = (await descripcion.boundingBox())!;
  expect(a.y - b.y - b.height).toBeLessThanOrEqual(80);
  control.errorLogin = "La organización no permite este ingreso. ".repeat(8);
  await organizacion.fill("A");
  await page.getByLabel("Email").fill("persona@prueba.test");
  await page.getByLabel("Clave").fill("incorrecta");
  await page.getByLabel("Clave").press("Enter");
  await expect(page.getByRole("alert")).toContainText(
    control.errorLogin.trim(),
  );
  expect(
    await page
      .getByRole("alert")
      .evaluate((e) => e.scrollHeight <= e.clientHeight),
  ).toBe(true);
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(
    390,
  );
  await page
    .getByRole("button", { name: "Ingresar", exact: true })
    .scrollIntoViewIfNeeded();
  await expect(
    page.getByRole("button", { name: "Ingresar", exact: true }),
  ).toBeInViewport();
});
