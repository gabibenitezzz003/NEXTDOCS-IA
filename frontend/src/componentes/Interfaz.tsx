import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from "react";
import { useEffect } from "react";
import { IconoCerrar } from "./Iconos";

export type Tono = "neutro" | "violeta" | "exito" | "alerta" | "rojo" | "informacion";

const TONO_SUAVE: Record<Tono, string> = {
  neutro: "bg-lienzo text-tinta-suave ring-borde",
  violeta: "bg-violeta-tenue text-violeta ring-violeta-borde",
  exito: "bg-exito-tenue text-exito ring-exito-borde",
  alerta: "bg-alerta-tenue text-alerta ring-alerta-borde",
  rojo: "bg-rojo-tenue text-rojo ring-rojo-borde",
  informacion: "bg-informacion-tenue text-informacion ring-informacion-borde",
};

const TONO_SOLIDO: Record<Tono, string> = {
  neutro: "bg-tinta-suave text-white",
  violeta: "bg-violeta text-white",
  exito: "bg-exito text-white",
  alerta: "bg-alerta text-white",
  rojo: "bg-rojo text-white",
  informacion: "bg-informacion text-white",
};

export const TONO_BARRA: Record<Tono, string> = {
  neutro: "bg-borde-fuerte",
  violeta: "bg-violeta",
  exito: "bg-exito",
  alerta: "bg-alerta",
  rojo: "bg-rojo",
  informacion: "bg-informacion",
};

export function Tarjeta({
  children,
  className = "",
  padding = "p-5",
}: {
  children: ReactNode;
  className?: string;
  padding?: string;
}) {
  return (
    <section
      className={`rounded-2xl border border-borde bg-white shadow-tarjeta ${padding} ${className}`}
    >
      {children}
    </section>
  );
}

export function CabeceraTarjeta({
  titulo,
  descripcion,
  acciones,
}: {
  titulo: string;
  descripcion?: string;
  acciones?: ReactNode;
}) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-3">
      <div className="min-w-0">
        <h2 className="font-titulo text-base text-tinta">{titulo}</h2>
        {descripcion ? <p className="mt-0.5 text-xs text-tinta-suave">{descripcion}</p> : null}
      </div>
      {acciones ? <div className="flex shrink-0 items-center gap-2">{acciones}</div> : null}
    </div>
  );
}

export function Pastilla({
  children,
  tono = "neutro",
  solido = false,
  className = "",
}: {
  children: ReactNode;
  tono?: Tono;
  solido?: boolean;
  className?: string;
}) {
  const estilo = solido
    ? TONO_SOLIDO[tono]
    : `ring-1 ring-inset ${TONO_SUAVE[tono]}`;
  return (
    <span
      className={`inline-flex items-center gap-1 whitespace-nowrap rounded-full px-2.5 py-0.5 text-[11px] font-semibold tracking-wide uppercase ${estilo} ${className}`}
    >
      {children}
    </span>
  );
}

type VarianteBoton = "primario" | "secundario" | "fantasma" | "peligro";

const VARIANTE_BOTON: Record<VarianteBoton, string> = {
  primario:
    "bg-violeta text-white shadow-violeta hover:bg-violeta-alto active:bg-violeta-alto disabled:bg-violeta/45 disabled:shadow-none",
  secundario:
    "border border-borde bg-white text-tinta shadow-plano hover:border-borde-fuerte hover:bg-lienzo disabled:text-tinta-tenue",
  fantasma: "text-tinta-suave hover:bg-lienzo hover:text-tinta disabled:text-tinta-tenue",
  peligro:
    "border border-rojo-borde bg-rojo-tenue text-rojo hover:bg-rojo hover:text-white hover:border-rojo disabled:opacity-50",
};

const TAMANO_BOTON = {
  sm: "h-8 px-3 text-xs",
  md: "h-9.5 px-4 text-sm",
  lg: "h-11 px-5 text-sm",
};

export function Boton({
  children,
  variante = "secundario",
  tamano = "md",
  className = "",
  ...resto
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variante?: VarianteBoton;
  tamano?: keyof typeof TAMANO_BOTON;
}) {
  return (
    <button
      className={`inline-flex items-center justify-center gap-1.5 rounded-xl font-semibold transition-[background-color,color,border-color,box-shadow] disabled:cursor-not-allowed ${VARIANTE_BOTON[variante]} ${TAMANO_BOTON[tamano]} ${className}`}
      {...resto}
    >
      {children}
    </button>
  );
}

const CAMPO_BASE =
  "w-full rounded-xl border border-borde bg-white text-sm text-tinta shadow-plano outline-none transition placeholder:text-tinta-tenue focus:border-violeta focus:ring-[3px] focus:ring-violeta/15 disabled:bg-lienzo disabled:text-tinta-tenue";

export function Campo({
  etiqueta,
  ayuda,
  className = "",
  ...resto
}: InputHTMLAttributes<HTMLInputElement> & { etiqueta?: string; ayuda?: string }) {
  const entrada = <input className={`${CAMPO_BASE} h-10 px-3 ${className}`} {...resto} />;
  if (!etiqueta) {
    return entrada;
  }
  return (
    <label className="block">
      <span className="mb-1.5 block text-xs font-semibold text-tinta-media">{etiqueta}</span>
      {entrada}
      {ayuda ? <span className="mt-1 block text-xs text-tinta-suave">{ayuda}</span> : null}
    </label>
  );
}

export function Selector({
  etiqueta,
  children,
  className = "",
  ...resto
}: SelectHTMLAttributes<HTMLSelectElement> & { etiqueta?: string }) {
  const entrada = (
    <select className={`${CAMPO_BASE} h-10 cursor-pointer px-3 pr-8 ${className}`} {...resto}>
      {children}
    </select>
  );
  if (!etiqueta) {
    return entrada;
  }
  return (
    <label className="block">
      <span className="mb-1.5 block text-xs font-semibold text-tinta-media">{etiqueta}</span>
      {entrada}
    </label>
  );
}

export function GrupoSegmentado<T extends string | number>({
  opciones,
  valor,
  alCambiar,
}: {
  opciones: { valor: T; texto: string }[];
  valor: T;
  alCambiar: (valor: T) => void;
}) {
  return (
    <div className="inline-flex rounded-xl border border-borde bg-white p-1 shadow-plano">
      {opciones.map((opcion) => (
        <button
          key={String(opcion.valor)}
          type="button"
          onClick={() => alCambiar(opcion.valor)}
          aria-pressed={valor === opcion.valor}
          className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition ${
            valor === opcion.valor
              ? "bg-grafito text-white shadow-plano"
              : "text-tinta-suave hover:text-tinta"
          }`}
        >
          {opcion.texto}
        </button>
      ))}
    </div>
  );
}

export function Barra({
  porcentaje,
  tono = "violeta",
  alto = "h-1.5",
}: {
  porcentaje?: number | null;
  tono?: Tono;
  alto?: string;
}) {
  const ancho = porcentaje == null ? 0 : Math.min(Math.max(porcentaje, 0), 100);
  return (
    <div className={`${alto} w-full overflow-hidden rounded-full bg-lienzo ring-1 ring-inset ring-borde`}>
      <div
        className={`h-full rounded-full transition-[width] duration-500 ${TONO_BARRA[tono]}`}
        style={{ width: `${ancho}%` }}
      />
    </div>
  );
}

export function Panel({
  titulo,
  descripcion,
  alCerrar,
  children,
  pie,
}: {
  titulo: string;
  descripcion?: string;
  alCerrar: () => void;
  children: ReactNode;
  pie?: ReactNode;
}) {
  useEffect(() => {
    function alTeclear(evento: KeyboardEvent) {
      if (evento.key === "Escape") {
        alCerrar();
      }
    }
    document.addEventListener("keydown", alTeclear);
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", alTeclear);
      document.body.style.overflow = "";
    };
  }, [alCerrar]);

  return (
    <div
      className="velo fixed inset-0 z-50 flex justify-end bg-grafito/45 backdrop-blur-[2px]"
      onClick={alCerrar}
    >
      <aside
        role="dialog"
        aria-modal="true"
        aria-label={titulo}
        className="entrar-lateral flex h-full w-full max-w-2xl flex-col border-l border-borde bg-white shadow-flotante"
        onClick={(evento) => evento.stopPropagation()}
      >
        <header className="flex items-start justify-between gap-4 border-b border-borde px-6 py-5">
          <div className="min-w-0">
            <h2 className="font-titulo text-lg text-tinta">{titulo}</h2>
            {descripcion ? <p className="mt-1 text-xs text-tinta-suave">{descripcion}</p> : null}
          </div>
          <Boton variante="fantasma" tamano="sm" onClick={alCerrar} aria-label="Cerrar">
            <IconoCerrar tamano={16} />
          </Boton>
        </header>
        <div className="barra-desplazamiento-fina flex-1 overflow-y-auto px-6 py-5">{children}</div>
        {pie ? <footer className="border-t border-borde bg-lienzo px-6 py-4">{pie}</footer> : null}
      </aside>
    </div>
  );
}

export function Metrica({
  etiqueta,
  valor,
  detalle,
  children,
}: {
  etiqueta: string;
  valor: ReactNode;
  detalle?: ReactNode;
  children?: ReactNode;
}) {
  return (
    <div>
      <p className="text-[11px] font-semibold uppercase tracking-wider text-tinta-suave">{etiqueta}</p>
      <p className="cifra mt-1.5 text-3xl leading-none text-tinta">{valor}</p>
      {detalle ? <div className="mt-2 text-xs text-tinta-suave">{detalle}</div> : null}
      {children}
    </div>
  );
}
