import { useEffect, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Contenido, Encabezado } from "../componentes/Disposicion";
import { Cargando, ErrorPanel, Vacio } from "../componentes/Estados";
import {
  Boton,
  CabeceraTarjeta,
  Campo,
  Pastilla,
  Selector,
  Tarjeta,
} from "../componentes/Interfaz";
import { IconoCheck, IconoFlechaAbajo, IconoFlechaArriba } from "../componentes/Iconos";
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
import { inspeccionarGrafo, serializarGrafo, type Paso } from "../utilidades/grafoProceso";
import { useSesion } from "../contextos/ProveedorSesion";

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

export function Procesos() {
  const [procesoAbierto, setProcesoAbierto] = useState<string | null>(null);

  if (procesoAbierto) {
    return <EstudioProceso procesoId={procesoAbierto} alVolver={() => setProcesoAbierto(null)} />;
  }
  return <ListaProcesos alAbrir={setProcesoAbierto} />;
}

function ListaProcesos({ alAbrir }: { alAbrir: (id: string) => void }) {
  const clienteConsultas = useQueryClient();
  const [creando, setCreando] = useState(false);
  const [codigo, setCodigo] = useState("");
  const [familia, setFamilia] = useState("");
  const [nombre, setNombre] = useState("");
  const [error, setError] = useState<string | null>(null);

  const consulta = useQuery({ queryKey: ["procesos"], queryFn: listarProcesos });

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
    <>
      <Encabezado
        titulo="Plantillas de proceso"
        descripcion="El estudio guiado para armar el recorrido de un proceso documental: que documentos pide, quien revisa y cuanto puede esperar cada paso."
        acciones={
          <Boton onClick={() => setCreando((valor) => !valor)}>
            {creando ? "Cancelar" : "Nuevo proceso"}
          </Boton>
        }
      />
      <Contenido>
        {error ? (
          <div className="aparecer mb-4 rounded-xl border border-rojo-borde bg-rojo-tenue px-4 py-3 text-sm text-rojo">
            {error}
          </div>
        ) : null}

        {creando ? (
          <Tarjeta className="mb-5">
            <CabeceraTarjeta titulo="Nuevo proceso" descripcion="Arranca con una version borrador v1." />
            <div className="grid gap-3 sm:grid-cols-3">
              <Campo
                etiqueta="Codigo"
                placeholder="COMEX-EX-MAR-FCL"
                value={codigo}
                onChange={(evento) => setCodigo(evento.target.value.toUpperCase())}
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
                disabled={crear.isPending || !codigo.trim() || !familia.trim() || !nombre.trim()}
                onClick={() =>
                  crear.mutate({ codigo: codigo.trim(), familia: familia.trim(), nombre: nombre.trim() })
                }
              >
                Crear
              </Boton>
            </div>
          </Tarjeta>
        ) : null}

        {consulta.isPending ? (
          <Cargando filas={3} alto="h-24" />
        ) : consulta.isError ? (
          <ErrorPanel mensaje={mensajeDeError(consulta.error)} reintentar={() => consulta.refetch()} />
        ) : procesos.length === 0 ? (
          <Vacio
            titulo="Todavia no hay procesos"
            detalle="Cre&aacute; el primero con Nuevo proceso: despues lo editas paso a paso, lo publicas y queda listo para que entren documentos."
          />
        ) : (
          <div className="space-y-3">
            {procesos.map((proceso) => (
              <Tarjeta key={proceso.id} className="flex flex-wrap items-center gap-4">
                <div className="min-w-0 flex-1">
                  <p className="truncate font-titulo text-base font-semibold text-tinta">
                    {proceso.nombre}
                  </p>
                  <p className="mt-0.5 text-xs text-tinta-suave">
                    {proceso.codigo} · {proceso.familia}
                  </p>
                </div>
                <VersionesProceso versiones={proceso.versiones} />
                <Boton variante="secundario" onClick={() => alAbrir(proceso.id)}>
                  Abrir estudio
                </Boton>
              </Tarjeta>
            ))}
          </div>
        )}
      </Contenido>
    </>
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
    <div className="flex items-center gap-1.5">
      {publicada ? <Pastilla tono="exito">v{publicada.numero} publicada</Pastilla> : null}
      {borrador ? <Pastilla tono="alerta">borrador</Pastilla> : null}
      {!publicada && !borrador ? <Pastilla tono="neutro">archivada</Pastilla> : null}
    </div>
  );
}

function EstudioProceso({ procesoId, alVolver }: { procesoId: string; alVolver: () => void }) {
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
  const borrador = proceso?.versiones.find((version) => version.estado === "BORRADOR");
  const publicada = proceso?.versiones.find((version) => version.estado === "PUBLICADA");

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
    baseLista && !analisis?.bloqueo && JSON.stringify(pasos) !== JSON.stringify(base.pasos);
  const advertenciaSalida = "Hay cambios sin guardar. ¿Querés descartarlos y salir del estudio?";

  useEffect(() => {
    if (!hayCambios) return;
    const alDescargar = (evento: BeforeUnloadEvent) => {
      evento.preventDefault();
      evento.returnValue = "";
    };
    const alNavegar = (evento: MouseEvent) => {
      const enlace = evento.target instanceof Element ? evento.target.closest("a") : null;
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
      if (enlace.pathname === window.location.pathname && enlace.origin === window.location.origin)
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
    if (versionCargada.current?.id !== borrador.id || versionCargada.current.grafo !== firma) {
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

  const refrescar = () => clienteConsultas.invalidateQueries({ queryKey: ["proceso", procesoId] });

  const guardar = useMutation({
    mutationFn: () => {
      if (!baseLista || bloqueo || consulta.isFetching)
        throw new Error(bloqueo ?? "El borrador todavía no está disponible");
      const grafo = serializarGrafo(base.grafo, pasos);
      return actualizarGrafo(borrador!.id, grafo);
    },
    onSuccess: (version) => {
      versionCargada.current = { id: version.id, grafo: JSON.stringify(version.grafo) };
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
    },
    onError: (fallo) => {
      setAviso(null);
      setError(mensajeDeError(fallo));
      setDetalles(erroresDeValidacion(fallo));
    },
  });

  const clonar = useMutation({
    mutationFn: () => nuevaVersion(procesoId, "Version creada desde el estudio"),
    onSuccess: () => {
      setError(null);
      setAviso(null);
      refrescar();
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
      ["CREADA", "ACTIVA", "ESPERANDO", "BLOQUEADA"].includes(consulta.state.data.estado)
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
      if (decision === "RECHAZADO" && (!motivo?.trim() || motivo.trim().length > 512))
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

  async function alCompletar(tareaId: string, decision?: string, motivo?: string) {
    try {
      await completar.mutateAsync({ tareaId, decision, motivo });
      return true;
    } catch {
      return false;
    }
  }

  const editandoBloqueado =
    !baseLista || !!bloqueo || consulta.isFetching || guardar.isPending || publicar.isPending;

  function agregarPaso(tipo: TipoNodoProceso) {
    const paso: Paso = {
      id: "paso-" + (pasos.length + 1) + "-" + Math.random().toString(36).slice(2, 6),
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
      <Contenido>
        <Cargando filas={4} alto="h-24" />
      </Contenido>
    );
  }
  if (consulta.isError || !proceso) {
    return (
      <Contenido>
        <ErrorPanel
          mensaje={mensajeDeError(consulta.error)}
          reintentar={() => consulta.refetch()}
        />
      </Contenido>
    );
  }

  return (
    <>
      <Encabezado
        titulo={proceso.nombre}
        descripcion={`${proceso.codigo} · ${proceso.familia}`}
        acciones={
          <div className="flex gap-2">
            <Boton variante="fantasma" onClick={volver}>
              Volver
            </Boton>
            {publicada ? (
              <Boton
                variante="secundario"
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
          <p className="mb-4 text-sm text-tinta-suave">
            Probar proceso crea una instancia real persistente de la versión publicada v
            {publicada.numero}. No utiliza los cambios del borrador.
          </p>
        ) : null}
        {aviso ? (
          <div className="aparecer mb-4 flex items-center gap-2 rounded-xl border border-exito-borde bg-exito-tenue px-4 py-3 text-sm text-exito">
            <IconoCheck tamano={15} />
            {aviso}
          </div>
        ) : null}
        {error ? (
          <div
            role="alert"
            className="aparecer mb-4 rounded-xl border border-rojo-borde bg-rojo-tenue px-4 py-3 text-sm text-rojo"
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
              descripcion={`La ultima version (${publicada ? "v" + publicada.numero + " publicada" : "sin publicar"}) esta cerrada. Para cambiarla se crea una version nueva que arranca igual.`}
            />
            <div className="flex justify-end">
              <Boton
                variante="primario"
                disabled={clonar.isPending}
                onClick={() => clonar.mutate()}
              >
                Nueva version
              </Boton>
            </div>
          </Tarjeta>
        ) : (
          <Tarjeta className="mb-5">
            <CabeceraTarjeta
              titulo={`Borrador v${borrador.numero}`}
              descripcion="Editá la secuencia y guardá el borrador antes de publicar. Workflow valida el recorrido y los tipos habilitados."
            />
            {bloqueo ? <ErrorPanel mensaje={bloqueo} /> : null}
            {hayCambios ? (
              <p role="status" className="mb-3 text-sm text-tinta-suave">
                Hay cambios sin guardar. Guardá el borrador antes de publicar.
              </p>
            ) : null}
            <p className="mb-3 text-xs text-tinta-suave">
              El catálogo MVP0 permite publicar Solicitud de documento, Formulario, Revisión humana
              y Temporizador. Los demás tipos del selector pueden guardarse en borrador, pero
              Workflow impide publicarlos.
            </p>
            <fieldset disabled={editandoBloqueado} className="min-w-0">
              <legend className="sr-only">Edición del borrador</legend>
              {!bloqueo ? (
                <ol className="mt-2 space-y-2">
                  <PasoFijo etiqueta="Inicio" />
                  {pasos.map((paso, indice) => (
                    <li key={paso.id}>
                      <div className="rounded-xl border border-borde bg-white shadow-plano">
                        <div className="flex flex-wrap items-center gap-3 px-4 py-3">
                          <span className="font-mono text-xs text-tinta-tenue">
                            {String(indice + 1).padStart(2, "0")}
                          </span>
                          <Pastilla tono="violeta">{paso.tipo}</Pastilla>
                          <span className="min-w-0 flex-1 truncate text-sm text-tinta">
                            {paso.nombre || paso.tipo}
                          </span>
                          <span className="flex gap-1">
                            <button
                              type="button"
                              aria-label="Subir"
                              disabled={indice === 0}
                              onClick={() => mover(paso.id, -1)}
                              className="rounded-lg p-1.5 text-tinta-suave transition hover:bg-lienzo hover:text-tinta disabled:opacity-30"
                            >
                              <IconoFlechaArriba tamano={14} />
                            </button>
                            <button
                              type="button"
                              aria-label="Bajar"
                              disabled={indice === pasos.length - 1}
                              onClick={() => mover(paso.id, 1)}
                              className="rounded-lg p-1.5 text-tinta-suave transition hover:bg-lienzo hover:text-tinta disabled:opacity-30"
                            >
                              <IconoFlechaAbajo tamano={14} />
                            </button>
                            <button
                              type="button"
                              aria-label="Quitar"
                              onClick={() =>
                                setPasos((actuales) =>
                                  actuales.filter((otro) => otro.id !== paso.id),
                                )
                              }
                              className="rounded-lg p-1.5 text-tinta-suave transition hover:bg-rojo-tenue hover:text-rojo"
                            >
                              ×
                            </button>
                          </span>
                          <Boton
                            variante="fantasma"
                            tamano="sm"
                            onClick={() => setExpandido(expandido === paso.id ? null : paso.id)}
                          >
                            {expandido === paso.id ? "Ocultar" : "Configurar"}
                          </Boton>
                        </div>
                        {expandido === paso.id ? (
                          <ConfiguracionPaso
                            paso={paso}
                            alCambiar={(cambio) =>
                              setPasos((actuales) =>
                                actuales.map((otro) =>
                                  otro.id === paso.id ? { ...otro, ...cambio } : otro,
                                ),
                              )
                            }
                          />
                        ) : null}
                      </div>
                    </li>
                  ))}
                  <PasoFijo etiqueta="Fin" />
                </ol>
              ) : null}
              <div className="mt-4 flex flex-wrap items-end justify-between gap-3">
                <Selector
                  etiqueta="Agregar paso"
                  value=""
                  onChange={(evento) => {
                    if (evento.target.value) {
                      agregarPaso(evento.target.value as TipoNodoProceso);
                      evento.target.value = "";
                    }
                  }}
                  className="w-56"
                >
                  <option value="">Elegi un tipo de paso...</option>
                  {TIPOS_PASO.map((opcion) => (
                    <option key={opcion.valor} value={opcion.valor}>
                      {opcion.texto}
                    </option>
                  ))}
                </Selector>
                <span className="flex gap-2">
                  <Boton
                    variante="secundario"
                    disabled={guardar.isPending}
                    onClick={() => guardar.mutate()}
                  >
                    Guardar borrador
                  </Boton>
                  <Boton
                    variante="primario"
                    disabled={editandoBloqueado || hayCambios || pasos.length === 0}
                    onClick={() => publicar.mutate()}
                  >
                    Publicar v{borrador.numero}
                  </Boton>
                </span>
              </div>
            </fieldset>
          </Tarjeta>
        )}

        {publicada ? (
          <Tarjeta>
            <CabeceraTarjeta
              titulo={`Version publicada v${publicada.numero}`}
              descripcion="Las instancias nuevas arrancan aca. Una instancia que ya empezo sigue en su version aunque publiques otra."
            />
            <p className="mt-1 font-mono text-xs text-tinta-suave">hash {publicada.hash ?? "—"}</p>
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
            />
          )
        ) : null}
      </Contenido>
    </>
  );
}

function PasoFijo({ etiqueta }: { etiqueta: string }) {
  return (
    <li className="flex items-center gap-3 rounded-xl border border-dashed border-borde-fuerte bg-lienzo/60 px-4 py-2.5">
      <span className="size-2 rounded-full bg-grafito" />
      <span className="text-xs font-semibold uppercase tracking-wider text-tinta-suave">{etiqueta}</span>
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
    <div className="grid gap-3 border-t border-borde bg-lienzo/50 px-4 py-4 sm:grid-cols-3">
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
          onChange={(evento) => alCambiar({ tipoDocumento: evento.target.value })}
        />
      ) : null}
      {paso.tipo === "SUBPROCESO" ? (
        <Campo
          etiqueta="Codigo del subproceso"
          placeholder="COMEX-OV-DG"
          value={paso.subprocesoCodigo ?? ""}
          onChange={(evento) => alCambiar({ subprocesoCodigo: evento.target.value.toUpperCase() })}
        />
      ) : null}
      {["SOLICITUD_DOCUMENTO", "FORMULARIO", "VALIDACION_IA", "REVISION_HUMANA", "TAREA_EXTERNA"].includes(
        paso.tipo,
      ) ? (
        <Campo
          etiqueta="Responsable"
          placeholder="comex@empresa.com o rol"
          value={paso.asignadoA ?? ""}
          onChange={(evento) => alCambiar({ asignadoA: evento.target.value })}
        />
      ) : null}
      {["SOLICITUD_DOCUMENTO", "FORMULARIO", "VALIDACION_IA", "REVISION_HUMANA", "TAREA_EXTERNA", "TEMPORIZADOR"].includes(
        paso.tipo,
      ) ? (
        <Campo
          etiqueta="SLA (horas)"
          type="number"
          min={1}
          value={paso.slaHoras ?? ""}
          onChange={(evento) =>
            alCambiar({ slaHoras: evento.target.value ? Number(evento.target.value) : undefined })
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
}: {
  instancia?: InstanciaProceso;
  alCompletar: (tareaId: string, decision?: string, motivo?: string) => Promise<boolean>;
  completando: boolean;
}) {
  const [tareaRechazo, setTareaRechazo] = useState<string | null>(null);
  const [motivo, setMotivo] = useState("");
  const enviando = useRef(false);

  async function enviar(tareaId: string, decision?: string) {
    if (enviando.current || completando) return;
    const motivoFinal = decision === "RECHAZADO" ? motivo.trim() : undefined;
    if (decision === "RECHAZADO" && (!motivoFinal || motivoFinal.length > 512)) return;
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
      <Tarjeta className="mt-5">
        <Cargando filas={2} />
      </Tarjeta>
    );
  }
  const finalizada = instancia.estado === "COMPLETADA" || instancia.estado === "CANCELADA";
  const permiteCompletar = instancia.estado === "ACTIVA" || instancia.estado === "ESPERANDO";
  const pendientes = instancia.tareas.filter(
    (tarea) => tarea.estado === "PENDIENTE" || tarea.estado === "VENCIDA",
  );
  return (
    <Tarjeta className="mt-5">
      <CabeceraTarjeta
        titulo={`Prueba: instancia ${instancia.id.slice(0, 8)}`}
        descripcion={`Estado ${instancia.estado} · v${instancia.numeroVersion}`}
      />
      {finalizada ? (
        <p role="status" className="mt-2 text-sm text-tinta-suave">
          La instancia terminó {instancia.estado.toLowerCase()}.
        </p>
      ) : (
        <>
          <p role="status" className="mt-2 text-sm text-tinta-suave">
            {instancia.estado === "BLOQUEADA"
              ? "La instancia está bloqueada y no admite completar tareas."
              : instancia.estado === "CREADA"
                ? "La instancia está creada y todavía no admite completar tareas."
                : instancia.estado === "ESPERANDO"
                  ? "La instancia está esperando la resolución de tareas."
                  : "La instancia está activa."}
          </p>
          {pendientes.length ? (
            <ul className="mt-2 space-y-2">
              {pendientes.map((tarea) => (
                <li
                  key={tarea.id}
                  className="rounded-xl border border-borde bg-white px-4 py-3 shadow-plano"
                >
                  <div className="flex flex-wrap items-center gap-3">
                    <Pastilla tono="violeta">{tarea.tipoNodo}</Pastilla>
                    <Pastilla tono={tarea.estado === "VENCIDA" ? "alerta" : "neutro"}>
                      {tarea.estado}
                    </Pastilla>
                    <span className="min-w-0 flex-1 truncate text-sm text-tinta">
                      {tarea.nodoId}
                    </span>
                    {permiteCompletar ? (
                      tarea.tipoNodo === "REVISION_HUMANA" ? (
                        <>
                          <Boton
                            tamano="sm"
                            disabled={completando}
                            onClick={() => enviar(tarea.id, "APROBADO")}
                          >
                            Aprobar
                          </Boton>
                          <Boton
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
                        <Boton tamano="sm" disabled={completando} onClick={() => enviar(tarea.id)}>
                          Completar
                        </Boton>
                      )
                    ) : null}
                  </div>
                  {permiteCompletar && tareaRechazo === tarea.id ? (
                    <form
                      className="mt-3 space-y-3"
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
                      <div className="flex gap-2">
                        <Boton
                          type="submit"
                          tamano="sm"
                          cargando={completando}
                          disabled={!motivo.trim() || motivo.trim().length > 512}
                        >
                          Confirmar rechazo
                        </Boton>
                        <Boton
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
            <p className="mt-2 text-sm text-tinta-suave">Sin tareas pendientes o vencidas.</p>
          )}
          {completando ? (
            <p role="status" aria-busy="true" className="mt-2 text-sm text-tinta-suave">
              Enviando decisión…
            </p>
          ) : null}
        </>
      )}
    </Tarjeta>
  );
}

function erroresDeValidacion(fallo: unknown): string[] {
  const detalles = (fallo as { response?: { data?: { detalles?: string[] } } })?.response?.data
    ?.detalles;
  return Array.isArray(detalles) ? detalles : [];
}
