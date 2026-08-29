import type { RefObject } from "react";
import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Contenido, Encabezado } from "../componentes/Disposicion";
import { Cargando, CargandoTarjetas, ErrorPanel, Vacio } from "../componentes/Estados";
import { InsigniaEstado } from "../componentes/Insignias";
import {
  Boton,
  CabeceraTarjeta,
  GrupoSegmentado,
  Panel as PanelLateral,
  Pastilla,
  Tarjeta,
} from "../componentes/Interfaz";
import {
  Anillo,
  AnilloApilado,
  BarraAnimada,
  Embudo,
  useContador,
  useVisible,
} from "../componentes/Graficos";
import type { ClaveTono } from "../componentes/Graficos";
import {
  IconoDerecha,
  IconoFlechaAbajo,
  IconoFlechaArriba,
  IconoIgual,
  IconoInfo,
} from "../componentes/Iconos";
import { listarKpiPorPlantilla, obtenerKpi, obtenerPoblacionKpi } from "../api/kpi";
import { mensajeDeError } from "../api/cliente";
import { formatearFecha } from "./Documentos";
import type { BarraKpi, IndicadorKpi, KpiPlantilla, SaludPlantilla, SemaforoKpi } from "../tipos/api";

const VENTANAS = [
  { valor: 7, texto: "7 dias" },
  { valor: 30, texto: "30 dias" },
  { valor: 90, texto: "90 dias" },
];

const DESTACADOS = ["documentosRecibidos", "automatizacion", "cumplimientoSla"];

const TONO_SEMAFORO: Record<SemaforoKpi, ClaveTono> = {
  VERDE: "exito",
  AMBAR: "alerta",
  ROJO: "rojo",
  SIN_DATOS: "neutro",
};

const ESTILO_SALUD: Record<SaludPlantilla, string> = {
  OK: "bg-exito-tenue text-exito ring-exito-borde",
  ATENCION: "bg-alerta-tenue text-alerta ring-alerta-borde",
  CRITICO: "bg-rojo-tenue text-rojo ring-rojo-borde",
  SIN_DATOS: "bg-lienzo text-tinta-suave ring-borde",
};

const COLOR_ESTADO: Record<string, string> = {
  RECIBIDO: "#9AA1B1",
  PROCESANDO: "#8A63FF",
  EXTRAIDO: "#6C38FF",
  VALIDADO: "#1D6FE0",
  OBSERVADO: "#C2760A",
  APROBADO: "#0F9D58",
  RECHAZADO: "#FF1E1E",
  CERRADO: "#3D4453",
  DIVIDIDO: "#5EA0F2",
};

const ORDEN_EMBUDO = ["RECIBIDO", "PROCESANDO", "EXTRAIDO", "VALIDADO", "APROBADO", "CERRADO"];

export function Panel() {
  const [dias, setDias] = useState(30);
  const [indicadorAbierto, setIndicadorAbierto] = useState<IndicadorKpi | null>(null);

  const rango = useMemo(
    () => ({ desde: new Date(Date.now() - dias * 86400000).toISOString() }),
    [dias],
  );

  const resumen = useQuery({ queryKey: ["kpi", "resumen", dias], queryFn: () => obtenerKpi(rango) });
  const plantillas = useQuery({
    queryKey: ["kpi", "plantillas", dias],
    queryFn: () => listarKpiPorPlantilla(rango),
  });

  const indicadores = resumen.data?.indicadores ?? [];
  const buscar = (clave: string) => indicadores.find((indicador) => indicador.clave === clave);
  const secundarios = indicadores.filter((indicador) => !DESTACADOS.includes(indicador.clave));

  const recibidos = buscar("documentosRecibidos");
  const automatizacion = buscar("automatizacion");
  const sla = buscar("cumplimientoSla");

  const porEstado = resumen.data?.porEstado ?? {};
  const segmentos = Object.entries(porEstado)
    .filter(([, cantidad]) => cantidad > 0)
    .sort((uno, otro) => otro[1] - uno[1])
    .map(([estado, cantidad]) => ({
      etiqueta: estado,
      valor: cantidad,
      color: COLOR_ESTADO[estado] ?? "#9AA1B1",
    }));
  const totalBacklog = segmentos.reduce((suma, segmento) => suma + segmento.valor, 0);

  const embudo = ORDEN_EMBUDO.filter((estado) => porEstado[estado] != null).map((estado) => ({
    etiqueta: estado,
    valor: porEstado[estado] ?? 0,
    tono: (estado === "APROBADO" ? "exito" : estado === "CERRADO" ? "neutro" : "violeta") as ClaveTono,
  }));

  return (
    <>
      <Encabezado
        titulo="Panel de control"
        descripcion="Cada indicador expone la formula con la que se calcula y, cuando aplica, la poblacion exacta que lo compone."
        acciones={
          <GrupoSegmentado
            opciones={VENTANAS.map((ventana) => ({ valor: ventana.valor, texto: ventana.texto }))}
            valor={dias}
            alCambiar={setDias}
          />
        }
      />

      <Contenido>
        {resumen.isPending ? (
          <CargandoTarjetas cantidad={3} />
        ) : resumen.isError ? (
          <ErrorPanel mensaje={mensajeDeError(resumen.error)} reintentar={() => resumen.refetch()} />
        ) : (
          <>
            <div className="grid gap-5 lg:grid-cols-[1.15fr_1fr_1fr]">
              {recibidos ? (
                <TarjetaHeroe
                  indicador={recibidos}
                  rango={resumen.data.rango}
                  alAbrir={() => setIndicadorAbierto(recibidos)}
                />
              ) : null}
              {automatizacion ? <TarjetaAnillo indicador={automatizacion} tono="violeta" /> : null}
              {sla ? <TarjetaAnillo indicador={sla} tono="exito" /> : null}
            </div>

            <div className="mt-5 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              {secundarios.map((indicador) => (
                <TarjetaIndicador
                  key={indicador.clave}
                  indicador={indicador}
                  alAbrir={() => setIndicadorAbierto(indicador)}
                />
              ))}
            </div>

            <section className="mt-6 grid gap-5 lg:grid-cols-[1fr_1fr]">
              <Tarjeta>
                <CabeceraTarjeta
                  titulo="Embudo del ciclo documental"
                  descripcion="Backlog actual del tenant por etapa, sin recorte de fechas."
                />
                <div className="mt-5">
                  {embudo.length ? (
                    <Embudo etapas={embudo} />
                  ) : (
                    <p className="text-sm text-tinta-suave">Todavia no hay documentos.</p>
                  )}
                </div>
              </Tarjeta>

              <Tarjeta>
                <CabeceraTarjeta
                  titulo="Composicion del backlog"
                  descripcion="Cada estado sobre el total de documentos vivos."
                />
                {totalBacklog ? (
                  <div className="mt-5 flex flex-wrap items-center gap-7">
                    <AnilloApilado segmentos={segmentos} total={totalBacklog} />
                    <ul className="min-w-40 flex-1 space-y-2">
                      {segmentos.map((segmento) => (
                        <li key={segmento.etiqueta} className="flex items-center gap-2.5 text-xs">
                          <span
                            className="size-2.5 shrink-0 rounded-sm"
                            style={{ background: segmento.color }}
                          />
                          <span className="flex-1 font-medium text-tinta-media">{segmento.etiqueta}</span>
                          <span className="cifra text-tinta">{segmento.valor.toLocaleString("es-AR")}</span>
                          <span className="w-9 text-right tabular-nums text-tinta-tenue">
                            {Math.round((segmento.valor / totalBacklog) * 100)}%
                          </span>
                        </li>
                      ))}
                    </ul>
                  </div>
                ) : (
                  <p className="mt-5 text-sm text-tinta-suave">Todavia no hay documentos.</p>
                )}
              </Tarjeta>
            </section>

            <section className="mt-6">
              <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
                <div>
                  <h2 className="font-titulo text-lg text-tinta">Salud por plantilla</h2>
                  <p className="mt-0.5 text-sm text-tinta-suave">
                    Priorizacion operativa por avance, documentacion, automatizacion y excepciones.
                  </p>
                </div>
                <p className="text-xs text-tinta-tenue">
                  {formatearFecha(resumen.data.rango.desde)} — {formatearFecha(resumen.data.rango.hasta)}
                </p>
              </div>

              {plantillas.isPending ? (
                <Cargando filas={3} alto="h-32" />
              ) : plantillas.isError ? (
                <ErrorPanel
                  mensaje={mensajeDeError(plantillas.error)}
                  reintentar={() => plantillas.refetch()}
                />
              ) : !plantillas.data.length ? (
                <Vacio
                  titulo="Sin documentos en esta ventana"
                  detalle="Ampliá el rango o ingresá documentos para ver el panel por plantilla."
                  accion={
                    <Link to="/documentos">
                      <Boton variante="primario">Ir a documentos</Boton>
                    </Link>
                  }
                />
              ) : (
                <div className="space-y-3">
                  {plantillas.data.map((plantilla, indice) => (
                    <FilaPlantilla key={plantilla.codigo} plantilla={plantilla} indice={indice} />
                  ))}
                </div>
              )}
            </section>
          </>
        )}
      </Contenido>

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

function TarjetaHeroe({
  indicador,
  rango,
  alAbrir,
}: {
  indicador: IndicadorKpi;
  rango: { desde: string; hasta: string; dias: number };
  alAbrir: () => void;
}) {
  const { referencia, visible } = useVisible<HTMLDivElement>();
  const animado = useContador(visible ? (indicador.valor ?? 0) : 0);
  const actual = indicador.valor ?? 0;
  const anterior = indicador.valorAnterior ?? 0;
  const maximo = Math.max(actual, anterior, 1);

  return (
    <article
      ref={referencia}
      className="superficie-oscura relative overflow-hidden rounded-2xl p-6 shadow-elevado"
    >
      <div
        className="pointer-events-none absolute -right-16 -top-16 size-52 rounded-full opacity-40 blur-3xl"
        style={{ background: "radial-gradient(circle, #6C38FF 0%, transparent 70%)" }}
      />

      <div className="relative">
        <div className="flex items-start justify-between gap-3">
          <p className="text-[11px] font-semibold uppercase tracking-wider text-white/50">
            {indicador.etiqueta}
          </p>
          <Pastilla tono="violeta" className="bg-white/10 text-violeta-claro ring-white/15">
            {rango.dias} dias
          </Pastilla>
        </div>

        <p className="cifra mt-3 text-[56px] leading-none text-white">
          {Math.round(animado).toLocaleString("es-AR")}
        </p>

        <div className="mt-6 space-y-2.5">
          {[
            { etiqueta: "Este periodo", valor: actual, fuerte: true },
            { etiqueta: "Periodo anterior", valor: anterior, fuerte: false },
          ].map((fila) => (
            <div key={fila.etiqueta} className="flex items-center gap-3">
              <span className="w-28 shrink-0 text-[11px] text-white/45">{fila.etiqueta}</span>
              <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-white/10">
                <div
                  className={`h-full rounded-full ${fila.fuerte ? "degradado-marca" : "bg-white/25"}`}
                  style={{
                    width: visible ? `${(fila.valor / maximo) * 100}%` : "0%",
                    transition: "width 1s cubic-bezier(0.22, 1, 0.36, 1)",
                  }}
                />
              </div>
              <span className="cifra w-12 shrink-0 text-right text-sm text-white/85">
                {fila.valor.toLocaleString("es-AR")}
              </span>
            </div>
          ))}
        </div>

        <div className="mt-5 flex items-center justify-between gap-3 border-t border-white/10 pt-4">
          <Tendencia indicador={indicador} claro />
          {indicador.tienePoblacion ? (
            <button
              type="button"
              onClick={alAbrir}
              className="inline-flex items-center gap-1 rounded-lg px-2 py-1 text-xs font-semibold text-white/70 transition hover:bg-white/10 hover:text-white"
            >
              Ver poblacion
              <IconoDerecha tamano={13} />
            </button>
          ) : null}
        </div>
      </div>
    </article>
  );
}

function TarjetaAnillo({ indicador, tono }: { indicador: IndicadorKpi; tono: ClaveTono }) {
  const sinDatos = indicador.valor == null;

  return (
    <Tarjeta className="@container/anillo flex flex-col">
      <CabeceraTarjeta titulo={indicador.etiqueta} />
      <div className="mt-4 flex flex-1 flex-col items-center gap-4 @[300px]/anillo:flex-row @[300px]/anillo:gap-5">
        <Anillo
          porcentaje={indicador.valor ?? null}
          tono={sinDatos ? "neutro" : tono}
          tamano={112}
          grosor={10}
        />
        <div className="min-w-0 flex-1 text-center @[300px]/anillo:text-left">
          {indicador.denominador ? (
            <p className="text-sm text-tinta-media">
              <span className="cifra text-tinta">{indicador.numerador?.toLocaleString("es-AR")}</span>
              <span className="text-tinta-tenue"> de </span>
              <span className="cifra text-tinta">{indicador.denominador.toLocaleString("es-AR")}</span>
            </p>
          ) : (
            <p className="text-sm text-tinta-tenue">Sin base de calculo en este periodo</p>
          )}
          <div className="mt-2">
            <Tendencia indicador={indicador} />
          </div>
        </div>
      </div>
      <p className="mt-4 border-t border-borde pt-3 text-[11px] leading-snug text-tinta-suave">
        {indicador.detalle ?? indicador.formula}
      </p>
    </Tarjeta>
  );
}

function TarjetaIndicador({
  indicador,
  alAbrir,
}: {
  indicador: IndicadorKpi;
  alAbrir: () => void;
}) {
  const { referencia, visible } = useVisible<HTMLElement>();
  const esConteo = indicador.unidad === "CONTEO";
  const animado = useContador(visible && esConteo ? (indicador.valor ?? 0) : 0);

  const contenido = (
    <>
      <div className="flex items-start justify-between gap-2">
        <p className="text-[11px] font-semibold uppercase tracking-wider text-tinta-suave">
          {indicador.etiqueta}
        </p>
        {indicador.tienePoblacion ? (
          <span className="shrink-0 rounded-md bg-violeta-tenue px-1.5 py-0.5 text-[9px] font-bold uppercase tracking-wide text-violeta">
            detalle
          </span>
        ) : null}
      </div>
      <p className="cifra mt-2 text-[32px] leading-none text-tinta">
        {esConteo ? Math.round(animado).toLocaleString("es-AR") : formatearValor(indicador)}
      </p>
      <div className="mt-2">
        <Tendencia indicador={indicador} />
      </div>
      <p className="mt-3 border-t border-borde pt-2.5 text-[11px] leading-snug text-tinta-suave">
        {indicador.detalle ?? indicador.formula}
      </p>
    </>
  );

  if (!indicador.tienePoblacion) {
    return (
      <div
        ref={referencia as RefObject<HTMLDivElement>}
        className="rounded-2xl border border-borde bg-white px-5 py-4 shadow-tarjeta"
      >
        {contenido}
      </div>
    );
  }

  return (
    <button
      ref={referencia as RefObject<HTMLButtonElement>}
      type="button"
      onClick={alAbrir}
      className="rounded-2xl border border-borde bg-white px-5 py-4 text-left shadow-tarjeta transition duration-200 hover:-translate-y-0.5 hover:border-violeta-borde hover:shadow-elevado"
    >
      {contenido}
    </button>
  );
}

function Tendencia({ indicador, claro = false }: { indicador: IndicadorKpi; claro?: boolean }) {
  if (indicador.tendencia === "SIN_COMPARACION" || indicador.variacion == null) {
    return (
      <span className={`text-xs ${claro ? "text-white/40" : "text-tinta-tenue"}`}>
        Sin periodo anterior comparable
      </span>
    );
  }

  const sube = indicador.tendencia === "SUBE";
  const estable = indicador.tendencia === "ESTABLE";
  const Icono = estable ? IconoIgual : sube ? IconoFlechaArriba : IconoFlechaAbajo;

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-lg px-2 py-0.5 text-xs font-semibold ${
        claro ? "bg-white/10 text-white/80" : "bg-lienzo text-tinta-media ring-1 ring-inset ring-borde"
      }`}
    >
      <Icono tamano={13} />
      <span className="tabular-nums">{Math.abs(indicador.variacion).toLocaleString("es-AR")}%</span>
      <span className={claro ? "font-normal text-white/45" : "font-normal text-tinta-tenue"}>
        vs anterior
      </span>
    </span>
  );
}

function FilaPlantilla({ plantilla, indice }: { plantilla: KpiPlantilla; indice: number }) {
  const estados = Object.entries(plantilla.porEstado).sort((uno, otro) => otro[1] - uno[1]);

  return (
    <article className="overflow-hidden rounded-2xl border border-borde bg-white shadow-tarjeta transition hover:border-borde-fuerte hover:shadow-elevado">
      <div className="grid gap-5 p-5 xl:grid-cols-[minmax(210px,1fr)_2.5fr] xl:items-center">
        <div className="min-w-0">
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <h3 className="truncate font-titulo text-[15px] text-tinta">
                {plantilla.nombre ?? plantilla.codigo}
              </h3>
              <p className="mt-0.5 font-mono text-[11px] text-tinta-tenue">{plantilla.codigo}</p>
            </div>
            <span
              className={`shrink-0 rounded-lg px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider ring-1 ring-inset ${
                ESTILO_SALUD[plantilla.salud]
              }`}
            >
              {plantilla.salud}
            </span>
          </div>
          <div className="mt-2.5 flex flex-wrap items-baseline gap-x-2 gap-y-1.5">
            <span className="cifra text-xl text-tinta">{plantilla.volumen.toLocaleString("es-AR")}</span>
            <span className="text-xs text-tinta-suave">documentos</span>
            {plantilla.documentosConExcepciones ? (
              <Pastilla tono="rojo">{plantilla.documentosConExcepciones} con excepcion</Pastilla>
            ) : null}
          </div>
        </div>

        <div className="grid gap-x-7 gap-y-3.5 sm:grid-cols-2">
          {plantilla.barras.map((barra, posicion) => (
            <CeldaBarra key={barra.clave} barra={barra} retraso={indice * 40 + posicion * 60} />
          ))}
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-2 border-t border-borde bg-lienzo/50 px-5 py-2.5">
        {estados.map(([estado, cantidad]) => (
          <span key={estado} className="flex items-center gap-1.5">
            <InsigniaEstado estado={estado as never} />
            <span className="text-xs font-semibold tabular-nums text-tinta-media">{cantidad}</span>
          </span>
        ))}
      </div>
    </article>
  );
}

function CeldaBarra({ barra, retraso }: { barra: BarraKpi; retraso: number }) {
  return (
    <div>
      <div className="flex items-baseline justify-between gap-2">
        <span className="truncate text-xs font-medium text-tinta-media" title={barra.formula}>
          {barra.etiqueta}
        </span>
        <span className="cifra shrink-0 text-xs text-tinta">
          {barra.porcentaje == null ? "sin datos" : `${barra.porcentaje.toLocaleString("es-AR")}%`}
        </span>
      </div>
      <div className="mt-1.5">
        <BarraAnimada
          porcentaje={barra.porcentaje}
          tono={TONO_SEMAFORO[barra.semaforo]}
          alto={7}
          retraso={retraso}
        />
      </div>
    </div>
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
    <PanelLateral titulo={indicador.etiqueta} descripcion={indicador.formula} alCerrar={alCerrar}>
      {poblacion.isPending ? (
        <Cargando filas={6} alto="h-16" />
      ) : poblacion.isError ? (
        <ErrorPanel mensaje={mensajeDeError(poblacion.error)} reintentar={() => poblacion.refetch()} />
      ) : !poblacion.data.length ? (
        <Vacio titulo="Sin documentos" detalle="Este indicador no tiene documentos en la ventana elegida." />
      ) : (
        <>
          <p className="flex items-center gap-2 rounded-xl bg-violeta-tenue px-4 py-3 text-xs text-violeta ring-1 ring-inset ring-violeta-borde">
            <IconoInfo tamano={15} />
            <span>
              <span className="font-bold">{poblacion.data.length.toLocaleString("es-AR")}</span>{" "}
              documentos componen este indicador. El numero de la tarjeta es exactamente este listado.
            </span>
          </p>
          <ul className="mt-4 space-y-2">
            {poblacion.data.map((documento) => (
              <li
                key={documento.id}
                className="rounded-xl border border-borde px-4 py-3 transition hover:border-borde-fuerte hover:bg-lienzo/60"
              >
                <div className="flex items-center justify-between gap-3">
                  <span className="truncate text-sm font-semibold text-tinta">
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
    </PanelLateral>
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
    return "< 1 h";
  }
  if (horas < 24) {
    return `${Math.round(horas)} h`;
  }
  const dias = Math.floor(horas / 24);
  const resto = Math.round(horas % 24);
  return resto ? `${dias} d ${resto} h` : `${dias} d`;
}
