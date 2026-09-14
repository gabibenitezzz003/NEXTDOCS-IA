import { useMemo, useState } from "react";
import { createPortal } from "react-dom";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Contenido, Encabezado } from "../componentes/Disposicion";
import {
  Cargando,
  CargandoTarjetas,
  ErrorPanel,
  Vacio,
} from "../componentes/Estados";
import { InsigniaEstado } from "../componentes/Insignias";
import {
  Boton,
  CabeceraTarjeta,
  GrupoSegmentado,
  Metrica,
  Panel as PanelLateral,
  Pastilla,
  Tarjeta,
} from "../componentes/Interfaz";
import {
  Anillo,
  AnilloApilado,
  BarraAnimada,
  Columnas,
  COLOR_GRAFICO,
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
import {
  listarKpiPorPlantilla,
  obtenerKpi,
  obtenerPoblacionKpi,
} from "../api/kpi";
import { mensajeDeError } from "../api/cliente";
import { formatearFecha } from "./Documentos";
import { ESTADOS_DOCUMENTALES } from "../utilidades/estadosDocumento";
import type { Tono } from "../componentes/Interfaz";
import type {
  EstadoDocumento,
  BarraKpi,
  IndicadorKpi,
  KpiPlantilla,
  SaludPlantilla,
  SemaforoKpi,
} from "../tipos/api";

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

const TONO_SALUD: Record<SaludPlantilla, ClaveTono> = {
  OK: "exito",
  ATENCION: "alerta",
  CRITICO: "rojo",
  SIN_DATOS: "neutro",
};

const ORDEN_EMBUDO = [
  "RECIBIDO",
  "PROCESANDO",
  "EXTRAIDO",
  "VALIDADO",
  "APROBADO",
  "CERRADO",
];

export function Panel() {
  const [dias, setDias] = useState(30);
  const [indicadorAbierto, setIndicadorAbierto] = useState<IndicadorKpi | null>(
    null,
  );

  const rango = useMemo(
    () => ({ desde: new Date(Date.now() - dias * 86400000).toISOString() }),
    [dias],
  );

  const resumen = useQuery({
    queryKey: ["kpi", "resumen", dias],
    queryFn: () => obtenerKpi(rango),
  });
  const plantillas = useQuery({
    queryKey: ["kpi", "plantillas", dias],
    queryFn: () => listarKpiPorPlantilla(rango),
  });

  const indicadores = resumen.data?.indicadores ?? [];
  const buscar = (clave: string) =>
    indicadores.find((indicador) => indicador.clave === clave);
  const secundarios = indicadores.filter(
    (indicador) => !DESTACADOS.includes(indicador.clave),
  );

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
      color:
        ESTADOS_DOCUMENTALES[estado as EstadoDocumento]?.color ??
        "var(--color-tinta-suave)",
    }));
  const totalBacklog = segmentos.reduce(
    (suma, segmento) => suma + segmento.valor,
    0,
  );

  const embudo = ORDEN_EMBUDO.filter((estado) => porEstado[estado] != null).map(
    (estado) => ({
      etiqueta: estado,
      valor: porEstado[estado] ?? 0,
      tono: ESTADOS_DOCUMENTALES[estado as EstadoDocumento].tono as ClaveTono,
    }),
  );

  return (
    <>
      <Encabezado
        titulo="Panel de control"
        descripcion="Indicadores documentales con sus fórmulas, poblaciones y contexto de medición."
      />
      <Contenido>
        <div className="mb-espacio-5 flex flex-wrap items-center justify-between gap-espacio-3">
          <GrupoSegmentado
            etiqueta="Período de los indicadores"
            opciones={VENTANAS.map((ventana) => ({
              valor: ventana.valor,
              texto: ventana.texto,
            }))}
            valor={dias}
            alCambiar={setDias}
          />
          {resumen.data && !resumen.isError ? (
            <p className="text-pequeno text-tinta-suave">
              {formatearFecha(resumen.data.rango.desde)} —{" "}
              {formatearFecha(resumen.data.rango.hasta)}
            </p>
          ) : null}
        </div>
        {resumen.isPending ? (
          <div className="space-y-espacio-5">
            <CargandoTarjetas cantidad={3} />
            <Cargando filas={3} alto="h-32" />
          </div>
        ) : resumen.isError ? (
          <ErrorPanel
            mensaje={mensajeDeError(resumen.error)}
            reintentar={() => resumen.refetch()}
          />
        ) : (
          <>
            {indicadores.length === 0 ? (
              <Vacio
                titulo="Sin indicadores disponibles"
                detalle="La respuesta no contiene indicadores. Esto no determina si existen documentos."
              />
            ) : null}
            <section
              aria-label="Indicadores principales"
              className="grid min-w-0 gap-espacio-4 sm:grid-cols-2 lg:grid-cols-3"
            >
              {recibidos ? (
                <TarjetaHeroe
                  indicador={recibidos}
                  rango={resumen.data.rango}
                  alAbrir={() => setIndicadorAbierto(recibidos)}
                />
              ) : null}
              {automatizacion ? (
                <TarjetaAnillo indicador={automatizacion} tono="violeta" />
              ) : null}
              {sla ? <TarjetaAnillo indicador={sla} tono="exito" /> : null}
            </section>
            <section
              aria-label="Detalle de indicadores"
              className="mt-espacio-4 grid min-w-0 gap-espacio-4 sm:grid-cols-2 xl:grid-cols-4"
            >
              {secundarios.map((indicador) => (
                <TarjetaIndicador
                  key={indicador.clave}
                  indicador={indicador}
                  alAbrir={() => setIndicadorAbierto(indicador)}
                />
              ))}
            </section>
            <section
              aria-label="Distribución documental actual"
              className="mt-espacio-6 grid min-w-0 gap-espacio-4 xl:grid-cols-2"
            >
              <Tarjeta>
                <CabeceraTarjeta
                  titulo="Embudo del ciclo documental"
                  descripcion="Conteo actual por etapa, sin recorte de fechas; incluye raíces y segmentos."
                />
                <p className="mt-espacio-3 text-pequeno text-tinta-suave">
                  Las barras se comparan con la etapa de mayor volumen; no
                  representan tasas de conversión.
                </p>
                <div className="mt-espacio-5">
                  {embudo.length ? (
                    <Embudo etapas={embudo} plano />
                  ) : (
                    <p className="text-pequeno text-tinta-suave">
                      Sin distribución por etapas disponible.
                    </p>
                  )}
                </div>
              </Tarjeta>
              <Tarjeta>
                <CabeceraTarjeta
                  titulo="Composición del backlog"
                  descripcion="Todos los documentos sin baja, incluidos segmentos y estados finales. Sin recorte de fechas."
                />
                {totalBacklog ? (
                  <div className="mt-espacio-5 flex min-w-0 flex-wrap items-center justify-center gap-espacio-5">
                    <div aria-hidden="true">
                      <AnilloApilado
                        segmentos={segmentos}
                        total={totalBacklog}
                        plano
                      />
                    </div>
                    <div className="min-w-0 flex-1 basis-56">
                      <p className="mb-espacio-3 text-pequeno font-semibold">
                        {totalBacklog.toLocaleString("es-AR")} documentos en
                        total
                      </p>
                      <ul
                        aria-label="Documentos por estado"
                        className="space-y-espacio-2"
                      >
                        {segmentos.map((segmento) => (
                          <li
                            key={segmento.etiqueta}
                            className="flex flex-wrap items-center gap-espacio-2 text-pequeno"
                          >
                            <span
                              aria-hidden="true"
                              className="size-espacio-2 shrink-0 rounded-insignia"
                              style={{ background: segmento.color }}
                            />
                            <span className="min-w-0 flex-1 font-medium [overflow-wrap:anywhere]">
                              {segmento.etiqueta}
                            </span>
                            <span className="tabular-nums">
                              {segmento.valor.toLocaleString("es-AR")}
                            </span>
                            <span className="w-espacio-10 text-right tabular-nums text-tinta-suave">
                              {Math.round(
                                (segmento.valor / totalBacklog) * 100,
                              )}
                              %
                            </span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  </div>
                ) : (
                  <p className="mt-espacio-5 text-pequeno text-tinta-suave">
                    {Object.keys(porEstado).length
                      ? "0 documentos en la distribución actual."
                      : "Sin distribución por estados disponible."}
                  </p>
                )}
                {Object.entries(porEstado).some(
                  ([, cantidad]) => cantidad === 0,
                ) ? (
                  <p className="mt-espacio-4 text-pequeno text-tinta-suave [overflow-wrap:anywhere]">
                    Con 0 documentos:{" "}
                    {Object.entries(porEstado)
                      .filter(([, cantidad]) => cantidad === 0)
                      .map(([estado]) => estado)
                      .join(", ")}
                    .
                  </p>
                ) : null}
              </Tarjeta>
            </section>
            <section
              aria-labelledby="salud-plantillas"
              className="mt-espacio-6 min-w-0"
            >
              <div className="mb-espacio-4">
                <h2
                  id="salud-plantillas"
                  className="font-titulo text-titulo-seccion"
                >
                  Salud por plantilla
                </h2>
                <p className="mt-espacio-2 text-pequeno text-tinta-suave">
                  Documentos con plantilla recibidos en el período, incluidos
                  segmentos. Salud según la menor razón disponible de las cuatro
                  barras; no es confianza de IA.
                </p>
                <p className="mt-espacio-2 text-pequeno text-tinta-suave">
                  OK ≥ 85% · ATENCIÓN ≥ 60% y &lt; 85% · CRÍTICO &lt; 60% · SIN
                  DATOS cuando todas las razones son nulas.
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
                  titulo="Sin datos por plantilla en este período"
                  detalle="No se devolvieron grupos de documentos con plantilla. Puede haber documentos fuera del período o sin plantilla."
                  accion={
                    <Link
                      to="/documentos"
                      className="rounded-control px-espacio-4 py-espacio-3 text-pequeno font-semibold text-accion-tonal-texto hover:bg-violeta-tenue focus-visible:outline-foco"
                    >
                      Ir a documentos
                    </Link>
                  }
                />
              ) : (
                <>
                  {plantillas.data.length > 1 ? (
                    <Tarjeta className="mb-espacio-4">
                      <CabeceraTarjeta
                        titulo="Volumen por plantilla"
                        descripcion="Cantidad de documentos recibidos en el período para cada plantilla, incluidos segmentos."
                      />
                      <div
                        aria-hidden="true"
                        className="mt-espacio-5 hidden sm:block"
                      >
                        <Columnas
                          barras={plantillas.data.map((plantilla) => ({
                            etiqueta: plantilla.codigo,
                            valor: plantilla.volumen,
                            tono: TONO_SALUD[plantilla.salud],
                            color: COLOR_GRAFICO[TONO_SALUD[plantilla.salud]],
                          }))}
                        />
                      </div>
                      <dl
                        aria-label="Volumen por plantilla en documentos"
                        className="mt-espacio-5 grid min-w-0 gap-espacio-3 sm:grid-cols-2 xl:grid-cols-3"
                      >
                        {plantillas.data.map((plantilla) => (
                          <div
                            key={plantilla.codigo}
                            className="flex min-w-0 items-baseline justify-between gap-espacio-3 border-t border-borde pt-espacio-2 text-pequeno"
                          >
                            <dt className="min-w-0 [overflow-wrap:anywhere]">
                              {plantilla.codigo}
                            </dt>
                            <dd className="shrink-0 font-semibold tabular-nums">
                              {plantilla.volumen.toLocaleString("es-AR")}
                            </dd>
                          </div>
                        ))}
                      </dl>
                    </Tarjeta>
                  ) : null}
                  <div className="space-y-espacio-4">
                    {plantillas.data.map((plantilla, indice) => (
                      <FilaPlantilla
                        key={plantilla.codigo}
                        plantilla={plantilla}
                        indice={indice}
                      />
                    ))}
                  </div>
                </>
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

const CONTEXTO_KPI: Record<string, string> = {
  documentosRecibidos: "Raíces recibidas en el período; excluye segmentos.",
  documentosCerrados: "Por fecha de cierre en el período; incluye segmentos.",
  automatizacion: "Cerrados sin revisión humana / cerrados del período.",
  cumplimientoSla:
    "Excepciones resueltas en el período; sin vencimiento también cumplen SLA.",
  tiempoCicloP50:
    "Cerrados en el período con fecha de recepción; incluye segmentos.",
  tiempoCicloP90:
    "Cerrados en el período con fecha de recepción; incluye segmentos.",
  excepcionesAbiertas: "Actual: ABIERTA + EN_CURSO. Sin recorte de fechas.",
  excepcionesVencidas: "Actual: vencidas no RESUELTAS, incluidas DESCARTADAS.",
  documentosPorVencer:
    "Retención en los próximos 30 días, independiente del período.",
  almacenamientoUtilizado:
    "Bytes de archivos vigentes / cuota de almacenamiento. Medición actual.",
  entregaDeEventos:
    "Registros de entrega creados en el período; no reintentos.",
};

function DetalleIndicador({ indicador }: { indicador: IndicadorKpi }) {
  return (
    <div className="mt-espacio-3 border-t border-borde pt-espacio-3 text-pequeno text-tinta-suave">
      {CONTEXTO_KPI[indicador.clave] ? (
        <p>{CONTEXTO_KPI[indicador.clave]}</p>
      ) : null}
      {indicador.detalle ? (
        <p className="mt-espacio-2">{indicador.detalle}</p>
      ) : null}
      <details className="mt-espacio-2">
        <summary className="w-fit cursor-pointer rounded-control text-accion-tonal-texto focus-visible:outline-foco">
          Fórmula del indicador
        </summary>
        <p className="mt-espacio-2 [overflow-wrap:anywhere]">
          {indicador.formula}
        </p>
      </details>
    </div>
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
    <div ref={referencia} className="min-w-0 sm:col-span-2 lg:col-span-1">
      <Tarjeta className="h-full">
        <Metrica
          etiqueta={indicador.etiqueta}
          valor={
            indicador.valor == null
              ? "Sin datos"
              : Math.round(animado).toLocaleString("es-AR")
          }
        />
        <p className="mt-espacio-2 text-pequeno text-tinta-suave">
          {rango.dias} días
        </p>
        <div
          className="mt-espacio-4 space-y-espacio-3"
          aria-label="Comparación de documentos recibidos"
        >
          {[
            {
              etiqueta: "Este período",
              valor: actual,
              disponible: indicador.valor != null,
              fuerte: true,
            },
            {
              etiqueta: "Período anterior",
              valor: anterior,
              disponible: indicador.valorAnterior != null,
              fuerte: false,
            },
          ].map((fila) => (
            <div key={fila.etiqueta}>
              <div className="mb-espacio-1 flex items-baseline justify-between gap-espacio-2 text-pequeno">
                <span className="text-tinta-suave">{fila.etiqueta}</span>
                <span className="font-semibold tabular-nums">
                  {fila.disponible
                    ? fila.valor.toLocaleString("es-AR")
                    : "Sin datos"}
                </span>
              </div>
              <div
                aria-hidden="true"
                className="h-espacio-2 overflow-hidden rounded-insignia bg-borde"
              >
                <div
                  className={
                    "h-full rounded-insignia " +
                    (fila.fuerte ? "bg-violeta" : "bg-tinta-suave")
                  }
                  style={{
                    width: visible ? `${(fila.valor / maximo) * 100}%` : "0%",
                    transition: "width 1.1s cubic-bezier(0.22, 1, 0.36, 1)",
                  }}
                />
              </div>
            </div>
          ))}
        </div>
        <div className="mt-espacio-3">
          <Tendencia indicador={indicador} />
        </div>
        <DetalleIndicador indicador={indicador} />
        {indicador.tienePoblacion ? (
          <Boton
            type="button"
            tamano="sm"
            onClick={alAbrir}
            className="mt-espacio-3"
          >
            Ver población
            <span aria-hidden="true">
              <IconoDerecha tamano={13} />
            </span>
          </Boton>
        ) : null}
      </Tarjeta>
    </div>
  );
}

function TarjetaAnillo({
  indicador,
  tono,
}: {
  indicador: IndicadorKpi;
  tono: ClaveTono;
}) {
  const sinDatos = indicador.valor == null;
  return (
    <Tarjeta>
      <h2 className="font-titulo text-titulo-panel">{indicador.etiqueta}</h2>
      <div className="mt-espacio-4 flex flex-wrap items-center justify-center gap-espacio-4">
        <div aria-hidden="true">
          <Anillo
            porcentaje={indicador.valor ?? null}
            tono={sinDatos ? "neutro" : tono}
            tamano={112}
            grosor={10}
            plano
            centro={
              <span className="cifra text-metrica-compacta">
                {formatearValor(indicador)}
              </span>
            }
          />
        </div>
        <div className="min-w-0 flex-1 basis-32 text-pequeno">
          <p className="sr-only">
            {indicador.etiqueta}: {formatearValor(indicador)}
          </p>
          {indicador.denominador ? (
            <p>
              {indicador.numerador?.toLocaleString("es-AR")} de{" "}
              {indicador.denominador.toLocaleString("es-AR")}
            </p>
          ) : (
            <p className="text-tinta-suave">
              Sin base de cálculo en este período
            </p>
          )}
          <div className="mt-espacio-2">
            <Tendencia indicador={indicador} />
          </div>
        </div>
      </div>
      <DetalleIndicador indicador={indicador} />
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
  const { referencia, visible } = useVisible<HTMLDivElement>();
  const esConteo = indicador.unidad === "CONTEO";
  const animado = useContador(visible && esConteo ? (indicador.valor ?? 0) : 0);
  const contenido = (
    <Metrica
      etiqueta={indicador.etiqueta}
      valor={
        indicador.valor == null
          ? "Sin datos"
          : esConteo
            ? Math.round(animado).toLocaleString("es-AR")
            : formatearValor(indicador)
      }
      detalle={<Tendencia indicador={indicador} />}
    />
  );
  return (
    <div ref={referencia} className="min-w-0">
      <Tarjeta className="h-full">
        {indicador.tienePoblacion ? (
          <button
            type="button"
            onClick={alAbrir}
            className="w-full rounded-control text-left focus-visible:outline-foco"
          >
            {contenido}
            <span className="mt-espacio-2 inline-block text-pequeno font-semibold text-accion-tonal-texto">
              Ver población
            </span>
          </button>
        ) : (
          contenido
        )}
        <DetalleIndicador indicador={indicador} />
      </Tarjeta>
    </div>
  );
}

function Tendencia({ indicador }: { indicador: IndicadorKpi }) {
  if (indicador.tendencia === "SIN_COMPARACION" || indicador.variacion == null)
    return (
      <span className="text-pequeno text-tinta-suave">
        Sin período anterior comparable
      </span>
    );
  const sube = indicador.tendencia === "SUBE";
  const estable = indicador.tendencia === "ESTABLE";
  const Icono = estable
    ? IconoIgual
    : sube
      ? IconoFlechaArriba
      : IconoFlechaAbajo;
  return (
    <span className="inline-flex flex-wrap items-center gap-espacio-1 text-pequeno text-tinta-media">
      <span aria-hidden="true">
        <Icono tamano={13} />
      </span>
      <span>{estable ? "Estable" : sube ? "Sube" : "Baja"}</span>
      <span className="tabular-nums">
        {Math.abs(indicador.variacion).toLocaleString("es-AR")}%
      </span>
      <span className="text-tinta-suave">vs anterior</span>
    </span>
  );
}

function FilaPlantilla({
  plantilla,
  indice,
}: {
  plantilla: KpiPlantilla;
  indice: number;
}) {
  const estados = Object.entries(plantilla.porEstado).sort(
    (uno, otro) => otro[1] - uno[1],
  );
  return (
    <Tarjeta>
      <div className="grid min-w-0 gap-espacio-5 xl:grid-cols-[minmax(0,1fr)_minmax(0,2fr)]">
        <div className="min-w-0">
          <h3 className="font-titulo text-titulo-panel [overflow-wrap:anywhere]">
            {plantilla.nombre ?? plantilla.codigo}
          </h3>
          <p className="mt-espacio-1 text-pequeno text-tinta-suave [overflow-wrap:anywhere]">
            {plantilla.codigo}
          </p>
          <div className="mt-espacio-3 flex flex-wrap items-center gap-espacio-2">
            <Pastilla tono={TONO_SALUD[plantilla.salud] as Tono}>
              {plantilla.salud.replace(/_/g, " ")}
            </Pastilla>
            <span className="text-pequeno">
              <span className="font-semibold tabular-nums">
                {plantilla.volumen.toLocaleString("es-AR")}
              </span>{" "}
              documentos
            </span>
            <Pastilla
              tono={plantilla.documentosConExcepciones ? "rojo" : "neutro"}
            >
              {plantilla.documentosConExcepciones} con excepción
            </Pastilla>
          </div>
        </div>
        <div className="grid min-w-0 gap-espacio-4 sm:grid-cols-2">
          {plantilla.barras.map((barra, posicion) => (
            <CeldaBarra
              key={barra.clave}
              barra={barra}
              retraso={indice * 40 + posicion * 60}
            />
          ))}
        </div>
      </div>
      <ul
        aria-label={"Estados de " + plantilla.codigo}
        className="mt-espacio-4 flex flex-wrap items-center gap-espacio-3 border-t border-borde pt-espacio-3"
      >
        {estados.map(([estado, cantidad]) => (
          <li key={estado} className="flex items-center gap-espacio-1">
            <InsigniaEstado estado={estado as EstadoDocumento} />
            <span className="text-pequeno font-semibold tabular-nums">
              {cantidad}
            </span>
          </li>
        ))}
      </ul>
    </Tarjeta>
  );
}

function CeldaBarra({ barra, retraso }: { barra: BarraKpi; retraso: number }) {
  return (
    <div className="min-w-0">
      <div className="flex items-baseline justify-between gap-espacio-2 text-pequeno">
        <span className="min-w-0 font-medium [overflow-wrap:anywhere]">
          {barra.etiqueta}
        </span>
        <span className="shrink-0 font-semibold tabular-nums">
          {barra.porcentaje == null
            ? "sin datos"
            : `${barra.porcentaje.toLocaleString("es-AR")}%`}
        </span>
      </div>
      <div aria-hidden="true" className="mt-espacio-2">
        <BarraAnimada
          porcentaje={barra.porcentaje}
          tono={TONO_SEMAFORO[barra.semaforo]}
          alto={7}
          retraso={retraso}
          plano
        />
      </div>
      <p className="mt-espacio-2 text-micro text-tinta-suave [overflow-wrap:anywhere]">
        {barra.formula}
      </p>
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

  return createPortal(
    <PanelLateral
      titulo={indicador.etiqueta}
      descripcion={indicador.formula}
      alCerrar={alCerrar}
    >
      {poblacion.isPending ? (
        <Cargando filas={6} alto="h-16" />
      ) : poblacion.isError ? (
        <ErrorPanel
          mensaje={mensajeDeError(poblacion.error)}
          reintentar={() => poblacion.refetch()}
        />
      ) : !poblacion.data.length ? (
        <Vacio
          titulo="Sin documentos"
          detalle="Este indicador no tiene documentos en la ventana elegida."
        />
      ) : (
        <>
          <p className="flex items-center gap-espacio-2 rounded-control bg-violeta-tenue px-espacio-4 py-espacio-3 text-pequeno text-accion-tonal-texto ring-1 ring-inset ring-violeta-borde">
            <span aria-hidden="true" className="shrink-0">
              <IconoInfo tamano={15} />
            </span>
            <span>
              <span className="font-bold">
                {poblacion.data.length.toLocaleString("es-AR")}
              </span>{" "}
              documentos devueltos para este indicador. La consulta devuelve
              hasta 200 registros; puede ser una muestra. En p50/p90 son la base
              del cálculo, no el valor en horas.
            </span>
          </p>
          <ul className="mt-espacio-4 space-y-espacio-2">
            {poblacion.data.map((documento) => (
              <li
                key={documento.id}
                className="rounded-control border border-borde px-espacio-4 py-espacio-3"
              >
                <div className="flex flex-wrap items-center justify-between gap-espacio-3">
                  <span className="min-w-0 text-pequeno font-semibold text-tinta [overflow-wrap:anywhere]">
                    {documento.nombre ?? documento.id}
                  </span>
                  <InsigniaEstado estado={documento.estado} />
                </div>
                <p className="mt-espacio-1 text-pequeno text-tinta-suave [overflow-wrap:anywhere]">
                  {documento.codigoPlantilla ?? "sin plantilla"} · recibido{" "}
                  {formatearFecha(documento.recibido)}
                </p>
              </li>
            ))}
          </ul>
        </>
      )}
    </PanelLateral>,
    document.body,
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
