import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Encabezado } from "../componentes/Disposicion";
import { Cargando, ErrorPanel, Vacio } from "../componentes/Estados";
import { InsigniaSeveridad } from "../componentes/Insignias";
import { listarExcepciones, resolverExcepcion } from "../api/excepciones";
import { mensajeDeError } from "../api/cliente";
import { formatearFecha } from "./Documentos";
import { VisorDocumento } from "./VisorDocumento";
import { useSesion } from "../contextos/ProveedorSesion";
import type { EstadoExcepcion } from "../tipos/api";

const ESTADOS: EstadoExcepcion[] = ["ABIERTA", "EN_CURSO", "RESUELTA", "DESCARTADA"];

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
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const excepciones = consulta.data?.content ?? [];
  const totalPaginas = consulta.data?.totalPages ?? 0;

  return (
    <>
      <Encabezado
        titulo="Centro de excepciones"
        descripcion="Todo lo que necesita intervencion humana, con su prioridad y su vencimiento."
      />
      <div className="px-8 py-6">
        <div className="mb-5 flex flex-wrap gap-1.5">
          {ESTADOS.map((candidato) => (
            <button
              key={candidato}
              type="button"
              onClick={() => {
                setEstado(candidato);
                setPagina(0);
              }}
              className={`rounded-full px-3 py-1 text-xs font-medium transition ${
                estado === candidato
                  ? "bg-grafito text-white"
                  : "border border-borde bg-white text-tinta-suave hover:border-grafito hover:text-tinta"
              }`}
            >
              {candidato.replace(/_/g, " ")}
            </button>
          ))}
        </div>

        {error ? (
          <div className="mb-4 rounded-lg border border-rojo/25 bg-rojo-tenue px-3.5 py-2.5 text-sm text-rojo">
            {error}
          </div>
        ) : null}

        {consulta.isPending ? (
          <Cargando filas={5} />
        ) : consulta.isError ? (
          <ErrorPanel mensaje={mensajeDeError(consulta.error)} reintentar={() => consulta.refetch()} />
        ) : excepciones.length === 0 ? (
          <Vacio
            titulo={`No hay excepciones en estado ${estado.replace(/_/g, " ").toLowerCase()}`}
            detalle="Cuando el pipeline necesite intervencion humana va a aparecer aca."
          />
        ) : (
          <>
            <ul className="space-y-2.5">
              {excepciones.map((excepcion) => (
                <li key={excepcion.id} className="rounded-xl border border-borde bg-white px-5 py-4">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <InsigniaSeveridad severidad={excepcion.severidad} />
                        <span className="rounded-full bg-lienzo px-2.5 py-0.5 text-xs text-tinta-suave">
                          {excepcion.tipo}
                        </span>
                        <span className="text-xs text-tinta-suave">{excepcion.prioridad}</span>
                        {excepcion.vencida ? (
                          <span className="rounded-full bg-rojo-tenue px-2.5 py-0.5 text-xs font-medium text-rojo">
                            SLA vencido
                          </span>
                        ) : null}
                      </div>
                      <p className="mt-2 text-sm text-tinta">{excepcion.detalle}</p>
                      <p className="mt-1 text-xs text-tinta-suave">
                        {excepcion.nombreDocumento ? `${excepcion.nombreDocumento} · ` : ""}
                        vence {formatearFecha(excepcion.venceEn)}
                      </p>
                    </div>
                    <div className="flex shrink-0 gap-2">
                      {excepcion.documentoId ? (
                        <button
                          type="button"
                          onClick={() => setDocumentoAbierto(excepcion.documentoId!)}
                          className="rounded-lg border border-borde px-3 py-1.5 text-xs font-medium transition hover:border-violeta hover:text-violeta"
                        >
                          Ver documento
                        </button>
                      ) : null}
                      {tienePermiso("excepciones.gestionar") && excepcion.estado !== "RESUELTA" ? (
                        <button
                          type="button"
                          onClick={() => {
                            setResolviendo(resolviendo === excepcion.id ? null : excepcion.id);
                            setResolucion("");
                          }}
                          className="rounded-lg border border-borde px-3 py-1.5 text-xs font-medium transition hover:border-exito hover:text-exito"
                        >
                          Resolver
                        </button>
                      ) : null}
                    </div>
                  </div>

                  {resolviendo === excepcion.id ? (
                    <form
                      onSubmit={(evento) => {
                        evento.preventDefault();
                        resolver.mutate({ id: excepcion.id, texto: resolucion });
                      }}
                      className="mt-3 flex gap-2 border-t border-borde pt-3"
                    >
                      <input
                        value={resolucion}
                        onChange={(evento) => setResolucion(evento.target.value)}
                        required
                        placeholder="Como se resolvio"
                        className="flex-1 rounded-lg border border-borde px-3 py-1.5 text-sm outline-none focus:border-violeta"
                      />
                      <button
                        type="submit"
                        disabled={resolver.isPending}
                        className="rounded-lg bg-exito px-4 py-1.5 text-sm font-semibold text-white transition hover:opacity-90 disabled:opacity-40"
                      >
                        Confirmar
                      </button>
                    </form>
                  ) : null}
                </li>
              ))}
            </ul>

            {totalPaginas > 1 ? (
              <div className="mt-4 flex items-center justify-end gap-2 text-sm text-tinta-suave">
                <button
                  type="button"
                  disabled={pagina === 0}
                  onClick={() => setPagina((actual) => actual - 1)}
                  className="rounded-lg border border-borde bg-white px-3 py-1.5 disabled:opacity-40"
                >
                  Anterior
                </button>
                <span>
                  {pagina + 1} de {totalPaginas}
                </span>
                <button
                  type="button"
                  disabled={pagina + 1 >= totalPaginas}
                  onClick={() => setPagina((actual) => actual + 1)}
                  className="rounded-lg border border-borde bg-white px-3 py-1.5 disabled:opacity-40"
                >
                  Siguiente
                </button>
              </div>
            ) : null}
          </>
        )}
      </div>

      {documentoAbierto ? (
        <VisorDocumento documentoId={documentoAbierto} alCerrar={() => setDocumentoAbierto(null)} />
      ) : null}
    </>
  );
}
