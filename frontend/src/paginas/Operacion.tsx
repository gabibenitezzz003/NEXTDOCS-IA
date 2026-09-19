import { useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Contenido, Encabezado } from "../componentes/Disposicion";
import {
  Cargando,
  CargandoTarjetas,
  ErrorPanel,
  Vacio,
} from "../componentes/Estados";
import {
  Barra,
  Boton,
  CabeceraTarjeta,
  Campo,
  GrupoSegmentado,
  Metrica,
  Pastilla,
  Selector,
  Tarjeta,
} from "../componentes/Interfaz";
import { TarjetaTarea } from "../componentes/TarjetaTarea";
import { AnilloApilado } from "../componentes/Graficos";
import { DiagramaProceso } from "../componentes/DiagramaProceso";
import type { EstadoNodoEjecucion } from "../componentes/DiagramaProceso";
import { formatearFecha } from "../utilidades/fechas";
import {
  INDICADOR_CUELLOS,
  cancelarInstancia,
  listarHallazgos,
  listarInstancias,
  listarProcesos,
  mensajeDeError,
  obtenerInstancia,
  obtenerProceso,
  pausarInstancia,
  poblacionKpiProcesos,
  reanudarInstancia,
  resolverHallazgo,
  resumenKpiProcesos,
} from "../api/procesos";
import type {
  EstadoInstanciaProceso,
  EventoInstancia,
  InstanciaProceso,
  KpiProcesoIndicador,
  KpiProcesoPoblacion,
} from "../api/procesos";
import { useSesion } from "../contextos/ProveedorSesion";
import { useIdioma } from "../contextos/ProveedorIdioma";

const ACCIONES_CONOCIDAS = new Set([
  "INSTANCIA_INICIADA",
  "NODO_INGRESADO",
  "TAREA_CREADA",
  "TAREA_COMPLETADA",
  "DECISION_TOMADA",
  "INSTANCIA_COMPLETADA",
  "INSTANCIA_CANCELADA",
  "INSTANCIA_BLOQUEADA",
  "INSTANCIA_REANUDADA",
  "TAREA_VENCIDA",
  "VALIDACION_IA_EJECUTADA",
  "NOTIFICACION_ENVIADA",
  "ACCION_API_EJECUTADA",
]);

type Traductor = (ruta: string, params?: Record<string, string | number>) => string;

function textoAccion(accion: string, t: Traductor): string {
  return ACCIONES_CONOCIDAS.has(accion) ? t(`accionEvento.${accion}`) : accion;
}

function EnlaceSujeto({ instancia }: { instancia: InstanciaProceso }) {
  const { t } = useIdioma();
  if (!instancia.sujetoId) return null;
  const esDocumento = (instancia.sujetoTipo ?? "")
    .toLowerCase()
    .includes("documento");
  if (!esDocumento) {
    return (
      <span>
        {instancia.sujetoTipo ?? t("operacion.sujetoTipo")} {instancia.sujetoId}
      </span>
    );
  }
  return (
    <Link
      to={`/documental?documento=${instancia.sujetoId}`}
      className="font-semibold text-accion-tonal-texto hover:underline focus-visible:outline-foco"
    >
      {t("operacion.verDocumento")}
    </Link>
  );
}

function tonoEstadoInstancia(estado: EstadoInstanciaProceso) {
  switch (estado) {
    case "COMPLETADA":
      return "exito" as const;
    case "BLOQUEADA":
      return "alerta" as const;
    case "CANCELADA":
      return "rojo" as const;
    case "ESPERANDO":
      return "informacion" as const;
    default:
      return "violeta" as const;
  }
}

const ES_FINAL = ["COMPLETADA", "CANCELADA"];

function estadosDeNodo(instancia: InstanciaProceso): Map<string, EstadoNodoEjecucion> {
  const mapa = new Map<string, EstadoNodoEjecucion>();
  for (const evento of instancia.eventos ?? []) {
    if (evento.nodoId) mapa.set(evento.nodoId, "COMPLETADO");
  }
  for (const tarea of instancia.tareas ?? []) {
    const previo = mapa.get(tarea.nodoId);
    if (tarea.estado === "PENDIENTE") {
      mapa.set(tarea.nodoId, "ACTUAL");
    } else if (tarea.estado === "VENCIDA") {
      if (previo !== "ACTUAL") mapa.set(tarea.nodoId, "VENCIDO");
    } else if (tarea.estado === "CANCELADA") {
      if (!previo) mapa.set(tarea.nodoId, "CANCELADO");
    } else if (previo !== "ACTUAL" && previo !== "VENCIDO") {
      mapa.set(tarea.nodoId, "COMPLETADO");
    }
  }
  return mapa;
}

function resumenTareas(instancia: InstanciaProceso) {
  const tareas = instancia.tareas ?? [];
  const completadas = tareas.filter((tarea) => tarea.estado === "COMPLETADA").length;
  const pendientes = tareas.filter(
    (tarea) => tarea.estado === "PENDIENTE" || tarea.estado === "VENCIDA",
  );
  const proximoVencimiento = pendientes
    .map((tarea) => tarea.vencimiento)
    .filter((fecha): fecha is string => Boolean(fecha))
    .sort()[0];
  return {
    total: tareas.length,
    completadas,
    pendientes: pendientes.length,
    porcentaje: tareas.length ? Math.round((completadas / tareas.length) * 100) : 0,
    responsable: pendientes.find((tarea) => tarea.asignadoA)?.asignadoA,
    proximoVencimiento,
  };
}

function tonoVencimiento(fecha: string) {
  const restante = new Date(fecha).getTime() - Date.now();
  if (restante < 0) return "rojo" as const;
  if (restante < 24 * 60 * 60 * 1000) return "alerta" as const;
  return "informacion" as const;
}

const DIAS_VENTANA = [7, 30, 90] as const;

const INDICADORES_FRANJA = [
  "procesosActivos",
  "tareasPendientes",
  "tareasVencidas",
  "slaProceso",
  "tiempoCicloP50",
  "instanciasCompletadas",
];

const FILTRO_POR_INDICADOR: Record<string, string> = {
  procesosActivos: "ACTIVA",
  tareasPendientes: "ESPERANDO",
  instanciasCompletadas: "COMPLETADA",
};

function formatearMinutos(minutos: number): string {
  const total = Math.round(minutos);
  if (total < 60) return `${total} min`;
  const horas = Math.floor(total / 60);
  const resto = total % 60;
  if (horas < 24) return resto ? `${horas} h ${resto} min` : `${horas} h`;
  const dias = Math.floor(horas / 24);
  const horasResto = horas % 24;
  return horasResto ? `${dias} d ${horasResto} h` : `${dias} d`;
}

function formatearIndicador(indicador: KpiProcesoIndicador): string {
  const { valor, unidad } = indicador;
  if (valor == null) return "—";
  if (unidad === "%") return `${valor}%`;
  if (unidad === "min") return formatearMinutos(valor);
  if (unidad === "instancias/hora") return `${valor}/h`;
  return `${valor}`;
}

function FranjaIndicadores({
  dias,
  alElegir,
}: {
  dias: number;
  alElegir: (estado: string) => void;
}) {
  const { t } = useIdioma();
  const consulta = useQuery({
    queryKey: ["kpi-procesos", dias],
    queryFn: () => resumenKpiProcesos(dias),
    placeholderData: (anterior) => anterior,
  });

  if (consulta.isPending) return <CargandoTarjetas cantidad={6} />;
  if (consulta.isError)
    return (
      <ErrorPanel
        contexto={t("operacion.errorIndicadores")}
        mensaje={mensajeDeError(consulta.error)}
        error={consulta.error}
        reintentar={() => consulta.refetch()}
      />
    );

  const porCodigo = new Map(
    (consulta.data?.indicadores ?? []).map((indicador) => [indicador.codigo, indicador]),
  );
  const visibles = INDICADORES_FRANJA.map((codigo) => porCodigo.get(codigo)).filter(
    (indicador): indicador is KpiProcesoIndicador => indicador != null,
  );

  if (visibles.length === 0)
    return (
      <Vacio
        titulo={t("operacion.sinIndicadores")}
        detalle={t("operacion.sinIndicadoresDetalle")}
      />
    );

  return (
    <div className="grid gap-espacio-4 sm:grid-cols-2 lg:grid-cols-3">
      {visibles.map((indicador, indice) => {
        const destino = FILTRO_POR_INDICADOR[indicador.codigo];
        return (
          <Tarjeta
            key={indicador.codigo}
            indice={indice}
            interactiva={Boolean(destino)}
            role={destino ? "button" : undefined}
            tabIndex={destino ? 0 : undefined}
            aria-label={
              destino
                ? t("operacion.verIndicador", { nombre: indicador.nombre })
                : undefined
            }
            onClick={destino ? () => alElegir(destino) : undefined}
            onKeyDown={
              destino
                ? (evento) => {
                    if (evento.key === "Enter" || evento.key === " ") {
                      evento.preventDefault();
                      alElegir(destino);
                    }
                  }
                : undefined
            }
          >
            <div className="flex items-start justify-between gap-espacio-3">
              <Metrica
                etiqueta={indicador.nombre}
                valor={formatearIndicador(indicador)}
                detalle={
                  indicador.valor == null
                    ? t("operacion.sinDatosVentana")
                    : indicador.formula
                }
              />
              {destino ? (
                <span
                  aria-hidden="true"
                  className="mt-espacio-1 text-titulo-panel text-accion-tonal-texto"
                >
                  →
                </span>
              ) : null}
            </div>
          </Tarjeta>
        );
      })}
    </div>
  );
}

interface CuelloDeBotella {
  clave: string;
  codigoDefinicion: string;
  nodoId: string;
  muestras: number;
  promedioMinutos: number;
}

function agruparCuellos(poblacion: KpiProcesoPoblacion[]): CuelloDeBotella[] {
  const acumulado = new Map<string, { codigoDefinicion: string; nodoId: string; total: number; muestras: number }>();
  for (const fila of poblacion) {
    if (fila.duracionMinutos == null || !fila.nodoId) continue;
    const codigoDefinicion = fila.codigoDefinicion ?? "—";
    const clave = `${codigoDefinicion}\x00${fila.nodoId}`;
    const previo = acumulado.get(clave);
    if (previo) {
      previo.total += fila.duracionMinutos;
      previo.muestras += 1;
    } else {
      acumulado.set(clave, {
        codigoDefinicion,
        nodoId: fila.nodoId,
        total: fila.duracionMinutos,
        muestras: 1,
      });
    }
  }
  return Array.from(acumulado.entries())
    .map(([clave, dato]) => ({
      clave,
      codigoDefinicion: dato.codigoDefinicion,
      nodoId: dato.nodoId,
      muestras: dato.muestras,
      promedioMinutos: dato.total / dato.muestras,
    }))
    .sort((uno, otro) => otro.promedioMinutos - uno.promedioMinutos)
    .slice(0, 10);
}

function CuellosDeBotella({ dias }: { dias: number }) {
  const { t } = useIdioma();
  const consulta = useQuery({
    queryKey: ["kpi-poblacion", INDICADOR_CUELLOS, dias],
    queryFn: () => poblacionKpiProcesos(INDICADOR_CUELLOS, dias),
    placeholderData: (anterior) => anterior,
  });

  if (consulta.isPending) return <Cargando filas={3} alto="h-10" />;
  if (consulta.isError)
    return (
      <ErrorPanel
        contexto={t("operacion.errorCuellos")}
        mensaje={mensajeDeError(consulta.error)}
        error={consulta.error}
        reintentar={() => consulta.refetch()}
      />
    );

  const cuellos = agruparCuellos(consulta.data ?? []);
  if (cuellos.length === 0)
    return (
      <Vacio
        titulo={t("operacion.sinPasos")}
        detalle={t("operacion.sinPasosDetalle")}
      />
    );

  const mayor = cuellos[0].promedioMinutos;

  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[32rem] border-collapse text-pequeno">
        <caption className="sr-only">
          {t("operacion.captionCuellos")}
        </caption>
        <thead>
          <tr className="border-b border-borde text-left text-micro uppercase tracking-wider text-tinta-suave">
            <th scope="col" className="py-espacio-2 pr-espacio-4 font-semibold">
              {t("operacion.colProceso")}
            </th>
            <th scope="col" className="py-espacio-2 pr-espacio-4 font-semibold">
              {t("operacion.colPaso")}
            </th>
            <th scope="col" className="py-espacio-2 pr-espacio-4 text-right font-semibold">
              {t("operacion.colTareas")}
            </th>
            <th scope="col" className="py-espacio-2 text-right font-semibold">
              {t("operacion.colPromedio")}
            </th>
          </tr>
        </thead>
        <tbody>
          {cuellos.map((cuello) => (
            <tr key={cuello.clave} className="border-b border-borde last:border-0">
              <td className="py-espacio-3 pr-espacio-4 text-tinta-suave">
                {cuello.codigoDefinicion}
              </td>
              <td className="py-espacio-3 pr-espacio-4">
                <p className="font-semibold text-tinta">{cuello.nodoId}</p>
                <div className="mt-espacio-2 max-w-[16rem]">
                  <Barra
                    porcentaje={mayor > 0 ? (cuello.promedioMinutos / mayor) * 100 : 0}
                    alto="h-1"
                  />
                </div>
              </td>
              <td className="py-espacio-3 pr-espacio-4 text-right tabular-nums text-tinta-suave">
                {cuello.muestras}
              </td>
              <td className="py-espacio-3 text-right tabular-nums font-semibold text-tinta">
                {formatearMinutos(cuello.promedioMinutos)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

const ESTADOS_DISTRIBUCION: {
  estado: EstadoInstanciaProceso;
  color: string;
}[] = [
  { estado: "ACTIVA", color: "#8B5CF6" },
  { estado: "ESPERANDO", color: "#3B82F6" },
  { estado: "BLOQUEADA", color: "#F59E0B" },
  { estado: "COMPLETADA", color: "#22C55E" },
  { estado: "CANCELADA", color: "#EF4444" },
];

function DistribucionEstados({
  estadoActual,
  alElegir,
}: {
  estadoActual: string;
  alElegir: (estado: string) => void;
}) {
  const { t } = useIdioma();
  const consulta = useQuery({
    queryKey: ["instancias", "distribucion"],
    queryFn: () => listarInstancias(),
    refetchInterval: 15000,
    placeholderData: (anterior) => anterior,
  });

  if (consulta.isPending) return <Cargando filas={2} alto="h-10" />;
  if (consulta.isError) return null;

  const instancias = consulta.data ?? [];
  const conteo = new Map<EstadoInstanciaProceso, number>();
  for (const instancia of instancias) {
    conteo.set(instancia.estado, (conteo.get(instancia.estado) ?? 0) + 1);
  }
  const total = instancias.length;
  if (total === 0) {
    return (
      <Vacio
        titulo={t("operacion.sinDistribucion")}
        detalle={t("operacion.sinDistribucionDetalle")}
      />
    );
  }

  const enCurso =
    (conteo.get("ACTIVA") ?? 0) + (conteo.get("ESPERANDO") ?? 0);

  return (
    <div className="grid min-w-0 items-center gap-espacio-5 sm:grid-cols-[auto_minmax(0,1fr)]">
      <div className="mx-auto">
        <AnilloApilado
          segmentos={ESTADOS_DISTRIBUCION.map((entrada) => ({
            etiqueta: t(`estadoInstancia.${entrada.estado}`),
            valor: conteo.get(entrada.estado) ?? 0,
            color: entrada.color,
          }))}
          total={total}
          etiquetaTotal={t("operacion.enCurso", { cantidad: enCurso })}
          tamano={168}
        />
      </div>
      <ul className="grid min-w-0 gap-espacio-2">
        {ESTADOS_DISTRIBUCION.map((entrada) => {
          const cantidad = conteo.get(entrada.estado) ?? 0;
          const activo = estadoActual === entrada.estado;
          return (
            <li key={entrada.estado}>
              <button
                type="button"
                onClick={() => alElegir(activo ? "" : entrada.estado)}
                aria-pressed={activo}
                className={`flex w-full items-center gap-espacio-3 rounded-control border px-espacio-3 py-espacio-2 text-left transition ${
                  activo
                    ? "border-accion-primaria bg-violeta-tenue"
                    : "border-borde bg-superficie hover:border-borde-fuerte"
                }`}
              >
                <span
                  aria-hidden="true"
                  className="h-2.5 w-2.5 shrink-0 rounded-full"
                  style={{ backgroundColor: entrada.color }}
                />
                <span className="min-w-0 flex-1 text-pequeno text-tinta">
                  {t(`estadoInstancia.${entrada.estado}`)}
                </span>
                <span className="cifra text-pequeno font-semibold text-tinta">
                  {cantidad}
                </span>
              </button>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

export function BandejaInstancias({ alAbrir }: { alAbrir: (id: string) => void }) {
  const { t } = useIdioma();
  const [estado, setEstado] = useState<string>("");
  const [definicion, setDefinicion] = useState<string>("");
  const [busqueda, setBusqueda] = useState<string>("");
  const [dias, setDias] = useState<number>(7);
  const referenciaFiltros = useRef<HTMLDivElement>(null);

  const elegirEstado = (valor: string) => {
    setEstado(valor);
    referenciaFiltros.current?.scrollIntoView({
      behavior: "smooth",
      block: "start",
    });
  };

  const consultaProcesos = useQuery({
    queryKey: ["procesos"],
    queryFn: listarProcesos,
  });

  const consulta = useQuery({
    queryKey: ["instancias", estado, definicion],
    queryFn: () => listarInstancias(estado || undefined, definicion || undefined),
    refetchInterval: 15000,
  });

  const texto = busqueda.trim().toLowerCase();
  const instancias = (consulta.data ?? []).filter((instancia) =>
    !texto
      ? true
      : instancia.codigoDefinicion.toLowerCase().includes(texto) ||
        instancia.id.toLowerCase().includes(texto) ||
        (instancia.sujetoId ?? "").toLowerCase().includes(texto),
  );

  return (
    <>
      <section aria-labelledby="titulo-indicadores" className="mb-espacio-8">
        <div className="mb-espacio-4 flex flex-wrap items-end justify-between gap-espacio-4">
          <div className="min-w-0">
            <h2
              id="titulo-indicadores"
              className="font-titulo text-titulo-panel text-tinta"
            >
              {t("operacion.indicadores")}
            </h2>
            <p className="mt-espacio-1 text-pequeno text-tinta-suave">
              {t("operacion.indicadoresDesc")}
            </p>
          </div>
          <GrupoSegmentado
            etiqueta={t("operacion.ventanaIndicadores")}
            valor={dias}
            alCambiar={setDias}
            opciones={DIAS_VENTANA.map((valor) => ({
              valor,
              texto: t("panel.dias", { dias: valor }),
            }))}
          />
        </div>
        <FranjaIndicadores dias={dias} alElegir={elegirEstado} />
        <div className="mt-espacio-6 grid min-w-0 gap-espacio-6 xl:grid-cols-2">
          <section aria-labelledby="titulo-distribucion">
            <h3
              id="titulo-distribucion"
              className="font-titulo text-titulo-panel text-tinta"
            >
              {t("operacion.distribucionTitulo")}
            </h3>
            <p className="mt-espacio-1 mb-espacio-4 text-pequeno text-tinta-suave">
              {t("operacion.distribucionDesc")}
            </p>
            <DistribucionEstados estadoActual={estado} alElegir={elegirEstado} />
          </section>
          <section aria-labelledby="titulo-cuellos">
            <h3
              id="titulo-cuellos"
              className="font-titulo text-titulo-panel text-tinta"
            >
              {t("operacion.cuellosTitulo")}
            </h3>
            <p className="mt-espacio-1 mb-espacio-4 text-pequeno text-tinta-suave">
              {t("operacion.cuellosDesc")}
            </p>
            <CuellosDeBotella dias={dias} />
          </section>
        </div>
      </section>

      <div
        ref={referenciaFiltros}
        className="mb-espacio-4 grid scroll-mt-espacio-6 gap-espacio-4 md:grid-cols-[12rem_minmax(0,1fr)_12rem]"
      >
        <Selector
          etiqueta={t("operacion.estado")}
          value={estado}
          onChange={(evento) => setEstado(evento.target.value)}
        >
          <option value="">{t("operacion.filtroTodos")}</option>
          <option value="ACTIVA">{t("operacion.enEjecucion")}</option>
          <option value="ESPERANDO">{t("operacion.esperandoTareas")}</option>
          <option value="BLOQUEADA">{t("operacion.bloqueadas")}</option>
          <option value="COMPLETADA">{t("operacion.completadas")}</option>
          <option value="CANCELADA">{t("operacion.canceladas")}</option>
        </Selector>
        <Selector
          etiqueta={t("operacion.proceso")}
          value={definicion}
          onChange={(evento) => setDefinicion(evento.target.value)}
        >
          <option value="">{t("operacion.todosLosProcesos")}</option>
          {(consultaProcesos.data ?? []).map((proceso) => (
            <option key={proceso.id} value={proceso.codigo}>
              {proceso.nombre}
            </option>
          ))}
        </Selector>
        <Campo
          etiqueta={t("operacion.buscar")}
          placeholder={t("operacion.buscarPlaceholder")}
          value={busqueda}
          onChange={(evento) => setBusqueda(evento.target.value)}
        />
      </div>

      {consulta.isPending ? (
        <Cargando filas={3} alto="h-24" />
      ) : consulta.isError ? (
        <ErrorPanel
          mensaje={mensajeDeError(consulta.error)}
          error={consulta.error}
          reintentar={() => consulta.refetch()}
        />
      ) : instancias.length === 0 ? (
        <Vacio
          titulo={t("operacion.sinInstancias")}
          detalle={t("operacion.sinInstanciasDetalle")}
        />
      ) : (
        <ul aria-label={t("operacion.listaInstancias")} className="space-y-espacio-4">
          {instancias.map((instancia) => {
            const resumen = resumenTareas(instancia);
            const vencida =
              resumen.proximoVencimiento != null &&
              new Date(resumen.proximoVencimiento).getTime() < Date.now();
            return (
              <li key={instancia.id}>
                <Tarjeta className="grid items-center gap-espacio-4 lg:grid-cols-[minmax(0,1fr)_auto]">
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-espacio-3">
                      <h2 className="break-words font-titulo text-titulo-panel text-tinta">
                        {instancia.codigoDefinicion}
                      </h2>
                      <Pastilla tono={tonoEstadoInstancia(instancia.estado)}>
                        {t(`estadoInstancia.${instancia.estado}`)}
                      </Pastilla>
                      {resumen.proximoVencimiento ? (
                        <Pastilla tono={tonoVencimiento(resumen.proximoVencimiento)}>
                          {vencida
                            ? t("operacion.vencio", {
                                fecha: formatearFecha(resumen.proximoVencimiento),
                              })
                            : t("operacion.vence", {
                                fecha: formatearFecha(resumen.proximoVencimiento),
                              })}
                        </Pastilla>
                      ) : null}
                    </div>
                    <p className="mt-espacio-1 text-pequeno text-tinta-suave">
                      {t("operacion.version", {
                        version: instancia.numeroVersion,
                      })}{" "}
                      ·{" "}
                      {t("operacion.iniciada", {
                        fecha: instancia.alta
                          ? formatearFecha(instancia.alta)
                          : "—",
                      })}
                      {instancia.sujetoId ? (
                        <>
                          {" · "}
                          <EnlaceSujeto instancia={instancia} />
                        </>
                      ) : null}
                      {resumen.responsable
                        ? ` · ${t("operacion.responsableDe", { actor: resumen.responsable })}`
                        : ""}
                    </p>
                    {resumen.total ? (
                      <div className="mt-espacio-3 max-w-[22rem]">
                        <div className="mb-espacio-1 flex items-baseline justify-between gap-espacio-3 text-pequeno">
                          <span className="text-tinta-suave">
                            {t("operacion.progresoTareas", {
                              completadas: resumen.completadas,
                              total: resumen.total,
                            })}
                          </span>
                          <span className="font-semibold tabular-nums text-tinta">
                            {resumen.porcentaje}%
                          </span>
                        </div>
                        <Barra porcentaje={resumen.porcentaje} alto="h-1.5" />
                      </div>
                    ) : null}
                  </div>
                  <Boton variante="secundario" onClick={() => alAbrir(instancia.id)}>
                    {t("operacion.verDetalle")}
                  </Boton>
                </Tarjeta>
              </li>
            );
          })}
        </ul>
      )}
    </>
  );
}

export function DetalleInstancia({
  instanciaId,
  alVolver,
}: {
  instanciaId: string;
  alVolver: () => void;
}) {
  const { sesion } = useSesion();
  const { t } = useIdioma();
  const clienteConsultas = useQueryClient();
  const [motivo, setMotivo] = useState("");
  const [error, setError] = useState<string | null>(null);

  const consulta = useQuery({
    queryKey: ["instancia", instanciaId],
    queryFn: () => obtenerInstancia(instanciaId),
    refetchInterval: (consultaActual) =>
      consultaActual.state.data && ES_FINAL.includes(consultaActual.state.data.estado)
        ? false
        : 15000,
  });

  const pausar = useMutation({
    mutationFn: () =>
      pausarInstancia(instanciaId, {
        motivo: motivo.trim(),
        actor: sesion?.email ?? "operador",
      }),
    onSuccess: () => {
      setError(null);
      setMotivo("");
      clienteConsultas.invalidateQueries({ queryKey: ["instancia", instanciaId] });
      clienteConsultas.invalidateQueries({ queryKey: ["instancias"] });
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const reanudar = useMutation({
    mutationFn: () => reanudarInstancia(instanciaId, sesion?.email ?? "operador"),
    onSuccess: () => {
      setError(null);
      clienteConsultas.invalidateQueries({ queryKey: ["instancia", instanciaId] });
      clienteConsultas.invalidateQueries({ queryKey: ["instancias"] });
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const cancelar = useMutation({
    mutationFn: () =>
      cancelarInstancia(instanciaId, {
        motivo: motivo.trim() || undefined,
        actor: sesion?.email ?? "operador",
      }),
    onSuccess: () => {
      setError(null);
      setMotivo("");
      clienteConsultas.invalidateQueries({ queryKey: ["instancia", instanciaId] });
      clienteConsultas.invalidateQueries({ queryKey: ["instancias"] });
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const instancia = consulta.data;

  const consultaProceso = useQuery({
    queryKey: ["proceso", instancia?.definicionId],
    queryFn: () => obtenerProceso(instancia!.definicionId),
    enabled: Boolean(instancia?.definicionId),
  });

  if (consulta.isPending) {
    return <Cargando filas={4} alto="h-24" />;
  }
  if (consulta.isError || !instancia) {
    return (
      <ErrorPanel
        mensaje={mensajeDeError(consulta.error ?? new Error(t("operacion.instanciaNoExiste")))}
        reintentar={() => consulta.refetch()}
      />
    );
  }

  const puedePausar =
    instancia.estado === "ACTIVA" || instancia.estado === "ESPERANDO";
  const puedeReanudar = instancia.estado === "BLOQUEADA";
  const puedeCancelar =
    instancia.estado === "ACTIVA" ||
    instancia.estado === "ESPERANDO" ||
    instancia.estado === "BLOQUEADA";

  const version = consultaProceso.data?.versiones.find(
    (v) => v.numero === instancia.numeroVersion,
  );
  const grafo = version?.grafo;
  const estados = estadosDeNodo(instancia);
  if (!instancia.eventos?.length && instancia.estado === "COMPLETADA") {
    for (const nodo of grafo?.nodos ?? []) {
      if (!estados.has(nodo.id)) estados.set(nodo.id, "COMPLETADO");
    }
  }

  return (
    <div className="space-y-espacio-6">
      <div className="flex flex-wrap items-center justify-between gap-espacio-4">
        <Boton variante="fantasma" onClick={alVolver}>
          {t("operacion.volver")}
        </Boton>
        <Pastilla tono={tonoEstadoInstancia(instancia.estado)}>
          {t(`estadoInstancia.${instancia.estado}`)}
        </Pastilla>
      </div>

      <Tarjeta>
        <CabeceraTarjeta
          titulo={`${instancia.codigoDefinicion} · ${t("operacion.version", { version: instancia.numeroVersion })}`}
          descripcion={`${instancia.alta ? formatearFecha(instancia.alta) : "—"}${
            instancia.fin
              ? ` · ${t("operacion.finalizada", { fecha: formatearFecha(instancia.fin) })}`
              : ""
          }`}
        />
        {instancia.sujetoId ? (
          <p className="mt-espacio-3 text-pequeno text-tinta-suave">
            {t("operacion.sujeto")}: <EnlaceSujeto instancia={instancia} />
          </p>
        ) : null}
        {error ? (
          <div
            role="alert"
            className="mt-espacio-3 rounded-panel border border-rojo-borde bg-rojo-tenue px-espacio-4 py-espacio-3 text-pequeno text-rojo-alto"
          >
            {error}
          </div>
        ) : null}
        {puedePausar || puedeReanudar || puedeCancelar ? (
          <div className="mt-espacio-4 space-y-espacio-3">
            {pausar.isPending || cancelar.isPending ? null : (
              <Campo
                etiqueta={t("operacion.motivoPausa")}
                placeholder={t("operacion.motivoPausaPlaceholder")}
                value={motivo}
                onChange={(evento) => setMotivo(evento.target.value)}
              />
            )}
            <div className="flex flex-wrap gap-espacio-2">
              {puedePausar ? (
                <Boton
                  variante="secundario"
                  cargando={pausar.isPending}
                  disabled={pausar.isPending || !motivo.trim()}
                  onClick={() => pausar.mutate()}
                >
                  {t("operacion.pausar")}
                </Boton>
              ) : null}
              {puedeReanudar ? (
                <Boton
                  variante="primario"
                  cargando={reanudar.isPending}
                  disabled={reanudar.isPending}
                  onClick={() => reanudar.mutate()}
                >
                  {t("operacion.reanudar")}
                </Boton>
              ) : null}
              {puedeCancelar ? (
                <Boton
                  variante="peligro"
                  cargando={cancelar.isPending}
                  disabled={cancelar.isPending}
                  onClick={() => {
                    if (window.confirm(t("operacion.confirmarCancelar"))) {
                      cancelar.mutate();
                    }
                  }}
                >
                  {t("operacion.cancelarProceso")}
                </Boton>
              ) : null}
            </div>
          </div>
        ) : null}
      </Tarjeta>

      <Tarjeta>
        <CabeceraTarjeta
          titulo={t("operacion.recorrido")}
          descripcion={t("operacion.recorridoDesc")}
        />
        {consultaProceso.isPending ? (
          <Cargando filas={2} alto="h-10" />
        ) : consultaProceso.isError ? (
          <p className="mt-espacio-3 text-pequeno text-tinta-suave">
            {t("operacion.errorRecorrido")}
          </p>
        ) : (
          <div className="mt-espacio-4">
            <DiagramaProceso grafo={grafo} estadoPorNodo={estados} />
          </div>
        )}
      </Tarjeta>

      <Tarjeta>
        <CabeceraTarjeta
          titulo={t("operacion.tareas")}
          descripcion={t("operacion.tareasDesc")}
        />
        {instancia.tareas?.length ? (
          <ul className="mt-espacio-4 space-y-espacio-4">
            {instancia.tareas.map((tarea) => (
              <li key={tarea.id}>
                <TarjetaTarea tarea={tarea} />
              </li>
            ))}
          </ul>
        ) : (
          <p className="mt-espacio-3 text-pequeno text-tinta-suave">
            {t("operacion.sinTareas")}
          </p>
        )}
      </Tarjeta>

      <PanelHallazgos instanciaId={instancia.id} />

      {instancia.datos && Object.keys(instancia.datos).length ? (
        <Tarjeta>
          <CabeceraTarjeta
            titulo={t("operacion.datosProceso")}
            descripcion={t("operacion.datosDesc")}
          />
          <dl className="mt-espacio-4 space-y-espacio-2">
            {Object.entries(instancia.datos).map(([clave, valor]) => (
              <div
                key={clave}
                className="grid gap-espacio-1 rounded-control bg-lienzo px-espacio-4 py-espacio-2 md:grid-cols-[16rem_minmax(0,1fr)]"
              >
                <dt className="break-all text-pequeno text-tinta-suave">{clave}</dt>
                <dd className="break-all text-pequeno text-tinta">
                  {typeof valor === "object" ? JSON.stringify(valor) : String(valor)}
                </dd>
              </div>
            ))}
          </dl>
        </Tarjeta>
      ) : null}

      <Tarjeta>
        <CabeceraTarjeta
          titulo={t("operacion.lineaTiempo")}
          descripcion={t("operacion.lineaDesc")}
        />
        <LineaTiempo eventos={instancia.eventos ?? []} />
      </Tarjeta>
    </div>
  );
}

function tonoSeveridad(severidad?: string) {
  switch (severidad) {
    case "CRITICA":
      return "rojo" as const;
    case "ALTA":
      return "alerta" as const;
    case "MEDIA":
      return "informacion" as const;
    default:
      return "neutro" as const;
  }
}

function PanelHallazgos({ instanciaId }: { instanciaId: string }) {
  const { t } = useIdioma();
  const clienteConsultas = useQueryClient();
  const [error, setError] = useState<string | null>(null);

  const consulta = useQuery({
    queryKey: ["hallazgos", instanciaId],
    queryFn: () => listarHallazgos(instanciaId),
    refetchInterval: 15000,
  });

  const resolver = useMutation({
    mutationFn: ({ hallazgoId, estado }: { hallazgoId: string; estado: "APROBADO" | "RECHAZADO" }) =>
      resolverHallazgo(hallazgoId, estado),
    onSuccess: () => {
      setError(null);
      clienteConsultas.invalidateQueries({ queryKey: ["hallazgos", instanciaId] });
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const hallazgos = consulta.data ?? [];

  return (
    <Tarjeta>
      <CabeceraTarjeta
        titulo={t("operacion.hallazgosTitulo")}
        descripcion={t("operacion.hallazgosDesc")}
      />
      {consulta.isError ? (
        <p className="mt-espacio-3 text-pequeno text-tinta-suave">
          {t("operacion.errorHallazgos")}
        </p>
      ) : hallazgos.length === 0 ? (
        <p className="mt-espacio-3 text-pequeno text-tinta-suave">
          {t("operacion.sinHallazgos")}
        </p>
      ) : (
        <ul className="mt-espacio-4 space-y-espacio-3" aria-label={t("operacion.listaHallazgos")}>
          {hallazgos.map((hallazgo) => (
            <li
              key={hallazgo.id}
              className="rounded-control bg-lienzo px-espacio-4 py-espacio-3"
            >
              <div className="flex flex-wrap items-center gap-espacio-3">
                <Pastilla tono={tonoSeveridad(hallazgo.severidad)}>
                  {hallazgo.severidad
                    ? t(`prioridad.${hallazgo.severidad}`)
                    : t("operacion.hallazgo")}
                </Pastilla>
                <Pastilla tono={hallazgo.estado === "PENDIENTE" ? "informacion" : "neutro"}>
                  {hallazgo.estado
                    ? t(`estadoHallazgo.${hallazgo.estado}`)
                    : t("estadoHallazgo.PENDIENTE")}
                </Pastilla>
                <span className="text-pequeno text-tinta-suave">
                  {hallazgo.tipo}
                  {hallazgo.accion
                    ? ` · ${t(`accionSupervisora.${hallazgo.accion}`)}`
                    : ""}
                </span>
              </div>
              {hallazgo.descripcion ? (
                <p className="mt-espacio-2 text-pequeno text-tinta">{hallazgo.descripcion}</p>
              ) : null}
              {hallazgo.estado === "PENDIENTE" ? (
                <div className="mt-espacio-3 flex flex-wrap gap-espacio-2">
                  <Boton
                    variante="primario"
                    tamano="sm"
                    cargando={resolver.isPending}
                    disabled={resolver.isPending}
                    onClick={() =>
                      resolver.mutate({ hallazgoId: hallazgo.id, estado: "APROBADO" })
                    }
                  >
                    {t("operacion.aprobar")}
                  </Boton>
                  <Boton
                    variante="peligro"
                    tamano="sm"
                    cargando={resolver.isPending}
                    disabled={resolver.isPending}
                    onClick={() =>
                      resolver.mutate({ hallazgoId: hallazgo.id, estado: "RECHAZADO" })
                    }
                  >
                    {t("operacion.rechazar")}
                  </Boton>
                </div>
              ) : null}
            </li>
          ))}
        </ul>
      )}
      {error ? (
        <div
          role="alert"
          className="mt-espacio-3 rounded-panel border border-rojo-borde bg-rojo-tenue px-espacio-4 py-espacio-3 text-pequeno text-rojo-alto"
        >
          {error}
        </div>
      ) : null}
    </Tarjeta>
  );
}

function LineaTiempo({ eventos }: { eventos: EventoInstancia[] }) {
  const { t } = useIdioma();
  if (!eventos.length) {
    return (
      <p className="mt-espacio-3 text-pequeno text-tinta-suave">
        {t("operacion.sinEventos")}
      </p>
    );
  }
  const ordenados = [...eventos].sort((a, b) =>
    (a.alta ?? "").localeCompare(b.alta ?? ""),
  );
  return (
    <ol className="mt-espacio-4 space-y-espacio-3" aria-label={t("operacion.eventosAria")}>
      {ordenados.map((evento) => (
        <li
          key={evento.id}
          className="grid gap-espacio-1 rounded-control bg-lienzo px-espacio-4 py-espacio-3 md:grid-cols-[10rem_minmax(0,1fr)_auto]"
        >
          <span className="text-pequeno text-tinta-suave">
            {evento.alta ? formatearFecha(evento.alta) : "—"}
          </span>
          <span className="text-pequeno text-tinta">
            {textoAccion(evento.accion, t)}
            {evento.nodoId ? ` · ${evento.nodoId}` : ""}
          </span>
          <span className="text-pequeno text-tinta-suave">
            {evento.actor ?? t("operacion.sistema")}
          </span>
        </li>
      ))}
    </ol>
  );
}

export function Operacion() {
  const { t } = useIdioma();
  const { instanciaId } = useParams();
  const navegar = useNavigate();

  return (
    <>
      <Encabezado
        titulo={t("operacion.titulo")}
        descripcion={t("operacion.descripcion")}
      />
      <Contenido>
        {instanciaId ? (
          <DetalleInstancia
            instanciaId={instanciaId}
            alVolver={() => navegar("/operacion")}
          />
        ) : (
          <BandejaInstancias
            alAbrir={(id) => navegar(`/operacion/instancias/${id}`)}
          />
        )}
      </Contenido>
    </>
  );
}
