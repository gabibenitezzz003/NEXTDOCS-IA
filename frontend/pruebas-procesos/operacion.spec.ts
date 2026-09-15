import { test, expect, type Page } from "@playwright/test";
import type { GrafoProceso, InstanciaProceso } from "../src/api/procesos";

function grafoLineal(): GrafoProceso {
  return {
    nodos: [
      { id: "entrada", tipo: "INICIO", nombre: "Entrada" },
      { id: "a", tipo: "REVISION_HUMANA", nombre: "Paso A" },
      { id: "salida", tipo: "FIN", nombre: "Salida" },
    ],
    aristas: [
      { origen: "entrada", destino: "a" },
      { origen: "a", destino: "salida" },
    ],
  };
}

function instanciaBase(): InstanciaProceso {
  return {
    id: "instancia-1",
    definicionId: "proceso",
    codigoDefinicion: "PRUEBA",
    numeroVersion: 7,
    estado: "ESPERANDO",
    alta: "2026-09-10T10:00:00Z",
    tareas: [
      {
        id: "t1",
        instanciaId: "instancia-1",
        nodoId: "entrada",
        tipoNodo: "FORMULARIO",
        estado: "COMPLETADA",
      },
      {
        id: "t2",
        instanciaId: "instancia-1",
        nodoId: "a",
        tipoNodo: "REVISION_HUMANA",
        estado: "PENDIENTE",
        asignadoA: "maria@prueba.test",
        vencimiento: "2020-01-01T00:00:00Z",
      },
    ],
    eventos: [
      { id: "e1", accion: "INSTANCIA_INICIADA" },
      { id: "e2", nodoId: "entrada", accion: "NODO_INGRESADO" },
    ],
  };
}

async function preparar(pagina: Page) {
  const control = { instancia: instanciaBase() };
  const proceso = () => ({
    id: "proceso",
    codigo: "PRUEBA",
    familia: "Pruebas",
    nombre: "Proceso controlado",
    versiones: [
      {
        id: "publicada",
        definicionId: "proceso",
        codigoDefinicion: "PRUEBA",
        numero: 7,
        estado: "PUBLICADA",
        grafo: grafoLineal(),
      },
    ],
  });
  await pagina.addInitScript(() =>
    sessionStorage.setItem("nextdocs.tokenRefresco", "controlado"),
  );
  await pagina.route("**/api/v1/**", async (ruta) => {
    const camino = new URL(ruta.request().url()).pathname;
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
        permisos: ["documentos.leer"],
      });
    if (camino === "/api/v1/procesos") return responder([proceso()]);
    if (camino === "/api/v1/procesos/proceso") return responder(proceso());
    if (camino === "/api/v1/instancias") return responder([control.instancia]);
    if (camino === "/api/v1/instancias/instancia-1")
      return responder(control.instancia);
    if (camino === "/api/v1/kpi-procesos") return responder({ indicadores: [] });
    if (camino === "/api/v1/kpi-procesos/poblacion") return responder([]);
    if (camino.endsWith("/hallazgos")) return responder([]);
    return responder({ mensaje: "Endpoint inesperado" }, 404);
  });
  return control;
}

test("la tarjeta de instancia muestra progreso, responsable y vencimiento", async ({
  page,
}) => {
  await preparar(page);
  await page.goto("/operacion");
  const tarjeta = page.locator("li", { hasText: "PRUEBA" });
  await expect(tarjeta.getByText("1 de 2 tareas")).toBeVisible();
  await expect(tarjeta.getByText("50%")).toBeVisible();
  await expect(
    tarjeta.getByText("Responsable: maria@prueba.test"),
  ).toBeVisible();
  await expect(tarjeta.getByText(/venció/)).toBeVisible();
});

test("el detalle dibuja el grafo con el estado de cada paso", async ({
  page,
}) => {
  await preparar(page);
  await page.goto("/operacion/instancias/instancia-1");
  await expect(
    page.getByRole("img", { name: /Diagrama del recorrido/ }),
  ).toBeVisible();
  await expect(page.getByText("Completado", { exact: true })).toBeVisible();
  await expect(page.getByText("En curso", { exact: true })).toBeVisible();
  await expect(page.getByText("Sin llegar", { exact: true })).toBeVisible();
  await expect(
    page.locator('svg g[opacity="0.45"]', { hasText: "Salida" }),
  ).toHaveCount(1);
});

test("una instancia completada sin eventos pinta todo el recorrido", async ({
  page,
}) => {
  const control = await preparar(page);
  control.instancia.estado = "COMPLETADA";
  control.instancia.eventos = [];
  await page.goto("/operacion/instancias/instancia-1");
  await expect(
    page.getByRole("img", { name: /Diagrama del recorrido/ }),
  ).toBeVisible();
  await expect(page.locator('svg g[opacity="0.45"]')).toHaveCount(0);
});
