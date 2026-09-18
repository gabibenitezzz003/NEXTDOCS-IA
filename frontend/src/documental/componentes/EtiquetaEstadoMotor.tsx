import type { EstadoDocumentoMotor } from "../../api/documental";
import { useIdioma } from "../../contextos/ProveedorIdioma";

const TONOS: Record<EstadoDocumentoMotor, string> = {
  RECIBIDO: "bg-lienzo text-tinta-media ring-1 ring-inset ring-borde",
  PROCESANDO:
    "bg-informacion-tenue text-informacion ring-1 ring-inset ring-informacion-borde",
  EXTRAIDO:
    "bg-violeta-tenue text-violeta ring-1 ring-inset ring-violeta-borde",
  VALIDADO:
    "bg-exito-tenue text-exito-texto ring-1 ring-inset ring-exito-borde",
  OBSERVADO:
    "bg-alerta-tenue text-alerta-texto ring-1 ring-inset ring-alerta-borde",
  APROBADO:
    "bg-exito-tenue text-exito-texto ring-1 ring-inset ring-exito-borde",
  RECHAZADO: "bg-rojo-tenue text-rojo-alto ring-1 ring-inset ring-rojo-borde",
  DIVIDIDO:
    "bg-informacion-tenue text-informacion ring-1 ring-inset ring-informacion-borde",
  CERRADO: "bg-lienzo text-tinta-suave ring-1 ring-inset ring-borde",
};

export function EtiquetaEstadoMotor({
  estado,
}: {
  estado: EstadoDocumentoMotor;
}) {
  const { t } = useIdioma();
  return (
    <span
      className={`inline-flex items-center whitespace-nowrap rounded-insignia px-espacio-3 py-espacio-1 text-micro font-bold uppercase ${TONOS[estado] ?? TONOS.RECIBIDO}`}
    >
      {t(`documental.estado.${estado}`)}
    </span>
  );
}

const TONOS_SEVERIDAD: Record<string, string> = {
  critico: "bg-rojo-tenue text-rojo-alto ring-1 ring-inset ring-rojo-borde",
  error: "bg-rojo-tenue text-rojo-alto ring-1 ring-inset ring-rojo-borde",
  advertencia:
    "bg-alerta-tenue text-alerta-texto ring-1 ring-inset ring-alerta-borde",
  informativo:
    "bg-informacion-tenue text-informacion ring-1 ring-inset ring-informacion-borde",
};

export function EtiquetaSeveridadMotor({ severidad }: { severidad: string }) {
  const { t } = useIdioma();
  return (
    <span
      className={`inline-flex items-center whitespace-nowrap rounded-insignia px-espacio-3 py-espacio-1 text-micro font-bold uppercase ${TONOS_SEVERIDAD[severidad] ?? TONOS_SEVERIDAD.informativo}`}
    >
      {t(`documental.severidad.${severidad}`)}
    </span>
  );
}
