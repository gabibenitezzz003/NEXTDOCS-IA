import { useMemo, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Contenido, Encabezado } from "../componentes/Disposicion";
import { Cargando, ErrorPanel, Vacio } from "../componentes/Estados";
import { InsigniaEstado } from "../componentes/Insignias";
import { Boton, Campo, Pastilla, Tarjeta } from "../componentes/Interfaz";
import {
  IconoBuscar,
  IconoCheck,
  IconoDerecha,
  IconoInfo,
  IconoIzquierda,
  IconoSubir,
} from "../componentes/Iconos";
import { VisorDocumento } from "./VisorDocumento";
import { ingresarDocumento, listarDocumentos } from "../api/documentos";
import { mensajeDeError } from "../api/cliente";
import { useSesion } from "../contextos/ProveedorSesion";
import type { Documento, EstadoDocumento } from "../tipos/api";
import { ESTADOS_DOCUMENTALES } from "../utilidades/estadosDocumento";

const ESTADOS = Object.keys(ESTADOS_DOCUMENTALES) as EstadoDocumento[];

const TAMANO = 25;

export function Documentos() {
  const { tienePermiso } = useSesion();
  const clienteConsultas = useQueryClient();
  const entradaArchivo = useRef<HTMLInputElement>(null);

  const [seleccionados, setSeleccionados] = useState<EstadoDocumento[]>([]);
  const [texto, setTexto] = useState("");
  const [busqueda, setBusqueda] = useState("");
  const [pagina, setPagina] = useState(0);
  const [documentoAbierto, setDocumentoAbierto] = useState<string | null>(null);
  const [avisoSubida, setAvisoSubida] = useState<{
    tono: "ok" | "error";
    texto: string;
  } | null>(null);

  const filtro = useMemo(
    () => ({
      estados: seleccionados.length ? seleccionados : undefined,
      texto: busqueda || undefined,
      soloRaiz: true,
      pagina,
      tamano: TAMANO,
      orden: "alta,desc",
    }),
    [seleccionados, busqueda, pagina],
  );

  const consulta = useQuery({
    queryKey: ["documentos", filtro],
    queryFn: () => listarDocumentos(filtro),
  });

  const subida = useMutation({
    mutationFn: (archivo: File) => ingresarDocumento(archivo),
    onSuccess: (documento) => {
      setAvisoSubida({
        tono: "ok",
        texto: `${documento.nombre ?? "Documento"} ingresado en estado ${ESTADOS_DOCUMENTALES[documento.estado].etiqueta}`,
      });
      clienteConsultas.invalidateQueries({ queryKey: ["documentos"] });
      clienteConsultas.invalidateQueries({ queryKey: ["resumen"] });
      clienteConsultas.invalidateQueries({ queryKey: ["kpi"] });
    },
    onError: (error) =>
      setAvisoSubida({ tono: "error", texto: mensajeDeError(error) }),
  });

  function alternarEstado(estado: EstadoDocumento) {
    setPagina(0);
    setSeleccionados((actuales) =>
      actuales.includes(estado)
        ? actuales.filter((valor) => valor !== estado)
        : [...actuales, estado],
    );
  }

  const documentos = consulta.data?.content ?? [];
  const total = consulta.data?.totalElements ?? 0;
  const totalPaginas = consulta.data?.totalPages ?? 0;
  const hayFiltro = Boolean(seleccionados.length || busqueda);

  function limpiarFiltros() {
    setSeleccionados([]);
    setBusqueda("");
    setTexto("");
    setPagina(0);
  }

  return (
    <>
      <Encabezado
        titulo="Bandeja documental"
        descripcion="Consultá los documentos, su estado y su trazabilidad."
        acciones={
          tienePermiso("documentos.escribir") ? (
            <>
              <input
                ref={entradaArchivo}
                type="file"
                accept=".pdf,.png,.jpg,.jpeg,.tif,.tiff,.webp"
                aria-label="Seleccionar un documento para cargar"
                disabled={subida.isPending}
                className="hidden"
                onChange={(evento) => {
                  const archivo = evento.target.files?.[0];
                  if (archivo) {
                    setAvisoSubida(null);
                    subida.mutate(archivo);
                  }
                  evento.target.value = "";
                }}
              />
              <Boton
                type="button"
                variante="primario"
                onClick={() => entradaArchivo.current?.click()}
                cargando={subida.isPending}
              >
                <span aria-hidden="true">
                  <IconoSubir tamano={16} />
                </span>
                Cargar documento
              </Boton>
            </>
          ) : null
        }
      />

      <Contenido>
        {avisoSubida ? (
          <div
            role={avisoSubida.tono === "ok" ? "status" : "alert"}
            aria-atomic="true"
            className={`mb-espacio-4 flex items-start gap-espacio-3 rounded-control border px-espacio-4 py-espacio-3 text-pequeno [overflow-wrap:anywhere] ${
              avisoSubida.tono === "ok"
                ? "border-exito-borde bg-exito-tenue text-exito-texto"
                : "border-rojo-borde bg-rojo-tenue text-rojo-alto"
            }`}
          >
            <span aria-hidden="true" className="mt-px shrink-0">
              {avisoSubida.tono === "ok" ? (
                <IconoCheck tamano={16} />
              ) : (
                <IconoInfo tamano={16} />
              )}
            </span>
            {avisoSubida.texto}
          </div>
        ) : null}
        <p role="status" aria-atomic="true" className="sr-only">
          {subida.isPending ? "Subiendo documento" : ""}
        </p>

        <Tarjeta padding="p-espacio-4" className="mb-espacio-5">
          <form
            role="search"
            aria-label="Buscar documentos"
            onSubmit={(evento) => {
              evento.preventDefault();
              setPagina(0);
              setBusqueda(texto.trim());
            }}
            className="grid min-w-0 gap-espacio-3 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-end"
          >
            <Campo
              etiqueta="Buscar documento"
              value={texto}
              onChange={(evento) => setTexto(evento.target.value)}
              placeholder="Buscar por nombre, remitente o referencia externa"
            />
            <Boton type="submit">
              <span aria-hidden="true">
                <IconoBuscar tamano={16} />
              </span>
              Buscar
            </Boton>
          </form>

          <fieldset className="mt-espacio-4 min-w-0 border-t border-borde pt-espacio-3">
            <legend className="text-pequeno font-semibold text-tinta-media">
              Filtrar por estado
            </legend>
            <div className="flex flex-wrap gap-espacio-2">
              {ESTADOS.map((estado) => {
                const activo = seleccionados.includes(estado);
                return (
                  <Boton
                    key={estado}
                    type="button"
                    onClick={() => alternarEstado(estado)}
                    aria-pressed={activo}
                    variante={activo ? "primario" : "secundario"}
                    tamano="sm"
                    className="rounded-insignia! text-micro!"
                  >
                    {ESTADOS_DOCUMENTALES[estado].etiqueta}
                  </Boton>
                );
              })}
              {hayFiltro ? (
                <Boton
                  type="button"
                  variante="fantasma"
                  tamano="sm"
                  onClick={limpiarFiltros}
                >
                  Limpiar filtros
                </Boton>
              ) : null}
            </div>
          </fieldset>
        </Tarjeta>

        {consulta.isPending ? (
          <Tarjeta>
            <Cargando filas={6} />
          </Tarjeta>
        ) : consulta.isError ? (
          <ErrorPanel
            titulo="No se pudieron cargar los documentos"
            mensaje={mensajeDeError(consulta.error)}
            reintentar={() => consulta.refetch()}
          />
        ) : documentos.length === 0 ? (
          <Vacio
            titulo={
              hayFiltro
                ? "No hay documentos que coincidan"
                : "Todavía no hay documentos"
            }
            detalle={
              hayFiltro
                ? "Probá quitando filtros o cambiando el término de búsqueda."
                : "Cuando ingrese el primer documento va a aparecer acá."
            }
            accion={
              hayFiltro ? (
                <Boton type="button" onClick={limpiarFiltros}>
                  Limpiar filtros
                </Boton>
              ) : undefined
            }
          />
        ) : (
          <>
            <Tarjeta padding="p-0" className="hidden lg:block">
              <table className="w-full table-fixed text-left text-pequeno">
                <caption className="sr-only">
                  Documentos de la página {pagina + 1}. Abrí un documento para
                  ver su detalle.
                </caption>
                <colgroup>
                  <col className="w-[27%]" />
                  <col className="w-[17%]" />
                  <col className="w-[20%]" />
                  <col className="w-[18%]" />
                  <col className="w-[18%]" />
                </colgroup>
                <thead>
                  <tr className="border-b border-borde bg-grafito-alto text-blanco">
                    {[
                      "Documento",
                      "Estado",
                      "Tipo detectado",
                      "Sujeto",
                      "Recibido",
                    ].map((columna) => (
                      <th
                        key={columna}
                        scope="col"
                        className="px-espacio-4 py-espacio-3 text-micro font-semibold uppercase tracking-wide first:rounded-tl-tarjeta last:rounded-tr-tarjeta"
                      >
                        {columna}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-borde">
                  {documentos.map((documento) => (
                    <tr
                      key={documento.id}
                      onClick={(evento) => {
                        evento.currentTarget.querySelector("button")?.focus();
                        setDocumentoAbierto(documento.id);
                      }}
                      className="group cursor-pointer transition-colors hover:bg-violeta-tenue focus-within:bg-violeta-tenue"
                    >
                      <td className="px-espacio-4 py-espacio-3">
                        <NombreDocumento
                          documento={documento}
                          alAbrir={() => setDocumentoAbierto(documento.id)}
                        />
                      </td>
                      <td className="px-espacio-4 py-espacio-3">
                        <InsigniaEstado estado={documento.estado} />
                      </td>
                      <td className="px-espacio-4 py-espacio-3">
                        <TipoDetectado documento={documento} />
                      </td>
                      <td className="px-espacio-4 py-espacio-3">
                        <SujetoDocumento documento={documento} />
                      </td>
                      <td className="px-espacio-4 py-espacio-3 tabular-nums text-tinta-suave">
                        {formatearFecha(documento.recibido)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </Tarjeta>

            <ul
              aria-label="Documentos de la página"
              className="grid min-w-0 gap-espacio-4 sm:grid-cols-2 lg:hidden"
            >
              {documentos.map((documento) => (
                <li key={documento.id} className="min-w-0">
                  <Tarjeta className="h-full">
                    <NombreDocumento
                      documento={documento}
                      alAbrir={() => setDocumentoAbierto(documento.id)}
                    />
                    <dl className="mt-espacio-4 grid min-w-0 gap-espacio-4 text-pequeno">
                      <div>
                        <dt className="mb-espacio-1 text-tinta-suave">
                          Estado
                        </dt>
                        <dd>
                          <InsigniaEstado estado={documento.estado} />
                        </dd>
                      </div>
                      <div>
                        <dt className="mb-espacio-1 text-tinta-suave">
                          Tipo detectado
                        </dt>
                        <dd>
                          <TipoDetectado documento={documento} />
                        </dd>
                      </div>
                      <div>
                        <dt className="mb-espacio-1 text-tinta-suave">
                          Sujeto
                        </dt>
                        <dd>
                          <SujetoDocumento documento={documento} />
                        </dd>
                      </div>
                      <div>
                        <dt className="mb-espacio-1 text-tinta-suave">
                          Recibido
                        </dt>
                        <dd className="tabular-nums">
                          {formatearFecha(documento.recibido)}
                        </dd>
                      </div>
                    </dl>
                  </Tarjeta>
                </li>
              ))}
            </ul>

            <div className="mt-espacio-4 flex flex-wrap items-center justify-between gap-espacio-3 text-pequeno">
              <span
                role="status"
                aria-atomic="true"
                className="text-tinta-suave"
              >
                <span className="font-semibold tabular-nums text-tinta">
                  {total}
                </span>{" "}
                documento
                {total === 1 ? "" : "s"}
              </span>
              {totalPaginas > 1 ? (
                <nav
                  aria-label="Paginación de documentos"
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
                  <span
                    aria-live="polite"
                    aria-atomic="true"
                    className="px-espacio-1 text-pequeno tabular-nums text-tinta-suave"
                  >
                    <span className="sr-only">Página </span>
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

function NombreDocumento({
  documento,
  alAbrir,
}: {
  documento: Documento;
  alAbrir: () => void;
}) {
  return (
    <div className="min-w-0 [overflow-wrap:anywhere]">
      <Boton
        type="button"
        variante="fantasma"
        aria-label={`Abrir documento ${documento.nombre ?? "Sin nombre"}`}
        onClick={(evento) => {
          evento.stopPropagation();
          alAbrir();
        }}
        className="h-auto! min-h-control-pequeno max-w-full justify-start px-0! text-left whitespace-normal! text-tinta hover:underline group-hover:text-violeta"
      >
        <span className="min-w-0">{documento.nombre ?? "Sin nombre"}</span>
      </Boton>
      <p className="mt-espacio-1 text-pequeno text-tinta-suave">
        {documento.origen}
        {documento.cantidadSegmentos
          ? ` · ${documento.cantidadSegmentos} segmentos`
          : ""}
      </p>
    </div>
  );
}

function TipoDetectado({ documento }: { documento: Documento }) {
  if (documento.origenTipo === "GENERICO") {
    return (
      <Pastilla
        tono="alerta"
        className="max-w-full whitespace-normal! [overflow-wrap:anywhere]"
      >
        Captura genérica
      </Pastilla>
    );
  }
  return documento.codigoPlantilla ? (
    <Pastilla
      tono="violeta"
      className="max-w-full whitespace-normal! [overflow-wrap:anywhere]"
    >
      {documento.codigoPlantilla}
    </Pastilla>
  ) : (
    <span className="text-tinta-suave">sin detectar</span>
  );
}

function SujetoDocumento({ documento }: { documento: Documento }) {
  return documento.sujetoIdObjeto ? (
    <Pastilla
      tono="informacion"
      className="max-w-full whitespace-normal! [overflow-wrap:anywhere]"
    >
      {documento.sujetoTipoObjeto} {documento.sujetoIdObjeto}
    </Pastilla>
  ) : (
    <span className="text-tinta-suave">—</span>
  );
}

export function formatearFecha(valor?: string) {
  if (!valor) {
    return "—";
  }
  return new Date(valor).toLocaleString("es-AR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}
