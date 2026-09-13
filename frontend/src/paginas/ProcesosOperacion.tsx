import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Cargando, ErrorPanel, Vacio } from "../componentes/Estados";
import {
  Boton,
  CabeceraTarjeta,
  Campo,
  Pastilla,
  Selector,
  Tarjeta,
} from "../componentes/Interfaz";
import { formatearFecha } from "./Documentos";
import {
  cancelarInstancia,
  completarTarea,
  listarInstancias,
  listarTareas,
  mensajeDeError,
  obtenerInstancia,
  pausarInstancia,
  reanudarInstancia,
} from "../api/procesos";
import type {
  EstadoInstanciaProceso,
  EventoInstancia,
  TareaProceso,
} from "../api/procesos";
import { useSesion } from "../contextos/ProveedorSesion";

const TEXTO_ACCION: Record<string, string> = {
  INSTANCIA_INICIADA: "Instancia iniciada",
  NODO_INGRESADO: "Paso ingresado",
  TAREA_CREADA: "Tarea creada",
  TAREA_COMPLETADA: "Tarea completada",
  DECISION_TOMADA: "Decisión tomada",
  INSTANCIA_COMPLETADA: "Proceso completado",
  INSTANCIA_CANCELADA: "Proceso cancelado",
  INSTANCIA_BLOQUEADA: "Proceso bloqueado",
  INSTANCIA_REANUDADA: "Proceso reanudado",
  TAREA_VENCIDA: "Tarea vencida",
  VALIDACION_IA_EJECUTADA: "Validación automática ejecutada",
  NOTIFICACION_ENVIADA: "Notificación enviada",
  ACCION_API_EJECUTADA: "Acción de API ejecutada",
};

const TEXTO_TIPO_TAREA: Record<string, string> = {
  SOLICITUD_DOCUMENTO: "Solicitar documento",
  FORMULARIO: "Formulario",
  VALIDACION_IA: "Validación con IA",
  REVISION_HUMANA: "Revisión humana",
  TAREA_EXTERNA: "Tarea externa",
  TEMPORIZADOR: "Espera (temporizador)",
  SUBPROCESO: "Subproceso",
};

function textoAccion(accion: string): string {
  return TEXTO_ACCION[accion] ?? accion;
}

function textoTipoTarea(tipo: string): string {
  return TEXTO_TIPO_TAREA[tipo] ?? tipo;
}

function tonoEstadoInstancia(estado: EstadoInstanciaProceso) {
  switch (estado) {
    case "COMPLETADA":
      return "exito" as const;
    case "BLOQUEADA":
      return "alerta" as const;
    case "CANCELADA":
      return "rojo" as const;
    case "ESPERANDO":
      return "informacion" as const;
    default:
      return "violeta" as const;
  }
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

const ES_FINAL = ["COMPLETADA", "CANCELADA"];

export function BandejaInstancias({ alAbrir }: { alAbrir: (id: string) => void }) {
  const [estado, setEstado] = useState<string>("");

  const consulta = useQuery({
    queryKey: ["instancias", estado],
    queryFn: () => listarInstancias(estado || undefined),
    refetchInterval: 15000,
  });

  const instancias = consulta.data ?? [];

  return (
    <>
      <div className="mb-espacio-4 flex flex-wrap items-end justify-between gap-espacio-4">
        <Selector
          etiqueta="Filtrar por estado"
          value={estado}
          onChange={(evento) => setEstado(evento.target.value)}
          className="max-w-56"
        >
          <option value="">Todos los estados</option>
          <option value="ACTIVA">En ejecución</option>
          <option value="ESPERANDO">Esperando tareas</option>
          <option value="BLOQUEADA">Bloqueadas</option>
          <option value="COMPLETADA">Completadas</option>
          <option value="CANCELADA">Canceladas</option>
        </Selector>
        <p className="text-pequeno text-tinta-suave">
          Cada instancia conserva la versión del proceso con la que comenzó.
        </p>
      </div>

      {consulta.isPending ? (
        <Cargando filas={3} alto="h-24" />
      ) : consulta.isError ? (
        <ErrorPanel
          mensaje={mensajeDeError(consulta.error)}
          reintentar={() => consulta.refetch()}
        />
      ) : instancias.length === 0 ? (
        <Vacio
          titulo="Todavía no hay procesos en ejecución"
          detalle="Iniciá uno desde la pestaña Definiciones con el botón Probar proceso de una versión publicada, o desde donde el producto lo dispare."
        />
      ) : (
        <ul aria-label="Instancias de proceso" className="space-y-espacio-4">
          {instancias.map((instancia) => (
            <li key={instancia.id}>
              <Tarjeta className="grid items-center gap-espacio-4 lg:grid-cols-[minmax(0,1fr)_auto]">
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-espacio-3">
                    <h2 className="break-words font-titulo text-titulo-panel text-tinta">
                      {instancia.codigoDefinicion}
                    </h2>
                    <Pastilla tono={tonoEstadoInstancia(instancia.estado)}>
                      {instancia.estado}
                    </Pastilla>
                  </div>
                  <p className="mt-espacio-1 text-pequeno text-tinta-suave">
                    v{instancia.numeroVersion} · iniciada{" "}
                    {instancia.alta ? formatearFecha(instancia.alta) : "—"}
                    {instancia.sujetoId ? ` · ${instancia.sujetoTipo ?? "sujeto"} ${instancia.sujetoId}` : ""}
                  </p>
                  {instancia.tareas?.length ? (
                    <p className="mt-espacio-2 text-pequeno text-tinta-suave">
                      {
                        instancia.tareas.filter(
                          (tarea) => tarea.estado === "PENDIENTE" || tarea.estado === "VENCIDA",
                        ).length
                      }{" "}
                      de {instancia.tareas.length} tareas pendientes
                    </p>
                  ) : null}
                </div>
                <Boton variante="secundario" onClick={() => alAbrir(instancia.id)}>
                  Ver detalle
                </Boton>
              </Tarjeta>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}

export function DetalleInstancia({
  instanciaId,
  alVolver,
}: {
  instanciaId: string;
  alVolver: () => void;
}) {
  const { sesion } = useSesion();
  const clienteConsultas = useQueryClient();
  const [motivo, setMotivo] = useState("");
  const [error, setError] = useState<string | null>(null);

  const consulta = useQuery({
    queryKey: ["instancia", instanciaId],
    queryFn: () => obtenerInstancia(instanciaId),
    refetchInterval: (consultaActual) =>
      consultaActual.state.data && ES_FINAL.includes(consultaActual.state.data.estado)
        ? false
        : 15000,
  });

  const pausar = useMutation({
    mutationFn: () =>
      pausarInstancia(instanciaId, {
        motivo: motivo.trim(),
        actor: sesion?.email ?? "operador",
      }),
    onSuccess: () => {
      setError(null);
      setMotivo("");
      clienteConsultas.invalidateQueries({ queryKey: ["instancia", instanciaId] });
      clienteConsultas.invalidateQueries({ queryKey: ["instancias"] });
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const reanudar = useMutation({
    mutationFn: () => reanudarInstancia(instanciaId, sesion?.email ?? "operador"),
    onSuccess: () => {
      setError(null);
      clienteConsultas.invalidateQueries({ queryKey: ["instancia", instanciaId] });
      clienteConsultas.invalidateQueries({ queryKey: ["instancias"] });
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const cancelar = useMutation({
    mutationFn: () =>
      cancelarInstancia(instanciaId, {
        motivo: motivo.trim() || undefined,
        actor: sesion?.email ?? "operador",
      }),
    onSuccess: () => {
      setError(null);
      setMotivo("");
      clienteConsultas.invalidateQueries({ queryKey: ["instancia", instanciaId] });
      clienteConsultas.invalidateQueries({ queryKey: ["instancias"] });
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const instancia = consulta.data;

  if (consulta.isPending) {
    return <Cargando filas={4} alto="h-24" />;
  }
  if (consulta.isError || !instancia) {
    return (
      <ErrorPanel
        mensaje={mensajeDeError(consulta.error ?? new Error("La instancia no existe"))}
        reintentar={() => consulta.refetch()}
      />
    );
  }

  const puedePausar =
    instancia.estado === "ACTIVA" || instancia.estado === "ESPERANDO";
  const puedeReanudar = instancia.estado === "BLOQUEADA";
  const puedeCancelar =
    instancia.estado === "ACTIVA" ||
    instancia.estado === "ESPERANDO" ||
    instancia.estado === "BLOQUEADA";

  return (
    <div className="space-y-espacio-6">
      <div className="flex flex-wrap items-center justify-between gap-espacio-4">
        <Boton variante="fantasma" onClick={alVolver}>
          ← Volver a las instancias
        </Boton>
        <Pastilla tono={tonoEstadoInstancia(instancia.estado)}>
          {instancia.estado}
        </Pastilla>
      </div>

      <Tarjeta>
        <CabeceraTarjeta
          titulo={`${instancia.codigoDefinicion} · v${instancia.numeroVersion}`}
          descripcion={`Iniciada ${instancia.alta ? formatearFecha(instancia.alta) : "—"}${
            instancia.fin ? ` · finalizada ${formatearFecha(instancia.fin)}` : ""
          }`}
        />
        {error ? (
          <div
            role="alert"
            className="mt-espacio-3 rounded-panel border border-rojo-borde bg-rojo-tenue px-espacio-4 py-espacio-3 text-pequeno text-rojo-alto"
          >
            {error}
          </div>
        ) : null}
        {puedePausar || puedeReanudar || puedeCancelar ? (
          <div className="mt-espacio-4 space-y-espacio-3">
            {pausar.isPending || cancelar.isPending ? null : (
              <Campo
                etiqueta="Motivo de la pausa o cancelación"
                placeholder="Ej.: falta documentación del proveedor"
                value={motivo}
                onChange={(evento) => setMotivo(evento.target.value)}
              />
            )}
            <div className="flex flex-wrap gap-espacio-2">
              {puedePausar ? (
                <Boton
                  variante="secundario"
                  cargando={pausar.isPending}
                  disabled={pausar.isPending || !motivo.trim()}
                  onClick={() => pausar.mutate()}
                >
                  Pausar
                </Boton>
              ) : null}
              {puedeReanudar ? (
                <Boton
                  variante="primario"
                  cargando={reanudar.isPending}
                  disabled={reanudar.isPending}
                  onClick={() => reanudar.mutate()}
                >
                  Reanudar
                </Boton>
              ) : null}
              {puedeCancelar ? (
                <Boton
                  variante="peligro"
                  cargando={cancelar.isPending}
                  disabled={cancelar.isPending}
                  onClick={() => {
                    if (window.confirm("¿Cancelar este proceso? Las tareas pendientes se cancelan.")) {
                      cancelar.mutate();
                    }
                  }}
                >
                  Cancelar proceso
                </Boton>
              ) : null}
            </div>
          </div>
        ) : null}
      </Tarjeta>

      <Tarjeta>
        <CabeceraTarjeta
          titulo="Tareas"
          descripcion="Las tareas humanas de este proceso. Las pendientes se pueden completar desde acá."
        />
        {instancia.tareas?.length ? (
          <ul className="mt-espacio-4 space-y-espacio-4">
            {instancia.tareas.map((tarea) => (
              <li key={tarea.id}>
                <TarjetaTarea tarea={tarea} />
              </li>
            ))}
          </ul>
        ) : (
          <p className="mt-espacio-3 text-pequeno text-tinta-suave">
            Este proceso no tiene tareas humanas.
          </p>
        )}
      </Tarjeta>

      <Tarjeta>
        <CabeceraTarjeta
          titulo="Línea de tiempo"
          descripcion="Todo lo que le pasó a esta instancia, en orden."
        />
        <LineaTiempo eventos={instancia.eventos ?? []} />
      </Tarjeta>
    </div>
  );
}

function LineaTiempo({ eventos }: { eventos: EventoInstancia[] }) {
  if (!eventos.length) {
    return (
      <p className="mt-espacio-3 text-pequeno text-tinta-suave">
        Todavía no hay eventos registrados.
      </p>
    );
  }
  const ordenados = [...eventos].sort((a, b) =>
    (a.alta ?? "").localeCompare(b.alta ?? ""),
  );
  return (
    <ol className="mt-espacio-4 space-y-espacio-3" aria-label="Eventos de la instancia">
      {ordenados.map((evento) => (
        <li
          key={evento.id}
          className="grid gap-espacio-1 rounded-control bg-lienzo px-espacio-4 py-espacio-3 md:grid-cols-[10rem_minmax(0,1fr)_auto]"
        >
          <span className="text-pequeno text-tinta-suave">
            {evento.alta ? formatearFecha(evento.alta) : "—"}
          </span>
          <span className="text-pequeno text-tinta">
            {textoAccion(evento.accion)}
            {evento.nodoId ? ` · ${evento.nodoId}` : ""}
          </span>
          <span className="text-pequeno text-tinta-suave">
            {evento.actor ?? "sistema"}
          </span>
        </li>
      ))}
    </ol>
  );
}

export function BandejaTareas() {
  const [filtro, setFiltro] = useState<string>("PENDIENTE");

  const consulta = useQuery({
    queryKey: ["tareas"],
    queryFn: listarTareas,
    refetchInterval: 15000,
  });

  const tareas = (consulta.data ?? []).filter((tarea) =>
    filtro === "TODAS" ? true : tarea.estado === filtro,
  );

  return (
    <>
      <div className="mb-espacio-4 flex flex-wrap items-end justify-between gap-espacio-4">
        <Selector
          etiqueta="Filtrar por estado"
          value={filtro}
          onChange={(evento) => setFiltro(evento.target.value)}
          className="max-w-56"
        >
          <option value="PENDIENTE">Pendientes</option>
          <option value="VENCIDA">Vencidas</option>
          <option value="COMPLETADA">Completadas</option>
          <option value="CANCELADA">Canceladas</option>
          <option value="TODAS">Todas</option>
        </Selector>
        <p className="text-pequeno text-tinta-suave">
          Las tareas de todos los procesos del espacio de trabajo.
        </p>
      </div>

      {consulta.isPending ? (
        <Cargando filas={3} alto="h-24" />
      ) : consulta.isError ? (
        <ErrorPanel
          mensaje={mensajeDeError(consulta.error)}
          reintentar={() => consulta.refetch()}
        />
      ) : tareas.length === 0 ? (
        <Vacio
          titulo={
            filtro === "PENDIENTE" || filtro === "VENCIDA"
              ? "No hay tareas para resolver"
              : "No hay tareas para mostrar"
          }
          detalle="Las tareas aparecen cuando un proceso en ejecución llega a un paso humano o de espera."
        />
      ) : (
        <ul aria-label="Tareas de proceso" className="space-y-espacio-4">
          {tareas.map((tarea) => (
            <li key={tarea.id}>
              <TarjetaTarea tarea={tarea} conInstancia />
            </li>
          ))}
        </ul>
      )}
    </>
  );
}

function TarjetaTarea({ tarea, conInstancia = false }: { tarea: TareaProceso; conInstancia?: boolean }) {
  const { sesion } = useSesion();
  const clienteConsultas = useQueryClient();
  const [abierta, setAbierta] = useState(false);
  const [decision, setDecision] = useState("APROBADO");
  const [motivo, setMotivo] = useState("");
  const [documentoId, setDocumentoId] = useState("");
  const [error, setError] = useState<string | null>(null);

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
        throw new Error("Ingresá un motivo de rechazo de hasta 512 caracteres");
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
    onError: (fallo) =>
      setError(fallo instanceof Error ? fallo.message : mensajeDeError(fallo)),
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
              {textoTipoTarea(tarea.tipoNodo)}
            </h3>
            <Pastilla tono={tonoEstadoTarea(tarea.estado)}>{tarea.estado}</Pastilla>
            {tarea.vencimiento ? (
              <Pastilla tono="neutro">
                vence {formatearFecha(tarea.vencimiento)}
              </Pastilla>
            ) : null}
          </div>
          <p className="mt-espacio-1 text-pequeno text-tinta-suave">
            {conInstancia ? `Instancia ${tarea.instanciaId} · ` : ""}
            paso {tarea.nodoId}
            {tarea.asignadoA ? ` · asignada a ${tarea.asignadoA}` : ""}
          </p>
          {tarea.completada ? (
            <p className="mt-espacio-1 text-pequeno text-tinta-suave">
              Completada {formatearFecha(tarea.completada)}
              {tarea.completadaPor ? ` por ${tarea.completadaPor}` : ""}
              {tarea.decision ? ` · ${tarea.decision}` : ""}
            </p>
          ) : null}
          {tarea.motivo ? (
            <p className="mt-espacio-1 text-pequeno text-tinta-suave">
              Motivo: {tarea.motivo}
            </p>
          ) : null}
          {enlaceToken ? (
            <p className="mt-espacio-2 break-all rounded-control bg-lienzo px-espacio-3 py-espacio-2 text-pequeno text-tinta-suave">
              Enlace para el externo: {enlaceToken}
            </p>
          ) : null}
        </div>
        {pendiente ? (
          <Boton
            variante={abierta ? "secundario" : "primario"}
            onClick={() => setAbierta((valor) => !valor)}
            aria-expanded={abierta}
          >
            {abierta ? "Cerrar" : "Completar"}
          </Boton>
        ) : null}
      </div>

      {abierta ? (
        <div className="mt-espacio-4 space-y-espacio-3 border-t border-borde pt-espacio-4">
          {tarea.tipoNodo === "SOLICITUD_DOCUMENTO" ? (
            <Campo
              etiqueta="Documento entregado (identificador)"
              placeholder="Ej.: FACTURA-0004-182"
              value={documentoId}
              onChange={(evento) => setDocumentoId(evento.target.value)}
            />
          ) : null}
          <div className="grid gap-espacio-3 md:grid-cols-2">
            <Selector
              etiqueta="Decisión"
              value={decision}
              onChange={(evento) => setDecision(evento.target.value)}
            >
              <option value="APROBADO">Aprobado</option>
              <option value="RECHAZADO">Rechazado</option>
            </Selector>
            <Campo
              etiqueta="Motivo (obligatorio si rechaza)"
              placeholder="Ej.: el importe no coincide"
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
              Confirmar
            </Boton>
          </div>
        </div>
      ) : null}
    </Tarjeta>
  );
}
