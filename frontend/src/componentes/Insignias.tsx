import type {
  EstadoDocumento,
  PresenciaCampo,
  SeveridadHallazgo,
} from "../tipos/api";
import { Pastilla, TONO_BARRA } from "./Interfaz";
import type { Tono } from "./Interfaz";
import { useIdioma } from "../contextos/ProveedorIdioma";
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

const ESTADOS_EN_CURSO: EstadoDocumento[] = [
  "RECIBIDO",
  "PROCESANDO",
  "EXTRAIDO",
  "VALIDADO",
];

export function InsigniaEstado({ estado }: { estado: EstadoDocumento }) {
  const { t } = useIdioma();
  const presentacion = ESTADOS_DOCUMENTALES[estado];
  const enCurso = ESTADOS_EN_CURSO.includes(estado);
  return (
    <Pastilla tono={presentacion?.tono ?? "neutro"}>
      {enCurso ? (
        <span
          aria-hidden="true"
          className="mr-espacio-2 inline-block size-espacio-3 animate-spin rounded-insignia border-2 border-current border-t-transparent align-middle motion-reduce:animate-none"
        />
      ) : null}
      {t(`estadosDocumento.${estado}`)}
    </Pastilla>
  );
}

export function InsigniaSeveridad({
  severidad,
}: {
  severidad: SeveridadHallazgo;
}) {
  const { t } = useIdioma();
  return (
    <Pastilla tono={TONO_SEVERIDAD[severidad] ?? "neutro"}>
      {t(`severidad.${severidad}`)}
    </Pastilla>
  );
}

export function InsigniaPresencia({
  presencia,
}: {
  presencia: PresenciaCampo;
}) {
  const { t } = useIdioma();
  return (
    <Pastilla tono={TONO_PRESENCIA[presencia] ?? "neutro"}>
      {t(`presencia.${presencia}`)}
    </Pastilla>
  );
}

export function BarraConfianza({ valor }: { valor?: number }) {
  const { t } = useIdioma();
  if (valor === undefined || valor === null) {
    return <span className="text-pequeno text-tinta-suave">{t("comun.sinDato")}</span>;
  }
  const porcentaje = Math.round(valor * 100);
  const tono: Tono =
    porcentaje >= 90 ? "exito" : porcentaje >= 70 ? "alerta" : "rojo";
  return (
    <div className="flex items-center gap-espacio-2">
      <div
        role="meter"
        aria-label={t("insignias.confianzaLectura")}
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
