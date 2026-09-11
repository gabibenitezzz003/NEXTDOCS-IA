import type {
  EstadoDocumento,
  PresenciaCampo,
  SeveridadHallazgo,
} from "../tipos/api";
import { Pastilla, TONO_BARRA } from "./Interfaz";
import type { Tono } from "./Interfaz";
import { ESTADOS_DOCUMENTALES } from "../utilidades/estadosDocumento";

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
  const presentacion = ESTADOS_DOCUMENTALES[estado];
  return (
    <Pastilla tono={presentacion?.tono ?? "neutro"}>
      {presentacion?.etiqueta ?? estado}
    </Pastilla>
  );
}

export function InsigniaSeveridad({
  severidad,
}: {
  severidad: SeveridadHallazgo;
}) {
  return (
    <Pastilla tono={TONO_SEVERIDAD[severidad] ?? "neutro"}>
      {severidad.replace(/_/g, " ")}
    </Pastilla>
  );
}

export function InsigniaPresencia({
  presencia,
}: {
  presencia: PresenciaCampo;
}) {
  return (
    <Pastilla tono={TONO_PRESENCIA[presencia] ?? "neutro"}>
      {presencia.replace(/_/g, " ")}
    </Pastilla>
  );
}

export function BarraConfianza({ valor }: { valor?: number }) {
  if (valor === undefined || valor === null) {
    return <span className="text-pequeno text-tinta-suave">sin dato</span>;
  }
  const porcentaje = Math.round(valor * 100);
  const tono: Tono =
    porcentaje >= 90 ? "exito" : porcentaje >= 70 ? "alerta" : "rojo";
  return (
    <div className="flex items-center gap-espacio-2">
      <div
        role="meter"
        aria-label="Confianza de lectura"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={porcentaje}
        aria-valuetext={`${porcentaje}%`}
        className="h-espacio-2 w-espacio-16 overflow-hidden rounded-insignia bg-lienzo ring-1 ring-inset ring-borde"
      >
        <div
          className={`h-full rounded-insignia ${TONO_BARRA[tono]}`}
          style={{ width: `${porcentaje}%` }}
        />
      </div>
      <span className="min-w-9 font-cuerpo text-pequeno font-medium tabular-nums text-tinta-media">
        {porcentaje}%
      </span>
    </div>
  );
}
