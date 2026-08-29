import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Encabezado } from "../componentes/Disposicion";
import { Cargando, ErrorPanel, Vacio } from "../componentes/Estados";
import { InsigniaEstado } from "../componentes/Insignias";
import { listarKpiPorPlantilla, obtenerKpi, obtenerPoblacionKpi } from "../api/kpi";
import { mensajeDeError } from "../api/cliente";
import type { BarraKpi, IndicadorKpi, KpiPlantilla, SaludPlantilla, SemaforoKpi } from "../tipos/api";

const VENTANAS = [
  { dias: 7, texto: "7 dias" },
  { dias: 30, texto: "30 dias" },
  { dias: 90, texto: "90 dias" },
];

const TONO_SEMAFORO: Record<SemaforoKpi, string> = {
  VERDE: "bg-exito",
  AMBAR: "bg-alerta",
  ROJO: "bg-rojo",
  SIN_DATOS: "bg-borde",
};

const TONO_SALUD: Record<SaludPlantilla, string> = {
  OK: "bg-exito-tenue text-exito",
  ATENCION: "bg-alerta-tenue text-alerta",
  CRITICO: "bg-rojo-tenue text-rojo",
  SIN_DATOS: "bg-lienzo text-tinta-suave",
};

export function Panel() {
  const [dias, setDias] = useState(30);
  const [indicadorAbierto, setIndicadorAbierto] = useState<IndicadorKpi | null>(null);

  const rango = { desde: new Date(Date.now() - dias * 86400000).toISOString() };

  const resumen = useQuery({ queryKey: ["kpi", "resumen", dias], queryFn: () => obtenerKpi(rango) });
  const plantillas = useQuery({
    queryKey: ["kpi", "plantillas", dias],
    queryFn: () => listarKpiPorPlantilla(rango),
  });

  return (
    <>
      <Encabezado
        titulo="Panel de control"
        descripcion="Cada indicador muestra la formula con la que se calcula y, cuando aplica, la poblacion que lo compone."
        acciones={
          <div className="flex rounded-lg border border-borde bg-white p-0.5">
            {VENTANAS.map((ventana) => (
              <button
                key={ventana.dias}
                type="button"
                onClick={() => setDias(ventana.dias)}
                className={`rounded-md px-3 py-1.5 text-xs font-medium transition ${
                  dias === ventana.dias ? "bg-violeta text-white" : "text-tinta-suave hover:text-tinta"
                }`}
              >
                {ventana.texto}
              </button>
            ))}
          </div>
        }
      />

      <div className="px-8 py-6">
        {resumen.isPending ? (
          <Cargando filas={4} />
        ) : resumen.isError ? (
          <ErrorPanel mensaje={mensajeDeError(resumen.error)} reintentar={() => resumen.refetch()} />
        ) : (
          <>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4">
              {resumen.data.indicadores.map((indicador) => (
                <TarjetaIndicador
                  key={indicador.clave}
                  indicador={indicador}
                  alAbrir={() => setIndicadorAbierto(indicador)}
                />
              ))}
            </div>

            <section className="mt-8">
              <div className="flex items-end justify-between">
                <div>
                  <h2 className="font-titulo text-lg text-tinta">Salud por plantilla</h2>
                  <p className="mt-1 text-sm text-tinta-suave">
                    Volumen y avance de los documentos recibidos en la ventana elegida.
                  </p>
                </div>
                <p className="text-xs text-tinta-suave">
                  Ventana: {formatearFecha(resumen.data.rango.desde)} a {formatearFecha(resumen.data.rango.hasta)}
                </p>
              </div>

              {plantillas.isPending ? (
                <div className="mt-4">
                  <Cargando filas={2} />
                </div>
              ) : plantillas.isError ? (
                <div className="mt-4">
                  <ErrorPanel
                    mensaje={mensajeDeError(plantillas.error)}
                    reintentar={() => plantillas.refetch()}
                  />
                </div>
              ) : !plantillas.data.length ? (
                <div className="mt-4">
                  <Vacio
                    titulo="Sin documentos en esta ventana"
                    detalle="Ampliá el rango o ingresá documentos para ver el panel por plantilla."
                    accion={
                      <Link
                        to="/documentos"
                        className="rounded-lg bg-violeta px-4 py-2 text-sm font-medium text-white transition hover:bg-violeta-claro"
                      >
                        Ir a documentos
                      </Link>
                    }
                  />
                </div>
              ) : (
                <div className="mt-4 grid gap-4 lg:grid-cols-2">
                  {plantillas.data.map((plantilla) => (
                    <TarjetaPlantilla key={plantilla.codigo} plantilla={plantilla} />
                  ))}
                </div>
              )}
            </section>
          </>
        )}
      </div>

      {indicadorAbierto ? (
        <PanelPoblacion
          indicador={indicadorAbierto}
          rango={rango}
          alCerrar={() => setIndicadorAbierto(null)}
        />
      ) : null}
    </>
  );
}

function TarjetaIndicador({
  indicador,
  alAbrir,
}: {
  indicador: IndicadorKpi;
  alAbrir: () => void;
}) {
  const contenido = (
    <>
      <div className="flex items-start justify-between gap-2">
        <p className="text-xs uppercase tracking-wide text-tinta-suave">{indicador.etiqueta}</p>
        {indicador.tienePoblacion ? (
          <span className="shrink-0 rounded-full bg-violeta-tenue px-2 py-0.5 text-[10px] font-medium text-violeta">
            ver detalle
          </span>
        ) : null}
      </div>
      <p className="mt-2 font-titulo text-3xl text-tinta">{formatearValor(indicador)}</p>
      <div className="mt-1 flex items-center gap-2 text-xs text-tinta-suave">
        <Variacion indicador={indicador} />
        {indicador.denominador ? (
          <span className="tabular-nums">
            {indicador.numerador?.toLocaleString("es-AR")} de {indicador.denominador.toLocaleString("es-AR")}
          </span>
        ) : null}
      </div>
      <p className="mt-3 border-t border-borde pt-2 text-[11px] leading-snug text-tinta-suave">
        {indicador.detalle ?? indicador.formula}
      </p>
    </>
  );

  if (!indicador.tienePoblacion) {
    return <article className="rounded-xl border border-borde bg-white px-5 py-4">{contenido}</article>;
  }

  return (
    <button
      type="button"
      onClick={alAbrir}
      className="rounded-xl border border-borde bg-white px-5 py-4 text-left transition hover:border-violeta hover:shadow-[0_1px_12px_rgba(108,59,255,0.12)]"
    >
      {contenido}
    </button>
  );
}

function Variacion({ indicador }: { indicador: IndicadorKpi }) {
  if (indicador.tendencia === "SIN_COMPARACION" || indicador.variacion == null) {
    return <span>Sin periodo anterior comparable</span>;
  }
  const flecha = indicador.tendencia === "SUBE" ? "↑" : indicador.tendencia === "BAJA" ? "↓" : "→";
  return (
    <span className="tabular-nums">
      {flecha} {Math.abs(indicador.variacion).toLocaleString("es-AR")}% vs periodo anterior
    </span>
  );
}

function TarjetaPlantilla({ plantilla }: { plantilla: KpiPlantilla }) {
  return (
    <article className="rounded-xl border border-borde bg-white p-5">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h3 className="truncate font-titulo text-base text-tinta">{plantilla.nombre ?? plantilla.codigo}</h3>
          <p className="mt-0.5 text-xs text-tinta-suave">
            {plantilla.volumen.toLocaleString("es-AR")} documentos ·{" "}
            {plantilla.documentosConExcepciones.toLocaleString("es-AR")} con excepciones abiertas
          </p>
        </div>
        <span
          className={`shrink-0 rounded-full px-2.5 py-1 text-[11px] font-medium ${TONO_SALUD[plantilla.salud]}`}
        >
          {plantilla.salud}
        </span>
      </div>

      <ul className="mt-4 space-y-3">
        {plantilla.barras.map((barra) => (
          <Barra key={barra.clave} barra={barra} />
        ))}
      </ul>

      <div className="mt-4 flex flex-wrap gap-1.5 border-t border-borde pt-3">
        {Object.entries(plantilla.porEstado)
          .sort((uno, otro) => otro[1] - uno[1])
          .map(([estado, cantidad]) => (
            <span key={estado} className="flex items-center gap-1">
              <InsigniaEstado estado={estado as never} />
              <span className="text-xs tabular-nums text-tinta-suave">{cantidad}</span>
            </span>
          ))}
      </div>
    </article>
  );
}

function Barra({ barra }: { barra: BarraKpi }) {
  return (
    <li>
      <div className="flex items-baseline justify-between gap-2">
        <span className="text-xs text-tinta">{barra.etiqueta}</span>
        <span className="text-xs tabular-nums text-tinta-suave">
          {barra.porcentaje == null ? "sin datos" : `${barra.porcentaje.toLocaleString("es-AR")}%`}
        </span>
      </div>
      <div className="mt-1 h-2 overflow-hidden rounded-full bg-lienzo">
        <div
          className={`h-full transition-[width] ${TONO_SEMAFORO[barra.semaforo]}`}
          style={{ width: `${barra.porcentaje ?? 0}%` }}
        />
      </div>
      <p className="mt-1 text-[11px] leading-snug text-tinta-suave">{barra.formula}</p>
    </li>
  );
}

function PanelPoblacion({
  indicador,
  rango,
  alCerrar,
}: {
  indicador: IndicadorKpi;
  rango: { desde?: string };
  alCerrar: () => void;
}) {
  const poblacion = useQuery({
    queryKey: ["kpi", "poblacion", indicador.clave, rango.desde],
    queryFn: () => obtenerPoblacionKpi(indicador.clave, rango),
  });

  return (
    <div className="fixed inset-0 z-40 flex justify-end bg-grafito/40" onClick={alCerrar}>
      <aside
        className="flex h-full w-full max-w-xl flex-col bg-white shadow-2xl"
        onClick={(evento) => evento.stopPropagation()}
      >
        <header className="flex items-start justify-between gap-4 border-b border-borde px-6 py-5">
          <div>
            <h2 className="font-titulo text-lg text-tinta">{indicador.etiqueta}</h2>
            <p className="mt-1 text-xs text-tinta-suave">{indicador.formula}</p>
          </div>
          <button
            type="button"
            onClick={alCerrar}
            className="rounded-lg border border-borde px-3 py-1.5 text-xs text-tinta-suave transition hover:text-tinta"
          >
            Cerrar
          </button>
        </header>

        <div className="flex-1 overflow-y-auto px-6 py-5">
          {poblacion.isPending ? (
            <Cargando filas={5} />
          ) : poblacion.isError ? (
            <ErrorPanel mensaje={mensajeDeError(poblacion.error)} reintentar={() => poblacion.refetch()} />
          ) : !poblacion.data.length ? (
            <Vacio titulo="Sin documentos" detalle="Este indicador no tiene documentos en la ventana elegida." />
          ) : (
            <>
              <p className="text-xs text-tinta-suave">
                {poblacion.data.length.toLocaleString("es-AR")} documentos componen este indicador.
              </p>
              <ul className="mt-3 space-y-2">
                {poblacion.data.map((documento) => (
                  <li key={documento.id} className="rounded-lg border border-borde px-3 py-2.5">
                    <div className="flex items-center justify-between gap-2">
                      <span className="truncate text-sm text-tinta">
                        {documento.nombre ?? documento.id}
                      </span>
                      <InsigniaEstado estado={documento.estado} />
                    </div>
                    <p className="mt-1 text-xs text-tinta-suave">
                      {documento.codigoPlantilla ?? "sin plantilla"} · recibido{" "}
                      {formatearFecha(documento.recibido)}
                    </p>
                  </li>
                ))}
              </ul>
            </>
          )}
        </div>
      </aside>
    </div>
  );
}

function formatearValor(indicador: IndicadorKpi) {
  if (indicador.valor == null) {
    return "Sin datos";
  }
  if (indicador.unidad === "PORCENTAJE") {
    return `${indicador.valor.toLocaleString("es-AR")}%`;
  }
  if (indicador.unidad === "HORAS") {
    return formatearHoras(indicador.valor);
  }
  return indicador.valor.toLocaleString("es-AR");
}

function formatearHoras(horas: number) {
  if (horas < 1) {
    return "menos de 1 h";
  }
  if (horas < 24) {
    return `${Math.round(horas)} h`;
  }
  const dias = Math.floor(horas / 24);
  const resto = Math.round(horas % 24);
  return resto ? `${dias} d ${resto} h` : `${dias} d`;
}

function formatearFecha(valor?: string) {
  if (!valor) {
    return "sin fecha";
  }
  return new Date(valor).toLocaleDateString("es-AR", { day: "2-digit", month: "short", year: "numeric" });
}
