import logotipo from "../assets/next-doc-ai-logo.png";
import simbolo from "../assets/next-doc-ai-symbol.png";

/*
 * Marca institucional NEXT DOC AI. Los archivos son los oficiales del Design
 * System v0.2 (proyecto-next-doc/src/assets). El documento del símbolo es
 * grafito, así que sobre fondo oscuro se apoya en una placa blanca, que es el
 * tratamiento que define .nd-brand en el SDK.
 */

const PROPORCION_LOGO = 505 / 157;
const PROPORCION_SIMBOLO = 189 / 157;

export function Isotipo({
  tamano = 32,
  claro = false,
}: {
  tamano?: number;
  claro?: boolean;
}) {
  return (
    <span
      className={
        claro
          ? "inline-flex items-center justify-center rounded-[6px] bg-blanco p-[3px]"
          : "inline-flex items-center justify-center"
      }
    >
      <img
        src={simbolo}
        alt=""
        aria-hidden
        width={Math.round(tamano * PROPORCION_SIMBOLO)}
        height={tamano}
        style={{ height: tamano, width: "auto" }}
        className="block select-none"
        draggable={false}
      />
    </span>
  );
}

export function Logotipo({
  claro = false,
  tamano = 32,
  escala = 1,
}: {
  claro?: boolean;
  tamano?: number;
  escala?: number;
}) {
  const alto = tamano * escala;
  return (
    <span
      role="img"
      aria-label="NEXT DOC AI"
      className={
        claro
          ? "inline-flex items-center rounded-[10px] bg-blanco px-[8px] py-[6px]"
          : "inline-flex items-center"
      }
    >
      <img
        src={logotipo}
        alt=""
        aria-hidden
        width={Math.round(alto * PROPORCION_LOGO)}
        height={alto}
        style={{ height: alto, width: "auto" }}
        className="block select-none"
        draggable={false}
      />
    </span>
  );
}
