import type { GrafoProceso, NodoProceso } from "../api/procesos";

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
