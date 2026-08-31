import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Contenido, Encabezado } from "../componentes/Disposicion";
import { Cargando, CargandoTarjetas, ErrorPanel } from "../componentes/Estados";
import { CabeceraTarjeta, Metrica, Pastilla, Tarjeta } from "../componentes/Interfaz";
import { Columnas, useContador, useVisible } from "../componentes/Graficos";
import type { ClaveTono } from "../componentes/Graficos";
import { IconoDerecha, IconoReloj } from "../componentes/Iconos";
import { InsigniaSeveridad } from "../componentes/Insignias";
import { obtenerResumen } from "../api/documentos";
import { listarExcepciones } from "../api/excepciones";
import { mensajeDeError } from "../api/cliente";
import { useSesion } from "../contextos/ProveedorSesion";
import type { Tono } from "../componentes/Interfaz";

const TECNICAS = ["profundidadCola", "profundidadReintento"];

const DESTACADOS: { clave: string; etiqueta: string; tono: Tono }[] = [
  { clave: "OBSERVADO", etiqueta: "Requieren revision", tono: "alerta" },
  { clave: "APROBADO", etiqueta: "Aprobados", tono: "exito" },
  { clave: "RECIBIDO", etiqueta: "En cola", tono: "informacion" },
  { clave: "RECHAZADO", etiqueta: "Rechazados", tono: "rojo" },
];

const AURA: Record<Tono, string> = {
  neutro: "bg-tinta-tenue/10",
  violeta: "bg-violeta/12",
  exito: "bg-exito/12",
  alerta: "bg-alerta/14",
  rojo: "bg-rojo/10",
  informacion: "bg-informacion/12",
};

const TONO_BARRA_ESTADO: Record<string, ClaveTono> = {
  APROBADO: "exito",
  CERRADO: "neutro",
  OBSERVADO: "alerta",
  RECHAZADO: "rojo",
  DIVIDIDO: "informacion",
};

export function Resumen() {
  const { sesion, tienePermiso } = useSesion();

  const resumen = useQuery({ queryKey: ["resumen"], queryFn: obtenerResumen });
  const excepciones = useQuery({
    queryKey: ["excepciones", "ABIERTA", 0],
    queryFn: () => listarExcepciones("ABIERTA", 0, 5),
    enabled: tienePermiso("excepciones.leer"),
  });

  const datos = resumen.data ?? {};
  const estados = Object.entries(datos).filter(([clave]) => !TECNICAS.includes(clave));
  const totalDocumentos = estados.reduce((suma, [, valor]) => suma + Number(valor ?? 0), 0);
  const enCola = Number(datos.profundidadCola ?? 0);

  return (
    <>
      <Encabezado
        titulo={`Hola, ${sesion?.nombre?.split(" ")[0] ?? ""}`}
        descripcion={`Estado documental de ${sesion?.nombreTenant ?? "tu organizacion"}.`}
        acciones={
          <Link
            to="/panel"
            className="inline-flex h-9.5 items-center gap-1.5 rounded-xl border border-borde bg-white px-4 text-sm font-semibold text-tinta shadow-plano transition hover:border-borde-fuerte hover:bg-lienzo"
          >
            Ver panel de control
            <IconoDerecha tamano={14} />
          </Link>
        }
      />

      <Contenido>
        {resumen.isPending ? (
          <CargandoTarjetas />
        ) : resumen.isError ? (
          <ErrorPanel mensaje={mensajeDeError(resumen.error)} reintentar={() => resumen.refetch()} />
        ) : (
          <>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-5">
              <TarjetaTotal total={totalDocumentos} enCola={enCola} />

              {DESTACADOS.map((destacado, indice) => {
                const cantidad = Number(datos[destacado.clave] ?? 0);
                return (
                  <Tarjeta
                    key={destacado.clave}
                    padding="px-5 py-4"
                    indice={indice + 1}
                    interactiva
                    className="overflow-hidden"
                  >
                    <span
                      aria-hidden
                      className={`pointer-events-none absolute -right-8 -top-10 size-28 rounded-full blur-2xl ${AURA[destacado.tono]}`}
                    />
                    <Metrica
                      etiqueta={destacado.etiqueta}
                      valor={<Contado valor={cantidad} />}
                      detalle={
                        <span className="flex items-center gap-2">
                          <Pastilla tono={destacado.tono}>{destacado.clave}</Pastilla>
                          <span className="tabular-nums">{porcentaje(cantidad, totalDocumentos)}</span>
                        </span>
                      }
                    />
                  </Tarjeta>
                );
              })}
            </div>

            <section className="mt-5 grid gap-5 lg:grid-cols-[1.35fr_1fr]">
              <Tarjeta indice={5}>
                <CabeceraTarjeta
                  titulo="Distribucion por estado"
                  descripcion="Backlog actual del tenant, sin recorte de fechas."
                />
                <div className="mt-6">
                  <Columnas
                    barras={estados
                      .sort((uno, otro) => Number(otro[1]) - Number(uno[1]))
                      .map(([estado, cantidad]) => ({
                        etiqueta: estado,
                        valor: Number(cantidad),
                        tono: TONO_BARRA_ESTADO[estado] ?? "violeta",
                      }))}
                    alto={168}
                  />
                </div>
              </Tarjeta>

              <Tarjeta indice={6}>
                <CabeceraTarjeta
                  titulo="Excepciones abiertas"
                  descripcion="Lo que esta esperando una decision humana."
                  acciones={
                    tienePermiso("excepciones.leer") ? (
                      <Link
                        to="/excepciones"
                        className="inline-flex items-center gap-1 text-xs font-semibold text-violeta transition hover:gap-1.5"
                      >
                        Ver todas
                        <IconoDerecha tamano={13} />
                      </Link>
                    ) : undefined
                  }
                />
                {!tienePermiso("excepciones.leer") ? (
                  <p className="mt-5 text-sm text-tinta-suave">No tenes permiso para ver excepciones.</p>
                ) : excepciones.isPending ? (
                  <div className="mt-5">
                    <Cargando filas={3} alto="h-14" />
                  </div>
                ) : excepciones.isError ? (
                  <p className="mt-5 text-sm text-rojo">{mensajeDeError(excepciones.error)}</p>
                ) : !excepciones.data?.content.length ? (
                  <p className="mt-5 rounded-xl bg-exito-tenue px-4 py-3 text-sm text-exito ring-1 ring-inset ring-exito-borde">
                    No hay excepciones abiertas.
                  </p>
                ) : (
                  <ul className="mt-5 space-y-2.5">
                    {excepciones.data.content.map((excepcion) => (
                      <li
                        key={excepcion.id}
                        className="rounded-2xl border border-borde px-3.5 py-3 transition duration-300 hover:-translate-y-0.5 hover:border-borde-fuerte hover:bg-lienzo/60 hover:shadow-tarjeta"
                      >
                        <div className="flex items-center justify-between gap-2">
                          <InsigniaSeveridad severidad={excepcion.severidad} />
                          <span className="text-[11px] font-medium uppercase tracking-wide text-tinta-tenue">
                            {excepcion.tipo}
                          </span>
                        </div>
                        <p className="mt-1.5 line-clamp-2 text-sm text-tinta-media">{excepcion.detalle}</p>
                      </li>
                    ))}
                  </ul>
                )}
              </Tarjeta>
            </section>
          </>
        )}
      </Contenido>
    </>
  );
}

function Contado({ valor }: { valor: number }) {
  const { referencia, visible } = useVisible<HTMLSpanElement>();
  const animado = useContador(visible ? valor : 0, 900);
  return <span ref={referencia}>{Math.round(animado).toLocaleString("es-AR")}</span>;
}

function TarjetaTotal({ total, enCola }: { total: number; enCola: number }) {
  const { referencia, visible } = useVisible<HTMLElement>();
  const animado = useContador(visible ? total : 0, 1100);

  return (
    <article
      ref={referencia}
      className="subir superficie-oscura elevar relieve-oscuro relative overflow-hidden rounded-3xl px-5 py-4"
    >
      <span
        aria-hidden
        className="pointer-events-none absolute -right-10 -top-14 h-40 w-40 rounded-full bg-violeta/35 blur-3xl"
      />
      <p className="relative text-[11px] font-semibold uppercase tracking-wider text-white/50">
        Documentos totales
      </p>
      <p className="cifra relative mt-2 text-[34px] leading-none text-white">
        {Math.round(animado).toLocaleString("es-AR")}
      </p>
      <p className="relative mt-2.5 flex items-center gap-1.5 text-xs text-white/55">
        <IconoReloj tamano={13} />
        {enCola.toLocaleString("es-AR")} en cola de extraccion
      </p>
    </article>
  );
}

function porcentaje(parte: number, total: number) {
  if (!total) {
    return "0% del total";
  }
  return `${Math.round((parte / total) * 100)}% del total`;
}
