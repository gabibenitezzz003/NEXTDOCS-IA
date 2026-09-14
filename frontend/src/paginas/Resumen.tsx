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
import { obtenerResumen } from "../api/documentos";
import { listarExcepciones } from "../api/excepciones";
import { mensajeDeError } from "../api/cliente";
import { useSesion } from "../contextos/ProveedorSesion";
import { ESTADOS_DOCUMENTALES } from "../utilidades/estadosDocumento";
import type { EstadoDocumento } from "../tipos/api";

const TECNICAS = ["profundidadCola", "profundidadReintento"];
const DESTACADOS: { clave: EstadoDocumento; etiqueta: string }[] = [
  { clave: "OBSERVADO", etiqueta: "Requieren revisión" },
  { clave: "APROBADO", etiqueta: "Aprobados" },
  { clave: "RECIBIDO", etiqueta: "Recibidos" },
  { clave: "RECHAZADO", etiqueta: "Rechazados" },
];
const GRILLA_METRICAS =
  "grid min-w-0 gap-espacio-4 sm:grid-cols-2 xl:grid-cols-5";
const GRILLA_OPERACION =
  "mt-espacio-6 grid min-w-0 items-start gap-espacio-6 xl:grid-cols-[minmax(0,1.35fr)_minmax(0,1fr)]";

export function Resumen() {
  const { tienePermiso } = useSesion();

  const resumen = useQuery({ queryKey: ["resumen"], queryFn: obtenerResumen });
  const excepciones = useQuery({
    queryKey: ["excepciones", "ABIERTA", 0, 5],
    queryFn: () => listarExcepciones("ABIERTA", 0, 5),
    enabled: tienePermiso("excepciones.leer"),
  });

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
        titulo="Resumen operativo"
        descripcion="Estado documental actual, sin recorte de fechas."
        acciones={
          <Link
            to="/panel"
            className="inline-flex min-h-control-mediano items-center gap-espacio-2 rounded-control border border-borde bg-superficie px-espacio-4 text-pequeno font-semibold text-tinta transition-colors hover:bg-lienzo focus-visible:outline-foco"
          >
            Ver panel de control
            <span aria-hidden="true">
              <IconoDerecha tamano={14} />
            </span>
          </Link>
        }
      />

      <Contenido>
        {resumen.isPending ? (
          <div role="status" aria-busy="true" aria-atomic="true">
            <span className="sr-only">Cargando resumen operativo</span>
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
            titulo="No se pudo cargar el resumen"
            mensaje={mensajeDeError(resumen.error)}
            reintentar={() => resumen.refetch()}
          />
        ) : (
          <>
            <section
              aria-label="Indicadores documentales"
              className={GRILLA_METRICAS}
            >
              <Tarjeta
                padding="p-espacio-4"
                className="min-h-36 rounded-metrica! sm:col-span-2 xl:col-span-1"
              >
                <Metrica
                  etiqueta="Documentos totales"
                  valor={totalDocumentos.toLocaleString("es-AR")}
                  detalle={
                    <span className="flex items-start gap-espacio-2">
                      <span aria-hidden="true" className="shrink-0">
                        <IconoReloj tamano={14} />
                      </span>
                      <span>
                        <span className="tabular-nums">
                          {enCola.toLocaleString("es-AR")}
                        </span>{" "}
                        en cola de extracción del servicio
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
                      etiqueta={destacado.etiqueta}
                      valor={cantidad.toLocaleString("es-AR")}
                      detalle={
                        <span className="flex flex-wrap items-center gap-espacio-2">
                          <InsigniaEstado estado={destacado.clave} />
                          <span className="tabular-nums">
                            {porcentaje(cantidad, totalDocumentos)}
                          </span>
                        </span>
                      }
                    />
                  </Tarjeta>
                );
              })}
            </section>

            <section
              aria-label="Operación y atención"
              className={GRILLA_OPERACION}
            >
              <Tarjeta className="rounded-panel!">
                <CabeceraTarjeta
                  titulo="Distribución por estado"
                  descripcion="Documentos actuales de la organización, sin recorte de fechas."
                />
                {totalDocumentos === 0 ? (
                  <div className="mt-espacio-6">
                    <Vacio
                      titulo="Todavía no hay documentos"
                      detalle="Cuando ingrese el primer documento, su estado aparecerá aquí."
                    />
                  </div>
                ) : (
                  <figure className="mt-espacio-6">
                    <div aria-hidden="true" className="hidden min-w-0 sm:block">
                      <Columnas
                        barras={estados
                          .sort((uno, otro) => Number(otro[1]) - Number(uno[1]))
                          .map(([estado, cantidad]) => ({
                            etiqueta:
                              ESTADOS_DOCUMENTALES[estado as EstadoDocumento]
                                ?.etiqueta ?? estado,
                            valor: Number(cantidad),
                            color:
                              ESTADOS_DOCUMENTALES[estado as EstadoDocumento]
                                ?.color ?? "var(--color-tinta-suave)",
                          }))}
                        alto={168}
                      />
                    </div>
                    <figcaption className="sr-only">
                      Cantidad de documentos por estado, de mayor a menor.
                    </figcaption>
                    <dl
                      aria-label="Cantidad por estado"
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
                            {Number(cantidad).toLocaleString("es-AR")}
                          </dd>
                        </div>
                      ))}
                    </dl>
                  </figure>
                )}
              </Tarjeta>

              <Tarjeta className="rounded-panel!">
                <CabeceraTarjeta
                  titulo="Excepciones abiertas"
                  descripcion="Pendientes de una decisión humana."
                  acciones={
                    tienePermiso("excepciones.leer") ? (
                      <Link
                        to="/excepciones"
                        className="inline-flex min-h-control-pequeno items-center gap-espacio-1 rounded-control text-pequeno font-semibold text-accion-tonal-texto hover:underline focus-visible:outline-foco"
                      >
                        Ver todas
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
                      No tenés permiso para ver excepciones.
                    </AvisoLinea>
                  ) : excepciones.isPending ? (
                    <Cargando filas={3} alto="h-20" />
                  ) : excepciones.isError ? (
                    <ErrorPanel
                      titulo="No se pudieron cargar las excepciones"
                      mensaje={mensajeDeError(excepciones.error)}
                      reintentar={() => excepciones.refetch()}
                    />
                  ) : !excepciones.data?.content.length ? (
                    <p
                      role="status"
                      aria-atomic="true"
                      className="rounded-control border border-exito-borde bg-exito-tenue p-espacio-4 text-pequeno text-exito-texto"
                    >
                      No hay excepciones abiertas.
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

function porcentaje(parte: number, total: number) {
  if (!total) {
    return "0% del total";
  }
  return `${Math.round((parte / total) * 100)}% del total`;
}
