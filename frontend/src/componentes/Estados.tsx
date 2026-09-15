import type { ReactNode } from "react";
import { tituloDeError } from "../api/cliente";
import { useIdioma } from "../contextos/ProveedorIdioma";
import { IconoInfo, IconoRecargar, IconoVacio } from "./Iconos";
import { Boton } from "./Interfaz";

export function Cargando({
  filas = 5,
  alto = "h-16",
}: {
  filas?: number;
  alto?: string;
}) {
  const { t } = useIdioma();
  return (
    <div
      className="space-y-espacio-3"
      role="status"
      aria-busy="true"
      aria-atomic="true"
    >
      <span className="sr-only">{t("comun.cargandoContenido")}</span>
      {Array.from({ length: filas }).map((_, indice) => (
        <div
          key={indice}
          aria-hidden="true"
          className={`esqueleto ${alto} rounded-panel`}
        />
      ))}
    </div>
  );
}

export function CargandoTarjetas({ cantidad = 4 }: { cantidad?: number }) {
  const { t } = useIdioma();
  return (
    <div
      className="grid gap-espacio-4 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4"
      role="status"
      aria-busy="true"
      aria-atomic="true"
    >
      <span className="sr-only">{t("comun.cargandoTarjetas")}</span>
      {Array.from({ length: cantidad }).map((_, indice) => (
        <div
          key={indice}
          aria-hidden="true"
          className="esqueleto h-36 rounded-tarjeta"
        />
      ))}
    </div>
  );
}

export function Vacio({
  titulo,
  detalle,
  accion,
}: {
  titulo: string;
  detalle?: string;
  accion?: ReactNode;
}) {
  return (
    <div className="rounded-panel border border-dashed border-borde-fuerte bg-superficie px-espacio-6 py-espacio-12 text-center">
      <span
        aria-hidden="true"
        className="mx-auto mb-espacio-4 flex size-espacio-12 items-center justify-center rounded-control bg-lienzo text-tinta-suave ring-1 ring-borde"
      >
        <IconoVacio tamano={24} />
      </span>
      <div role="status" aria-atomic="true">
        <p className="font-titulo text-titulo-panel text-tinta">{titulo}</p>
        {detalle ? (
          <p className="mx-auto mt-espacio-2 max-w-md text-pequeno text-tinta-suave">
            {detalle}
          </p>
        ) : null}
      </div>
      {accion ? (
        <div className="mt-espacio-5 flex flex-wrap justify-center gap-espacio-2">
          {accion}
        </div>
      ) : null}
    </div>
  );
}

export function ErrorPanel({
  mensaje,
  reintentar,
  titulo,
  error,
  contexto,
}: {
  mensaje: string;
  reintentar?: () => void;
  titulo?: string;
  error?: unknown;
  contexto?: string;
}) {
  const { t } = useIdioma();
  const encabezado =
    titulo ?? (error === undefined ? t("comun.noSePudoCargar") : tituloDeError(error));
  const detalle = contexto ? `${contexto}. ${mensaje}` : mensaje;
  return (
    <div
      role="alert"
      aria-atomic="true"
      className="flex flex-wrap items-start gap-espacio-3 rounded-panel border border-rojo-borde bg-rojo-tenue p-espacio-4"
    >
      <span aria-hidden="true" className="mt-0.5 text-rojo-alto">
        <IconoInfo tamano={18} />
      </span>
      <div className="min-w-0 flex-1">
        <p className="text-pequeno font-semibold text-rojo-alto">{encabezado}</p>
        <p className="mt-espacio-1 text-pequeno text-tinta-media break-words">
          {detalle}
        </p>
      </div>
      {reintentar ? (
        <Boton
          type="button"
          variante="peligro"
          tamano="sm"
          onClick={reintentar}
        >
          <span aria-hidden="true" className="inline-flex">
            <IconoRecargar tamano={14} />
          </span>
          {t("comun.reintentar")}
        </Boton>
      ) : null}
    </div>
  );
}

export function AvisoLinea({ children }: { children: ReactNode }) {
  return (
    <p
      role="note"
      className="flex items-start gap-espacio-2 rounded-control bg-lienzo px-espacio-4 py-espacio-3 text-pequeno text-tinta-suave ring-1 ring-inset ring-borde"
    >
      <span aria-hidden="true" className="mt-px shrink-0">
        <IconoInfo tamano={14} />
      </span>
      {children}
    </p>
  );
}
