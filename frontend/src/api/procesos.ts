import axios from "axios";
import {
  instalarRefrescoDeSesion,
  mensajeDeError,
  tokenAccesoActual,
} from "./cliente";

export type TipoNodoProceso =
  | "INICIO"
  | "FIN"
  | "SOLICITUD_DOCUMENTO"
  | "FORMULARIO"
  | "VALIDACION_IA"
  | "REVISION_HUMANA"
  | "DECISION"
  | "TAREA_EXTERNA"
  | "NOTIFICACION"
  | "TEMPORIZADOR"
  | "ACCION_API"
  | "SUBPROCESO";

export type EstadoVersionProceso = "BORRADOR" | "PUBLICADA" | "ARCHIVADA";

export interface NodoProceso {
  id: string;
  tipo: TipoNodoProceso;
  nombre?: string;
  configuracion?: Record<string, unknown>;
}

export interface AristaProceso {
  origen: string;
  destino: string;
  condicion?: string;
}

export interface GrafoProceso {
  nodos: NodoProceso[];
  aristas: AristaProceso[];
}

export interface VersionProceso {
  id: string;
  definicionId: string;
  codigoDefinicion: string;
  numero: number;
  estado: EstadoVersionProceso;
  grafo: GrafoProceso;
  hash?: string;
  cambios?: string;
  alta?: string;
  publicada?: string;
}

export interface Proceso {
  id: string;
  codigo: string;
  familia: string;
  nombre: string;
  descripcion?: string;
  visibilidad?: string;
  etiquetas?: string;
  slaHoras?: number;
  versiones: VersionProceso[];
  alta?: string;
}

export interface TareaProceso {
  id: string;
  instanciaId: string;
  codigoDefinicion?: string;
  nombreDefinicion?: string;
  nodoId: string;
  nombreNodo?: string;
  tipoNodo: string;
  estado: "PENDIENTE" | "COMPLETADA" | "CANCELADA" | "VENCIDA";
  asignadoA?: string;
  decision?: string;
  datos?: Record<string, unknown>;
  motivo?: string;
  vencimiento?: string;
  alta?: string;
  completada?: string;
  completadaPor?: string;
}

export interface EventoInstancia {
  id: string;
  nodoId?: string;
  accion: string;
  detalle?: Record<string, unknown>;
  actor?: string;
  alta?: string;
}

export type EstadoInstanciaProceso =
  | "CREADA"
  | "ACTIVA"
  | "ESPERANDO"
  | "BLOQUEADA"
  | "COMPLETADA"
  | "CANCELADA";

export interface InstanciaProceso {
  id: string;
  definicionId: string;
  codigoDefinicion: string;
  numeroVersion: number;
  estado: EstadoInstanciaProceso;
  sujetoOrigen?: string;
  sujetoTipo?: string;
  sujetoId?: string;
  datos?: Record<string, unknown>;
  correlacionId?: string;
  tareas: TareaProceso[];
  eventos?: EventoInstancia[];
  alta?: string;
  fin?: string;
}

export interface HallazgoProceso {
  id: string;
  instanciaId: string;
  nodoId?: string;
  reglaId?: string;
  tipo: string;
  severidad?: "BAJA" | "MEDIA" | "ALTA" | "CRITICA";
  accion?: string;
  descripcion?: string;
  estado?: "PENDIENTE" | "APROBADO" | "RECHAZADO";
  referencia?: Record<string, unknown>;
  alta?: string;
  resolucion?: string;
}

export type OperadorRegla = "MAYOR" | "MENOR";

export type AccionRegla = "SOLICITAR" | "ADVERTIR" | "BLOQUEAR" | "REVIEW";

export type SeveridadRegla = "BAJA" | "MEDIA" | "ALTA" | "CRITICA";

export interface ReglaSupervisora {
  id: string;
  plantillaId?: string;
  nombre: string;
  tipo: string;
  umbral: number;
  operador?: OperadorRegla;
  accion?: AccionRegla;
  severidad?: SeveridadRegla;
  mensaje?: string;
  alta?: string;
}

export interface CambiosRegla {
  plantillaId?: string;
  nombre: string;
  tipo: string;
  umbral: number;
  operador?: OperadorRegla;
  accion?: AccionRegla;
  severidad?: SeveridadRegla;
  mensaje?: string;
}

export interface KpiProcesoIndicador {
  codigo: string;
  nombre: string;
  valor?: number | null;
  unidad?: string;
  formula?: string;
  fuente?: string;
  tendencia?: string;
  estadoFuente?: string;
  drilldown?: string;
}

export interface KpiProcesoResumen {
  desde?: string;
  hasta?: string;
  indicadores: KpiProcesoIndicador[];
}

export interface KpiProcesoPoblacion {
  id: string;
  entidad: string;
  codigoDefinicion?: string;
  nodoId?: string;
  estado?: string;
  asignadoA?: string;
  alta?: string;
  fin?: string;
  vencimiento?: string;
  duracionMinutos?: number | null;
}

let tenantActual: string | null = null;

export const clienteProcesos = axios.create({
  baseURL: "/api/v1",
  headers: { "Content-Type": "application/json" },
});

clienteProcesos.interceptors.request.use((configuracion) => {
  const token = tokenAccesoActual();
  if (token) {
    configuracion.headers.Authorization = `Bearer ${token}`;
  }
  if (tenantActual) {
    configuracion.headers["X-Tenant-Id"] = tenantActual;
  }
  return configuracion;
});

instalarRefrescoDeSesion(clienteProcesos);

export function fijarTenantProcesos(tenantId: string | null) {
  tenantActual = tenantId;
}

export { mensajeDeError };

export interface NuevoProceso {
  codigo: string;
  familia: string;
  nombre: string;
  descripcion?: string;
  slaHoras?: number;
}

export interface CambiosProceso {
  familia: string;
  nombre: string;
  descripcion?: string;
  etiquetas?: string;
  slaHoras?: number;
}

export async function listarProcesos(): Promise<Proceso[]> {
  const { data } = await clienteProcesos.get<Proceso[]>("/procesos");
  return data;
}

export async function crearProceso(requerimiento: NuevoProceso): Promise<Proceso> {
  const { data } = await clienteProcesos.post<Proceso>("/procesos", requerimiento);
  return data;
}

export async function obtenerProceso(definicionId: string): Promise<Proceso> {
  const { data } = await clienteProcesos.get<Proceso>(`/procesos/${definicionId}`);
  return data;
}

export async function actualizarProceso(
  definicionId: string,
  cambios: CambiosProceso,
): Promise<Proceso> {
  const { data } = await clienteProcesos.put<Proceso>(`/procesos/${definicionId}`, cambios);
  return data;
}

export async function nuevaVersion(definicionId: string, cambios?: string): Promise<VersionProceso> {
  const { data } = await clienteProcesos.post<VersionProceso>(`/procesos/${definicionId}/versiones`, {
    cambios,
  });
  return data;
}

export async function actualizarGrafo(versionId: string, grafo: GrafoProceso): Promise<VersionProceso> {
  const { data } = await clienteProcesos.put<VersionProceso>(`/procesos/versiones/${versionId}/grafo`, grafo);
  return data;
}

export async function validarVersion(versionId: string): Promise<VersionProceso> {
  const { data } = await clienteProcesos.post<VersionProceso>(`/procesos/versiones/${versionId}/validar`);
  return data;
}

export async function publicarVersion(versionId: string): Promise<VersionProceso> {
  const { data } = await clienteProcesos.post<VersionProceso>(`/procesos/versiones/${versionId}/publicar`);
  return data;
}

export async function iniciarInstancia(definicionId: string): Promise<InstanciaProceso> {
  const { data } = await clienteProcesos.post<InstanciaProceso>("/instancias", { definicionId });
  return data;
}

export async function obtenerInstancia(instanciaId: string): Promise<InstanciaProceso> {
  const { data } = await clienteProcesos.get<InstanciaProceso>(`/instancias/${instanciaId}`);
  return data;
}

export async function listarInstancias(
  estado?: string,
  definicion?: string,
): Promise<InstanciaProceso[]> {
  const { data } = await clienteProcesos.get<InstanciaProceso[]>("/instancias", {
    params: {
      ...(estado ? { estado } : {}),
      ...(definicion ? { definicion } : {}),
    },
  });
  return data;
}

export async function pausarInstancia(
  instanciaId: string,
  cuerpo: { motivo: string; actor?: string },
): Promise<InstanciaProceso> {
  const { data } = await clienteProcesos.post<InstanciaProceso>(
    `/instancias/${instanciaId}/pausar`,
    cuerpo,
  );
  return data;
}

export async function reanudarInstancia(
  instanciaId: string,
  actor?: string,
): Promise<InstanciaProceso> {
  const { data } = await clienteProcesos.post<InstanciaProceso>(
    `/instancias/${instanciaId}/reanudar`,
    { actor },
  );
  return data;
}

export async function cancelarInstancia(
  instanciaId: string,
  cuerpo: { motivo?: string; actor?: string },
): Promise<InstanciaProceso> {
  const { data } = await clienteProcesos.post<InstanciaProceso>(
    `/instancias/${instanciaId}/cancelar`,
    cuerpo,
  );
  return data;
}

export async function listarTareas(estados?: string[]): Promise<TareaProceso[]> {
  const { data } = await clienteProcesos.get<TareaProceso[]>("/tareas", {
    params: estados?.length ? { estados: estados.join(",") } : undefined,
  });
  return data;
}

export const INDICADOR_CUELLOS = "tareasCompletadas";

function ventana(dias: number): { desde: string; hasta: string } {
  const hasta = new Date();
  const desde = new Date(hasta.getTime() - dias * 24 * 60 * 60 * 1000);
  return { desde: desde.toISOString(), hasta: hasta.toISOString() };
}

export async function resumenKpiProcesos(dias: number): Promise<KpiProcesoResumen> {
  const { data } = await clienteProcesos.get<KpiProcesoResumen>("/kpi-procesos", {
    params: ventana(dias),
  });
  return data;
}

export async function poblacionKpiProcesos(
  indicador: string,
  dias: number,
): Promise<KpiProcesoPoblacion[]> {
  const { data } = await clienteProcesos.get<KpiProcesoPoblacion[]>(
    "/kpi-procesos/poblacion",
    { params: { indicador, ...ventana(dias) } },
  );
  return data;
}

export async function listarReglas(plantillaId?: string): Promise<ReglaSupervisora[]> {
  const { data } = await clienteProcesos.get<ReglaSupervisora[]>("/supervisora/reglas", {
    params: plantillaId ? { plantillaId } : undefined,
  });
  return data;
}

export async function crearRegla(regla: CambiosRegla): Promise<ReglaSupervisora> {
  const { data } = await clienteProcesos.post<ReglaSupervisora>("/supervisora/reglas", regla);
  return data;
}

export async function actualizarRegla(
  reglaId: string,
  cambios: CambiosRegla,
): Promise<ReglaSupervisora> {
  const { data } = await clienteProcesos.put<ReglaSupervisora>(
    `/supervisora/reglas/${reglaId}`,
    cambios,
  );
  return data;
}

export async function darDeBajaRegla(reglaId: string): Promise<void> {
  await clienteProcesos.delete(`/supervisora/reglas/${reglaId}`);
}

export async function listarHallazgos(instanciaId: string): Promise<HallazgoProceso[]> {
  const { data } = await clienteProcesos.get<HallazgoProceso[]>(
    `/supervisora/instancias/${instanciaId}/hallazgos`,
  );
  return data;
}

export async function resolverHallazgo(
  hallazgoId: string,
  estado: "APROBADO" | "RECHAZADO",
): Promise<HallazgoProceso> {
  const { data } = await clienteProcesos.put<HallazgoProceso>(
    `/supervisora/hallazgos/${hallazgoId}`,
    estado,
    { headers: { "Content-Type": "text/plain" } },
  );
  return data;
}

export async function completarTarea(
  tareaId: string,
  cuerpo: {
    actor: string;
    decision?: string;
    motivo?: string;
    datos?: Record<string, unknown>;
  },
): Promise<TareaProceso> {
  const { data } = await clienteProcesos.post<TareaProceso>(`/tareas/${tareaId}/completar`, cuerpo);
  return data;
}
