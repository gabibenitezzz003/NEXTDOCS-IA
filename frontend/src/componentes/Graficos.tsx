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

export function useVisible<T extends HTMLElement>() {
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

export function Anillo({
  porcentaje,
  tono = "violeta",
  tamano = 132,
  grosor = 12,
  centro,
  subtitulo,
}: {
  porcentaje: number | null;
  tono?: ClaveTono;
  tamano?: number;
  grosor?: number;
  centro?: ReactNode;
  subtitulo?: string;
}) {
  const id = useId();
  const { referencia, visible } = useVisible<HTMLDivElement>();
  const objetivo = porcentaje == null ? 0 : Math.min(Math.max(porcentaje, 0), 100);
  const animado = useContador(visible ? objetivo : 0, 1100);

  const radio = (tamano - grosor) / 2;
  const circunferencia = 2 * Math.PI * radio;
  const [claro, oscuro] = PALETA[tono];

  return (
    <div ref={referencia} className="relative inline-flex items-center justify-center">
      <svg width={tamano} height={tamano} className="-rotate-90">
        <defs>
          <linearGradient id={`anillo-${id}`} x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor={claro} />
            <stop offset="100%" stopColor={oscuro} />
          </linearGradient>
        </defs>
        <circle
          cx={tamano / 2}
          cy={tamano / 2}
          r={radio}
          fill="none"
          stroke="#EDEFF4"
          strokeWidth={grosor}
        />
        <circle
          cx={tamano / 2}
          cy={tamano / 2}
          r={radio}
          fill="none"
          stroke={`url(#anillo-${id})`}
          strokeWidth={grosor}
          strokeLinecap="round"
          strokeDasharray={circunferencia}
          strokeDashoffset={circunferencia - (animado / 100) * circunferencia}
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        {centro ?? (
          <span className="cifra text-2xl text-tinta">
            {porcentaje == null ? "—" : `${Math.round(animado)}%`}
          </span>
        )}
        {subtitulo ? (
          <span className="mt-0.5 text-[10px] font-semibold uppercase tracking-wider text-tinta-tenue">
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
  alto = 8,
  retraso = 0,
}: {
  porcentaje?: number | null;
  tono?: ClaveTono;
  alto?: number;
  retraso?: number;
}) {
  const { referencia, visible } = useVisible<HTMLDivElement>();
  const objetivo = porcentaje == null ? 0 : Math.min(Math.max(porcentaje, 0), 100);
  const [claro, oscuro] = PALETA[tono];

  return (
    <div
      ref={referencia}
      className="w-full overflow-hidden rounded-full bg-[#EDEFF4]"
      style={{ height: alto }}
    >
      <div
        className="h-full rounded-full"
        style={{
          width: visible ? `${objetivo}%` : "0%",
          backgroundImage: `linear-gradient(90deg, ${claro}, ${oscuro})`,
          transition: `width 1s cubic-bezier(0.22, 1, 0.36, 1) ${retraso}ms`,
        }}
      />
    </div>
  );
}

export function Chispa({
  valores,
  tono = "violeta",
  ancho = 132,
  alto = 40,
}: {
  valores: number[];
  tono?: ClaveTono;
  ancho?: number;
  alto?: number;
}) {
  const id = useId();
  const [claro, oscuro] = PALETA[tono];

  if (valores.length < 2) {
    return <div style={{ width: ancho, height: alto }} />;
  }

  const maximo = Math.max(...valores);
  const minimo = Math.min(...valores);
  const rango = maximo - minimo || 1;
  const margen = 3;

  const puntos = valores.map((valor, indice) => {
    const x = (indice / (valores.length - 1)) * ancho;
    const y = alto - margen - ((valor - minimo) / rango) * (alto - margen * 2);
    return [x, y] as const;
  });

  const linea = puntos
    .map(([x, y], indice) => {
      if (indice === 0) {
        return `M${x.toFixed(1)},${y.toFixed(1)}`;
      }
      const [xPrevio, yPrevio] = puntos[indice - 1];
      const control = (x - xPrevio) / 2;
      return `C${(xPrevio + control).toFixed(1)},${yPrevio.toFixed(1)} ${(x - control).toFixed(1)},${y.toFixed(1)} ${x.toFixed(1)},${y.toFixed(1)}`;
    })
    .join(" ");

  const area = `${linea} L${ancho},${alto} L0,${alto} Z`;
  const [ultimoX, ultimoY] = puntos[puntos.length - 1];

  return (
    <svg width={ancho} height={alto} className="overflow-visible">
      <defs>
        <linearGradient id={`chispa-${id}`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={claro} stopOpacity="0.28" />
          <stop offset="100%" stopColor={claro} stopOpacity="0" />
        </linearGradient>
      </defs>
      <path d={area} fill={`url(#chispa-${id})`} />
      <path
        d={linea}
        fill="none"
        stroke={oscuro}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle cx={ultimoX} cy={ultimoY} r="3.2" fill={oscuro} stroke="#fff" strokeWidth="1.6" />
    </svg>
  );
}

export function Embudo({
  etapas,
}: {
  etapas: { etiqueta: string; valor: number; tono?: ClaveTono }[];
}) {
  const maximo = Math.max(...etapas.map((etapa) => etapa.valor), 1);

  return (
    <ul className="space-y-3">
      {etapas.map((etapa, indice) => (
        <li key={etapa.etiqueta}>
          <div className="mb-1.5 flex items-baseline justify-between gap-2">
            <span className="text-xs font-medium text-tinta-media">{etapa.etiqueta}</span>
            <span className="cifra text-sm text-tinta">{etapa.valor.toLocaleString("es-AR")}</span>
          </div>
          <BarraAnimada
            porcentaje={(etapa.valor / maximo) * 100}
            tono={etapa.tono ?? "violeta"}
            retraso={indice * 70}
          />
        </li>
      ))}
    </ul>
  );
}

export function AnilloApilado({
  segmentos,
  tamano = 168,
  grosor = 16,
  total,
  etiquetaTotal = "documentos",
}: {
  segmentos: { etiqueta: string; valor: number; color: string }[];
  tamano?: number;
  grosor?: number;
  total: number;
  etiquetaTotal?: string;
}) {
  const { referencia, visible } = useVisible<HTMLDivElement>();
  const animado = useContador(visible ? total : 0, 1000);
  const radio = (tamano - grosor) / 2;
  const circunferencia = 2 * Math.PI * radio;
  const suma = segmentos.reduce((acumulado, segmento) => acumulado + segmento.valor, 0) || 1;

  let acumulado = 0;

  return (
    <div ref={referencia} className="relative inline-flex items-center justify-center">
      <svg width={tamano} height={tamano} className="-rotate-90">
        <circle
          cx={tamano / 2}
          cy={tamano / 2}
          r={radio}
          fill="none"
          stroke="#EDEFF4"
          strokeWidth={grosor}
        />
        {segmentos.map((segmento) => {
          const proporcion = segmento.valor / suma;
          const largo = proporcion * circunferencia;
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
              strokeLinecap="butt"
              strokeDasharray={`${visible ? largo : 0} ${circunferencia}`}
              strokeDashoffset={-desplazamiento}
              style={{ transition: "stroke-dasharray 1s cubic-bezier(0.22, 1, 0.36, 1)" }}
            />
          );
        })}
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <span className="cifra text-[28px] leading-none text-tinta">
          {Math.round(animado).toLocaleString("es-AR")}
        </span>
        <span className="mt-1 text-[10px] font-semibold uppercase tracking-wider text-tinta-tenue">
          {etiquetaTotal}
        </span>
      </div>
    </div>
  );
}
