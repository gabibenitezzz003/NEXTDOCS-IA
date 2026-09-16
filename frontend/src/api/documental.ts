import { cliente } from "./cliente";

export type EstadoDocumentoMotor =
  | "RECIBIDO"
  | "PROCESANDO"
  | "EXTRAIDO"
  | "VALIDADO"
  | "OBSERVADO"
  | "APROBADO"
  | "RECHAZADO"
  | "CERRADO";

export interface DocumentoMotor {
  id: string;
  estado: EstadoDocumentoMotor;
  origen: string;
  plantilla_codigo: string | null;
  familia: string | null;
  nombre_archivo: string;
  referencia_externa: string | null;
  tipo_mime: string | null;
  sujeto_tipo: string | null;
  sujeto_id: string | null;
  confianza: number | null;
  excepciones_abiertas?: number;
  creado_en: string;
}

export interface CampoValor {
  clave: string;
  valor_leido: string | null;
  valor_normalizado: unknown;
  confianza: number | null;
  pagina: number | null;
  recorte: number[] | null;
  texto_fuente: string | null;
  presencia: "PRESENTE" | "NO_FIGURA" | "ILEGIBLE" | null;
  estado: string | null;
}

export interface Hallazgo {
  codigo: string;
  severidad: string;
  detalle?: string;
  mensaje?: string;
}

export interface Candidato {
  id: string;
  tipo_objeto: string;
  objeto_id: string | null;
  referencia_externa: string | null;
  puntaje: number;
  metodo: string;
  estado: string;
  razones?: unknown;
}

export interface ItemExtraido {
  orden: number;
  [clave: string]: unknown;
}

export interface FichaDocumento {
  documento: DocumentoMotor & { archivo?: { clave_almacen: string } | null };
  valores: CampoValor[];
  validacion: { resultado?: string; hallazgos: Hallazgo[] } | null;
  candidatos: Candidato[];
  items: ItemExtraido[];
  hijos: { id: string; nombre_archivo: string; estado: string }[];
}

export interface EntradaBitacora {
  id: string;
  accion: string;
  tipo_actor: string;
  actor_id: string | null;
  origen: string;
  creado_en: string;
}

export interface ExcepcionMotor {
  id: string;
  documento_id: string;
  codigo_motivo: string;
  severidad: string;
  prioridad: number;
  motivos?: unknown;
  accion_sugerida?: string | null;
  estado: string;
  vence_en: string | null;
  creado_en: string;
  nombre_archivo: string;
  plantilla_codigo: string | null;
  estado_documento: string;
}

export interface PlantillaMotor {
  codigo: string;
  nombre: string;
  version: number;
  estado: string;
  umbral_auto_aprobacion?: number;
  familia?: string;
  definicion?: {
    familia?: string;
    umbralAutoAprobacion?: number;
    diasAvisoVencimiento?: number;
    campos?: CampoPlantilla[];
  };
}

export interface CampoPlantilla {
  clave: string;
  tipo?: string;
  requerido?: boolean;
  critico?: boolean;
  umbral?: number;
}

export interface PlantillaCatalogo {
  codigo: string;
  nombre: string;
  familia: string | null;
}

export interface EventoMotor {
  id: string;
  tipo_agregado: string;
  agregado_id: string;
  tipo_evento: string;
  estado?: string;
  intentos?: number;
  creado_en: string;
}

export interface VencimientoFila {
  id: string;
  nombre_archivo: string;
  plantilla_codigo: string | null;
  familia: string | null;
  estado: string;
  situacion: "VENCIDO" | "POR_VENCER" | "VIGENTE";
  vence_en: string | null;
  dias_restantes: number | null;
  sujeto_tipo: string | null;
  sujeto_id: string | null;
}

export interface Destinatario {
  id: string;
  nombre: string;
  correo: string;
  familias: string[];
  situaciones: string[];
}

export interface Envio {
  id: string;
  documento_id: string;
  nombre_archivo?: string;
  correo: string;
  estado: string;
  creado_en: string;
  enviado_en?: string | null;
  error?: string | null;
}

export interface BandejaRespuesta {
  documentos: DocumentoMotor[];
  cursor?: string | null;
}

export interface FiltrosBandeja {
  estado?: string;
  plantilla?: string;
  limite?: number;
  cursor?: string;
}

const sinVacios = (parametros: object) =>
  Object.fromEntries(
    Object.entries(parametros).filter(
      ([, valor]) => valor !== undefined && valor !== null && valor !== "",
    ),
  );

export async function obtenerEstadoDocumental(): Promise<{
  habilitado: boolean;
}> {
  const { data } = await cliente.get<{ habilitado: boolean }>(
    "/documental/estado",
  );
  return data;
}

export async function obtenerBandeja(
  filtros: FiltrosBandeja,
): Promise<BandejaRespuesta> {
  const { data } = await cliente.get<BandejaRespuesta>(
    "/documental/documentos",
    {
      params: sinVacios(filtros),
    },
  );
  return data;
}

export async function obtenerResumenDocumental(): Promise<
  Record<string, number>
> {
  const { data } = await cliente.get<{ porEstado: Record<string, number> }>(
    "/documental/documentos/resumen",
  );
  return data?.porEstado || {};
}

export async function obtenerFicha(
  documentoId: string,
): Promise<FichaDocumento> {
  const { data } = await cliente.get<FichaDocumento>(
    `/documental/documentos/${documentoId}`,
  );
  return data;
}

export async function obtenerOriginal(
  documentoId: string,
): Promise<{ url: string; expiraEnSegundos: number }> {
  const { data } = await cliente.get<{ url: string; expiraEnSegundos: number }>(
    `/documental/documentos/${documentoId}/original`,
  );
  return data;
}

export async function obtenerBitacora(
  documentoId: string,
): Promise<EntradaBitacora[]> {
  const { data } = await cliente.get<{ entradas: EntradaBitacora[] }>(
    `/documental/documentos/${documentoId}/bitacora`,
  );
  return data?.entradas || [];
}

export async function cargarDocumento(documento: {
  origen: string;
  nombreArchivo: string;
  tipoMime?: string;
  contenidoBase64: string;
  referenciaExterna?: string;
}): Promise<{ motivo?: string; documento?: { id: string } }> {
  const { data } = await cliente.post<{
    motivo?: string;
    documento?: { id: string };
  }>("/documental/documentos", documento);
  return data;
}

export async function revisarDocumento(
  documentoId: string,
  revision: {
    decision: string;
    motivo?: string | null;
    claveCampo?: string;
    valor?: string;
  },
): Promise<void> {
  await cliente.post(
    `/documental/documentos/${documentoId}/revisiones`,
    revision,
  );
}

export async function confirmarEmparejamiento(
  documentoId: string,
  candidatoId: string,
  motivo?: string | null,
): Promise<void> {
  await cliente.post(`/documental/documentos/${documentoId}/emparejamiento`, {
    candidatoId,
    motivo: motivo ?? null,
  });
}

export async function reprocesarDocumento(documentoId: string): Promise<void> {
  await cliente.post(`/documental/documentos/${documentoId}/reprocesar`);
}

export async function obtenerExcepcionesMotor(filtros: {
  estado?: string;
  limite?: number;
}): Promise<ExcepcionMotor[]> {
  const { data } = await cliente.get<{ excepciones: ExcepcionMotor[] }>(
    "/documental/excepciones",
    { params: sinVacios(filtros) },
  );
  return data?.excepciones || [];
}

export async function resolverExcepcion(
  excepcionId: string,
  resolucion: { decision: string; resolucion: string },
): Promise<void> {
  await cliente.post(
    `/documental/excepciones/${excepcionId}/resolver`,
    resolucion,
  );
}

export async function obtenerPlantillasMotor(): Promise<PlantillaMotor[]> {
  const { data } = await cliente.get<{ plantillas: PlantillaMotor[] }>(
    "/documental/plantillas",
  );
  return data?.plantillas || [];
}

export async function obtenerCatalogoPlantillas(): Promise<
  PlantillaCatalogo[]
> {
  const { data } = await cliente.get<{
    disponibles?: PlantillaCatalogo[];
    plantillas?: PlantillaCatalogo[];
  }>("/documental/plantillas/catalogo");
  return data?.disponibles || data?.plantillas || [];
}

export async function ajustarPlantilla(
  codigo: string,
  ajuste: {
    umbralAutoAprobacion: number;
    diasAvisoVencimiento: number;
    campos: {
      clave: string;
      requerido: boolean;
      critico: boolean;
      umbral: number;
    }[];
  },
): Promise<void> {
  await cliente.patch(`/documental/plantillas/${codigo}`, ajuste);
}

export async function cambiarEstadoPlantilla(
  codigo: string,
  estado: string,
): Promise<void> {
  await cliente.put(`/documental/plantillas/${codigo}/estado`, { estado });
}

export async function instalarPlantilla(codigo: string): Promise<void> {
  await cliente.post(`/documental/plantillas/${codigo}/instalar`);
}

export async function obtenerEventos(filtros: {
  estado?: string;
  limite?: number;
}): Promise<EventoMotor[]> {
  const { data } = await cliente.get<{ eventos: EventoMotor[] }>(
    "/documental/eventos",
    {
      params: sinVacios(filtros),
    },
  );
  return data?.eventos || [];
}

export async function obtenerVencimientos(filtros: {
  familia?: string;
  dias?: number;
  situacion?: string;
  limite?: number;
}): Promise<{
  resumen: { VENCIDO: number; POR_VENCER: number; VIGENTE: number };
  documentos: VencimientoFila[];
}> {
  const { data } = await cliente.get<{
    resumen: { VENCIDO: number; POR_VENCER: number; VIGENTE: number };
    documentos: VencimientoFila[];
  }>("/documental/vencimientos", { params: sinVacios(filtros) });
  return data;
}

export async function obtenerDestinatarios(): Promise<Destinatario[]> {
  const { data } = await cliente.get<{ destinatarios: Destinatario[] }>(
    "/documental/destinatarios",
  );
  return data?.destinatarios || [];
}

export async function guardarDestinatario(destinatario: {
  nombre: string;
  correo: string;
  familias: string[];
  situaciones: string[];
}): Promise<void> {
  await cliente.post("/documental/destinatarios", destinatario);
}

export async function borrarDestinatario(
  destinatarioId: string,
): Promise<void> {
  await cliente.delete(`/documental/destinatarios/${destinatarioId}`);
}

export async function obtenerEnvios(filtros: {
  estado?: string;
  limite?: number;
}): Promise<Envio[]> {
  const { data } = await cliente.get<{ envios: Envio[] }>(
    "/documental/envios",
    {
      params: sinVacios(filtros),
    },
  );
  return data?.envios || [];
}

export async function enviarDocumento(
  documentoId: string,
  correo: string,
): Promise<void> {
  await cliente.post(`/documental/documentos/${documentoId}/enviar`, {
    correo,
  });
}
