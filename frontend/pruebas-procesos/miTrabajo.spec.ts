import { test, expect } from "@playwright/test";
import { preparar } from "../pruebas-transversales/entorno";
import type { TareaProceso } from "../src/api/procesos";

function tarea(parcial: Partial<TareaProceso>): TareaProceso {
  return {
    id: "tarea-" + (parcial.nombreNodo ?? "x"),
    instanciaId: "instancia",
    codigoDefinicion: "PRUEBA",
    nombreDefinicion: "Proceso controlado",
    nodoId: "nodo",
    tipoNodo: "REVISION_HUMANA",
    estado: "PENDIENTE",
    ...parcial,
  };
}

test("Mi trabajo lista las tareas pendientes ordenadas por vencimiento", async ({
  page,
}) => {
  const control = await preparar(page);
  control.tareas = [
    tarea({
      nombreNodo: "Revisión de factura",
      vencimiento: "2030-01-01T00:00:00Z",
    }),
    tarea({
      nombreNodo: "Validación vencida",
      estado: "VENCIDA",
      vencimiento: "2020-01-01T00:00:00Z",
    }),
    tarea({
      nombreNodo: "Aprobación final",
      asignadoA: "otra@prueba.test",
    }),
  ];
  control.observados = 3;
  control.instancias = [
    {
      id: "i1",
      definicionId: "proceso",
      codigoDefinicion: "PRUEBA",
      numeroVersion: 8,
      estado: "BLOQUEADA",
      tareas: [],
    },
  ];
  await page.goto("/resumen");

  await expect(
    page.getByRole("heading", { name: "Mi trabajo" }),
  ).toBeVisible();
  const seccion = page.getByRole("region", { name: "Acción requerida" });
  await expect(
    seccion.getByRole("heading", { name: "Tu trabajo pendiente" }),
  ).toBeVisible();
  const primerItem = seccion.locator("ul li").first();
  await expect(primerItem.getByText("Validación vencida")).toBeVisible();
  await expect(primerItem.getByText(/venció/)).toBeVisible();
  await expect(seccion.getByText("asignada a otra@prueba.test")).toBeVisible();
  await expect(seccion.getByText("sin asignar").first()).toBeVisible();

  const atencion = seccion.getByRole("heading", { name: "Atención" });
  await expect(atencion).toBeVisible();
  await expect(
    seccion.getByRole("link", { name: /Procesos bloqueados/ }),
  ).toHaveText(/1/);
  await expect(
    seccion.getByRole("link", { name: /Documentos observados/ }),
  ).toHaveText(/3/);
});

test("Mi trabajo muestra la actividad reciente y el estado al día", async ({
  page,
}) => {
  const control = await preparar(page);
  control.tareas = [
    tarea({
      nombreNodo: "Paso ya hecho",
      estado: "COMPLETADA",
      completada: "2026-09-10T12:00:00Z",
      completadaPor: "persona@prueba.test",
      decision: "APROBADO",
    }),
  ];
  await page.goto("/resumen");

  const seccion = page.getByRole("region", { name: "Acción requerida" });
  await expect(
    seccion.getByText("Estás al día — no hay tareas pendientes."),
  ).toBeVisible();
  await expect(seccion.getByText("Paso ya hecho")).toBeVisible();
  await expect(
    seccion.getByText(/persona@prueba\.test/),
  ).toBeVisible();
});
