import { useMemo, useRef, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  cargarDocumento,
  obtenerBandeja,
  reprocesarDocumento,
  revisarDocumento,
  type DocumentoMotor,
} from "../../api/documental";
import { mensajeDeError } from "../../api/cliente";
import {
  Boton,
  BotonIcono,
  Campo,
  Selector,
  Tarjeta,
} from "../../componentes/Interfaz";
import { Cargando, ErrorPanel, Vacio } from "../../componentes/Estados";
import { BarraConfianza } from "../../componentes/Insignias";
import {
  IconoBuscar,
  IconoDocumentos,
  IconoEliminar,
  IconoRecargar,
  IconoSubir,
} from "../../componentes/Iconos";
import { useIdioma } from "../../contextos/ProveedorIdioma";
import {
  ESTADOS_DOCUMENTO,
  ESTADOS_EN_CURSO,
  ESTADOS_RECHAZABLES,
  ESTADOS_REPROCESABLES,
  comoFecha,
  leerBase64,
} from "../dominio";
import { EtiquetaEstadoMotor } from "./EtiquetaEstadoMotor";

const TAMANO_PAGINA = 25;

const textoOMenos = (valor: string | null | undefined) =>
  valor === null || valor === undefined || valor === "" ? "-" : valor;

export function BandejaDocumental({
  alAbrirDocumento,
}: {
  alAbrirDocumento: (documentoId: string) => void;
}) {
  const { t, idioma } = useIdioma();
  const clienteConsultas = useQueryClient();

  const [estado, setEstado] = useState("");
  const [plantilla, setPlantilla] = useState("");
  const [subiendo, setSubiendo] = useState(false);
  const [aviso, setAviso] = useState<{
    tono: "ok" | "error";
    texto: string;
  } | null>(null);
  const entradaArchivo = useRef<HTMLInputElement>(null);

  const filtros = useMemo(
    () => ({ estado, plantilla, limite: TAMANO_PAGINA }),
    [estado, plantilla],
  );

  const bandeja = useQuery({
    queryKey: ["documental-bandeja", filtros],
    queryFn: () => obtenerBandeja(filtros),
    refetchInterval: (consultaActiva) => {
      const documentos = consultaActiva.state.data?.documentos || [];
      return documentos.some((d) => ESTADOS_EN_CURSO.includes(d.estado))
        ? 2500
        : 30000;
    },
    staleTime: 5000,
    placeholderData: (previo) => previo,
  });

  const documentos = bandeja.data?.documentos || [];

  const invalidar = () =>
    clienteConsultas.invalidateQueries({ queryKey: ["documental-bandeja"] });

  const subir = async (evento: React.ChangeEvent<HTMLInputElement>) => {
    const archivo = evento.target.files?.[0];
    evento.target.value = "";
    if (!archivo) return;

    setSubiendo(true);
    try {
      const contenidoBase64 = await leerBase64(archivo);
      const respuesta = await cargarDocumento({
        origen: "WEB",
        nombreArchivo: archivo.name,
        tipoMime: archivo.type || undefined,
        contenidoBase64,
      });

      if (respuesta?.motivo === "DUPLICADO") {
        setAviso({ tono: "ok", texto: t("documental.bandeja.yaCargado") });
      } else {
        setAviso({ tono: "ok", texto: t("documental.bandeja.cargaAceptada") });
      }
      invalidar();
    } catch (error) {
      setAviso({ tono: "error", texto: mensajeDeError(error) });
    } finally {
      setSubiendo(false);
    }
  };

  const reprocesar = async (documentoId: string) => {
    try {
      await reprocesarDocumento(documentoId);
      setAviso({ tono: "ok", texto: t("documental.bandeja.reencolado") });
      invalidar();
    } catch (error) {
      setAviso({ tono: "error", texto: mensajeDeError(error) });
    }
  };

  const rechazar = async (documentoId: string) => {
    const motivo = window.prompt(t("documental.bandeja.motivoRechazo"));
    if (motivo === null) return;
    try {
      await revisarDocumento(documentoId, {
        decision: "RECHAZAR",
        motivo: motivo.trim() || null,
      });
      setAviso({ tono: "ok", texto: t("documental.bandeja.rechazado") });
      invalidar();
    } catch (error) {
      setAviso({ tono: "error", texto: mensajeDeError(error) });
    }
  };

  return (
    <div
      className="flex h-full flex-col gap-espacio-4"
      data-testid="documental-bandeja"
    >
      {aviso ? (
        <p
          role="status"
          className={`rounded-control px-espacio-3 py-espacio-2 text-pequeno font-semibold ${
            aviso.tono === "ok"
              ? "bg-exito-tenue text-exito-texto"
              : "bg-rojo-tenue text-rojo-alto"
          }`}
        >
          {aviso.texto}
        </p>
      ) : null}

      <Tarjeta className="flex min-h-0 flex-1 flex-col" padding="p-0">
        <div className="flex flex-wrap items-end gap-espacio-3 border-b border-borde px-espacio-4 py-espacio-3">
          <Selector
            etiqueta={t("documental.bandeja.colEstado")}
            value={estado}
            onChange={(evento) => setEstado(evento.target.value)}
            className="w-44"
          >
            <option value="">{t("documental.bandeja.todos")}</option>
            {ESTADOS_DOCUMENTO.map((item) => (
              <option key={item} value={item}>
                {t(`documental.estado.${item}`)}
              </option>
            ))}
          </Selector>

          <Campo
            etiqueta={t("documental.bandeja.colTipo")}
            value={plantilla}
            onChange={(evento) =>
              setPlantilla(evento.target.value.toUpperCase())
            }
            className="w-40"
            placeholder={t("documental.bandeja.filtroPlantillaPlaceholder")}
          />

          <div className="flex-1" />

          <input
            ref={entradaArchivo}
            type="file"
            accept=".pdf,.jpg,.jpeg,.png,.webp,.tif,.tiff"
            hidden
            onChange={subir}
          />

          <Boton
            variante="primario"
            cargando={subiendo}
            onClick={() => entradaArchivo.current?.click()}
          >
            <IconoSubir />
            {subiendo
              ? t("documental.bandeja.subiendo")
              : t("documental.bandeja.cargar")}
          </Boton>
        </div>

        <div className="min-h-0 flex-1 overflow-auto">
        {bandeja.isError ? (
          <div className="p-espacio-4">
            <ErrorPanel
              error={bandeja.error}
              mensaje={mensajeDeError(bandeja.error)}
              reintentar={() => bandeja.refetch()}
            />
          </div>
        ) : bandeja.isLoading ? (
          <Cargando />
        ) : !documentos.length ? (
          <Vacio
            titulo={t("documental.bandeja.vacio")}
            detalle={t("documental.bandeja.vacioDetalle")}
          />
        ) : (
          <table className="w-full border-collapse">
            <thead className="sticky top-0 z-10">
              <tr className="border-b border-borde bg-lienzo">
                <th className="px-espacio-4 py-espacio-3 text-left text-micro font-bold uppercase tracking-wider text-tinta-suave">
                  {t("documental.bandeja.colDocumento")}
                </th>
                <th className="px-espacio-4 py-espacio-3 text-left text-micro font-bold uppercase tracking-wider text-tinta-suave">
                  {t("documental.bandeja.colTipo")}
                </th>
                <th className="px-espacio-4 py-espacio-3 text-left text-micro font-bold uppercase tracking-wider text-tinta-suave">
                  {t("documental.bandeja.colOrigen")}
                </th>
                <th className="px-espacio-4 py-espacio-3 text-left text-micro font-bold uppercase tracking-wider text-tinta-suave">
                  {t("documental.bandeja.colEstado")}
                </th>
                <th className="px-espacio-4 py-espacio-3 text-left text-micro font-bold uppercase tracking-wider text-tinta-suave">
                  {t("documental.bandeja.colConfianza")}
                </th>
                <th className="px-espacio-4 py-espacio-3 text-left text-micro font-bold uppercase tracking-wider text-tinta-suave">
                  {t("documental.bandeja.colAsociado")}
                </th>
                <th className="px-espacio-4 py-espacio-3 text-left text-micro font-bold uppercase tracking-wider text-tinta-suave">
                  {t("documental.bandeja.colExcepciones")}
                </th>
                <th className="px-espacio-4 py-espacio-3 text-left text-micro font-bold uppercase tracking-wider text-tinta-suave">
                  {t("documental.bandeja.colFecha")}
                </th>
                <th className="px-espacio-4 py-espacio-3 text-right text-micro font-bold uppercase tracking-wider text-tinta-suave">
                  {t("documental.bandeja.colAcciones")}
                </th>
              </tr>
            </thead>
            <tbody>
              {documentos.map((documento: DocumentoMotor) => (
                <tr
                  key={documento.id}
                  className="cursor-pointer border-b border-borde last:border-b-0 hover:bg-lienzo"
                  onClick={() => alAbrirDocumento(documento.id)}
                >
                  <td className="px-espacio-4 py-espacio-3">
                    <div className="flex items-center gap-espacio-3">
                      <span className="grid size-8 shrink-0 place-items-center rounded-control bg-violeta-tenue text-violeta">
                        <IconoDocumentos />
                      </span>
                      <div className="min-w-0">
                        <p className="truncate text-pequeno font-semibold text-tinta">
                          {textoOMenos(documento.nombre_archivo)}
                        </p>
                        <p className="truncate text-micro text-tinta-suave">
                          {documento.referencia_externa || documento.origen}
                        </p>
                      </div>
                    </div>
                  </td>
                  <td className="px-espacio-4 py-espacio-3">
                    {documento.plantilla_codigo ? (
                      <span className="inline-flex items-center whitespace-nowrap rounded-insignia bg-accion-tonal px-espacio-3 py-espacio-1 text-micro font-bold uppercase text-accion-tonal-texto">
                        {documento.plantilla_codigo}
                      </span>
                    ) : (
                      <span className="text-micro text-tinta-suave">
                        {t("documental.bandeja.sinClasificar")}
                      </span>
                    )}
                  </td>
                  <td className="px-espacio-4 py-espacio-3 text-pequeno text-tinta">
                    {textoOMenos(documento.origen)}
                  </td>
                  <td className="px-espacio-4 py-espacio-3">
                    <EtiquetaEstadoMotor estado={documento.estado} />
                  </td>
                  <td className="px-espacio-4 py-espacio-3">
                    {documento.confianza === null ? (
                      <span className="text-micro text-tinta-suave">-</span>
                    ) : (
                      <BarraConfianza valor={documento.confianza} />
                    )}
                  </td>
                  <td className="px-espacio-4 py-espacio-3">
                    {documento.sujeto_id ? (
                      <div className="min-w-0">
                        <p className="truncate text-pequeno font-semibold text-tinta">
                          {documento.sujeto_tipo}
                        </p>
                        <p className="truncate text-micro text-tinta-suave">
                          {documento.sujeto_id}
                        </p>
                      </div>
                    ) : (
                      <span className="text-micro text-tinta-suave">
                        {t("documental.bandeja.sinAsociar")}
                      </span>
                    )}
                  </td>
                  <td className="px-espacio-4 py-espacio-3">
                    {Number(documento.excepciones_abiertas || 0) ? (
                      <span className="inline-flex items-center gap-espacio-1 rounded-insignia bg-alerta-tenue px-espacio-3 py-espacio-1 text-micro font-bold text-alerta-texto">
                        <span className="size-1.5 rounded-insignia bg-alerta" />
                        {documento.excepciones_abiertas}
                      </span>
                    ) : (
                      <span className="text-micro text-tinta-suave">—</span>
                    )}
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
                      {ESTADOS_REPROCESABLES.includes(documento.estado) ? (
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
                      {ESTADOS_RECHAZABLES.includes(documento.estado) ? (
                        <BotonIcono
                          tamano="sm"
                          aria-label={t("documental.bandeja.rechazar")}
                          className="text-rojo-alto hover:bg-rojo-tenue"
                          data-testid="documental-rechazar-fila"
                          onClick={(evento) => {
                            evento.stopPropagation();
                            void rechazar(documento.id);
                          }}
                        >
                          <IconoEliminar />
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
      </Tarjeta>
    </div>
  );
}
