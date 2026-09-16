import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Boton,
  Campo,
  Pastilla,
  Selector,
  Tarjeta,
} from "./Interfaz";
import { formatearFecha } from "../paginas/Documentos";
import { completarTarea, mensajeDeError } from "../api/procesos";
import type { TareaProceso } from "../api/procesos";
import { listarDocumentos } from "../api/documentos";
import { useSesion } from "../contextos/ProveedorSesion";
import { useIdioma } from "../contextos/ProveedorIdioma";

const TIPOS_TAREA_CONOCIDOS = new Set([
  "SOLICITUD_DOCUMENTO",
  "FORMULARIO",
  "VALIDACION_IA",
  "REVISION_HUMANA",
  "TAREA_EXTERNA",
  "TEMPORIZADOR",
  "SUBPROCESO",
]);

type Traductor = (ruta: string, params?: Record<string, string | number>) => string;

export function textoTipoTarea(tipo: string, t: Traductor): string {
  return TIPOS_TAREA_CONOCIDOS.has(tipo) ? t(`tipoTarea.${tipo}`) : tipo;
}

function tonoEstadoTarea(estado: TareaProceso["estado"]) {
  switch (estado) {
    case "COMPLETADA":
      return "exito" as const;
    case "VENCIDA":
      return "rojo" as const;
    case "CANCELADA":
      return "neutro" as const;
    default:
      return "informacion" as const;
  }
}

export function TarjetaTarea({ tarea, conInstancia = false }: { tarea: TareaProceso; conInstancia?: boolean }) {
  const { sesion } = useSesion();
  const { t } = useIdioma();
  const clienteConsultas = useQueryClient();
  const [abierta, setAbierta] = useState(false);
  const [decision, setDecision] = useState("APROBADO");
  const [motivo, setMotivo] = useState("");
  const [documentoId, setDocumentoId] = useState("");
  const [error, setError] = useState<string | null>(null);

  const consultaDocumentos = useQuery({
    queryKey: ["documentos", "entregables"],
    queryFn: () =>
      listarDocumentos({ tamano: 50, orden: "alta,desc", soloRaiz: true }),
    enabled: abierta && tarea.tipoNodo === "SOLICITUD_DOCUMENTO",
    staleTime: 30_000,
  });
  const documentos = consultaDocumentos.data?.content ?? [];

  const completar = useMutation({
    mutationFn: () => {
      const datos: Record<string, unknown> = {};
      if (tarea.tipoNodo === "SOLICITUD_DOCUMENTO" && documentoId.trim()) {
        datos.documentoId = documentoId.trim();
      }
      if (
        decision === "RECHAZADO" &&
        (!motivo.trim() || motivo.trim().length > 512)
      ) {
        throw new Error(t("operacion.motivoRechazoError"));
      }
      return completarTarea(tarea.id, {
        actor: sesion?.email ?? "operador",
        decision: decision || undefined,
        motivo: motivo.trim() || undefined,
        datos: Object.keys(datos).length ? datos : undefined,
      });
    },
    onSuccess: () => {
      setError(null);
      setAbierta(false);
      setMotivo("");
      setDocumentoId("");
      clienteConsultas.invalidateQueries({ queryKey: ["tareas"] });
      clienteConsultas.invalidateQueries({ queryKey: ["instancias"] });
      clienteConsultas.invalidateQueries({ queryKey: ["instancia"] });
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const pendiente = tarea.estado === "PENDIENTE" || tarea.estado === "VENCIDA";
  const enlaceToken =
    tarea.tipoNodo === "TAREA_EXTERNA" && tarea.datos
      ? String(tarea.datos["enlaceToken"] ?? "")
      : "";

  return (
    <Tarjeta>
      <div className="flex flex-wrap items-start justify-between gap-espacio-4">
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-espacio-3">
            <h3 className="font-titulo text-titulo-panel text-tinta">
              {textoTipoTarea(tarea.tipoNodo, t)}
            </h3>
            <Pastilla tono={tonoEstadoTarea(tarea.estado)}>
              {t(`estadoTarea.${tarea.estado}`)}
            </Pastilla>
            {tarea.vencimiento ? (
              <Pastilla tono="neutro">
                {t("operacion.vence", {
                  fecha: formatearFecha(tarea.vencimiento),
                })}
              </Pastilla>
            ) : null}
          </div>
          <p className="mt-espacio-1 text-pequeno text-tinta-suave">
            {conInstancia
              ? `${tarea.nombreDefinicion?.trim() || tarea.codigoDefinicion?.trim() || t("operacion.procesoSinNombre")} · `
              : ""}
            {t("operacion.paso", {
              nodo: tarea.nombreNodo?.trim() || tarea.nodoId,
            })}
            {tarea.asignadoA
              ? ` · ${t("operacion.asignada", { actor: tarea.asignadoA })}`
              : ""}
          </p>
          {tarea.completada ? (
            <p className="mt-espacio-1 text-pequeno text-tinta-suave">
              {t("operacion.completadaEl", {
                fecha: formatearFecha(tarea.completada),
              })}
              {tarea.completadaPor
                ? ` ${t("operacion.completadaPor", { actor: tarea.completadaPor })}`
                : ""}
              {tarea.decision
                ? ` · ${t(`estadoHallazgo.${tarea.decision}`)}`
                : ""}
            </p>
          ) : null}
          {tarea.motivo ? (
            <p className="mt-espacio-1 text-pequeno text-tinta-suave">
              {t("operacion.motivo", { motivo: tarea.motivo })}
            </p>
          ) : null}
          {enlaceToken ? (
            <p className="mt-espacio-2 break-all rounded-control bg-lienzo px-espacio-3 py-espacio-2 text-pequeno text-tinta-suave">
              {t("operacion.enlaceExterno", { token: enlaceToken })}
            </p>
          ) : null}
        </div>
        {pendiente ? (
          <Boton
            variante={abierta ? "secundario" : "primario"}
            onClick={() => setAbierta((valor) => !valor)}
            aria-expanded={abierta}
          >
            {abierta ? t("operacion.cerrar") : t("operacion.completar")}
          </Boton>
        ) : null}
      </div>

      {abierta ? (
        <div className="mt-espacio-4 space-y-espacio-3 border-t border-borde pt-espacio-4">
          {tarea.tipoNodo === "SOLICITUD_DOCUMENTO" ? (
            <Selector
              etiqueta={t("operacion.documentoEntregado")}
              ayuda={t("operacion.documentoEntregadoAyuda")}
              value={documentoId}
              onChange={(evento) => setDocumentoId(evento.target.value)}
            >
              <option value="">
                {consultaDocumentos.isPending
                  ? t("operacion.documentosCargando")
                  : t("operacion.documentoElegir")}
              </option>
              {documentos.map((documento) => (
                <option key={documento.id} value={documento.id}>
                  {[
                    documento.nombre ?? t("operacion.documentoSinNombre"),
                    documento.nombrePlantilla ?? documento.codigoPlantilla,
                    documento.estado,
                    documento.alta ? formatearFecha(documento.alta) : undefined,
                  ]
                    .filter(Boolean)
                    .join(" · ")}
                </option>
              ))}
            </Selector>
          ) : null}
          <div className="grid gap-espacio-3 md:grid-cols-2">
            <Selector
              etiqueta={t("operacion.decision")}
              value={decision}
              onChange={(evento) => setDecision(evento.target.value)}
            >
              <option value="APROBADO">{t("operacion.aprobado")}</option>
              <option value="RECHAZADO">{t("operacion.rechazado")}</option>
            </Selector>
            <Campo
              etiqueta={t("operacion.motivoRechazo")}
              placeholder={t("operacion.motivoRechazoPlaceholder")}
              value={motivo}
              onChange={(evento) => setMotivo(evento.target.value)}
            />
          </div>
          {error ? (
            <div
              role="alert"
              className="rounded-panel border border-rojo-borde bg-rojo-tenue px-espacio-4 py-espacio-3 text-pequeno text-rojo-alto"
            >
              {error}
            </div>
          ) : null}
          <div className="flex justify-end">
            <Boton
              variante="primario"
              cargando={completar.isPending}
              disabled={
                completar.isPending ||
                (decision === "RECHAZADO" && !motivo.trim()) ||
                (tarea.tipoNodo === "SOLICITUD_DOCUMENTO" && !documentoId.trim())
              }
              onClick={() => completar.mutate()}
            >
              {t("comun.confirmar")}
            </Boton>
          </div>
        </div>
      ) : null}
    </Tarjeta>
  );
}
