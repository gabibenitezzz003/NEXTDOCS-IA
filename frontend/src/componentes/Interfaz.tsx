import type {
  ButtonHTMLAttributes,
  CSSProperties,
  InputHTMLAttributes,
  ReactElement,
  ReactNode,
  SelectHTMLAttributes,
} from "react";
import { useEffect, useId, useRef } from "react";
import { IconoCerrar } from "./Iconos";

export type Tono =
  "neutro" | "violeta" | "exito" | "alerta" | "rojo" | "informacion";

const TONO_SUAVE: Record<Tono, string> = {
  neutro: "bg-lienzo text-neutro-texto ring-borde",
  violeta: "bg-violeta-tenue text-violeta ring-violeta-borde",
  exito: "bg-exito-tenue text-exito-texto ring-exito-borde",
  alerta: "bg-alerta-tenue text-alerta-texto ring-alerta-borde",
  rojo: "bg-rojo-tenue text-rojo-alto ring-rojo-borde",
  informacion: "bg-informacion-tenue text-tinta-media ring-informacion-borde",
};

const TONO_SOLIDO: Record<Tono, string> = {
  neutro: "bg-tinta-suave text-white",
  violeta: "bg-violeta text-white",
  exito: "bg-exito-texto text-white",
  alerta: "bg-alerta-texto text-white",
  rojo: "bg-rojo-alto text-white",
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
  padding = "p-espacio-5",
  indice,
  interactiva = false,
}: {
  children: ReactNode;
  className?: string;
  padding?: string;
  indice?: number;
  interactiva?: boolean;
}) {
  const cascada =
    indice == null
      ? undefined
      : ({ "--retraso": `${indice * 55}ms` } as CSSProperties);
  return (
    <section
      style={cascada}
      className={`relative min-w-0 rounded-tarjeta border border-borde bg-superficie shadow-superficie ${
        indice == null ? "" : "subir"
      } ${interactiva ? "transition-shadow hover:border-borde-fuerte hover:shadow-superficie-elevada motion-reduce:transition-none" : ""} ${padding} ${className}`}
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
    <div className="flex flex-wrap items-start justify-between gap-espacio-3">
      <div className="min-w-0">
        <h2 className="font-titulo text-titulo-panel text-tinta break-words">
          {titulo}
        </h2>
        {descripcion ? (
          <p className="mt-espacio-1 text-pequeno text-tinta-suave">
            {descripcion}
          </p>
        ) : null}
      </div>
      {acciones ? (
        <div className="flex flex-wrap items-center gap-espacio-2">
          {acciones}
        </div>
      ) : null}
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
      className={`inline-flex items-center gap-espacio-1 whitespace-nowrap rounded-insignia px-espacio-3 py-espacio-1 font-cuerpo text-micro font-bold uppercase ${estilo} ${className}`}
    >
      {children}
    </span>
  );
}

export type VarianteBoton = "primario" | "secundario" | "fantasma" | "peligro";

const VARIANTE_BOTON: Record<VarianteBoton, string> = {
  primario:
    "bg-accion-primaria text-blanco enabled:hover:bg-accion-primaria-presionada enabled:active:bg-accion-primaria-presionada",
  secundario:
    "border border-borde bg-superficie text-tinta enabled:hover:border-borde-fuerte enabled:hover:bg-lienzo enabled:active:bg-borde",
  fantasma:
    "bg-transparent text-accion-primaria enabled:hover:bg-violeta-tenue enabled:active:bg-violeta-borde",
  peligro:
    "border border-rojo-alto bg-rojo-alto text-blanco enabled:hover:brightness-95 enabled:active:brightness-90",
};

const TAMANO_BOTON = {
  sm: "h-control-pequeno px-espacio-3",
  md: "h-control-mediano px-espacio-4",
  lg: "h-control-grande px-espacio-5",
};

type PropsBoton = ButtonHTMLAttributes<HTMLButtonElement> & {
  variante?: VarianteBoton;
  tamano?: keyof typeof TAMANO_BOTON;
  cargando?: boolean;
};

export function Boton({
  children,
  variante = "secundario",
  tamano = "md",
  className = "",
  cargando = false,
  disabled,
  ...resto
}: PropsBoton) {
  return (
    <button
      className={`relative inline-flex shrink-0 items-center justify-center whitespace-nowrap rounded-control font-cuerpo text-pequeno font-semibold transition-colors focus-visible:outline-foco motion-reduce:transition-none disabled:cursor-not-allowed disabled:opacity-40 ${VARIANTE_BOTON[variante]} ${TAMANO_BOTON[tamano]} ${className}`}
      {...resto}
      disabled={disabled || cargando}
      aria-busy={cargando || resto["aria-busy"]}
    >
      <span
        className={`inline-flex items-center justify-center gap-espacio-2 ${cargando ? "opacity-0" : ""}`}
      >
        {children}
      </span>
      {cargando ? (
        <span
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 grid place-items-center"
        >
          <span className="size-espacio-4 animate-spin rounded-insignia border-2 border-current border-t-transparent motion-reduce:animate-none" />
        </span>
      ) : null}
    </button>
  );
}

export function BotonIcono({
  children,
  tamano = "md",
  type = "button",
  className = "",
  ...resto
}: Omit<PropsBoton, "children"> & {
  children: ReactElement;
  "aria-label": string;
}) {
  const ancho = {
    sm: "w-control-pequeno",
    md: "w-control-mediano",
    lg: "w-control-grande",
  };
  return (
    <Boton
      {...resto}
      type={type}
      tamano={tamano}
      className={`${ancho[tamano]} px-0! ${className}`}
    >
      <span aria-hidden="true" className="inline-flex">
        {children}
      </span>
    </Boton>
  );
}

const CAMPO_BASE =
  "h-control-mediano w-full min-w-0 rounded-control border border-borde bg-superficie px-espacio-3 font-cuerpo text-pequeno text-tinta transition-colors placeholder:text-tinta-suave enabled:hover:border-borde-fuerte focus:border-foco focus-visible:outline-foco aria-invalid:border-rojo-alto disabled:cursor-not-allowed disabled:bg-lienzo disabled:text-tinta-tenue";

interface MensajesCampo {
  etiqueta?: string;
  ayuda?: string;
  error?: string;
}

function MarcoCampo({
  id,
  etiqueta,
  ayuda,
  error,
  children,
}: MensajesCampo & { id: string; children: ReactNode }) {
  if (!etiqueta && !ayuda && !error) return children;
  return (
    <div className="min-w-0">
      {etiqueta ? (
        <label
          htmlFor={id}
          className="mb-espacio-2 block text-pequeno font-semibold text-tinta-media"
        >
          {etiqueta}
        </label>
      ) : null}
      {children}
      {ayuda ? (
        <p
          id={`${id}-ayuda`}
          className="mt-espacio-1 text-pequeno text-tinta-suave"
        >
          {ayuda}
        </p>
      ) : null}
      {error ? (
        <p
          id={`${id}-error`}
          className="mt-espacio-1 text-pequeno text-rojo-alto"
        >
          {error}
        </p>
      ) : null}
    </div>
  );
}

function useDescripcionCampo(
  id: string | undefined,
  ayuda: string | undefined,
  error: string | undefined,
  descripcion: string | undefined,
) {
  const generado = useId();
  const identificador = id ?? generado;
  const descritoPor =
    [
      descripcion,
      ayuda && `${identificador}-ayuda`,
      error && `${identificador}-error`,
    ]
      .filter(Boolean)
      .join(" ") || undefined;
  return { identificador, descritoPor };
}

export function Campo({
  etiqueta,
  ayuda,
  error,
  id,
  "aria-describedby": descripcion,
  "aria-invalid": invalido,
  className = "",
  ...resto
}: InputHTMLAttributes<HTMLInputElement> & MensajesCampo) {
  const { identificador, descritoPor } = useDescripcionCampo(
    id,
    ayuda,
    error,
    descripcion,
  );
  return (
    <MarcoCampo
      id={identificador}
      etiqueta={etiqueta}
      ayuda={ayuda}
      error={error}
    >
      <input
        {...resto}
        id={identificador}
        aria-describedby={descritoPor}
        aria-invalid={error ? true : invalido}
        className={`${CAMPO_BASE} ${className}`}
      />
    </MarcoCampo>
  );
}

export function Selector({
  etiqueta,
  ayuda,
  error,
  id,
  "aria-describedby": descripcion,
  "aria-invalid": invalido,
  children,
  className = "",
  ...resto
}: SelectHTMLAttributes<HTMLSelectElement> & MensajesCampo) {
  const { identificador, descritoPor } = useDescripcionCampo(
    id,
    ayuda,
    error,
    descripcion,
  );
  return (
    <MarcoCampo
      id={identificador}
      etiqueta={etiqueta}
      ayuda={ayuda}
      error={error}
    >
      <select
        {...resto}
        id={identificador}
        aria-describedby={descritoPor}
        aria-invalid={error ? true : invalido}
        className={`${CAMPO_BASE} cursor-pointer pr-espacio-8 ${className}`}
      >
        {children}
      </select>
    </MarcoCampo>
  );
}

export function GrupoSegmentado<T extends string | number>({
  opciones,
  valor,
  alCambiar,
  etiqueta,
  disabled = false,
}: {
  opciones: { valor: T; texto: string }[];
  valor: T;
  alCambiar: (valor: T) => void;
  etiqueta?: string;
  disabled?: boolean;
}) {
  return (
    <div
      role={etiqueta?.trim() ? "group" : undefined}
      aria-label={etiqueta?.trim() || undefined}
      className="inline-flex max-w-full flex-wrap gap-espacio-1 rounded-control bg-lienzo p-espacio-1"
    >
      {opciones.map((opcion) => (
        <button
          key={String(opcion.valor)}
          type="button"
          disabled={disabled}
          onClick={() => alCambiar(opcion.valor)}
          aria-pressed={valor === opcion.valor}
          className={`min-h-control-pequeno rounded-control px-espacio-3 py-espacio-2 text-pequeno font-semibold transition-colors focus-visible:outline-foco disabled:cursor-not-allowed disabled:opacity-40 ${
            valor === opcion.valor
              ? "bg-superficie text-tinta shadow-plano"
              : "text-tinta-suave enabled:hover:text-tinta enabled:active:bg-borde"
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
  alto = "h-2",
}: {
  porcentaje?: number | null;
  tono?: Tono;
  alto?: string;
}) {
  const ancho = porcentaje == null ? 0 : Math.min(Math.max(porcentaje, 0), 100);
  return (
    <div
      className={`${alto} w-full overflow-hidden rounded-full bg-lienzo shadow-hundido`}
    >
      <div
        className={`h-full rounded-full transition-[width] duration-700 ease-[cubic-bezier(0.22,1,0.36,1)] ${TONO_BARRA[tono]}`}
        style={{
          width: `${ancho}%`,
          boxShadow: "inset 0 1px 0 rgba(255,255,255,0.35)",
        }}
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
  const identificador = useId();
  const dialogo = useRef<HTMLDialogElement>(null);
  const encabezado = useRef<HTMLHeadingElement>(null);
  useEffect(() => {
    const elemento = dialogo.current;
    const origen =
      document.activeElement instanceof HTMLElement
        ? document.activeElement
        : null;
    const desbordamiento = document.body.style.overflow;
    elemento?.showModal();
    encabezado.current?.focus({ preventScroll: true });
    document.body.style.overflow = "hidden";
    return () => {
      elemento?.close();
      document.body.style.overflow = desbordamiento;
      if (origen?.isConnected) origen.focus({ preventScroll: true });
    };
  }, []);

  return (
    <dialog
      ref={dialogo}
      aria-modal="true"
      aria-labelledby={`${identificador}-titulo`}
      aria-describedby={
        descripcion ? `${identificador}-descripcion` : undefined
      }
      onCancel={(evento) => {
        evento.preventDefault();
        alCerrar();
      }}
      onClick={(evento) => {
        if (evento.target !== evento.currentTarget) return;
        const caja = evento.currentTarget.getBoundingClientRect();
        if (
          evento.clientX < caja.left ||
          evento.clientX > caja.right ||
          evento.clientY < caja.top ||
          evento.clientY > caja.bottom
        )
          alCerrar();
      }}
      onKeyDown={(evento) => {
        if (evento.key !== "Tab") return;
        const controles = Array.from(
          evento.currentTarget.querySelectorAll<HTMLElement>(
            "button:not(:disabled), summary, a[href], input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex]",
          ),
        ).filter(
          (elemento) =>
            elemento.tabIndex >= 0 && elemento.getClientRects().length > 0,
        );
        const primero = controles[0];
        const ultimo = controles.at(-1);
        const activo = document.activeElement;
        if (
          evento.shiftKey &&
          (activo === primero || !controles.includes(activo as HTMLElement))
        ) {
          evento.preventDefault();
          (ultimo ?? encabezado.current)?.focus();
        } else if (!evento.shiftKey && (activo === ultimo || !primero)) {
          evento.preventDefault();
          (primero ?? encabezado.current)?.focus();
        }
      }}
      className="fixed inset-y-0 right-0 left-auto m-0 h-dvh max-h-none w-full max-w-2xl rounded-panel-lateral border-0 border-l border-borde bg-superficie p-0 text-tinta shadow-panel-lateral backdrop:bg-grafito/50 backdrop:backdrop-blur-[3px]"
    >
      <div className="flex h-full min-h-0 flex-col">
        <header className="flex items-start justify-between gap-espacio-4 border-b border-borde px-espacio-6 py-espacio-5">
          <div className="min-w-0">
            <h2
              ref={encabezado}
              tabIndex={-1}
              id={`${identificador}-titulo`}
              className="font-titulo text-titulo-panel text-tinta break-words focus:outline-none"
            >
              {titulo}
            </h2>
            {descripcion ? (
              <p
                id={`${identificador}-descripcion`}
                className="mt-espacio-1 text-pequeno text-tinta-suave"
              >
                {descripcion}
              </p>
            ) : null}
          </div>
          <BotonIcono
            variante="fantasma"
            tamano="sm"
            onClick={alCerrar}
            aria-label="Cerrar"
          >
            <IconoCerrar tamano={16} />
          </BotonIcono>
        </header>
        <div className="barra-desplazamiento-fina min-h-0 flex-1 overflow-y-auto px-espacio-6 py-espacio-5">
          {children}
        </div>
        {pie ? (
          <footer className="border-t border-borde bg-lienzo px-espacio-6 py-espacio-4">
            {pie}
          </footer>
        ) : null}
      </div>
    </dialog>
  );
}

export function Metrica({
  etiqueta,
  valor,
  detalle,
  children,
  destacada = false,
}: {
  etiqueta: string;
  valor: ReactNode;
  detalle?: ReactNode;
  children?: ReactNode;
  destacada?: boolean;
}) {
  return (
    <div>
      <p className="text-micro uppercase tracking-wider text-tinta-suave">
        {etiqueta}
      </p>
      <p
        className={`cifra mt-espacio-2 break-words ${
          destacada
            ? "cifra-degradada text-metrica-destacada"
            : "text-metrica-compacta text-tinta"
        }`}
      >
        {valor}
      </p>
      {detalle ? (
        <div className="mt-espacio-3 text-pequeno text-tinta-suave">
          {detalle}
        </div>
      ) : null}
      {children}
    </div>
  );
}
