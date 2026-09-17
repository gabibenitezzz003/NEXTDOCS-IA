import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  obtenerBandeja,
  obtenerCatalogoPlantillas,
  reprocesarDocumento,
  type DocumentoMotor,
} from "../../api/documental";
import { mensajeDeError } from "../../api/cliente";
import { Boton, BotonIcono, Tarjeta } from "../../componentes/Interfaz";
import { Cargando, ErrorPanel, Vacio } from "../../componentes/Estados";
import { BarraConfianza } from "../../componentes/Insignias";
import {
  IconoBuscar,
  IconoDocumentos,
  IconoRecargar,
} from "../../componentes/Iconos";
import { useIdioma } from "../../contextos/ProveedorIdioma";
import { comoFecha, fusionarCatalogoPlantillas } from "../dominio";
import type { PlantillaCatalogo } from "../../api/documental";
import { EtiquetaEstadoMotor } from "./EtiquetaEstadoMotor";

const sinDivididos = (documentos: DocumentoMotor[]) =>
  (documentos || []).filter(
    (documento) => (documento.estado as string) !== "DIVIDIDO",
  );

const fusionarPorId = (previos: DocumentoMotor[], pagina: DocumentoMotor[]) => {
  if (!pagina.length) return previos;
  const vistos = new Set(previos.map((documento) => documento.id));
  const extra = pagina.filter((documento) => !vistos.has(documento.id));
  return extra.length ? [...previos, ...extra] : previos;
};

export function ArchivoDocumentos({
  alAbrirDocumento,
}: {
  alAbrirDocumento: (documentoId: string) => void;
}) {
  const { t, idioma } = useIdioma();
  const clienteConsultas = useQueryClient();

  const catalogo = useQuery({
    queryKey: ["documental-catalogo"],
    queryFn: obtenerCatalogoPlantillas,
  });
  const carpetas = fusionarCatalogoPlantillas(catalogo.data);

  const [codigoElegido, setCodigoElegido] = useState<string | null>(null);
  const [acumulados, setAcumulados] = useState<DocumentoMotor[]>([]);
  const [cursorCarga, setCursorCarga] = useState<string | null>(null);
  const [aviso, setAviso] = useState<{
    tono: "ok" | "error";
    texto: string;
  } | null>(null);

  const codigoCarpeta = codigoElegido ?? carpetas[0]?.codigo ?? "";
  const carpetaActiva =
    carpetas.find((item) => item.codigo === codigoCarpeta) || null;

  const consulta = useQuery({
    queryKey: ["documental-archivo", codigoCarpeta, cursorCarga],
    queryFn: () =>
      obtenerBandeja({
        plantilla: codigoCarpeta,
        cursor: cursorCarga || undefined,
        limite: 100,
      }),
    enabled: Boolean(codigoCarpeta),
    staleTime: 15000,
    refetchInterval: 30000,
  });

  const paginaDeEstaCarpeta = consulta.data !== undefined;
  const paginaActual = paginaDeEstaCarpeta
    ? sinDivididos(consulta.data?.documentos || [])
    : [];
  const documentos = fusionarPorId(acumulados, paginaActual);
  const hayMas = Boolean(consulta.data?.cursor);
  const cargandoPrimera =
    Boolean(codigoCarpeta) && !documentos.length && consulta.isLoading;

  const elegirCarpeta = (codigo: string) => {
    if (codigo === codigoCarpeta) return;
    setCodigoElegido(codigo);
    setAcumulados([]);
    setCursorCarga(null);
  };

  const cargarMas = () => {
    const siguiente = consulta.data?.cursor;
    if (!siguiente) return;
    setAcumulados(documentos);
    setCursorCarga(siguiente);
  };

  const recargar = () => {
    setAcumulados([]);
    setCursorCarga(null);
    clienteConsultas.invalidateQueries({
      queryKey: ["documental-archivo", codigoCarpeta],
    });
  };

  const reprocesar = async (documentoId: string) => {
    try {
      await reprocesarDocumento(documentoId);
      setAviso({ tono: "ok", texto: t("documental.bandeja.reencolado") });
      recargar();
    } catch (error) {
      setAviso({ tono: "error", texto: mensajeDeError(error) });
    }
  };

  return (
    <div
      className="grid h-full min-h-0 grid-cols-1 gap-espacio-4 lg:grid-cols-[16rem_1fr]"
      data-testid="documental-documentos"
    >
      <Tarjeta className="min-h-0 overflow-auto" padding="p-espacio-2">
        {catalogo.isLoading ? (
          <Cargando />
        ) : (
          carpetas.map((carpeta: PlantillaCatalogo) => (
            <button
              key={carpeta.codigo}
              type="button"
              onClick={() => elegirCarpeta(carpeta.codigo)}
              aria-pressed={carpeta.codigo === codigoCarpeta}
              className={`flex w-full items-center gap-espacio-3 rounded-control px-espacio-3 py-espacio-2 text-left transition-colors focus-visible:outline-foco ${
                carpeta.codigo === codigoCarpeta
                  ? "bg-violeta-tenue"
                  : "hover:bg-lienzo"
              }`}
              data-testid={`documental-carpeta-${carpeta.codigo}`}
            >
              <span className="grid size-8 shrink-0 place-items-center rounded-control bg-accion-tonal text-accion-tonal-texto">
                <IconoDocumentos />
              </span>
              <span className="min-w-0">
                <span className="block truncate text-pequeno font-semibold text-tinta">
                  {carpeta.nombre}
                </span>
                <span className="block truncate text-micro text-tinta-suave">
                  {carpeta.codigo}
                </span>
              </span>
            </button>
          ))
        )}
      </Tarjeta>

      <Tarjeta className="flex min-h-0 flex-col overflow-hidden" padding="p-0">
        <div className="flex items-center justify-between gap-espacio-3 border-b border-borde px-espacio-4 py-espacio-3">
          <div className="min-w-0">
            <h3 className="truncate text-pequeno font-bold text-tinta">
              {carpetaActiva?.nombre || "—"}
            </h3>
            <p className="text-micro text-tinta-suave">{codigoCarpeta}</p>
          </div>
          <BotonIcono
            tamano="sm"
            aria-label={t("documental.archivo.recargar")}
            onClick={recargar}
          >
            <IconoRecargar />
          </BotonIcono>
        </div>

        {aviso ? (
          <p
            role="status"
            className={`px-espacio-4 pt-espacio-2 text-pequeno font-semibold ${aviso.tono === "ok" ? "text-exito-texto" : "text-rojo-alto"}`}
          >
            {aviso.texto}
          </p>
        ) : null}

        <div className="min-h-0 flex-1 overflow-auto">
          {consulta.isError ? (
            <div className="p-espacio-4">
              <ErrorPanel
                error={consulta.error}
                mensaje={mensajeDeError(consulta.error)}
                reintentar={() => consulta.refetch()}
              />
            </div>
          ) : cargandoPrimera ? (
            <Cargando />
          ) : !documentos.length ? (
            <div className="p-espacio-4">
              <Vacio
                titulo={t("documental.archivo.vacio")}
                detalle={t("documental.archivo.vacioDetalle")}
              />
            </div>
          ) : (
            <table className="w-full border-collapse">
              <thead>
                <tr className="border-b border-borde bg-lienzo">
                  {[
                    "colDocumento",
                    "colEstado",
                    "colConfianza",
                    "colFecha",
                  ].map((clave) => (
                    <th
                      key={clave}
                      className="px-espacio-4 py-espacio-3 text-left text-micro font-bold uppercase tracking-wider text-tinta-suave"
                    >
                      {t(`documental.bandeja.${clave}`)}
                    </th>
                  ))}
                  <th className="px-espacio-4 py-espacio-3" />
                </tr>
              </thead>
              <tbody>
                {documentos.map((documento) => (
                  <tr
                    key={documento.id}
                    className="cursor-pointer border-b border-borde last:border-b-0 hover:bg-lienzo"
                    onClick={() => alAbrirDocumento(documento.id)}
                  >
                    <td className="px-espacio-4 py-espacio-3">
                      <p className="truncate text-pequeno font-semibold text-tinta">
                        {documento.nombre_archivo}
                      </p>
                      <p className="truncate text-micro text-tinta-suave">
                        {documento.referencia_externa || documento.origen}
                      </p>
                    </td>
                    <td className="px-espacio-4 py-espacio-3">
                      <EtiquetaEstadoMotor estado={documento.estado} />
                    </td>
                    <td className="px-espacio-4 py-espacio-3">
                      <BarraConfianza
                        valor={documento.confianza ?? undefined}
                      />
                    </td>
                    <td className="whitespace-nowrap px-espacio-4 py-espacio-3 text-pequeno text-tinta">
                      {comoFecha(documento.creado_en, idioma)}
                    </td>
                    <td className="px-espacio-4 py-espacio-3 text-right">
                      <div className="flex justify-end gap-espacio-1">
                        <BotonIcono
                          tamano="sm"
                          aria-label={t("documental.bandeja.abrir")}
                          onClick={(evento) => {
                            evento.stopPropagation();
                            alAbrirDocumento(documento.id);
                          }}
                        >
                          <IconoBuscar />
                        </BotonIcono>
                        {documento.estado === "RECIBIDO" ||
                        documento.estado === "OBSERVADO" ? (
                          <BotonIcono
                            tamano="sm"
                            aria-label={t("documental.bandeja.reprocesar")}
                            onClick={(evento) => {
                              evento.stopPropagation();
                              void reprocesar(documento.id);
                            }}
                          >
                            <IconoRecargar />
                          </BotonIcono>
                        ) : null}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {hayMas ? (
          <div className="border-t border-borde p-espacio-3 text-center">
            <Boton
              variante="secundario"
              tamano="sm"
              cargando={consulta.isFetching}
              onClick={cargarMas}
              data-testid="documental-cargar-mas"
            >
              {t("documental.archivo.cargarMas")}
            </Boton>
          </div>
        ) : null}
      </Tarjeta>
    </div>
  );
}
