import type { ReactNode } from "react";

export function Cargando({ filas = 5 }: { filas?: number }) {
  return (
    <div className="space-y-2" role="status" aria-label="Cargando">
      {Array.from({ length: filas }).map((_, indice) => (
        <div key={indice} className="esqueleto h-14 rounded-lg bg-white ring-1 ring-borde" />
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
    <div className="rounded-xl border border-dashed border-borde bg-white px-6 py-12 text-center">
      <p className="font-titulo text-base text-tinta">{titulo}</p>
      {detalle ? <p className="mt-1.5 text-sm text-tinta-suave">{detalle}</p> : null}
      {accion ? <div className="mt-4 flex justify-center">{accion}</div> : null}
    </div>
  );
}

export function ErrorPanel({ mensaje, reintentar }: { mensaje: string; reintentar?: () => void }) {
  return (
    <div className="rounded-xl border border-rojo/25 bg-rojo-tenue px-5 py-4">
      <p className="font-titulo text-sm text-rojo">No se pudo cargar</p>
      <p className="mt-1 text-sm text-tinta">{mensaje}</p>
      {reintentar ? (
        <button
          type="button"
          onClick={reintentar}
          className="mt-3 rounded-lg border border-rojo/30 px-3 py-1.5 text-sm font-medium text-rojo transition hover:bg-white"
        >
          Reintentar
        </button>
      ) : null}
    </div>
  );
}
