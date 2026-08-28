import { useMemo, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Encabezado } from "../componentes/Disposicion";
import { Cargando, ErrorPanel, Vacio } from "../componentes/Estados";
import { InsigniaEstado } from "../componentes/Insignias";
import { VisorDocumento } from "./VisorDocumento";
import { ingresarDocumento, listarDocumentos } from "../api/documentos";
import { listarPlantillas } from "../api/plantillas";
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
  const [plantillaSubida, setPlantillaSubida] = useState("");
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

  const plantillas = useQuery({
    queryKey: ["plantillas"],
    queryFn: listarPlantillas,
    enabled: tienePermiso("plantillas.leer"),
  });

  const subida = useMutation({
    mutationFn: (archivo: File) => ingresarDocumento(archivo, plantillaSubida || undefined),
    onSuccess: (documento) => {
      setAvisoSubida({
        tono: "ok",
        texto: `${documento.nombre ?? "Documento"} ingresado en estado ${documento.estado}`,
      });
      clienteConsultas.invalidateQueries({ queryKey: ["documentos"] });
      clienteConsultas.invalidateQueries({ queryKey: ["resumen"] });
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

  return (
    <>
      <Encabezado
        titulo="Bandeja documental"
        descripcion="Todo lo que entro al sistema, con su estado y su trazabilidad."
        acciones={
          tienePermiso("documentos.escribir") ? (
            <>
              {plantillas.data?.length ? (
                <select
                  value={plantillaSubida}
                  onChange={(evento) => setPlantillaSubida(evento.target.value)}
                  className="rounded-lg border border-borde bg-white px-3 py-2 text-sm outline-none focus:border-violeta"
                >
                  <option value="">Sin plantilla</option>
                  {plantillas.data.map((plantilla) => (
                    <option key={plantilla.id} value={plantilla.codigo}>
                      {plantilla.nombre}
                    </option>
                  ))}
                </select>
              ) : null}
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
              <button
                type="button"
                onClick={() => entradaArchivo.current?.click()}
                disabled={subida.isPending}
                className="degradado-marca rounded-lg px-4 py-2 text-sm font-semibold text-white transition hover:opacity-90 disabled:opacity-50"
              >
                {subida.isPending ? "Subiendo..." : "Subir documento"}
              </button>
            </>
          ) : null
        }
      />

      <div className="px-8 py-6">
        {avisoSubida ? (
          <div
            role="status"
            className={`mb-4 rounded-lg px-3.5 py-2.5 text-sm ${
              avisoSubida.tono === "ok"
                ? "border border-exito/25 bg-exito-tenue text-exito"
                : "border border-rojo/25 bg-rojo-tenue text-rojo"
            }`}
          >
            {avisoSubida.texto}
          </div>
        ) : null}

        <div className="mb-4 flex flex-wrap items-center gap-3">
          <form
            onSubmit={(evento) => {
              evento.preventDefault();
              setPagina(0);
              setBusqueda(texto.trim());
            }}
            className="flex flex-1 gap-2"
          >
            <input
              value={texto}
              onChange={(evento) => setTexto(evento.target.value)}
              placeholder="Buscar por nombre, remitente o referencia externa"
              className="min-w-56 flex-1 rounded-lg border border-borde bg-white px-3.5 py-2 text-sm outline-none transition focus:border-violeta focus:ring-2 focus:ring-violeta/15"
            />
            <button
              type="submit"
              className="rounded-lg border border-borde bg-white px-4 py-2 text-sm font-medium text-tinta transition hover:border-violeta hover:text-violeta"
            >
              Buscar
            </button>
          </form>
        </div>

        <div className="mb-5 flex flex-wrap gap-1.5">
          {ESTADOS.map((estado) => {
            const activo = seleccionados.includes(estado);
            return (
              <button
                key={estado}
                type="button"
                onClick={() => alternarEstado(estado)}
                aria-pressed={activo}
                className={`rounded-full px-3 py-1 text-xs font-medium transition ${
                  activo
                    ? "bg-grafito text-white"
                    : "border border-borde bg-white text-tinta-suave hover:border-grafito hover:text-tinta"
                }`}
              >
                {estado}
              </button>
            );
          })}
          {seleccionados.length ? (
            <button
              type="button"
              onClick={() => {
                setSeleccionados([]);
                setPagina(0);
              }}
              className="rounded-full px-3 py-1 text-xs font-medium text-violeta hover:underline"
            >
              Limpiar
            </button>
          ) : null}
        </div>

        {consulta.isPending ? (
          <Cargando filas={6} />
        ) : consulta.isError ? (
          <ErrorPanel mensaje={mensajeDeError(consulta.error)} reintentar={() => consulta.refetch()} />
        ) : documentos.length === 0 ? (
          <Vacio
            titulo="No hay documentos que coincidan"
            detalle={
              seleccionados.length || busqueda
                ? "Proba quitando filtros o cambiando el termino de busqueda."
                : "Cuando ingrese el primer documento va a aparecer aca."
            }
          />
        ) : (
          <>
            <div className="overflow-hidden rounded-xl border border-borde bg-white">
              <table className="w-full text-left text-sm">
                <thead className="border-b border-borde bg-lienzo text-xs uppercase tracking-wide text-tinta-suave">
                  <tr>
                    <th className="px-4 py-3 font-medium">Documento</th>
                    <th className="px-4 py-3 font-medium">Estado</th>
                    <th className="px-4 py-3 font-medium">Plantilla</th>
                    <th className="px-4 py-3 font-medium">Sujeto</th>
                    <th className="px-4 py-3 font-medium">Recibido</th>
                  </tr>
                </thead>
                <tbody>
                  {documentos.map((documento) => (
                    <tr
                      key={documento.id}
                      onClick={() => setDocumentoAbierto(documento.id)}
                      className="cursor-pointer border-b border-borde/70 transition last:border-0 hover:bg-violeta-tenue/40"
                    >
                      <td className="px-4 py-3">
                        <p className="font-medium text-tinta">{documento.nombre ?? "Sin nombre"}</p>
                        <p className="text-xs text-tinta-suave">
                          {documento.origen}
                          {documento.cantidadSegmentos
                            ? ` · ${documento.cantidadSegmentos} segmentos`
                            : ""}
                        </p>
                      </td>
                      <td className="px-4 py-3">
                        <InsigniaEstado estado={documento.estado} />
                      </td>
                      <td className="px-4 py-3 text-tinta-suave">
                        {documento.codigoPlantilla
                          ? `${documento.codigoPlantilla} v${documento.numeroVersionPlantilla}`
                          : "—"}
                      </td>
                      <td className="px-4 py-3 text-tinta-suave">
                        {documento.sujetoIdObjeto
                          ? `${documento.sujetoTipoObjeto} ${documento.sujetoIdObjeto}`
                          : "—"}
                      </td>
                      <td className="px-4 py-3 text-tinta-suave">{formatearFecha(documento.recibido)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="mt-4 flex items-center justify-between text-sm text-tinta-suave">
              <span>
                {total} documento{total === 1 ? "" : "s"}
              </span>
              {totalPaginas > 1 ? (
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    disabled={pagina === 0}
                    onClick={() => setPagina((actual) => actual - 1)}
                    className="rounded-lg border border-borde bg-white px-3 py-1.5 transition disabled:opacity-40"
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
                    className="rounded-lg border border-borde bg-white px-3 py-1.5 transition disabled:opacity-40"
                  >
                    Siguiente
                  </button>
                </div>
              ) : null}
            </div>
          </>
        )}
      </div>

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
