import type { GrafoProceso, NodoProceso, TipoNodoProceso } from "../api/procesos";

export interface Paso {
  id: string;
  tipo: TipoNodoProceso;
  nombre: string;
  tipoDocumento?: string;
  subprocesoCodigo?: string;
  asignadoA?: string;
  slaHoras?: number;
}

const mensajeBloqueo = "procesos.bloqueoSecuencial";
const propiedades = ["tipoDocumento", "subprocesoCodigo", "asignadoA", "slaHoras"] as const;

function comoPaso(nodo: NodoProceso): Paso {
  const configuracion = nodo.configuracion ?? {};
  return {
    id: nodo.id,
    tipo: nodo.tipo,
    nombre: nodo.nombre ?? nodo.tipo,
    tipoDocumento: configuracion.tipoDocumento as string | undefined,
    subprocesoCodigo: configuracion.subprocesoCodigo as string | undefined,
    asignadoA: configuracion.asignadoA as string | undefined,
    slaHoras: configuracion.slaHoras as number | undefined,
  };
}

export function inspeccionarGrafo(grafo: GrafoProceso): {
  pasos: Paso[];
  bloqueo: string | null;
} {
  const bloquear = () => ({ pasos: [], bloqueo: mensajeBloqueo });
  if (!Array.isArray(grafo.nodos) || !Array.isArray(grafo.aristas)) return bloquear();
  if (!grafo.nodos.length && !grafo.aristas.length) return { pasos: [], bloqueo: null };
  const nodos = new Map(grafo.nodos.map((nodo) => [nodo.id, nodo]));
  if (nodos.size !== grafo.nodos.length || grafo.nodos.some((nodo) => !nodo.id)) return bloquear();
  const inicios = grafo.nodos.filter((nodo) => nodo.tipo === "INICIO");
  const finales = grafo.nodos.filter((nodo) => nodo.tipo === "FIN");
  if (inicios.length !== 1 || finales.length !== 1) return bloquear();
  const tipos = [
    "INICIO",
    "FIN",
    "SOLICITUD_DOCUMENTO",
    "FORMULARIO",
    "VALIDACION_IA",
    "REVISION_HUMANA",
    "TAREA_EXTERNA",
    "NOTIFICACION",
    "TEMPORIZADOR",
    "ACCION_API",
    "SUBPROCESO",
  ];
  for (const nodo of grafo.nodos) {
    if (!tipos.includes(nodo.tipo)) return bloquear();
    if (
      nodo.configuracion != null &&
      (typeof nodo.configuracion !== "object" || Array.isArray(nodo.configuracion))
    )
      return bloquear();
    for (const propiedad of propiedades) {
      const valor = nodo.configuracion?.[propiedad];
      if (
        valor != null &&
        (propiedad === "slaHoras"
          ? typeof valor !== "number" || !Number.isFinite(valor)
          : typeof valor !== "string")
      )
        return bloquear();
    }
  }
  const salidas = new Map<string, string>();
  const entradas = new Set<string>();
  for (const arista of grafo.aristas) {
    if (
      arista.condicion != null ||
      Object.keys(arista).some((clave) => !["origen", "destino", "condicion"].includes(clave))
    )
      return bloquear();
    if (
      !nodos.has(arista.origen) ||
      !nodos.has(arista.destino) ||
      salidas.has(arista.origen) ||
      entradas.has(arista.destino)
    )
      return bloquear();
    salidas.set(arista.origen, arista.destino);
    entradas.add(arista.destino);
  }
  if (entradas.has(inicios[0].id) || salidas.has(finales[0].id)) return bloquear();
  const visitados = new Set<string>();
  const pasos: Paso[] = [];
  let actual: string | undefined = inicios[0].id;
  while (actual) {
    if (visitados.has(actual)) return bloquear();
    visitados.add(actual);
    const nodo = nodos.get(actual)!;
    if (nodo.tipo !== "INICIO" && nodo.tipo !== "FIN") pasos.push(comoPaso(nodo));
    actual = salidas.get(actual);
  }
  if (visitados.size !== nodos.size || !visitados.has(finales[0].id)) return bloquear();
  return { pasos, bloqueo: null };
}

export function serializarGrafo(grafo: GrafoProceso, pasos: Paso[]): GrafoProceso {
  const analisis = inspeccionarGrafo(grafo);
  if (analisis.bloqueo) throw new Error(analisis.bloqueo);
  const inicio = grafo.nodos.find((nodo) => nodo.tipo === "INICIO") ?? {
    id: "inicio",
    tipo: "INICIO" as const,
    nombre: "Inicio",
  };
  const fin = grafo.nodos.find((nodo) => nodo.tipo === "FIN") ?? {
    id: "fin",
    tipo: "FIN" as const,
    nombre: "Fin",
  };
  const originales = new Map(grafo.nodos.map((nodo) => [nodo.id, nodo]));
  const nodos = [
    inicio,
    ...pasos.map((paso) => {
      const original = originales.get(paso.id);
      const nodo: NodoProceso = original
        ? { ...original }
        : { id: paso.id, tipo: paso.tipo, nombre: paso.nombre };
      const previo = original ? comoPaso(original) : undefined;
      if (previo && paso.tipo !== previo.tipo) throw new Error(mensajeBloqueo);
      if (!previo || paso.nombre !== previo.nombre) nodo.nombre = paso.nombre;
      for (const propiedad of propiedades) {
        if (paso[propiedad] === previo?.[propiedad]) continue;
        nodo.configuracion = { ...nodo.configuracion };
        if (paso[propiedad] === undefined) delete nodo.configuracion[propiedad];
        else nodo.configuracion[propiedad] = paso[propiedad];
      }
      return nodo;
    }),
    fin,
  ];
  const mismaSecuencia =
    analisis.pasos.length === pasos.length &&
    analisis.pasos.every((paso, indice) => paso.id === pasos[indice].id);
  const resultado = {
    ...grafo,
    nodos:
      mismaSecuencia && grafo.nodos.length
        ? grafo.nodos.map((nodo) => nodos.find((actual) => actual.id === nodo.id)!)
        : nodos,
    aristas:
      mismaSecuencia && grafo.nodos.length
        ? grafo.aristas
        : nodos.slice(1).map((nodo, indice) => ({
            origen: nodos[indice].id,
            destino: nodo.id,
          })),
  };
  if (inspeccionarGrafo(resultado).bloqueo) throw new Error(mensajeBloqueo);
  return resultado;
}
