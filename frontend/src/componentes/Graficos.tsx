import { useEffect, useId, useRef, useState } from "react";
import type { ReactNode } from "react";

export function useContador(objetivo: number, duracion = 900) {
  const [valor, setValor] = useState(0);
  const previo = useRef(0);

  useEffect(() => {
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      previo.current = objetivo;
      setValor(objetivo);
      return;
    }
    const desde = previo.current;
    const delta = objetivo - desde;
    if (delta === 0) {
      return;
    }
    let cuadro = 0;
    const inicio = performance.now();

    function paso(ahora: number) {
      const avance = Math.min((ahora - inicio) / duracion, 1);
      const suavizado = 1 - Math.pow(1 - avance, 3);
      setValor(desde + delta * suavizado);
      if (avance < 1) {
        cuadro = requestAnimationFrame(paso);
      } else {
        previo.current = objetivo;
      }
    }

    cuadro = requestAnimationFrame(paso);
    return () => cancelAnimationFrame(cuadro);
  }, [objetivo, duracion]);

  return valor;
}

export function useVisible<T extends Element>() {
  const referencia = useRef<T>(null);
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const elemento = referencia.current;
    if (!elemento) {
      return;
    }
    const observador = new IntersectionObserver(
      ([entrada]) => {
        if (entrada.isIntersecting) {
          setVisible(true);
          observador.disconnect();
        }
      },
      { threshold: 0.25 },
    );
    observador.observe(elemento);
    return () => observador.disconnect();
  }, []);

  return { referencia, visible };
}

const PALETA: Record<string, [string, string]> = {
  violeta: ["#8A63FF", "#6C38FF"],
  exito: ["#34C77B", "#0F9D58"],
  alerta: ["#F0A93B", "#C2760A"],
  rojo: ["#FF5C5C", "#FF1E1E"],
  informacion: ["#5EA0F2", "#1D6FE0"],
  neutro: ["#C3C9D6", "#9AA1B1"],
};

export type ClaveTono = keyof typeof PALETA;

export const COLOR_GRAFICO: Record<ClaveTono, string> = {
  violeta: "var(--color-violeta)",
  exito: "var(--color-exito)",
  alerta: "var(--color-alerta)",
  rojo: "var(--color-rojo)",
  informacion: "var(--color-informacion)",
  neutro: "var(--color-tinta-suave)",
};

const PISTA = "#E9ECF3";

function Resplandor({
  id,
  color,
  fuerza = 3,
}: {
  id: string;
  color: string;
  fuerza?: number;
}) {
  return (
    <filter id={id} x="-50%" y="-50%" width="200%" height="200%">
      <feDropShadow
        dx="0"
        dy="1"
        stdDeviation={fuerza}
        floodColor={color}
        floodOpacity="0.45"
      />
    </filter>
  );
}

export function Anillo({
  porcentaje,
  tono = "violeta",
  tamano = 148,
  grosor = 14,
  centro,
  subtitulo,
  plano = false,
}: {
  porcentaje: number | null;
  tono?: ClaveTono;
  tamano?: number;
  grosor?: number;
  centro?: ReactNode;
  subtitulo?: string;
  plano?: boolean;
}) {
  const id = useId().replace(/:/g, "");
  const { referencia, visible } = useVisible<HTMLDivElement>();
  const objetivo =
    porcentaje == null ? 0 : Math.min(Math.max(porcentaje, 0), 100);
  const animado = useContador(visible ? objetivo : 0, 1200);

  const radio = (tamano - grosor) / 2;
  const circunferencia = 2 * Math.PI * radio;
  const [claro, oscuro] = PALETA[tono];

  return (
    <div
      ref={referencia}
      className="relative inline-flex items-center justify-center"
    >
      <span
        aria-hidden
        className="absolute rounded-full blur-2xl transition-opacity duration-1000"
        style={{
          width: tamano * 0.72,
          height: tamano * 0.72,
          background: claro,
          opacity: visible && !plano ? 0.16 : 0,
        }}
      />
      <svg
        width={tamano}
        height={tamano}
        className="-rotate-90 overflow-visible"
      >
        <defs>
          <linearGradient id={`anillo-${id}`} x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor={claro} />
            <stop offset="100%" stopColor={oscuro} />
          </linearGradient>
          <Resplandor id={`luz-${id}`} color={oscuro} fuerza={3.5} />
        </defs>
        <circle
          cx={tamano / 2}
          cy={tamano / 2}
          r={radio}
          fill="none"
          stroke={plano ? "var(--color-borde)" : PISTA}
          strokeWidth={grosor}
        />
        <circle
          cx={tamano / 2}
          cy={tamano / 2}
          r={radio}
          fill="none"
          stroke={plano ? COLOR_GRAFICO[tono] : `url(#anillo-${id})`}
          strokeWidth={grosor}
          strokeLinecap="round"
          filter={plano ? undefined : `url(#luz-${id})`}
          strokeDasharray={circunferencia}
          strokeDashoffset={circunferencia - (animado / 100) * circunferencia}
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        {centro ?? (
          <span className="cifra cifra-degradada text-[28px]">
            {porcentaje == null ? "—" : `${Math.round(animado)}%`}
          </span>
        )}
        {subtitulo ? (
          <span className="mt-1 text-[10px] font-semibold uppercase tracking-wider text-tinta-tenue">
            {subtitulo}
          </span>
        ) : null}
      </div>
    </div>
  );
}

export function BarraAnimada({
  porcentaje,
  tono = "violeta",
  alto = 10,
  retraso = 0,
  plano = false,
}: {
  porcentaje?: number | null;
  tono?: ClaveTono;
  alto?: number;
  retraso?: number;
  plano?: boolean;
}) {
  const { referencia, visible } = useVisible<HTMLDivElement>();
  const objetivo =
    porcentaje == null ? 0 : Math.min(Math.max(porcentaje, 0), 100);
  const [claro, oscuro] = PALETA[tono];

  return (
    <div
      ref={referencia}
      className={
        "w-full overflow-hidden rounded-full " + (plano ? "" : "shadow-hundido")
      }
      style={{ height: alto, background: plano ? "var(--color-borde)" : PISTA }}
    >
      <div
        className={
          "relative h-full overflow-hidden rounded-full " +
          (plano ? "" : "destello")
        }
        style={{
          width: visible ? `${objetivo}%` : "0%",
          background: plano
            ? COLOR_GRAFICO[tono]
            : `linear-gradient(90deg, ${claro}, ${oscuro})`,
          boxShadow: plano
            ? undefined
            : `inset 0 1px 0 rgba(255,255,255,0.4), 0 1px 6px -1px ${oscuro}80`,
          transition: `width 1.1s cubic-bezier(0.22, 1, 0.36, 1) ${retraso}ms`,
        }}
      />
    </div>
  );
}

export function Columnas({
  barras,
  alto = 120,
}: {
  barras: {
    etiqueta: string;
    valor: number;
    tono?: ClaveTono;
    color?: string;
  }[];
  alto?: number;
}) {
  const { referencia, visible } = useVisible<HTMLDivElement>();
  const maximo = Math.max(...barras.map((barra) => barra.valor), 1);

  return (
    <div
      ref={referencia}
      className="flex items-end gap-2"
      style={{ height: alto }}
    >
      {barras.map((barra, indice) => {
        const [claro, oscuro] = PALETA[barra.tono ?? "violeta"];
        const proporcion = Math.max(
          (barra.valor / maximo) * 100,
          barra.valor > 0 ? 4 : 0,
        );
        return (
          <div
            key={barra.etiqueta}
            className="group flex h-full min-w-0 flex-1 flex-col justify-end gap-1.5"
          >
            <span className="cifra text-center text-[11px] text-tinta-suave tabular-nums">
              {barra.valor.toLocaleString("es-AR")}
            </span>
            <div
              className="w-full rounded-t-lg rounded-b-sm transition-transform duration-300 group-hover:scale-y-[1.03]"
              style={{
                height: visible ? `${proporcion}%` : "0%",
                transformOrigin: "bottom",
                background:
                  barra.color ?? `linear-gradient(180deg, ${claro}, ${oscuro})`,
                boxShadow: barra.color
                  ? undefined
                  : `inset 0 1px 0 rgba(255,255,255,0.35), 0 4px 12px -4px ${oscuro}99`,
                transition: `height 0.95s cubic-bezier(0.22, 1, 0.36, 1) ${indice * 60}ms`,
              }}
            />
            <span className="truncate text-center text-[10px] uppercase tracking-wider text-tinta-tenue">
              {barra.etiqueta}
            </span>
          </div>
        );
      })}
    </div>
  );
}

export function Embudo({
  etapas,
  plano = false,
}: {
  etapas: { etiqueta: string; valor: number; tono?: ClaveTono }[];
  plano?: boolean;
}) {
  const maximo = Math.max(...etapas.map((etapa) => etapa.valor), 1);

  return (
    <ul className="space-y-3.5">
      {etapas.map((etapa, indice) => (
        <li key={etapa.etiqueta}>
          <div className="mb-1.5 flex items-baseline justify-between gap-2">
            <span className="text-xs font-medium text-tinta-media">
              {etapa.etiqueta}
            </span>
            <span className="cifra text-sm text-tinta">
              {etapa.valor.toLocaleString("es-AR")}
            </span>
          </div>
          <BarraAnimada
            plano={plano}
            porcentaje={(etapa.valor / maximo) * 100}
            tono={etapa.tono ?? "violeta"}
            retraso={indice * 80}
          />
        </li>
      ))}
    </ul>
  );
}

export function AnilloApilado({
  segmentos,
  tamano = 184,
  grosor = 20,
  total,
  etiquetaTotal = "documentos",
  plano = false,
}: {
  segmentos: { etiqueta: string; valor: number; color: string }[];
  tamano?: number;
  grosor?: number;
  total: number;
  etiquetaTotal?: string;
  plano?: boolean;
}) {
  const id = useId().replace(/:/g, "");
  const { referencia, visible } = useVisible<HTMLDivElement>();
  const animado = useContador(visible ? total : 0, 1100);
  const radio = (tamano - grosor) / 2;
  const circunferencia = 2 * Math.PI * radio;
  const suma =
    segmentos.reduce((acumulado, segmento) => acumulado + segmento.valor, 0) ||
    1;
  const separacion =
    segmentos.filter((segmento) => segmento.valor > 0).length > 1 ? 2.5 : 0;

  let acumulado = 0;

  return (
    <div
      ref={referencia}
      className="relative inline-flex items-center justify-center"
    >
      <span
        aria-hidden
        className="absolute rounded-full bg-violeta blur-3xl transition-opacity duration-1000"
        style={{
          width: tamano * 0.6,
          height: tamano * 0.6,
          opacity: visible && !plano ? 0.12 : 0,
        }}
      />
      <svg
        width={tamano}
        height={tamano}
        className="-rotate-90 overflow-visible"
      >
        <defs>
          <filter
            id={`sombra-${id}`}
            x="-30%"
            y="-30%"
            width="160%"
            height="160%"
          >
            <feDropShadow
              dx="0"
              dy="2"
              stdDeviation="3"
              floodColor="#1C1444"
              floodOpacity="0.22"
            />
          </filter>
        </defs>
        <circle
          cx={tamano / 2}
          cy={tamano / 2}
          r={radio}
          fill="none"
          stroke={plano ? "var(--color-borde)" : PISTA}
          strokeWidth={grosor}
        />
        <g filter={plano ? undefined : `url(#sombra-${id})`}>
          {segmentos.map((segmento) => {
            const proporcion = segmento.valor / suma;
            const largo = Math.max(proporcion * circunferencia - separacion, 0);
            const desplazamiento = acumulado * circunferencia;
            acumulado += proporcion;
            if (!segmento.valor) {
              return null;
            }
            return (
              <circle
                key={segmento.etiqueta}
                cx={tamano / 2}
                cy={tamano / 2}
                r={radio}
                fill="none"
                stroke={segmento.color}
                strokeWidth={grosor}
                strokeLinecap="round"
                strokeDasharray={`${visible ? largo : 0} ${circunferencia}`}
                strokeDashoffset={-desplazamiento}
                style={{
                  transition:
                    "stroke-dasharray 1.1s cubic-bezier(0.22, 1, 0.36, 1)",
                }}
              />
            );
          })}
        </g>
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <span
          className={
            plano
              ? "cifra text-metrica-compacta text-tinta"
              : "cifra cifra-degradada text-[32px] leading-none"
          }
        >
          {Math.round(animado).toLocaleString("es-AR")}
        </span>
        <span className="mt-1.5 text-[10px] font-semibold uppercase tracking-wider text-tinta-tenue">
          {etiquetaTotal}
        </span>
      </div>
    </div>
  );
}
