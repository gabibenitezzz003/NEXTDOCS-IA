import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Contenido, Encabezado } from "../componentes/Disposicion";
import { Cargando, ErrorPanel, Vacio } from "../componentes/Estados";
import { InsigniaSeveridad } from "../componentes/Insignias";
import { Boton, GrupoSegmentado, Pastilla } from "../componentes/Interfaz";
import { IconoCheck, IconoDerecha, IconoIzquierda, IconoReloj } from "../componentes/Iconos";
import { listarExcepciones, resolverExcepcion } from "../api/excepciones";
import { mensajeDeError } from "../api/cliente";
import { formatearFecha } from "./Documentos";
import { VisorDocumento } from "./VisorDocumento";
import { useSesion } from "../contextos/ProveedorSesion";
import type { EstadoExcepcion, PrioridadExcepcion } from "../tipos/api";
import type { Tono } from "../componentes/Interfaz";

const ESTADOS: EstadoExcepcion[] = ["ABIERTA", "EN_CURSO", "RESUELTA", "DESCARTADA"];

const TONO_PRIORIDAD: Record<PrioridadExcepcion, Tono> = {
  BAJA: "neutro",
  MEDIA: "informacion",
  ALTA: "alerta",
  CRITICA: "rojo",
};

export function Excepciones() {
  const { tienePermiso } = useSesion();
  const clienteConsultas = useQueryClient();
  const [estado, setEstado] = useState<EstadoExcepcion>("ABIERTA");
  const [pagina, setPagina] = useState(0);
  const [documentoAbierto, setDocumentoAbierto] = useState<string | null>(null);
  const [resolviendo, setResolviendo] = useState<string | null>(null);
  const [resolucion, setResolucion] = useState("");
  const [error, setError] = useState<string | null>(null);

  const consulta = useQuery({
    queryKey: ["excepciones", estado, pagina],
    queryFn: () => listarExcepciones(estado, pagina, 25),
  });

  const resolver = useMutation({
    mutationFn: ({ id, texto }: { id: string; texto: string }) => resolverExcepcion(id, texto),
    onSuccess: () => {
      setResolviendo(null);
      setResolucion("");
      setError(null);
      clienteConsultas.invalidateQueries({ queryKey: ["excepciones"] });
      clienteConsultas.invalidateQueries({ queryKey: ["kpi"] });
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const excepciones = consulta.data?.content ?? [];
  const total = consulta.data?.totalElements ?? 0;
  const totalPaginas = consulta.data?.totalPages ?? 0;

  return (
    <>
      <Encabezado
        titulo="Centro de excepciones"
        descripcion="Todo lo que necesita intervencion humana, con su prioridad y su vencimiento."
        acciones={
          <GrupoSegmentado
            etiqueta="Estado de las excepciones"
            opciones={ESTADOS.map((candidato) => ({
              valor: candidato,
              texto: candidato.replace(/_/g, " "),
            }))}
            valor={estado}
            alCambiar={(nuevo) => {
              setEstado(nuevo);
              setPagina(0);
            }}
          />
        }
      />

      <Contenido>
        {error ? (
          <div className="aparecer mb-4 rounded-xl border border-rojo-borde bg-rojo-tenue px-4 py-3 text-sm text-rojo">
            {error}
          </div>
        ) : null}

        {consulta.isPending ? (
          <Cargando filas={5} alto="h-28" />
        ) : consulta.isError ? (
          <ErrorPanel mensaje={mensajeDeError(consulta.error)} reintentar={() => consulta.refetch()} />
        ) : excepciones.length === 0 ? (
          <Vacio
            titulo={`No hay excepciones en estado ${estado.replace(/_/g, " ").toLowerCase()}`}
            detalle="Cuando el pipeline necesite intervencion humana va a aparecer aca."
          />
        ) : (
          <>
            <ul className="space-y-3">
              {excepciones.map((excepcion) => (
                <li
                  key={excepcion.id}
                  className={`overflow-hidden rounded-3xl border bg-white relieve elevar ${
                    excepcion.vencida ? "border-rojo-borde" : "border-borde hover:border-borde-fuerte"
                  }`}
                >
                  <div className="flex flex-wrap items-start justify-between gap-4 px-5 py-4">
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-1.5">
                        <InsigniaSeveridad severidad={excepcion.severidad} />
                        <Pastilla tono={TONO_PRIORIDAD[excepcion.prioridad] ?? "neutro"}>
                          {excepcion.prioridad}
                        </Pastilla>
                        <span className="rounded-full bg-lienzo px-2.5 py-0.5 text-[11px] font-medium text-tinta-suave ring-1 ring-inset ring-borde">
                          {excepcion.tipo}
                        </span>
                        {excepcion.vencida ? (
                          <Pastilla tono="rojo" solido>
                            SLA vencido
                          </Pastilla>
                        ) : null}
                      </div>

                      <p className="mt-2.5 text-sm leading-relaxed text-tinta">{excepcion.detalle}</p>

                      <p className="mt-1.5 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-tinta-suave">
                        {excepcion.nombreDocumento ? (
                          <span className="font-medium text-tinta-media">{excepcion.nombreDocumento}</span>
                        ) : null}
                        <span className="flex items-center gap-1">
                          <IconoReloj tamano={12} />
                          vence {formatearFecha(excepcion.venceEn)}
                        </span>
                      </p>
                    </div>

                    <div className="flex shrink-0 gap-2">
                      {excepcion.documentoId ? (
                        <Boton tamano="sm" onClick={() => setDocumentoAbierto(excepcion.documentoId!)}>
                          Ver documento
                        </Boton>
                      ) : null}
                      {tienePermiso("excepciones.gestionar") && excepcion.estado !== "RESUELTA" ? (
                        <Boton
                          tamano="sm"
                          variante={resolviendo === excepcion.id ? "primario" : "secundario"}
                          onClick={() => {
                            setResolviendo(resolviendo === excepcion.id ? null : excepcion.id);
                            setResolucion("");
                          }}
                        >
                          <IconoCheck tamano={14} />
                          Resolver
                        </Boton>
                      ) : null}
                    </div>
                  </div>

                  {resolviendo === excepcion.id ? (
                    <form
                      onSubmit={(evento) => {
                        evento.preventDefault();
                        resolver.mutate({ id: excepcion.id, texto: resolucion });
                      }}
                      className="aparecer flex gap-2 border-t border-borde bg-lienzo/60 px-5 py-3.5"
                    >
                      <input
                        value={resolucion}
                        onChange={(evento) => setResolucion(evento.target.value)}
                        required
                        autoFocus
                        placeholder="Como se resolvio"
                        className="h-9.5 flex-1 rounded-xl border border-borde bg-white px-3 text-sm text-tinta outline-none transition placeholder:text-tinta-tenue focus:border-violeta focus:ring-[3px] focus:ring-violeta/15"
                      />
                      <Boton type="submit" variante="primario" disabled={resolver.isPending}>
                        {resolver.isPending ? "Guardando..." : "Confirmar"}
                      </Boton>
                    </form>
                  ) : null}
                </li>
              ))}
            </ul>

            <div className="mt-4 flex items-center justify-between text-sm">
              <span className="text-tinta-suave">
                <span className="font-semibold tabular-nums text-tinta">{total}</span> excepcion
                {total === 1 ? "" : "es"}
              </span>
              {totalPaginas > 1 ? (
                <div className="flex items-center gap-2">
                  <Boton
                    tamano="sm"
                    disabled={pagina === 0}
                    onClick={() => setPagina((actual) => actual - 1)}
                  >
                    <IconoIzquierda tamano={14} />
                    Anterior
                  </Boton>
                  <span className="px-1 text-xs tabular-nums text-tinta-suave">
                    {pagina + 1} de {totalPaginas}
                  </span>
                  <Boton
                    tamano="sm"
                    disabled={pagina + 1 >= totalPaginas}
                    onClick={() => setPagina((actual) => actual + 1)}
                  >
                    Siguiente
                    <IconoDerecha tamano={14} />
                  </Boton>
                </div>
              ) : null}
            </div>
          </>
        )}
      </Contenido>

      {documentoAbierto ? (
        <VisorDocumento documentoId={documentoAbierto} alCerrar={() => setDocumentoAbierto(null)} />
      ) : null}
    </>
  );
}
