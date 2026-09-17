import type {
  ButtonHTMLAttributes,
  CSSProperties,
  HTMLAttributes,
  InputHTMLAttributes,
  KeyboardEvent,
  ReactElement,
  ReactNode,
  SelectHTMLAttributes,
  TextareaHTMLAttributes,
} from "react";
import { Children, isValidElement, useEffect, useId, useRef, useState } from "react";
import { IconoCerrar, IconoFlechaAbajo } from "./Iconos";
import { useIdioma } from "../contextos/ProveedorIdioma";

export type Tono =
  "neutro" | "violeta" | "exito" | "alerta" | "rojo" | "informacion";

const TONO_SUAVE: Record<Tono, string> = {
  neutro: "bg-lienzo text-neutro-texto ring-borde",
  violeta: "bg-violeta-tenue text-accion-tonal-texto ring-violeta-borde",
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
  ...resto
}: {
  children: ReactNode;
  className?: string;
  padding?: string;
  indice?: number;
  interactiva?: boolean;
} & Omit<HTMLAttributes<HTMLElement>, "className" | "children">) {
  const cascada =
    indice == null
      ? undefined
      : ({ "--retraso": `${indice * 55}ms` } as CSSProperties);
  return (
    <section
      style={cascada}
      className={`relative min-w-0 rounded-tarjeta border border-borde bg-superficie relieve ${
        indice == null ? "" : "subir"
      } ${interactiva ? "transition-shadow hover:border-borde-fuerte hover:shadow-superficie-elevada motion-reduce:transition-none" : ""} ${padding} ${className}`}
      {...resto}
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
    "degradado-marca text-blanco shadow-violeta enabled:hover:shadow-violeta-alto enabled:hover:brightness-110 enabled:active:brightness-95",
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
          id={`${id}-etiqueta`}
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

export function AreaTexto({
  etiqueta,
  ayuda,
  error,
  id,
  "aria-describedby": descripcion,
  "aria-invalid": invalido,
  className = "",
  ...resto
}: TextareaHTMLAttributes<HTMLTextAreaElement> & MensajesCampo) {
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
      <textarea
        {...resto}
        id={identificador}
        aria-describedby={descritoPor}
        aria-invalid={error ? true : invalido}
        className={`${CAMPO_BASE} h-auto min-h-20 py-espacio-2 ${className}`}
      />
    </MarcoCampo>
  );
}

interface OpcionSelector {
  valor: string;
  texto: string;
  deshabilitada: boolean;
}

function leerOpciones(children: ReactNode): OpcionSelector[] {
  const opciones: OpcionSelector[] = [];
  Children.forEach(children, (hijo) => {
    if (!isValidElement(hijo)) return;
    const props = hijo.props as {
      value?: string | number;
      children?: ReactNode;
      disabled?: boolean;
    };
    opciones.push({
      valor: String(props.value ?? ""),
      texto: String(props.children ?? ""),
      deshabilitada: Boolean(props.disabled),
    });
  });
  return opciones;
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
  value,
  onChange,
  disabled,
  name,
}: SelectHTMLAttributes<HTMLSelectElement> & MensajesCampo) {
  const { identificador, descritoPor } = useDescripcionCampo(
    id,
    ayuda,
    error,
    descripcion,
  );
  const opciones = leerOpciones(children);
  const seleccionada = String(value ?? "");
  const indiceActual = Math.max(
    0,
    opciones.findIndex((opcion) => opcion.valor === seleccionada),
  );

  const [abierto, setAbierto] = useState(false);
  const [resaltada, setResaltada] = useState(indiceActual);
  const contenedor = useRef<HTMLDivElement>(null);
  const lista = useRef<HTMLUListElement>(null);

  useEffect(() => {
    if (!abierto) return;
    function alClicFuera(evento: MouseEvent) {
      if (!contenedor.current?.contains(evento.target as Node)) setAbierto(false);
    }
    document.addEventListener("mousedown", alClicFuera);
    return () => document.removeEventListener("mousedown", alClicFuera);
  }, [abierto]);

  useEffect(() => {
    if (!abierto) return;
    lista.current?.children[resaltada]?.scrollIntoView({ block: "nearest" });
  }, [abierto, resaltada]);

  function elegir(indice: number) {
    const opcion = opciones[indice];
    if (!opcion || opcion.deshabilitada) return;
    setAbierto(false);
    if (opcion.valor === seleccionada) return;
    onChange?.({
      target: { value: opcion.valor, name: name ?? "" },
      currentTarget: { value: opcion.valor, name: name ?? "" },
    } as never);
  }

  function mover(salto: number) {
    setResaltada((actual) => {
      let siguiente = actual;
      for (let intento = 0; intento < opciones.length; intento++) {
        siguiente =
          (siguiente + salto + opciones.length) % opciones.length;
        if (!opciones[siguiente].deshabilitada) return siguiente;
      }
      return actual;
    });
  }

  function alTeclado(evento: KeyboardEvent<HTMLDivElement>) {
    if (disabled) return;
    if (evento.key === "Escape" && abierto) {
      evento.preventDefault();
      setAbierto(false);
      return;
    }
    if (evento.key === "Tab") {
      setAbierto(false);
      return;
    }
    if (!abierto) {
      if (["Enter", " ", "ArrowDown", "ArrowUp"].includes(evento.key)) {
        evento.preventDefault();
        setResaltada(indiceActual);
        setAbierto(true);
      }
      return;
    }
    if (evento.key === "ArrowDown") {
      evento.preventDefault();
      mover(1);
    } else if (evento.key === "ArrowUp") {
      evento.preventDefault();
      mover(-1);
    } else if (evento.key === "Home") {
      evento.preventDefault();
      setResaltada(0);
    } else if (evento.key === "End") {
      evento.preventDefault();
      setResaltada(opciones.length - 1);
    } else if (evento.key === "Enter" || evento.key === " ") {
      evento.preventDefault();
      elegir(resaltada);
    } else if (evento.key.length === 1) {
      const buscado = evento.key.toLowerCase();
      const encontrado = opciones.findIndex(
        (opcion) =>
          !opcion.deshabilitada &&
          opcion.texto.toLowerCase().startsWith(buscado),
      );
      if (encontrado >= 0) setResaltada(encontrado);
    }
  }

  const textoVisible = opciones[indiceActual]?.texto ?? "";
  const idLista = `${identificador}-lista`;

  return (
    <MarcoCampo
      id={identificador}
      etiqueta={etiqueta}
      ayuda={ayuda}
      error={error}
    >
      <div ref={contenedor} className={`relative min-w-0 ${className}`}>
        <div
          id={identificador}
          role="combobox"
          tabIndex={disabled ? -1 : 0}
          aria-expanded={abierto}
          aria-controls={abierto ? idLista : undefined}
          aria-haspopup="listbox"
          aria-labelledby={etiqueta ? `${identificador}-etiqueta` : undefined}
          aria-describedby={descritoPor}
          aria-invalid={error ? true : invalido}
          aria-disabled={disabled}
          onKeyDown={alTeclado}
          onClick={() => {
            if (disabled) return;
            setResaltada(indiceActual);
            setAbierto((valor) => !valor);
          }}
          className={`${CAMPO_BASE} flex cursor-pointer items-center justify-between gap-espacio-2 pr-espacio-3 ${
            disabled ? "cursor-not-allowed bg-lienzo text-tinta-tenue" : ""
          } ${abierto ? "border-foco" : ""}`}
        >
          <span className="truncate">{textoVisible}</span>
          <span
            aria-hidden="true"
            className={`shrink-0 text-tinta-suave transition-transform ${abierto ? "rotate-180" : ""}`}
          >
            <IconoFlechaAbajo tamano={14} />
          </span>
        </div>
        {abierto ? (
          <ul
            ref={lista}
            id={idLista}
            role="listbox"
            aria-labelledby={etiqueta ? `${identificador}-etiqueta` : undefined}
            className="absolute z-50 mt-espacio-1 max-h-64 w-full overflow-auto rounded-control border border-borde bg-superficie py-espacio-1 shadow-elevado"
          >
            {opciones.map((opcion, indice) => (
              <li
                key={opcion.valor + indice}
                role="option"
                aria-selected={opcion.valor === seleccionada}
                aria-disabled={opcion.deshabilitada || undefined}
                onMouseEnter={() => setResaltada(indice)}
                onMouseDown={(evento) => evento.preventDefault()}
                onClick={() => elegir(indice)}
                className={`cursor-pointer px-espacio-3 py-espacio-2 text-pequeno ${
                  opcion.deshabilitada
                    ? "cursor-not-allowed text-tinta-tenue"
                    : indice === resaltada
                      ? "bg-violeta-tenue text-accion-tonal-texto"
                      : "text-tinta"
                }`}
              >
                {opcion.texto}
              </li>
            ))}
          </ul>
        ) : null}
      </div>
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
  const { t } = useIdioma();
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
            aria-label={t("comun.cerrar")}
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
