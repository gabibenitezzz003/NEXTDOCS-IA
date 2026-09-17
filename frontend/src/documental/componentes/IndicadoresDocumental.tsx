import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import {
  obtenerExcepcionesMotor,
  obtenerResumenDocumental,
  obtenerVencimientos,
} from "../../api/documental";
import { CargandoTarjetas } from "../../componentes/Estados";
import { useIdioma } from "../../contextos/ProveedorIdioma";
import { horasRestantes } from "../dominio";
import {
  IconoCheck,
  IconoDocumentos,
  IconoExcepciones,
  IconoInteligencia,
  IconoReloj,
} from "../../componentes/Iconos";
import type { ComponentType, ReactNode } from "react";

const TONOS_CHIP: Record<string, string> = {
  violeta: "bg-violeta-tenue text-accion-tonal-texto",
  exito: "bg-exito-tenue text-exito-texto",
  alerta: "bg-alerta-tenue text-alerta-texto",
  rojo: "bg-rojo-tenue text-rojo-alto",
  informacion: "bg-informacion-tenue text-informacion",
};

function TarjetaIndicador({
  titulo,
  icono: Icono,
  tono,
  destino,
  children,
  extra,
}: {
  titulo: string;
  icono: ComponentType<{ tamano?: number }>;
  tono: keyof typeof TONOS_CHIP;
  destino: string;
  children: ReactNode;
  extra?: ReactNode;
}) {
  const navegar = useNavigate();
  return (
    <button
      type="button"
      onClick={() => navegar(destino)}
      className="elevar group min-w-0 rounded-metrica border border-borde bg-superficie p-espacio-4 text-left relieve focus-visible:outline-foco"
    >
      <div className="flex items-center justify-between gap-espacio-2">
        <p className="text-micro font-bold uppercase tracking-wider text-tinta-suave">
          {titulo}
        </p>
        <span
          aria-hidden="true"
          className={`inline-flex size-espacio-8 shrink-0 items-center justify-center rounded-control ${TONOS_CHIP[tono]}`}
        >
          <Icono tamano={16} />
        </span>
      </div>
      <div className="mt-espacio-3">{children}</div>
      {extra}
    </button>
  );
}

function Dato({
  valor,
  etiqueta,
  tono = "text-tinta",
}: {
  valor: number | string;
  etiqueta: string;
  tono?: string;
}) {
  return (
    <div>
      <p className={`cifra text-metrica-compacta font-semibold ${tono}`}>
        {valor}
      </p>
      <p className="mt-espacio-1 text-micro uppercase tracking-wider text-tinta-suave">
        {etiqueta}
      </p>
    </div>
  );
}

export function IndicadoresDocumental() {
  const { t } = useIdioma();

  const resumen = useQuery({
    queryKey: ["documental-resumen"],
    queryFn: obtenerResumenDocumental,
    staleTime: 15000,
    refetchInterval: 30000,
  });
  const excepciones = useQuery({
    queryKey: ["documental-excepciones", { estado: "ABIERTA", limite: 200 }],
    queryFn: () => obtenerExcepcionesMotor({ estado: "ABIERTA", limite: 200 }),
    staleTime: 15000,
    refetchInterval: 30000,
  });
  const vencimientos = useQuery({
    queryKey: ["documental-vencimientos", { dias: 30, limite: 200 }],
    queryFn: () => obtenerVencimientos({ dias: 30, limite: 200 }),
    staleTime: 30000,
    refetchInterval: 60000,
  });

  const cargando = resumen.isLoading || excepciones.isLoading;

  const calculado = useMemo(() => {
    const porEstado = resumen.data || {};
    const abiertas = excepciones.data || [];

    const total = Object.values(porEstado).reduce(
      (suma, n) => suma + Number(n || 0),
      0,
    );
    const aprobados = Number(porEstado.APROBADO || 0);
    const observados = Number(porEstado.OBSERVADO || 0);
    const enCurso =
      Number(porEstado.RECIBIDO || 0) +
      Number(porEstado.PROCESANDO || 0) +
      Number(porEstado.EXTRAIDO || 0) +
      Number(porEstado.VALIDADO || 0);

    const resueltos = aprobados + observados + Number(porEstado.RECHAZADO || 0);
    const automatizacion = resueltos
      ? Math.round((aprobados / resueltos) * 100)
      : null;

    const vencidas = abiertas.filter((e) => {
      const horas = horasRestantes(e.vence_en);
      return horas !== null && horas < 0;
    }).length;

    return {
      total,
      aprobados,
      observados,
      enCurso,
      automatizacion,
      abiertas: abiertas.length,
      vencidas,
    };
  }, [resumen.data, excepciones.data]);

  const porVencer = vencimientos.data?.resumen || {
    VENCIDO: 0,
    POR_VENCER: 0,
    VIGENTE: 0,
  };

  if (cargando) {
    return <CargandoTarjetas cantidad={4} />;
  }

  return (
    <div
      className="grid grid-cols-2 gap-espacio-3 lg:grid-cols-5"
      data-testid="documental-indicadores"
    >
      <TarjetaIndicador
        titulo={t("documental.kpi.volumen")}
        icono={IconoDocumentos}
        tono="violeta"
        destino="/documental/bandeja"
      >
        <div className="flex items-end justify-between gap-espacio-3">
          <Dato valor={calculado.total} etiqueta={t("documental.kpi.total")} />
          <Dato
            valor={calculado.enCurso}
            etiqueta={t("documental.kpi.enCurso")}
            tono={calculado.enCurso ? "text-informacion" : "text-tinta-suave"}
          />
        </div>
      </TarjetaIndicador>

      <TarjetaIndicador
        titulo={t("documental.kpi.automatizacion")}
        icono={IconoInteligencia}
        tono="exito"
        destino="/documental/tablero"
        extra={
          calculado.automatizacion === null ? undefined : (
            <div className="mt-espacio-3 h-espacio-2 overflow-hidden rounded-insignia bg-lienzo shadow-hundido">
              <div
                className="barra-progreso h-full rounded-insignia bg-exito"
                style={{ width: `${calculado.automatizacion}%` }}
              />
            </div>
          )
        }
      >
        <div className="flex items-end justify-between gap-espacio-3">
          <Dato
            valor={
              calculado.automatizacion === null
                ? "—"
                : `${calculado.automatizacion}%`
            }
            etiqueta={t("documental.kpi.sinTocar")}
            tono="text-exito-texto"
          />
          <Dato
            valor={calculado.aprobados}
            etiqueta={t("documental.kpi.aprobados")}
          />
        </div>
      </TarjetaIndicador>

      <TarjetaIndicador
        titulo={t("documental.kpi.excepciones")}
        icono={IconoExcepciones}
        tono="alerta"
        destino="/documental/excepciones"
      >
        <div className="flex items-end justify-between gap-espacio-3">
          <Dato
            valor={calculado.abiertas}
            etiqueta={t("documental.kpi.abiertas")}
            tono={calculado.abiertas ? "text-alerta-texto" : "text-tinta-suave"}
          />
          <Dato
            valor={calculado.vencidas}
            etiqueta={t("documental.kpi.vencidas")}
            tono={calculado.vencidas ? "text-rojo-alto" : "text-tinta-suave"}
          />
        </div>
      </TarjetaIndicador>

      <TarjetaIndicador
        titulo={t("documental.kpi.vigencias")}
        icono={IconoReloj}
        tono="rojo"
        destino="/documental/vencimientos"
      >
        <div className="flex items-end justify-between gap-espacio-3">
          <Dato
            valor={porVencer.VENCIDO || 0}
            etiqueta={t("documental.vencimientos.VENCIDO")}
            tono={porVencer.VENCIDO ? "text-rojo-alto" : "text-tinta-suave"}
          />
          <Dato
            valor={porVencer.POR_VENCER || 0}
            etiqueta={t("documental.kpi.porVencer")}
            tono={
              porVencer.POR_VENCER ? "text-alerta-texto" : "text-tinta-suave"
            }
          />
        </div>
      </TarjetaIndicador>

      <TarjetaIndicador
        titulo={t("documental.kpi.resolucion")}
        icono={IconoCheck}
        tono="informacion"
        destino="/documental/excepciones"
      >
        <div className="flex items-end justify-between gap-espacio-3">
          <Dato
            valor={calculado.observados}
            etiqueta={t("documental.kpi.observados")}
            tono={
              calculado.observados ? "text-alerta-texto" : "text-tinta-suave"
            }
          />
        </div>
      </TarjetaIndicador>
    </div>
  );
}
