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
  TipoNodoProceso,
  VersionProceso,
} from "../api/procesos";
import {
  inspeccionarGrafo,
  serializarGrafo,
  type Paso,
} from "../utilidades/grafoProceso";
import { useSesion } from "../contextos/ProveedorSesion";
import {
  BandejaInstancias,
  BandejaTareas,
  DetalleInstancia,
} from "./ProcesosOperacion";

const TIPOS_PASO: { valor: TipoNodoProceso; texto: string }[] = [
  { valor: "SOLICITUD_DOCUMENTO", texto: "Solicitar documento" },
  { valor: "FORMULARIO", texto: "Formulario" },
  { valor: "VALIDACION_IA", texto: "Validar con IA" },
  { valor: "REVISION_HUMANA", texto: "Revision humana" },
  { valor: "TAREA_EXTERNA", texto: "Tarea externa" },
  { valor: "NOTIFICACION", texto: "Notificar" },
  { valor: "TEMPORIZADOR", texto: "Esperar (temporizador)" },
  { valor: "ACCION_API", texto: "Accion de API" },
  { valor: "SUBPROCESO", texto: "Subproceso" },
];

type VistaProcesos = "definiciones" | "instancias" | "tareas";

export function Procesos() {
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
        titulo="Procesos"
        descripcion="Diseñá procesos en el Studio, ejecutalos y resolvé las tareas pendientes desde las bandejas."
        acciones={
          <GrupoSegmentado
            etiqueta="Vistas de procesos"
            valor={vista}
            alCambiar={setVista}
            opciones={[
              { valor: "definiciones", texto: "Definiciones" },
              { valor: "instancias", texto: "Instancias" },
              { valor: "tareas", texto: "Tareas" },
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
        ) : (
          <BandejaTareas />
        )}
      </Contenido>
    </>
  );
}

function ListaProcesos({ alAbrir }: { alAbrir: (id: string) => void }) {
  const clienteConsultas = useQueryClient();
  const [creando, setCreando] = useState(false);
  const [codigo, setCodigo] = useState("");
  const [familia, setFamilia] = useState("");
  const [nombre, setNombre] = useState("");
  const [error, setError] = useState<string | null>(null);

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
          Biblioteca de definiciones del espacio de trabajo. Abrí el Studio para
          configurar los pasos de cada proceso y publicar una versión.
        </p>
        <Boton
          variante={creando ? "secundario" : "primario"}
          aria-expanded={creando}
          aria-controls={creando ? "crear-proceso" : undefined}
          onClick={() => setCreando((valor) => !valor)}
        >
          {creando ? "Cancelar" : "Nuevo proceso"}
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
                titulo="Nuevo proceso"
                descripcion="Creá una definición con su primera versión en borrador."
              />
              <div className="mt-espacio-5 grid gap-espacio-4 md:grid-cols-3">
                <Campo
                  etiqueta="Codigo"
                  placeholder="COMEX-EX-MAR-FCL"
                  value={codigo}
                  onChange={(evento) =>
                    setCodigo(evento.target.value.toUpperCase())
                  }
                />
                <Campo
                  etiqueta="Familia"
                  placeholder="COMEX"
                  value={familia}
                  onChange={(evento) => setFamilia(evento.target.value)}
                />
                <Campo
                  etiqueta="Nombre"
                  placeholder="Exportacion maritima FCL"
                  value={nombre}
                  onChange={(evento) => setNombre(evento.target.value)}
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
                    })
                  }
                >
                  Crear
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
            reintentar={() => consulta.refetch()}
          />
        ) : procesos.length === 0 ? (
          <Vacio
            titulo="Todavía no hay definiciones de proceso"
            detalle="Usá Nuevo proceso para crear un borrador y configurar su secuencia de pasos."
          />
        ) : (
          <ul
            aria-label="Definiciones de proceso"
            className="space-y-espacio-4"
          >
            {procesos.map((proceso) => (
              <li key={proceso.id}>
                <Tarjeta className="grid items-center gap-espacio-4 lg:grid-cols-[minmax(0,1fr)_auto]">
                  <div className="min-w-0 flex-1">
                    <h2 className="break-words font-titulo text-titulo-panel text-tinta">
                      {proceso.nombre}
                    </h2>
                    <p className="mt-espacio-1 break-words text-pequeno text-tinta-suave">
                      {proceso.codigo} · {proceso.familia}
                    </p>
                    {proceso.alta ? (
                      <p className="mt-espacio-2 text-pequeno text-tinta-suave">
                        Creado el {formatearFecha(proceso.alta)}
                      </p>
                    ) : null}
                  </div>
                  <div className="flex flex-wrap items-center justify-between gap-espacio-4 lg:justify-end">
                    <VersionesProceso versiones={proceso.versiones} />
                    <Boton
                      variante="secundario"
                      onClick={() => alAbrir(proceso.id)}
                    >
                      Abrir estudio
                    </Boton>
                  </div>
                </Tarjeta>
              </li>
            ))}
          </ul>
        )}
    </div>
  );
}

function VersionesProceso({ versiones }: { versiones: VersionProceso[] }) {
  if (!versiones.length) {
    return <Pastilla tono="neutro">sin versiones</Pastilla>;
  }
  const publicada = versiones
    .filter((version) => version.estado === "PUBLICADA")
    .sort((a, b) => b.numero - a.numero)[0];
  const borrador = versiones.some((version) => version.estado === "BORRADOR");
  return (
    <div className="flex flex-wrap items-center gap-espacio-2">
      {publicada ? (
        <Pastilla tono="exito">v{publicada.numero} publicada</Pastilla>
      ) : null}
      {borrador ? <Pastilla tono="alerta">borrador</Pastilla> : null}
      {!publicada && !borrador ? (
        <Pastilla tono="neutro">archivada</Pastilla>
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
  const clienteConsultas = useQueryClient();
  const [pasos, setPasos] = useState<Paso[]>([]);
  const [expandido, setExpandido] = useState<string | null>(null);
  const [instanciaPrueba, setInstanciaPrueba] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [detalles, setDetalles] = useState<string[]>([]);
  const [aviso, setAviso] = useState<string | null>(null);

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
  const bloqueo = analisis?.bloqueo ?? conflicto;
  const baseLista = !!borrador && base?.versionId === borrador.id;
  const hayCambios =
    baseLista &&
    !analisis?.bloqueo &&
    JSON.stringify(pasos) !== JSON.stringify(base.pasos);
  const advertenciaSalida =
    "Hay cambios sin guardar. ¿Querés descartarlos y salir del estudio?";

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
        setConflicto(
          "El borrador cambió en el servidor. El guardado y la publicación están bloqueados. Volvé al listado y abrí el estudio para cargar la versión actual.",
        );
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

  const refrescar = () =>
    clienteConsultas.invalidateQueries({ queryKey: ["proceso", procesoId] });

  const guardar = useMutation({
    mutationFn: () => {
      if (!baseLista || bloqueo || consulta.isFetching)
        throw new Error(bloqueo ?? "El borrador todavía no está disponible");
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
      setAviso("Los pasos del borrador quedaron guardados.");
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
        throw new Error(bloqueo ?? "Guardá los cambios antes de publicar");
      return publicarVersion(borrador!.id);
    },
    onSuccess: () => {
      setError(null);
      setDetalles([]);
      setAviso("Version publicada. Las instancias nuevas usan esta version.");
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
      nuevaVersion(procesoId, "Version creada desde el estudio"),
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
        throw new Error("Ingresá un motivo de rechazo de hasta 512 caracteres");
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
    const paso: Paso = {
      id:
        "paso-" +
        (pasos.length + 1) +
        "-" +
        Math.random().toString(36).slice(2, 6),
      tipo,
      nombre: TIPOS_PASO.find((opcion) => opcion.valor === tipo)?.texto ?? tipo,
    };
    setPasos((actuales) => [...actuales, paso]);
    setExpandido(paso.id);
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
          titulo="Studio de proceso"
          descripcion="Cargando la definición y sus versiones."
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
          titulo="Studio de proceso"
          acciones={
            <Boton variante="fantasma" onClick={volver}>
              Volver
            </Boton>
          }
        />
        <Contenido>
          <ErrorPanel
            titulo="No se pudo abrir el Studio"
            mensaje={mensajeDeError(consulta.error)}
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
              Volver
            </Boton>
            {publicada ? (
              <Boton
                variante="secundario"
                cargando={probar.isPending}
                disabled={probar.isPending}
                onClick={() => probar.mutate()}
              >
                Probar proceso
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
            Probar proceso crea una instancia real persistente de la versión
            publicada v{publicada.numero}. No utiliza los cambios del borrador.
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
              titulo="Sin borrador abierto"
              descripcion="Para editar el proceso, creá un borrador a partir de su versión más reciente. Las versiones existentes se conservan."
            />
            <div className="flex justify-end">
              <Boton
                variante="primario"
                cargando={clonar.isPending}
                disabled={clonar.isPending}
                onClick={() => clonar.mutate()}
              >
                Nueva version
              </Boton>
            </div>
          </Tarjeta>
        ) : (
          <Tarjeta
            className="mb-espacio-6"
            padding="p-espacio-4 sm:p-espacio-6"
          >
            <CabeceraTarjeta
              titulo={`Borrador v${borrador.numero}`}
              descripcion="Editá la secuencia y guardá el borrador antes de publicar. Workflow valida el recorrido y los tipos habilitados."
              acciones={
                <Pastilla tono={hayCambios ? "alerta" : "neutro"}>
                  {hayCambios ? "Sin guardar" : "Borrador"}
                </Pastilla>
              }
            />
            {bloqueo ? (
              <div className="mt-espacio-4">
                <ErrorPanel
                  titulo="Esta versión no se puede editar de forma segura"
                  mensaje={bloqueo}
                />
              </div>
            ) : null}
            {hayCambios ? (
              <p
                role="status"
                className="my-espacio-4 rounded-panel border border-alerta-borde bg-alerta-tenue p-espacio-3 text-pequeno text-alerta-texto"
              >
                Hay cambios sin guardar. Guardá el borrador antes de publicar.
              </p>
            ) : null}
            <p
              role="note"
              className="mb-espacio-5 mt-espacio-4 text-pequeno text-tinta-suave"
            >
              El catálogo MVP0 permite publicar Solicitud de documento,
              Formulario, Revisión humana y Temporizador. Los demás tipos del
              selector pueden guardarse en borrador, pero Workflow impide
              publicarlos.
            </p>
            <fieldset disabled={editandoBloqueado} className="min-w-0">
              <legend className="sr-only">Edición del borrador</legend>
              {!bloqueo ? (
                <ol
                  aria-label="Secuencia de pasos"
                  className="mt-espacio-4 [&>li+li]:before:mx-auto [&>li+li]:before:block [&>li+li]:before:h-espacio-5 [&>li+li]:before:w-px [&>li+li]:before:bg-violeta-borde"
                >
                  <PasoFijo etiqueta="Inicio" />
                  {pasos.map((paso, indice) => (
                    <li key={paso.id}>
                      <Tarjeta
                        padding="p-0"
                        className={
                          expandido === paso.id ? "border-violeta" : ""
                        }
                      >
                        <div className="flex flex-wrap items-center gap-espacio-3 p-espacio-4">
                          <span
                            aria-hidden="true"
                            className="flex size-control-pequeno shrink-0 items-center justify-center rounded-control bg-violeta-tenue text-pequeno font-semibold tabular-nums text-violeta"
                          >
                            {String(indice + 1).padStart(2, "0")}
                          </span>
                          <div className="min-w-0 flex-1 basis-40">
                            <p className="break-words text-micro font-semibold tracking-wide text-tinta-suave">
                              {paso.tipo}
                            </p>
                            <h3
                              id={`${idEstudio}-paso-${paso.id}`}
                              className="mt-espacio-1 break-words font-titulo text-titulo-panel text-tinta"
                            >
                              <span className="sr-only">
                                Paso {indice + 1}:{" "}
                              </span>
                              {paso.nombre || paso.tipo}
                            </h3>
                          </div>
                          <div className="flex w-full flex-wrap items-center justify-between gap-espacio-2 sm:w-auto">
                            <span className="flex gap-espacio-1">
                              <BotonIcono
                                variante="fantasma"
                                tamano="sm"
                                aria-label={`Subir paso ${indice + 1}: ${paso.nombre}`}
                                disabled={indice === 0}
                                onClick={() => mover(paso.id, -1)}
                              >
                                <IconoFlechaArriba tamano={14} />
                              </BotonIcono>
                              <BotonIcono
                                variante="fantasma"
                                tamano="sm"
                                aria-label={`Bajar paso ${indice + 1}: ${paso.nombre}`}
                                disabled={indice === pasos.length - 1}
                                onClick={() => mover(paso.id, 1)}
                              >
                                <IconoFlechaAbajo tamano={14} />
                              </BotonIcono>
                              <BotonIcono
                                variante="fantasma"
                                tamano="sm"
                                aria-label={`Quitar paso ${indice + 1}: ${paso.nombre}`}
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
                              {expandido === paso.id ? "Ocultar" : "Configurar"}
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
                  <PasoFijo etiqueta="Fin" />
                </ol>
              ) : null}
              <div className="mt-espacio-6 grid gap-espacio-5 border-t border-borde pt-espacio-5">
                <Selector
                  etiqueta="Agregar paso"
                  value=""
                  onChange={(evento) => {
                    if (evento.target.value) {
                      agregarPaso(evento.target.value as TipoNodoProceso);
                      evento.target.value = "";
                    }
                  }}
                  className="w-full sm:max-w-sm"
                >
                  <option value="">Elegi un tipo de paso...</option>
                  {TIPOS_PASO.map((opcion) => (
                    <option key={opcion.valor} value={opcion.valor}>
                      {opcion.texto}
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
                    Guardar borrador
                  </Boton>
                  <Boton
                    variante="primario"
                    cargando={publicar.isPending}
                    disabled={
                      editandoBloqueado || hayCambios || pasos.length === 0
                    }
                    onClick={() => publicar.mutate()}
                  >
                    Publicar v{borrador.numero}
                  </Boton>
                </div>
              </div>
            </fieldset>
          </Tarjeta>
        )}

        {publicada ? (
          <Tarjeta>
            <CabeceraTarjeta
              titulo={`Versión publicada v${publicada.numero}`}
              descripcion="Las instancias nuevas usan esta versión. Una instancia ya iniciada conserva su versión aunque publiques otra."
              acciones={<Pastilla tono="exito">Publicada</Pastilla>}
            />
            <p className="mt-espacio-3 break-all rounded-control bg-lienzo p-espacio-3 font-codigo text-codigo text-tinta-suave">
              Hash: {publicada.hash ?? "—"}
            </p>
          </Tarjeta>
        ) : null}

        {instanciaPrueba ? (
          consultaInstancia.isError ? (
            <ErrorPanel
              mensaje={mensajeDeError(consultaInstancia.error)}
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
  return (
    <div className="rounded-b-tarjeta border-t border-violeta-borde bg-lienzo p-espacio-4 sm:p-espacio-5">
      <p className="mb-espacio-4 text-pequeno font-semibold text-tinta">
        Configuración del paso
      </p>
      <div className="grid gap-espacio-4 md:grid-cols-2 xl:grid-cols-3">
        <Campo
          etiqueta="Nombre del paso"
          value={paso.nombre}
          onChange={(evento) => alCambiar({ nombre: evento.target.value })}
        />
        {paso.tipo === "SOLICITUD_DOCUMENTO" ? (
          <Campo
            etiqueta="Tipo de documento"
            placeholder="FACTURA_COMERCIAL"
            value={paso.tipoDocumento ?? ""}
            onChange={(evento) =>
              alCambiar({ tipoDocumento: evento.target.value })
            }
          />
        ) : null}
        {paso.tipo === "SUBPROCESO" ? (
          <Campo
            etiqueta="Codigo del subproceso"
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
            etiqueta="Responsable"
            placeholder="comex@empresa.com o rol"
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
            etiqueta="SLA (horas)"
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
        <CabeceraTarjeta titulo="Cargando instancia de prueba" />
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
          titulo={`Prueba: instancia ${instancia.id.slice(0, 8)}`}
          descripcion={`Estado ${instancia.estado} · v${instancia.numeroVersion}`}
        />
        <Boton variante="fantasma" onClick={() => alAbrir(instancia.id)}>
          Abrir en Instancias
        </Boton>
      </div>
      {finalizada ? (
        <p
          role="status"
          className="mt-espacio-4 rounded-panel border border-borde bg-lienzo p-espacio-4 text-pequeno text-tinta-media"
        >
          La instancia terminó {instancia.estado.toLowerCase()}.
        </p>
      ) : (
        <>
          <p
            role="status"
            className="mt-espacio-4 text-pequeno text-tinta-media"
          >
            {instancia.estado === "BLOQUEADA"
              ? "La instancia está bloqueada y no admite completar tareas."
              : instancia.estado === "CREADA"
                ? "La instancia está creada y todavía no admite completar tareas."
                : instancia.estado === "ESPERANDO"
                  ? "La instancia está esperando la resolución de tareas."
                  : "La instancia está activa."}
          </p>
          {pendientes.length ? (
            <ul
              aria-label="Tareas pendientes o vencidas de la prueba"
              className="mt-espacio-5 space-y-espacio-4"
            >
              {pendientes.map((tarea) => (
                <li
                  key={tarea.id}
                  className="rounded-panel border border-borde bg-lienzo p-espacio-4"
                >
                  <div className="flex flex-wrap items-center gap-espacio-3">
                    <div className="flex min-w-0 basis-full flex-wrap gap-espacio-2">
                      <Pastilla tono="violeta">{tarea.tipoNodo}</Pastilla>
                      <Pastilla
                        tono={tarea.estado === "VENCIDA" ? "alerta" : "neutro"}
                      >
                        {tarea.estado}
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
                            Aprobar
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
                            Rechazar
                          </Boton>
                        </>
                      ) : (
                        <Boton
                          variante="primario"
                          tamano="sm"
                          disabled={completando}
                          onClick={() => enviar(tarea.id)}
                        >
                          Completar
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
                        etiqueta="Motivo del rechazo"
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
                          Confirmar rechazo
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
                          Cancelar
                        </Boton>
                      </div>
                    </form>
                  ) : null}
                </li>
              ))}
            </ul>
          ) : (
            <p className="mt-2 text-sm text-tinta-suave">
              Sin tareas pendientes o vencidas.
            </p>
          )}
          {completando ? (
            <p
              role="status"
              aria-busy="true"
              className="mt-2 text-sm text-tinta-suave"
            >
              Enviando decisión…
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
