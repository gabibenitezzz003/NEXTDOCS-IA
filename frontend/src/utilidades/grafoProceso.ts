import type { GrafoProceso, NodoProceso, Proceso } from "../api/procesos";

export interface DisparoInicio {
  evento?: string;
  tipoDocumento?: string;
  horaDiaria?: string;
  intervaloMinutos?: number;
}

export function disparoDeInicio(
  grafo: GrafoProceso | null | undefined,
): DisparoInicio {
  const inicio = grafo?.nodos?.find((nodo) => nodo.tipo === "INICIO");
  const configuracion = inicio?.configuracion ?? {};
  const texto = (clave: string) => {
    const valor = configuracion[clave];
    return typeof valor === "string" && valor.trim() ? valor.trim() : undefined;
  };
  const numero = (clave: string) => {
    const valor = configuracion[clave];
    return typeof valor === "number" && Number.isFinite(valor) && valor > 0
      ? valor
      : undefined;
  };
  return {
    evento: texto("evento"),
    tipoDocumento: texto("tipoDocumento"),
    horaDiaria: texto("horaDiaria"),
    intervaloMinutos: numero("programadoMinutos"),
  };
}

export interface ProcesoReferencia {
  id: string;
  nombre: string;
}

export interface UsoTipoDocumento {
  dispara: ProcesoReferencia[];
  pide: ProcesoReferencia[];
}

const TIPOS_QUE_PIDEN_DOCUMENTO = new Set([
  "SOLICITUD_DOCUMENTO",
  "FIRMA",
  "TAREA_EXTERNA",
]);

export function usosDeTipoDocumento(
  procesos: Proceso[],
  codigoPlantilla: string,
): UsoTipoDocumento {
  const dispara = new Map<string, ProcesoReferencia>();
  const pide = new Map<string, ProcesoReferencia>();
  for (const proceso of procesos) {
    const version =
      proceso.versiones.find((v) => v.estado === "PUBLICADA") ??
      proceso.versiones.find((v) => v.estado === "BORRADOR") ??
      proceso.versiones[0];
    for (const nodo of version?.grafo?.nodos ?? []) {
      const tipo = nodo.configuracion?.["tipoDocumento"];
      if (
        typeof tipo !== "string" ||
        tipo.trim().toLowerCase() !== codigoPlantilla.trim().toLowerCase()
      )
        continue;
      const referencia = { id: proceso.id, nombre: proceso.nombre };
      if (nodo.tipo === "INICIO") dispara.set(proceso.id, referencia);
      else if (TIPOS_QUE_PIDEN_DOCUMENTO.has(nodo.tipo))
        pide.set(proceso.id, referencia);
    }
  }
  return { dispara: [...dispara.values()], pide: [...pide.values()] };
}

export function aplicarConfiguracion(
  nodo: NodoProceso,
  clave: string,
  valor: unknown,
): NodoProceso {
  const configuracion = { ...nodo.configuracion };
  if (valor === undefined || valor === "") delete configuracion[clave];
  else configuracion[clave] = valor;
  return { ...nodo, configuracion };
}

export function avisosNodo(nodo: NodoProceso, grafo?: GrafoProceso): string[] {
  const configuracion = nodo.configuracion ?? {};
  const texto = (clave: string) => {
    const valor = configuracion[clave];
    return typeof valor === "string" && valor.trim() ? valor : undefined;
  };
  const numero = (clave: string) => {
    const valor = configuracion[clave];
    return typeof valor === "number" && Number.isFinite(valor) && valor > 0
      ? valor
      : undefined;
  };
  const lista = (clave: string) => {
    const valor = configuracion[clave];
    return Array.isArray(valor) && valor.some((item) => String(item).trim())
      ? valor
      : undefined;
  };
  const avisos: string[] = [];
  switch (nodo.tipo) {
    case "VALIDACION_IA":
      if (!lista("condiciones") && !texto("condicion"))
        avisos.push("canvas.avisoCondiciones");
      break;
    case "ACCION_API":
      if (!texto("url")) avisos.push("canvas.avisoUrl");
      break;
    case "NOTIFICACION":
      if (!texto("mensaje")) avisos.push("canvas.avisoMensaje");
      break;
    case "TEMPORIZADOR":
      if (
        numero("segundos") == null &&
        numero("minutos") == null &&
        numero("horas") == null &&
        numero("slaHoras") == null
      )
        avisos.push("canvas.avisoDuracion");
      break;
    case "SUBPROCESO":
      if (!texto("subprocesoCodigo")) avisos.push("canvas.avisoSubproceso");
      break;
    case "SOLICITUD_DOCUMENTO":
    case "FIRMA":
      if (!texto("tipoDocumento") && !lista("identificadores"))
        avisos.push("canvas.avisoTipoDocumento");
      break;
    case "PARALELO":
      if (
        grafo &&
        grafo.aristas.filter((arista) => arista.origen === nodo.id).length < 2
      )
        avisos.push("canvas.avisoParalelo");
      break;
    case "UNION":
      if (grafo) {
        const entradas = grafo.aristas.filter(
          (arista) => arista.destino === nodo.id,
        ).length;
        const salidas = grafo.aristas.filter(
          (arista) => arista.origen === nodo.id,
        ).length;
        if (entradas < 2) avisos.push("canvas.avisoUnionEntradas");
        if (salidas > 1) avisos.push("canvas.avisoUnionSalida");
      }
      break;
    case "INICIO":
      if (
        configuracion.programadoMinutos != null &&
        numero("programadoMinutos") == null
      )
        avisos.push("canvas.avisoProgramado");
      break;
    case "DECISION":
      break;
    default:
      break;
  }
  return avisos;
}
