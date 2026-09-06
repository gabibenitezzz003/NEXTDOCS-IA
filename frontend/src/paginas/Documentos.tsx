import { useMemo, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Contenido, Encabezado } from "../componentes/Disposicion";
import { Cargando, ErrorPanel, Vacio } from "../componentes/Estados";
import { InsigniaEstado } from "../componentes/Insignias";
import { Boton, Pastilla, Tarjeta } from "../componentes/Interfaz";
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
import type { EstadoDocumento } from "../tipos/api";

const ESTADOS: EstadoDocumento[] = [
  "RECIBIDO",
  "PROCESANDO",
  "EXTRAIDO",
  "VALIDADO",
  "OBSERVADO",
  "APROBADO",
  "RECHAZADO",
  "DIVIDIDO",
  "CERRADO",
];

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
  const [avisoSubida, setAvisoSubida] = useState<{ tono: "ok" | "error"; texto: string } | null>(null);

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
        texto: `${documento.nombre ?? "Documento"} ingresado en estado ${documento.estado}`,
      });
      clienteConsultas.invalidateQueries({ queryKey: ["documentos"] });
      clienteConsultas.invalidateQueries({ queryKey: ["resumen"] });
      clienteConsultas.invalidateQueries({ queryKey: ["kpi"] });
    },
    onError: (error) => setAvisoSubida({ tono: "error", texto: mensajeDeError(error) }),
  });

  function alternarEstado(estado: EstadoDocumento) {
    setPagina(0);
    setSeleccionados((actuales) =>
      actuales.includes(estado) ? actuales.filter((valor) => valor !== estado) : [...actuales, estado],
    );
  }

  const documentos = consulta.data?.content ?? [];
  const total = consulta.data?.totalElements ?? 0;
  const totalPaginas = consulta.data?.totalPages ?? 0;
  const hayFiltro = Boolean(seleccionados.length || busqueda);

  return (
    <>
      <Encabezado
        titulo="Bandeja documental"
        descripcion="Todo lo que entro al sistema, con su estado y su trazabilidad."
        acciones={
          tienePermiso("documentos.escribir") ? (
            <>
              <input
                ref={entradaArchivo}
                type="file"
                accept=".pdf,.png,.jpg,.jpeg,.tif,.tiff,.webp"
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
                variante="primario"
                onClick={() => entradaArchivo.current?.click()}
                disabled={subida.isPending}
              >
                <IconoSubir tamano={16} />
                {subida.isPending ? "Subiendo..." : "Subir documento"}
              </Boton>
            </>
          ) : null
        }
      />

      <Contenido>
        {avisoSubida ? (
          <div
            role="status"
            className={`aparecer mb-4 flex items-start gap-2.5 rounded-xl border px-4 py-3 text-sm ${
              avisoSubida.tono === "ok"
                ? "border-exito-borde bg-exito-tenue text-exito"
                : "border-rojo-borde bg-rojo-tenue text-rojo"
            }`}
          >
            <span className="mt-px shrink-0">
              {avisoSubida.tono === "ok" ? <IconoCheck tamano={16} /> : <IconoInfo tamano={16} />}
            </span>
            {avisoSubida.texto}
          </div>
        ) : null}

        <Tarjeta padding="p-4" className="mb-5">
          <form
            onSubmit={(evento) => {
              evento.preventDefault();
              setPagina(0);
              setBusqueda(texto.trim());
            }}
            className="flex gap-2"
          >
            <div className="relative flex-1">
              <span className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-tinta-tenue">
                <IconoBuscar tamano={16} />
              </span>
              <input
                value={texto}
                onChange={(evento) => setTexto(evento.target.value)}
                placeholder="Buscar por nombre, remitente o referencia externa"
                className="h-10 w-full rounded-xl border border-borde bg-white pl-10 pr-3 text-sm text-tinta shadow-plano outline-none transition placeholder:text-tinta-tenue focus:border-violeta focus:ring-[3px] focus:ring-violeta/15"
              />
            </div>
            <Boton type="submit">Buscar</Boton>
          </form>

          <div className="mt-3.5 flex flex-wrap items-center gap-1.5 border-t border-borde pt-3.5">
            <span className="mr-1 text-[11px] font-semibold uppercase tracking-wider text-tinta-tenue">
              Estado
            </span>
            {ESTADOS.map((estado) => {
              const activo = seleccionados.includes(estado);
              return (
                <button
                  key={estado}
                  type="button"
                  onClick={() => alternarEstado(estado)}
                  aria-pressed={activo}
                  className={`rounded-full px-3 py-1 text-[11px] font-semibold uppercase tracking-wide transition ${
                    activo
                      ? "bg-grafito text-white shadow-plano"
                      : "border border-borde bg-white text-tinta-suave hover:border-borde-fuerte hover:text-tinta"
                  }`}
                >
                  {estado}
                </button>
              );
            })}
            {hayFiltro ? (
              <button
                type="button"
                onClick={() => {
                  setSeleccionados([]);
                  setBusqueda("");
                  setTexto("");
                  setPagina(0);
                }}
                className="ml-1 rounded-full px-2.5 py-1 text-[11px] font-semibold text-violeta transition hover:bg-violeta-tenue"
              >
                Limpiar filtros
              </button>
            ) : null}
          </div>
        </Tarjeta>

        {consulta.isPending ? (
          <Cargando filas={6} />
        ) : consulta.isError ? (
          <ErrorPanel mensaje={mensajeDeError(consulta.error)} reintentar={() => consulta.refetch()} />
        ) : documentos.length === 0 ? (
          <Vacio
            titulo="No hay documentos que coincidan"
            detalle={
              hayFiltro
                ? "Proba quitando filtros o cambiando el termino de busqueda."
                : "Cuando ingrese el primer documento va a aparecer aca."
            }
          />
        ) : (
          <>
            <div className="overflow-hidden rounded-3xl border border-borde bg-white relieve">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-borde bg-lienzo/70">
                    {["Documento", "Estado", "Tipo detectado", "Sujeto", "Recibido"].map((columna) => (
                      <th
                        key={columna}
                        className="px-5 py-3 text-[11px] font-semibold uppercase tracking-wider text-tinta-suave"
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
                      onClick={() => setDocumentoAbierto(documento.id)}
                      className="group cursor-pointer transition hover:bg-lienzo/60"
                    >
                      <td className="px-5 py-3.5">
                        <p className="font-semibold text-tinta transition group-hover:text-violeta">
                          {documento.nombre ?? "Sin nombre"}
                        </p>
                        <p className="mt-0.5 text-xs text-tinta-suave">
                          {documento.origen}
                          {documento.cantidadSegmentos
                            ? ` · ${documento.cantidadSegmentos} segmentos`
                            : ""}
                        </p>
                      </td>
                      <td className="px-5 py-3.5">
                        <InsigniaEstado estado={documento.estado} />
                      </td>
                      <td className="px-5 py-3.5">
                        {documento.origenTipo === "GENERICO" ? (
                          <Pastilla tono="alerta">Captura generica</Pastilla>
                        ) : documento.codigoPlantilla ? (
                          <Pastilla tono="violeta">{documento.codigoPlantilla}</Pastilla>
                        ) : (
                          <span className="text-tinta-tenue">sin detectar</span>
                        )}
                      </td>
                      <td className="px-5 py-3.5">
                        {documento.sujetoIdObjeto ? (
                          <Pastilla tono="informacion">
                            {documento.sujetoTipoObjeto} {documento.sujetoIdObjeto}
                          </Pastilla>
                        ) : (
                          <span className="text-tinta-tenue">—</span>
                        )}
                      </td>
                      <td className="px-5 py-3.5 tabular-nums text-tinta-suave">
                        {formatearFecha(documento.recibido)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="mt-4 flex items-center justify-between text-sm">
              <span className="text-tinta-suave">
                <span className="font-semibold tabular-nums text-tinta">{total}</span> documento
                {total === 1 ? "" : "s"}
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
