import { test, expect, type Page } from "@playwright/test";
import type { ReglaSupervisora } from "../src/api/procesos";
import { elegirEnDesplegable } from "../pruebas-transversales/desplegable";

function reglaBase(): ReglaSupervisora {
  return {
    id: "regla-1",
    plantillaId: "proceso",
    nombre: "Confianza minima",
    tipo: "confidence",
    umbral: 80,
    operador: "MENOR",
    accion: "ADVERTIR",
    severidad: "ALTA",
    mensaje: "Revisar la extraccion",
  };
}

async function preparar(pagina: Page, reglas: ReglaSupervisora[] = []) {
  const control = {
    reglas: [...reglas],
    creadas: [] as unknown[],
    actualizadas: [] as unknown[],
    bajas: [] as string[],
    errorCrear: false,
  };
  const proceso = {
    id: "proceso",
    codigo: "PRUEBA",
    familia: "Pruebas",
    nombre: "Proceso controlado",
    versiones: [],
  };
  await pagina.addInitScript(() =>
    sessionStorage.setItem("nextdocs.tokenRefresco", "controlado"),
  );
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
    if (camino === "/api/v1/procesos") return responder([proceso]);
    if (camino === "/api/v1/supervisora/reglas" && peticion.method() === "GET") {
      expect(peticion.headers()["x-tenant-id"]).toBe("tenant-prueba");
      return responder(control.reglas);
    }
    if (camino === "/api/v1/supervisora/reglas" && peticion.method() === "POST") {
      if (control.errorCrear) return responder({ mensaje: "El umbral es obligatorio" }, 400);
      const cuerpo = peticion.postDataJSON();
      control.creadas.push(cuerpo);
      const creada = { id: `regla-${control.reglas.length + 1}`, ...cuerpo };
      control.reglas.push(creada);
      return responder(creada, 201);
    }
    if (camino.startsWith("/api/v1/supervisora/reglas/") && peticion.method() === "PUT") {
      const id = camino.split("/").pop() as string;
      const cuerpo = peticion.postDataJSON();
      control.actualizadas.push(cuerpo);
      const indice = control.reglas.findIndex((regla) => regla.id === id);
      control.reglas[indice] = { id, ...cuerpo };
      return responder(control.reglas[indice]);
    }
    if (camino.startsWith("/api/v1/supervisora/reglas/") && peticion.method() === "DELETE") {
      const id = camino.split("/").pop() as string;
      control.bajas.push(id);
      control.reglas = control.reglas.filter((regla) => regla.id !== id);
      return ruta.fulfill({ status: 204, body: "" });
    }
    return responder({ mensaje: "Endpoint inesperado" }, 404);
  });
  await pagina.goto("/supervisora");
  return control;
}

test("sin reglas avisa que la supervisora no genera hallazgos", async ({ page }) => {
  await preparar(page);
  await expect(page.getByText("Todavia no hay reglas", { exact: true })).toBeVisible();
});

test("crear una regla exige nombre, dato y umbral, y manda el cuerpo completo", async ({
  page,
}) => {
  const control = await preparar(page);
  await page.getByRole("button", { name: "Nueva regla" }).click();

  const crear = page.getByRole("button", { name: "Crear regla" });
  await expect(crear).toBeDisabled();

  await page.getByLabel("Nombre de la regla").fill("Costo maximo");
  await expect(crear).toBeDisabled();
  await page.getByLabel("Dato a observar").fill("costo");
  await expect(crear).toBeDisabled();
  await page.getByLabel("Umbral").fill("500");
  await expect(crear).toBeEnabled();

  await elegirEnDesplegable(page, "Condicion", "es mayor que");
  await elegirEnDesplegable(page, "Que hace la supervisora", "Bloquear la instancia");
  await elegirEnDesplegable(page, "Severidad del hallazgo", "Critica");
  await crear.click();

  expect(control.creadas).toEqual([
    {
      nombre: "Costo maximo",
      tipo: "costo",
      umbral: 500,
      operador: "MAYOR",
      accion: "BLOQUEAR",
      severidad: "CRITICA",
    },
  ]);
  await expect(page.getByText("Si costo es mayor que 500", { exact: true })).toBeVisible();
});

test("un umbral por encima del maximo de la columna no se puede guardar", async ({ page }) => {
  await preparar(page);
  await page.getByRole("button", { name: "Nueva regla" }).click();
  await page.getByLabel("Nombre de la regla").fill("Fuera de rango");
  await page.getByLabel("Dato a observar").fill("monto");
  await page.getByLabel("Umbral").fill("1000");

  await expect(page.getByText("Tiene que ser un numero de hasta 999.99")).toBeVisible();
  await expect(page.getByRole("button", { name: "Crear regla" })).toBeDisabled();
});

test("editar una regla manda el umbral nuevo y refresca la condicion", async ({ page }) => {
  const control = await preparar(page, [reglaBase()]);
  await expect(page.getByText("Si confidence es menor que 80", { exact: true })).toBeVisible();

  await page.getByRole("button", { name: "Editar" }).click();
  await page.getByLabel("Umbral").fill("95");
  await page.getByRole("button", { name: "Guardar cambios" }).click();

  expect(control.actualizadas).toEqual([
    {
      plantillaId: "proceso",
      nombre: "Confianza minima",
      tipo: "confidence",
      umbral: 95,
      operador: "MENOR",
      accion: "ADVERTIR",
      severidad: "ALTA",
      mensaje: "Revisar la extraccion",
    },
  ]);
  await expect(page.getByText("Si confidence es menor que 95", { exact: true })).toBeVisible();
});

test("dar de baja pide confirmacion antes de llamar al backend", async ({ page }) => {
  const control = await preparar(page, [reglaBase()]);

  await page.getByRole("button", { name: "Dar de baja" }).click();
  expect(control.bajas).toEqual([]);
  await expect(page.getByText(/La regla deja de evaluarse/)).toBeVisible();

  await page.getByRole("button", { name: "Confirmar baja" }).click();
  await expect(page.getByText("Todavia no hay reglas", { exact: true })).toBeVisible();
  expect(control.bajas).toEqual(["regla-1"]);
});

test("arrepentirse de la baja deja la regla intacta", async ({ page }) => {
  const control = await preparar(page, [reglaBase()]);

  await page.getByRole("button", { name: "Dar de baja" }).click();
  await page.getByRole("button", { name: "Conservar" }).click();

  await expect(page.getByText(/La regla deja de evaluarse/)).toBeHidden();
  await expect(page.getByText("Confianza minima", { exact: true })).toBeVisible();
  expect(control.bajas).toEqual([]);
});

test("una regla que bloquea se distingue de una que solo advierte", async ({ page }) => {
  await preparar(page, [
    reglaBase(),
    { ...reglaBase(), id: "regla-2", nombre: "Costo critico", accion: "BLOQUEAR" },
  ]);

  await expect(page.getByText("Bloquea", { exact: true })).toHaveCount(1);
  await expect(page.getByText("Advertir", { exact: true })).toHaveCount(1);
});

test("una regla sin plantilla se muestra como global", async ({ page }) => {
  await preparar(page, [{ ...reglaBase(), plantillaId: undefined }]);
  await expect(page.getByText("Todas las plantillas", { exact: true })).toBeVisible();
});

test("el error del backend al crear se muestra en el formulario", async ({ page }) => {
  const control = await preparar(page);
  control.errorCrear = true;
  await page.getByRole("button", { name: "Nueva regla" }).click();
  await page.getByLabel("Nombre de la regla").fill("Con error");
  await page.getByLabel("Dato a observar").fill("monto");
  await page.getByLabel("Umbral").fill("10");
  await page.getByRole("button", { name: "Crear regla" }).click();

  await expect(page.getByText("El umbral es obligatorio", { exact: true })).toBeVisible();
});
