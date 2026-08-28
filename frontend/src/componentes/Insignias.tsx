import type { EstadoDocumento, PresenciaCampo, SeveridadHallazgo } from "../tipos/api";

const ESTILOS_ESTADO: Record<EstadoDocumento, string> = {
  RECIBIDO: "bg-slate-100 text-slate-700 ring-slate-200",
  PROCESANDO: "bg-violeta-tenue text-violeta ring-violeta/20",
  EXTRAIDO: "bg-violeta-tenue text-violeta ring-violeta/20",
  VALIDADO: "bg-sky-50 text-sky-700 ring-sky-200",
  OBSERVADO: "bg-alerta-tenue text-alerta ring-alerta/25",
  APROBADO: "bg-exito-tenue text-exito ring-exito/25",
  RECHAZADO: "bg-rojo-tenue text-rojo ring-rojo/25",
  CERRADO: "bg-slate-100 text-slate-500 ring-slate-200",
  DIVIDIDO: "bg-arena text-tinta ring-black/10",
};

const ESTILOS_SEVERIDAD: Record<SeveridadHallazgo, string> = {
  INFORMATIVO: "bg-slate-100 text-slate-700 ring-slate-200",
  ADVERTENCIA: "bg-alerta-tenue text-alerta ring-alerta/25",
  REQUIERE_REVISION: "bg-alerta-tenue text-alerta ring-alerta/25",
  BLOQUEANTE: "bg-rojo-tenue text-rojo ring-rojo/25",
};

const ESTILOS_PRESENCIA: Record<PresenciaCampo, string> = {
  PRESENTE: "bg-exito-tenue text-exito ring-exito/25",
  NO_FIGURA: "bg-slate-100 text-slate-600 ring-slate-200",
  ILEGIBLE: "bg-rojo-tenue text-rojo ring-rojo/25",
};

function base(clases: string) {
  return `inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset ${clases}`;
}

export function InsigniaEstado({ estado }: { estado: EstadoDocumento }) {
  return <span className={base(ESTILOS_ESTADO[estado] ?? ESTILOS_ESTADO.RECIBIDO)}>{estado}</span>;
}

export function InsigniaSeveridad({ severidad }: { severidad: SeveridadHallazgo }) {
  return <span className={base(ESTILOS_SEVERIDAD[severidad])}>{severidad.replace(/_/g, " ")}</span>;
}

export function InsigniaPresencia({ presencia }: { presencia: PresenciaCampo }) {
  return <span className={base(ESTILOS_PRESENCIA[presencia])}>{presencia.replace(/_/g, " ")}</span>;
}

export function BarraConfianza({ valor }: { valor?: number }) {
  if (valor === undefined || valor === null) {
    return <span className="text-xs text-tinta-suave">sin dato</span>;
  }
  const porcentaje = Math.round(valor * 100);
  const color = porcentaje >= 90 ? "bg-exito" : porcentaje >= 70 ? "bg-alerta" : "bg-rojo";
  return (
    <div className="flex items-center gap-2">
      <div className="h-1.5 w-16 overflow-hidden rounded-full bg-borde">
        <div className={`h-full ${color}`} style={{ width: `${porcentaje}%` }} />
      </div>
      <span className="w-9 text-xs tabular-nums text-tinta-suave">{porcentaje}%</span>
    </div>
  );
}
