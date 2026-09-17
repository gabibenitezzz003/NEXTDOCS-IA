import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  ajustarPlantilla,
  cambiarEstadoPlantilla,
  instalarPlantilla,
  obtenerCatalogoPlantillas,
  obtenerPlantillasMotor,
  type CampoPlantilla,
  type PlantillaMotor,
} from "../../api/documental";
import { mensajeDeError } from "../../api/cliente";
import { Boton, Campo, Tarjeta } from "../../componentes/Interfaz";
import { Cargando, ErrorPanel, Vacio } from "../../componentes/Estados";
import { IconoFlechaAbajo, IconoDerecha } from "../../componentes/Iconos";
import { useIdioma } from "../../contextos/ProveedorIdioma";
import { enPorcentaje, fusionarCatalogoPlantillas } from "../dominio";
import { EtiquetaSeveridadMotor } from "./EtiquetaEstadoMotor";
import { ContenidoTiposPropuestos } from "../../paginas/TiposPropuestos";

function Interruptor({
  marcado,
  etiqueta,
  alCambiar,
  deshabilitado,
}: {
  marcado: boolean;
  etiqueta: string;
  alCambiar: (valor: boolean) => void;
  deshabilitado?: boolean;
}) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={marcado}
      aria-label={etiqueta}
      disabled={deshabilitado}
      onClick={() => alCambiar(!marcado)}
      className="inline-flex items-center gap-espacio-2 focus-visible:outline-foco disabled:cursor-not-allowed disabled:opacity-40"
    >
      <span
        className={`relative h-5 w-9 rounded-insignia transition-colors ${
          marcado ? "bg-accion-primaria" : "bg-borde-fuerte"
        }`}
      >
        <span
          className={`absolute top-0.5 size-4 rounded-insignia bg-blanco transition-transform ${
            marcado ? "translate-x-4.5" : "translate-x-0.5"
          }`}
        />
      </span>
      <span className="text-micro font-medium text-tinta-media">
        {etiqueta}
      </span>
    </button>
  );
}

function CampoPlantillaFila({
  campo,
  deshabilitado,
  alCambiar,
}: {
  campo: CampoPlantilla;
  deshabilitado: boolean;
  alCambiar: (cambio: Partial<CampoPlantilla>) => void;
}) {
  const { t } = useIdioma();
  return (
    <div className="grid grid-cols-[1fr_auto_auto_5.75rem] items-center gap-espacio-2 border-b border-borde py-espacio-2">
      <div className="min-w-0">
        <p className="truncate text-pequeno font-semibold text-tinta">
          {campo.clave}
        </p>
        <p className="text-micro text-tinta-suave">{campo.tipo}</p>
      </div>
      <Interruptor
        marcado={Boolean(campo.requerido)}
        etiqueta={t("documental.plantillas.requerido")}
        deshabilitado={deshabilitado}
        alCambiar={(valor) => alCambiar({ requerido: valor })}
      />
      <Interruptor
        marcado={Boolean(campo.critico)}
        etiqueta={t("documental.plantillas.critico")}
        deshabilitado={deshabilitado}
        alCambiar={(valor) => alCambiar({ critico: valor })}
      />
      <input
        type="number"
        aria-label={t("documental.plantillas.umbralCampo")}
        disabled={deshabilitado}
        className="h-control-pequeno w-full rounded-control border border-borde bg-superficie px-espacio-2 text-pequeno text-tinta focus-visible:outline-foco disabled:opacity-40"
        value={Math.round(Number(campo.umbral ?? 0.7) * 100)}
        min={0}
        max={100}
        step={5}
        onChange={(evento) =>
          alCambiar({ umbral: Number(evento.target.value) / 100 })
        }
      />
    </div>
  );
}

function PlantillaFila({
  plantilla,
  alGuardar,
  alCambiarEstado,
}: {
  plantilla: PlantillaMotor;
  alGuardar: (
    codigo: string,
    ajuste: {
      umbralAutoAprobacion: number;
      diasAvisoVencimiento: number;
      campos: {
        clave: string;
        requerido: boolean;
        critico: boolean;
        umbral: number;
      }[];
    },
  ) => Promise<void>;
  alCambiarEstado: (codigo: string, estado: string) => Promise<void>;
}) {
  const { t } = useIdioma();
  const definicion = plantilla.definicion || {};
  const [expandida, setExpandida] = useState(false);

  const inicial = () => ({
    umbralAutoAprobacion: Number(
      definicion.umbralAutoAprobacion ??
        plantilla.umbral_auto_aprobacion ??
        0.9,
    ),
    diasAvisoVencimiento: Number(definicion.diasAvisoVencimiento ?? 30),
    campos: (definicion.campos || []).map((c) => ({ ...c })),
  });

  const [borrador, setBorrador] = useState(inicial);
  const [tocado, setTocado] = useState(false);
  const [guardando, setGuardando] = useState(false);

  const cambiarCampo = (clave: string, cambio: Partial<CampoPlantilla>) => {
    setTocado(true);
    setBorrador((previo) => ({
      ...previo,
      campos: previo.campos.map((c) =>
        c.clave === clave ? { ...c, ...cambio } : c,
      ),
    }));
  };

  const descartar = () => {
    setBorrador(inicial());
    setTocado(false);
  };

  const guardar = async () => {
    setGuardando(true);
    try {
      await alGuardar(plantilla.codigo, {
        umbralAutoAprobacion: borrador.umbralAutoAprobacion,
        diasAvisoVencimiento: borrador.diasAvisoVencimiento,
        campos: borrador.campos.map((c) => ({
          clave: c.clave,
          requerido: Boolean(c.requerido),
          critico: Boolean(c.critico),
          umbral: Number(c.umbral ?? 0.7),
        })),
      });
      setTocado(false);
    } finally {
      setGuardando(false);
    }
  };

  const deprecada = plantilla.estado === "DEPRECADA";
  const reglas =
    (definicion as { reglas?: { severidad: string; mensaje: string }[] })
      .reglas || [];

  return (
    <Tarjeta padding="p-0">
      <button
        type="button"
        aria-expanded={expandida}
        onClick={() => setExpandida(!expandida)}
        className="flex w-full items-center gap-espacio-3 px-espacio-4 py-espacio-3 text-left focus-visible:outline-foco"
      >
        {expandida ? <IconoFlechaAbajo /> : <IconoDerecha />}
        <span className="min-w-0 flex-1 truncate text-pequeno font-bold text-tinta">
          {plantilla.nombre}
        </span>
        <span className="rounded-insignia bg-accion-tonal px-espacio-2 py-espacio-1 text-micro font-bold uppercase text-accion-tonal-texto">
          {definicion.familia || plantilla.familia || "—"}
        </span>
        <span className="rounded-insignia bg-lienzo px-espacio-2 py-espacio-1 text-micro font-bold text-tinta-media ring-1 ring-inset ring-borde">
          v{plantilla.version}
        </span>
        <span
          className={`rounded-insignia px-espacio-2 py-espacio-1 text-micro font-bold uppercase ${
            deprecada
              ? "bg-lienzo text-tinta-suave ring-1 ring-inset ring-borde"
              : "bg-exito-tenue text-exito-texto ring-1 ring-inset ring-exito-borde"
          }`}
        >
          {deprecada
            ? t("documental.plantillas.estadoInactiva")
            : t("documental.plantillas.estadoActiva")}
        </span>
        <span className="hidden text-micro text-tinta-suave sm:inline">
          {t("documental.plantillas.aprobaSolo")}{" "}
          {enPorcentaje(borrador.umbralAutoAprobacion)}
        </span>
      </button>

      {expandida ? (
        <div className="grid grid-cols-1 gap-espacio-4 border-t border-borde p-espacio-4 lg:grid-cols-[1fr_1.2fr]">
          <div className="flex flex-col gap-espacio-4">
            <div>
              <p className="mb-espacio-2 text-micro font-semibold text-tinta-media">
                {t("documental.plantillas.umbralAyuda")}
              </p>
              <input
                type="range"
                min={0.5}
                max={1}
                step={0.01}
                disabled={deprecada}
                value={borrador.umbralAutoAprobacion}
                aria-label={t("documental.plantillas.aprobaSolo")}
                aria-valuetext={enPorcentaje(borrador.umbralAutoAprobacion)}
                onChange={(evento) => {
                  setTocado(true);
                  setBorrador((previo) => ({
                    ...previo,
                    umbralAutoAprobacion: Number(evento.target.value),
                  }));
                }}
                className="w-full accent-[--color-accion-primaria] disabled:opacity-40"
              />
              <p className="text-micro text-tinta-suave">
                {enPorcentaje(borrador.umbralAutoAprobacion)}
              </p>
            </div>

            <Campo
              etiqueta={t("documental.plantillas.avisoVencimiento")}
              ayuda={t("documental.plantillas.avisoAyuda")}
              type="number"
              disabled={deprecada}
              min={1}
              max={365}
              value={borrador.diasAvisoVencimiento}
              onChange={(evento) => {
                setTocado(true);
                setBorrador((previo) => ({
                  ...previo,
                  diasAvisoVencimiento: Number(evento.target.value),
                }));
              }}
            />

            {reglas.length ? (
              <div className="flex flex-col gap-espacio-2">
                {reglas.map((regla, indice) => (
                  <div key={indice} className="flex items-center gap-espacio-2">
                    <EtiquetaSeveridadMotor severidad={regla.severidad} />
                    <p className="text-pequeno text-tinta">{regla.mensaje}</p>
                  </div>
                ))}
              </div>
            ) : null}

            <div className="flex flex-wrap gap-espacio-2">
              <Boton
                variante="primario"
                tamano="sm"
                cargando={guardando}
                disabled={!tocado || deprecada}
                onClick={() => void guardar()}
              >
                {t("documental.plantillas.guardar")}
              </Boton>
              <Boton
                variante="secundario"
                tamano="sm"
                disabled={!tocado}
                onClick={descartar}
              >
                {t("documental.plantillas.descartar")}
              </Boton>
              <Boton
                variante="secundario"
                tamano="sm"
                onClick={() =>
                  void alCambiarEstado(
                    plantilla.codigo,
                    deprecada ? "PUBLICADA" : "DEPRECADA",
                  )
                }
              >
                {deprecada
                  ? t("documental.plantillas.republicar")
                  : t("documental.plantillas.deprecar")}
              </Boton>
            </div>
          </div>

          <div>
            <div className="mb-espacio-2 flex items-center justify-between">
              <p className="text-micro font-bold uppercase tracking-wider text-tinta-suave">
                {t("documental.plantillas.campos")} ({borrador.campos.length})
              </p>
              <p className="text-micro text-tinta-suave">
                {t("documental.plantillas.umbralCampo")}
              </p>
            </div>
            <p className="mb-espacio-2 text-micro text-tinta-suave">
              {t("documental.plantillas.leyendaCampos")}
            </p>
            <div>
              {borrador.campos.map((campo) => (
                <CampoPlantillaFila
                  key={campo.clave}
                  campo={campo}
                  deshabilitado={deprecada}
                  alCambiar={(cambio) => cambiarCampo(campo.clave, cambio)}
                />
              ))}
            </div>
          </div>
        </div>
      ) : null}
    </Tarjeta>
  );
}

export function PlantillasDocumentales() {
  const { t } = useIdioma();
  const clienteConsultas = useQueryClient();
  const [aviso, setAviso] = useState<{
    tono: "ok" | "error";
    texto: string;
  } | null>(null);

  const plantillas = useQuery({
    queryKey: ["documental-plantillas"],
    queryFn: obtenerPlantillasMotor,
  });

  const catalogo = useQuery({
    queryKey: ["documental-catalogo"],
    queryFn: obtenerCatalogoPlantillas,
  });

  const instalados = new Set((plantillas.data || []).map((p) => p.codigo));
  const faltantes = fusionarCatalogoPlantillas(catalogo.data).filter(
    (item) => !instalados.has(item.codigo),
  );

  const guardar = async (
    codigo: string,
    ajuste: {
      umbralAutoAprobacion: number;
      diasAvisoVencimiento: number;
      campos: {
        clave: string;
        requerido: boolean;
        critico: boolean;
        umbral: number;
      }[];
    },
  ) => {
    try {
      await ajustarPlantilla(codigo, ajuste);
      setAviso({ tono: "ok", texto: t("documental.plantillas.guardada") });
      clienteConsultas.invalidateQueries({
        queryKey: ["documental-plantillas"],
      });
    } catch (error) {
      setAviso({ tono: "error", texto: mensajeDeError(error) });
    }
  };

  const cambiarEstado = async (codigo: string, estado: string) => {
    try {
      await cambiarEstadoPlantilla(codigo, estado);
      setAviso({
        tono: "ok",
        texto: t("documental.plantillas.estadoCambiado"),
      });
      clienteConsultas.invalidateQueries({
        queryKey: ["documental-plantillas"],
      });
    } catch (error) {
      setAviso({ tono: "error", texto: mensajeDeError(error) });
    }
  };

  const instalar = async (codigo: string) => {
    try {
      await instalarPlantilla(codigo);
      setAviso({ tono: "ok", texto: t("documental.plantillas.instalada") });
      clienteConsultas.invalidateQueries({
        queryKey: ["documental-plantillas"],
      });
    } catch (error) {
      setAviso({ tono: "error", texto: mensajeDeError(error) });
    }
  };

  if (plantillas.isLoading) return <Cargando />;
  if (plantillas.isError) {
    return (
      <ErrorPanel
        error={plantillas.error}
        mensaje={mensajeDeError(plantillas.error)}
        reintentar={() => plantillas.refetch()}
      />
    );
  }

  const listado = plantillas.data || [];

  return (
    <div
      className="flex flex-col gap-espacio-4 overflow-auto"
      data-testid="documental-plantillas"
    >
      <p className="text-pequeno text-tinta-suave">
        {t("documental.plantillas.bajada")}
      </p>

      {aviso ? (
        <p
          role="status"
          className={`text-pequeno font-semibold ${aviso.tono === "ok" ? "text-exito-texto" : "text-rojo-alto"}`}
        >
          {aviso.texto}
        </p>
      ) : null}

      {!listado.length ? (
        <Vacio
          titulo={t("documental.plantillas.vacio")}
          detalle={t("documental.plantillas.vacioDetalle")}
        />
      ) : (
        listado.map((plantilla) => (
          <PlantillaFila
            key={`${plantilla.codigo}-${plantilla.version}`}
            plantilla={plantilla}
            alGuardar={guardar}
            alCambiarEstado={cambiarEstado}
          />
        ))
      )}

      <section
        aria-label={t("documental.plantillas.detectados")}
        className="rounded-panel border border-borde bg-lienzo p-espacio-4"
      >
        <h3 className="text-pequeno font-bold text-tinta">
          {t("documental.plantillas.detectados")}
        </h3>
        <p className="mt-espacio-1 text-micro text-tinta-suave">
          {t("documental.plantillas.detectadosDesc")}
        </p>
        <div className="mt-espacio-4">
          <ContenidoTiposPropuestos />
        </div>
      </section>

      {faltantes.length ? (
        <Tarjeta padding="p-0">
          <div className="border-b border-borde px-espacio-4 py-espacio-3">
            <h3 className="text-pequeno font-bold text-tinta">
              {t("documental.plantillas.catalogo")}
            </h3>
          </div>
          {faltantes.map((item) => (
            <div
              key={item.codigo}
              className="flex items-center gap-espacio-3 border-b border-borde px-espacio-4 py-espacio-3 last:border-b-0"
            >
              <div className="min-w-0 flex-1">
                <p className="truncate text-pequeno font-semibold text-tinta">
                  {item.nombre}
                </p>
                <p className="text-micro text-tinta-suave">{item.codigo}</p>
              </div>
              <Boton tamano="sm" onClick={() => void instalar(item.codigo)}>
                {t("documental.plantillas.instalar")}
              </Boton>
            </div>
          ))}
        </Tarjeta>
      ) : null}
    </div>
  );
}
