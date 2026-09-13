import { test, expect, type Page } from "@playwright/test";
import type { GrafoProceso, InstanciaProceso } from "../src/api/procesos";

function grafoLineal(): GrafoProceso {
  return {
    nodos: [
      {
        id: "entrada",
        tipo: "INICIO",
        nombre: "Entrada",
        configuracion: { origen: "manual" },
      },
      {
        id: "a",
        tipo: "FORMULARIO",
        nombre: "Paso A",
        configuracion: { adicional: { valores: [1, 2] } },
      },
      {
        id: "salida",
        tipo: "FIN",
        nombre: "Salida",
        configuracion: { conservar: true },
      },
    ],
    aristas: [
      { origen: "entrada", destino: "a" },
      { origen: "a", destino: "salida" },
    ],
  };
}

async function preparar(pagina: Page, grafo = grafoLineal()) {
  const control = {
    grafo,
    guardados: [] as GrafoProceso[],
    publicaciones: 0,
    errorGuardar: false,
    errorInstancia: false,
    errorCompletar: false,
    consultasInstancia: 0,
    completadas: [] as unknown[],
    instancia: {
      id: "instancia-prueba",
      definicionId: "proceso",
      codigoDefinicion: "PRUEBA",
      numeroVersion: 7,
      estado: "ESPERANDO",
      tareas: [
        {
          id: "tarea",
          instanciaId: "instancia-prueba",
          nodoId: "a",
          tipoNodo: "REVISION_HUMANA",
          estado: "VENCIDA",
        },
      ],
    } as InstanciaProceso,
  };
  const version = () => ({
    id: "borrador",
    definicionId: "proceso",
    codigoDefinicion: "PRUEBA",
    numero: 8,
    estado: "BORRADOR",
    grafo: control.grafo,
  });
  const proceso = () => ({
    id: "proceso",
    codigo: "PRUEBA",
    familia: "Pruebas",
    nombre: "Proceso controlado",
    versiones: [version(), { ...version(), id: "publicada", numero: 7, estado: "PUBLICADA" }],
  });
  await pagina.addInitScript(() => sessionStorage.setItem("nextdocs.tokenRefresco", "controlado"));
  await pagina.route("**/api/v1/**", async (ruta) => {
    const peticion = ruta.request();
    const camino = new URL(peticion.url()).pathname;
    const responder = (cuerpo: unknown, status = 200) => ruta.fulfill({ status, json: cuerpo });
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
      expect(peticion.headers()["x-tenant-id"]).toBe("tenant-prueba");
      if (control.errorGuardar) return responder({ mensaje: "No se pudo guardar" }, 409);
      control.grafo = peticion.postDataJSON();
      control.guardados.push(control.grafo);
      return responder(version());
    }
    if (camino.endsWith("/publicar")) {
      control.publicaciones++;
      return responder({ ...version(), estado: "PUBLICADA" });
    }
    if (camino === "/api/v1/instancias") return responder(control.instancia);
    if (camino === "/api/v1/instancias/instancia-prueba") {
      control.consultasInstancia++;
      return control.errorInstancia
        ? responder({ mensaje: "Detalle de prueba no disponible" }, 503)
        : responder(control.instancia);
    }
    if (camino.endsWith("/completar")) {
      control.completadas.push(peticion.postDataJSON());
      return control.errorCompletar
        ? responder({ mensaje: "La tarea fue modificada" }, 409)
        : responder(control.instancia.tareas[0]);
    }
    return responder({ mensaje: "Endpoint inesperado" }, 404);
  });
  await pagina.goto("/procesos");
  return control;
}

test("guardar conserva extremos y configuración adicional; publicar exige guardar", async ({
  page,
}) => {
  const control = await preparar(page);
  await expect(page.getByText("v7 publicada", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Abrir estudio" }).click();
  await page.getByRole("button", { name: "Configurar", exact: true }).click();
  await page.getByLabel("Nombre del paso").fill("Paso modificado");
  await expect(page.getByRole("button", { name: "Publicar v8" })).toBeDisabled();
  control.errorGuardar = true;
  await page.getByRole("button", { name: "Guardar borrador" }).click();
  await expect(page.getByText("No se pudo guardar", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "Publicar v8" })).toBeDisabled();
  control.errorGuardar = false;
  await page.getByRole("button", { name: "Guardar borrador" }).click();
  await expect(page.getByRole("button", { name: "Publicar v8" })).toBeEnabled();
  const esperado = grafoLineal();
  esperado.nodos[1].nombre = "Paso modificado";
  expect(control.guardados).toEqual([esperado]);
  await page.getByRole("button", { name: "Publicar v8" }).click();
  await expect.poll(() => control.publicaciones).toBe(1);
});

for (const caso of ["ramas", "condición"]) {
  test(`no permite sobrescribir un grafo con ${caso}`, async ({ page }) => {
    const grafo = grafoLineal();
    if (caso === "ramas") {
      grafo.nodos[1].tipo = "DECISION";
      grafo.nodos.push({ id: "otra-salida", tipo: "FIN" });
      grafo.aristas.push({
        origen: "a",
        destino: "otra-salida",
        condicion: "decision=RECHAZADO",
      });
    } else grafo.aristas[0].condicion = "decision=APROBADO";
    const control = await preparar(page, grafo);
    await page.getByRole("button", { name: "Abrir estudio" }).click();
    await expect(page.getByText(/no puede representar fielmente/i)).toBeVisible();
    await expect(page.getByRole("button", { name: "Guardar borrador" })).toBeDisabled();
    await expect(page.getByRole("button", { name: "Publicar v8" })).toBeDisabled();
    await expect(page.getByLabel("Agregar paso")).toBeDisabled();
    expect(control.guardados).toEqual([]);
    expect(control.publicaciones).toBe(0);
  });
}

test("protege la salida con cambios locales", async ({ page }) => {
  await preparar(page);
  await page.getByRole("button", { name: "Abrir estudio" }).click();
  await page.getByRole("button", { name: "Configurar", exact: true }).click();
  await page.getByLabel("Nombre del paso").fill("Sin guardar");
  page.once("dialog", (dialogo) => dialogo.dismiss());
  await page.getByRole("button", { name: "Volver", exact: true }).click();
  await expect(page.getByLabel("Nombre del paso")).toHaveValue("Sin guardar");
  page.once("dialog", (dialogo) => dialogo.accept());
  await page.getByRole("button", { name: "Volver", exact: true }).click();
  await expect(page.getByRole("button", { name: "Abrir estudio" })).toBeVisible();
});

test("ESPERANDO permite VENCIDA y rechaza con motivo real", async ({ page }) => {
  const control = await preparar(page);
  await page.getByRole("button", { name: "Abrir estudio" }).click();
  await page.getByRole("button", { name: /Probar/ }).click();
  await expect(page.getByText("VENCIDA", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Rechazar", exact: true }).click();
  await expect(page.getByRole("button", { name: "Confirmar rechazo" })).toBeDisabled();
  await page.getByLabel("Motivo del rechazo").fill("   ");
  await expect(page.getByRole("button", { name: "Confirmar rechazo" })).toBeDisabled();
  await page.getByLabel("Motivo del rechazo").fill(" Evidencia insuficiente ");
  await page.getByRole("button", { name: "Confirmar rechazo" }).click();
  await expect
    .poll(() => control.completadas)
    .toEqual([
      {
        actor: "persona@prueba.test",
        decision: "RECHAZADO",
        motivo: "Evidencia insuficiente",
      },
    ]);
});

test("consulta fallida muestra error y permite reintentar", async ({ page }) => {
  const control = await preparar(page);
  control.errorInstancia = true;
  await page.getByRole("button", { name: "Abrir estudio" }).click();
  await page.getByRole("button", { name: /Probar/ }).click();
  await expect(page.getByRole("alert")).toContainText("Detalle de prueba no disponible");
  control.errorInstancia = false;
  await page.getByRole("button", { name: "Reintentar" }).click();
  await expect(page.getByRole("button", { name: "Rechazar", exact: true })).toBeVisible();
});

for (const estado of [
  "CREADA",
  "ACTIVA",
  "ESPERANDO",
  "BLOQUEADA",
  "COMPLETADA",
  "CANCELADA",
] as const) {
  test(`representa ${estado} sin confundir curso con finalización`, async ({ page }) => {
    const control = await preparar(page);
    control.instancia.estado = estado;
    control.instancia.tareas[0].estado = "PENDIENTE";
    await page.getByRole("button", { name: "Abrir estudio" }).click();
    await page.getByRole("button", { name: /Probar/ }).click();
    await expect(page.getByText(`Estado ${estado} · v7`, { exact: true })).toBeVisible();
    const finalizada = estado === "COMPLETADA" || estado === "CANCELADA";
    await expect(page.getByText(/La instancia terminó/)).toHaveCount(finalizada ? 1 : 0);
    await expect(page.getByRole("button", { name: "Aprobar", exact: true })).toHaveCount(
      estado === "ACTIVA" || estado === "ESPERANDO" ? 1 : 0,
    );
  });
}

test("actualiza por polling y se detiene al finalizar", async ({ page }) => {
  await page.clock.install();
  const control = await preparar(page);
  await page.getByRole("button", { name: "Abrir estudio" }).click();
  await page.getByRole("button", { name: /Probar/ }).click();
  await expect(page.getByText("Estado ESPERANDO · v7", { exact: true })).toBeVisible();
  const inicial = control.consultasInstancia;
  control.instancia.estado = "COMPLETADA";
  await page.clock.fastForward(15_001);
  await expect(page.getByText("La instancia terminó completada.", { exact: true })).toBeVisible();
  expect(control.consultasInstancia).toBe(inicial + 1);
  const final = control.consultasInstancia;
  await page.clock.fastForward(45_000);
  expect(control.consultasInstancia).toBe(final);
});

test("el rechazo conserva motivo y muestra error si falla el envío", async ({ page }) => {
  const control = await preparar(page);
  control.errorCompletar = true;
  await page.getByRole("button", { name: "Abrir estudio" }).click();
  await page.getByRole("button", { name: /Probar/ }).click();
  await page.getByRole("button", { name: "Rechazar", exact: true }).click();
  await page.getByLabel("Motivo del rechazo").fill("Motivo conservado");
  await page.getByRole("button", { name: "Confirmar rechazo" }).click();
  await expect(page.getByRole("alert")).toContainText("La tarea fue modificada");
  await expect(page.getByLabel("Motivo del rechazo")).toHaveValue("Motivo conservado");
  await expect(page.getByRole("button", { name: "Confirmar rechazo" })).toBeEnabled();
});

test("bloquea también un grafo actualizado después de abrir desde caché", async ({ page }) => {
  await page.clock.install();
  const control = await preparar(page);
  await page.getByRole("button", { name: "Abrir estudio" }).click();
  await expect(page.getByRole("button", { name: "Configurar", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Volver", exact: true }).click();
  control.grafo = grafoLineal();
  control.grafo.aristas[0].condicion = "decision=APROBADO";
  await page.clock.fastForward(16_000);
  await page.getByRole("button", { name: "Abrir estudio" }).click();
  await expect(page.getByText(/no puede representar fielmente/i)).toBeVisible();
  await expect(page.getByRole("button", { name: "Guardar borrador" })).toBeDisabled();
});
