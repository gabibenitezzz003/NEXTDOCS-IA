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
  Tarjeta,
} from "../componentes/Interfaz";
import {
  IconoAdjunto,
  IconoCheck,
  IconoCopiar,
  IconoDerecha,
  IconoEnlace,
  IconoEscudo,
  IconoIzquierda,
  IconoMas,
  IconoReloj,
} from "../componentes/Iconos";
import {
  anularCorrelacion,
  autorizarContacto,
  cambiarEstadoLinea,
  crearCorrelacion,
  crearLinea,
  listarCorrelaciones,
  listarLineas,
  listarMensajes,
  listarSalientes,
  obtenerMensaje,
  probarLinea,
  revocarContacto,
} from "../api/canalWhatsapp";
import { mensajeDeError } from "../api/cliente";
import { formatearFecha } from "./Documentos";
import { VisorDocumento } from "./VisorDocumento";
import { useSesion } from "../contextos/ProveedorSesion";
import type {
  EstadoLineaWhatsapp,
  LineaWhatsapp,
  MensajeWhatsapp,
  NuevaLineaWhatsapp,
  ResultadoMediaWhatsapp,
  ResultadoMensajeWhatsapp,
} from "../tipos/api";
import type { Tono } from "../componentes/Interfaz";

type Pestana = "bandeja" | "lineas" | "solicitudes";

const TONO_RESULTADO: Record<ResultadoMensajeWhatsapp, Tono> = {
  INGESTADO: "exito",
  PARCIAL: "alerta",
  SIN_MEDIA: "neutro",
  CONTACTO_NO_AUTORIZADO: "rojo",
  SIN_CORRELACION: "alerta",
  CORRELACION_AMBIGUA: "alerta",
  RECHAZADO: "rojo",
  ERROR: "rojo",
};

const TONO_MEDIA: Record<ResultadoMediaWhatsapp, Tono> = {
  INGESTADO: "exito",
  EN_CUARENTENA: "rojo",
  RECHAZADO: "rojo",
  ERROR: "rojo",
};

const TONO_LINEA: Record<EstadoLineaWhatsapp, Tono> = {
  ACTIVO: "exito",
  PAUSADO: "neutro",
  ERROR: "rojo",
};

const FILTROS: { valor: ResultadoMensajeWhatsapp | "TODOS"; texto: string }[] = [
  { valor: "TODOS", texto: "Todo" },
  { valor: "INGESTADO", texto: "Ingestado" },
  { valor: "SIN_CORRELACION", texto: "Sin asociar" },
  { valor: "CORRELACION_AMBIGUA", texto: "Ambiguo" },
  { valor: "CONTACTO_NO_AUTORIZADO", texto: "No autorizado" },
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

export function CanalWhatsapp() {
  const { tienePermiso } = useSesion();
  const [pestana, setPestana] = useState<Pestana>("bandeja");
  const puedeAdministrar = tienePermiso("canales.administrar");

  return (
    <>
      <Encabezado
        titulo="Canal de WhatsApp"
        descripcion="Los archivos que llegan por la Cloud API pasan por la misma ingesta que la API: antivirus, tipo real y tope de tamano."
        acciones={
          <GrupoSegmentado
            opciones={[
              { valor: "bandeja" as Pestana, texto: "Bandeja" },
              { valor: "lineas" as Pestana, texto: "Lineas" },
              { valor: "solicitudes" as Pestana, texto: "Solicitudes" },
            ]}
            valor={pestana}
            alCambiar={setPestana}
          />
        }
      />
      <Contenido>
        {pestana === "bandeja" ? <Bandeja /> : null}
        {pestana === "lineas" ? <Lineas puedeAdministrar={puedeAdministrar} /> : null}
        {pestana === "solicitudes" ? <Solicitudes puedeAdministrar={puedeAdministrar} /> : null}
      </Contenido>
    </>
  );
}

function Bandeja() {
  const [filtro, setFiltro] = useState<ResultadoMensajeWhatsapp | "TODOS">("TODOS");
  const [pagina, setPagina] = useState(0);
  const [abierto, setAbierto] = useState<string | null>(null);

  const consulta = useQuery({
    queryKey: ["whatsapp", "mensajes", filtro, pagina],
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
          titulo="No hay mensajes en este filtro"
          detalle="Todo lo que entra por la linea queda registrado aca, tambien lo que se rechaza."
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
                        {mensaje.nombrePerfil || mensaje.numeroOrigen || "(numero desconocido)"}
                      </p>
                      <p className="mt-1 flex flex-wrap items-center gap-x-2.5 gap-y-1 text-xs text-tinta-suave">
                        <span className="font-mono text-tinta-media">{mensaje.numeroOrigen}</span>
                        <span className="flex items-center gap-1">
                          <IconoReloj tamano={12} />
                          {formatearFecha(mensaje.recibidoEn ?? mensaje.alta)}
                        </span>
                      </p>
                      {mensaje.texto ? (
                        <p className="mt-2 truncate text-xs italic text-tinta-suave">
                          &laquo;{mensaje.texto}&raquo;
                        </p>
                      ) : null}
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
    queryKey: ["whatsapp", "mensaje", mensajeId],
    queryFn: () => obtenerMensaje(mensajeId),
  });

  return (
    <>
      <Panel
        titulo="Mensaje recibido"
        descripcion="Que hizo el canal con cada archivo"
        alCerrar={alCerrar}
      >
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
  mensaje: MensajeWhatsapp;
  alAbrirDocumento: (id: string) => void;
}) {
  return (
    <div className="space-y-5">
      <div>
        <Pastilla tono={TONO_RESULTADO[mensaje.resultado]} solido>
          {mensaje.resultado.replace(/_/g, " ")}
        </Pastilla>
        <p className="mt-3 text-sm font-semibold leading-snug text-tinta">
          {mensaje.nombrePerfil || mensaje.numeroOrigen}
        </p>
      </div>

      <dl className="space-y-2 rounded-xl border border-borde bg-lienzo/60 p-4 text-xs">
        <Dato etiqueta="De" valor={mensaje.numeroOrigen} monoespaciado />
        <Dato etiqueta="Linea" valor={mensaje.numeroLinea} monoespaciado />
        <Dato etiqueta="Tipo" valor={mensaje.tipo} />
        <Dato etiqueta="Recibido" valor={formatearFecha(mensaje.recibidoEn ?? mensaje.alta)} />
        <Dato etiqueta="Token" valor={mensaje.tokenDetectado ?? "sin token"} monoespaciado />
        <Dato etiqueta="wamid" valor={mensaje.identificadorMensaje} monoespaciado />
      </dl>

      {mensaje.texto ? (
        <div className="rounded-xl border border-borde bg-white px-4 py-3 text-xs italic leading-relaxed text-tinta-media">
          &laquo;{mensaje.texto}&raquo;
        </div>
      ) : null}

      {mensaje.motivo ? (
        <div className="rounded-xl border border-ambar-borde bg-ambar-tenue px-4 py-3 text-xs leading-relaxed text-tinta">
          {mensaje.motivo}
        </div>
      ) : null}

      <div>
        <h3 className="mb-2 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-tinta-suave">
          <IconoAdjunto tamano={13} />
          Archivos ({mensaje.detalleMedia.length})
        </h3>
        {mensaje.detalleMedia.length === 0 ? (
          <p className="rounded-xl border border-dashed border-borde px-4 py-6 text-center text-xs text-tinta-suave">
            El mensaje no traia archivos.
          </p>
        ) : (
          <ul className="space-y-2">
            {mensaje.detalleMedia.map((media) => (
              <li key={media.id} className="rounded-xl border border-borde bg-white p-3.5">
                <div className="flex items-start justify-between gap-3">
                  <p className="min-w-0 flex-1 truncate text-sm font-medium text-tinta">
                    {media.nombreArchivo}
                  </p>
                  <Pastilla tono={TONO_MEDIA[media.resultado]}>
                    {media.resultado.replace(/_/g, " ")}
                  </Pastilla>
                </div>
                <p className="mt-1 text-[11px] text-tinta-suave">
                  {presentarTamano(media.tamanoBytes)}
                  {media.tipoMime ? ` · ${media.tipoMime}` : ""}
                  {media.codigoRechazo ? ` · ${media.codigoRechazo}` : ""}
                </p>
                {media.motivo ? (
                  <p className="mt-1.5 text-xs leading-relaxed text-tinta-suave">{media.motivo}</p>
                ) : null}
                {media.sha256 ? (
                  <p className="mt-1.5 truncate font-mono text-[10px] text-tinta-tenue">
                    sha256 {media.sha256}
                  </p>
                ) : null}
                {media.documentoId ? (
                  <Boton
                    tamano="sm"
                    className="mt-2.5"
                    onClick={() => alAbrirDocumento(media.documentoId!)}
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

function Lineas({ puedeAdministrar }: { puedeAdministrar: boolean }) {
  const clienteConsultas = useQueryClient();
  const [creando, setCreando] = useState(false);
  const [aviso, setAviso] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const consulta = useQuery({ queryKey: ["whatsapp", "lineas"], queryFn: listarLineas });
  const refrescar = () => clienteConsultas.invalidateQueries({ queryKey: ["whatsapp"] });

  const probar = useMutation({
    mutationFn: probarLinea,
    onSuccess: () => {
      setError(null);
      setAviso("La Cloud API responde con el token configurado");
    },
    onError: (fallo) => {
      setAviso(null);
      setError(mensajeDeError(fallo));
    },
  });

  const cambiarEstado = useMutation({
    mutationFn: ({ id, estado }: { id: string; estado: EstadoLineaWhatsapp }) =>
      cambiarEstadoLinea(id, estado),
    onSuccess: refrescar,
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const lineas = consulta.data ?? [];

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
            Nueva linea
          </Boton>
        </div>
      ) : null}

      {consulta.isPending ? (
        <Cargando filas={2} alto="h-44" />
      ) : consulta.isError ? (
        <ErrorPanel mensaje={mensajeDeError(consulta.error)} reintentar={() => consulta.refetch()} />
      ) : lineas.length === 0 ? (
        <Vacio
          titulo="No hay ninguna linea configurada"
          detalle="Una linea es un numero de WhatsApp Business con su webhook propio. El secreto nunca se guarda en la base: se referencia con env:."
        />
      ) : (
        <div className="space-y-4">
          {lineas.map((linea) => (
            <TarjetaLinea
              key={linea.id}
              linea={linea}
              puedeAdministrar={puedeAdministrar}
              enProceso={probar.isPending || cambiarEstado.isPending}
              alProbar={() => probar.mutate(linea.id)}
              alCambiarEstado={(estado) => cambiarEstado.mutate({ id: linea.id, estado })}
              alFallar={setError}
              alRefrescar={refrescar}
            />
          ))}
        </div>
      )}

      {creando ? (
        <FormularioLinea
          alCerrar={() => setCreando(false)}
          alCrear={() => {
            setCreando(false);
            setError(null);
            setAviso("Linea creada. Pega la URL del webhook en la app de Meta y verifica la suscripcion");
            refrescar();
          }}
        />
      ) : null}
    </>
  );
}

function TarjetaLinea({
  linea,
  puedeAdministrar,
  enProceso,
  alProbar,
  alCambiarEstado,
  alFallar,
  alRefrescar,
}: {
  linea: LineaWhatsapp;
  puedeAdministrar: boolean;
  enProceso: boolean;
  alProbar: () => void;
  alCambiarEstado: (estado: EstadoLineaWhatsapp) => void;
  alFallar: (mensaje: string) => void;
  alRefrescar: () => void;
}) {
  const [patron, setPatron] = useState("");
  const [copiado, setCopiado] = useState(false);

  const autorizar = useMutation({
    mutationFn: () => autorizarContacto(linea.id, patron.trim()),
    onSuccess: () => {
      setPatron("");
      alRefrescar();
    },
    onError: (fallo) => alFallar(mensajeDeError(fallo)),
  });

  const revocar = useMutation({
    mutationFn: revocarContacto,
    onSuccess: alRefrescar,
    onError: (fallo) => alFallar(mensajeDeError(fallo)),
  });

  const copiarWebhook = async () => {
    try {
      await navigator.clipboard.writeText(linea.urlWebhook);
      setCopiado(true);
      window.setTimeout(() => setCopiado(false), 2000);
    } catch {
      alFallar("El navegador no dejo copiar la URL. Seleccionala a mano.");
    }
  };

  return (
    <Tarjeta>
      <CabeceraTarjeta
        titulo={linea.nombre}
        descripcion={linea.numeroTelefono}
        acciones={<Pastilla tono={TONO_LINEA[linea.estado]}>{linea.estado}</Pastilla>}
      />

      <div className="mt-4 rounded-xl border border-borde bg-lienzo/60 p-4">
        <p className="text-[10px] font-semibold uppercase tracking-wider text-tinta-suave">
          URL del webhook para Meta
        </p>
        <div className="mt-1.5 flex items-center gap-2">
          <code className="min-w-0 flex-1 truncate font-mono text-[11px] text-tinta">
            {linea.urlWebhook}
          </code>
          <Boton tamano="sm" onClick={copiarWebhook}>
            {copiado ? <IconoCheck tamano={13} /> : <IconoCopiar tamano={13} />}
            {copiado ? "Copiado" : "Copiar"}
          </Boton>
        </div>
        <p className="mt-2 text-[11px] leading-relaxed text-tinta-suave">
          Cada linea tiene su ruta propia, asi el tenant no se deduce del cuerpo del evento. Meta
          firma cada entrega y el canal la valida contra el secreto de la aplicacion antes de tocar
          nada.
        </p>
      </div>

      <dl className="mt-4 grid gap-x-6 gap-y-2 text-xs sm:grid-cols-2">
        <Dato etiqueta="Numero" valor={linea.identificadorNumero} monoespaciado />
        <Dato etiqueta="Cuenta" valor={linea.identificadorCuenta} monoespaciado />
        <Dato etiqueta="Plantilla" valor={linea.codigoPlantillaPorDefecto ?? "sin plantilla"} />
        <Dato etiqueta="Ventana" valor={`${linea.minutosVentanaCorrelacion} min sin token`} />
        <Dato etiqueta="Ultimo" valor={formatearFecha(linea.ultimoMensaje)} />
        <Dato etiqueta="Maximo" valor={`${linea.maximoMediaPorMensaje} archivos por mensaje`} />
      </dl>

      <div className="mt-3 flex flex-wrap gap-1.5">
        {linea.exigirContactoAutorizado ? (
          <Pastilla tono="informacion">Solo contactos autorizados</Pastilla>
        ) : (
          <Pastilla tono="alerta">Abierta a cualquier numero</Pastilla>
        )}
        {linea.exigirCorrelacion ? <Pastilla tono="informacion">Exige token</Pastilla> : null}
        {linea.acusarRecibo ? <Pastilla tono="neutro">Acusa recibo</Pastilla> : null}
        {linea.puedeResponderFueraDeVentana ? (
          <Pastilla tono="neutro">Plantilla {linea.nombrePlantillaSolicitud}</Pastilla>
        ) : (
          <Pastilla tono="neutro">Solo responde dentro de 24 h</Pastilla>
        )}
      </div>

      {linea.ultimoError ? (
        <p className="mt-3 rounded-xl border border-rojo-borde bg-rojo-tenue px-3.5 py-2.5 text-xs leading-relaxed text-rojo">
          {linea.ultimoError}
          {linea.fallosConsecutivos > 1
            ? ` (${linea.fallosConsecutivos} fallos seguidos)`
            : ""}
        </p>
      ) : null}

      <div className="mt-4 border-t border-borde pt-4">
        <h4 className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-tinta-suave">
          <IconoEscudo tamano={13} />
          Contactos autorizados ({linea.contactos.length})
        </h4>
        {linea.contactos.length === 0 ? (
          <p className="mt-2 text-xs text-tinta-suave">
            {linea.exigirContactoAutorizado
              ? "Sin contactos cargados no entra ningun mensaje."
              : "La linea acepta cualquier numero porque no exige lista blanca."}
          </p>
        ) : (
          <ul className="mt-2 flex flex-wrap gap-1.5">
            {linea.contactos.map((contacto) => (
              <li
                key={contacto.id}
                className="inline-flex items-center gap-1.5 rounded-full bg-lienzo px-2.5 py-1 font-mono text-[11px] text-tinta-media ring-1 ring-inset ring-borde"
              >
                {contacto.patron}
                {puedeAdministrar ? (
                  <button
                    type="button"
                    onClick={() => revocar.mutate(contacto.id)}
                    className="text-tinta-tenue transition hover:text-rojo"
                    aria-label={`Revocar ${contacto.patron}`}
                  >
                    ×
                  </button>
                ) : null}
              </li>
            ))}
          </ul>
        )}

        {puedeAdministrar ? (
          <form
            className="mt-3 flex flex-wrap items-end gap-2"
            onSubmit={(evento) => {
              evento.preventDefault();
              if (patron.trim()) {
                autorizar.mutate();
              }
            }}
          >
            <div className="min-w-[16rem] flex-1">
              <Campo
                etiqueta="Autorizar numero o prefijo"
                value={patron}
                onChange={(evento) => setPatron(evento.target.value)}
                placeholder="+5491133224455 o +54911*"
                ayuda="Siempre en E.164. Un numero sin prefijo internacional puede caer en otro pais."
              />
            </div>
            <Boton type="submit" disabled={!patron.trim() || autorizar.isPending}>
              Autorizar
            </Boton>
          </form>
        ) : null}
      </div>

      {puedeAdministrar ? (
        <div className="mt-4 flex flex-wrap gap-2 border-t border-borde pt-4">
          <Boton tamano="sm" disabled={enProceso} onClick={alProbar}>
            Probar conexion
          </Boton>
          <Boton
            tamano="sm"
            disabled={enProceso}
            onClick={() =>
              alCambiarEstado(linea.estado === "ACTIVO" ? "PAUSADO" : "ACTIVO")
            }
          >
            {linea.estado === "ACTIVO" ? "Pausar" : "Reactivar"}
          </Boton>
        </div>
      ) : null}
    </Tarjeta>
  );
}

function FormularioLinea({
  alCerrar,
  alCrear,
}: {
  alCerrar: () => void;
  alCrear: () => void;
}) {
  const [datos, setDatos] = useState<NuevaLineaWhatsapp>({
    nombre: "",
    numeroTelefono: "",
    identificadorNumero: "",
    identificadorCuenta: "",
    referenciaTokenAcceso: "env:",
    referenciaSecretoAplicacion: "env:",
    referenciaTokenVerificacion: "env:",
    codigoPlantillaPorDefecto: "",
    exigirContactoAutorizado: true,
    exigirCorrelacion: false,
    acusarRecibo: false,
    maximoMediaPorMensaje: 10,
    minutosVentanaCorrelacion: 1440,
    nombrePlantillaSolicitud: "",
    idiomaPlantillaSolicitud: "es",
  });
  const [error, setError] = useState<string | null>(null);

  const guardar = useMutation({
    mutationFn: () => crearLinea(datos),
    onSuccess: alCrear,
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const cambiar = <C extends keyof NuevaLineaWhatsapp>(clave: C, valor: NuevaLineaWhatsapp[C]) =>
    setDatos((anterior) => ({ ...anterior, [clave]: valor }));

  return (
    <Panel
      titulo="Nueva linea de WhatsApp"
      descripcion="Los tres secretos se referencian con env: y nunca se guardan en la base"
      alCerrar={alCerrar}
    >
      <form
        className="space-y-4"
        onSubmit={(evento) => {
          evento.preventDefault();
          guardar.mutate();
        }}
      >
        {error ? (
          <div className="rounded-xl border border-rojo-borde bg-rojo-tenue px-4 py-3 text-sm text-rojo">
            {error}
          </div>
        ) : null}

        <Campo
          etiqueta="Nombre"
          value={datos.nombre}
          onChange={(evento) => cambiar("nombre", evento.target.value)}
          placeholder="Mesa de entradas"
        />
        <Campo
          etiqueta="Numero de la linea"
          value={datos.numeroTelefono}
          onChange={(evento) => cambiar("numeroTelefono", evento.target.value)}
          placeholder="+541150000000"
          ayuda="En E.164, con el + y el codigo de pais."
        />
        <Campo
          etiqueta="Phone number ID"
          value={datos.identificadorNumero}
          onChange={(evento) => cambiar("identificadorNumero", evento.target.value)}
          ayuda="El identificador que da Meta en la Cloud API, no el numero."
        />
        <Campo
          etiqueta="WhatsApp Business Account ID"
          value={datos.identificadorCuenta ?? ""}
          onChange={(evento) => cambiar("identificadorCuenta", evento.target.value)}
        />
        <Campo
          etiqueta="Token de acceso"
          value={datos.referenciaTokenAcceso}
          onChange={(evento) => cambiar("referenciaTokenAcceso", evento.target.value)}
          placeholder="env:WHATSAPP_TOKEN"
          ayuda="Referencia a una variable de entorno. El valor no viaja ni se guarda."
        />
        <Campo
          etiqueta="Secreto de la aplicacion"
          value={datos.referenciaSecretoAplicacion}
          onChange={(evento) => cambiar("referenciaSecretoAplicacion", evento.target.value)}
          placeholder="env:WHATSAPP_APP_SECRET"
          ayuda="Con esto se valida la firma X-Hub-Signature-256 de cada entrega de Meta."
        />
        <Campo
          etiqueta="Token de verificacion"
          value={datos.referenciaTokenVerificacion}
          onChange={(evento) => cambiar("referenciaTokenVerificacion", evento.target.value)}
          placeholder="env:WHATSAPP_VERIFY_TOKEN"
          ayuda="El que Meta manda una sola vez al dar de alta la suscripcion."
        />
        <Campo
          etiqueta="Plantilla documental por defecto"
          value={datos.codigoPlantillaPorDefecto ?? ""}
          onChange={(evento) => cambiar("codigoPlantillaPorDefecto", evento.target.value)}
          placeholder="REMITO"
        />
        <Campo
          etiqueta="Plantilla aprobada por Meta"
          value={datos.nombrePlantillaSolicitud ?? ""}
          onChange={(evento) => cambiar("nombrePlantillaSolicitud", evento.target.value)}
          placeholder="solicitud_documentacion"
          ayuda="Sin esto la linea no puede escribir primero: fuera de las 24 h WhatsApp solo acepta plantillas."
        />

        <div className="space-y-2 rounded-xl border border-borde bg-lienzo/60 p-4">
          <Interruptor
            etiqueta="Solo contactos autorizados"
            valor={datos.exigirContactoAutorizado}
            alCambiar={(valor) => cambiar("exigirContactoAutorizado", valor)}
          />
          <Interruptor
            etiqueta="Exigir token de correlacion"
            valor={datos.exigirCorrelacion}
            alCambiar={(valor) => cambiar("exigirCorrelacion", valor)}
          />
          <Interruptor
            etiqueta="Acusar recibo al contacto"
            valor={datos.acusarRecibo}
            alCambiar={(valor) => cambiar("acusarRecibo", valor)}
          />
        </div>

        <div className="flex justify-end gap-2 pt-2">
          <Boton onClick={alCerrar}>Cancelar</Boton>
          <Boton type="submit" variante="primario" disabled={guardar.isPending}>
            {guardar.isPending ? "Creando..." : "Crear linea"}
          </Boton>
        </div>
      </form>
    </Panel>
  );
}

function Interruptor({
  etiqueta,
  valor,
  alCambiar,
}: {
  etiqueta: string;
  valor: boolean;
  alCambiar: (valor: boolean) => void;
}) {
  return (
    <label className="flex cursor-pointer items-center gap-2.5 text-sm text-tinta">
      <input
        type="checkbox"
        checked={valor}
        onChange={(evento) => alCambiar(evento.target.checked)}
        className="h-4 w-4 rounded border-borde-fuerte text-violeta focus:ring-violeta"
      />
      {etiqueta}
    </label>
  );
}

function Solicitudes({ puedeAdministrar }: { puedeAdministrar: boolean }) {
  const clienteConsultas = useQueryClient();
  const [pagina, setPagina] = useState(0);
  const [creando, setCreando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const consulta = useQuery({
    queryKey: ["whatsapp", "correlaciones", pagina],
    queryFn: () => listarCorrelaciones(pagina, 20),
  });
  const salientes = useQuery({
    queryKey: ["whatsapp", "salientes"],
    queryFn: () => listarSalientes(0, 5),
  });
  const refrescar = () => clienteConsultas.invalidateQueries({ queryKey: ["whatsapp"] });

  const anular = useMutation({
    mutationFn: anularCorrelacion,
    onSuccess: refrescar,
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
            <IconoMas tamano={14} />
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
          detalle="Cada solicitud emite un token. Sin token los documentos entran igual, pero quedan sin asociar y con una excepcion abierta."
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
                      <span className="font-mono text-sm font-semibold text-violeta">
                        {correlacion.token}
                      </span>
                      <Pastilla tono={correlacion.vigente ? "exito" : "neutro"}>
                        {correlacion.vigente ? "vigente" : "vencida"}
                      </Pastilla>
                      {correlacion.numeroVinculado ? (
                        <Pastilla tono="informacion">
                          vinculada a {correlacion.numeroVinculado}
                        </Pastilla>
                      ) : null}
                    </div>
                    <p className="mt-2 text-sm text-tinta">
                      {correlacion.sujetoTipoObjeto} {correlacion.sujetoIdObjeto}
                      {correlacion.descripcion ? ` · ${correlacion.descripcion}` : ""}
                    </p>
                    <p className="mt-1 text-xs text-tinta-suave">
                      Vence {formatearFecha(correlacion.venceEn)}
                      {correlacion.numeroDestino ? ` · pedida a ${correlacion.numeroDestino}` : ""}
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

      {(salientes.data?.content ?? []).length > 0 ? (
        <Tarjeta className="mt-6">
          <CabeceraTarjeta
            titulo="Ultimos mensajes salientes"
            descripcion="Fuera de la ventana de 24 horas WhatsApp solo acepta plantillas aprobadas"
            />
          <ul className="mt-3 divide-y divide-borde text-xs">
            {(salientes.data?.content ?? []).map((saliente) => (
              <li key={saliente.id} className="flex flex-wrap items-center gap-x-3 gap-y-1 py-2.5">
                <Pastilla tono={saliente.estado === "ENVIADO" ? "exito" : "rojo"}>
                  {saliente.estado}
                </Pastilla>
                <span className="font-medium text-tinta">
                  {saliente.plantilla.replace(/_/g, " ")}
                </span>
                <span className="font-mono text-tinta-suave">{saliente.numeroDestino}</span>
                <Pastilla tono={saliente.dentroDeVentana ? "neutro" : "alerta"}>
                  {saliente.dentroDeVentana ? "dentro de 24 h" : "fuera de 24 h"}
                </Pastilla>
                {saliente.detalleError ? (
                  <span className="w-full text-tinta-suave">{saliente.detalleError}</span>
                ) : null}
              </li>
            ))}
          </ul>
        </Tarjeta>
      ) : null}

      {creando ? (
        <FormularioCorrelacion
          alCerrar={() => setCreando(false)}
          alCrear={() => {
            setError(null);
            refrescar();
          }}
        />
      ) : null}
    </>
  );
}

function FormularioCorrelacion({
  alCerrar,
  alCrear,
}: {
  alCerrar: () => void;
  alCrear: () => void;
}) {
  const lineas = useQuery({ queryKey: ["whatsapp", "lineas"], queryFn: listarLineas });
  const [lineaId, setLineaId] = useState("");
  const [sujetoOrigen, setSujetoOrigen] = useState("FOLLOW");
  const [sujetoTipoObjeto, setSujetoTipoObjeto] = useState("Caso");
  const [sujetoIdObjeto, setSujetoIdObjeto] = useState("");
  const [codigoPlantilla, setCodigoPlantilla] = useState("");
  const [numeroDestino, setNumeroDestino] = useState("");
  const [descripcion, setDescripcion] = useState("");
  const [enviarSolicitud, setEnviarSolicitud] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [emitida, setEmitida] = useState<string | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);

  const opciones = lineas.data ?? [];
  const seleccionada = lineaId || opciones[0]?.id || "";

  const guardar = useMutation({
    mutationFn: () =>
      crearCorrelacion({
        lineaId: seleccionada,
        sujetoOrigen,
        sujetoTipoObjeto,
        sujetoIdObjeto,
        codigoPlantilla: codigoPlantilla || undefined,
        numeroDestino: numeroDestino || undefined,
        descripcion: descripcion || undefined,
        diasVigencia: 30,
        enviarSolicitud,
      }),
    onSuccess: (correlacion) => {
      setEmitida(correlacion.token);
      setAviso(
        correlacion.mensajeSalienteId
          ? "Se registro el envio. Revisa el estado en la lista de salientes."
          : null,
      );
      alCrear();
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  if (emitida) {
    return (
      <Panel titulo="Token emitido" descripcion="El contacto tiene que incluirlo en el chat" alCerrar={alCerrar}>
        <div className="space-y-4">
          <div className="rounded-xl border border-exito-borde bg-exito-tenue px-4 py-4 text-center">
            <p className="font-mono text-lg font-semibold text-exito">{emitida}</p>
          </div>
          <p className="text-xs leading-relaxed text-tinta-suave">
            Con el token en el mensaje los documentos se asocian solos. Si el contacto lo manda
            primero y despues las fotos, el canal las asocia igual mientras la ventana siga abierta.
            Si hay mas de una solicitud abierta para ese numero y el mensaje no trae token, el canal
            no elige: ingesta igual y abre una excepcion para revision humana.
          </p>
          {aviso ? <p className="text-xs text-tinta-suave">{aviso}</p> : null}
          <div className="flex justify-end">
            <Boton variante="primario" onClick={alCerrar}>
              Listo
            </Boton>
          </div>
        </div>
      </Panel>
    );
  }

  return (
    <Panel
      titulo="Pedir documentacion"
      descripcion="Emite un token para asociar lo que llegue por WhatsApp"
      alCerrar={alCerrar}
    >
      <form
        className="space-y-4"
        onSubmit={(evento) => {
          evento.preventDefault();
          guardar.mutate();
        }}
      >
        {error ? (
          <div className="rounded-xl border border-rojo-borde bg-rojo-tenue px-4 py-3 text-sm text-rojo">
            {error}
          </div>
        ) : null}

        <label className="block">
          <span className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-tinta-suave">
            Linea
          </span>
          <select
            value={seleccionada}
            onChange={(evento) => setLineaId(evento.target.value)}
            className="w-full rounded-xl border border-borde bg-white px-3.5 py-2.5 text-sm text-tinta focus:border-violeta focus:outline-none"
          >
            {opciones.map((linea) => (
              <option key={linea.id} value={linea.id}>
                {linea.nombre} · {linea.numeroTelefono}
              </option>
            ))}
          </select>
        </label>

        <Campo etiqueta="Origen del sujeto" value={sujetoOrigen} onChange={(evento) => setSujetoOrigen(evento.target.value)} />
        <Campo etiqueta="Tipo de objeto" value={sujetoTipoObjeto} onChange={(evento) => setSujetoTipoObjeto(evento.target.value)} />
        <Campo
          etiqueta="Identificador del objeto"
          value={sujetoIdObjeto}
          onChange={(evento) => setSujetoIdObjeto(evento.target.value)}
          placeholder="CASO-4477"
        />
        <Campo
          etiqueta="Plantilla documental"
          value={codigoPlantilla}
          onChange={(evento) => setCodigoPlantilla(evento.target.value)}
          placeholder="REMITO"
        />
        <Campo
          etiqueta="Numero del contacto"
          value={numeroDestino}
          onChange={(evento) => setNumeroDestino(evento.target.value)}
          placeholder="+5491133224455"
          ayuda="En E.164. Si la linea no le escribio en las ultimas 24 horas hace falta una plantilla aprobada por Meta."
        />
        <Campo etiqueta="Descripcion" value={descripcion} onChange={(evento) => setDescripcion(evento.target.value)} />

        <div className="rounded-xl border border-borde bg-lienzo/60 p-4">
          <Interruptor
            etiqueta="Enviar la solicitud ahora por WhatsApp"
            valor={enviarSolicitud}
            alCambiar={setEnviarSolicitud}
          />
        </div>

        <div className="flex justify-end gap-2 pt-2">
          <Boton onClick={alCerrar}>Cancelar</Boton>
          <Boton
            type="submit"
            variante="primario"
            disabled={!sujetoIdObjeto.trim() || !seleccionada || guardar.isPending}
          >
            {guardar.isPending ? "Emitiendo..." : "Emitir token"}
          </Boton>
        </div>
      </form>
    </Panel>
  );
}
