import { useMemo, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  confirmarEmparejamiento,
  enviarDocumento,
  obtenerBitacora,
  obtenerFicha,
  reprocesarDocumento,
  revisarDocumento,
  type CampoValor,
} from "../../api/documental";
import { mensajeDeError } from "../../api/cliente";
import { Boton, BotonIcono, Tarjeta } from "../../componentes/Interfaz";
import { Cargando, ErrorPanel, Vacio } from "../../componentes/Estados";
import { BarraConfianza } from "../../componentes/Insignias";
import {
  IconoCheck,
  IconoCerrar,
  IconoIzquierda,
  IconoRecargar,
  IconoEnlaceExterno,
} from "../../componentes/Iconos";
import { useIdioma } from "../../contextos/ProveedorIdioma";
import { comoFecha, presenciaDeCampo, textoDeValor } from "../dominio";
import {
  EtiquetaEstadoMotor,
  EtiquetaSeveridadMotor,
} from "./EtiquetaEstadoMotor";
import { LienzoOriginal, type Evidencia } from "./LienzoOriginal";

type PestanaVisor =
  "campos" | "renglones" | "hallazgos" | "asociacion" | "bitacora";

function FilaCampo({
  campo,
  activo,
  alSeleccionar,
  alCorregir,
}: {
  campo: CampoValor;
  activo: boolean;
  alSeleccionar: () => void;
  alCorregir: (clave: string, valor: string) => Promise<void>;
}) {
  const { t } = useIdioma();
  const presencia = presenciaDeCampo(campo);
  const [editando, setEditando] = useState(false);
  const [borrador, setBorrador] = useState(() =>
    textoDeValor(campo.valor_normalizado),
  );

  const guardar = async () => {
    await alCorregir(campo.clave, borrador);
    setEditando(false);
  };

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={alSeleccionar}
      onKeyDown={(evento) => {
        if (evento.key === "Enter" || evento.key === " ") {
          evento.preventDefault();
          alSeleccionar();
        }
      }}
      className={`cursor-pointer border-l-4 px-espacio-4 py-espacio-3 ${
        activo
          ? "border-alerta bg-alerta-tenue"
          : "border-transparent hover:bg-lienzo"
      }`}
      data-testid={`documental-campo-${campo.clave}`}
    >
      <div className="flex items-center gap-espacio-2">
        <span className="flex-1 text-micro text-tinta-suave">
          {campo.clave}
        </span>
        {presencia === "PRESENTE" ? (
          <BarraConfianza valor={campo.confianza ?? undefined} />
        ) : (
          <span
            className={`max-w-fit rounded-insignia px-espacio-2 py-espacio-1 text-micro font-medium ${
              presencia === "ILEGIBLE"
                ? "bg-alerta-tenue text-alerta-texto"
                : "bg-lienzo text-tinta-suave"
            }`}
          >
            {presencia === "ILEGIBLE"
              ? t("documental.visor.ilegible")
              : t("documental.visor.noFigura")}
          </span>
        )}
        <BotonIcono
          tamano="sm"
          aria-label={t("documental.visor.corregir")}
          onClick={(evento) => {
            evento.stopPropagation();
            setEditando(true);
          }}
        >
          <svg
            aria-hidden="true"
            viewBox="0 0 20 20"
            fill="none"
            stroke="currentColor"
            strokeWidth={1.5}
            className="size-4"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M13.6 3.6a1.8 1.8 0 0 1 2.6 2.5L7 15.3l-3.4.9.9-3.4 9.1-9.2Z"
            />
          </svg>
        </BotonIcono>
      </div>

      {editando ? (
        <div className="mt-espacio-2 flex gap-espacio-2">
          <input
            className="h-control-pequeno min-w-0 flex-1 rounded-control border border-borde bg-superficie px-espacio-3 text-pequeno text-tinta focus-visible:outline-foco"
            value={borrador}
            autoFocus
            onClick={(evento) => evento.stopPropagation()}
            onChange={(evento) => setBorrador(evento.target.value)}
            onKeyDown={(evento) => {
              if (evento.key === "Enter") {
                evento.preventDefault();
                void guardar();
              }
            }}
          />
          <BotonIcono
            tamano="sm"
            aria-label={t("documental.visor.guardar")}
            onClick={(evento) => {
              evento.stopPropagation();
              void guardar();
            }}
          >
            <IconoCheck />
          </BotonIcono>
          <BotonIcono
            tamano="sm"
            aria-label={t("documental.visor.cancelar")}
            onClick={(evento) => {
              evento.stopPropagation();
              setEditando(false);
            }}
          >
            <IconoCerrar />
          </BotonIcono>
        </div>
      ) : (
        <p
          className={`text-pequeno ${
            presencia === "PRESENTE"
              ? "font-semibold text-tinta"
              : "text-tinta-tenue"
          } break-words`}
        >
          {presencia === "PRESENTE"
            ? textoDeValor(campo.valor_normalizado)
            : t("documental.visor.sinDato")}
        </p>
      )}
    </div>
  );
}

export function VisorInteligente({
  documentoId,
  alVolver,
}: {
  documentoId: string;
  alVolver: () => void;
}) {
  const { t, idioma } = useIdioma();
  const clienteConsultas = useQueryClient();

  const ficha = useQuery({
    queryKey: ["documental-ficha", documentoId],
    queryFn: () => obtenerFicha(documentoId),
    refetchInterval: (consultaActiva) => {
      const estado = consultaActiva.state.data?.documento?.estado;
      return estado &&
        ["RECIBIDO", "PROCESANDO", "EXTRAIDO", "VALIDADO"].includes(estado)
        ? 3000
        : false;
    },
  });

  const bitacora = useQuery({
    queryKey: ["documental-bitacora", documentoId],
    queryFn: () => obtenerBitacora(documentoId),
  });

  const [campoActivo, setCampoActivo] = useState<string | null>(null);
  const [pestana, setPestana] = useState<PestanaVisor>("campos");
  const [motivo, setMotivo] = useState("");
  const [correoDestino, setCorreoDestino] = useState("");
  const [aviso, setAviso] = useState<{
    tono: "ok" | "error";
    texto: string;
  } | null>(null);

  const datos = ficha.data;
  const documento = datos?.documento;
  const valores = useMemo(() => datos?.valores || [], [datos]);
  const hallazgos = datos?.validacion?.hallazgos || [];
  const candidatos = useMemo(() => datos?.candidatos || [], [datos]);
  const items = useMemo(() => datos?.items || [], [datos]);
  const hermanos = datos?.hijos || [];

  const evidencia: Evidencia | null = useMemo(() => {
    const campo = valores.find((v) => v.clave === campoActivo);
    if (!campo) return null;
    return {
      pagina: campo.pagina,
      recorte: campo.recorte,
      textoFuente: campo.texto_fuente,
    };
  }, [valores, campoActivo]);

  const columnasDeItems = useMemo(() => {
    const claves = new Set<string>();
    for (const item of items) {
      for (const clave of Object.keys(item)) {
        if (clave !== "orden") claves.add(clave);
      }
    }
    return [...claves];
  }, [items]);

  const invalidar = () => {
    clienteConsultas.invalidateQueries({
      queryKey: ["documental-ficha", documentoId],
    });
    clienteConsultas.invalidateQueries({
      queryKey: ["documental-bitacora", documentoId],
    });
    clienteConsultas.invalidateQueries({ queryKey: ["documental-bandeja"] });
  };

  const mandarPorCorreo = async () => {
    try {
      await enviarDocumento(documentoId, correoDestino);
      setAviso({ tono: "ok", texto: t("documental.visor.correoEncolado") });
      setCorreoDestino("");
    } catch (error) {
      setAviso({ tono: "error", texto: mensajeDeError(error) });
    }
  };

  const corregir = async (clave: string, valor: string) => {
    try {
      await revisarDocumento(documentoId, {
        decision: "CORREGIR",
        claveCampo: clave,
        valor,
      });
      setAviso({ tono: "ok", texto: t("documental.visor.correccionGuardada") });
      invalidar();
    } catch (error) {
      setAviso({ tono: "error", texto: mensajeDeError(error) });
    }
  };

  const decidir = async (decision: "APROBAR" | "RECHAZAR") => {
    try {
      await revisarDocumento(documentoId, { decision, motivo: motivo || null });
      setAviso({
        tono: "ok",
        texto: t(`documental.visor.decision.${decision}`),
      });
      setMotivo("");
      invalidar();
    } catch (error) {
      setAviso({ tono: "error", texto: mensajeDeError(error) });
    }
  };

  const reprocesar = async () => {
    try {
      await reprocesarDocumento(documentoId);
      setAviso({ tono: "ok", texto: t("documental.visor.reencolado") });
      invalidar();
    } catch (error) {
      setAviso({ tono: "error", texto: mensajeDeError(error) });
    }
  };

  const asociar = async (candidatoId: string) => {
    try {
      await confirmarEmparejamiento(documentoId, candidatoId, motivo || null);
      setAviso({ tono: "ok", texto: t("documental.visor.asociado") });
      invalidar();
    } catch (error) {
      setAviso({ tono: "error", texto: mensajeDeError(error) });
    }
  };

  if (ficha.isLoading) {
    return <Cargando />;
  }

  if (ficha.isError) {
    return (
      <ErrorPanel
        error={ficha.error}
        mensaje={mensajeDeError(ficha.error)}
        reintentar={() => ficha.refetch()}
      />
    );
  }

  if (!documento) {
    return <Vacio titulo={t("documental.visor.noEncontrado")} />;
  }

  const cerrado =
    documento.estado === "APROBADO" || documento.estado === "RECHAZADO";

  const pestanas: { id: PestanaVisor; etiqueta: string }[] = [
    { id: "campos", etiqueta: t("documental.visor.campos") },
    {
      id: "renglones",
      etiqueta: `${t("documental.visor.renglones")}${items.length ? ` (${items.length})` : ""}`,
    },
    {
      id: "hallazgos",
      etiqueta: `${t("documental.visor.hallazgos")}${hallazgos.length ? ` (${hallazgos.length})` : ""}`,
    },
    { id: "asociacion", etiqueta: t("documental.visor.asociacion") },
    { id: "bitacora", etiqueta: t("documental.visor.bitacora") },
  ];

  return (
    <div
      className="flex h-full flex-col gap-espacio-4"
      data-testid="documental-visor"
    >
      <div className="flex flex-wrap items-center gap-espacio-2">
        <BotonIcono
          tamano="sm"
          aria-label={t("documental.visor.volver")}
          onClick={alVolver}
          data-testid="documental-volver"
        >
          <IconoIzquierda />
        </BotonIcono>

        <h2 className="max-w-72 truncate font-titulo text-titulo-panel text-tinta">
          {documento.nombre_archivo}
        </h2>

        <EtiquetaEstadoMotor estado={documento.estado} />

        {documento.confianza !== null ? (
          <BarraConfianza valor={documento.confianza} />
        ) : null}

        {hermanos.length ? (
          <span className="text-micro text-tinta-suave">
            {t("documental.visor.delLote", { valor: hermanos.length })}
          </span>
        ) : null}

        <div className="flex-1" />

        <div className="flex items-center gap-espacio-1">
          <input
            type="email"
            aria-label={t("documental.visor.enviarA")}
            placeholder={t("documental.visor.enviarA")}
            value={correoDestino}
            onChange={(evento) => setCorreoDestino(evento.target.value)}
            className="h-control-pequeno w-44 rounded-control border border-borde bg-superficie px-espacio-3 text-pequeno text-tinta focus-visible:outline-foco"
          />
          <BotonIcono
            tamano="sm"
            disabled={!correoDestino.includes("@")}
            aria-label={t("documental.visor.enviar")}
            onClick={() => void mandarPorCorreo()}
          >
            <IconoEnlaceExterno />
          </BotonIcono>
        </div>

        {!cerrado ? (
          <>
            <input
              aria-label={t("documental.visor.motivoOpcional")}
              placeholder={t("documental.visor.motivoOpcional")}
              value={motivo}
              onChange={(evento) => setMotivo(evento.target.value)}
              className="h-control-pequeno w-44 rounded-control border border-borde bg-superficie px-espacio-3 text-pequeno text-tinta focus-visible:outline-foco"
            />
            <Boton
              variante="secundario"
              tamano="sm"
              onClick={() => void reprocesar()}
            >
              <IconoRecargar />
              {t("documental.visor.reprocesar")}
            </Boton>
            <Boton
              variante="peligro"
              tamano="sm"
              onClick={() => void decidir("RECHAZAR")}
              data-testid="documental-rechazar"
            >
              {t("documental.visor.rechazar")}
            </Boton>
            <Boton
              variante="primario"
              tamano="sm"
              onClick={() => void decidir("APROBAR")}
              data-testid="documental-aprobar"
            >
              {t("documental.visor.aprobar")}
            </Boton>
          </>
        ) : null}
      </div>

      {aviso ? (
        <p
          role="status"
          className={`text-pequeno font-semibold ${aviso.tono === "ok" ? "text-exito-texto" : "text-rojo-alto"}`}
        >
          {aviso.texto}
        </p>
      ) : null}

      <div className="grid min-h-0 flex-1 grid-cols-1 gap-espacio-4 lg:grid-cols-[1.15fr_1fr]">
        <Tarjeta
          className="flex min-h-0 flex-col overflow-hidden"
          padding="p-0"
        >
          <div className="border-b border-borde bg-lienzo px-espacio-4 py-espacio-2 text-pequeno font-bold text-tinta">
            {t("documental.visor.original")}
          </div>
          <div className="min-h-0 flex-1">
            <LienzoOriginal documento={documento} evidencia={evidencia} />
          </div>
        </Tarjeta>

        <Tarjeta
          className="flex min-h-0 flex-col overflow-hidden"
          padding="p-0"
        >
          <div role="tablist" className="flex border-b border-borde">
            {pestanas.map((item) => (
              <button
                key={item.id}
                type="button"
                role="tab"
                aria-selected={pestana === item.id}
                onClick={() => setPestana(item.id)}
                className={`min-h-control-pequeno flex-1 px-espacio-2 text-pequeno font-semibold transition-colors focus-visible:outline-foco ${
                  pestana === item.id
                    ? "border-b-2 border-accion-primaria text-tinta"
                    : "text-tinta-suave hover:text-tinta"
                }`}
              >
                {item.etiqueta}
              </button>
            ))}
          </div>

          <div className="min-h-0 flex-1 overflow-auto">
            {pestana === "campos" ? (
              <div className="flex flex-col">
                {valores.length ? (
                  valores.map((campo) => (
                    <FilaCampo
                      key={campo.clave}
                      campo={campo}
                      activo={campoActivo === campo.clave}
                      alSeleccionar={() => setCampoActivo(campo.clave)}
                      alCorregir={corregir}
                    />
                  ))
                ) : (
                  <div className="p-espacio-4">
                    <p className="text-pequeno text-tinta-suave">
                      {t("documental.visor.sinCampos")}
                    </p>
                  </div>
                )}
              </div>
            ) : null}

            {pestana === "renglones" ? (
              items.length ? (
                <div className="p-espacio-4">
                  <div className="overflow-auto rounded-panel border border-borde">
                    <table className="w-full border-collapse">
                      <thead>
                        <tr>
                          <th className="border-b border-borde bg-lienzo px-espacio-3 py-espacio-2 text-left text-micro font-bold uppercase tracking-wider text-tinta-suave">
                            #
                          </th>
                          {columnasDeItems.map((columna) => (
                            <th
                              key={columna}
                              className="whitespace-nowrap border-b border-borde bg-lienzo px-espacio-3 py-espacio-2 text-left text-micro font-bold uppercase tracking-wider text-tinta-suave"
                            >
                              {columna}
                            </th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        {items.map((item) => (
                          <tr key={item.orden} className="hover:bg-lienzo">
                            <td className="w-10 border-b border-borde px-espacio-3 py-espacio-2 text-pequeno text-tinta-suave">
                              {item.orden + 1}
                            </td>
                            {columnasDeItems.map((columna) => (
                              <td
                                key={columna}
                                className="border-b border-borde px-espacio-3 py-espacio-2 text-pequeno text-tinta"
                              >
                                <span
                                  className={
                                    typeof item[columna] === "number"
                                      ? "font-mono"
                                      : undefined
                                  }
                                >
                                  {textoDeValor(item[columna])}
                                </span>
                              </td>
                            ))}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              ) : (
                <div className="p-espacio-4">
                  <p className="text-pequeno text-tinta-suave">
                    {t("documental.visor.sinRenglones")}
                  </p>
                </div>
              )
            ) : null}

            {pestana === "hallazgos" ? (
              <div className="flex flex-col gap-espacio-3 p-espacio-4">
                {hallazgos.length ? (
                  hallazgos.map((hallazgo, indice) => (
                    <div
                      key={`${hallazgo.codigo}-${indice}`}
                      className="flex flex-col gap-espacio-2 rounded-panel border border-borde p-espacio-3"
                    >
                      <div className="flex items-center gap-espacio-2">
                        <EtiquetaSeveridadMotor
                          severidad={hallazgo.severidad}
                        />
                        <span className="text-micro font-bold text-tinta">
                          {hallazgo.codigo}
                        </span>
                      </div>
                      <p className="text-pequeno text-tinta">
                        {hallazgo.detalle || hallazgo.mensaje}
                      </p>
                    </div>
                  ))
                ) : (
                  <p className="text-pequeno text-tinta-suave">
                    {t("documental.visor.sinHallazgos")}
                  </p>
                )}
              </div>
            ) : null}

            {pestana === "asociacion" ? (
              <div className="flex flex-col gap-espacio-3 p-espacio-4">
                {candidatos.length ? (
                  candidatos.map((candidato) => (
                    <div
                      key={candidato.id}
                      className="flex items-center gap-espacio-2 rounded-panel border border-borde p-espacio-3"
                    >
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-pequeno font-semibold text-tinta">
                          {candidato.tipo_objeto} · {candidato.objeto_id || "-"}
                        </p>
                        <p className="text-micro text-tinta-suave">
                          {t("documental.visor.puntaje")} {candidato.puntaje} ·{" "}
                          {candidato.metodo} · {candidato.estado}
                        </p>
                      </div>
                      {!cerrado ? (
                        <Boton
                          tamano="sm"
                          onClick={() => void asociar(candidato.id)}
                        >
                          {t("documental.visor.confirmar")}
                        </Boton>
                      ) : null}
                    </div>
                  ))
                ) : (
                  <p className="text-pequeno text-tinta-suave">
                    {t("documental.visor.sinCandidatos")}
                  </p>
                )}
              </div>
            ) : null}

            {pestana === "bitacora" ? (
              <div className="flex flex-col gap-espacio-3 p-espacio-4">
                {(bitacora.data || []).map((entrada) => (
                  <div key={entrada.id} className="flex gap-espacio-3">
                    <span className="min-w-32 text-micro text-tinta-suave">
                      {comoFecha(entrada.creado_en, idioma)}
                    </span>
                    <div>
                      <p className="text-pequeno font-semibold text-tinta">
                        {entrada.accion}
                      </p>
                      <p className="text-micro text-tinta-suave">
                        {entrada.tipo_actor} · {entrada.origen}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            ) : null}
          </div>
        </Tarjeta>
      </div>
    </div>
  );
}
