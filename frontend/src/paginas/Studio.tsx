import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Contenido, Encabezado } from "../componentes/Disposicion";
import { Cargando, ErrorPanel, Vacio } from "../componentes/Estados";
import {
  AreaTexto,
  Boton,
  CabeceraTarjeta,
  Campo,
  Pastilla,
  Selector,
  Tarjeta,
} from "../componentes/Interfaz";
import { IconoCheck } from "../componentes/Iconos";
import { formatearFecha } from "../utilidades/fechas";
import {
  actualizarGrafo,
  actualizarProceso,
  completarTarea,
  crearProceso,
  iniciarInstancia,
  listarProcesos,
  mensajeDeError,
  nuevaVersion,
  obtenerInstancia,
  obtenerProceso,
  publicarVersion,
  simularVersion,
  validarVersion,
} from "../api/procesos";
import type {
  GrafoProceso,
  InstanciaProceso,
  NodoProceso,
  Proceso,
  SimulacionResultado,
  VersionProceso,
} from "../api/procesos";
import { aplicarConfiguracion, avisosNodo } from "../utilidades/grafoProceso";
import { useSesion } from "../contextos/ProveedorSesion";
import { useIdioma } from "../contextos/ProveedorIdioma";
import {
  CanvasProceso,
  claveArista,
  type SeleccionCanvas,
} from "../componentes/CanvasProceso";

export function Studio() {
  const { t } = useIdioma();
  const { procesoId } = useParams();
  const navegar = useNavigate();

  if (procesoId) {
    return (
      <EstudioProceso
        procesoId={procesoId}
        alVolver={() => navegar("/studio")}
        alAbrirInstancia={(instanciaId) =>
          navegar(`/operacion/instancias/${instanciaId}`)
        }
      />
    );
  }

  return (
    <>
      <Encabezado
        titulo={t("studio.titulo")}
        descripcion={t("studio.descripcion")}
      />
      <Contenido>
        <ListaProcesos alAbrir={(id) => navegar(`/studio/${id}`)} />
      </Contenido>
    </>
  );
}

function ListaProcesos({ alAbrir }: { alAbrir: (id: string) => void }) {
  const { t } = useIdioma();
  const clienteConsultas = useQueryClient();
  const [error, setError] = useState<string | null>(null);
  const [editando, setEditando] = useState<string | null>(null);

  const consulta = useQuery({
    queryKey: ["procesos"],
    queryFn: listarProcesos,
  });

  const crear = useMutation({
    mutationFn: crearProceso,
    onSuccess: (proceso) => {
      setError(null);
      clienteConsultas.invalidateQueries({ queryKey: ["procesos"] });
      alAbrir(proceso.id);
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const procesos = consulta.data ?? [];

  return (
    <div>
      <div className="mb-espacio-4 flex flex-wrap items-end justify-between gap-espacio-4">
        <p className="max-w-xl text-pequeno text-tinta-suave">
          {t("procesos.bibliotecaDesc")}
        </p>
        <Boton
          variante="primario"
          cargando={crear.isPending}
          disabled={crear.isPending}
          onClick={() =>
            crear.mutate({
              codigo: `PROC-${Date.now().toString(36).toUpperCase()}`,
              familia: "GENERAL",
              nombre: t("procesos.procesoNuevo"),
            })
          }
        >
          {t("procesos.nuevoProceso")}
        </Boton>
      </div>
      {error ? (
        <div
          role="alert"
          className="aparecer mb-espacio-4 rounded-panel border border-rojo-borde bg-rojo-tenue px-espacio-4 py-espacio-3 text-pequeno text-rojo-alto"
        >
          {error}
        </div>
      ) : null}

      {consulta.isPending ? (
        <Cargando filas={3} alto="h-24" />
      ) : consulta.isError ? (
        <ErrorPanel
          mensaje={mensajeDeError(consulta.error)}
          error={consulta.error}
          reintentar={() => consulta.refetch()}
        />
      ) : procesos.length === 0 ? (
        <Vacio
          titulo={t("procesos.sinDefiniciones")}
          detalle={t("procesos.sinDefinicionesDetalle")}
        />
      ) : (
        <ul
          aria-label={t("procesos.listaDefiniciones")}
          className="space-y-espacio-4"
        >
          {procesos.map((proceso) => (
            <li key={proceso.id}>
              <Tarjeta>
                <div className="grid items-center gap-espacio-4 lg:grid-cols-[minmax(0,1fr)_auto]">
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-espacio-3">
                      <h2 className="break-words font-titulo text-titulo-panel text-tinta">
                        {proceso.nombre}
                      </h2>
                      <Pastilla
                        tono={proceso.slaHoras == null ? "neutro" : "violeta"}
                      >
                        {textoSla(proceso.slaHoras, t)}
                      </Pastilla>
                    </div>
                    <p className="mt-espacio-1 break-words text-pequeno text-tinta-suave">
                      {proceso.codigo} · {proceso.familia}
                    </p>
                    {proceso.alta ? (
                      <p className="mt-espacio-2 text-pequeno text-tinta-suave">
                        {t("procesos.creadoEl", {
                          fecha: formatearFecha(proceso.alta),
                        })}
                      </p>
                    ) : null}
                  </div>
                  <div className="flex flex-wrap items-center justify-between gap-espacio-4 lg:justify-end">
                    <VersionesProceso versiones={proceso.versiones} />
                    <Boton
                      variante="fantasma"
                      aria-expanded={editando === proceso.id}
                      onClick={() =>
                        setEditando((actual) =>
                          actual === proceso.id ? null : proceso.id,
                        )
                      }
                    >
                      {editando === proceso.id
                        ? t("procesos.cerrar")
                        : t("procesos.editar")}
                    </Boton>
                    <Boton
                      variante="primario"
                      onClick={() => alAbrir(proceso.id)}
                    >
                      {t("procesos.abrirEstudio")}
                    </Boton>
                  </div>
                </div>
                {editando === proceso.id ? (
                  <EditorProceso
                    proceso={proceso}
                    alCerrar={() => setEditando(null)}
                  />
                ) : null}
              </Tarjeta>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function slaNumerico(texto: string): number | undefined {
  const valor = Number(texto.trim());
  return texto.trim() && Number.isFinite(valor) && valor > 0
    ? valor
    : undefined;
}

function textoSla(
  horas: number | undefined,
  t: (ruta: string, params?: Record<string, string | number>) => string,
): string {
  if (horas == null) return t("procesos.sinSla");
  const valor = Number.isInteger(horas)
    ? horas
    : horas.toFixed(2).replace(/0$/, "");
  return t("procesos.sla", { horas: valor });
}

function EditorProceso({
  proceso,
  alCerrar,
}: {
  proceso: Proceso;
  alCerrar: () => void;
}) {
  const { t } = useIdioma();
  const clienteConsultas = useQueryClient();
  const [familia, setFamilia] = useState(proceso.familia);
  const [nombre, setNombre] = useState(proceso.nombre);
  const [sla, setSla] = useState(
    proceso.slaHoras == null ? "" : String(proceso.slaHoras),
  );
  const [error, setError] = useState<string | null>(null);

  const guardar = useMutation({
    mutationFn: () =>
      actualizarProceso(proceso.id, {
        familia: familia.trim(),
        nombre: nombre.trim(),
        descripcion: proceso.descripcion,
        etiquetas: proceso.etiquetas,
        slaHoras: slaNumerico(sla),
      }),
    onSuccess: () => {
      setError(null);
      clienteConsultas.invalidateQueries({ queryKey: ["procesos"] });
      alCerrar();
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  return (
    <div className="mt-espacio-4 border-t border-borde pt-espacio-4">
      {error ? (
        <div
          role="alert"
          className="mb-espacio-3 rounded-panel border border-rojo-borde bg-rojo-tenue px-espacio-3 py-espacio-2 text-pequeno text-rojo-alto"
        >
          {error}
        </div>
      ) : null}
      <div className="grid gap-espacio-4 md:grid-cols-3">
        <Campo
          etiqueta={t("procesos.familia")}
          value={familia}
          onChange={(evento) => setFamilia(evento.target.value)}
        />
        <Campo
          etiqueta={t("procesos.nombre")}
          value={nombre}
          onChange={(evento) => setNombre(evento.target.value)}
        />
        <Campo
          etiqueta={t("procesos.slaPlantilla")}
          type="number"
          min="0.01"
          step="0.25"
          placeholder={t("procesos.sinSla")}
          value={sla}
          onChange={(evento) => setSla(evento.target.value)}
          ayuda={t("procesos.slaEditorAyuda")}
        />
      </div>
      <div className="mt-espacio-4 flex justify-end gap-espacio-2">
        <Boton
          variante="secundario"
          onClick={alCerrar}
          disabled={guardar.isPending}
        >
          {t("comun.cancelar")}
        </Boton>
        <Boton
          variante="primario"
          cargando={guardar.isPending}
          disabled={guardar.isPending || !familia.trim() || !nombre.trim()}
          onClick={() => guardar.mutate()}
        >
          {t("comun.guardar")}
        </Boton>
      </div>
    </div>
  );
}

function VersionesProceso({ versiones }: { versiones: VersionProceso[] }) {
  const { t } = useIdioma();
  if (!versiones.length) {
    return <Pastilla tono="neutro">{t("procesos.sinVersiones")}</Pastilla>;
  }
  const publicada = versiones
    .filter((version) => version.estado === "PUBLICADA")
    .sort((a, b) => b.numero - a.numero)[0];
  const borrador = versiones.some((version) => version.estado === "BORRADOR");
  return (
    <div className="flex flex-wrap items-center gap-espacio-2">
      {publicada ? (
        <Pastilla tono="exito">
          {t("procesos.versionPublicada", { numero: publicada.numero })}
        </Pastilla>
      ) : null}
      {borrador ? (
        <Pastilla tono="alerta">{t("procesos.borradorEtiqueta")}</Pastilla>
      ) : null}
      {!publicada && !borrador ? (
        <Pastilla tono="neutro">{t("procesos.archivada")}</Pastilla>
      ) : null}
    </div>
  );
}

function EstudioProceso({
  procesoId,
  alVolver,
  alAbrirInstancia,
}: {
  procesoId: string;
  alVolver: () => void;
  alAbrirInstancia: (instanciaId: string) => void;
}) {
  const { sesion } = useSesion();
  const { t } = useIdioma();
  const clienteConsultas = useQueryClient();
  const [grafoTrabajo, setGrafoTrabajo] = useState<GrafoProceso | null>(null);
  const pilaDeshacer = useRef<GrafoProceso[]>([]);
  const pilaRehacer = useRef<GrafoProceso[]>([]);
  const rafagaEdicion = useRef({ continua: false, instante: 0 });
  const [historial, setHistorial] = useState({ deshacer: 0, rehacer: 0 });

  function aplicarGrafo(
    siguiente: GrafoProceso | ((actual: GrafoProceso) => GrafoProceso),
    continuo = false,
  ) {
    setGrafoTrabajo((actual) => {
      if (!actual) return actual;
      const nuevo =
        typeof siguiente === "function" ? siguiente(actual) : siguiente;
      if (nuevo === actual) return actual;
      const ahora = Date.now();
      const estructural =
        nuevo.nodos.length !== actual.nodos.length ||
        nuevo.aristas.length !== actual.aristas.length;
      const enRafaga =
        !estructural &&
        rafagaEdicion.current.continua === continuo &&
        ahora - rafagaEdicion.current.instante < 700;
      if (!enRafaga) {
        pilaDeshacer.current.push(actual);
        if (pilaDeshacer.current.length > 60) pilaDeshacer.current.shift();
        pilaRehacer.current = [];
        setHistorial({
          deshacer: pilaDeshacer.current.length,
          rehacer: 0,
        });
      }
      rafagaEdicion.current = { continua: continuo, instante: ahora };
      return nuevo;
    });
  }

  function deshacer() {
    const anterior = pilaDeshacer.current.pop();
    if (!anterior || !grafoTrabajo) return;
    pilaRehacer.current.push(grafoTrabajo);
    rafagaEdicion.current = { continua: false, instante: 0 };
    setGrafoTrabajo(anterior);
    setHistorial({
      deshacer: pilaDeshacer.current.length,
      rehacer: pilaRehacer.current.length,
    });
  }

  function rehacer() {
    const siguiente = pilaRehacer.current.pop();
    if (!siguiente || !grafoTrabajo) return;
    pilaDeshacer.current.push(grafoTrabajo);
    rafagaEdicion.current = { continua: false, instante: 0 };
    setGrafoTrabajo(siguiente);
    setHistorial({
      deshacer: pilaDeshacer.current.length,
      rehacer: pilaRehacer.current.length,
    });
  }

  function limpiarHistorial() {
    pilaDeshacer.current = [];
    pilaRehacer.current = [];
    rafagaEdicion.current = { continua: false, instante: 0 };
    setHistorial({ deshacer: 0, rehacer: 0 });
  }
  const [instanciaPrueba, setInstanciaPrueba] = useState<string | null>(null);
  const [simulacionAbierta, setSimulacionAbierta] = useState(false);
  const [editandoDatos, setEditandoDatos] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [detalles, setDetalles] = useState<string[]>([]);
  const [aviso, setAviso] = useState<string | null>(null);
  const [seleccion, setSeleccion] = useState<SeleccionCanvas | null>(null);

  const consulta = useQuery({
    queryKey: ["proceso", procesoId],
    queryFn: () => obtenerProceso(procesoId),
  });
  const proceso = consulta.data;
  const borrador = proceso?.versiones.find(
    (version) => version.estado === "BORRADOR",
  );
  const publicada = proceso?.versiones.find(
    (version) => version.estado === "PUBLICADA",
  );

  const versionCargada = useRef<{ id: string; grafo: string } | null>(null);
  const [conflicto, setConflicto] = useState<string | null>(null);
  const [base, setBase] = useState<{
    versionId: string;
    grafo: GrafoProceso;
  } | null>(null);
  const baseLista = !!borrador && base?.versionId === borrador.id;
  const hayCambios =
    baseLista &&
    !!grafoTrabajo &&
    JSON.stringify(grafoTrabajo) !== JSON.stringify(base.grafo);
  const advertenciaSalida = t("procesos.advertenciaSalida");

  useEffect(() => {
    if (!hayCambios) return;
    const alDescargar = (evento: BeforeUnloadEvent) => {
      evento.preventDefault();
      evento.returnValue = "";
    };
    const alNavegar = (evento: MouseEvent) => {
      const enlace =
        evento.target instanceof Element ? evento.target.closest("a") : null;
      if (
        !enlace ||
        enlace.target === "_blank" ||
        enlace.hasAttribute("download") ||
        evento.ctrlKey ||
        evento.metaKey ||
        evento.shiftKey ||
        evento.altKey
      )
        return;
      if (
        enlace.pathname === window.location.pathname &&
        enlace.origin === window.location.origin
      )
        return;
      if (!window.confirm(advertenciaSalida)) {
        evento.preventDefault();
        evento.stopPropagation();
      }
    };
    window.addEventListener("beforeunload", alDescargar);
    document.addEventListener("click", alNavegar, true);
    return () => {
      window.removeEventListener("beforeunload", alDescargar);
      document.removeEventListener("click", alNavegar, true);
    };
  }, [hayCambios]);

  function volver() {
    if (!hayCambios || window.confirm(advertenciaSalida)) alVolver();
  }
  useEffect(() => {
    if (!borrador) return;
    const firma = JSON.stringify(borrador.grafo);
    if (
      versionCargada.current?.id !== borrador.id ||
      versionCargada.current.grafo !== firma
    ) {
      if (hayCambios) {
        setConflicto(t("procesos.conflictoBorrador"));
        return;
      }
      versionCargada.current = { id: borrador.id, grafo: firma };
      setConflicto(null);
      const grafo = structuredClone(borrador.grafo);
      setBase({ versionId: borrador.id, grafo });
      limpiarHistorial();
      setGrafoTrabajo(structuredClone(grafo));
    }
  }, [borrador]);

  const refrescar = () =>
    clienteConsultas.invalidateQueries({ queryKey: ["proceso", procesoId] });

  const guardar = useMutation({
    mutationFn: (grafo: GrafoProceso) => {
      if (!baseLista || conflicto || consulta.isFetching)
        throw new Error(conflicto ?? t("procesos.borradorNoDisponible"));
      return actualizarGrafo(borrador!.id, grafo);
    },
    onSuccess: (version, grafoEnviado) => {
      versionCargada.current = {
        id: version.id,
        grafo: JSON.stringify(version.grafo),
      };
      const grafo = structuredClone(version.grafo);
      setBase({ versionId: version.id, grafo });
      limpiarHistorial();
      setGrafoTrabajo((actual) =>
        actual && JSON.stringify(actual) !== JSON.stringify(grafoEnviado)
          ? actual
          : structuredClone(grafo),
      );
      setError(null);
      setDetalles([]);
      setAviso(t("procesos.guardadoOk"));
      refrescar();
    },
    onError: (fallo) => {
      setAviso(null);
      setError(mensajeDeError(fallo));
      setDetalles(erroresDeValidacion(fallo));
    },
  });

  const publicar = useMutation({
    mutationFn: () => {
      if (!baseLista || conflicto || hayCambios || consulta.isFetching)
        throw new Error(conflicto ?? t("procesos.guardarAntesDePublicar"));
      return publicarVersion(borrador!.id);
    },
    onSuccess: () => {
      setError(null);
      setDetalles([]);
      setAviso(t("procesos.publicadoOk"));
      refrescar();
      clienteConsultas.invalidateQueries({ queryKey: ["procesos"] });
    },
    onError: (fallo) => {
      setAviso(null);
      setError(mensajeDeError(fallo));
      setDetalles(erroresDeValidacion(fallo));
    },
  });

  const validar = useMutation({
    mutationFn: () => {
      if (!baseLista || conflicto || hayCambios || consulta.isFetching)
        throw new Error(conflicto ?? t("procesos.guardarAntesDePublicar"));
      return validarVersion(borrador!.id);
    },
    onSuccess: () => {
      setError(null);
      setDetalles([]);
      setAviso(t("procesos.validacionOk"));
    },
    onError: (fallo) => {
      setAviso(null);
      setError(mensajeDeError(fallo));
      setDetalles(erroresDeValidacion(fallo));
    },
  });

  const simular = useMutation({
    mutationFn: (datos: Record<string, unknown>) => {
      if (!borrador) throw new Error(t("procesos.guardarAntesDePublicar"));
      return simularVersion(borrador.id, datos);
    },
    onError: (fallo) => {
      setAviso(null);
      setError(mensajeDeError(fallo));
    },
  });

  const clonar = useMutation({
    mutationFn: () => nuevaVersion(procesoId, t("procesos.notaNuevaVersion")),
    onSuccess: () => {
      setError(null);
      setAviso(null);
      refrescar();
      clienteConsultas.invalidateQueries({ queryKey: ["procesos"] });
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const probar = useMutation({
    mutationFn: () => iniciarInstancia(procesoId),
    onSuccess: (instancia) => {
      setError(null);
      setInstanciaPrueba(instancia.id);
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const consultaInstancia = useQuery({
    queryKey: ["instancia-prueba", instanciaPrueba],
    queryFn: () => obtenerInstancia(instanciaPrueba!),
    enabled: instanciaPrueba !== null,
    refetchInterval: (consulta) =>
      consulta.state.status !== "error" &&
      consulta.state.data &&
      ["CREADA", "ACTIVA", "ESPERANDO", "BLOQUEADA"].includes(
        consulta.state.data.estado,
      )
        ? 15_000
        : false,
    refetchIntervalInBackground: false,
  });

  const completar = useMutation({
    mutationFn: ({
      tareaId,
      decision,
      motivo,
    }: {
      tareaId: string;
      decision?: string;
      motivo?: string;
    }) => {
      if (
        decision === "RECHAZADO" &&
        (!motivo?.trim() || motivo.trim().length > 512)
      )
        throw new Error(t("operacion.motivoRechazoError"));
      return completarTarea(tareaId, {
        actor: sesion?.email ?? "estudio",
        decision,
        motivo,
      });
    },
    onSuccess: async () => {
      setError(null);
      await clienteConsultas.invalidateQueries({
        queryKey: ["instancia-prueba", instanciaPrueba],
      });
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  async function alCompletar(
    tareaId: string,
    decision?: string,
    motivo?: string,
  ) {
    try {
      await completar.mutateAsync({ tareaId, decision, motivo });
      return true;
    } catch {
      return false;
    }
  }

  const editandoBloqueado =
    !baseLista ||
    !!conflicto ||
    consulta.isFetching ||
    guardar.isPending ||
    publicar.isPending;

  function editarNodo(id: string, siguiente: NodoProceso) {
    aplicarGrafo((actual) =>
      actual
        ? {
            ...actual,
            nodos: actual.nodos.map((nodo) =>
              nodo.id === id ? siguiente : nodo,
            ),
          }
        : actual,
    );
  }

  function duplicarNodo(id: string) {
    aplicarGrafo((actual) => {
      const original = actual?.nodos.find((nodo) => nodo.id === id);
      if (!actual || !original) return actual;
      const copia: NodoProceso = {
        ...structuredClone(original),
        id:
          "paso-" +
          Math.random().toString(36).slice(2, 8) +
          Date.now().toString(36),
        nombre: `${original.nombre ?? original.tipo} ${t("canvas.copia")}`,
      };
      const posicion = copia.configuracion?.posicion as
        { x?: number; y?: number } | undefined;
      if (posicion) {
        copia.configuracion = {
          ...copia.configuracion,
          posicion: { x: (posicion.x ?? 0) + 40, y: (posicion.y ?? 0) + 80 },
        };
      }
      return { ...actual, nodos: [...actual.nodos, copia] };
    });
    setAviso(t("canvas.pasoDuplicado"));
  }

  function eliminarNodo(id: string) {
    eliminarNodos([id]);
  }

  function eliminarNodos(ids: string[]) {
    const eliminables = ids.filter((id) => {
      const nodo = grafoTrabajo?.nodos.find((actual) => actual.id === id);
      return nodo && nodo.tipo !== "INICIO" && nodo.tipo !== "FIN";
    });
    if (!eliminables.length) return;
    if (!window.confirm(t("canvas.confirmarEliminarNodo"))) return;
    const conjunto = new Set(eliminables);
    aplicarGrafo((actual) =>
      actual
        ? {
            ...actual,
            nodos: actual.nodos.filter((nodo) => !conjunto.has(nodo.id)),
            aristas: actual.aristas.filter(
              (arista) =>
                !conjunto.has(arista.origen) && !conjunto.has(arista.destino),
            ),
          }
        : actual,
    );
    setSeleccion(null);
  }

  function editarArista(clave: string, condicion?: string) {
    aplicarGrafo((actual) =>
      actual
        ? {
            ...actual,
            aristas: actual.aristas.map((arista) =>
              claveArista(arista) === clave
                ? { ...arista, condicion: condicion?.trim() || undefined }
                : arista,
            ),
          }
        : actual,
    );
  }

  function eliminarArista(clave: string) {
    if (!window.confirm(t("canvas.confirmarEliminarConexion"))) return;
    aplicarGrafo((actual) =>
      actual
        ? {
            ...actual,
            aristas: actual.aristas.filter(
              (arista) => claveArista(arista) !== clave,
            ),
          }
        : actual,
    );
    setSeleccion(null);
  }

  useEffect(() => {
    if (!seleccion || editandoBloqueado) return;
    const alTecla = (evento: KeyboardEvent) => {
      if (evento.key !== "Delete" && evento.key !== "Backspace") return;
      const destino = evento.target as HTMLElement | null;
      if (
        destino &&
        (["INPUT", "TEXTAREA", "SELECT"].includes(destino.tagName) ||
          destino.isContentEditable)
      )
        return;
      evento.preventDefault();
      if (seleccion.tipo === "arista") {
        eliminarArista(seleccion.id);
        return;
      }
      if (seleccion.tipo === "nodos") {
        eliminarNodos(seleccion.ids ?? []);
        return;
      }
      eliminarNodos([seleccion.id]);
    };
    window.addEventListener("keydown", alTecla);
    return () => window.removeEventListener("keydown", alTecla);
  }, [seleccion, editandoBloqueado, grafoTrabajo]);

  useEffect(() => {
    if (editandoBloqueado) return;
    const alTecla = (evento: KeyboardEvent) => {
      if (!(evento.ctrlKey || evento.metaKey)) return;
      const destino = evento.target as HTMLElement | null;
      if (destino && destino.isContentEditable) return;
      const tecla = evento.key.toLowerCase();
      if (tecla === "z" && evento.shiftKey) {
        evento.preventDefault();
        rehacer();
      } else if (tecla === "z") {
        evento.preventDefault();
        deshacer();
      } else if (tecla === "y") {
        evento.preventDefault();
        rehacer();
      }
    };
    window.addEventListener("keydown", alTecla);
    return () => window.removeEventListener("keydown", alTecla);
  });

  if (consulta.isPending) {
    return (
      <>
        <Encabezado
          titulo={t("procesos.studioTitulo")}
          descripcion={t("procesos.studioCargando")}
        />
        <Contenido>
          <Cargando filas={4} alto="h-24" />
        </Contenido>
      </>
    );
  }
  if (consulta.isError || !proceso) {
    return (
      <>
        <Encabezado
          titulo={t("procesos.studioTitulo")}
          acciones={
            <Boton variante="fantasma" onClick={volver}>
              {t("procesos.volver")}
            </Boton>
          }
        />
        <Contenido>
          <ErrorPanel
            contexto={t("procesos.errorEditor")}
            mensaje={mensajeDeError(consulta.error)}
            error={consulta.error}
            reintentar={() => consulta.refetch()}
          />
        </Contenido>
      </>
    );
  }

  return (
    <>
      <Encabezado
        titulo={proceso.nombre}
        descripcion={`${proceso.codigo} · ${proceso.familia}`}
        acciones={
          <div className="flex flex-wrap gap-espacio-2">
            <Boton variante="fantasma" onClick={volver}>
              {t("procesos.volver")}
            </Boton>
            <Boton
              variante="secundario"
              aria-expanded={editandoDatos}
              onClick={() => setEditandoDatos((valor) => !valor)}
            >
              {t("procesos.editar")}
            </Boton>
            {publicada ? (
              <Boton
                variante="secundario"
                cargando={probar.isPending}
                disabled={probar.isPending}
                onClick={() => probar.mutate()}
              >
                {t("procesos.probarProceso")}
              </Boton>
            ) : null}
          </div>
        }
      />
      <Contenido>
        {editandoDatos ? (
          <Tarjeta className="mb-espacio-6">
            <CabeceraTarjeta titulo={t("procesos.editar")} />
            <EditorProceso
              proceso={proceso}
              alCerrar={() => {
                setEditandoDatos(false);
                refrescar();
              }}
            />
          </Tarjeta>
        ) : null}
        {publicada ? (
          <p
            role="note"
            className="mb-espacio-6 rounded-panel border border-informacion-borde bg-informacion-tenue p-espacio-4 text-pequeno text-tinta-media"
          >
            {t("procesos.probarNota", { numero: publicada.numero })}
          </p>
        ) : null}
        {aviso ? (
          <div
            role="status"
            aria-atomic="true"
            className="aparecer mb-espacio-4 flex items-center gap-espacio-2 rounded-panel border border-exito-borde bg-exito-tenue px-espacio-4 py-espacio-3 text-pequeno text-exito-texto"
          >
            <span aria-hidden="true">
              <IconoCheck tamano={16} />
            </span>
            {aviso}
          </div>
        ) : null}
        {error ? (
          <div
            role="alert"
            className="aparecer mb-espacio-4 rounded-panel border border-rojo-borde bg-rojo-tenue px-espacio-4 py-espacio-3 text-pequeno text-rojo-alto"
          >
            <p>{error}</p>
            {detalles.length ? (
              <ul className="mt-1.5 list-disc pl-5 text-xs">
                {detalles.map((detalle) => (
                  <li key={detalle}>{detalle}</li>
                ))}
              </ul>
            ) : null}
          </div>
        ) : null}

        {!borrador ? (
          <>
            <Tarjeta className="mb-5">
              <CabeceraTarjeta
                titulo={t("procesos.sinBorrador")}
                descripcion={t("procesos.sinBorradorDesc")}
              />
              <div className="flex justify-end">
                <Boton
                  variante="primario"
                  cargando={clonar.isPending}
                  disabled={clonar.isPending}
                  onClick={() => clonar.mutate()}
                >
                  {t("procesos.nuevaVersion")}
                </Boton>
              </div>
            </Tarjeta>
            {publicada ? (
              <Tarjeta className="mb-espacio-6">
                <CabeceraTarjeta
                  titulo={t("procesos.recorridoPublicado", {
                    numero: publicada.numero,
                  })}
                  descripcion={t("procesos.recorridoPublicadoDesc")}
                />
                <div className="mt-espacio-5">
                  <CanvasProceso
                    grafo={publicada.grafo}
                    alCambiar={() => {}}
                    seleccion={seleccion}
                    alSeleccionar={setSeleccion}
                    deshabilitado
                    soloLectura
                  />
                </div>
              </Tarjeta>
            ) : null}
          </>
        ) : (
          <Tarjeta
            className="mb-espacio-6"
            padding="p-espacio-4 sm:p-espacio-6"
          >
            <CabeceraTarjeta
              titulo={t("procesos.borradorVersion", {
                numero: borrador.numero,
              })}
              descripcion={t("procesos.borradorDesc")}
              acciones={
                <Pastilla tono={hayCambios ? "alerta" : "neutro"}>
                  {hayCambios
                    ? t("procesos.sinGuardar")
                    : t("procesos.borradorEtiqueta")}
                </Pastilla>
              }
            />
            {conflicto ? (
              <div className="mt-espacio-4">
                <ErrorPanel
                  titulo={t("procesos.versionNoEditable")}
                  mensaje={conflicto}
                />
              </div>
            ) : null}
            {grafoTrabajo ? (
              <div className="mt-espacio-5 grid items-start gap-espacio-4 xl:grid-cols-[minmax(0,1fr)_21rem]">
                <CanvasProceso
                  grafo={grafoTrabajo}
                  alCambiar={aplicarGrafo}
                  seleccion={seleccion}
                  alSeleccionar={setSeleccion}
                  deshabilitado={editandoBloqueado}
                  alDeshacer={deshacer}
                  alRehacer={rehacer}
                  puedeDeshacer={historial.deshacer > 0}
                  puedeRehacer={historial.rehacer > 0}
                  alDuplicarNodo={duplicarNodo}
                  alEliminarNodo={eliminarNodo}
                  alEliminarArista={eliminarArista}
                />
                <PanelSeleccion
                  seleccion={seleccion}
                  grafo={grafoTrabajo}
                  alCambiarNodo={editarNodo}
                  alDuplicarNodo={duplicarNodo}
                  alEliminarNodo={eliminarNodo}
                  alCambiarArista={editarArista}
                  alEliminarArista={eliminarArista}
                  deshabilitado={editandoBloqueado}
                />
              </div>
            ) : null}
            {hayCambios ? (
              <p
                role="status"
                className="my-espacio-4 rounded-panel border border-alerta-borde bg-alerta-tenue p-espacio-3 text-pequeno text-alerta-texto"
              >
                {t("procesos.hayCambios")}
              </p>
            ) : null}
            <div className="mt-espacio-6 grid gap-espacio-3 border-t border-borde pt-espacio-5 sm:flex sm:justify-end">
              <Boton
                variante="secundario"
                cargando={guardar.isPending}
                disabled={editandoBloqueado || !grafoTrabajo}
                onClick={() => guardar.mutate(grafoTrabajo!)}
              >
                {t("procesos.guardarBorrador")}
              </Boton>
              <Boton
                variante="secundario"
                cargando={validar.isPending}
                disabled={
                  editandoBloqueado ||
                  validar.isPending ||
                  hayCambios ||
                  !grafoTrabajo
                }
                onClick={() => validar.mutate()}
              >
                {t("procesos.validar")}
              </Boton>
              <Boton
                variante="secundario"
                disabled={editandoBloqueado || hayCambios || !grafoTrabajo}
                onClick={() => {
                  setError(null);
                  setSimulacionAbierta((abierta) => !abierta);
                }}
              >
                {t("procesos.simular")}
              </Boton>
              <Boton
                variante="primario"
                cargando={publicar.isPending}
                disabled={
                  editandoBloqueado ||
                  hayCambios ||
                  !grafoTrabajo ||
                  grafoTrabajo.nodos.filter(
                    (nodo) => nodo.tipo !== "INICIO" && nodo.tipo !== "FIN",
                  ).length === 0
                }
                onClick={() => publicar.mutate()}
              >
                {t("procesos.publicarVersion", {
                  numero: borrador.numero,
                })}
              </Boton>
            </div>
          </Tarjeta>
        )}

        {simulacionAbierta ? (
          <PanelSimulacion
            alSimular={(datos) => simular.mutate(datos)}
            resultado={simular.data}
            simulando={simular.isPending}
            alCerrar={() => {
              setSimulacionAbierta(false);
              simular.reset();
            }}
          />
        ) : null}

        {publicada ? (
          <Tarjeta>
            <CabeceraTarjeta
              titulo={t("procesos.versionPublicadaTitulo", {
                numero: publicada.numero,
              })}
              descripcion={t("procesos.versionPublicadaDesc")}
              acciones={
                <Pastilla tono="exito">{t("procesos.publicada")}</Pastilla>
              }
            />
            <dl className="mt-espacio-4 grid gap-espacio-4 sm:grid-cols-3">
              <div>
                <dt className="text-micro uppercase tracking-wide text-tinta-suave">
                  {t("procesos.publicada")}
                </dt>
                <dd className="mt-espacio-1 text-pequeno text-tinta">
                  {publicada.publicada
                    ? formatearFecha(publicada.publicada)
                    : "—"}
                </dd>
              </div>
              <div>
                <dt className="text-micro uppercase tracking-wide text-tinta-suave">
                  {t("procesos.pasosDelRecorrido")}
                </dt>
                <dd className="mt-espacio-1 text-pequeno text-tinta tabular-nums">
                  {
                    (publicada.grafo.nodos ?? []).filter(
                      (nodo) => nodo.tipo !== "INICIO" && nodo.tipo !== "FIN",
                    ).length
                  }
                </dd>
              </div>
              <div>
                <dt className="text-micro uppercase tracking-wide text-tinta-suave">
                  {t("procesos.notaVersion")}
                </dt>
                <dd className="mt-espacio-1 text-pequeno text-tinta [overflow-wrap:anywhere]">
                  {publicada.cambios?.trim() || t("procesos.sinNota")}
                </dd>
              </div>
            </dl>
            {publicada.hash ? (
              <details className="mt-espacio-4 border-t border-borde pt-espacio-3">
                <summary className="cursor-pointer text-pequeno text-tinta-suave">
                  {t("procesos.huella")}
                </summary>
                <p className="mt-espacio-2 text-pequeno text-tinta-suave">
                  {t("procesos.huellaDesc")}
                </p>
                <p className="mt-espacio-2 break-all rounded-control bg-lienzo p-espacio-3 font-codigo text-codigo text-tinta-suave">
                  {publicada.hash}
                </p>
              </details>
            ) : null}
          </Tarjeta>
        ) : null}

        {instanciaPrueba ? (
          consultaInstancia.isError ? (
            <ErrorPanel
              mensaje={mensajeDeError(consultaInstancia.error)}
              error={consultaInstancia.error}
              reintentar={() => consultaInstancia.refetch()}
            />
          ) : (
            <PanelPrueba
              key={instanciaPrueba}
              instancia={consultaInstancia.data}
              alCompletar={alCompletar}
              completando={completar.isPending}
              alAbrir={alAbrirInstancia}
            />
          )
        ) : null}
      </Contenido>
    </>
  );
}

const TIPOS_CON_RESPONSABLE = new Set([
  "SOLICITUD_DOCUMENTO",
  "FORMULARIO",
  "VALIDACION_IA",
  "REVISION_HUMANA",
  "TAREA_EXTERNA",
  "FIRMA",
]);

const TIPOS_CON_SLA = new Set([...TIPOS_CON_RESPONSABLE, "TEMPORIZADOR"]);

function ConfiguracionNodo({
  nodo,
  alCambiar,
}: {
  nodo: NodoProceso;
  alCambiar: (siguiente: NodoProceso) => void;
}) {
  const { t } = useIdioma();
  const configuracion = nodo.configuracion ?? {};
  const texto = (clave: string) =>
    typeof configuracion[clave] === "string"
      ? (configuracion[clave] as string)
      : "";
  const numero = (clave: string) =>
    typeof configuracion[clave] === "number"
      ? (configuracion[clave] as number)
      : "";
  const cambiar = (clave: string, valor: unknown) =>
    alCambiar(aplicarConfiguracion(nodo, clave, valor));

  return (
    <div className="grid gap-espacio-4">
      <Campo
        etiqueta={t("procesos.nombreDelPaso")}
        value={nodo.nombre ?? ""}
        onChange={(evento) =>
          alCambiar({ ...nodo, nombre: evento.target.value })
        }
      />
      {nodo.tipo === "INICIO" ? (
        <>
          <Campo
            etiqueta={t("canvas.programadoMinutos")}
            type="number"
            min={1}
            placeholder="60"
            ayuda={t("canvas.programadoMinutosAyuda")}
            value={numero("programadoMinutos")}
            onChange={(evento) =>
              cambiar(
                "programadoMinutos",
                evento.target.value ? Number(evento.target.value) : undefined,
              )
            }
          />
          <Campo
            etiqueta={t("canvas.eventoInicio")}
            placeholder="documento.recibido"
            ayuda={t("canvas.eventoInicioAyuda")}
            value={texto("evento")}
            onChange={(evento) => cambiar("evento", evento.target.value)}
          />
        </>
      ) : null}
      {nodo.tipo === "SOLICITUD_DOCUMENTO" || nodo.tipo === "FIRMA" ? (
        <Campo
          etiqueta={t("procesos.tipoDocumento")}
          placeholder="FACTURA_COMERCIAL"
          ayuda={
            nodo.tipo === "FIRMA"
              ? t("canvas.tipoDocumentoFirmaAyuda")
              : t("canvas.tipoDocumentoAyuda")
          }
          value={texto("tipoDocumento")}
          onChange={(evento) => cambiar("tipoDocumento", evento.target.value)}
        />
      ) : null}
      {nodo.tipo === "PARALELO" ? (
        <p
          role="note"
          className="rounded-control bg-lienzo p-espacio-3 text-pequeno text-tinta-suave"
        >
          {t("canvas.paraleloAyuda")}
        </p>
      ) : null}
      {nodo.tipo === "UNION" ? (
        <p
          role="note"
          className="rounded-control bg-lienzo p-espacio-3 text-pequeno text-tinta-suave"
        >
          {t("canvas.unionAyuda")}
        </p>
      ) : null}
      {nodo.tipo === "SUBPROCESO" ? (
        <Campo
          etiqueta={t("procesos.codigoSubproceso")}
          placeholder="COMEX-OV-DG"
          ayuda={t("canvas.subprocesoAyuda")}
          value={texto("subprocesoCodigo")}
          onChange={(evento) =>
            cambiar("subprocesoCodigo", evento.target.value.toUpperCase())
          }
        />
      ) : null}
      {nodo.tipo === "VALIDACION_IA" ? (
        <AreaTexto
          etiqueta={t("canvas.condicionesIa")}
          placeholder={"monto_total > 0\nmoneda = USD"}
          ayuda={t("canvas.condicionesIaAyuda")}
          rows={3}
          value={
            Array.isArray(configuracion.condiciones)
              ? (configuracion.condiciones as unknown[]).map(String).join("\n")
              : texto("condicion")
          }
          onChange={(evento) =>
            cambiar(
              "condiciones",
              evento.target.value
                .split("\n")
                .map((linea) => linea.trim())
                .filter(Boolean),
            )
          }
        />
      ) : null}
      {nodo.tipo === "NOTIFICACION" ? (
        <AreaTexto
          etiqueta={t("canvas.mensajeNotificacion")}
          placeholder={t("canvas.mensajePlaceholder")}
          rows={3}
          value={texto("mensaje")}
          onChange={(evento) => cambiar("mensaje", evento.target.value)}
        />
      ) : null}
      {nodo.tipo === "TEMPORIZADOR" ? (
        <Campo
          etiqueta={t("canvas.duracionHoras")}
          type="number"
          min={0}
          step="0.25"
          ayuda={t("canvas.duracionAyuda")}
          value={numero("horas")}
          onChange={(evento) =>
            cambiar(
              "horas",
              evento.target.value ? Number(evento.target.value) : undefined,
            )
          }
        />
      ) : null}
      {nodo.tipo === "ACCION_API" ? (
        <>
          <Campo
            etiqueta={t("canvas.urlApi")}
            placeholder="https://erp.example.com/api/ordenes"
            value={texto("url")}
            onChange={(evento) => cambiar("url", evento.target.value)}
          />
          <Selector
            etiqueta={t("canvas.metodoApi")}
            value={texto("metodo") || "POST"}
            onChange={(evento) => cambiar("metodo", evento.target.value)}
          >
            {["GET", "POST", "PUT", "PATCH", "DELETE"].map((metodo) => (
              <option key={metodo} value={metodo}>
                {metodo}
              </option>
            ))}
          </Selector>
          <AreaTexto
            etiqueta={t("canvas.cuerpoApi")}
            placeholder='{"orden": "{{datos.orden}}"}'
            ayuda={t("canvas.cuerpoApiAyuda")}
            rows={3}
            value={texto("cuerpo")}
            onChange={(evento) => cambiar("cuerpo", evento.target.value)}
          />
          <Campo
            etiqueta={t("canvas.campoRespuesta")}
            placeholder="respuesta.erp"
            ayuda={t("canvas.campoRespuestaAyuda")}
            value={texto("campoRespuesta")}
            onChange={(evento) =>
              cambiar("campoRespuesta", evento.target.value)
            }
          />
        </>
      ) : null}
      {nodo.tipo === "TAREA_EXTERNA" ? (
        <>
          <Campo
            etiqueta={t("canvas.expiracionEnlace")}
            type="number"
            min={1}
            ayuda={t("canvas.expiracionEnlaceAyuda")}
            value={numero("expiracionEnlaceHoras")}
            onChange={(evento) =>
              cambiar(
                "expiracionEnlaceHoras",
                evento.target.value ? Number(evento.target.value) : undefined,
              )
            }
          />
          <Campo
            etiqueta={t("procesos.tipoDocumento")}
            placeholder="FACTURA_COMERCIAL"
            ayuda={t("canvas.tipoDocumentoExternoAyuda")}
            value={texto("tipoDocumento")}
            onChange={(evento) => cambiar("tipoDocumento", evento.target.value)}
          />
        </>
      ) : null}
      {nodo.tipo === "DECISION" ? (
        <p
          role="note"
          className="rounded-control bg-lienzo p-espacio-3 text-pequeno text-tinta-suave"
        >
          {t("canvas.decisionAyuda")}
        </p>
      ) : null}
      {TIPOS_CON_RESPONSABLE.has(nodo.tipo) ? (
        <Campo
          etiqueta={t("procesos.responsablePaso")}
          placeholder={t("procesos.responsablePlaceholder")}
          value={texto("asignadoA")}
          onChange={(evento) => cambiar("asignadoA", evento.target.value)}
        />
      ) : null}
      {TIPOS_CON_SLA.has(nodo.tipo) ? (
        <Campo
          etiqueta={t("procesos.slaHoras")}
          type="number"
          min={1}
          value={numero("slaHoras")}
          onChange={(evento) =>
            cambiar(
              "slaHoras",
              evento.target.value ? Number(evento.target.value) : undefined,
            )
          }
        />
      ) : null}
    </div>
  );
}

function PanelPrueba({
  instancia,
  alCompletar,
  completando,
  alAbrir,
}: {
  instancia?: InstanciaProceso;
  alCompletar: (
    tareaId: string,
    decision?: string,
    motivo?: string,
  ) => Promise<boolean>;
  completando: boolean;
  alAbrir: (instanciaId: string) => void;
}) {
  const { t } = useIdioma();
  const [tareaRechazo, setTareaRechazo] = useState<string | null>(null);
  const [motivo, setMotivo] = useState("");
  const enviando = useRef(false);

  async function enviar(tareaId: string, decision?: string) {
    if (enviando.current || completando) return;
    const motivoFinal = decision === "RECHAZADO" ? motivo.trim() : undefined;
    if (decision === "RECHAZADO" && (!motivoFinal || motivoFinal.length > 512))
      return;
    enviando.current = true;
    try {
      if (await alCompletar(tareaId, decision, motivoFinal)) {
        setTareaRechazo(null);
        setMotivo("");
      }
    } finally {
      enviando.current = false;
    }
  }

  if (!instancia) {
    return (
      <Tarjeta className="mt-espacio-6">
        <CabeceraTarjeta titulo={t("procesos.cargandoPrueba")} />
        <Cargando filas={2} />
      </Tarjeta>
    );
  }
  const finalizada =
    instancia.estado === "COMPLETADA" || instancia.estado === "CANCELADA";
  const permiteCompletar =
    instancia.estado === "ACTIVA" || instancia.estado === "ESPERANDO";
  const pendientes = instancia.tareas.filter(
    (tarea) => tarea.estado === "PENDIENTE" || tarea.estado === "VENCIDA",
  );
  return (
    <Tarjeta className="mt-espacio-6" padding="p-espacio-4 sm:p-espacio-6">
      <div className="flex flex-wrap items-start justify-between gap-espacio-4">
        <CabeceraTarjeta
          titulo={t("procesos.pruebaTitulo", {
            id: instancia.id.slice(0, 8),
          })}
          descripcion={t("procesos.pruebaEstado", {
            estado: t(`estadoInstancia.${instancia.estado}`),
            version: instancia.numeroVersion,
          })}
        />
        <Boton variante="fantasma" onClick={() => alAbrir(instancia.id)}>
          {t("procesos.abrirEnInstancias")}
        </Boton>
      </div>
      {finalizada ? (
        <p
          role="status"
          className="mt-espacio-4 rounded-panel border border-borde bg-lienzo p-espacio-4 text-pequeno text-tinta-media"
        >
          {t("procesos.instanciaTermino", {
            estado: t(`estadoInstancia.${instancia.estado}`).toLowerCase(),
          })}
        </p>
      ) : (
        <>
          <p
            role="status"
            className="mt-espacio-4 text-pequeno text-tinta-media"
          >
            {instancia.estado === "BLOQUEADA"
              ? t("procesos.instanciaBloqueada")
              : instancia.estado === "CREADA"
                ? t("procesos.instanciaCreada")
                : instancia.estado === "ESPERANDO"
                  ? t("procesos.instanciaEsperando")
                  : t("procesos.instanciaActiva")}
          </p>
          {pendientes.length ? (
            <ul
              aria-label={t("procesos.tareasPrueba")}
              className="mt-espacio-5 space-y-espacio-4"
            >
              {pendientes.map((tarea) => (
                <li
                  key={tarea.id}
                  className="rounded-panel border border-borde bg-lienzo p-espacio-4"
                >
                  <div className="flex flex-wrap items-center gap-espacio-3">
                    <div className="flex min-w-0 basis-full flex-wrap gap-espacio-2">
                      <Pastilla tono="violeta">
                        {t(`tipoNodo.${tarea.tipoNodo}`)}
                      </Pastilla>
                      <Pastilla
                        tono={tarea.estado === "VENCIDA" ? "alerta" : "neutro"}
                      >
                        {t(`estadoTarea.${tarea.estado}`)}
                      </Pastilla>
                    </div>
                    <h3 className="min-w-0 basis-full break-words font-titulo text-titulo-panel text-tinta sm:flex-1 sm:basis-auto">
                      {tarea.nodoId}
                    </h3>
                    {permiteCompletar ? (
                      tarea.tipoNodo === "REVISION_HUMANA" ? (
                        <>
                          <Boton
                            variante="primario"
                            tamano="sm"
                            disabled={completando}
                            onClick={() => enviar(tarea.id, "APROBADO")}
                          >
                            {t("operacion.aprobar")}
                          </Boton>
                          <Boton
                            variante="peligro"
                            tamano="sm"
                            disabled={completando}
                            onClick={() => {
                              setTareaRechazo(tarea.id);
                              setMotivo("");
                            }}
                          >
                            {t("operacion.rechazar")}
                          </Boton>
                        </>
                      ) : (
                        <Boton
                          variante="primario"
                          tamano="sm"
                          disabled={completando}
                          onClick={() => enviar(tarea.id)}
                        >
                          {t("operacion.completar")}
                        </Boton>
                      )
                    ) : null}
                  </div>
                  {permiteCompletar && tareaRechazo === tarea.id ? (
                    <form
                      className="mt-espacio-5 space-y-espacio-4 border-t border-borde pt-espacio-4"
                      onSubmit={(evento) => {
                        evento.preventDefault();
                        void enviar(tarea.id, "RECHAZADO");
                      }}
                    >
                      <Campo
                        etiqueta={t("procesos.motivoRechazo")}
                        value={motivo}
                        onChange={(evento) => setMotivo(evento.target.value)}
                        required
                        maxLength={512}
                        disabled={completando}
                        autoFocus
                      />
                      <div className="flex flex-wrap gap-espacio-2">
                        <Boton
                          type="submit"
                          variante="peligro"
                          tamano="sm"
                          cargando={completando}
                          disabled={
                            !motivo.trim() || motivo.trim().length > 512
                          }
                        >
                          {t("procesos.confirmarRechazo")}
                        </Boton>
                        <Boton
                          type="button"
                          tamano="sm"
                          variante="fantasma"
                          disabled={completando}
                          onClick={() => {
                            setTareaRechazo(null);
                            setMotivo("");
                          }}
                        >
                          {t("comun.cancelar")}
                        </Boton>
                      </div>
                    </form>
                  ) : null}
                </li>
              ))}
            </ul>
          ) : (
            <p className="mt-2 text-sm text-tinta-suave">
              {t("procesos.sinTareasPendientes")}
            </p>
          )}
          {completando ? (
            <p
              role="status"
              aria-busy="true"
              className="mt-2 text-sm text-tinta-suave"
            >
              {t("procesos.enviandoDecision")}
            </p>
          ) : null}
        </>
      )}
    </Tarjeta>
  );
}

function PanelSeleccion({
  seleccion,
  grafo,
  alCambiarNodo,
  alDuplicarNodo,
  alEliminarNodo,
  alCambiarArista,
  alEliminarArista,
  deshabilitado,
}: {
  seleccion: SeleccionCanvas | null;
  grafo: GrafoProceso;
  alCambiarNodo: (id: string, siguiente: NodoProceso) => void;
  alDuplicarNodo: (id: string) => void;
  alEliminarNodo: (id: string) => void;
  alCambiarArista: (clave: string, condicion?: string) => void;
  alEliminarArista: (clave: string) => void;
  deshabilitado: boolean;
}) {
  const { t } = useIdioma();

  const nodo =
    seleccion?.tipo === "nodo"
      ? grafo.nodos.find((actual) => actual.id === seleccion.id)
      : undefined;
  const arista =
    seleccion?.tipo === "arista"
      ? grafo.aristas.find((actual) => claveArista(actual) === seleccion.id)
      : undefined;

  if (seleccion?.tipo === "nodos") {
    return (
      <Tarjeta padding="p-0" className="overflow-hidden">
        <div className="p-espacio-4 sm:p-espacio-5">
          <CabeceraTarjeta
            titulo={t("canvas.seleccionMultiple")}
            descripcion={t("canvas.seleccionMultipleDetalle", {
              cantidad: seleccion.ids?.length ?? 0,
            })}
          />
          <p className="mt-espacio-3 text-pequeno text-tinta-suave">
            {t("canvas.seleccionMultipleAyuda")}
          </p>
        </div>
      </Tarjeta>
    );
  }

  if (nodo) {
    const extremo = nodo.tipo === "INICIO" || nodo.tipo === "FIN";
    const avisos = avisosNodo(nodo, grafo);
    return (
      <Tarjeta padding="p-0" className="overflow-hidden">
        <div className="p-espacio-4 sm:p-espacio-5">
          <CabeceraTarjeta
            titulo={t("canvas.propiedadesPaso")}
            descripcion={t(`tipoNodo.${nodo.tipo}`)}
          />
          {avisos.length ? (
            <ul
              role="alert"
              className="mt-espacio-4 space-y-espacio-1 rounded-control border border-alerta-borde bg-alerta-tenue p-espacio-3 text-pequeno text-alerta-texto"
            >
              {avisos.map((aviso) => (
                <li key={aviso}>{t(aviso)}</li>
              ))}
            </ul>
          ) : null}
        </div>
        <fieldset
          disabled={deshabilitado}
          className="min-w-0 border-t border-borde p-espacio-4 sm:p-espacio-5"
        >
          <ConfiguracionNodo
            nodo={nodo}
            alCambiar={(siguiente) => alCambiarNodo(nodo.id, siguiente)}
          />
        </fieldset>
        {!extremo ? (
          <div className="flex flex-wrap gap-espacio-2 border-t border-borde p-espacio-4">
            <Boton
              variante="secundario"
              tamano="sm"
              disabled={deshabilitado}
              onClick={() => alDuplicarNodo(nodo.id)}
            >
              {t("canvas.duplicarPaso")}
            </Boton>
            <Boton
              variante="peligro"
              tamano="sm"
              disabled={deshabilitado}
              onClick={() => alEliminarNodo(nodo.id)}
            >
              {t("canvas.eliminarPaso")}
            </Boton>
          </div>
        ) : null}
      </Tarjeta>
    );
  }

  if (arista) {
    return (
      <Tarjeta>
        <CabeceraTarjeta
          titulo={t("canvas.conexion")}
          descripcion={`${arista.origen} → ${arista.destino}`}
        />
        <div className="mt-espacio-4">
          <Campo
            etiqueta={t("canvas.condicion")}
            placeholder={t("canvas.condicionPlaceholder")}
            value={arista.condicion ?? ""}
            disabled={deshabilitado}
            onChange={(evento) =>
              alCambiarArista(claveArista(arista), evento.target.value)
            }
          />
          <p className="mt-espacio-2 text-pequeno text-tinta-suave">
            {t("canvas.condicionAyuda")}
          </p>
        </div>
        <div className="mt-espacio-4 border-t border-borde pt-espacio-4">
          <Boton
            variante="peligro"
            tamano="sm"
            disabled={deshabilitado}
            onClick={() => alEliminarArista(claveArista(arista))}
          >
            {t("canvas.eliminarConexion")}
          </Boton>
        </div>
      </Tarjeta>
    );
  }

  return (
    <Tarjeta>
      <CabeceraTarjeta
        titulo={t("canvas.propiedades")}
        descripcion={t("canvas.propiedadesDesc")}
      />
      <p className="mt-espacio-4 text-pequeno text-tinta-suave">
        {t("canvas.ayuda")}
      </p>
    </Tarjeta>
  );
}

function PanelSimulacion({
  alSimular,
  resultado,
  simulando,
  alCerrar,
}: {
  alSimular: (datos: Record<string, unknown>) => void;
  resultado?: SimulacionResultado;
  simulando: boolean;
  alCerrar: () => void;
}) {
  const { t } = useIdioma();
  const [datos, setDatos] = useState("{\n\n}");
  const [errorDatos, setErrorDatos] = useState<string | null>(null);

  function ejecutar() {
    let parseado: Record<string, unknown>;
    try {
      parseado = JSON.parse(datos || "{}") as Record<string, unknown>;
      if (
        typeof parseado !== "object" ||
        parseado === null ||
        Array.isArray(parseado)
      )
        throw new Error();
    } catch {
      setErrorDatos(t("procesos.simulacionDatosInvalidos"));
      return;
    }
    setErrorDatos(null);
    alSimular(parseado);
  }

  return (
    <Tarjeta className="mt-espacio-6" padding="p-espacio-4 sm:p-espacio-6">
      <div className="flex flex-wrap items-start justify-between gap-espacio-4">
        <CabeceraTarjeta
          titulo={t("procesos.simulacionTitulo")}
          descripcion={t("procesos.simulacionDesc")}
        />
        <Boton variante="fantasma" tamano="sm" onClick={alCerrar}>
          {t("comun.cerrar")}
        </Boton>
      </div>
      <AreaTexto
        etiqueta={t("procesos.simulacionDatos")}
        placeholder='{"monto_total": 1200, "decision": "APROBADO"}'
        ayuda={t("procesos.simulacionDatosAyuda")}
        rows={4}
        className="mt-espacio-4 font-codigo"
        value={datos}
        onChange={(evento) => setDatos(evento.target.value)}
      />
      {errorDatos ? (
        <p role="alert" className="mt-espacio-2 text-pequeno text-peligro">
          {errorDatos}
        </p>
      ) : null}
      <div className="mt-espacio-4">
        <Boton
          variante="primario"
          cargando={simulando}
          disabled={simulando}
          onClick={ejecutar}
        >
          {t("procesos.ejecutarSimulacion")}
        </Boton>
      </div>
      {resultado ? (
        <div className="mt-espacio-5 border-t border-borde pt-espacio-4">
          <p className="flex items-center gap-espacio-2 text-pequeno text-tinta-media">
            <Pastilla tono={resultado.terminada ? "exito" : "informacion"}>
              {resultado.terminada
                ? t("procesos.simulacionTerminada")
                : t("procesos.simulacionIncompleta")}
            </Pastilla>
            <span className="tabular-nums">
              {t("procesos.simulacionPasos", {
                cantidad: resultado.pasos.length,
              })}
            </span>
          </p>
          {resultado.advertencias.length ? (
            <ul
              role="alert"
              className="mt-espacio-4 space-y-espacio-1 rounded-control border border-alerta-borde bg-alerta-tenue p-espacio-3 text-pequeno text-alerta-texto"
            >
              {resultado.advertencias.map((advertencia) => (
                <li key={advertencia}>{advertencia}</li>
              ))}
            </ul>
          ) : null}
          <ol className="mt-espacio-4 space-y-espacio-2">
            {resultado.pasos.map((paso, indice) => (
              <li
                key={`${paso.nodoId}-${indice}`}
                className="flex items-baseline gap-espacio-3 rounded-control bg-lienzo p-espacio-3"
              >
                <span className="w-6 shrink-0 text-right text-micro text-tinta-suave tabular-nums">
                  {indice + 1}
                </span>
                <div className="min-w-0">
                  <p className="text-pequeno font-semibold text-tinta">
                    {paso.nombre || paso.nodoId}
                    <span className="ml-espacio-2 font-normal text-tinta-suave">
                      {paso.tipo ? t(`tipoNodo.${paso.tipo}`) : ""}
                    </span>
                  </p>
                  {paso.detalle ? (
                    <p className="text-pequeno text-tinta-suave">
                      {paso.detalle}
                    </p>
                  ) : null}
                </div>
              </li>
            ))}
          </ol>
        </div>
      ) : null}
    </Tarjeta>
  );
}

function erroresDeValidacion(fallo: unknown): string[] {
  const detalles = (fallo as { response?: { data?: { detalles?: string[] } } })
    ?.response?.data?.detalles;
  return Array.isArray(detalles) ? detalles : [];
}
