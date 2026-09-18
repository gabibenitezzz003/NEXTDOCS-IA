import type {
  CampoValor,
  EstadoDocumentoMotor,
  PlantillaCatalogo,
} from "../api/documental";

export const ESTADOS_DOCUMENTO: EstadoDocumentoMotor[] = [
  "RECIBIDO",
  "PROCESANDO",
  "EXTRAIDO",
  "VALIDADO",
  "OBSERVADO",
  "APROBADO",
  "RECHAZADO",
  "DIVIDIDO",
  "ELIMINADO",
  "CERRADO",
];

export const ESTADOS_EN_CURSO: EstadoDocumentoMotor[] = [
  "RECIBIDO",
  "PROCESANDO",
  "EXTRAIDO",
  "VALIDADO",
];

export const ESTADOS_REPROCESABLES: EstadoDocumentoMotor[] = [
  "RECIBIDO",
  "PROCESANDO",
  "OBSERVADO",
  "DIVIDIDO",
];

export const ESTADOS_RECHAZABLES: EstadoDocumentoMotor[] = [
  "RECIBIDO",
  "PROCESANDO",
  "EXTRAIDO",
  "VALIDADO",
  "OBSERVADO",
  "DIVIDIDO",
];

export const ESTADOS_APROBABLES: EstadoDocumentoMotor[] = [
  "VALIDADO",
  "OBSERVADO",
];

export const FAMILIAS = [
  "FISCAL",
  "LOGISTICO",
  "IDENTIDAD",
  "VEHICULAR",
  "HABILITANTE",
  "COMERCIAL",
];

export const SITUACIONES_ENVIO = [
  "APROBADO",
  "OBSERVADO",
  "RECHAZADO",
  "VENCIDO",
  "POR_VENCER",
];

export const PLANTILLAS_CATALOGO: PlantillaCatalogo[] = [
  { codigo: "REMITO", nombre: "Remito conformado", familia: "LOGISTICO" },
  { codigo: "FACTURA", nombre: "Factura de compra", familia: "FISCAL" },
  { codigo: "NOTA_CREDITO", nombre: "Nota de credito", familia: "FISCAL" },
  { codigo: "NOTA_DEBITO", nombre: "Nota de debito", familia: "FISCAL" },
  {
    codigo: "DNI",
    nombre: "Documento nacional de identidad",
    familia: "IDENTIDAD",
  },
  {
    codigo: "LICENCIA_CONDUCIR",
    nombre: "Licencia nacional de conducir",
    familia: "IDENTIDAD",
  },
  {
    codigo: "VTV",
    nombre: "Verificacion tecnica vehicular",
    familia: "VEHICULAR",
  },
  {
    codigo: "RTO",
    nombre: "Revision tecnica obligatoria",
    familia: "VEHICULAR",
  },
  {
    codigo: "CEDULA_VEHICULAR",
    nombre: "Cedula de identificacion del vehiculo",
    familia: "VEHICULAR",
  },
  {
    codigo: "SEGURO_VEHICULAR",
    nombre: "Poliza de seguro del vehiculo",
    familia: "VEHICULAR",
  },
  {
    codigo: "CONSTANCIA_CUIT",
    nombre: "Constancia de inscripcion",
    familia: "FISCAL",
  },
];

const CODIGOS_CATALOGO = new Set(
  PLANTILLAS_CATALOGO.map((item) => item.codigo),
);

export function fusionarCatalogoPlantillas(
  remotos: PlantillaCatalogo[] | undefined,
): PlantillaCatalogo[] {
  const porCodigo = new Map<string, PlantillaCatalogo>();

  (remotos || []).forEach((item) => {
    if (!item?.codigo) return;
    porCodigo.set(item.codigo, {
      codigo: item.codigo,
      nombre: item.nombre || item.codigo,
      familia: item.familia || null,
    });
  });

  const base = PLANTILLAS_CATALOGO.map(
    (item) => porCodigo.get(item.codigo) || item,
  );
  const extras = [...porCodigo.values()].filter(
    (item) => !CODIGOS_CATALOGO.has(item.codigo),
  );

  return extras.length ? [...base, ...extras] : base;
}

export type Presencia = "PRESENTE" | "NO_FIGURA" | "ILEGIBLE";

export function presenciaDeCampo(campo: CampoValor): Presencia {
  if (campo?.presencia) return campo.presencia;
  if (
    campo?.valor_normalizado === null ||
    campo?.valor_normalizado === undefined
  ) {
    return "NO_FIGURA";
  }
  return "PRESENTE";
}

export function enPorcentaje(valor: unknown): string {
  const numero = Number(valor);
  if (!Number.isFinite(numero)) return "-";
  return `${Math.round(numero * 100)}%`;
}

export function comoFecha(
  valor: string | null | undefined,
  idioma = "es",
): string {
  if (!valor) return "-";
  const fecha = new Date(valor);
  if (Number.isNaN(fecha.getTime())) return "-";
  return fecha.toLocaleString(idioma, {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function horasRestantes(
  vencimiento: string | null | undefined,
): number | null {
  if (!vencimiento) return null;
  const fecha = new Date(vencimiento);
  if (Number.isNaN(fecha.getTime())) return null;
  return (fecha.getTime() - Date.now()) / 3600000;
}

export function textoDeValor(valor: unknown): string {
  if (valor === null || valor === undefined) return "-";
  if (typeof valor === "boolean") return valor ? "si" : "no";
  if (typeof valor === "object") return JSON.stringify(valor);
  return String(valor);
}

export function motivoHumano(
  codigo: string,
  t: (ruta: string, params?: Record<string, string | number>) => string,
): string {
  const traducido = t(`documental.motivo.${codigo}`);
  if (traducido !== `documental.motivo.${codigo}`) return traducido;
  return codigo
    .toLowerCase()
    .split("_")
    .map((palabra) => palabra.charAt(0).toUpperCase() + palabra.slice(1))
    .join(" ");
}

export function leerBase64(archivo: File): Promise<string> {
  return new Promise((resolver, rechazar) => {
    const lector = new FileReader();
    lector.onload = () =>
      resolver(String(lector.result).split(",").pop() || "");
    lector.onerror = () => rechazar(lector.error);
    lector.readAsDataURL(archivo);
  });
}
