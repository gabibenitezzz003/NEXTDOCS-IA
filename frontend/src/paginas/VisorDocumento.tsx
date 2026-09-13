import { useEffect, useId, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Cargando, ErrorPanel, Vacio } from "../componentes/Estados";
import type { DetalleDocumento, ValorExtraido } from "../tipos/api";
import { ESTADOS_DOCUMENTALES } from "../utilidades/estadosDocumento";
import {
  Boton,
  BotonIcono,
  Campo,
  Pastilla,
  Tarjeta,
} from "../componentes/Interfaz";
import {
  IconoCerrar,
  IconoDescargar,
  IconoRecargar,
} from "../componentes/Iconos";
import {
  BarraConfianza,
  InsigniaEstado,
  InsigniaPresencia,
  InsigniaSeveridad,
} from "../componentes/Insignias";
import { formatearFecha } from "./Documentos";
import { mensajeDeError } from "../api/cliente";
import {
  cerrar,
  obtenerDetalle,
  reprocesar,
  revisar,
  seleccionarCandidato,
  urlOriginal,
} from "../api/documentos";
import { useSesion } from "../contextos/ProveedorSesion";

type Pestana = "campos" | "hallazgos" | "asociacion" | "actividad";

export function VisorDocumento({
  documentoId,
  alCerrar,
}: {
  documentoId: string;
  alCerrar: () => void;
}) {
  const { tienePermiso } = useSesion();
  const clienteConsultas = useQueryClient();
  const dialogo = useRef<HTMLDialogElement>(null);
  const titulo = useRef<HTMLHeadingElement>(null);
  const identificador = useId();

  useEffect(() => {
    const elemento = dialogo.current;
    const origen =
      document.activeElement instanceof HTMLElement
        ? document.activeElement
        : null;
    const desbordamiento = document.body.style.overflow;
    elemento?.showModal();
    titulo.current?.focus({ preventScroll: true });
    document.body.style.overflow = "hidden";
    return () => {
      elemento?.close();
      document.body.style.overflow = desbordamiento;
      if (origen?.isConnected) origen.focus({ preventScroll: true });
    };
  }, []);
  const [pestana, setPestana] = useState<Pestana>("campos");
  const [correcciones, setCorrecciones] = useState<Record<string, string>>({});
  const [motivo, setMotivo] = useState("");
  const [aviso, setAviso] = useState<{
    tono: "ok" | "error";
    texto: string;
  } | null>(null);

  const consulta = useQuery({
    queryKey: ["documento", documentoId],
    queryFn: () => obtenerDetalle(documentoId),
  });

  function invalidar(actualizarIndicadores = false) {
    clienteConsultas.invalidateQueries({
      queryKey: ["documento", documentoId],
    });
    clienteConsultas.invalidateQueries({ queryKey: ["documentos"] });
    clienteConsultas.invalidateQueries({ queryKey: ["excepciones"] });
    clienteConsultas.invalidateQueries({ queryKey: ["resumen"] });
    if (actualizarIndicadores)
      clienteConsultas.invalidateQueries({ queryKey: ["kpi"] });
  }

  const decidir = useMutation({
    mutationFn: (decision: string) =>
      revisar(documentoId, {
        decision,
        motivo: motivo.trim() || undefined,
        correcciones: Object.keys(correcciones).length
          ? correcciones
          : undefined,
      }),
    onSuccess: (revision) => {
      setAviso({
        tono: "ok",
        texto: `Documento ${revision.estadoNuevo ? ESTADOS_DOCUMENTALES[revision.estadoNuevo].etiqueta : "revisado"}`,
      });
      setCorrecciones({});
      setMotivo("");
      invalidar(true);
    },
    onError: (error) =>
      setAviso({ tono: "error", texto: mensajeDeError(error) }),
  });

  const reproceso = useMutation({
    mutationFn: () => reprocesar(documentoId),
    onSuccess: () => {
      setAviso({ tono: "ok", texto: "Documento reencolado para reproceso" });
      invalidar(true);
    },
    onError: (error) =>
      setAviso({ tono: "error", texto: mensajeDeError(error) }),
  });

  const cierre = useMutation({
    mutationFn: () => cerrar(documentoId),
    onSuccess: () => {
      setAviso({ tono: "ok", texto: "Documento cerrado" });
      invalidar(true);
    },
    onError: (error) =>
      setAviso({ tono: "error", texto: mensajeDeError(error) }),
  });

  const eleccion = useMutation({
    mutationFn: (candidatoId: string) =>
      seleccionarCandidato(
        documentoId,
        candidatoId,
        motivo.trim() || "Seleccion desde el portal",
      ),
    onSuccess: () => {
      setAviso({ tono: "ok", texto: "Candidato asociado" });
      invalidar();
    },
    onError: (error) =>
      setAviso({ tono: "error", texto: mensajeDeError(error) }),
  });

  async function abrirOriginal() {
    try {
      window.open(await urlOriginal(documentoId), "_blank", "noopener");
    } catch (error) {
      setAviso({ tono: "error", texto: mensajeDeError(error) });
    }
  }

  const detalle = consulta.data;
  const documento = detalle?.documento;
  const puedeRevisar = tienePermiso("documentos.revisar");
  const puedeEscribir = tienePermiso("documentos.escribir");
  const trabajando =
    decidir.isPending ||
    reproceso.isPending ||
    cierre.isPending ||
    eleccion.isPending;

  const pestanas: [Pestana, string][] = [
    ["campos", "Campos (" + (detalle?.extraccion?.valores.length ?? 0) + ")"],
    [
      "hallazgos",
      "Hallazgos (" + (detalle?.validacion?.hallazgos.length ?? 0) + ")",
    ],
    ["asociacion", "Asociación (" + (detalle?.candidatos.length ?? 0) + ")"],
    ["actividad", "Actividad (" + (detalle?.revisiones.length ?? 0) + ")"],
  ];

  return (
    <dialog
      ref={dialogo}
      aria-modal="true"
      aria-labelledby={identificador + "-titulo"}
      aria-describedby={identificador + "-descripcion"}
      onKeyDown={(evento) => {
        if (evento.key !== "Tab") return;
        const controles = Array.from(
          evento.currentTarget.querySelectorAll<HTMLElement>(
            "button:not(:disabled), summary, a[href], input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex]",
          ),
        ).filter(
          (elemento) =>
            elemento.tabIndex >= 0 && elemento.getClientRects().length > 0,
        );
        const primero = controles[0];
        const ultimo = controles.at(-1);
        const activo = document.activeElement;
        if (
          evento.shiftKey &&
          (activo === primero || !controles.includes(activo as HTMLElement))
        ) {
          evento.preventDefault();
          (ultimo ?? titulo.current)?.focus();
        } else if (!evento.shiftKey && (activo === ultimo || !primero)) {
          evento.preventDefault();
          (primero ?? titulo.current)?.focus();
        }
      }}
      onCancel={(evento) => {
        evento.preventDefault();
        alCerrar();
      }}
      onClick={(evento) => {
        if (evento.target !== evento.currentTarget) return;
        const caja = evento.currentTarget.getBoundingClientRect();
        if (
          evento.clientX < caja.left ||
          evento.clientX > caja.right ||
          evento.clientY < caja.top ||
          evento.clientY > caja.bottom
        )
          alCerrar();
      }}
      className="fixed inset-y-0 right-0 left-auto m-0 h-dvh max-h-none w-full max-w-none border-0 bg-lienzo p-0 text-tinta shadow-panel-lateral backdrop:bg-grafito/50 md:w-[min(90vw,64rem)]"
    >
      <div className="flex h-full min-h-0 flex-col overflow-y-auto md:overflow-hidden [@media(max-height:600px)]:overflow-y-auto">
        <header className="sticky top-0 z-10 shrink-0 md:static border-b border-borde bg-superficie p-espacio-4 sm:px-espacio-6">
          <div className="flex items-start justify-between gap-espacio-3">
            <div className="min-w-0">
              <p
                id={identificador + "-descripcion"}
                className="mb-espacio-1 text-micro uppercase tracking-wide text-tinta-suave"
              >
                Revisión documental
              </p>
              <h2
                ref={titulo}
                tabIndex={-1}
                id={identificador + "-titulo"}
                title={documento?.nombre ?? "Documento"}
                className="line-clamp-2 font-titulo text-titulo-panel [overflow-wrap:anywhere] focus:outline-none"
              >
                {documento?.nombre ?? "Documento"}
              </h2>
            </div>
            <BotonIcono
              aria-label="Cerrar visor"
              variante="fantasma"
              onClick={alCerrar}
            >
              <IconoCerrar tamano={18} />
            </BotonIcono>
          </div>
          <div className="mt-espacio-3 flex flex-wrap items-center justify-between gap-espacio-3">
            <div className="flex min-w-0 flex-1 flex-wrap items-center gap-espacio-2 text-pequeno text-tinta-suave [overflow-wrap:anywhere]">
              {documento ? <InsigniaEstado estado={documento.estado} /> : null}
              {documento?.origenTipo === "GENERICO" ? (
                <Pastilla tono="alerta">Captura genérica</Pastilla>
              ) : null}
            </div>
            <Boton type="button" tamano="sm" onClick={abrirOriginal}>
              <span aria-hidden="true">
                <IconoDescargar tamano={14} />
              </span>
              Ver original
            </Boton>
          </div>
        </header>

        {detalle && !consulta.isError ? (
          <div
            role="tablist"
            aria-label="Información del documento"
            className="grid shrink-0 grid-cols-2 gap-espacio-1 border-b border-borde bg-superficie p-espacio-2 sm:grid-cols-4 sm:px-espacio-6"
            onKeyDown={(evento) => {
              const indice = pestanas.findIndex(([clave]) => clave === pestana);
              const siguiente =
                evento.key === "ArrowRight"
                  ? (indice + 1) % pestanas.length
                  : evento.key === "ArrowLeft"
                    ? (indice + pestanas.length - 1) % pestanas.length
                    : evento.key === "Home"
                      ? 0
                      : evento.key === "End"
                        ? pestanas.length - 1
                        : null;
              if (siguiente === null) return;
              evento.preventDefault();
              setPestana(pestanas[siguiente][0]);
              evento.currentTarget
                .querySelectorAll<HTMLButtonElement>('[role="tab"]')
                [siguiente]?.focus();
            }}
          >
            {pestanas.map(([clave, texto]) => (
              <Boton
                key={clave}
                type="button"
                role="tab"
                id={identificador + "-tab-" + clave}
                aria-controls={identificador + "-panel"}
                aria-selected={pestana === clave}
                tabIndex={pestana === clave ? 0 : -1}
                variante={pestana === clave ? "primario" : "fantasma"}
                tamano="sm"
                onClick={() => setPestana(clave)}
                className="min-w-0 px-espacio-2!"
              >
                {texto}
              </Boton>
            ))}
          </div>
        ) : null}

        <div className="barra-desplazamiento-fina min-h-0 flex-none overflow-visible overscroll-contain p-espacio-4 sm:p-espacio-6 md:flex-1 md:overflow-y-auto [@media(max-height:600px)]:flex-none [@media(max-height:600px)]:overflow-visible">
          {aviso ? (
            <div
              role={aviso.tono === "ok" ? "status" : "alert"}
              aria-atomic="true"
              className={
                "mb-espacio-4 rounded-control border p-espacio-3 text-pequeno [overflow-wrap:anywhere] " +
                (aviso.tono === "ok"
                  ? "border-exito-borde bg-exito-tenue text-exito-texto"
                  : "border-rojo-borde bg-rojo-tenue text-rojo-alto")
              }
            >
              {aviso.texto}
            </div>
          ) : null}
          {consulta.isPending ? (
            <Cargando filas={5} />
          ) : consulta.isError ? (
            <ErrorPanel
              titulo="No se pudo cargar el documento"
              mensaje={mensajeDeError(consulta.error)}
              reintentar={() => consulta.refetch()}
            />
          ) : !detalle ? (
            <Vacio titulo="Sin detalle disponible" />
          ) : (
            <>
              <details className="mb-espacio-4 min-w-0 rounded-panel border border-borde bg-superficie p-espacio-3">
                <summary className="cursor-pointer rounded-control text-pequeno font-semibold text-tinta-media focus-visible:outline-foco">
                  Datos del documento
                </summary>
                <dl className="mt-espacio-3 space-y-espacio-3 text-pequeno [overflow-wrap:anywhere]">
                  {[
                    ["Nombre completo", documento?.nombre ?? "Documento"],
                    [
                      "Tipo y versión",
                      documento?.codigoPlantilla
                        ? documento.codigoPlantilla +
                          (documento.numeroVersionPlantilla != null
                            ? " v" + documento.numeroVersionPlantilla
                            : "")
                        : "Sin plantilla",
                    ],
                    ["Origen del sujeto", documento?.sujetoOrigen ?? "—"],
                    ["Tipo de sujeto", documento?.sujetoTipoObjeto ?? "—"],
                    [
                      "Identificador del sujeto",
                      documento?.sujetoIdObjeto ?? "—",
                    ],
                  ].map(([etiqueta, valor]) => (
                    <div key={etiqueta}>
                      <dt className="text-micro uppercase text-tinta-suave">
                        {etiqueta}
                      </dt>
                      <dd className="mt-espacio-1">{valor}</dd>
                    </div>
                  ))}
                </dl>
              </details>
              {documento?.origenTipo === "GENERICO" ? (
                <div
                  role="note"
                  className="mb-espacio-4 rounded-control border border-alerta-borde bg-alerta-tenue p-espacio-3 text-pequeno text-alerta-texto"
                >
                  <p className="font-semibold">
                    Este documento no correspondía a ningún tipo del catálogo y
                    se capturó con el esquema genérico.
                  </p>
                  {documento.motivoTipo ? (
                    <p className="mt-espacio-1 [overflow-wrap:anywhere]">
                      {documento.motivoTipo}
                    </p>
                  ) : null}
                </div>
              ) : null}
              <div
                role="tabpanel"
                tabIndex={0}
                id={identificador + "-panel"}
                aria-labelledby={identificador + "-tab-" + pestana}
                className="min-w-0 rounded-control focus-visible:outline-foco"
              >
                {pestana === "campos" ? (
                  <PanelCampos
                    detalle={detalle}
                    correcciones={correcciones}
                    editable={puedeRevisar}
                    bloqueado={trabajando}
                    alCorregir={(clave, valor) =>
                      setCorrecciones((actuales) => {
                        const copia = { ...actuales };
                        if (valor === null) {
                          delete copia[clave];
                        } else {
                          copia[clave] = valor;
                        }
                        return copia;
                      })
                    }
                  />
                ) : null}
                {pestana === "hallazgos" ? (
                  <PanelHallazgos detalle={detalle} />
                ) : null}
                {pestana === "asociacion" ? (
                  <PanelAsociacion
                    detalle={detalle}
                    puedeElegir={puedeRevisar}
                    eligiendo={eleccion.isPending}
                    bloqueado={trabajando}
                    alElegir={(candidatoId) => eleccion.mutate(candidatoId)}
                  />
                ) : null}
                {pestana === "actividad" ? (
                  <PanelActividad detalle={detalle} />
                ) : null}
              </div>
              <dl
                aria-label="Contexto de extracción y validación"
                className="mt-espacio-5 grid min-w-0 grid-cols-2 gap-espacio-3 sm:grid-cols-4"
              >
                {[
                  ["Proveedor", detalle.extraccion?.proveedor ?? "—"],
                  ["Modelo", detalle.extraccion?.modelo ?? "—"],
                  ["Validación", detalle.validacion?.resultado ?? "—"],
                  [
                    "Autoaprobado",
                    detalle.validacion
                      ? detalle.validacion.autoaprobado
                        ? "Sí"
                        : "No"
                      : "—",
                  ],
                ].map(([etiqueta, valor]) => (
                  <div
                    key={etiqueta}
                    className="min-w-0 rounded-control border border-borde bg-superficie p-espacio-3"
                  >
                    <dt className="text-micro uppercase text-tinta-suave">
                      {etiqueta}
                    </dt>
                    <dd className="mt-espacio-1 text-pequeno font-semibold [overflow-wrap:anywhere]">
                      {valor}
                    </dd>
                  </div>
                ))}
              </dl>
            </>
          )}
        </div>

        {(puedeRevisar || puedeEscribir) && documento && !consulta.isError ? (
          <footer
            aria-label="Decisiones documentales"
            className="shrink-0 border-t border-borde bg-superficie p-espacio-4 sm:px-espacio-6"
          >
            {puedeRevisar && Object.keys(correcciones).length ? (
              <p
                role="status"
                aria-atomic="true"
                className="mb-espacio-2 text-pequeno font-semibold text-violeta"
              >
                {Object.keys(correcciones).length} campo(s) corregido(s) sin
                enviar. Se envían con la decisión.
              </p>
            ) : null}
            {puedeRevisar ? (
              <Campo
                etiqueta="Motivo de la decisión"
                value={motivo}
                disabled={trabajando}
                onChange={(evento) => setMotivo(evento.target.value)}
                placeholder="Motivo de la decisión (obligatorio para rechazar, observar o corregir)"
              />
            ) : null}
            <div className="mt-espacio-3 grid grid-cols-2 gap-espacio-2 sm:flex sm:flex-wrap">
              {puedeRevisar ? (
                <>
                  <Boton
                    type="button"
                    variante="primario"
                    disabled={
                      trabajando ||
                      !documento.transicionesPosibles.includes("APROBADO")
                    }
                    cargando={
                      decidir.isPending && decidir.variables === "APROBAR"
                    }
                    onClick={() => decidir.mutate("APROBAR")}
                  >
                    Aprobar
                  </Boton>
                  <Boton
                    type="button"
                    disabled={
                      trabajando ||
                      !documento.transicionesPosibles.includes("OBSERVADO")
                    }
                    cargando={
                      decidir.isPending && decidir.variables === "OBSERVAR"
                    }
                    onClick={() => decidir.mutate("OBSERVAR")}
                  >
                    Observar
                  </Boton>
                  <Boton
                    type="button"
                    variante="peligro"
                    disabled={
                      trabajando ||
                      !documento.transicionesPosibles.includes("RECHAZADO")
                    }
                    cargando={
                      decidir.isPending && decidir.variables === "RECHAZAR"
                    }
                    onClick={() => decidir.mutate("RECHAZAR")}
                  >
                    Rechazar
                  </Boton>
                </>
              ) : null}
              {puedeEscribir ? (
                <>
                  <Boton
                    type="button"
                    disabled={trabajando}
                    cargando={reproceso.isPending}
                    onClick={() => reproceso.mutate()}
                  >
                    <span aria-hidden="true">
                      <IconoRecargar tamano={14} />
                    </span>
                    Reprocesar
                  </Boton>
                  <Boton
                    type="button"
                    disabled={
                      trabajando ||
                      !documento.transicionesPosibles.includes("CERRADO")
                    }
                    cargando={cierre.isPending}
                    onClick={() => cierre.mutate()}
                    className="col-span-2"
                  >
                    Cerrar documento
                  </Boton>
                </>
              ) : null}
            </div>
            <span role="status" aria-atomic="true" className="sr-only">
              {trabajando ? "Enviando acción documental" : ""}
            </span>
            {documento.transicionesPosibles.length === 0 ? (
              <p className="mt-espacio-2 text-pequeno text-tinta-suave">
                Este documento está en un estado final y no admite más
                transiciones.
              </p>
            ) : null}
          </footer>
        ) : null}
      </div>
    </dialog>
  );
}

function PanelCampos({
  detalle,
  correcciones,
  editable,
  bloqueado,
  alCorregir,
}: {
  detalle: DetalleDocumento;
  correcciones: Record<string, string>;
  editable: boolean;
  bloqueado: boolean;
  alCorregir: (clave: string, valor: string | null) => void;
}) {
  if (!detalle.extraccion)
    return (
      <Vacio
        titulo="Sin extracción"
        detalle="Todavía no se ejecutó ninguna extracción sobre este documento."
      />
    );
  if (!detalle.extraccion.valores.length)
    return (
      <Vacio
        titulo="Sin campos extraídos"
        detalle="La extracción no devolvió campos para mostrar."
      />
    );
  return (
    <div>
      <h3 className="mb-espacio-3 font-titulo text-titulo-panel">
        Campos extraídos
      </h3>
      <ul className="space-y-espacio-3">
        {detalle.extraccion.valores.map((valor) => (
          <li key={valor.id}>
            <Tarjeta padding="p-espacio-4" className="rounded-panel!">
              <CampoExtraido
                valor={valor}
                corregido={correcciones[valor.claveCampo]}
                editable={editable}
                bloqueado={bloqueado}
                alCorregir={alCorregir}
              />
            </Tarjeta>
          </li>
        ))}
      </ul>
    </div>
  );
}

function CampoExtraido({
  valor,
  corregido,
  editable,
  bloqueado,
  alCorregir,
}: {
  valor: ValorExtraido;
  corregido?: string;
  editable: boolean;
  bloqueado: boolean;
  alCorregir: (clave: string, valor: string | null) => void;
}) {
  const id = useId();
  return (
    <div className="grid min-w-0 gap-espacio-3 sm:grid-cols-[minmax(0,1fr)_minmax(0,1.5fr)] lg:grid-cols-[minmax(0,1fr)_minmax(0,1.5fr)_minmax(0,1fr)]">
      <div className="min-w-0 [overflow-wrap:anywhere]">
        <p id={id + "-etiqueta"} className="text-pequeno font-semibold">
          {valor.etiqueta ?? valor.claveCampo}
        </p>
        <p
          id={id + "-clave"}
          className="mt-espacio-1 text-micro text-tinta-suave"
        >
          {valor.claveCampo}
        </p>
      </div>
      <div className="min-w-0">
        {editable ? (
          <Campo
            disabled={bloqueado}
            aria-labelledby={id + "-etiqueta"}
            aria-describedby={id + "-clave"}
            value={corregido ?? valor.valorNormalizado ?? ""}
            onChange={(evento) => {
              const nuevo = evento.target.value;
              alCorregir(
                valor.claveCampo,
                nuevo === (valor.valorNormalizado ?? "") ? null : nuevo,
              );
            }}
            className={
              corregido !== undefined
                ? "border-violeta! bg-violeta-tenue! font-medium text-violeta"
                : ""
            }
          />
        ) : (
          <p className="text-pequeno [overflow-wrap:anywhere]">
            {valor.valorNormalizado ?? "—"}
          </p>
        )}
        {valor.corregidoManualmente ? (
          <p className="mt-espacio-1 text-micro text-violeta">
            corregido manualmente
          </p>
        ) : null}
      </div>
      <dl className="flex flex-wrap gap-espacio-3 sm:col-span-2 lg:col-span-1">
        <div>
          <dt className="mb-espacio-1 text-micro text-tinta-suave">
            Presencia
          </dt>
          <dd>
            <InsigniaPresencia presencia={valor.presencia} />
          </dd>
        </div>
        <div>
          <dt className="mb-espacio-1 text-micro text-tinta-suave">
            Confianza
          </dt>
          <dd>
            <BarraConfianza valor={valor.confianza} />
          </dd>
        </div>
      </dl>
    </div>
  );
}

function PanelHallazgos({ detalle }: { detalle: DetalleDocumento }) {
  const hallazgos = detalle.validacion?.hallazgos ?? [];
  if (!detalle.validacion)
    return (
      <Vacio
        titulo="Sin validación"
        detalle="Todavía no hay una validación disponible para consultar hallazgos."
      />
    );
  if (!hallazgos.length)
    return (
      <p
        role="status"
        className="rounded-panel border border-exito-borde bg-exito-tenue p-espacio-6 text-center text-pequeno text-exito-texto"
      >
        La validación no encontró hallazgos.
      </p>
    );
  return (
    <div>
      <h3 className="mb-espacio-3 font-titulo text-titulo-panel">Hallazgos</h3>
      <ul className="space-y-espacio-3">
        {hallazgos.map((hallazgo) => (
          <li key={hallazgo.id}>
            <Tarjeta padding="p-espacio-4" className="rounded-panel!">
              <div className="flex flex-wrap items-center gap-espacio-2 text-micro text-tinta-suave [overflow-wrap:anywhere]">
                <InsigniaSeveridad severidad={hallazgo.severidad} />
                {hallazgo.codigoRegla ? (
                  <span>{hallazgo.codigoRegla}</span>
                ) : null}
                {hallazgo.claveCampo ? (
                  <span>{hallazgo.claveCampo}</span>
                ) : null}
              </div>
              <p className="mt-espacio-3 text-pequeno [overflow-wrap:anywhere]">
                {hallazgo.mensaje ?? "Sin mensaje disponible"}
              </p>
              {hallazgo.sobreescrito ? (
                <p className="mt-espacio-2 text-pequeno text-violeta [overflow-wrap:anywhere]">
                  Sobreescrito
                  {hallazgo.sobreescritoPor
                    ? " por " + hallazgo.sobreescritoPor
                    : ""}
                  {hallazgo.motivoSobreescritura
                    ? ": " + hallazgo.motivoSobreescritura
                    : ""}
                </p>
              ) : null}
            </Tarjeta>
          </li>
        ))}
      </ul>
    </div>
  );
}

function PanelAsociacion({
  detalle,
  puedeElegir,
  eligiendo,
  bloqueado,
  alElegir,
}: {
  detalle: DetalleDocumento;
  puedeElegir: boolean;
  eligiendo: boolean;
  bloqueado: boolean;
  alElegir: (candidatoId: string) => void;
}) {
  if (!detalle.candidatos.length)
    return (
      <Vacio
        titulo="Sin candidatos"
        detalle="Ningún conector devolvió candidatos para este documento."
      />
    );
  return (
    <div>
      <h3 className="mb-espacio-3 font-titulo text-titulo-panel">Asociación</h3>
      <ul className="space-y-espacio-3">
        {detalle.candidatos.map((candidato) => (
          <li key={candidato.id}>
            <Tarjeta
              padding="p-espacio-4"
              className={
                "rounded-panel! " +
                (candidato.seleccionado
                  ? "border-exito-borde! bg-exito-tenue!"
                  : "") +
                (candidato.descartado ? " opacity-50" : "")
              }
            >
              <div className="flex flex-wrap items-center justify-between gap-espacio-3">
                <div className="min-w-0 flex-1 [overflow-wrap:anywhere]">
                  <p className="text-pequeno font-semibold">
                    {candidato.descripcion ??
                      candidato.idObjeto ??
                      "Sin descripción disponible"}
                  </p>
                  <p className="mt-espacio-1 text-pequeno text-tinta-suave">
                    {[
                      candidato.conector,
                      candidato.tipoObjeto,
                      candidato.idObjeto,
                    ]
                      .filter(Boolean)
                      .join(" · ")}
                    {candidato.puntaje != null
                      ? " · puntaje " + candidato.puntaje
                      : ""}
                  </p>
                </div>
                {candidato.seleccionado ? (
                  <Pastilla tono="exito">Seleccionado</Pastilla>
                ) : puedeElegir && !candidato.descartado ? (
                  <Boton
                    type="button"
                    tamano="sm"
                    disabled={bloqueado}
                    onClick={() => alElegir(candidato.id)}
                  >
                    Elegir
                  </Boton>
                ) : null}
              </div>
            </Tarjeta>
          </li>
        ))}
      </ul>
      <span role="status" aria-atomic="true" className="sr-only">
        {eligiendo ? "Seleccionando candidato" : ""}
      </span>
    </div>
  );
}

function PanelActividad({ detalle }: { detalle: DetalleDocumento }) {
  if (!detalle.revisiones.length)
    return (
      <Vacio
        titulo="Sin actividad"
        detalle="Todavía no hubo revisiones humanas sobre este documento."
      />
    );
  return (
    <div>
      <h3 className="mb-espacio-3 font-titulo text-titulo-panel">Actividad</h3>
      <ol className="space-y-espacio-4 border-l border-borde pl-espacio-4">
        {detalle.revisiones.map((revision) => (
          <li key={revision.id} className="relative">
            <span
              aria-hidden="true"
              className="absolute top-espacio-4 -left-espacio-5 size-espacio-2 rounded-insignia bg-violeta"
            />
            <Tarjeta padding="p-espacio-4" className="rounded-panel!">
              <div className="flex flex-wrap items-center gap-espacio-2 text-pequeno">
                <span className="font-semibold">{revision.decision}</span>
                <span className="flex flex-wrap items-center gap-espacio-1">
                  {revision.estadoAnterior ? (
                    <InsigniaEstado estado={revision.estadoAnterior} />
                  ) : (
                    "—"
                  )}
                  <span aria-label="hacia">→</span>
                  {revision.estadoNuevo ? (
                    <InsigniaEstado estado={revision.estadoNuevo} />
                  ) : (
                    "—"
                  )}
                </span>
                <span className="text-pequeno text-tinta-suave sm:ml-auto">
                  {formatearFecha(revision.alta)}
                </span>
              </div>
              <p className="mt-espacio-2 text-pequeno text-tinta-suave [overflow-wrap:anywhere]">
                {revision.actor ?? "Actor no informado"}
              </p>
              {revision.motivo ? (
                <p className="mt-espacio-2 text-pequeno [overflow-wrap:anywhere]">
                  {revision.motivo}
                </p>
              ) : null}
              {revision.cambios.length ? (
                <ul className="mt-espacio-3 space-y-espacio-2 border-t border-borde pt-espacio-3">
                  {revision.cambios.map((cambio) => (
                    <li
                      key={cambio.claveCampo}
                      className="text-pequeno [overflow-wrap:anywhere]"
                    >
                      <span className="font-semibold">{cambio.claveCampo}</span>{" "}
                      <span className="line-through text-tinta-suave">
                        {cambio.valorAnterior ?? "vacío"}
                      </span>
                      {" → "}
                      <span className="text-violeta">
                        {cambio.valorNuevo ?? "vacío"}
                      </span>
                    </li>
                  ))}
                </ul>
              ) : null}
            </Tarjeta>
          </li>
        ))}
      </ol>
    </div>
  );
}
