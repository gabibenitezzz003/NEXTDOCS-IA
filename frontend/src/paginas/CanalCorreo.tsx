import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Contenido, Encabezado } from "../componentes/Disposicion";
import { Cargando, ErrorPanel, Vacio } from "../componentes/Estados";
import {
  Boton,
  CabeceraTarjeta,
  Campo,
  GrupoSegmentado,
  Panel,
  Pastilla,
  Selector,
  Tarjeta,
} from "../componentes/Interfaz";
import {
  IconoAdjunto,
  IconoCheck,
  IconoCorreo,
  IconoDerecha,
  IconoEnlace,
  IconoEscudo,
  IconoIzquierda,
  IconoMas,
  IconoRecargar,
  IconoReloj,
} from "../componentes/Iconos";
import {
  anularCorrelacion,
  autorizarRemitente,
  cambiarEstadoBuzon,
  crearBuzon,
  crearCorrelacion,
  leerBuzon,
  listarBuzones,
  listarCorrelaciones,
  listarMensajes,
  obtenerMensaje,
  probarBuzon,
  revocarRemitente,
} from "../api/canalCorreo";
import { mensajeDeError } from "../api/cliente";
import { formatearFecha } from "./Documentos";
import { VisorDocumento } from "./VisorDocumento";
import { useSesion } from "../contextos/ProveedorSesion";
import type {
  BuzonCorreo,
  EstadoBuzonCorreo,
  LecturaBuzon,
  MensajeCorreo,
  ResultadoAdjuntoCorreo,
  ResultadoMensajeCorreo,
} from "../tipos/api";
import type { Tono } from "../componentes/Interfaz";

type Pestana = "bandeja" | "buzones" | "correlaciones";

const TONO_RESULTADO: Record<ResultadoMensajeCorreo, Tono> = {
  INGESTADO: "exito",
  PARCIAL: "alerta",
  SIN_ADJUNTOS: "neutro",
  REMITENTE_NO_AUTORIZADO: "rojo",
  SIN_CORRELACION: "alerta",
  RECHAZADO: "rojo",
  ERROR: "rojo",
};

const TONO_ADJUNTO: Record<ResultadoAdjuntoCorreo, Tono> = {
  INGESTADO: "exito",
  EN_CUARENTENA: "rojo",
  RECHAZADO: "rojo",
  ERROR: "rojo",
};

const TONO_BUZON: Record<EstadoBuzonCorreo, Tono> = {
  ACTIVO: "exito",
  PAUSADO: "neutro",
  ERROR: "rojo",
};

const FILTROS: { valor: ResultadoMensajeCorreo | "TODOS"; texto: string }[] = [
  { valor: "TODOS", texto: "Todo" },
  { valor: "INGESTADO", texto: "Ingestado" },
  { valor: "SIN_CORRELACION", texto: "Sin asociar" },
  { valor: "REMITENTE_NO_AUTORIZADO", texto: "No autorizado" },
  { valor: "RECHAZADO", texto: "Rechazado" },
];

function presentarTamano(bytes: number) {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  const unidades = ["KB", "MB", "GB"];
  let valor = bytes / 1024;
  let indice = 0;
  while (valor >= 1024 && indice < unidades.length - 1) {
    valor /= 1024;
    indice += 1;
  }
  return `${valor.toFixed(1)} ${unidades[indice]}`;
}

export function CanalCorreo() {
  const { tienePermiso } = useSesion();
  const [pestana, setPestana] = useState<Pestana>("bandeja");
  const puedeAdministrar = tienePermiso("canales.administrar");

  return (
    <>
      <Encabezado
        titulo="Canal de email"
        descripcion="Buzon dedicado por tenant. Ningun adjunto entra sin pasar por los mismos controles que la API."
        acciones={
          <GrupoSegmentado
            opciones={[
              { valor: "bandeja" as Pestana, texto: "Bandeja" },
              { valor: "buzones" as Pestana, texto: "Buzones" },
              { valor: "correlaciones" as Pestana, texto: "Solicitudes" },
            ]}
            valor={pestana}
            alCambiar={setPestana}
          />
        }
      />
      <Contenido>
        {pestana === "bandeja" ? <Bandeja /> : null}
        {pestana === "buzones" ? <Buzones puedeAdministrar={puedeAdministrar} /> : null}
        {pestana === "correlaciones" ? (
          <Correlaciones puedeAdministrar={puedeAdministrar} />
        ) : null}
      </Contenido>
    </>
  );
}

function Bandeja() {
  const [filtro, setFiltro] = useState<ResultadoMensajeCorreo | "TODOS">("TODOS");
  const [pagina, setPagina] = useState(0);
  const [abierto, setAbierto] = useState<string | null>(null);

  const consulta = useQuery({
    queryKey: ["correo", "mensajes", filtro, pagina],
    queryFn: () => listarMensajes(filtro === "TODOS" ? undefined : filtro, pagina, 20),
  });

  const mensajes = consulta.data?.content ?? [];
  const totalPaginas = consulta.data?.totalPages ?? 0;

  return (
    <>
      <div className="mb-4">
        <GrupoSegmentado
          opciones={FILTROS}
          valor={filtro}
          alCambiar={(nuevo) => {
            setFiltro(nuevo);
            setPagina(0);
          }}
        />
      </div>

      {consulta.isPending ? (
        <Cargando filas={5} alto="h-24" />
      ) : consulta.isError ? (
        <ErrorPanel mensaje={mensajeDeError(consulta.error)} reintentar={() => consulta.refetch()} />
      ) : mensajes.length === 0 ? (
        <Vacio
          titulo="No hay correos en este filtro"
          detalle="Cada mensaje que llegue al buzon queda registrado aca, tambien los que se rechazan."
        />
      ) : (
        <>
          <ul className="space-y-3">
            {mensajes.map((mensaje) => (
              <li key={mensaje.id}>
                <button
                  type="button"
                  onClick={() => setAbierto(mensaje.id)}
                  className="w-full rounded-2xl border border-borde bg-white px-5 py-4 text-left shadow-tarjeta transition hover:-translate-y-0.5 hover:border-borde-fuerte hover:shadow-elevado"
                >
                  <div className="flex flex-wrap items-start justify-between gap-4">
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-1.5">
                        <Pastilla tono={TONO_RESULTADO[mensaje.resultado]}>
                          {mensaje.resultado.replace(/_/g, " ")}
                        </Pastilla>
                        {mensaje.tokenDetectado ? (
                          <span className="inline-flex items-center gap-1 rounded-full bg-lienzo px-2.5 py-0.5 font-mono text-[11px] text-tinta-suave ring-1 ring-inset ring-borde">
                            <IconoEnlace tamano={11} />
                            {mensaje.tokenDetectado}
                          </span>
                        ) : null}
                      </div>

                      <p className="mt-2.5 truncate text-sm font-semibold text-tinta">
                        {mensaje.asunto || "(sin asunto)"}
                      </p>
                      <p className="mt-1 flex flex-wrap items-center gap-x-2.5 gap-y-1 text-xs text-tinta-suave">
                        <span className="font-medium text-tinta-media">{mensaje.remitente}</span>
                        <span className="flex items-center gap-1">
                          <IconoReloj tamano={12} />
                          {formatearFecha(mensaje.alta)}
                        </span>
                      </p>
                      {mensaje.motivo ? (
                        <p className="mt-2 text-xs leading-relaxed text-tinta-suave">
                          {mensaje.motivo}
                        </p>
                      ) : null}
                    </div>

                    <div className="flex shrink-0 items-center gap-4 text-center">
                      <div>
                        <p className="cifra text-xl leading-none text-exito">{mensaje.ingestados}</p>
                        <p className="mt-1 text-[10px] uppercase tracking-wider text-tinta-suave">
                          ingestados
                        </p>
                      </div>
                      <div>
                        <p
                          className={`cifra text-xl leading-none ${
                            mensaje.rechazados ? "text-rojo" : "text-tinta-tenue"
                          }`}
                        >
                          {mensaje.rechazados}
                        </p>
                        <p className="mt-1 text-[10px] uppercase tracking-wider text-tinta-suave">
                          rechazados
                        </p>
                      </div>
                    </div>
                  </div>
                </button>
              </li>
            ))}
          </ul>

          {totalPaginas > 1 ? (
            <div className="mt-4 flex items-center justify-end gap-2">
              <Boton tamano="sm" disabled={pagina === 0} onClick={() => setPagina((a) => a - 1)}>
                <IconoIzquierda tamano={14} />
                Anterior
              </Boton>
              <span className="px-1 text-xs tabular-nums text-tinta-suave">
                {pagina + 1} de {totalPaginas}
              </span>
              <Boton
                tamano="sm"
                disabled={pagina + 1 >= totalPaginas}
                onClick={() => setPagina((a) => a + 1)}
              >
                Siguiente
                <IconoDerecha tamano={14} />
              </Boton>
            </div>
          ) : null}
        </>
      )}

      {abierto ? <DetalleMensaje mensajeId={abierto} alCerrar={() => setAbierto(null)} /> : null}
    </>
  );
}

function DetalleMensaje({ mensajeId, alCerrar }: { mensajeId: string; alCerrar: () => void }) {
  const [documento, setDocumento] = useState<string | null>(null);
  const consulta = useQuery({
    queryKey: ["correo", "mensaje", mensajeId],
    queryFn: () => obtenerMensaje(mensajeId),
  });

  return (
    <>
      <Panel titulo="Correo recibido" descripcion="Que hizo el canal con cada adjunto" alCerrar={alCerrar}>
        {consulta.isPending ? (
          <Cargando filas={4} alto="h-16" />
        ) : consulta.isError ? (
          <ErrorPanel mensaje={mensajeDeError(consulta.error)} />
        ) : consulta.data ? (
          <DetalleMensajeCuerpo mensaje={consulta.data} alAbrirDocumento={setDocumento} />
        ) : null}
      </Panel>
      {documento ? (
        <VisorDocumento documentoId={documento} alCerrar={() => setDocumento(null)} />
      ) : null}
    </>
  );
}

function DetalleMensajeCuerpo({
  mensaje,
  alAbrirDocumento,
}: {
  mensaje: MensajeCorreo;
  alAbrirDocumento: (id: string) => void;
}) {
  return (
    <div className="space-y-5">
      <div>
        <Pastilla tono={TONO_RESULTADO[mensaje.resultado]} solido>
          {mensaje.resultado.replace(/_/g, " ")}
        </Pastilla>
        <p className="mt-3 text-sm font-semibold leading-snug text-tinta">
          {mensaje.asunto || "(sin asunto)"}
        </p>
      </div>

      <dl className="space-y-2 rounded-xl border border-borde bg-lienzo/60 p-4 text-xs">
        <Dato etiqueta="De" valor={mensaje.remitente} />
        <Dato etiqueta="Para" valor={mensaje.destinatarios} />
        <Dato etiqueta="Buzon" valor={mensaje.buzonDireccion} />
        <Dato etiqueta="Recibido" valor={formatearFecha(mensaje.alta)} />
        <Dato etiqueta="Token" valor={mensaje.tokenDetectado ?? "sin token"} monoespaciado />
        <Dato etiqueta="Message-ID" valor={mensaje.identificadorMensaje} monoespaciado />
      </dl>

      {mensaje.motivo ? (
        <div className="rounded-xl border border-ambar-borde bg-ambar-tenue px-4 py-3 text-xs leading-relaxed text-tinta">
          {mensaje.motivo}
        </div>
      ) : null}

      <div>
        <h3 className="mb-2 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-tinta-suave">
          <IconoAdjunto tamano={13} />
          Adjuntos ({mensaje.detalleAdjuntos.length})
        </h3>
        {mensaje.detalleAdjuntos.length === 0 ? (
          <p className="rounded-xl border border-dashed border-borde px-4 py-6 text-center text-xs text-tinta-suave">
            El mensaje no dejo ningun adjunto registrado.
          </p>
        ) : (
          <ul className="space-y-2">
            {mensaje.detalleAdjuntos.map((adjunto) => (
              <li key={adjunto.id} className="rounded-xl border border-borde bg-white p-3.5">
                <div className="flex items-start justify-between gap-3">
                  <p className="min-w-0 flex-1 truncate text-sm font-medium text-tinta">
                    {adjunto.nombreArchivo}
                  </p>
                  <Pastilla tono={TONO_ADJUNTO[adjunto.resultado]}>
                    {adjunto.resultado.replace(/_/g, " ")}
                  </Pastilla>
                </div>
                <p className="mt-1 text-[11px] text-tinta-suave">
                  {presentarTamano(adjunto.tamanoBytes)}
                  {adjunto.codigoRechazo ? ` · ${adjunto.codigoRechazo}` : ""}
                </p>
                {adjunto.motivo ? (
                  <p className="mt-1.5 text-xs leading-relaxed text-tinta-suave">{adjunto.motivo}</p>
                ) : null}
                {adjunto.sha256 ? (
                  <p className="mt-1.5 truncate font-mono text-[10px] text-tinta-tenue">
                    sha256 {adjunto.sha256}
                  </p>
                ) : null}
                {adjunto.documentoId ? (
                  <Boton
                    tamano="sm"
                    className="mt-2.5"
                    onClick={() => alAbrirDocumento(adjunto.documentoId!)}
                  >
                    Ver documento
                  </Boton>
                ) : null}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

function Dato({
  etiqueta,
  valor,
  monoespaciado = false,
}: {
  etiqueta: string;
  valor?: string;
  monoespaciado?: boolean;
}) {
  if (!valor) {
    return null;
  }
  return (
    <div className="flex gap-3">
      <dt className="w-20 shrink-0 font-semibold text-tinta-suave">{etiqueta}</dt>
      <dd className={`min-w-0 flex-1 break-all text-tinta ${monoespaciado ? "font-mono" : ""}`}>
        {valor}
      </dd>
    </div>
  );
}

function Buzones({ puedeAdministrar }: { puedeAdministrar: boolean }) {
  const clienteConsultas = useQueryClient();
  const [creando, setCreando] = useState(false);
  const [aviso, setAviso] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const consulta = useQuery({ queryKey: ["correo", "buzones"], queryFn: listarBuzones });
  const refrescar = () => clienteConsultas.invalidateQueries({ queryKey: ["correo"] });

  const probar = useMutation({
    mutationFn: probarBuzon,
    onSuccess: () => {
      setError(null);
      setAviso("Conexion IMAP verificada");
    },
    onError: (fallo) => {
      setAviso(null);
      setError(mensajeDeError(fallo));
    },
  });

  const leer = useMutation({
    mutationFn: leerBuzon,
    onSuccess: (lectura: LecturaBuzon) => {
      setError(lectura.error ?? null);
      setAviso(
        lectura.error
          ? null
          : `${lectura.mensajesLeidos} mensaje${lectura.mensajesLeidos === 1 ? "" : "s"} leido${
              lectura.mensajesLeidos === 1 ? "" : "s"
            }, ${lectura.documentosIngestados} documento${
              lectura.documentosIngestados === 1 ? "" : "s"
            } ingestado${lectura.documentosIngestados === 1 ? "" : "s"}`,
      );
      refrescar();
    },
    onError: (fallo) => {
      setAviso(null);
      setError(mensajeDeError(fallo));
    },
  });

  const cambiarEstado = useMutation({
    mutationFn: ({ id, estado }: { id: string; estado: EstadoBuzonCorreo }) =>
      cambiarEstadoBuzon(id, estado),
    onSuccess: refrescar,
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const buzones = consulta.data ?? [];

  return (
    <>
      {aviso ? (
        <div className="aparecer mb-4 flex items-center gap-2 rounded-xl border border-exito-borde bg-exito-tenue px-4 py-3 text-sm text-exito">
          <IconoCheck tamano={15} />
          {aviso}
        </div>
      ) : null}
      {error ? (
        <div className="aparecer mb-4 rounded-xl border border-rojo-borde bg-rojo-tenue px-4 py-3 text-sm text-rojo">
          {error}
        </div>
      ) : null}

      {puedeAdministrar ? (
        <div className="mb-4 flex justify-end">
          <Boton variante="primario" onClick={() => setCreando(true)}>
            <IconoMas tamano={14} />
            Nuevo buzon
          </Boton>
        </div>
      ) : null}

      {consulta.isPending ? (
        <Cargando filas={2} alto="h-44" />
      ) : consulta.isError ? (
        <ErrorPanel mensaje={mensajeDeError(consulta.error)} reintentar={() => consulta.refetch()} />
      ) : buzones.length === 0 ? (
        <Vacio
          titulo="Todavia no hay buzones"
          detalle="Un buzon dedicado evita que la documentacion entre por la casilla personal de alguien."
        />
      ) : (
        <div className="grid gap-4 lg:grid-cols-2">
          {buzones.map((buzon) => (
            <TarjetaBuzon
              key={buzon.id}
              buzon={buzon}
              puedeAdministrar={puedeAdministrar}
              ocupado={probar.isPending || leer.isPending}
              alProbar={() => probar.mutate(buzon.id)}
              alLeer={() => leer.mutate(buzon.id)}
              alCambiarEstado={(estado) => cambiarEstado.mutate({ id: buzon.id, estado })}
              alFallar={setError}
            />
          ))}
        </div>
      )}

      {creando ? (
        <FormularioBuzon
          alCerrar={() => setCreando(false)}
          alCrear={() => {
            setCreando(false);
            setAviso("Buzon creado");
            refrescar();
          }}
        />
      ) : null}
    </>
  );
}

function TarjetaBuzon({
  buzon,
  puedeAdministrar,
  ocupado,
  alProbar,
  alLeer,
  alCambiarEstado,
  alFallar,
}: {
  buzon: BuzonCorreo;
  puedeAdministrar: boolean;
  ocupado: boolean;
  alProbar: () => void;
  alLeer: () => void;
  alCambiarEstado: (estado: EstadoBuzonCorreo) => void;
  alFallar: (mensaje: string) => void;
}) {
  const clienteConsultas = useQueryClient();
  const [patron, setPatron] = useState("");

  const autorizar = useMutation({
    mutationFn: () => autorizarRemitente(buzon.id, patron.trim()),
    onSuccess: () => {
      setPatron("");
      clienteConsultas.invalidateQueries({ queryKey: ["correo", "buzones"] });
    },
    onError: (fallo) => alFallar(mensajeDeError(fallo)),
  });

  const revocar = useMutation({
    mutationFn: revocarRemitente,
    onSuccess: () => clienteConsultas.invalidateQueries({ queryKey: ["correo", "buzones"] }),
    onError: (fallo) => alFallar(mensajeDeError(fallo)),
  });

  return (
    <Tarjeta className="flex flex-col">
      <CabeceraTarjeta
        titulo={buzon.nombre}
        descripcion={buzon.direccion}
        acciones={<Pastilla tono={TONO_BUZON[buzon.estado]}>{buzon.estado}</Pastilla>}
      />

      <div className="mt-4 grid grid-cols-2 gap-3 text-xs">
        <div>
          <p className="text-[10px] uppercase tracking-wider text-tinta-suave">Entrada</p>
          <p className="mt-0.5 truncate font-mono text-tinta">
            {buzon.hostEntrada}:{buzon.puertoEntrada}
          </p>
        </div>
        <div>
          <p className="text-[10px] uppercase tracking-wider text-tinta-suave">Ultima lectura</p>
          <p className="mt-0.5 text-tinta">
            {buzon.ultimaLectura ? formatearFecha(buzon.ultimaLectura) : "nunca"}
          </p>
        </div>
      </div>

      <div className="mt-3 flex flex-wrap gap-1.5">
        {buzon.exigirRemitenteAutorizado ? (
          <Pastilla tono="informacion">
            <IconoEscudo tamano={11} />
            Lista blanca
          </Pastilla>
        ) : (
          <Pastilla tono="alerta">Acepta cualquier remitente</Pastilla>
        )}
        {buzon.exigirCorrelacion ? <Pastilla tono="informacion">Exige token</Pastilla> : null}
        {buzon.acusarRecibo ? <Pastilla tono="neutro">Acusa recibo</Pastilla> : null}
        {buzon.codigoPlantillaPorDefecto ? (
          <Pastilla tono="neutro">{buzon.codigoPlantillaPorDefecto}</Pastilla>
        ) : null}
      </div>

      {buzon.ultimoError ? (
        <p className="mt-3 rounded-xl border border-rojo-borde bg-rojo-tenue px-3 py-2 text-xs leading-relaxed text-rojo">
          {buzon.fallosConsecutivos} fallo{buzon.fallosConsecutivos === 1 ? "" : "s"} seguido
          {buzon.fallosConsecutivos === 1 ? "" : "s"}: {buzon.ultimoError}
        </p>
      ) : null}

      <div className="mt-4 border-t border-borde pt-3">
        <p className="text-[10px] uppercase tracking-wider text-tinta-suave">
          Remitentes autorizados
        </p>
        {buzon.remitentes.length === 0 ? (
          <p className="mt-1.5 text-xs text-tinta-suave">
            {buzon.exigirRemitenteAutorizado
              ? "Sin remitentes cargados: el buzon rechaza todo."
              : "No hace falta: el buzon no exige lista blanca."}
          </p>
        ) : (
          <div className="mt-2 flex flex-wrap gap-1.5">
            {buzon.remitentes.map((remitente) => (
              <span
                key={remitente.id}
                className="inline-flex items-center gap-1.5 rounded-full bg-lienzo py-0.5 pl-2.5 pr-1 text-[11px] text-tinta ring-1 ring-inset ring-borde"
              >
                {remitente.patron}
                {puedeAdministrar ? (
                  <button
                    type="button"
                    aria-label={`Revocar ${remitente.patron}`}
                    onClick={() => revocar.mutate(remitente.id)}
                    className="rounded-full px-1 text-tinta-tenue transition hover:text-rojo"
                  >
                    ×
                  </button>
                ) : null}
              </span>
            ))}
          </div>
        )}

        {puedeAdministrar ? (
          <form
            onSubmit={(evento) => {
              evento.preventDefault();
              autorizar.mutate();
            }}
            className="mt-2.5 flex gap-2"
          >
            <Campo
              value={patron}
              onChange={(evento) => setPatron(evento.target.value)}
              required
              placeholder="proveedor@empresa.com o @empresa.com"
              className="h-9 text-xs"
            />
            <Boton type="submit" tamano="sm" disabled={autorizar.isPending}>
              Autorizar
            </Boton>
          </form>
        ) : null}
      </div>

      {puedeAdministrar ? (
        <div className="mt-4 flex flex-wrap gap-2 border-t border-borde pt-3">
          <Boton tamano="sm" onClick={alProbar} disabled={ocupado}>
            Probar conexion
          </Boton>
          <Boton tamano="sm" variante="primario" onClick={alLeer} disabled={ocupado}>
            <IconoRecargar tamano={13} />
            Leer ahora
          </Boton>
          <Boton
            tamano="sm"
            onClick={() => alCambiarEstado(buzon.estado === "ACTIVO" ? "PAUSADO" : "ACTIVO")}
          >
            {buzon.estado === "ACTIVO" ? "Pausar" : "Activar"}
          </Boton>
        </div>
      ) : null}
    </Tarjeta>
  );
}

function FormularioBuzon({
  alCerrar,
  alCrear,
}: {
  alCerrar: () => void;
  alCrear: () => void;
}) {
  const [error, setError] = useState<string | null>(null);
  const [datos, setDatos] = useState({
    direccion: "",
    nombre: "",
    hostEntrada: "",
    puertoEntrada: 993,
    usuarioEntrada: "",
    referenciaSecretoEntrada: "env:",
    carpeta: "INBOX",
    entradaSegura: true,
    hostSalida: "",
    puertoSalida: 587,
    usuarioSalida: "",
    referenciaSecretoSalida: "",
    salidaSegura: true,
    codigoPlantillaPorDefecto: "",
    exigirRemitenteAutorizado: true,
    exigirCorrelacion: false,
    acusarRecibo: false,
    maximoAdjuntosPorMensaje: 10,
  });

  const crear = useMutation({
    mutationFn: () =>
      crearBuzon({
        ...datos,
        usuarioEntrada: datos.usuarioEntrada || datos.direccion,
        hostSalida: datos.hostSalida || undefined,
        usuarioSalida: datos.usuarioSalida || undefined,
        referenciaSecretoSalida: datos.referenciaSecretoSalida || undefined,
        codigoPlantillaPorDefecto: datos.codigoPlantillaPorDefecto || undefined,
      }),
    onSuccess: alCrear,
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const actualizar = <C extends keyof typeof datos>(clave: C, valor: (typeof datos)[C]) =>
    setDatos((actual) => ({ ...actual, [clave]: valor }));

  return (
    <Panel
      titulo="Nuevo buzon dedicado"
      descripcion="Una direccion exclusiva para documentacion, nunca la casilla de una persona"
      alCerrar={alCerrar}
      pie={
        <div className="flex justify-end gap-2">
          <Boton onClick={alCerrar}>Cancelar</Boton>
          <Boton
            variante="primario"
            form="formulario-buzon"
            type="submit"
            disabled={crear.isPending}
          >
            {crear.isPending ? "Creando..." : "Crear buzon"}
          </Boton>
        </div>
      }
    >
      <form
        id="formulario-buzon"
        onSubmit={(evento) => {
          evento.preventDefault();
          crear.mutate();
        }}
        className="space-y-4"
      >
        {error ? (
          <div className="rounded-xl border border-rojo-borde bg-rojo-tenue px-4 py-3 text-sm text-rojo">
            {error}
          </div>
        ) : null}

        <Campo
          etiqueta="Direccion"
          type="email"
          required
          value={datos.direccion}
          onChange={(evento) => actualizar("direccion", evento.target.value)}
          placeholder="documentacion@tuempresa.com"
          ayuda="Unica en toda la instalacion: un alias resuelve a un solo tenant."
        />
        <Campo
          etiqueta="Nombre visible"
          required
          value={datos.nombre}
          onChange={(evento) => actualizar("nombre", evento.target.value)}
          placeholder="Documentacion de proveedores"
        />

        <div className="grid grid-cols-3 gap-3">
          <div className="col-span-2">
            <Campo
              etiqueta="Host IMAP"
              required
              value={datos.hostEntrada}
              onChange={(evento) => actualizar("hostEntrada", evento.target.value)}
              placeholder="imap.tuempresa.com"
            />
          </div>
          <Campo
            etiqueta="Puerto"
            type="number"
            required
            value={datos.puertoEntrada}
            onChange={(evento) => actualizar("puertoEntrada", Number(evento.target.value))}
          />
        </div>

        <Campo
          etiqueta="Usuario IMAP"
          value={datos.usuarioEntrada}
          onChange={(evento) => actualizar("usuarioEntrada", evento.target.value)}
          placeholder="por defecto, la misma direccion"
        />
        <Campo
          etiqueta="Referencia del secreto"
          required
          value={datos.referenciaSecretoEntrada}
          onChange={(evento) => actualizar("referenciaSecretoEntrada", evento.target.value)}
          placeholder="env:NEXTDOCS_BUZON_CLAVE"
          ayuda="La clave no se guarda: se resuelve por variable de entorno con el prefijo env:"
        />

        <div className="grid grid-cols-3 gap-3">
          <div className="col-span-2">
            <Campo
              etiqueta="Host SMTP (opcional)"
              value={datos.hostSalida}
              onChange={(evento) => actualizar("hostSalida", evento.target.value)}
              placeholder="smtp.tuempresa.com"
            />
          </div>
          <Campo
            etiqueta="Puerto"
            type="number"
            value={datos.puertoSalida}
            onChange={(evento) => actualizar("puertoSalida", Number(evento.target.value))}
          />
        </div>
        <Campo
          etiqueta="Referencia del secreto SMTP"
          value={datos.referenciaSecretoSalida}
          onChange={(evento) => actualizar("referenciaSecretoSalida", evento.target.value)}
          placeholder="env:NEXTDOCS_BUZON_CLAVE_SMTP"
        />

        <Campo
          etiqueta="Plantilla por defecto"
          value={datos.codigoPlantillaPorDefecto}
          onChange={(evento) => actualizar("codigoPlantillaPorDefecto", evento.target.value)}
          placeholder="REMITO"
          ayuda="Se aplica cuando la solicitud no define una propia."
        />

        <div className="space-y-2.5 rounded-xl border border-borde bg-lienzo/60 p-4">
          <Interruptor
            etiqueta="Exigir remitente autorizado"
            detalle="Si el remitente no esta en la lista blanca, no se ingesta ni un byte."
            valor={datos.exigirRemitenteAutorizado}
            alCambiar={(valor) => actualizar("exigirRemitenteAutorizado", valor)}
          />
          <Interruptor
            etiqueta="Exigir token de correlacion"
            detalle="Sin token se ingesta igual, pero abre una excepcion para asociarlo a mano."
            valor={datos.exigirCorrelacion}
            alCambiar={(valor) => actualizar("exigirCorrelacion", valor)}
          />
          <Interruptor
            etiqueta="Acusar recibo"
            detalle="Responde al remitente con el resultado. Necesita salida SMTP."
            valor={datos.acusarRecibo}
            alCambiar={(valor) => actualizar("acusarRecibo", valor)}
          />
          <Interruptor
            etiqueta="Conexiones cifradas"
            detalle="IMAPS y STARTTLS. Desactivalo solo contra un servidor local de pruebas."
            valor={datos.entradaSegura}
            alCambiar={(valor) => {
              actualizar("entradaSegura", valor);
              actualizar("salidaSegura", valor);
            }}
          />
        </div>
      </form>
    </Panel>
  );
}

function Interruptor({
  etiqueta,
  detalle,
  valor,
  alCambiar,
}: {
  etiqueta: string;
  detalle: string;
  valor: boolean;
  alCambiar: (valor: boolean) => void;
}) {
  return (
    <label className="flex cursor-pointer items-start gap-3">
      <input
        type="checkbox"
        checked={valor}
        onChange={(evento) => alCambiar(evento.target.checked)}
        className="mt-0.5 h-4 w-4 shrink-0 cursor-pointer accent-violeta"
      />
      <span className="min-w-0">
        <span className="block text-xs font-semibold text-tinta">{etiqueta}</span>
        <span className="mt-0.5 block text-[11px] leading-relaxed text-tinta-suave">{detalle}</span>
      </span>
    </label>
  );
}

function Correlaciones({ puedeAdministrar }: { puedeAdministrar: boolean }) {
  const clienteConsultas = useQueryClient();
  const [pagina, setPagina] = useState(0);
  const [creando, setCreando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const consulta = useQuery({
    queryKey: ["correo", "correlaciones", pagina],
    queryFn: () => listarCorrelaciones(pagina, 20),
  });

  const anular = useMutation({
    mutationFn: anularCorrelacion,
    onSuccess: () => clienteConsultas.invalidateQueries({ queryKey: ["correo", "correlaciones"] }),
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const correlaciones = consulta.data?.content ?? [];
  const totalPaginas = consulta.data?.totalPages ?? 0;

  return (
    <>
      {error ? (
        <div className="aparecer mb-4 rounded-xl border border-rojo-borde bg-rojo-tenue px-4 py-3 text-sm text-rojo">
          {error}
        </div>
      ) : null}

      {puedeAdministrar ? (
        <div className="mb-4 flex justify-end">
          <Boton variante="primario" onClick={() => setCreando(true)}>
            <IconoCorreo tamano={14} />
            Pedir documentacion
          </Boton>
        </div>
      ) : null}

      {consulta.isPending ? (
        <Cargando filas={4} alto="h-20" />
      ) : consulta.isError ? (
        <ErrorPanel mensaje={mensajeDeError(consulta.error)} reintentar={() => consulta.refetch()} />
      ) : correlaciones.length === 0 ? (
        <Vacio
          titulo="No hay solicitudes abiertas"
          detalle="Cada solicitud emite un token: sin el, los documentos que lleguen quedan sin asociar."
        />
      ) : (
        <>
          <ul className="space-y-3">
            {correlaciones.map((correlacion) => (
              <li
                key={correlacion.id}
                className="rounded-2xl border border-borde bg-white px-5 py-4 shadow-tarjeta"
              >
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-1.5">
                      <span className="rounded-full bg-grafito px-2.5 py-0.5 font-mono text-[11px] text-white">
                        {correlacion.token}
                      </span>
                      <Pastilla tono={correlacion.vigente ? "exito" : "neutro"}>
                        {correlacion.vigente ? "vigente" : "vencida"}
                      </Pastilla>
                    </div>
                    <p className="mt-2 text-sm font-semibold text-tinta">
                      {correlacion.sujetoTipoObjeto} {correlacion.sujetoIdObjeto}
                    </p>
                    <p className="mt-1 flex flex-wrap items-center gap-x-2.5 gap-y-1 text-xs text-tinta-suave">
                      {correlacion.destinatario ? <span>{correlacion.destinatario}</span> : null}
                      {correlacion.codigoPlantilla ? (
                        <span className="text-tinta-media">{correlacion.codigoPlantilla}</span>
                      ) : null}
                      <span className="flex items-center gap-1">
                        <IconoReloj tamano={12} />
                        vence {formatearFecha(correlacion.venceEn)}
                      </span>
                    </p>
                  </div>

                  <div className="flex shrink-0 items-center gap-4">
                    <div className="text-center">
                      <p className="cifra text-xl leading-none text-tinta">
                        {correlacion.documentosRecibidos}
                      </p>
                      <p className="mt-1 text-[10px] uppercase tracking-wider text-tinta-suave">
                        recibidos
                      </p>
                    </div>
                    {puedeAdministrar && correlacion.vigente ? (
                      <Boton tamano="sm" onClick={() => anular.mutate(correlacion.id)}>
                        Anular
                      </Boton>
                    ) : null}
                  </div>
                </div>
              </li>
            ))}
          </ul>

          {totalPaginas > 1 ? (
            <div className="mt-4 flex items-center justify-end gap-2">
              <Boton tamano="sm" disabled={pagina === 0} onClick={() => setPagina((a) => a - 1)}>
                <IconoIzquierda tamano={14} />
                Anterior
              </Boton>
              <span className="px-1 text-xs tabular-nums text-tinta-suave">
                {pagina + 1} de {totalPaginas}
              </span>
              <Boton
                tamano="sm"
                disabled={pagina + 1 >= totalPaginas}
                onClick={() => setPagina((a) => a + 1)}
              >
                Siguiente
                <IconoDerecha tamano={14} />
              </Boton>
            </div>
          ) : null}
        </>
      )}

      {creando ? <FormularioCorrelacion alCerrar={() => setCreando(false)} /> : null}
    </>
  );
}

function FormularioCorrelacion({ alCerrar }: { alCerrar: () => void }) {
  const clienteConsultas = useQueryClient();
  const [error, setError] = useState<string | null>(null);
  const [emitida, setEmitida] = useState<{ token: string; direccion?: string } | null>(null);
  const buzones = useQuery({ queryKey: ["correo", "buzones"], queryFn: listarBuzones });
  const [datos, setDatos] = useState({
    buzonId: "",
    sujetoOrigen: "FOLLOW",
    sujetoTipoObjeto: "Caso",
    sujetoIdObjeto: "",
    codigoPlantilla: "",
    destinatario: "",
    descripcion: "",
    diasVigencia: 30,
    enviarSolicitud: true,
  });

  const opciones = buzones.data ?? [];
  const buzonElegido = datos.buzonId || opciones[0]?.id || "";

  const crear = useMutation({
    mutationFn: () =>
      crearCorrelacion({
        ...datos,
        buzonId: buzonElegido,
        codigoPlantilla: datos.codigoPlantilla || undefined,
        destinatario: datos.destinatario || undefined,
        descripcion: datos.descripcion || undefined,
      }),
    onSuccess: (correlacion) => {
      setError(null);
      setEmitida({ token: correlacion.token, direccion: correlacion.direccionConEtiqueta });
      clienteConsultas.invalidateQueries({ queryKey: ["correo", "correlaciones"] });
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const actualizar = <C extends keyof typeof datos>(clave: C, valor: (typeof datos)[C]) =>
    setDatos((actual) => ({ ...actual, [clave]: valor }));

  if (emitida) {
    return (
      <Panel
        titulo="Solicitud emitida"
        descripcion="El token asocia lo que llegue con el caso"
        alCerrar={alCerrar}
        pie={
          <div className="flex justify-end">
            <Boton variante="primario" onClick={alCerrar}>
              Listo
            </Boton>
          </div>
        }
      >
        <div className="space-y-4">
          <div className="superficie-oscura rounded-2xl p-5 text-center">
            <p className="text-[10px] uppercase tracking-wider text-white/50">Token</p>
            <p className="mt-2 font-mono text-lg font-semibold text-white">{emitida.token}</p>
          </div>
          <p className="text-xs leading-relaxed text-tinta-suave">
            Viaja entre corchetes en el asunto. Si el tercero responde sin el, el documento igual
            entra pero queda sin asociar y abre una excepcion: el sistema nunca elige un caso por
            parecido.
          </p>
          {emitida.direccion ? (
            <div className="rounded-xl border border-borde bg-lienzo/60 p-4">
              <p className="text-[10px] uppercase tracking-wider text-tinta-suave">
                Direccion con etiqueta
              </p>
              <p className="mt-1 break-all font-mono text-xs text-tinta">{emitida.direccion}</p>
              <p className="mt-1.5 text-[11px] leading-relaxed text-tinta-suave">
                Sirve igual que el asunto si el proveedor de correo soporta subdireccionamiento.
              </p>
            </div>
          ) : null}
        </div>
      </Panel>
    );
  }

  return (
    <Panel
      titulo="Pedir documentacion"
      descripcion="Emite un token contra un caso concreto"
      alCerrar={alCerrar}
      pie={
        <div className="flex justify-end gap-2">
          <Boton onClick={alCerrar}>Cancelar</Boton>
          <Boton
            variante="primario"
            form="formulario-correlacion"
            type="submit"
            disabled={crear.isPending || !buzonElegido}
          >
            {crear.isPending ? "Emitiendo..." : "Emitir solicitud"}
          </Boton>
        </div>
      }
    >
      <form
        id="formulario-correlacion"
        onSubmit={(evento) => {
          evento.preventDefault();
          crear.mutate();
        }}
        className="space-y-4"
      >
        {error ? (
          <div className="rounded-xl border border-rojo-borde bg-rojo-tenue px-4 py-3 text-sm text-rojo">
            {error}
          </div>
        ) : null}

        {opciones.length === 0 && !buzones.isPending ? (
          <div className="rounded-xl border border-ambar-borde bg-ambar-tenue px-4 py-3 text-xs text-tinta">
            Primero crea un buzon: la solicitud sale desde el y la respuesta vuelve ahi.
          </div>
        ) : null}

        <Selector
          etiqueta="Buzon"
          value={buzonElegido}
          onChange={(evento) => actualizar("buzonId", evento.target.value)}
        >
          {opciones.map((buzon) => (
            <option key={buzon.id} value={buzon.id}>
              {buzon.direccion}
            </option>
          ))}
        </Selector>

        <div className="grid grid-cols-2 gap-3">
          <Campo
            etiqueta="Origen"
            required
            value={datos.sujetoOrigen}
            onChange={(evento) => actualizar("sujetoOrigen", evento.target.value)}
          />
          <Campo
            etiqueta="Tipo de objeto"
            required
            value={datos.sujetoTipoObjeto}
            onChange={(evento) => actualizar("sujetoTipoObjeto", evento.target.value)}
          />
        </div>
        <Campo
          etiqueta="Identificador del caso"
          required
          value={datos.sujetoIdObjeto}
          onChange={(evento) => actualizar("sujetoIdObjeto", evento.target.value)}
          placeholder="CASO-4477"
        />
        <Campo
          etiqueta="Plantilla esperada"
          value={datos.codigoPlantilla}
          onChange={(evento) => actualizar("codigoPlantilla", evento.target.value)}
          placeholder="REMITO"
        />
        <Campo
          etiqueta="Destinatario"
          type="email"
          value={datos.destinatario}
          onChange={(evento) => actualizar("destinatario", evento.target.value)}
          placeholder="compras@proveedor.com"
        />
        <Campo
          etiqueta="Que se pide"
          value={datos.descripcion}
          onChange={(evento) => actualizar("descripcion", evento.target.value)}
          placeholder="Necesitamos el remito firmado"
          ayuda="Va en el asunto, antes del token."
        />
        <Campo
          etiqueta="Dias de vigencia"
          type="number"
          min={1}
          max={365}
          required
          value={datos.diasVigencia}
          onChange={(evento) => actualizar("diasVigencia", Number(evento.target.value))}
        />

        <div className="rounded-xl border border-borde bg-lienzo/60 p-4">
          <Interruptor
            etiqueta="Enviar la solicitud por correo"
            detalle="Sale del buzon con el token en el asunto y queda auditada con su Message-ID."
            valor={datos.enviarSolicitud}
            alCambiar={(valor) => actualizar("enviarSolicitud", valor)}
          />
        </div>
      </form>
    </Panel>
  );
}
