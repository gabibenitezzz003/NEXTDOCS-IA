import type { ReactNode } from "react";
import { IconoInfo, IconoRecargar, IconoVacio } from "./Iconos";
import { Boton } from "./Interfaz";

export function Cargando({ filas = 5, alto = "h-16" }: { filas?: number; alto?: string }) {
  return (
    <div className="space-y-3" role="status" aria-label="Cargando">
      {Array.from({ length: filas }).map((_, indice) => (
        <div key={indice} className={`esqueleto ${alto} rounded-2xl`} />
      ))}
    </div>
  );
}

export function CargandoTarjetas({ cantidad = 4 }: { cantidad?: number }) {
  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4" role="status" aria-label="Cargando">
      {Array.from({ length: cantidad }).map((_, indice) => (
        <div key={indice} className="esqueleto h-36 rounded-2xl" />
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
    <div className="rounded-2xl border border-dashed border-borde-fuerte bg-white px-6 py-14 text-center">
      <span className="mx-auto mb-4 flex size-12 items-center justify-center rounded-2xl bg-lienzo text-tinta-tenue ring-1 ring-borde">
        <IconoVacio tamano={24} />
      </span>
      <p className="font-titulo text-base text-tinta">{titulo}</p>
      {detalle ? <p className="mx-auto mt-1.5 max-w-md text-sm text-tinta-suave">{detalle}</p> : null}
      {accion ? <div className="mt-5 flex justify-center">{accion}</div> : null}
    </div>
  );
}

export function ErrorPanel({ mensaje, reintentar }: { mensaje: string; reintentar?: () => void }) {
  return (
    <div
      role="alert"
      className="flex flex-wrap items-start gap-3 rounded-2xl border border-rojo-borde bg-rojo-tenue px-5 py-4"
    >
      <span className="mt-0.5 text-rojo">
        <IconoInfo tamano={18} />
      </span>
      <div className="min-w-0 flex-1">
        <p className="font-titulo text-sm text-rojo">No se pudo cargar</p>
        <p className="mt-0.5 text-sm text-tinta-media">{mensaje}</p>
      </div>
      {reintentar ? (
        <Boton variante="peligro" tamano="sm" onClick={reintentar}>
          <IconoRecargar tamano={14} />
          Reintentar
        </Boton>
      ) : null}
    </div>
  );
}

export function AvisoLinea({ children }: { children: ReactNode }) {
  return (
    <p className="flex items-start gap-2 rounded-xl bg-lienzo px-3.5 py-2.5 text-xs text-tinta-suave ring-1 ring-inset ring-borde">
      <span className="mt-px shrink-0 text-tinta-tenue">
        <IconoInfo tamano={14} />
      </span>
      {children}
    </p>
  );
}
