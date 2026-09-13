import { expect, type Page } from "@playwright/test";
import type {
  DetalleDocumento,
  Documento,
  Excepcion,
  KpiResumen,
  Sesion,
} from "../src/tipos/api";
import type { Proceso } from "../src/api/procesos";

export function esperaControlada() {
  let liberar!: () => void;
  const promesa = new Promise<void>((resolver) => {
    liberar = resolver;
  });
  return { promesa, liberar };
}

export function detalleInicial(): DetalleDocumento {
  return {
    documento: {
      id: "documento",
      nombre: "Documento A.pdf",
      estado: "VALIDADO",
      origen: "WEB",
      codigoPlantilla: "FACTURA",
      numeroVersionPlantilla: 3,
      sujetoOrigen: "Pruebas",
      sujetoTipoObjeto: "Operacion",
      sujetoIdObjeto: "123",
      archivos: [],
      transicionesPosibles: ["APROBADO", "OBSERVADO", "RECHAZADO", "CERRADO"],
    },
    extraccion: {
      id: "extraccion",
      proveedor: "SIMULADO",
      estado: "COMPLETADA",
      intento: 1,
      tokensEntrada: 0,
      tokensSalida: 0,
      paginasProcesadas: 1,
      duracionMilisegundos: 10,
      valores: [
        {
          id: "valor",
          claveCampo: "razonSocial",
          etiqueta: "Razón social",
          valorNormalizado: "Empresa original",
          presencia: "PRESENTE",
          confianza: 0.8,
        },
      ],
    },
    candidatos: [
      {
        id: "candidato",
        conector: "CONTROLADO",
        idObjeto: "456",
        descripcion: "Operación candidata",
        seleccionado: false,
      },
    ],
    revisiones: [],
    segmentos: [],
  };
}

export async function preparar(pagina: Page, restaurar = true) {
  const detalle = detalleInicial();
  const proceso: Proceso = {
    id: "proceso",
    codigo: "PRUEBA",
    familia: "Pruebas",
    nombre: "Proceso controlado",
    versiones: [
      {
        id: "version",
        definicionId: "proceso",
        codigoDefinicion: "PRUEBA",
        numero: 8,
        estado: "BORRADOR",
        grafo: {
          nodos: [
            { id: "inicio", tipo: "INICIO" },
            { id: "paso", tipo: "FORMULARIO", nombre: "Paso" },
            { id: "fin", tipo: "FIN" },
          ],
          aristas: [
            { origen: "inicio", destino: "paso" },
            { origen: "paso", destino: "fin" },
          ],
        },
      },
    ],
  };
  const control = {
    detalle,
    proceso,
    documentos: { A: "Documento A.pdf", B: "Documento B.pdf" } as Record<
      string,
      string
    >,
    demoraDocumentos: null as Promise<void> | null,
    demoraMutacion: null as Promise<void> | null,
    errorMutacion: false,
    errorLogin: "",
    kpi: 1,
    peticiones: [] as {
      metodo: string;
      ruta: string;
      parametros: Record<string, string>;
      cuerpo: unknown;
      tenant: string;
    }[],
    inesperadas: [] as string[],
    errores: [] as string[],
    consola: [] as string[],
  };
  if (restaurar)
    await pagina.addInitScript(() =>
      sessionStorage.setItem("nextdocs.tokenRefresco", "A"),
    );
  pagina.on("pageerror", (error) => control.errores.push(error.message));
  pagina.on("console", (mensaje) => {
    if (
      ["error", "warning"].includes(mensaje.type()) &&
      !mensaje.text().startsWith("Failed to load resource:")
    )
      control.consola.push(mensaje.text());
  });
  await pagina.route("**/api/v1/**", async (ruta) => {
    const peticion = ruta.request();
    const url = new URL(peticion.url());
    const camino = url.pathname;
    const tenant =
      peticion.headers().authorization?.replace("Bearer ", "") ?? "A";
    const cuerpo = peticion.postData() ? peticion.postDataJSON() : null;
    control.peticiones.push({
      metodo: peticion.method(),
      ruta: camino,
      parametros: Object.fromEntries(url.searchParams),
      cuerpo,
      tenant,
    });
    const responder = (json: unknown, status = 200) =>
      ruta.fulfill({ json, status });
    if (camino.endsWith("/autenticacion/refrescar"))
      return responder(sesionControlada(cuerpo.tokenRefresco));
    if (camino.endsWith("/autenticacion/ingresar"))
      return control.errorLogin
        ? responder({ mensaje: control.errorLogin }, 400)
        : responder(sesionControlada(cuerpo.codigoTenant));
    if (camino === "/api/v1/documentos") {
      const documento: Documento = {
        ...detalle.documento,
        nombre: control.documentos[tenant],
      };
      const demora = control.demoraDocumentos;
      if (demora) await demora;
      return responder({
        content: [documento],
        totalElements: 1,
        totalPages: 1,
        number: 0,
        size: 25,
      });
    }
    if (camino.endsWith("/detalle")) return responder(detalle);
    if (camino === "/api/v1/documentos/resumen")
      return responder({ VALIDADO: 1 });
    if (camino === "/api/v1/excepciones") {
      const pagina = Number(url.searchParams.get("pagina"));
      const tamano = Number(url.searchParams.get("tamano"));
      const excepciones: Excepcion[] = Array.from({ length: 30 }, (_, i) => ({
        id: String(i),
        codigo: "EXCEPCION-" + i,
        documentoId: "documento",
        tipo: "VALIDACION",
        severidad: "REQUIERE_REVISION",
        prioridad: "ALTA",
        estado: "ABIERTA",
        detalle: "Evidencia " + i,
      }));
      return responder({
        content: excepciones.slice(pagina * tamano, (pagina + 1) * tamano),
        totalElements: 30,
        totalPages: Math.ceil(30 / tamano),
        number: pagina,
        size: tamano,
      });
    }
    if (camino === "/api/v1/kpi/resumen") {
      const resumen: KpiResumen = {
        rango: {
          desde: "2026-08-01T00:00:00Z",
          hasta: "2026-09-01T00:00:00Z",
          dias: 30,
        },
        porEstado: { VALIDADO: 1 },
        indicadores: [
          {
            clave: "documentosRecibidos",
            etiqueta: "Documentos recibidos",
            unidad: "CONTEO",
            valor: control.kpi,
            tendencia: "SIN_COMPARACION",
            formula: "Documentos raíz recibidos en el período",
            tienePoblacion: true,
          },
        ],
      };
      return responder(resumen);
    }
    if (camino === "/api/v1/kpi/plantillas") return responder([]);
    if (camino === "/api/v1/kpi/poblacion")
      return responder([detalle.documento]);
    if (camino === "/api/v1/procesos") return responder([proceso]);
    if (camino === "/api/v1/procesos/proceso") return responder(proceso);
    if (peticion.method() === "POST") {
      if (control.demoraMutacion) await control.demoraMutacion;
      if (control.errorMutacion)
        return responder({ mensaje: "Acción no disponible" }, 409);
      if (/\/revisiones$|\/reprocesar$|\/cerrar$|\/seleccionar$/.test(camino)) {
        control.kpi = 2;
        return responder({ estadoNuevo: "APROBADO" });
      }
      if (camino.endsWith("/publicar")) {
        proceso.versiones[0].estado = "PUBLICADA";
        return responder(proceso.versiones[0]);
      }
      if (camino.endsWith("/versiones")) {
        const nueva = {
          ...proceso.versiones[0],
          id: "nueva",
          numero: 9,
          estado: "BORRADOR",
        };
        proceso.versiones.unshift({
          ...proceso.versiones[0],
          id: "nueva",
          numero: 9,
          estado: "BORRADOR",
        });
        return responder(nueva);
      }
    }
    control.inesperadas.push(camino);
    return responder({ mensaje: "Endpoint inesperado" }, 404);
  });
  return control;
}

export async function navegar(pagina: Page, nombre: string) {
  const abrir = pagina.getByRole("button", { name: "Abrir navegación" });
  if (await abrir.isVisible()) await abrir.click();
  await pagina.getByRole("link", { name: nombre, exact: true }).first().click();
}

export async function salir(pagina: Page) {
  await pagina.getByRole("button", { name: /Menú de usuario/ }).click();
  await pagina
    .getByRole("button", { name: "Cerrar sesión", exact: true })
    .click();
  await expect(
    pagina.getByLabel("Organización", { exact: true }),
  ).toBeVisible();
}

export async function ingresar(pagina: Page, tenant: string) {
  await pagina.getByLabel("Organización", { exact: true }).fill(tenant);
  await pagina.getByLabel("Email").fill("persona@prueba.test");
  await pagina.getByLabel("Clave").fill("clave-controlada");
  await pagina.getByLabel("Clave").press("Enter");
  await expect(
    pagina.getByRole("heading", { name: "Resumen operativo" }),
  ).toBeVisible();
}

export const sesionControlada = (tenant: string): Sesion => ({
  tokenAcceso: tenant,
  tokenRefresco: tenant,
  tenantId: tenant,
  codigoTenant: tenant,
  usuarioId: "persona",
  nombre: "Persona",
  email: "persona@prueba.test",
  nombreTenant: "Organización " + tenant,
  permisos: [
    "documentos.leer",
    "documentos.escribir",
    "documentos.revisar",
    "excepciones.leer",
    "excepciones.gestionar",
    "tenant.administrar",
    "plantillas.publicar",
  ],
});
