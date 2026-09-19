import { useRef, useState } from "react";
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
import { useIdioma } from "../contextos/ProveedorIdioma";
import {
  actualizarRegla,
  crearRegla,
  darDeBajaRegla,
  generarReglaConIa,
  listarProcesos,
  listarReglas,
  mensajeDeError,
} from "../api/procesos";
import type {
  AccionRegla,
  CambiosRegla,
  OperadorRegla,
  Proceso,
  PropuestaRegla,
  ReglaSupervisora,
  SeveridadRegla,
} from "../api/procesos";
import type { Tono } from "../componentes/Interfaz";

const OPERADORES: { valor: OperadorRegla; texto: string }[] = [
  { valor: "MENOR", texto: "reglasSupervisora.esMenorQue" },
  { valor: "MAYOR", texto: "reglasSupervisora.esMayorQue" },
];

const ACCIONES: { valor: AccionRegla; texto: string }[] = [
  { valor: "ADVERTIR", texto: "reglasSupervisora.accionAdvertir" },
  { valor: "SOLICITAR", texto: "reglasSupervisora.accionSolicitar" },
  { valor: "REVIEW", texto: "reglasSupervisora.accionReview" },
  { valor: "BLOQUEAR", texto: "reglasSupervisora.accionBloquear" },
];

const SEVERIDADES: { valor: SeveridadRegla; texto: string }[] = [
  { valor: "BAJA", texto: "reglasSupervisora.severidadBaja" },
  { valor: "MEDIA", texto: "reglasSupervisora.severidadMedia" },
  { valor: "ALTA", texto: "reglasSupervisora.severidadAlta" },
  { valor: "CRITICA", texto: "reglasSupervisora.severidadCritica" },
];

const TONO_SEVERIDAD: Record<SeveridadRegla, Tono> = {
  BAJA: "neutro",
  MEDIA: "informacion",
  ALTA: "alerta",
  CRITICA: "rojo",
};

const UMBRAL_MAXIMO = 999.99;

export function Supervisora() {
  const { t } = useIdioma();
  return (
    <>
      <Encabezado
        titulo={t("reglasSupervisora.titulo")}
        descripcion={t("reglasSupervisora.descripcion")}
      />
      <Contenido>
        <ReglasSupervisora />
      </Contenido>
    </>
  );
}

function ReglasSupervisora() {
  const { t } = useIdioma();
  const [creando, setCreando] = useState(false);
  const [editando, setEditando] = useState<string | null>(null);

  const consulta = useQuery({
    queryKey: ["reglas-supervisora"],
    queryFn: () => listarReglas(),
  });

  const procesos = useQuery({
    queryKey: ["procesos"],
    queryFn: listarProcesos,
  });

  if (consulta.isPending) {
    return <Cargando filas={3} />;
  }

  if (consulta.isError) {
    return (
      <ErrorPanel
        mensaje={mensajeDeError(consulta.error)}
          error={consulta.error}
        reintentar={() => consulta.refetch()}
      />
    );
  }

  const reglas = consulta.data ?? [];

  return (
    <div className="grid gap-espacio-4">
      <div className="grid min-w-0 gap-espacio-4 xl:grid-cols-2">
        <ArbolSupervisora />
        <ChatSupervisora />
      </div>
      <Tarjeta>
        <CabeceraTarjeta
          titulo={t("reglasSupervisora.titulo")}
          descripcion={t("reglasSupervisora.descripcion")}
          acciones={
            <Boton
              variante={creando ? "secundario" : "primario"}
              onClick={() => {
                setCreando((actual) => !actual);
                setEditando(null);
              }}
            >
              {creando ? t("comun.cancelar") : t("reglasSupervisora.nuevaRegla")}
            </Boton>
          }
        />
        {creando ? (
          <FormularioRegla
            procesos={procesos.data ?? []}
            alCerrar={() => setCreando(false)}
          />
        ) : null}
      </Tarjeta>

      {reglas.length === 0 ? (
        <Vacio
          titulo={t("reglasSupervisora.sinReglas")}
          detalle={t("reglasSupervisora.sinReglasDetalle")}
        />
      ) : (
        reglas.map((regla, indice) => (
          <TarjetaRegla
            key={regla.id}
            regla={regla}
            indice={indice}
            procesos={procesos.data ?? []}
            editando={editando === regla.id}
            alEditar={() => {
              setEditando(editando === regla.id ? null : regla.id);
              setCreando(false);
            }}
            alCerrarEdicion={() => setEditando(null)}
          />
        ))
      )}
    </div>
  );
}

function TarjetaRegla({
  regla,
  indice,
  procesos,
  editando,
  alEditar,
  alCerrarEdicion,
}: {
  regla: ReglaSupervisora;
  indice: number;
  procesos: Proceso[];
  editando: boolean;
  alEditar: () => void;
  alCerrarEdicion: () => void;
}) {
  const clienteConsultas = useQueryClient();
  const { t } = useIdioma();
  const [confirmando, setConfirmando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const baja = useMutation({
    mutationFn: () => darDeBajaRegla(regla.id),
    onSuccess: () => {
      setError(null);
      setConfirmando(false);
      clienteConsultas.invalidateQueries({ queryKey: ["reglas-supervisora"] });
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const severidad = regla.severidad ?? "MEDIA";
  const alcance = procesos.find((proceso) => proceso.id === regla.plantillaId);

  return (
    <Tarjeta indice={indice}>
      <CabeceraTarjeta
        titulo={regla.nombre}
        descripcion={textoCondicion(regla, t)}
        acciones={
          <div className="flex flex-wrap items-center gap-espacio-2">
            <Pastilla tono={TONO_SEVERIDAD[severidad]}>
              {t(`prioridad.${severidad}`)}
            </Pastilla>
            {regla.accion === "BLOQUEAR" ? (
              <Pastilla tono="rojo" solido>
                {t("reglasSupervisora.bloquea")}
              </Pastilla>
            ) : (
              <Pastilla tono="neutro">{textoAccion(regla.accion, t)}</Pastilla>
            )}
            <Boton variante="secundario" onClick={alEditar}>
              {editando ? t("reglasSupervisora.cerrar") : t("reglasSupervisora.editar")}
            </Boton>
            {confirmando ? (
              <Boton
                variante="secundario"
                disabled={baja.isPending}
                onClick={() => setConfirmando(false)}
              >
                {t("reglasSupervisora.conservar")}
              </Boton>
            ) : null}
            <Boton
              variante={confirmando ? "peligro" : "fantasma"}
              cargando={baja.isPending}
              disabled={baja.isPending}
              onClick={() => (confirmando ? baja.mutate() : setConfirmando(true))}
            >
              {confirmando
                ? t("reglasSupervisora.confirmarBaja")
                : t("reglasSupervisora.darDeBaja")}
            </Boton>
          </div>
        }
      />

      <div className="mt-espacio-3 flex flex-wrap items-center gap-espacio-2">
        <Pastilla tono="violeta">
          {alcance ? alcance.nombre : t("reglasSupervisora.todasLasPlantillas")}
        </Pastilla>
        <span className="text-pequeno text-tinta-suave">
          {t("reglasSupervisora.datoObservado")}
          <code className="font-semibold text-tinta">{regla.tipo}</code>
        </span>
      </div>

      {regla.mensaje ? (
        <p className="mt-espacio-3 text-pequeno text-tinta-suave">{regla.mensaje}</p>
      ) : null}

      {confirmando && !baja.isPending ? (
        <div
          role="alert"
          className="mt-espacio-3 rounded-panel border border-alerta-borde bg-alerta-tenue px-espacio-3 py-espacio-2 text-pequeno text-alerta-texto"
        >
          {t("reglasSupervisora.confirmarBajaDetalle")}
        </div>
      ) : null}

      {error ? (
        <div
          role="alert"
          className="mt-espacio-3 rounded-panel border border-rojo-borde bg-rojo-tenue px-espacio-3 py-espacio-2 text-pequeno text-rojo-alto"
        >
          {error}
        </div>
      ) : null}

      {editando ? (
        <FormularioRegla regla={regla} procesos={procesos} alCerrar={alCerrarEdicion} />
      ) : null}
    </Tarjeta>
  );
}

function FormularioRegla({
  regla,
  procesos,
  alCerrar,
}: {
  regla?: ReglaSupervisora;
  procesos: Proceso[];
  alCerrar: () => void;
}) {
  const clienteConsultas = useQueryClient();
  const { t } = useIdioma();
  const [plantillaId, setPlantillaId] = useState(regla?.plantillaId ?? "");
  const [nombre, setNombre] = useState(regla?.nombre ?? "");
  const [tipo, setTipo] = useState(regla?.tipo ?? "");
  const [umbral, setUmbral] = useState(regla == null ? "" : String(regla.umbral));
  const [operador, setOperador] = useState<OperadorRegla>(regla?.operador ?? "MENOR");
  const [accion, setAccion] = useState<AccionRegla>(regla?.accion ?? "ADVERTIR");
  const [severidad, setSeveridad] = useState<SeveridadRegla>(regla?.severidad ?? "MEDIA");
  const [mensaje, setMensaje] = useState(regla?.mensaje ?? "");
  const [error, setError] = useState<string | null>(null);

  const valorUmbral = Number(umbral.trim());
  const umbralValido =
    umbral.trim() !== "" && Number.isFinite(valorUmbral) && valorUmbral <= UMBRAL_MAXIMO;
  const errorUmbral =
    umbral.trim() !== "" && !umbralValido
      ? t("reglasSupervisora.umbralError", { maximo: UMBRAL_MAXIMO })
      : undefined;

  const guardar = useMutation({
    mutationFn: () => {
      const cambios: CambiosRegla = {
        plantillaId: plantillaId || undefined,
        nombre: nombre.trim(),
        tipo: tipo.trim(),
        umbral: valorUmbral,
        operador,
        accion,
        severidad,
        mensaje: mensaje.trim() || undefined,
      };
      return regla ? actualizarRegla(regla.id, cambios) : crearRegla(cambios);
    },
    onSuccess: () => {
      setError(null);
      clienteConsultas.invalidateQueries({ queryKey: ["reglas-supervisora"] });
      alCerrar();
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const completo = nombre.trim() !== "" && tipo.trim() !== "" && umbralValido;

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

      <div className="grid gap-espacio-4 md:grid-cols-2">
        <Campo
          etiqueta={t("reglasSupervisora.nombreRegla")}
          value={nombre}
          onChange={(evento) => setNombre(evento.target.value)}
          maxLength={128}
        />
        <Selector
          etiqueta={t("reglasSupervisora.alcance")}
          value={plantillaId}
          onChange={(evento) => setPlantillaId(evento.target.value)}
          ayuda={t("reglasSupervisora.alcanceAyuda")}
        >
          <option value="">{t("reglasSupervisora.todasLasPlantillas")}</option>
          {procesos.map((proceso) => (
            <option key={proceso.id} value={proceso.id}>
              {proceso.nombre}
            </option>
          ))}
        </Selector>
      </div>

      <div className="mt-espacio-4 grid gap-espacio-4 md:grid-cols-3">
        <Campo
          etiqueta={t("reglasSupervisora.datoAObservar")}
          value={tipo}
          onChange={(evento) => setTipo(evento.target.value)}
          maxLength={64}
          ayuda={t("reglasSupervisora.datoAObservarAyuda")}
        />
        <Selector
          etiqueta={t("reglasSupervisora.condicion")}
          value={operador}
          onChange={(evento) => setOperador(evento.target.value as OperadorRegla)}
        >
          {OPERADORES.map((opcion) => (
            <option key={opcion.valor} value={opcion.valor}>
              {t(opcion.texto)}
            </option>
          ))}
        </Selector>
        <Campo
          etiqueta={t("reglasSupervisora.umbral")}
          type="number"
          step="0.01"
          max={UMBRAL_MAXIMO}
          value={umbral}
          onChange={(evento) => setUmbral(evento.target.value)}
          error={errorUmbral}
        />
      </div>

      <div className="mt-espacio-4 grid gap-espacio-4 md:grid-cols-2">
        <Selector
          etiqueta={t("reglasSupervisora.queHace")}
          value={accion}
          onChange={(evento) => setAccion(evento.target.value as AccionRegla)}
          ayuda={
            accion === "BLOQUEAR"
              ? t("reglasSupervisora.bloquearAyuda")
              : undefined
          }
        >
          {ACCIONES.map((opcion) => (
            <option key={opcion.valor} value={opcion.valor}>
              {t(opcion.texto)}
            </option>
          ))}
        </Selector>
        <Selector
          etiqueta={t("reglasSupervisora.severidadHallazgo")}
          value={severidad}
          onChange={(evento) => setSeveridad(evento.target.value as SeveridadRegla)}
        >
          {SEVERIDADES.map((opcion) => (
            <option key={opcion.valor} value={opcion.valor}>
              {t(opcion.texto)}
            </option>
          ))}
        </Selector>
      </div>

      <div className="mt-espacio-4">
        <Campo
          etiqueta={t("reglasSupervisora.mensajeHallazgo")}
          value={mensaje}
          onChange={(evento) => setMensaje(evento.target.value)}
          maxLength={2048}
          placeholder={t("reglasSupervisora.opcional")}
        />
      </div>

      <div className="mt-espacio-4 flex justify-end gap-espacio-2">
        <Boton variante="secundario" onClick={alCerrar} disabled={guardar.isPending}>
          {t("comun.cancelar")}
        </Boton>
        <Boton
          variante="primario"
          cargando={guardar.isPending}
          disabled={guardar.isPending || !completo}
          onClick={() => guardar.mutate()}
        >
          {regla ? t("reglasSupervisora.guardarCambios") : t("reglasSupervisora.crearRegla")}
        </Boton>
      </div>
    </div>
  );
}

function NodoArbol({
  titulo,
  detalle,
  tono = "neutro",
}: {
  titulo: string;
  detalle?: string;
  tono?: "neutro" | "violeta" | "alerta" | "rojo" | "exito";
}) {
  const estilos: Record<string, string> = {
    neutro: "border-borde bg-superficie",
    violeta: "border-violeta-borde bg-violeta-tenue",
    alerta: "border-alerta-borde bg-alerta-tenue",
    rojo: "border-rojo-borde bg-rojo-tenue",
    exito: "border-exito-borde bg-exito-tenue",
  };
  return (
    <div className={`rounded-panel border px-espacio-3 py-espacio-2 ${estilos[tono]}`}>
      <p className="text-pequeno font-semibold text-tinta">{titulo}</p>
      {detalle ? (
        <p className="mt-0.5 text-micro text-tinta-suave">{detalle}</p>
      ) : null}
    </div>
  );
}

function RamaArbol() {
  return (
    <div aria-hidden="true" className="flex justify-center py-0.5">
      <span className="block h-4 w-px bg-borde-fuerte" />
    </div>
  );
}

function ArbolSupervisora() {
  const { t } = useIdioma();
  return (
    <Tarjeta className="border-violeta-borde! bg-violeta-tenue!">
      <CabeceraTarjeta
        titulo={t("reglasSupervisora.arbolTitulo")}
        descripcion={t("reglasSupervisora.arbolDesc")}
      />
      <div className="mt-espacio-4" role="img" aria-label={t("reglasSupervisora.arbolAria")}>
        <NodoArbol
          titulo={t("reglasSupervisora.arbolEntrada")}
          detalle={t("reglasSupervisora.arbolEntradaDesc")}
        />
        <RamaArbol />
        <NodoArbol
          titulo={t("reglasSupervisora.arbolProceso")}
          detalle={t("reglasSupervisora.arbolProcesoDesc")}
        />
        <RamaArbol />
        <NodoArbol
          titulo={t("reglasSupervisora.arbolPaso")}
          detalle={t("reglasSupervisora.arbolPasoDesc")}
        />
        <RamaArbol />
        <div className="rounded-panel border-2 border-accion-primaria bg-superficie p-espacio-3 shadow-sm">
          <div className="flex items-center gap-espacio-2">
            <span
              aria-hidden="true"
              className="inline-flex h-7 w-7 items-center justify-center rounded-full bg-accion-primaria text-pequeno font-bold text-white"
            >
              IA
            </span>
            <p className="text-pequeno font-bold text-tinta">
              {t("reglasSupervisora.arbolSupervisora")}
            </p>
          </div>
          <p className="mt-espacio-1 text-micro text-tinta-media">
            {t("reglasSupervisora.arbolSupervisoraDesc")}
          </p>
          <div className="mt-espacio-3 grid grid-cols-[1fr_auto_1fr] items-center gap-espacio-2">
            <div className="text-center">
              <div className="rounded-control border border-exito-borde bg-exito-tenue px-espacio-2 py-espacio-1">
                <p className="text-micro font-semibold text-exito-texto">
                  {t("reglasSupervisora.arbolSigue")}
                </p>
              </div>
              <p className="mt-1 text-micro text-tinta-suave">
                {t("reglasSupervisora.arbolSigueDesc")}
              </p>
            </div>
            <span aria-hidden="true" className="text-tinta-suave">
              ⇄
            </span>
            <div className="text-center">
              <div className="rounded-control border border-alerta-borde bg-alerta-tenue px-espacio-2 py-espacio-1">
                <p className="text-micro font-semibold text-alerta-texto">
                  {t("reglasSupervisora.arbolHallazgo")}
                </p>
              </div>
              <p className="mt-1 text-micro text-tinta-suave">
                {t("reglasSupervisora.arbolHallazgoDesc")}
              </p>
            </div>
          </div>
        </div>
        <RamaArbol />
        <NodoArbol
          tono="rojo"
          titulo={t("reglasSupervisora.arbolBloqueo")}
          detalle={t("reglasSupervisora.arbolBloqueoDesc")}
        />
      </div>
    </Tarjeta>
  );
}

function ChatSupervisora() {
  const { t } = useIdioma();
  const clienteConsultas = useQueryClient();
  const [entrada, setEntrada] = useState("");
  const [conversacion, setConversacion] = useState<
    { rol: "usuario" | "asistente"; texto: string; propuesta?: PropuestaRegla }[]
  >([]);
  const [error, setError] = useState<string | null>(null);
  const finalRef = useRef<HTMLDivElement>(null);

  const proponer = useMutation({
    mutationFn: (descripcion: string) => generarReglaConIa({ descripcion }),
    onSuccess: (propuesta) => {
      setError(null);
      setConversacion((prev) => [
        ...prev,
        {
          rol: "asistente",
          texto:
            propuesta.explicacion ??
            t("reglasSupervisora.chatPropuestaLista"),
          propuesta,
        },
      ]);
      setTimeout(() => finalRef.current?.scrollIntoView({ behavior: "smooth" }), 60);
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const crear = useMutation({
    mutationFn: (regla: CambiosRegla) => crearRegla(regla),
    onSuccess: () => {
      setConversacion((prev) => [
        ...prev,
        { rol: "asistente", texto: t("reglasSupervisora.chatCreada") },
      ]);
      clienteConsultas.invalidateQueries({ queryKey: ["reglas-supervisora"] });
      setTimeout(() => finalRef.current?.scrollIntoView({ behavior: "smooth" }), 60);
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const enviar = (texto: string) => {
    const descripcion = texto.trim();
    if (!descripcion || proponer.isPending) return;
    setError(null);
    setConversacion((prev) => [...prev, { rol: "usuario", texto: descripcion }]);
    setEntrada("");
    proponer.mutate(descripcion);
  };

  return (
    <Tarjeta className="flex min-h-0 flex-col">
      <CabeceraTarjeta
        titulo={t("reglasSupervisora.chatTitulo")}
        descripcion={t("reglasSupervisora.chatDesc")}
      />
      <div className="mt-espacio-3 flex min-h-0 flex-1 flex-col gap-espacio-3 overflow-y-auto rounded-panel border border-borde bg-lienzo p-espacio-3" style={{ maxHeight: 380 }}>
        {conversacion.length === 0 ? (
          <div className="grid gap-espacio-2">
            {[1, 2, 3].map((ejemplo) => (
              <button
                key={ejemplo}
                type="button"
                onClick={() => enviar(t(`reglasSupervisora.chatEjemplo${ejemplo}`))}
                className="rounded-control border border-borde bg-superficie px-espacio-3 py-espacio-2 text-left text-pequeno text-tinta-media transition hover:border-accion-primaria hover:text-tinta"
              >
                {t(`reglasSupervisora.chatEjemplo${ejemplo}`)}
              </button>
            ))}
          </div>
        ) : null}
        {conversacion.map((mensaje, indice) => (
          <div
            key={indice}
            className={
              mensaje.rol === "usuario"
                ? "ml-8 rounded-panel border border-violeta-borde bg-accion-tonal px-espacio-3 py-espacio-2"
                : "mr-8 rounded-panel border border-borde bg-superficie px-espacio-3 py-espacio-2"
            }
          >
            <p className="text-pequeno text-tinta">{mensaje.texto}</p>
            {mensaje.propuesta?.regla ? (
              <div className="mt-espacio-3 rounded-panel border border-violeta-borde bg-violeta-tenue p-espacio-3">
                <div className="flex flex-wrap items-center gap-espacio-2">
                  <Pastilla tono="violeta">{mensaje.propuesta.regla.nombre}</Pastilla>
                  <Pastilla tono={TONO_SEVERIDAD[mensaje.propuesta.regla.severidad ?? "MEDIA"]}>
                    {t(`prioridad.${mensaje.propuesta.regla.severidad ?? "MEDIA"}`)}
                  </Pastilla>
                </div>
                <p className="mt-espacio-2 text-pequeno text-tinta-media">
                  {textoCondicion(
                    {
                      id: "",
                      nombre: mensaje.propuesta.regla.nombre,
                      tipo: mensaje.propuesta.regla.tipo,
                      umbral: mensaje.propuesta.regla.umbral,
                      operador: mensaje.propuesta.regla.operador,
                    },
                    t,
                  )}
                  {" → "}
                  {textoAccion(mensaje.propuesta.regla.accion, t)}
                </p>
                {(mensaje.propuesta.advertencias ?? []).map((aviso) => (
                  <p key={aviso} className="mt-espacio-1 text-micro text-alerta-texto">
                    {aviso}
                  </p>
                ))}
                <div className="mt-espacio-3">
                  <Boton
                    variante="primario"
                    cargando={crear.isPending}
                    disabled={crear.isPending}
                    onClick={() => crear.mutate(mensaje.propuesta!.regla!)}
                  >
                    {t("reglasSupervisora.chatCrear")}
                  </Boton>
                </div>
              </div>
            ) : null}
          </div>
        ))}
        {proponer.isPending ? (
          <div className="mr-8 rounded-panel border border-borde bg-superficie px-espacio-3 py-espacio-2">
            <p className="animate-pulse text-pequeno text-tinta-suave">
              {t("reglasSupervisora.chatPensando")}
            </p>
          </div>
        ) : null}
        <div ref={finalRef} />
      </div>
      {error ? (
        <div
          role="alert"
          className="mt-espacio-3 rounded-panel border border-rojo-borde bg-rojo-tenue px-espacio-3 py-espacio-2 text-pequeno text-rojo-alto"
        >
          {error}
        </div>
      ) : null}
      <form
        className="mt-espacio-3 flex gap-espacio-2"
        onSubmit={(evento) => {
          evento.preventDefault();
          enviar(entrada);
        }}
      >
        <input
          type="text"
          value={entrada}
          onChange={(evento) => setEntrada(evento.target.value)}
          placeholder={t("reglasSupervisora.chatPlaceholder")}
          maxLength={4000}
          className="min-w-0 flex-1 rounded-control border border-borde bg-superficie px-espacio-3 py-espacio-2 text-pequeno text-tinta outline-none focus:border-accion-primaria"
        />
        <Boton
          variante="primario"
          type="submit"
          disabled={proponer.isPending || !entrada.trim()}
        >
          {t("reglasSupervisora.chatEnviar")}
        </Boton>
      </form>
    </Tarjeta>
  );
}

type Traductor = (ruta: string, params?: Record<string, string | number>) => string;

function textoCondicion(regla: ReglaSupervisora, t: Traductor): string {
  const comparacion = t(
    regla.operador === "MAYOR"
      ? "reglasSupervisora.esMayorQue"
      : "reglasSupervisora.esMenorQue",
  );
  return t("reglasSupervisora.condicionTexto", {
    tipo: regla.tipo,
    comparacion,
    umbral: regla.umbral,
  });
}

function textoAccion(accion: AccionRegla | undefined, t: Traductor): string {
  const encontrada = ACCIONES.find((opcion) => opcion.valor === accion);
  return t(encontrada ? encontrada.texto : "reglasSupervisora.accionAdvertir");
}
