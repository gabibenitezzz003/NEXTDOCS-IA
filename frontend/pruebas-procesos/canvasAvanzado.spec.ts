import { test, expect, type Page } from "@playwright/test";
import type { GrafoProceso } from "../src/api/procesos";

function grafoConPosiciones(): GrafoProceso {
  return {
    nodos: [
      {
        id: "inicio",
        tipo: "INICIO",
        nombre: "Inicio",
        configuracion: { posicion: { x: 80, y: 120 } },
      },
      {
        id: "a",
        tipo: "FORMULARIO",
        nombre: "Paso A",
        configuracion: { posicion: { x: 100, y: 40 } },
      },
      {
        id: "b",
        tipo: "REVISION_HUMANA",
        nombre: "Paso B",
        configuracion: { posicion: { x: 300, y: 200 } },
      },
      {
        id: "c",
        tipo: "NOTIFICACION",
        nombre: "Paso C",
        configuracion: { posicion: { x: 500, y: 320 } },
      },
      {
        id: "fin",
        tipo: "FIN",
        nombre: "Fin",
        configuracion: { posicion: { x: 700, y: 120 } },
      },
    ],
    aristas: [
      { origen: "inicio", destino: "a" },
      { origen: "a", destino: "b" },
      { origen: "b", destino: "c" },
      { origen: "c", destino: "fin" },
    ],
  };
}

async function preparar(pagina: Page) {
  const control = {
    guardados: [] as GrafoProceso[],
    actual: null as GrafoProceso | null,
  };
  await pagina.addInitScript(() =>
    sessionStorage.setItem("nextdocs.tokenRefresco", "controlado"),
  );
  await pagina.route("**/api/v1/**", async (ruta) => {
    const peticion = ruta.request();
    const camino = new URL(peticion.url()).pathname;
    const responder = (cuerpo: unknown, status = 200) =>
      ruta.fulfill({ status, json: cuerpo });
    const version = () => ({
      id: "borrador",
      definicionId: "proceso",
      codigoDefinicion: "PRUEBA",
      numero: 8,
      estado: "BORRADOR",
      grafo: control.actual ?? grafoConPosiciones(),
    });
    const proceso = () => ({
      id: "proceso",
      codigo: "PRUEBA",
      familia: "Pruebas",
      nombre: "Proceso controlado",
      versiones: [version()],
    });
    if (camino.endsWith("/autenticacion/refrescar"))
      return responder({
        tokenAcceso: "controlado",
        tokenRefresco: "controlado",
        tenantId: "tenant-prueba",
        email: "persona@prueba.test",
        nombre: "Persona",
        nombreTenant: "Pruebas",
        permisos: ["plantillas.publicar"],
      });
    if (camino === "/api/v1/procesos") return responder([proceso()]);
    if (camino === "/api/v1/procesos/proceso") return responder(proceso());
    if (camino.endsWith("/grafo")) {
      control.actual = peticion.postDataJSON();
      control.guardados.push(control.actual);
      return responder(version());
    }
    return responder({ mensaje: "Endpoint inesperado" }, 404);
  });
  await pagina.goto("/workflow");
  await pagina.getByRole("button", { name: "Abrir proceso" }).click();
  await expect(
    pagina.getByRole("application", { name: /Canvas del recorrido/ }),
  ).toBeVisible();
  return control;
}

function posicionDe(grafo: GrafoProceso, id: string): { x: number; y: number } {
  const nodo = grafo.nodos.find((actual) => actual.id === id)!;
  return (nodo.configuracion as { posicion: { x: number; y: number } })
    .posicion;
}

test("shift+click marca varios pasos y alinear los pone a la misma x", async ({
  page,
}) => {
  const control = await preparar(page);
  const canvas = page.getByRole("application", {
    name: /Canvas del recorrido/,
  });

  await canvas.getByText("Paso A", { exact: true }).click();
  await canvas
    .getByText("Paso B", { exact: true })
    .click({ modifiers: ["Shift"] });

  await expect(page.getByText("2 pasos marcados")).toBeVisible();

  await page.getByRole("button", { name: "Alinear a la izquierda" }).click();
  await page.getByRole("button", { name: "Guardar borrador" }).click();
  await expect.poll(() => control.guardados.length).toBe(1);

  const a = posicionDe(control.guardados[0], "a");
  const b = posicionDe(control.guardados[0], "b");
  expect(a.x).toBe(b.x);
  expect(a.x).toBe(100);
  expect(b.y).toBe(200);
});

test("distribuir horizontal deja los pasos a distancias iguales", async ({
  page,
}) => {
  const control = await preparar(page);
  const canvas = page.getByRole("application", {
    name: /Canvas del recorrido/,
  });

  await canvas.getByText("Paso A", { exact: true }).click();
  await canvas
    .getByText("Paso B", { exact: true })
    .click({ modifiers: ["Shift"] });
  await canvas
    .getByText("Paso C", { exact: true })
    .click({ modifiers: ["Shift"] });

  await expect(page.getByText("3 pasos marcados")).toBeVisible();
  await page.getByRole("button", { name: "Distribuir en horizontal" }).click();
  await page.getByRole("button", { name: "Guardar borrador" }).click();
  await expect.poll(() => control.guardados.length).toBe(1);

  const a = posicionDe(control.guardados[0], "a");
  const b = posicionDe(control.guardados[0], "b");
  const c = posicionDe(control.guardados[0], "c");
  expect(a.x).toBe(100);
  expect(c.x).toBe(500);
  expect(b.x).toBeCloseTo(300, 0);
});

test("las flechas mueven la selección y Ctrl+D la duplica", async ({
  page,
}) => {
  const control = await preparar(page);
  const canvas = page.getByRole("application", {
    name: /Canvas del recorrido/,
  });

  await canvas.getByText("Paso A", { exact: true }).click();
  await page.keyboard.press("ArrowRight");
  await page.keyboard.press("ArrowDown");
  await page.getByRole("button", { name: "Guardar borrador" }).click();
  await expect.poll(() => control.guardados.length).toBe(1);
  expect(posicionDe(control.guardados[0], "a").x).toBe(124);
  expect(posicionDe(control.guardados[0], "a").y).toBe(64);

  await page.keyboard.press("ControlOrMeta+d");
  await expect(
    canvas.getByText("Paso A (copia)", { exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Guardar borrador" }).click();
  await expect.poll(() => control.guardados.length).toBe(2);
  expect(control.guardados[1].nodos).toHaveLength(6);
  expect(
    control.guardados[1].nodos.filter((nodo) => nodo.tipo === "FORMULARIO"),
  ).toHaveLength(2);
});

test("Ctrl+A marca todo y Supr borra los pasos internos", async ({ page }) => {
  const control = await preparar(page);
  page.on("dialog", (dialogo) => dialogo.accept());
  await page
    .getByRole("application", { name: /Canvas del recorrido/ })
    .click({ position: { x: 10, y: 10 } });
  await page.keyboard.press("ControlOrMeta+a");
  await expect(page.getByText("5 pasos marcados")).toBeVisible();

  await page.keyboard.press("Delete");
  await page.getByRole("button", { name: "Guardar borrador" }).click();
  await expect.poll(() => control.guardados.length).toBe(1);
  const tipos = control.guardados[0].nodos.map((nodo) => nodo.tipo);
  expect(tipos).toEqual(["INICIO", "FIN"]);
});
