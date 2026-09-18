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
  | "SUBPROCESO"
  | "PARALELO"
  | "UNION"
  | "FIRMA";

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

export interface GeneracionProcesoReq {
  descripcion?: string;
  contenidoBase64?: string;
  tipoMime?: string;
  nombreArchivo?: string;
}

export interface GeneracionProcesoRes {
  definicion: Proceso;
  advertencias: string[];
  intentos: number;
}

export async function generarProcesoConIa(
  requerimiento: GeneracionProcesoReq,
): Promise<GeneracionProcesoRes> {
  const { data } = await clienteProcesos.post<GeneracionProcesoRes>(
    "/procesos/generar",
    requerimiento,
  );
  return data;
}

export async function refinarProcesoConIa(
  definicionId: string,
  requerimiento: GeneracionProcesoReq,
): Promise<GeneracionProcesoRes> {
  const { data } = await clienteProcesos.post<GeneracionProcesoRes>(
    `/procesos/${definicionId}/refinar`,
    requerimiento,
  );
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

export interface SimulacionPaso {
  nodoId: string;
  nombre?: string;
  tipo?: string;
  accion?: string;
  detalle?: string;
}

export interface SimulacionResultado {
  pasos: SimulacionPaso[];
  terminada: boolean;
  advertencias: string[];
}

export async function simularVersion(
  versionId: string,
  datos: Record<string, unknown>,
): Promise<SimulacionResultado> {
  const { data } = await clienteProcesos.post<SimulacionResultado>(
    `/procesos/versiones/${versionId}/simular`,
    { datos },
  );
  return data;
}

export async function dispararEvento(
  evento: string,
  datos?: Record<string, unknown>,
): Promise<InstanciaProceso[]> {
  const { data } = await clienteProcesos.post<InstanciaProceso[]>(
    `/procesos/eventos/${encodeURIComponent(evento)}`,
    datos ?? {},
  );
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

export interface EnlaceExternoPublico {
  estado?: string;
  expiracion?: string;
  actor?: string;
  nombrePaso?: string;
  tipoNodo?: string;
  nombreProceso?: string;
  codigoProceso?: string;
  vencimientoTarea?: string;
  estadoTarea?: string;
  decision?: string;
  motivo?: string;
  completada?: string;
  documentosEsperados?: string[];
  documentosCargados?: string[];
}

export interface DocumentoExternoCargado {
  id?: string;
  nombre?: string;
  estado?: string;
}

export async function obtenerEnlaceExterno(token: string): Promise<EnlaceExternoPublico> {
  const { data } = await clienteProcesos.get<EnlaceExternoPublico>(
    `/colaboracion-externa/enlaces/${token}`,
  );
  return data;
}

export async function usarEnlaceExterno(
  token: string,
  cuerpo: { decision?: string; motivo?: string; datos?: Record<string, unknown> },
): Promise<TareaProceso> {
  const { data } = await clienteProcesos.post<TareaProceso>(
    `/colaboracion-externa/enlaces/${token}`,
    cuerpo,
  );
  return data;
}

export async function subirDocumentoEnlaceExterno(
  token: string,
  archivo: File,
): Promise<DocumentoExternoCargado> {
  const formulario = new FormData();
  formulario.append("archivo", archivo);
  const { data } = await clienteProcesos.post<DocumentoExternoCargado>(
    `/colaboracion-externa/enlaces/${token}/documentos`,
    formulario,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data;
}

export interface EnlaceExterno {
  id: string;
  token: string;
  tareaId: string;
  actor?: string;
  scopes?: string[];
  expiracion?: string;
  usosMaximos?: number;
  usos?: number;
  estado?: string;
  alta?: string;
}

export async function listarEnlacesExternos(): Promise<EnlaceExterno[]> {
  const { data } = await clienteProcesos.get<EnlaceExterno[]>(
    "/colaboracion-externa/enlaces",
  );
  return data;
}

export async function revocarEnlaceExterno(enlaceId: string): Promise<void> {
  await clienteProcesos.delete(`/colaboracion-externa/enlaces/${enlaceId}`);
}

export interface OrganizacionPartner {
  id: string;
  codigo: string;
  nombre: string;
  emailContacto?: string;
  estado?: string;
  alta?: string;
  delegaciones?: DelegacionAcceso[];
}

export interface NuevaOrganizacionPartner {
  codigo: string;
  nombre: string;
  emailContacto?: string;
}

export interface DelegacionAcceso {
  id: string;
  partnerId: string;
  tenantClienteId: string;
  scopes?: string[];
  expiracion?: string;
  aprobador?: string;
  alta?: string;
}

export interface NuevaDelegacionAcceso {
  partnerId: string;
  tenantClienteId: string;
  scopes?: string[];
  expiracion?: string;
  aprobador?: string;
}

export async function listarOrganizacionesPartner(): Promise<OrganizacionPartner[]> {
  const { data } = await clienteProcesos.get<OrganizacionPartner[]>(
    "/partners/organizaciones",
  );
  return data;
}

export async function crearOrganizacionPartner(
  requerimiento: NuevaOrganizacionPartner,
): Promise<OrganizacionPartner> {
  const { data } = await clienteProcesos.post<OrganizacionPartner>(
    "/partners/organizaciones",
    requerimiento,
  );
  return data;
}

export async function obtenerOrganizacionPartner(
  organizacionId: string,
): Promise<OrganizacionPartner> {
  const { data } = await clienteProcesos.get<OrganizacionPartner>(
    `/partners/organizaciones/${organizacionId}`,
  );
  return data;
}

export async function desactivarOrganizacionPartner(
  organizacionId: string,
): Promise<void> {
  await clienteProcesos.delete(`/partners/organizaciones/${organizacionId}`);
}

export async function listarDelegaciones(): Promise<DelegacionAcceso[]> {
  const { data } = await clienteProcesos.get<DelegacionAcceso[]>(
    "/partners/delegaciones",
  );
  return data;
}

export async function crearDelegacion(
  requerimiento: NuevaDelegacionAcceso,
): Promise<DelegacionAcceso> {
  const { data } = await clienteProcesos.post<DelegacionAcceso>(
    "/partners/delegaciones",
    requerimiento,
  );
  return data;
}

export async function revocarDelegacion(delegacionId: string): Promise<void> {
  await clienteProcesos.delete(`/partners/delegaciones/${delegacionId}`);
}

export interface PublicacionMarketplace {
  id: string;
  definicionId: string;
  tenantId: string;
  categoria?: string;
  comercial?: {
    nombreProceso?: string;
    codigoProceso?: string;
    descripcionProceso?: string;
    versionNumero?: number;
    nodosTotal?: number;
    tiposNodo?: string[];
    precio?: string;
    contacto?: string;
    publicadorNombre?: string;
  };
  alta?: string;
}

export interface NuevaPublicacionMarketplace {
  versionId: string;
  categoria?: string;
  comercial?: Record<string, unknown>;
}

export interface SobreescrituraPlantilla {
  id: string;
  instalacionId: string;
  nodoId: string;
  datos?: Record<string, unknown>;
  alta?: string;
}

export interface NuevaSobreescrituraPlantilla {
  instalacionId?: string;
  nodoId: string;
  datos?: Record<string, unknown>;
}

export interface InstalacionMarketplace {
  id: string;
  definicionId: string;
  versionId: string;
  publicadorTenantId: string;
  definicionLocalId?: string;
  pin?: boolean;
  politicaActualizacion?: string;
  alta?: string;
  sobreescrituras?: SobreescrituraPlantilla[];
}

export interface NuevaInstalacionMarketplace {
  publicacionId: string;
  pin?: boolean;
  politicaActualizacion?: string;
  sobreescrituras?: NuevaSobreescrituraPlantilla[];
}

export async function publicarPlantilla(
  requerimiento: NuevaPublicacionMarketplace,
): Promise<PublicacionMarketplace> {
  const { data } = await clienteProcesos.post<PublicacionMarketplace>(
    "/marketplace/publicaciones",
    requerimiento,
  );
  return data;
}

export async function despublicarPlantilla(publicacionId: string): Promise<void> {
  await clienteProcesos.delete(`/marketplace/publicaciones/${publicacionId}`);
}

export async function listarCatalogoMarketplace(): Promise<PublicacionMarketplace[]> {
  const { data } = await clienteProcesos.get<PublicacionMarketplace[]>(
    "/marketplace/publicaciones",
  );
  return data;
}

export async function listarMisPublicaciones(): Promise<PublicacionMarketplace[]> {
  const { data } = await clienteProcesos.get<PublicacionMarketplace[]>(
    "/marketplace/publicaciones/mias",
  );
  return data;
}

export async function obtenerPublicacion(
  publicacionId: string,
): Promise<PublicacionMarketplace> {
  const { data } = await clienteProcesos.get<PublicacionMarketplace>(
    `/marketplace/publicaciones/${publicacionId}`,
  );
  return data;
}

export async function instalarPlantilla(
  requerimiento: NuevaInstalacionMarketplace,
): Promise<InstalacionMarketplace> {
  const { data } = await clienteProcesos.post<InstalacionMarketplace>(
    "/marketplace/instalaciones",
    requerimiento,
  );
  return data;
}

export async function listarInstalaciones(): Promise<InstalacionMarketplace[]> {
  const { data } = await clienteProcesos.get<InstalacionMarketplace[]>(
    "/marketplace/instalaciones",
  );
  return data;
}

export async function agregarSobreescritura(
  instalacionId: string,
  requerimiento: NuevaSobreescrituraPlantilla,
): Promise<SobreescrituraPlantilla> {
  const { data } = await clienteProcesos.post<SobreescrituraPlantilla>(
    `/marketplace/instalaciones/${instalacionId}/sobrescrituras`,
    requerimiento,
  );
  return data;
}

export async function listarSobreescrituras(
  instalacionId: string,
): Promise<SobreescrituraPlantilla[]> {
  const { data } = await clienteProcesos.get<SobreescrituraPlantilla[]>(
    `/marketplace/instalaciones/${instalacionId}/sobrescrituras`,
  );
  return data;
}
