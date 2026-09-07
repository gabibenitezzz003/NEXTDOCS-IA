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
  NodoProceso,
  TipoNodoProceso,
  VersionProceso,
} from "../api/procesos";
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

interface Paso {
  id: string;
  tipo: TipoNodoProceso;
  nombre: string;
  tipoDocumento?: string;
  subprocesoCodigo?: string;
  asignadoA?: string;
  slaHoras?: number;
}

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
  const publicadas = versiones.filter((version) => version.estado === "PUBLICADA").length;
  const borrador = versiones.some((version) => version.estado === "BORRADOR");
  return (
    <div className="flex items-center gap-1.5">
      {publicadas ? <Pastilla tono="exito">v{publicadas} publicada{publicadas > 1 ? "s" : ""}</Pastilla> : null}
      {borrador ? <Pastilla tono="alerta">borrador</Pastilla> : null}
      {!publicadas && !borrador ? <Pastilla tono="neutro">archivada</Pastilla> : null}
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

  const versionCargada = useRef<string | null>(null);
  useEffect(() => {
    if (borrador && versionCargada.current !== borrador.id) {
      versionCargada.current = borrador.id;
      setPasos(desdeGrafo(borrador.grafo));
    }
  }, [borrador]);

  const refrescar = () => clienteConsultas.invalidateQueries({ queryKey: ["proceso", procesoId] });

  const guardar = useMutation({
    mutationFn: () => {
      const grafo = serializar(pasos);
      return actualizarGrafo(borrador!.id, grafo);
    },
    onSuccess: () => {
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
    mutationFn: () => publicarVersion(borrador!.id),
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
  });

  const completar = useMutation({
    mutationFn: ({ tareaId, decision }: { tareaId: string; decision?: string }) =>
      completarTarea(tareaId, { actor: sesion?.email ?? "estudio", decision }),
    onSuccess: () => {
      clienteConsultas.invalidateQueries({ queryKey: ["instancia-prueba", instanciaPrueba] });
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  function alCompletar(tareaId: string, decision?: string) {
    completar.mutate({ tareaId, decision });
  }

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
        <ErrorPanel mensaje={mensajeDeError(consulta.error)} reintentar={() => consulta.refetch()} />
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
            <Boton variante="fantasma" onClick={alVolver}>
              Volver
            </Boton>
            {publicada ? (
              <Boton variante="secundario" disabled={probar.isPending} onClick={() => probar.mutate()}>
                Probar
              </Boton>
            ) : null}
          </div>
        }
      />
      <Contenido>
        {aviso ? (
          <div className="aparecer mb-4 flex items-center gap-2 rounded-xl border border-exito-borde bg-exito-tenue px-4 py-3 text-sm text-exito">
            <IconoCheck tamano={15} />
            {aviso}
          </div>
        ) : null}
        {error ? (
          <div className="aparecer mb-4 rounded-xl border border-rojo-borde bg-rojo-tenue px-4 py-3 text-sm text-rojo">
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
              <Boton variante="primario" disabled={clonar.isPending} onClick={() => clonar.mutate()}>
                Nueva version
              </Boton>
            </div>
          </Tarjeta>
        ) : (
          <Tarjeta className="mb-5">
            <CabeceraTarjeta
              titulo={`Borrador v${borrador.numero}`}
              descripcion="Los pasos corren en orden. Cada paso pausa el proceso hasta que alguien lo completa; la publicacion valida el recorrido."
            />
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
                            setPasos((actuales) => actuales.filter((otro) => otro.id !== paso.id))
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
                      <ConfiguracionPaso paso={paso} alCambiar={(cambio) =>
                        setPasos((actuales) =>
                          actuales.map((otro) => (otro.id === paso.id ? { ...otro, ...cambio } : otro)),
                        )
                      } />
                    ) : null}
                  </div>
                </li>
              ))}
              <PasoFijo etiqueta="Fin" />
            </ol>
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
                <Boton variante="secundario" disabled={guardar.isPending} onClick={() => guardar.mutate()}>
                  Guardar borrador
                </Boton>
                <Boton
                  variante="primario"
                  disabled={guardar.isPending || publicar.isPending || pasos.length === 0}
                  onClick={() => publicar.mutate()}
                >
                  Publicar v{borrador.numero}
                </Boton>
              </span>
            </div>
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
          <PanelPrueba instancia={consultaInstancia.data} alCompletar={alCompletar} />
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
}: {
  instancia?: InstanciaProceso;
  alCompletar: (tareaId: string, decision?: string) => void;
}) {
  if (!instancia) {
    return (
      <Tarjeta className="mt-5">
        <Cargando filas={2} />
      </Tarjeta>
    );
  }
  const pendientes = instancia.tareas.filter((tarea) => tarea.estado === "PENDIENTE");
  return (
    <Tarjeta className="mt-5">
      <CabeceraTarjeta
        titulo={`Prueba: instancia ${instancia.id.slice(0, 8)}`}
        descripcion={`Estado ${instancia.estado} · v${instancia.numeroVersion}`}
      />
      {instancia.estado !== "ACTIVA" ? (
        <p className="mt-2 rounded-xl border border-exito-borde bg-exito-tenue px-4 py-3 text-sm text-exito">
          La instancia termino {instancia.estado.toLowerCase()}.
        </p>
      ) : pendientes.length ? (
        <ul className="mt-2 space-y-2">
          {pendientes.map((tarea) => (
            <li
              key={tarea.id}
              className="flex flex-wrap items-center gap-3 rounded-xl border border-borde bg-white px-4 py-3 shadow-plano"
            >
              <Pastilla tono="violeta">{tarea.tipoNodo}</Pastilla>
              <span className="min-w-0 flex-1 truncate text-sm text-tinta">{tarea.nodoId}</span>
              {tarea.tipoNodo === "REVISION_HUMANA" ? (
                <>
                  <Boton
                    tamano="sm"
                    onClick={() => alCompletar(tarea.id, "APROBADO")}
                  >
                    Aprobar
                  </Boton>
                  <Boton
                    tamano="sm"
                    onClick={() => alCompletar(tarea.id, "RECHAZADO")}
                  >
                    Rechazar
                  </Boton>
                </>
              ) : (
                <Boton tamano="sm" onClick={() => alCompletar(tarea.id)}>
                  Completar
                </Boton>
              )}
            </li>
          ))}
        </ul>
      ) : (
        <p className="mt-2 text-sm text-tinta-suave">
          Sin tareas pendientes. {instancia.estado === "ACTIVA" ? "La instancia esta avanzando." : ""}
        </p>
      )}
    </Tarjeta>
  );
}

function serializar(pasos: Paso[]): GrafoProceso {
  const nodos: NodoProceso[] = [{ id: "inicio", tipo: "INICIO", nombre: "Inicio" }];
  const aristas: { origen: string; destino: string }[] = [];
  let anterior = "inicio";
  for (const paso of pasos) {
    const configuracion: Record<string, unknown> = {};
    if (paso.tipoDocumento) configuracion.tipoDocumento = paso.tipoDocumento;
    if (paso.subprocesoCodigo) configuracion.subprocesoCodigo = paso.subprocesoCodigo;
    if (paso.asignadoA) configuracion.asignadoA = paso.asignadoA;
    if (paso.slaHoras !== undefined) configuracion.slaHoras = paso.slaHoras;
    nodos.push({ id: paso.id, tipo: paso.tipo, nombre: paso.nombre, configuracion });
    aristas.push({ origen: anterior, destino: paso.id });
    anterior = paso.id;
  }
  nodos.push({ id: "fin", tipo: "FIN", nombre: "Fin" });
  aristas.push({ origen: anterior, destino: "fin" });
  return { nodos, aristas };
}

function desdeGrafo(grafo: GrafoProceso | undefined): Paso[] {
  if (!grafo || !grafo.nodos.length) {
    return [];
  }
  const porId = new Map(grafo.nodos.map((nodo) => [nodo.id, nodo]));
  const salidas = new Map<string, string>();
  for (const arista of grafo.aristas ?? []) {
    if (!salidas.has(arista.origen)) {
      salidas.set(arista.origen, arista.destino);
    }
  }
  const pasos: Paso[] = [];
  const inicio = grafo.nodos.find((nodo) => nodo.tipo === "INICIO");
  let actual = inicio ? salidas.get(inicio.id) : undefined;
  let vueltas = 0;
  while (actual && porId.get(actual)?.tipo !== "FIN" && vueltas < 100) {
    vueltas++;
    const nodo = porId.get(actual);
    if (!nodo) {
      break;
    }
    const configuracion = (nodo.configuracion ?? {}) as Record<string, string | number>;
    pasos.push({
      id: nodo.id,
      tipo: nodo.tipo,
      nombre: nodo.nombre ?? nodo.tipo,
      tipoDocumento: configuracion.tipoDocumento as string | undefined,
      subprocesoCodigo: configuracion.subprocesoCodigo as string | undefined,
      asignadoA: configuracion.asignadoA as string | undefined,
      slaHoras: typeof configuracion.slaHoras === "number" ? configuracion.slaHoras : undefined,
    });
    actual = salidas.get(actual);
  }
  return pasos;
}

function erroresDeValidacion(fallo: unknown): string[] {
  const detalles = (fallo as { response?: { data?: { detalles?: string[] } } })?.response?.data
    ?.detalles;
  return Array.isArray(detalles) ? detalles : [];
}
