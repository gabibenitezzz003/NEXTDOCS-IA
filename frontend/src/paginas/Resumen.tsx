import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Contenido, Encabezado } from "../componentes/Disposicion";
import {
  AvisoLinea,
  Cargando,
  ErrorPanel,
  Vacio,
} from "../componentes/Estados";
import { CabeceraTarjeta, Metrica, Tarjeta } from "../componentes/Interfaz";
import { Columnas } from "../componentes/Graficos";
import { IconoDerecha, IconoReloj } from "../componentes/Iconos";
import { InsigniaEstado, InsigniaSeveridad } from "../componentes/Insignias";
import { obtenerResumen, listarDocumentos } from "../api/documentos";
import { listarExcepciones } from "../api/excepciones";
import { listarTareas, listarInstancias } from "../api/procesos";
import type { TareaProceso } from "../api/procesos";
import { mensajeDeError } from "../api/cliente";
import { useIdioma } from "../contextos/ProveedorIdioma";
import { formatearNumero } from "../i18n";
import { useSesion } from "../contextos/ProveedorSesion";
import { ESTADOS_DOCUMENTALES } from "../utilidades/estadosDocumento";
import { formatearFecha } from "./Documentos";
import { textoTipoTarea } from "../componentes/TarjetaTarea";
import { Pastilla } from "../componentes/Interfaz";
import type { EstadoDocumento } from "../tipos/api";

const TECNICAS = ["profundidadCola", "profundidadReintento"];
const DESTACADOS: { clave: EstadoDocumento; etiqueta: string }[] = [
  { clave: "OBSERVADO", etiqueta: "resumen.requierenRevision" },
  { clave: "APROBADO", etiqueta: "resumen.aprobados" },
  { clave: "RECIBIDO", etiqueta: "resumen.recibidos" },
  { clave: "RECHAZADO", etiqueta: "resumen.rechazados" },
];
const GRILLA_METRICAS =
  "grid min-w-0 gap-espacio-4 sm:grid-cols-2 xl:grid-cols-5";
const GRILLA_OPERACION =
  "mt-espacio-6 grid min-w-0 items-start gap-espacio-6 xl:grid-cols-[minmax(0,1.35fr)_minmax(0,1fr)]";

export function Resumen() {
  const { tienePermiso, sesion } = useSesion();
  const { t } = useIdioma();

  const resumen = useQuery({ queryKey: ["resumen"], queryFn: obtenerResumen });
  const excepciones = useQuery({
    queryKey: ["excepciones", "ABIERTA", 0, 5],
    queryFn: () => listarExcepciones("ABIERTA", 0, 5),
    enabled: tienePermiso("excepciones.leer"),
  });
  const tareasPendientes = useQuery({
    queryKey: ["tareas", "pendientes"],
    queryFn: () => listarTareas(["PENDIENTE", "VENCIDA"]),
  });
  const tareasCompletadas = useQuery({
    queryKey: ["tareas", "recientes"],
    queryFn: () => listarTareas(["COMPLETADA"]),
  });
  const bloqueadas = useQuery({
    queryKey: ["instancias", "BLOQUEADA"],
    queryFn: () => listarInstancias("BLOQUEADA"),
  });
  const observados = useQuery({
    queryKey: ["documentos", "observados"],
    queryFn: () => listarDocumentos({ estados: ["OBSERVADO"], tamano: 1 }),
  });

  const pendientes = (tareasPendientes.data ?? [])
    .slice()
    .sort((una, otra) => {
      const vencida = (item: TareaProceso) =>
        item.vencimiento && new Date(item.vencimiento).getTime() < Date.now()
          ? 0
          : 1;
      const porEstado = vencida(una) - vencida(otra);
      if (porEstado !== 0) return porEstado;
      const fecha = (item: TareaProceso) =>
        item.vencimiento ? new Date(item.vencimiento).getTime() : Infinity;
      return fecha(una) - fecha(otra);
    });
  const recientes = (tareasCompletadas.data ?? [])
    .slice()
    .sort(
      (una, otra) =>
        new Date(otra.completada ?? 0).getTime() -
        new Date(una.completada ?? 0).getTime(),
    )
    .slice(0, 5);

  const datos = resumen.data ?? {};
  const estados = Object.entries(datos).filter(
    ([clave]) => !TECNICAS.includes(clave),
  );
  const totalDocumentos = estados.reduce(
    (suma, [, valor]) => suma + Number(valor ?? 0),
    0,
  );
  const enCola = Number(datos.profundidadCola ?? 0);

  return (
    <>
      <Encabezado
        titulo={t("resumen.titulo")}
        descripcion={t("resumen.descripcion")}
        acciones={
          <Link
            to="/panel"
            className="inline-flex min-h-control-mediano items-center gap-espacio-2 rounded-control border border-borde bg-superficie px-espacio-4 text-pequeno font-semibold text-tinta transition-colors hover:bg-lienzo focus-visible:outline-foco"
          >
            {t("resumen.verPanel")}
            <span aria-hidden="true">
              <IconoDerecha tamano={14} />
            </span>
          </Link>
        }
      />

      <Contenido>
        <section
          aria-label={t("resumen.accionRequerida")}
          className="mb-espacio-6 grid min-w-0 items-start gap-espacio-6 xl:grid-cols-[minmax(0,1.35fr)_minmax(0,1fr)]"
        >
          <Tarjeta className="rounded-panel!">
            <CabeceraTarjeta
              titulo={t("resumen.miTrabajo")}
              descripcion={t("resumen.miTrabajoDesc")}
              acciones={
                <Link
                  to="/tareas"
                  className="inline-flex min-h-control-pequeno items-center gap-espacio-1 rounded-control text-pequeno font-semibold text-accion-tonal-texto hover:underline focus-visible:outline-foco"
                >
                  {t("comun.verTodas")}
                  <span aria-hidden="true">
                    <IconoDerecha tamano={13} />
                  </span>
                </Link>
              }
            />
            <div className="mt-espacio-5">
              {tareasPendientes.isPending ? (
                <Cargando filas={3} alto="h-16" />
              ) : tareasPendientes.isError ? (
                <AvisoLinea>{mensajeDeError(tareasPendientes.error)}</AvisoLinea>
              ) : !pendientes.length ? (
                <p
                  role="status"
                  className="rounded-control border border-exito-borde bg-exito-tenue p-espacio-4 text-pequeno text-exito-texto"
                >
                  {t("resumen.alDia")}
                </p>
              ) : (
                <ul className="divide-y divide-borde">
                  {pendientes.slice(0, 6).map((tarea) => {
                    const mia = tarea.asignadoA === sesion?.email;
                    const vencida =
                      tarea.vencimiento &&
                      new Date(tarea.vencimiento).getTime() < Date.now();
                    return (
                      <li key={tarea.id} className="py-espacio-3 first:pt-0 last:pb-0">
                        <div className="flex flex-wrap items-center justify-between gap-espacio-2">
                          <div className="min-w-0">
                            <p className="truncate text-pequeno font-semibold text-tinta">
                              {tarea.nombreNodo?.trim() ||
                                textoTipoTarea(tarea.tipoNodo, t)}
                            </p>
                            <p className="truncate text-micro text-tinta-suave">
                              {tarea.nombreDefinicion?.trim() ||
                                tarea.codigoDefinicion ||
                                ""}
                              {tarea.asignadoA && !mia
                                ? ` · ${t("resumen.asignadaA", { actor: tarea.asignadoA })}`
                                : ""}
                              {!tarea.asignadoA
                                ? ` · ${t("resumen.sinAsignar")}`
                                : ""}
                            </p>
                          </div>
                          {tarea.vencimiento ? (
                            <Pastilla tono={vencida ? "rojo" : "alerta"}>
                              {vencida
                                ? t("resumen.vencidaEl", {
                                    fecha: formatearFecha(tarea.vencimiento),
                                  })
                                : t("resumen.venceEl", {
                                    fecha: formatearFecha(tarea.vencimiento),
                                  })}
                            </Pastilla>
                          ) : (
                            <Pastilla tono="neutro">
                              {t(`estadoTarea.${tarea.estado}`)}
                            </Pastilla>
                          )}
                        </div>
                      </li>
                    );
                  })}
                </ul>
              )}
            </div>
          </Tarjeta>

          <div className="grid min-w-0 gap-espacio-6">
            <Tarjeta className="rounded-panel!">
              <CabeceraTarjeta
                titulo={t("resumen.atencion")}
                descripcion={t("resumen.atencionDesc")}
              />
              <ul className="mt-espacio-4 divide-y divide-borde">
                <li>
                  <Link
                    to="/operacion"
                    className="flex items-center justify-between gap-espacio-2 py-espacio-3 text-pequeno transition-colors hover:text-accion-tonal-texto focus-visible:outline-foco"
                  >
                    <span className="text-tinta">{t("resumen.procesosBloqueados")}</span>
                    <Pastilla
                      tono={bloqueadas.data?.length ? "rojo" : "neutro"}
                    >
                      {formatearNumero(bloqueadas.data?.length ?? 0)}
                    </Pastilla>
                  </Link>
                </li>
                <li>
                  <Link
                    to="/documentos"
                    className="flex items-center justify-between gap-espacio-2 py-espacio-3 text-pequeno transition-colors hover:text-accion-tonal-texto focus-visible:outline-foco"
                  >
                    <span className="text-tinta">
                      {t("resumen.documentosObservados")}
                    </span>
                    <Pastilla
                      tono={observados.data?.totalElements ? "alerta" : "neutro"}
                    >
                      {formatearNumero(observados.data?.totalElements ?? 0)}
                    </Pastilla>
                  </Link>
                </li>
              </ul>
            </Tarjeta>

            <Tarjeta className="rounded-panel!">
              <CabeceraTarjeta
                titulo={t("resumen.actividadReciente")}
                descripcion={t("resumen.actividadRecienteDesc")}
              />
              <div className="mt-espacio-4">
                {tareasCompletadas.isPending ? (
                  <Cargando filas={3} alto="h-10" />
                ) : !recientes.length ? (
                  <p className="text-pequeno text-tinta-suave">
                    {t("resumen.sinActividad")}
                  </p>
                ) : (
                  <ul className="divide-y divide-borde">
                    {recientes.map((tarea) => (
                      <li key={tarea.id} className="py-espacio-3 first:pt-0 last:pb-0">
                        <p className="truncate text-pequeno text-tinta">
                          {tarea.nombreNodo?.trim() ||
                            textoTipoTarea(tarea.tipoNodo, t)}
                          {tarea.decision
                            ? ` · ${t(`estadoHallazgo.${tarea.decision}`)}`
                            : ""}
                        </p>
                        <p className="truncate text-micro text-tinta-suave">
                          {tarea.nombreDefinicion?.trim() ||
                            tarea.codigoDefinicion ||
                            ""}
                          {tarea.completadaPor
                            ? ` · ${tarea.completadaPor}`
                            : ""}
                          {tarea.completada
                            ? ` · ${formatearFecha(tarea.completada)}`
                            : ""}
                        </p>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </Tarjeta>
          </div>
        </section>

        {resumen.isPending ? (
          <div role="status" aria-busy="true" aria-atomic="true">
            <span className="sr-only">{t("resumen.cargando")}</span>
            <div aria-hidden="true">
              <div className={GRILLA_METRICAS}>
                {Array.from({ length: 5 }).map((_, indice) => (
                  <div
                    key={indice}
                    className={
                      indice === 0 ? "sm:col-span-2 xl:col-span-1" : ""
                    }
                  >
                    <Cargando filas={1} alto="h-36" />
                  </div>
                ))}
              </div>
              <div className={GRILLA_OPERACION}>
                <Cargando filas={1} alto="h-96" />
                <Cargando filas={1} alto="h-96" />
              </div>
            </div>
          </div>
        ) : resumen.isError ? (
          <ErrorPanel
            contexto={t("resumen.errorContexto")}
            mensaje={mensajeDeError(resumen.error)}
          error={resumen.error}
            reintentar={() => resumen.refetch()}
          />
        ) : (
          <>
            <section
              aria-label={t("resumen.indicadores")}
              className={GRILLA_METRICAS}
            >
              <Tarjeta
                padding="p-espacio-4"
                className="min-h-36 rounded-metrica! sm:col-span-2 xl:col-span-1"
              >
                <Metrica
                  etiqueta={t("resumen.documentosTotales")}
                  valor={formatearNumero(totalDocumentos)}
                  detalle={
                    <span className="flex items-start gap-espacio-2">
                      <span aria-hidden="true" className="shrink-0">
                        <IconoReloj tamano={14} />
                      </span>
                      <span>
                        <span className="tabular-nums">
                          {formatearNumero(enCola)}
                        </span>{" "}
                        {t("resumen.enCola")}
                      </span>
                    </span>
                  }
                />
              </Tarjeta>
              {DESTACADOS.map((destacado) => {
                const cantidad = Number(datos[destacado.clave] ?? 0);
                return (
                  <Tarjeta
                    key={destacado.clave}
                    padding="p-espacio-4"
                    className={`min-h-36 rounded-metrica! ${destacado.clave === "OBSERVADO" ? "border-alerta-borde! bg-alerta-tenue!" : ""}`}
                  >
                    <Metrica
                      etiqueta={t(destacado.etiqueta)}
                      valor={formatearNumero(cantidad)}
                      detalle={
                        <span className="flex flex-wrap items-center gap-espacio-2">
                          <InsigniaEstado estado={destacado.clave} />
                          <span className="tabular-nums">
                            {porcentaje(cantidad, totalDocumentos, t)}
                          </span>
                        </span>
                      }
                    />
                  </Tarjeta>
                );
              })}
            </section>

            <section
              aria-label={t("resumen.operacionYAtencion")}
              className={GRILLA_OPERACION}
            >
              <Tarjeta className="rounded-panel!">
                <CabeceraTarjeta
                  titulo={t("resumen.distribucion")}
                  descripcion={t("resumen.distribucionDesc")}
                />
                {totalDocumentos === 0 ? (
                  <div className="mt-espacio-6">
                    <Vacio
                      titulo={t("resumen.sinDocumentos")}
                      detalle={t("resumen.sinDocumentosDetalle")}
                    />
                  </div>
                ) : (
                  <figure className="mt-espacio-6">
                    <div aria-hidden="true" className="hidden min-w-0 sm:block">
                      <Columnas
                        barras={estados
                          .sort((uno, otro) => Number(otro[1]) - Number(uno[1]))
                          .map(([estado, cantidad]) => ({
                            etiqueta: t(`estadosDocumento.${estado}`),
                            valor: Number(cantidad),
                            color:
                              ESTADOS_DOCUMENTALES[estado as EstadoDocumento]
                                ?.color ?? "var(--color-tinta-suave)",
                          }))}
                        alto={168}
                      />
                    </div>
                    <figcaption className="sr-only">
                      {t("resumen.figcaption")}
                    </figcaption>
                    <dl
                      aria-label={t("resumen.cantidadPorEstado")}
                      className="grid min-w-0 gap-x-espacio-6 divide-y divide-borde sm:mt-espacio-6 sm:grid-cols-2"
                    >
                      {estados.map(([estado, cantidad]) => (
                        <div
                          key={estado}
                          className="flex min-w-0 flex-wrap items-center justify-between gap-espacio-2 py-espacio-3"
                        >
                          <dt>
                            <InsigniaEstado
                              estado={estado as EstadoDocumento}
                            />
                          </dt>
                          <dd className="text-pequeno font-semibold text-tinta tabular-nums">
                            {formatearNumero(Number(cantidad))}
                          </dd>
                        </div>
                      ))}
                    </dl>
                  </figure>
                )}
              </Tarjeta>

              <Tarjeta className="rounded-panel!">
                <CabeceraTarjeta
                  titulo={t("resumen.excepcionesAbiertas")}
                  descripcion={t("resumen.excepcionesDesc")}
                  acciones={
                    tienePermiso("excepciones.leer") ? (
                      <Link
                        to="/excepciones"
                        className="inline-flex min-h-control-pequeno items-center gap-espacio-1 rounded-control text-pequeno font-semibold text-accion-tonal-texto hover:underline focus-visible:outline-foco"
                      >
                        {t("comun.verTodas")}
                        <span aria-hidden="true">
                          <IconoDerecha tamano={13} />
                        </span>
                      </Link>
                    ) : undefined
                  }
                />
                <div className="mt-espacio-5">
                  {!tienePermiso("excepciones.leer") ? (
                    <AvisoLinea>
                      {t("resumen.sinPermisoExcepciones")}
                    </AvisoLinea>
                  ) : excepciones.isPending ? (
                    <Cargando filas={3} alto="h-20" />
                  ) : excepciones.isError ? (
                    <ErrorPanel
                      contexto={t("resumen.errorExcepciones")}
                      mensaje={mensajeDeError(excepciones.error)}
          error={excepciones.error}
                      reintentar={() => excepciones.refetch()}
                    />
                  ) : !excepciones.data?.content.length ? (
                    <p
                      role="status"
                      aria-atomic="true"
                      className="rounded-control border border-exito-borde bg-exito-tenue p-espacio-4 text-pequeno text-exito-texto"
                    >
                      {t("resumen.sinExcepcionesAbiertas")}
                    </p>
                  ) : (
                    <ul className="divide-y divide-borde">
                      {excepciones.data.content.map((excepcion) => (
                        <li
                          key={excepcion.id}
                          className="py-espacio-4 first:pt-0 last:pb-0"
                        >
                          <div className="flex flex-wrap items-start justify-between gap-espacio-2">
                            <InsigniaSeveridad
                              severidad={excepcion.severidad}
                            />
                            <span className="min-w-0 text-micro uppercase tracking-wide text-tinta-suave [overflow-wrap:anywhere]">
                              {excepcion.tipo}
                            </span>
                          </div>
                          <p className="mt-espacio-2 text-pequeno text-tinta-media [overflow-wrap:anywhere]">
                            {excepcion.detalle}
                          </p>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </Tarjeta>
            </section>
          </>
        )}
      </Contenido>
    </>
  );
}

function porcentaje(
  parte: number,
  total: number,
  t: (ruta: string, params?: Record<string, string | number>) => string,
) {
  if (!total) {
    return t("resumen.ceroDelTotal");
  }
  return t("resumen.delTotal", { porcentaje: Math.round((parte / total) * 100) });
}
