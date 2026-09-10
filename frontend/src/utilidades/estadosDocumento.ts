import type { EstadoDocumento } from "../tipos/api";
import type { Tono } from "../componentes/Interfaz";

interface PresentacionEstadoDocumento {
  readonly etiqueta: string;
  readonly tono: Tono;
  readonly color: `var(--color-${string})`;
}

export const ESTADOS_DOCUMENTALES = {
  RECIBIDO: {
    etiqueta: "RECIBIDO",
    tono: "neutro",
    color: "var(--color-tinta-suave)",
  },
  PROCESANDO: {
    etiqueta: "PROCESANDO",
    tono: "informacion",
    color: "var(--color-informacion)",
  },
  EXTRAIDO: {
    etiqueta: "EXTRAIDO",
    tono: "informacion",
    color: "var(--color-informacion)",
  },
  VALIDADO: {
    etiqueta: "VALIDADO",
    tono: "violeta",
    color: "var(--color-violeta)",
  },
  OBSERVADO: {
    etiqueta: "OBSERVADO",
    tono: "alerta",
    color: "var(--color-alerta)",
  },
  APROBADO: {
    etiqueta: "APROBADO",
    tono: "exito",
    color: "var(--color-exito)",
  },
  RECHAZADO: {
    etiqueta: "RECHAZADO",
    tono: "rojo",
    color: "var(--color-rojo)",
  },
  DIVIDIDO: {
    etiqueta: "DIVIDIDO",
    tono: "neutro",
    color: "var(--color-tinta-suave)",
  },
  CERRADO: {
    etiqueta: "CERRADO",
    tono: "neutro",
    color: "var(--color-tinta-suave)",
  },
} as const satisfies Readonly<
  Record<EstadoDocumento, PresentacionEstadoDocumento>
>;
