export type EstadoDocumento =
  | "RECIBIDO"
  | "PROCESANDO"
  | "EXTRAIDO"
  | "VALIDADO"
  | "OBSERVADO"
  | "APROBADO"
  | "RECHAZADO"
  | "CERRADO"
  | "DIVIDIDO";

export type PresenciaCampo = "PRESENTE" | "NO_FIGURA" | "ILEGIBLE";

export type SeveridadHallazgo = "INFORMATIVO" | "ADVERTENCIA" | "REQUIERE_REVISION" | "BLOQUEANTE";

export type EstadoExcepcion = "ABIERTA" | "EN_CURSO" | "RESUELTA" | "DESCARTADA";

export type PrioridadExcepcion = "BAJA" | "MEDIA" | "ALTA" | "CRITICA";

export type DecisionRevision = "APROBAR" | "RECHAZAR" | "OBSERVAR" | "CORREGIR" | "REPROCESAR";

export interface Sesion {
  tokenAcceso: string;
  tokenRefresco: string;
  usuarioId: string;
  email: string;
  nombre: string;
  tenantId: string;
  codigoTenant: string;
  nombreTenant: string;
  permisos: string[];
}

export interface ArchivoDocumento {
  id: string;
  nombreArchivo: string;
  tipoMime: string;
  extension?: string;
  tamano: number;
  checksum?: string;
  paginas: number;
  version: number;
  original: boolean;
  resultadoEscaneo?: string;
  amenazaDetectada?: string;
  motorEscaneo?: string;
  enCuarentena?: boolean;
  alta?: string;
}

export interface Documento {
  id: string;
  estado: EstadoDocumento;
  origen: string;
  nombre?: string;
  hashContenido?: string;
  correlacionId?: string;
  codigoPlantilla?: string;
  nombrePlantilla?: string;
  versionPlantillaId?: string;
  numeroVersionPlantilla?: number;
  documentoPadreId?: string;
  paginaDesde?: number;
  paginaHasta?: number;
  cantidadSegmentos?: number;
  sujetoOrigen?: string;
  sujetoTipoObjeto?: string;
  sujetoIdObjeto?: string;
  remitente?: string;
  observacion?: string;
  ingresadoPor?: string;
  retencionLegal?: boolean;
  recibido?: string;
  procesado?: string;
  cerrado?: string;
  alta?: string;
  archivos: ArchivoDocumento[];
  transicionesPosibles: EstadoDocumento[];
}

export interface ValorExtraido {
  id: string;
  claveCampo: string;
  etiqueta?: string;
  valorCrudo?: string;
  valorNormalizado?: string;
  presencia: PresenciaCampo;
  confianza?: number;
  confianzaProveedor?: number;
  evidenciaPagina?: number;
  corregidoManualmente?: boolean;
  valorAnterior?: string;
}

export interface EjecucionExtraccion {
  id: string;
  proveedor: string;
  modelo?: string;
  versionPrompt?: string;
  versionEsquema?: string;
  estado: string;
  intento: number;
  tokensEntrada: number;
  tokensSalida: number;
  paginasProcesadas: number;
  costo?: number;
  duracionMilisegundos: number;
  codigoError?: string;
  mensajeError?: string;
  inicio?: string;
  fin?: string;
  valores: ValorExtraido[];
}

export interface HallazgoValidacion {
  id: string;
  codigoRegla?: string;
  claveCampo?: string;
  severidad: SeveridadHallazgo;
  mensaje?: string;
  sobreescrito?: boolean;
  motivoSobreescritura?: string;
  sobreescritoPor?: string;
}

export interface EjecucionValidacion {
  id: string;
  estado: string;
  resultado?: string;
  cantidadHallazgos: number;
  cantidadBloqueantes: number;
  autoaprobado: boolean;
  motivoResultado?: string;
  hallazgos: HallazgoValidacion[];
}

export interface CandidatoAsociacion {
  id: string;
  conector: string;
  origen?: string;
  tipoObjeto?: string;
  idObjeto?: string;
  descripcion?: string;
  puntaje?: number;
  razones?: string;
  seleccionado: boolean;
  descartado?: boolean;
  motivoSeleccion?: string;
}

export interface RevisionDocumento {
  id: string;
  actor?: string;
  decision: DecisionRevision;
  estadoAnterior?: EstadoDocumento;
  estadoNuevo?: EstadoDocumento;
  motivo?: string;
  cantidadCorrecciones: number;
  alta?: string;
  cambios: { claveCampo: string; valorAnterior?: string; valorNuevo?: string; motivo?: string }[];
}

export interface OriginalFisico {
  id: string;
  documentoId?: string;
  estado: string;
  politica?: string;
  bloqueaCierre: boolean;
  pendiente: boolean;
  ubicacion?: string;
  referenciaFisica?: string;
  recibidoPor?: string;
  transicionesPosibles: string[];
}

export interface DetalleDocumento {
  documento: Documento;
  extraccion?: EjecucionExtraccion | null;
  validacion?: EjecucionValidacion | null;
  candidatos: CandidatoAsociacion[];
  revisiones: RevisionDocumento[];
  segmentos: Documento[];
  originalFisico?: OriginalFisico | null;
}

export interface Excepcion {
  id: string;
  documentoId?: string;
  nombreDocumento?: string;
  tipo: string;
  severidad: SeveridadHallazgo;
  prioridad: PrioridadExcepcion;
  estado: EstadoExcepcion;
  codigo?: string;
  detalle?: string;
  responsable?: string;
  resueltaPor?: string;
  resolucion?: string;
  vencida?: boolean;
  venceEn?: string;
  alta?: string;
}

export type UnidadKpi = "CONTEO" | "PORCENTAJE" | "HORAS";

export type TendenciaKpi = "SUBE" | "BAJA" | "ESTABLE" | "SIN_COMPARACION";

export type SemaforoKpi = "VERDE" | "AMBAR" | "ROJO" | "SIN_DATOS";

export type SaludPlantilla = "OK" | "ATENCION" | "CRITICO" | "SIN_DATOS";

export interface RangoKpiResuelto {
  desde: string;
  hasta: string;
  dias: number;
}

export interface IndicadorKpi {
  clave: string;
  etiqueta: string;
  unidad: UnidadKpi;
  valor?: number | null;
  valorAnterior?: number | null;
  variacion?: number | null;
  tendencia: TendenciaKpi;
  numerador?: number | null;
  denominador?: number | null;
  formula: string;
  detalle?: string | null;
  tienePoblacion: boolean;
}

export interface KpiResumen {
  rango: RangoKpiResuelto;
  indicadores: IndicadorKpi[];
  porEstado: Record<string, number>;
}

export interface BarraKpi {
  clave: string;
  etiqueta: string;
  valor?: number | null;
  porcentaje?: number | null;
  semaforo: SemaforoKpi;
  formula: string;
}

export interface KpiPlantilla {
  codigo: string;
  nombre?: string;
  volumen: number;
  documentosConExcepciones: number;
  salud: SaludPlantilla;
  porEstado: Record<string, number>;
  barras: BarraKpi[];
}

export interface Pagina<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ErrorApi {
  mensaje: string;
  ruta?: string;
  estado?: string;
  codigo?: number;
  correlacionId?: string;
  campos?: Record<string, string>;
}

export type EstadoBuzonCorreo = "ACTIVO" | "PAUSADO" | "ERROR";

export type ResultadoMensajeCorreo =
  | "INGESTADO"
  | "PARCIAL"
  | "SIN_ADJUNTOS"
  | "REMITENTE_NO_AUTORIZADO"
  | "SIN_CORRELACION"
  | "RECHAZADO"
  | "ERROR";

export type ResultadoAdjuntoCorreo = "INGESTADO" | "EN_CUARENTENA" | "RECHAZADO" | "ERROR";

export type EstadoMensajeSaliente = "PENDIENTE" | "ENVIADO" | "FALLIDO";

export interface RemitenteAutorizado {
  id: string;
  patron: string;
  descripcion?: string;
  alta?: string;
}

export interface BuzonCorreo {
  id: string;
  direccion: string;
  nombre: string;
  estado: EstadoBuzonCorreo;
  hostEntrada: string;
  puertoEntrada: number;
  usuarioEntrada: string;
  carpeta: string;
  entradaSegura: boolean;
  hostSalida?: string;
  puertoSalida: number;
  salidaSegura: boolean;
  puedeResponder: boolean;
  codigoPlantillaPorDefecto?: string;
  exigirRemitenteAutorizado: boolean;
  exigirCorrelacion: boolean;
  acusarRecibo: boolean;
  maximoAdjuntosPorMensaje: number;
  ultimaLectura?: string;
  ultimoError?: string;
  fallosConsecutivos: number;
  alta?: string;
  remitentes: RemitenteAutorizado[];
}

export interface NuevoBuzonCorreo {
  direccion: string;
  nombre: string;
  hostEntrada: string;
  puertoEntrada: number;
  usuarioEntrada: string;
  referenciaSecretoEntrada: string;
  carpeta?: string;
  entradaSegura: boolean;
  hostSalida?: string;
  puertoSalida: number;
  usuarioSalida?: string;
  referenciaSecretoSalida?: string;
  salidaSegura: boolean;
  codigoPlantillaPorDefecto?: string;
  exigirRemitenteAutorizado: boolean;
  exigirCorrelacion: boolean;
  acusarRecibo: boolean;
  maximoAdjuntosPorMensaje: number;
}

export interface CorrelacionCorreo {
  id: string;
  token: string;
  etiquetaAsunto: string;
  direccionConEtiqueta?: string;
  sujetoOrigen?: string;
  sujetoTipoObjeto?: string;
  sujetoIdObjeto?: string;
  codigoPlantilla?: string;
  destinatario?: string;
  descripcion?: string;
  venceEn?: string;
  vigente: boolean;
  documentosRecibidos: number;
  ultimoUso?: string;
  alta?: string;
  mensajeSalienteId?: string;
}

export interface NuevaCorrelacionCorreo {
  buzonId: string;
  sujetoOrigen: string;
  sujetoTipoObjeto: string;
  sujetoIdObjeto: string;
  codigoPlantilla?: string;
  destinatario?: string;
  descripcion?: string;
  diasVigencia: number;
  enviarSolicitud: boolean;
}

export interface AdjuntoCorreo {
  id: string;
  documentoId?: string;
  nombreArchivo: string;
  tipoMime?: string;
  tamanoBytes: number;
  sha256?: string;
  resultado: ResultadoAdjuntoCorreo;
  codigoRechazo?: string;
  motivo?: string;
}

export interface MensajeCorreo {
  id: string;
  buzonId?: string;
  buzonDireccion?: string;
  identificadorMensaje: string;
  remitente?: string;
  destinatarios?: string;
  asunto?: string;
  tokenDetectado?: string;
  correlacionId?: string;
  resultado: ResultadoMensajeCorreo;
  motivo?: string;
  adjuntos: number;
  ingestados: number;
  rechazados: number;
  enviadoEn?: string;
  alta?: string;
  detalleAdjuntos: AdjuntoCorreo[];
}

export interface MensajeSaliente {
  id: string;
  buzonId?: string;
  correlacionId?: string;
  plantilla: string;
  destinatarios: string;
  asunto?: string;
  identificadorMensaje?: string;
  estado: EstadoMensajeSaliente;
  detalleError?: string;
  enviado?: string;
  alta?: string;
}

export interface LecturaBuzon {
  buzonId: string;
  direccion: string;
  mensajesLeidos: number;
  mensajesProcesados: number;
  documentosIngestados: number;
  error?: string;
  mensajes: MensajeCorreo[];
}
