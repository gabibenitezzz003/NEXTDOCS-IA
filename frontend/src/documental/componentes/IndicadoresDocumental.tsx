import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  obtenerExcepcionesMotor,
  obtenerResumenDocumental,
  obtenerVencimientos,
} from "../../api/documental";
import { Tarjeta } from "../../componentes/Interfaz";
import { CargandoTarjetas } from "../../componentes/Estados";
import { useIdioma } from "../../contextos/ProveedorIdioma";
import { horasRestantes } from "../dominio";

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
      <p className="text-micro uppercase tracking-wider text-tinta-suave">
        {etiqueta}
      </p>
      <p className={`mt-espacio-1 text-metrica-compacta font-semibold ${tono}`}>
        {valor}
      </p>
    </div>
  );
}

export function IndicadoresDocumental() {
  const { t } = useIdioma();

  const resumen = useQuery({
    queryKey: ["documental-resumen"],
    queryFn: obtenerResumenDocumental,
    refetchInterval: 15000,
  });
  const excepciones = useQuery({
    queryKey: ["documental-excepciones", { estado: "ABIERTA", limite: 200 }],
    queryFn: () => obtenerExcepcionesMotor({ estado: "ABIERTA", limite: 200 }),
    refetchInterval: 15000,
  });
  const vencimientos = useQuery({
    queryKey: ["documental-vencimientos", { dias: 30, limite: 200 }],
    queryFn: () => obtenerVencimientos({ dias: 30, limite: 200 }),
    refetchInterval: 15000,
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
      <Tarjeta padding="p-espacio-4">
        <p className="text-micro font-bold uppercase tracking-wider text-tinta-suave">
          {t("documental.kpi.volumen")}
        </p>
        <div className="mt-espacio-3 flex items-end justify-between gap-espacio-3">
          <Dato valor={calculado.total} etiqueta={t("documental.kpi.total")} />
          <Dato
            valor={calculado.enCurso}
            etiqueta={t("documental.kpi.enCurso")}
            tono={calculado.enCurso ? "text-informacion" : "text-tinta-suave"}
          />
        </div>
      </Tarjeta>

      <Tarjeta padding="p-espacio-4">
        <p className="text-micro font-bold uppercase tracking-wider text-tinta-suave">
          {t("documental.kpi.automatizacion")}
        </p>
        <div className="mt-espacio-3 flex items-end justify-between gap-espacio-3">
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
        {calculado.automatizacion === null ? null : (
          <div className="mt-espacio-3 h-espacio-2 overflow-hidden rounded-insignia bg-lienzo">
            <div
              className="h-full rounded-insignia bg-exito transition-[width] duration-200 motion-reduce:transition-none"
              style={{ width: `${calculado.automatizacion}%` }}
            />
          </div>
        )}
      </Tarjeta>

      <Tarjeta padding="p-espacio-4">
        <p className="text-micro font-bold uppercase tracking-wider text-tinta-suave">
          {t("documental.kpi.excepciones")}
        </p>
        <div className="mt-espacio-3 flex items-end justify-between gap-espacio-3">
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
      </Tarjeta>

      <Tarjeta padding="p-espacio-4">
        <p className="text-micro font-bold uppercase tracking-wider text-tinta-suave">
          {t("documental.kpi.vigencias")}
        </p>
        <div className="mt-espacio-3 flex items-end justify-between gap-espacio-3">
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
      </Tarjeta>

      <Tarjeta padding="p-espacio-4">
        <p className="text-micro font-bold uppercase tracking-wider text-tinta-suave">
          {t("documental.kpi.resolucion")}
        </p>
        <div className="mt-espacio-3 flex items-end justify-between gap-espacio-3">
          <Dato
            valor={calculado.observados}
            etiqueta={t("documental.kpi.observados")}
            tono={
              calculado.observados ? "text-alerta-texto" : "text-tinta-suave"
            }
          />
        </div>
      </Tarjeta>
    </div>
  );
}
