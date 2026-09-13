import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Contenido, Encabezado } from "../componentes/Disposicion";
import { Cargando, ErrorPanel, Vacio } from "../componentes/Estados";
import { InsigniaSeveridad } from "../componentes/Insignias";
import {
  Boton,
  Campo,
  GrupoSegmentado,
  Pastilla,
  Tarjeta,
} from "../componentes/Interfaz";
import {
  IconoCheck,
  IconoDerecha,
  IconoIzquierda,
  IconoReloj,
} from "../componentes/Iconos";
import { listarExcepciones, resolverExcepcion } from "../api/excepciones";
import { mensajeDeError } from "../api/cliente";
import { formatearFecha } from "./Documentos";
import { VisorDocumento } from "./VisorDocumento";
import { useSesion } from "../contextos/ProveedorSesion";
import type { EstadoExcepcion, PrioridadExcepcion } from "../tipos/api";
import type { Tono } from "../componentes/Interfaz";

const ESTADOS: EstadoExcepcion[] = [
  "ABIERTA",
  "EN_CURSO",
  "RESUELTA",
  "DESCARTADA",
];

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
    queryKey: ["excepciones", estado, pagina, 25],
    queryFn: () => listarExcepciones(estado, pagina, 25),
  });

  const resolver = useMutation({
    mutationFn: ({ id, texto }: { id: string; texto: string }) =>
      resolverExcepcion(id, texto),
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
        descripcion="Consultá y resolvé las excepciones del flujo documental, con su prioridad y vencimiento."
      />
      <Contenido>
        <div className="mb-espacio-5 flex min-w-0 flex-wrap items-center justify-between gap-espacio-3">
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
          {!consulta.isPending && !consulta.isError ? (
            <p
              role="status"
              aria-atomic="true"
              className="text-pequeno text-tinta-suave"
            >
              <span className="font-semibold tabular-nums text-tinta">
                {total}
              </span>{" "}
              {total === 1 ? "excepción" : "excepciones"} ·{" "}
              {estado.replace(/_/g, " ")}
            </p>
          ) : null}
        </div>

        {error ? (
          <div className="mb-espacio-4">
            <ErrorPanel
              titulo="No se pudo resolver la excepción"
              mensaje={error}
            />
          </div>
        ) : null}

        {consulta.isPending ? (
          <Cargando filas={5} alto="h-48" />
        ) : consulta.isError ? (
          <ErrorPanel
            titulo="No se pudieron cargar las excepciones"
            mensaje={mensajeDeError(consulta.error)}
            reintentar={() => consulta.refetch()}
          />
        ) : excepciones.length === 0 ? (
          <Vacio
            titulo={
              total > 0
                ? "Sin resultados en esta página"
                : estado === "ABIERTA"
                  ? "No hay excepciones abiertas"
                  : "Sin coincidencias en este estado"
            }
            detalle={
              total > 0
                ? "Todavía hay excepciones en este estado. Seleccioná nuevamente el estado para volver a la primera página."
                : estado === "ABIERTA"
                  ? "La consulta de excepciones abiertas no devolvió resultados. Podés consultar los demás estados con el filtro."
                  : `No hay resultados en estado ${estado.replace(/_/g, " ").toLowerCase()} para esta página. Podés consultar otro estado con el filtro.`
            }
          />
        ) : (
          <>
            <ul
              aria-label="Excepciones documentales"
              className="space-y-espacio-4"
            >
              {excepciones.map((excepcion) => (
                <li key={excepcion.id} className="min-w-0">
                  <Tarjeta
                    padding="p-0"
                    className={`overflow-hidden ${excepcion.vencida ? "border-rojo-borde!" : ""}`}
                  >
                    <div className="p-espacio-4 sm:p-espacio-6">
                      <div className="flex min-w-0 flex-wrap items-start justify-between gap-espacio-3">
                        <div className="flex flex-wrap items-center gap-espacio-2">
                          <Pastilla
                            tono={
                              excepcion.estado === "RESUELTA"
                                ? "exito"
                                : "neutro"
                            }
                          >
                            {excepcion.estado.replace(/_/g, " ")}
                          </Pastilla>
                          <InsigniaSeveridad severidad={excepcion.severidad} />
                          <Pastilla
                            tono={
                              TONO_PRIORIDAD[excepcion.prioridad] ?? "neutro"
                            }
                          >
                            Prioridad {excepcion.prioridad}
                          </Pastilla>
                          {excepcion.vencida ? (
                            <Pastilla tono="rojo" solido>
                              SLA vencido
                            </Pastilla>
                          ) : null}
                        </div>
                        <h2 className="min-w-0 font-titulo text-pequeno font-bold text-violeta [overflow-wrap:anywhere]">
                          {excepcion.tipo}
                        </h2>
                      </div>
                      <p className="mt-espacio-2 text-micro text-tinta-suave [overflow-wrap:anywhere]">
                        ID: {excepcion.id}
                        {excepcion.codigo
                          ? " · Código: " + excepcion.codigo
                          : ""}
                      </p>
                      <p className="mt-espacio-4 text-cuerpo font-medium [overflow-wrap:anywhere]">
                        {excepcion.detalle ?? "Sin detalle disponible"}
                      </p>
                      <dl className="mt-espacio-4 grid min-w-0 gap-espacio-3 text-pequeno sm:grid-cols-2">
                        <div className="min-w-0">
                          <dt className="text-micro text-tinta-suave">
                            Documento
                          </dt>
                          <dd className="mt-espacio-1 font-medium [overflow-wrap:anywhere]">
                            {excepcion.nombreDocumento ??
                              excepcion.documentoId ??
                              "Sin documento relacionado"}
                          </dd>
                        </div>
                        <div className="min-w-0 sm:text-right">
                          <dt className="text-micro text-tinta-suave">
                            Vencimiento
                          </dt>
                          <dd
                            className={
                              "mt-espacio-1 flex items-start gap-espacio-1 sm:justify-end " +
                              (excepcion.vencida
                                ? "text-rojo-alto"
                                : "text-tinta-media")
                            }
                          >
                            <span
                              aria-hidden="true"
                              className="mt-0.5 shrink-0"
                            >
                              <IconoReloj tamano={14} />
                            </span>
                            {formatearFecha(excepcion.venceEn)}
                          </dd>
                        </div>
                        {excepcion.alta ? (
                          <div>
                            <dt className="text-micro text-tinta-suave">
                              Creada
                            </dt>
                            <dd className="mt-espacio-1">
                              {formatearFecha(excepcion.alta)}
                            </dd>
                          </div>
                        ) : null}
                        {excepcion.responsable ? (
                          <div className="min-w-0 sm:text-right">
                            <dt className="text-micro text-tinta-suave">
                              Responsable
                            </dt>
                            <dd className="mt-espacio-1 [overflow-wrap:anywhere]">
                              {excepcion.responsable}
                            </dd>
                          </div>
                        ) : null}
                      </dl>
                      {excepcion.resolucion || excepcion.resueltaPor ? (
                        <div className="mt-espacio-4 rounded-control border border-borde bg-lienzo p-espacio-3">
                          <p className="text-micro font-semibold text-tinta-suave">
                            Resolución registrada
                          </p>
                          {excepcion.resolucion ? (
                            <p className="mt-espacio-1 whitespace-pre-wrap text-pequeno [overflow-wrap:anywhere]">
                              {excepcion.resolucion}
                            </p>
                          ) : null}
                          {excepcion.resueltaPor ? (
                            <p className="mt-espacio-2 text-pequeno text-tinta-suave [overflow-wrap:anywhere]">
                              Resuelta por {excepcion.resueltaPor}
                            </p>
                          ) : null}
                        </div>
                      ) : null}
                      {(excepcion.documentoId &&
                        tienePermiso("documentos.leer")) ||
                      (tienePermiso("excepciones.gestionar") &&
                        excepcion.estado !== "RESUELTA") ? (
                        <div className="mt-espacio-4 flex flex-wrap justify-end gap-espacio-2">
                          {excepcion.documentoId &&
                          tienePermiso("documentos.leer") ? (
                            <Boton
                              type="button"
                              onClick={() =>
                                setDocumentoAbierto(excepcion.documentoId!)
                              }
                              className="flex-1 sm:flex-none"
                            >
                              Ver documento
                            </Boton>
                          ) : null}
                          {tienePermiso("excepciones.gestionar") &&
                          excepcion.estado !== "RESUELTA" ? (
                            <Boton
                              type="button"
                              variante={
                                resolviendo === excepcion.id
                                  ? "primario"
                                  : "secundario"
                              }
                              aria-expanded={resolviendo === excepcion.id}
                              aria-controls={
                                resolviendo === excepcion.id
                                  ? "resolucion-" + excepcion.id
                                  : undefined
                              }
                              onClick={() => {
                                setResolviendo(
                                  resolviendo === excepcion.id
                                    ? null
                                    : excepcion.id,
                                );
                                setResolucion("");
                              }}
                              className="flex-1 sm:flex-none"
                            >
                              <span aria-hidden="true">
                                <IconoCheck tamano={14} />
                              </span>
                              Resolver
                            </Boton>
                          ) : null}
                        </div>
                      ) : null}
                    </div>
                    {resolviendo === excepcion.id ? (
                      <form
                        id={"resolucion-" + excepcion.id}
                        aria-label="Resolución de la excepción"
                        aria-busy={resolver.isPending}
                        onSubmit={(evento) => {
                          evento.preventDefault();
                          resolver.mutate({
                            id: excepcion.id,
                            texto: resolucion,
                          });
                        }}
                        className="flex min-w-0 flex-col gap-espacio-3 border-t border-violeta-borde bg-violeta-tenue p-espacio-4 sm:flex-row sm:items-end sm:px-espacio-6"
                      >
                        <div className="min-w-0 flex-1">
                          <Campo
                            etiqueta="Cómo se resolvió"
                            value={resolucion}
                            onChange={(evento) =>
                              setResolucion(evento.target.value)
                            }
                            required
                            autoFocus
                            placeholder="Describí la resolución"
                          />
                        </div>
                        <Boton
                          type="submit"
                          variante="primario"
                          disabled={resolver.isPending}
                          cargando={resolver.isPending}
                        >
                          Confirmar
                        </Boton>
                        <span
                          role="status"
                          aria-atomic="true"
                          className="sr-only"
                        >
                          {resolver.isPending ? "Guardando resolución" : ""}
                        </span>
                      </form>
                    ) : null}
                  </Tarjeta>
                </li>
              ))}
            </ul>
            <div className="mt-espacio-5 flex flex-wrap items-center justify-between gap-espacio-3 text-pequeno">
              <span className="text-tinta-suave">
                Hasta 25 excepciones por página
              </span>
              {totalPaginas > 1 ? (
                <nav
                  aria-label="Paginación de excepciones"
                  className="flex flex-wrap items-center gap-espacio-2"
                >
                  <Boton
                    type="button"
                    tamano="sm"
                    disabled={pagina === 0}
                    onClick={() => setPagina((actual) => actual - 1)}
                  >
                    <span aria-hidden="true">
                      <IconoIzquierda tamano={14} />
                    </span>
                    Anterior
                  </Boton>
                  <span className="text-pequeno tabular-nums text-tinta-suave">
                    {pagina + 1} de {totalPaginas}
                  </span>
                  <Boton
                    type="button"
                    tamano="sm"
                    disabled={pagina + 1 >= totalPaginas}
                    onClick={() => setPagina((actual) => actual + 1)}
                  >
                    Siguiente
                    <span aria-hidden="true">
                      <IconoDerecha tamano={14} />
                    </span>
                  </Boton>
                </nav>
              ) : null}
            </div>
          </>
        )}
      </Contenido>
      {documentoAbierto ? (
        <VisorDocumento
          documentoId={documentoAbierto}
          alCerrar={() => setDocumentoAbierto(null)}
        />
      ) : null}
    </>
  );
}
