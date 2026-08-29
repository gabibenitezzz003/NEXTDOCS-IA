import type { EstadoDocumento, PresenciaCampo, SeveridadHallazgo } from "../tipos/api";
import { Pastilla } from "./Interfaz";
import type { Tono } from "./Interfaz";

const TONO_ESTADO: Record<EstadoDocumento, Tono> = {
  RECIBIDO: "neutro",
  PROCESANDO: "violeta",
  EXTRAIDO: "violeta",
  VALIDADO: "informacion",
  OBSERVADO: "alerta",
  APROBADO: "exito",
  RECHAZADO: "rojo",
  CERRADO: "neutro",
  DIVIDIDO: "informacion",
};

const TONO_SEVERIDAD: Record<SeveridadHallazgo, Tono> = {
  INFORMATIVO: "neutro",
  ADVERTENCIA: "alerta",
  REQUIERE_REVISION: "alerta",
  BLOQUEANTE: "rojo",
};

const TONO_PRESENCIA: Record<PresenciaCampo, Tono> = {
  PRESENTE: "exito",
  NO_FIGURA: "neutro",
  ILEGIBLE: "rojo",
};

export function InsigniaEstado({ estado }: { estado: EstadoDocumento }) {
  return <Pastilla tono={TONO_ESTADO[estado] ?? "neutro"}>{estado}</Pastilla>;
}

export function InsigniaSeveridad({ severidad }: { severidad: SeveridadHallazgo }) {
  return <Pastilla tono={TONO_SEVERIDAD[severidad] ?? "neutro"}>{severidad.replace(/_/g, " ")}</Pastilla>;
}

export function InsigniaPresencia({ presencia }: { presencia: PresenciaCampo }) {
  return <Pastilla tono={TONO_PRESENCIA[presencia] ?? "neutro"}>{presencia.replace(/_/g, " ")}</Pastilla>;
}

export function BarraConfianza({ valor }: { valor?: number }) {
  if (valor === undefined || valor === null) {
    return <span className="text-xs text-tinta-tenue">sin dato</span>;
  }
  const porcentaje = Math.round(valor * 100);
  const color = porcentaje >= 90 ? "bg-exito" : porcentaje >= 70 ? "bg-alerta" : "bg-rojo";
  return (
    <div className="flex items-center gap-2">
      <div className="h-1.5 w-16 overflow-hidden rounded-full bg-lienzo ring-1 ring-inset ring-borde">
        <div className={`h-full rounded-full ${color}`} style={{ width: `${porcentaje}%` }} />
      </div>
      <span className="w-9 text-xs font-medium tabular-nums text-tinta-media">{porcentaje}%</span>
    </div>
  );
}
