import { useEffect, useId, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Contenido, Encabezado } from "../componentes/Disposicion";
import { Cargando, ErrorPanel, Vacio } from "../componentes/Estados";
import {
  Boton,
  BotonIcono,
  CabeceraTarjeta,
  Campo,
  GrupoSegmentado,
  Pastilla,
  Selector,
  Tarjeta,
} from "../componentes/Interfaz";
import {
  IconoCheck,
  IconoCerrar,
  IconoFlechaAbajo,
  IconoFlechaArriba,
} from "../componentes/Iconos";
import { formatearFecha } from "./Documentos";
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
} from "../api/procesos";
import type {
  GrafoProceso,
  InstanciaProceso,
  Proceso,
  TipoNodoProceso,
  VersionProceso,
} from "../api/procesos";
import {
  inspeccionarGrafo,
  serializarGrafo,
  type Paso,
} from "../utilidades/grafoProceso";
import { useSesion } from "../contextos/ProveedorSesion";
import { useIdioma } from "../contextos/ProveedorIdioma";
import {
  BandejaInstancias,
  BandejaTareas,
  DetalleInstancia,
} from "./ProcesosOperacion";
import { ReglasSupervisora } from "./ReglasSupervisora";
import { DiagramaProceso } from "../componentes/DiagramaProceso";

const TIPOS_PASO: TipoNodoProceso[] = [
  "SOLICITUD_DOCUMENTO",
  "FORMULARIO",
  "VALIDACION_IA",
  "REVISION_HUMANA",
  "TAREA_EXTERNA",
  "NOTIFICACION",
  "TEMPORIZADOR",
  "ACCION_API",
  "SUBPROCESO",
];

type VistaProcesos = "definiciones" | "instancias" | "tareas" | "reglas";

export function Procesos() {
  const { t } = useIdioma();
  const [vista, setVista] = useState<VistaProcesos>("definiciones");
  const [procesoAbierto, setProcesoAbierto] = useState<string | null>(null);
  const [instanciaAbierta, setInstanciaAbierta] = useState<string | null>(null);

  if (procesoAbierto) {
    return (
      <EstudioProceso
        procesoId={procesoAbierto}
        alVolver={() => setProcesoAbierto(null)}
        alAbrirInstancia={(instanciaId) => {
          setVista("instancias");
          setInstanciaAbierta(instanciaId);
        }}
      />
    );
  }

  return (
    <>
      <Encabezado
        titulo={t("procesos.titulo")}
        descripcion={t("procesos.descripcion")}
        acciones={
          <GrupoSegmentado
            etiqueta={t("procesos.vistas")}
            valor={vista}
            alCambiar={setVista}
            opciones={[
              { valor: "definiciones", texto: t("procesos.definiciones") },
              { valor: "instancias", texto: t("procesos.instancias") },
              { valor: "tareas", texto: t("procesos.tareas") },
              { valor: "reglas", texto: t("procesos.reglas") },
            ]}
          />
        }
      />
      <Contenido>
        {vista === "definiciones" ? (
          <ListaProcesos alAbrir={setProcesoAbierto} />
        ) : vista === "instancias" ? (
          instanciaAbierta ? (
            <DetalleInstancia
              instanciaId={instanciaAbierta}
              alVolver={() => setInstanciaAbierta(null)}
            />
          ) : (
            <BandejaInstancias alAbrir={setInstanciaAbierta} />
          )
        ) : vista === "reglas" ? (
          <ReglasSupervisora />
        ) : (
          <BandejaTareas />
        )}
      </Contenido>
    </>
  );
}

function ListaProcesos({ alAbrir }: { alAbrir: (id: string) => void }) {
  const { t } = useIdioma();
  const clienteConsultas = useQueryClient();
  const [creando, setCreando] = useState(false);
  const [codigo, setCodigo] = useState("");
  const [familia, setFamilia] = useState("");
  const [nombre, setNombre] = useState("");
  const [sla, setSla] = useState("");
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
      setCreando(false);
      setCodigo("");
      setFamilia("");
      setNombre("");
      setSla("");
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
          variante={creando ? "secundario" : "primario"}
          aria-expanded={creando}
          aria-controls={creando ? "crear-proceso" : undefined}
          onClick={() => setCreando((valor) => !valor)}
        >
          {creando ? t("comun.cancelar") : t("procesos.nuevoProceso")}
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

        {creando ? (
          <Tarjeta className="mb-espacio-6">
            <div id="crear-proceso">
              <CabeceraTarjeta
                titulo={t("procesos.nuevoProcesoTitulo")}
                descripcion={t("procesos.nuevoProcesoDesc")}
              />
              <div className="mt-espacio-5 grid gap-espacio-4 md:grid-cols-2 xl:grid-cols-4">
                <Campo
                  etiqueta={t("procesos.codigo")}
                  placeholder="COMEX-EX-MAR-FCL"
                  value={codigo}
                  onChange={(evento) =>
                    setCodigo(evento.target.value.toUpperCase())
                  }
                />
                <Campo
                  etiqueta={t("procesos.familia")}
                  placeholder="COMEX"
                  value={familia}
                  onChange={(evento) => setFamilia(evento.target.value)}
                />
                <Campo
                  etiqueta={t("procesos.nombre")}
                  placeholder="Exportacion maritima FCL"
                  value={nombre}
                  onChange={(evento) => setNombre(evento.target.value)}
                />
                <Campo
                  etiqueta={t("procesos.slaPlantilla")}
                  type="number"
                  min="0.01"
                  step="0.25"
                  placeholder="24"
                  value={sla}
                  onChange={(evento) => setSla(evento.target.value)}
                  ayuda={t("procesos.slaAyuda")}
                />
              </div>
              <div className="mt-4 flex justify-end">
                <Boton
                  variante="primario"
                  cargando={crear.isPending}
                  disabled={
                    crear.isPending ||
                    !codigo.trim() ||
                    !familia.trim() ||
                    !nombre.trim()
                  }
                  onClick={() =>
                    crear.mutate({
                      codigo: codigo.trim(),
                      familia: familia.trim(),
                      nombre: nombre.trim(),
                      slaHoras: slaNumerico(sla),
                    })
                  }
                >
                  {t("procesos.crear")}
                </Boton>
              </div>
            </div>
          </Tarjeta>
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
                        <Pastilla tono={proceso.slaHoras == null ? "neutro" : "violeta"}>
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
                        variante="secundario"
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
  return texto.trim() && Number.isFinite(valor) && valor > 0 ? valor : undefined;
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
  const [sla, setSla] = useState(proceso.slaHoras == null ? "" : String(proceso.slaHoras));
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
        <Boton variante="secundario" onClick={alCerrar} disabled={guardar.isPending}>
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
  const idEstudio = useId();
  const { sesion } = useSesion();
  const { t } = useIdioma();
  const clienteConsultas = useQueryClient();
  const [pasos, setPasos] = useState<Paso[]>([]);
  const [expandido, setExpandido] = useState<string | null>(null);
  const [instanciaPrueba, setInstanciaPrueba] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [detalles, setDetalles] = useState<string[]>([]);
  const [aviso, setAviso] = useState<string | null>(null);
  const [pasoAgregado, setPasoAgregado] = useState<string | null>(null);
  const [tipoNuevoPaso, setTipoNuevoPaso] = useState("");
  const [vistaRecorrido, setVistaRecorrido] = useState<"lista" | "diagrama">("lista");

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
    pasos: Paso[];
  } | null>(null);
  const analisis = base ? inspeccionarGrafo(base.grafo) : null;
  const bloqueo = analisis?.bloqueo ? t(analisis.bloqueo) : conflicto;
  const baseLista = !!borrador && base?.versionId === borrador.id;
  const hayCambios =
    baseLista &&
    !analisis?.bloqueo &&
    JSON.stringify(pasos) !== JSON.stringify(base.pasos);
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
      const iniciales = inspeccionarGrafo(grafo).pasos;
      setBase({ versionId: borrador.id, grafo, pasos: iniciales });
      setPasos(iniciales);
    }
  }, [borrador]);

  useEffect(() => {
    if (!pasoAgregado) return;
    const elemento = document.getElementById("paso-" + pasoAgregado);
    elemento?.scrollIntoView({ block: "center", behavior: "smooth" });
    setPasoAgregado(null);
  }, [pasoAgregado]);

  const refrescar = () =>
    clienteConsultas.invalidateQueries({ queryKey: ["proceso", procesoId] });

  const guardar = useMutation({
    mutationFn: () => {
      if (!baseLista || bloqueo || consulta.isFetching)
        throw new Error(bloqueo ?? t("procesos.borradorNoDisponible"));
      const grafo = serializarGrafo(base.grafo, pasos);
      return actualizarGrafo(borrador!.id, grafo);
    },
    onSuccess: (version) => {
      versionCargada.current = {
        id: version.id,
        grafo: JSON.stringify(version.grafo),
      };
      const grafo = structuredClone(version.grafo);
      const guardados = inspeccionarGrafo(grafo).pasos;
      setBase({ versionId: version.id, grafo, pasos: guardados });
      setPasos(guardados);
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
      if (!baseLista || bloqueo || hayCambios || consulta.isFetching)
        throw new Error(bloqueo ?? t("procesos.guardarAntesDePublicar"));
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

  const clonar = useMutation({
    mutationFn: () =>
      nuevaVersion(procesoId, t("procesos.notaNuevaVersion")),
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
    !!bloqueo ||
    consulta.isFetching ||
    guardar.isPending ||
    publicar.isPending;

  function agregarPaso(tipo: TipoNodoProceso) {
    const nombre = t(`tipoPaso.${tipo}`);
    const paso: Paso = {
      id:
        "paso-" +
        (pasos.length + 1) +
        "-" +
        Math.random().toString(36).slice(2, 6),
      tipo,
      nombre,
    };
    setPasos((actuales) => [...actuales, paso]);
    setExpandido(paso.id);
    setPasoAgregado(paso.id);
    setAviso(t("procesos.pasoAgregado", { nombre }));
  }

  function mover(id: string, desplazamiento: -1 | 1) {
    setPasos((actuales) => {
      const copia = [...actuales];
      const indice = copia.findIndex((paso) => paso.id === id);
      const destino = indice + desplazamiento;
      if (indice < 0 || destino < 0 || destino >= copia.length) {
        return actuales;
      }
      [copia[indice], copia[destino]] = [copia[destino], copia[indice]];
      return copia;
    });
  }

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
                <div className="flex flex-wrap items-center gap-espacio-2">
                  <GrupoSegmentado
                    etiqueta={t("procesos.formaVerRecorrido")}
                    valor={vistaRecorrido}
                    alCambiar={setVistaRecorrido}
                    opciones={[
                      { valor: "lista", texto: t("procesos.lista") },
                      { valor: "diagrama", texto: t("procesos.diagrama") },
                    ]}
                  />
                  <Pastilla tono={hayCambios ? "alerta" : "neutro"}>
                    {hayCambios
                      ? t("procesos.sinGuardar")
                      : t("procesos.borradorEtiqueta")}
                  </Pastilla>
                </div>
              }
            />
            {bloqueo ? (
              <div className="mt-espacio-4">
                <ErrorPanel
                  titulo={t("procesos.versionNoEditable")}
                  mensaje={t("procesos.versionNoEditableDesc", { bloqueo })}
                />
              </div>
            ) : null}
            {vistaRecorrido === "diagrama" ? (
              <div className="mt-espacio-5">
                <DiagramaProceso grafo={base?.grafo ?? borrador.grafo} />
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
            <p
              role="note"
              className="mb-espacio-5 mt-espacio-4 text-pequeno text-tinta-suave"
            >
              {t("procesos.catalogoNota")}
            </p>
            <fieldset
              disabled={editandoBloqueado}
              className={`min-w-0 ${vistaRecorrido === "diagrama" ? "hidden" : ""}`}
            >
              <legend className="sr-only">{t("procesos.edicionBorrador")}</legend>
              {!bloqueo ? (
                <ol
                  aria-label={t("procesos.secuenciaPasos")}
                  className="mt-espacio-4 [&>li+li]:before:mx-auto [&>li+li]:before:block [&>li+li]:before:h-espacio-5 [&>li+li]:before:w-px [&>li+li]:before:bg-violeta-borde"
                >
                  <PasoFijo etiqueta={t("procesos.inicio")} />
                  {pasos.map((paso, indice) => (
                    <li key={paso.id} id={"paso-" + paso.id}>
                      <Tarjeta
                        padding="p-0"
                        className={
                          expandido === paso.id ? "border-violeta" : ""
                        }
                      >
                        <div className="flex flex-wrap items-center gap-espacio-3 p-espacio-4">
                          <span
                            aria-hidden="true"
                            className="flex size-control-pequeno shrink-0 items-center justify-center rounded-control bg-violeta-tenue text-pequeno font-semibold tabular-nums text-accion-tonal-texto"
                          >
                            {String(indice + 1).padStart(2, "0")}
                          </span>
                          <div className="min-w-0 flex-1 basis-40">
                            <p className="break-words text-micro font-semibold tracking-wide text-tinta-suave">
                              {t(`tipoNodo.${paso.tipo}`)}
                            </p>
                            <h3
                              id={`${idEstudio}-paso-${paso.id}`}
                              className="mt-espacio-1 break-words font-titulo text-titulo-panel text-tinta"
                            >
                              <span className="sr-only">
                                {t("procesos.pasoN", { numero: indice + 1 })}
                              </span>
                              {paso.nombre || t(`tipoNodo.${paso.tipo}`)}
                            </h3>
                          </div>
                          <div className="flex w-full flex-wrap items-center justify-between gap-espacio-2 sm:w-auto">
                            <span className="flex gap-espacio-1">
                              <BotonIcono
                                variante="fantasma"
                                tamano="sm"
                                aria-label={t("procesos.subirPaso", {
                                  numero: indice + 1,
                                  nombre: paso.nombre,
                                })}
                                disabled={indice === 0}
                                onClick={() => mover(paso.id, -1)}
                              >
                                <IconoFlechaArriba tamano={14} />
                              </BotonIcono>
                              <BotonIcono
                                variante="fantasma"
                                tamano="sm"
                                aria-label={t("procesos.bajarPaso", {
                                  numero: indice + 1,
                                  nombre: paso.nombre,
                                })}
                                disabled={indice === pasos.length - 1}
                                onClick={() => mover(paso.id, 1)}
                              >
                                <IconoFlechaAbajo tamano={14} />
                              </BotonIcono>
                              <BotonIcono
                                variante="fantasma"
                                tamano="sm"
                                aria-label={t("procesos.quitarPaso", {
                                  numero: indice + 1,
                                  nombre: paso.nombre,
                                })}
                                onClick={() =>
                                  setPasos((actuales) =>
                                    actuales.filter(
                                      (otro) => otro.id !== paso.id,
                                    ),
                                  )
                                }
                                className="text-rojo-alto"
                              >
                                <IconoCerrar tamano={14} />
                              </BotonIcono>
                            </span>
                            <Boton
                              variante="fantasma"
                              tamano="sm"
                              aria-expanded={expandido === paso.id}
                              aria-controls={
                                expandido === paso.id
                                  ? `${idEstudio}-configuracion-${paso.id}`
                                  : undefined
                              }
                              onClick={() =>
                                setExpandido(
                                  expandido === paso.id ? null : paso.id,
                                )
                              }
                            >
                              {expandido === paso.id
                                ? t("procesos.ocultar")
                                : t("procesos.configurar")}
                            </Boton>
                          </div>
                        </div>
                        {expandido === paso.id ? (
                          <div
                            id={`${idEstudio}-configuracion-${paso.id}`}
                            role="region"
                            aria-labelledby={`${idEstudio}-paso-${paso.id}`}
                          >
                            <ConfiguracionPaso
                              paso={paso}
                              alCambiar={(cambio) =>
                                setPasos((actuales) =>
                                  actuales.map((otro) =>
                                    otro.id === paso.id
                                      ? { ...otro, ...cambio }
                                      : otro,
                                  ),
                                )
                              }
                            />
                          </div>
                        ) : null}
                      </Tarjeta>
                    </li>
                  ))}
                  <PasoFijo etiqueta={t("procesos.fin")} />
                </ol>
              ) : null}
              <div className="mt-espacio-6 grid gap-espacio-5 border-t border-borde pt-espacio-5">
                <Selector
                  etiqueta={t("procesos.agregarPaso")}
                  ayuda={t("procesos.agregarPasoAyuda")}
                  disabled={editandoBloqueado}
                  value={tipoNuevoPaso}
                  onChange={(evento) => {
                    const elegido = evento.target.value;
                    setTipoNuevoPaso(elegido);
                    if (elegido) {
                      agregarPaso(elegido as TipoNodoProceso);
                      setTipoNuevoPaso("");
                    }
                  }}
                  className="w-full sm:max-w-sm"
                >
                  <option value="">{t("procesos.elegirTipo")}</option>
                  {TIPOS_PASO.map((opcion) => (
                    <option key={opcion} value={opcion}>
                      {t(`tipoPaso.${opcion}`)}
                    </option>
                  ))}
                </Selector>
                <div className="grid gap-espacio-3 sm:flex sm:justify-end">
                  <Boton
                    variante="secundario"
                    cargando={guardar.isPending}
                    disabled={guardar.isPending}
                    onClick={() => guardar.mutate()}
                  >
                    {t("procesos.guardarBorrador")}
                  </Boton>
                  <Boton
                    variante="primario"
                    cargando={publicar.isPending}
                    disabled={
                      editandoBloqueado || hayCambios || pasos.length === 0
                    }
                    onClick={() => publicar.mutate()}
                  >
                    {t("procesos.publicarVersion", {
                      numero: borrador.numero,
                    })}
                  </Boton>
                </div>
              </div>
            </fieldset>
          </Tarjeta>
        )}

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
                  {publicada.publicada ? formatearFecha(publicada.publicada) : "—"}
                </dd>
              </div>
              <div>
                <dt className="text-micro uppercase tracking-wide text-tinta-suave">
                  {t("procesos.pasosDelRecorrido")}
                </dt>
                <dd className="mt-espacio-1 text-pequeno text-tinta tabular-nums">
                  {inspeccionarGrafo(publicada.grafo).pasos.length}
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

function PasoFijo({ etiqueta }: { etiqueta: string }) {
  return (
    <li>
      <div className="flex items-center justify-center gap-espacio-3 rounded-control border border-dashed border-borde-fuerte bg-lienzo px-espacio-4 py-espacio-3">
        <span
          aria-hidden="true"
          className="size-espacio-2 rounded-insignia bg-grafito"
        />
        <span className="text-micro font-semibold uppercase tracking-wider text-tinta-media">
          {etiqueta}
        </span>
      </div>
    </li>
  );
}

function ConfiguracionPaso({
  paso,
  alCambiar,
}: {
  paso: Paso;
  alCambiar: (cambio: Partial<Paso>) => void;
}) {
  const { t } = useIdioma();
  return (
    <div className="rounded-b-tarjeta border-t border-violeta-borde bg-lienzo p-espacio-4 sm:p-espacio-5">
      <p className="mb-espacio-4 text-pequeno font-semibold text-tinta">
        {t("procesos.configuracionPaso")}
      </p>
      <div className="grid gap-espacio-4 md:grid-cols-2 xl:grid-cols-3">
        <Campo
          etiqueta={t("procesos.nombreDelPaso")}
          value={paso.nombre}
          onChange={(evento) => alCambiar({ nombre: evento.target.value })}
        />
        {paso.tipo === "SOLICITUD_DOCUMENTO" ? (
          <Campo
            etiqueta={t("procesos.tipoDocumento")}
            placeholder="FACTURA_COMERCIAL"
            value={paso.tipoDocumento ?? ""}
            onChange={(evento) =>
              alCambiar({ tipoDocumento: evento.target.value })
            }
          />
        ) : null}
        {paso.tipo === "SUBPROCESO" ? (
          <Campo
            etiqueta={t("procesos.codigoSubproceso")}
            placeholder="COMEX-OV-DG"
            value={paso.subprocesoCodigo ?? ""}
            onChange={(evento) =>
              alCambiar({ subprocesoCodigo: evento.target.value.toUpperCase() })
            }
          />
        ) : null}
        {[
          "SOLICITUD_DOCUMENTO",
          "FORMULARIO",
          "VALIDACION_IA",
          "REVISION_HUMANA",
          "TAREA_EXTERNA",
        ].includes(paso.tipo) ? (
          <Campo
            etiqueta={t("procesos.responsablePaso")}
            placeholder={t("procesos.responsablePlaceholder")}
            value={paso.asignadoA ?? ""}
            onChange={(evento) => alCambiar({ asignadoA: evento.target.value })}
          />
        ) : null}
        {[
          "SOLICITUD_DOCUMENTO",
          "FORMULARIO",
          "VALIDACION_IA",
          "REVISION_HUMANA",
          "TAREA_EXTERNA",
          "TEMPORIZADOR",
        ].includes(paso.tipo) ? (
          <Campo
            etiqueta={t("procesos.slaHoras")}
            type="number"
            min={1}
            value={paso.slaHoras ?? ""}
            onChange={(evento) =>
              alCambiar({
                slaHoras: evento.target.value
                  ? Number(evento.target.value)
                  : undefined,
              })
            }
          />
        ) : null}
      </div>
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

function erroresDeValidacion(fallo: unknown): string[] {
  const detalles = (fallo as { response?: { data?: { detalles?: string[] } } })
    ?.response?.data?.detalles;
  return Array.isArray(detalles) ? detalles : [];
}
