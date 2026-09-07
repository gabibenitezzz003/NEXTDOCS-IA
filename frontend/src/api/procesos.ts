import axios from "axios";
import { mensajeDeError } from "./cliente";

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
  versiones: VersionProceso[];
  alta?: string;
}

export interface TareaProceso {
  id: string;
  instanciaId: string;
  nodoId: string;
  tipoNodo: string;
  estado: "PENDIENTE" | "COMPLETADA" | "CANCELADA" | "VENCIDA";
  asignadoA?: string;
  decision?: string;
  datos?: Record<string, unknown>;
  vencimiento?: string;
  alta?: string;
  completada?: string;
  completadaPor?: string;
}

export interface InstanciaProceso {
  id: string;
  definicionId: string;
  codigoDefinicion: string;
  numeroVersion: number;
  estado: "ACTIVA" | "COMPLETADA" | "CANCELADA";
  tareas: TareaProceso[];
  alta?: string;
  fin?: string;
}

let tenantActual: string | null = null;

export const clienteProcesos = axios.create({
  baseURL: "/api/v1",
  headers: { "Content-Type": "application/json" },
});

clienteProcesos.interceptors.request.use((configuracion) => {
  if (tenantActual) {
    configuracion.headers["X-Tenant-Id"] = tenantActual;
  }
  return configuracion;
});

export function fijarTenantProcesos(tenantId: string | null) {
  tenantActual = tenantId;
}

export { mensajeDeError };

export interface NuevoProceso {
  codigo: string;
  familia: string;
  nombre: string;
  descripcion?: string;
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

export async function completarTarea(
  tareaId: string,
  cuerpo: { actor: string; decision?: string; motivo?: string },
): Promise<TareaProceso> {
  const { data } = await clienteProcesos.post<TareaProceso>(`/tareas/${tareaId}/completar`, cuerpo);
  return data;
}
