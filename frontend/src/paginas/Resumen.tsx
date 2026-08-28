import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Encabezado } from "../componentes/Disposicion";
import { Cargando, ErrorPanel } from "../componentes/Estados";
import { obtenerResumen } from "../api/documentos";
import { listarExcepciones } from "../api/excepciones";
import { mensajeDeError } from "../api/cliente";
import { InsigniaSeveridad } from "../componentes/Insignias";
import { useSesion } from "../contextos/ProveedorSesion";

const DESTACADOS: { clave: string; etiqueta: string; tono: string }[] = [
  { clave: "APROBADO", etiqueta: "Aprobados", tono: "text-exito" },
  { clave: "OBSERVADO", etiqueta: "Observados", tono: "text-alerta" },
  { clave: "RECIBIDO", etiqueta: "En cola", tono: "text-tinta" },
  { clave: "RECHAZADO", etiqueta: "Rechazados", tono: "text-rojo" },
];

export function Resumen() {
  const { sesion, tienePermiso } = useSesion();

  const resumen = useQuery({ queryKey: ["resumen"], queryFn: obtenerResumen });
  const excepciones = useQuery({
    queryKey: ["excepciones", "ABIERTA", 0],
    queryFn: () => listarExcepciones("ABIERTA", 0, 5),
    enabled: tienePermiso("excepciones.leer"),
  });

  const datos = resumen.data ?? {};
  const totalDocumentos = Object.entries(datos)
    .filter(([clave]) => clave !== "profundidadCola" && clave !== "profundidadReintento")
    .reduce((suma, [, valor]) => suma + Number(valor ?? 0), 0);

  return (
    <>
      <Encabezado
        titulo={`Hola, ${sesion?.nombre?.split(" ")[0] ?? ""}`}
        descripcion={`Estado documental de ${sesion?.nombreTenant ?? "tu organizacion"}.`}
      />
      <div className="px-8 py-6">
        {resumen.isPending ? (
          <Cargando filas={3} />
        ) : resumen.isError ? (
          <ErrorPanel mensaje={mensajeDeError(resumen.error)} reintentar={() => resumen.refetch()} />
        ) : (
          <>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              <article className="rounded-xl border border-borde bg-grafito px-5 py-4 text-white">
                <p className="text-xs uppercase tracking-wide text-white/50">Documentos totales</p>
                <p className="mt-2 font-titulo text-3xl">{totalDocumentos.toLocaleString("es-AR")}</p>
                <p className="mt-1 text-xs text-white/50">
                  {Number(datos.profundidadCola ?? 0)} en cola de extraccion
                </p>
              </article>
              {DESTACADOS.map((destacado) => (
                <article key={destacado.clave} className="rounded-xl border border-borde bg-white px-5 py-4">
                  <p className="text-xs uppercase tracking-wide text-tinta-suave">{destacado.etiqueta}</p>
                  <p className={`mt-2 font-titulo text-3xl ${destacado.tono}`}>
                    {Number(datos[destacado.clave] ?? 0).toLocaleString("es-AR")}
                  </p>
                  <p className="mt-1 text-xs text-tinta-suave">
                    {porcentaje(Number(datos[destacado.clave] ?? 0), totalDocumentos)} del total
                  </p>
                </article>
              ))}
            </div>

            <section className="mt-8 grid gap-6 lg:grid-cols-[1.3fr_1fr]">
              <div className="rounded-xl border border-borde bg-white p-5">
                <h2 className="font-titulo text-base text-tinta">Distribucion por estado</h2>
                <ul className="mt-4 space-y-2.5">
                  {Object.entries(datos)
                    .filter(([clave]) => clave !== "profundidadCola" && clave !== "profundidadReintento")
                    .sort((uno, otro) => Number(otro[1]) - Number(uno[1]))
                    .map(([estado, cantidad]) => (
                      <li key={estado} className="flex items-center gap-3">
                        <span className="w-24 shrink-0 text-xs text-tinta-suave">{estado}</span>
                        <div className="h-2 flex-1 overflow-hidden rounded-full bg-lienzo">
                          <div
                            className="degradado-marca h-full"
                            style={{
                              width: totalDocumentos
                                ? `${Math.max((Number(cantidad) / totalDocumentos) * 100, Number(cantidad) ? 2 : 0)}%`
                                : "0%",
                            }}
                          />
                        </div>
                        <span className="w-10 text-right text-xs tabular-nums text-tinta">
                          {Number(cantidad)}
                        </span>
                      </li>
                    ))}
                </ul>
              </div>

              <div className="rounded-xl border border-borde bg-white p-5">
                <div className="flex items-center justify-between">
                  <h2 className="font-titulo text-base text-tinta">Excepciones abiertas</h2>
                  {tienePermiso("excepciones.leer") ? (
                    <Link to="/excepciones" className="text-xs font-medium text-violeta hover:underline">
                      Ver todas
                    </Link>
                  ) : null}
                </div>
                {!tienePermiso("excepciones.leer") ? (
                  <p className="mt-4 text-sm text-tinta-suave">No tenes permiso para ver excepciones.</p>
                ) : excepciones.isPending ? (
                  <div className="mt-4">
                    <Cargando filas={3} />
                  </div>
                ) : excepciones.isError ? (
                  <p className="mt-4 text-sm text-rojo">{mensajeDeError(excepciones.error)}</p>
                ) : !excepciones.data?.content.length ? (
                  <p className="mt-4 text-sm text-tinta-suave">No hay excepciones abiertas.</p>
                ) : (
                  <ul className="mt-4 space-y-2.5">
                    {excepciones.data.content.map((excepcion) => (
                      <li key={excepcion.id} className="rounded-lg border border-borde px-3 py-2.5">
                        <div className="flex items-center gap-2">
                          <InsigniaSeveridad severidad={excepcion.severidad} />
                          <span className="text-xs text-tinta-suave">{excepcion.tipo}</span>
                        </div>
                        <p className="mt-1 line-clamp-2 text-sm text-tinta">{excepcion.detalle}</p>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </section>
          </>
        )}
      </div>
    </>
  );
}

function porcentaje(parte: number, total: number) {
  if (!total) {
    return "0%";
  }
  return `${Math.round((parte / total) * 100)}%`;
}
