import { useEffect, useState, type ComponentType } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { Contenido, Encabezado } from "../componentes/Disposicion";
import { Cargando, ErrorPanel } from "../componentes/Estados";
import {
  Boton,
  Campo,
  DialogoConfirmacion,
  Pastilla,
  Selector,
  Tarjeta,
} from "../componentes/Interfaz";
import {
  IconoCheck,
  IconoCorreo,
  IconoEliminar,
  IconoGoogle,
  IconoInfo,
  IconoMicrosoft,
  IconoTelegram,
  IconoWhatsapp,
} from "../componentes/Iconos";
import { mensajeDeError } from "../api/cliente";
import {
  eliminarIntegracion,
  guardarIntegracion,
  iniciarOauth,
  listarIntegraciones,
  listarProveedoresOauth,
  probarIntegracion,
  type Integracion,
  type ResultadoPrueba,
  type TipoIntegracion,
} from "../api/integraciones";
import { useIdioma } from "../contextos/ProveedorIdioma";
import type { Tono } from "../componentes/Interfaz";

const ICONO_TIPO: Record<TipoIntegracion, ComponentType<{ tamano?: number }>> = {
  CORREO: IconoCorreo,
  TELEGRAM: IconoTelegram,
  WHATSAPP: IconoWhatsapp,
};

export function Integraciones() {
  const { t } = useIdioma();
  const [parametros, setParametros] = useSearchParams();
  const [avisoConexion] = useState<string | null>(() => {
    if (!parametros.get("conectado")) {
      return null;
    }
    const proveedor = parametros.get("proveedor");
    const nombre = proveedor === "google" ? "Gmail" : proveedor === "microsoft" ? "Microsoft 365" : proveedor ?? "";
    return t("integraciones.oauth.conectada", { nombre });
  });
  const [errorConexion] = useState<string | null>(() =>
    parametros.get("error"),
  );

  useEffect(() => {
    if (parametros.get("conectado") || parametros.get("error")) {
      setParametros({}, { replace: true });
    }
  }, [parametros, setParametros]);

  const consulta = useQuery({
    queryKey: ["integraciones"],
    queryFn: listarIntegraciones,
  });
  const consultaOauth = useQuery({
    queryKey: ["integraciones-oauth"],
    queryFn: listarProveedoresOauth,
  });

  const datos = consulta.data ?? [];
  const correo = datos.find((i) => i.tipo === "CORREO");
  const telegram = datos.find((i) => i.tipo === "TELEGRAM");
  const whatsapp = datos.find((i) => i.tipo === "WHATSAPP");
  const oauthDisponibles = consultaOauth.data ?? [];

  return (
    <>
      <Encabezado
        titulo={t("integraciones.titulo")}
        descripcion={t("integraciones.descripcion")}
      />
      <Contenido>
        {avisoConexion ? (
          <p
            role="status"
            className="mb-espacio-5 flex items-center gap-espacio-2 rounded-panel border border-exito-borde bg-exito-tenue p-espacio-4 text-pequeno font-semibold text-exito-texto"
          >
            <IconoCheck tamano={16} />
            {avisoConexion}
          </p>
        ) : null}
        {errorConexion ? (
          <p
            role="alert"
            className="mb-espacio-5 rounded-panel border border-rojo-borde bg-rojo-tenue p-espacio-4 text-pequeno text-rojo-texto"
          >
            {errorConexion}
          </p>
        ) : null}
        <div
          role="note"
          className="mb-espacio-5 flex items-start gap-espacio-3 rounded-panel border border-violeta-borde bg-violeta-tenue p-espacio-4 text-pequeno text-tinta"
        >
          <span aria-hidden="true" className="mt-px shrink-0 text-violeta">
            <IconoInfo tamano={18} />
          </span>
          <p className="min-w-0 [overflow-wrap:anywhere]">
            {t("integraciones.ayuda")}
          </p>
        </div>
        {consulta.isPending ? (
          <Cargando filas={3} alto="h-40" />
        ) : consulta.isError ? (
          <ErrorPanel
            titulo={t("integraciones.errorCarga")}
            mensaje={mensajeDeError(consulta.error)}
          />
        ) : (
          <div className="flex flex-col gap-espacio-6">
            <section>
              <h2 className="mb-espacio-3 text-base font-semibold text-tinta">
                {t("integraciones.seccion.correo")}
              </h2>
              <div className="grid gap-espacio-5 xl:grid-cols-2">
                <TarjetaOauth
                  proveedor="google"
                  disponible={oauthDisponibles.includes("google")}
                  integracion={correo}
                  modoEsperado="OAUTH_GOOGLE"
                  Icono={IconoGoogle}
                />
                <TarjetaOauth
                  proveedor="microsoft"
                  disponible={oauthDisponibles.includes("microsoft")}
                  integracion={correo}
                  modoEsperado="OAUTH_MICROSOFT"
                  Icono={IconoMicrosoft}
                />
                {correo ? (
                  <TarjetaIntegracion
                    integracion={correo}
                    titulo={t("integraciones.nombre.SMTP")}
                    avisoPrevio={
                      correo.configurada &&
                      correo.configuracion?.modo?.startsWith("OAUTH_")
                        ? t("integraciones.smtpReemplaza", {
                            cuenta: correo.configuracion?.correo ?? "",
                          })
                        : undefined
                    }
                  />
                ) : null}
              </div>
            </section>
            <section>
              <h2 className="mb-espacio-3 text-base font-semibold text-tinta">
                {t("integraciones.seccion.mensajeria")}
              </h2>
              <div className="grid gap-espacio-5 xl:grid-cols-2">
                {telegram ? (
                  <TarjetaIntegracion
                    integracion={telegram}
                    guia={[
                      t("integraciones.guia.telegram.1"),
                      t("integraciones.guia.telegram.2"),
                      t("integraciones.guia.telegram.3"),
                    ]}
                  />
                ) : null}
                {whatsapp ? (
                  <TarjetaIntegracion
                    integracion={whatsapp}
                    guia={[
                      t("integraciones.guia.whatsapp.1"),
                      t("integraciones.guia.whatsapp.2"),
                      t("integraciones.guia.whatsapp.3"),
                    ]}
                  />
                ) : null}
              </div>
            </section>
            <section>
              <h2 className="mb-espacio-3 text-base font-semibold text-tinta">
                {t("integraciones.seccion.proximamente")}
              </h2>
              <div className="grid gap-espacio-5 xl:grid-cols-2">
                {["slack", "googledrive"].map((app) => (
                  <Tarjeta key={app} className="opacity-60">
                    <div className="flex items-center justify-between gap-espacio-3">
                      <div className="flex items-center gap-espacio-3">
                        <span
                          aria-hidden="true"
                          className="grid size-espacio-10 shrink-0 place-items-center rounded-insignia bg-tinta-tenue text-tinta-media"
                        >
                          <IconoCorreo tamano={22} />
                        </span>
                        <div className="min-w-0">
                          <h3 className="text-base font-semibold text-tinta">
                            {t(`integraciones.catalogo.${app}.nombre`)}
                          </h3>
                          <p className="text-micro text-tinta-media">
                            {t(`integraciones.catalogo.${app}.detalle`)}
                          </p>
                        </div>
                      </div>
                      <Pastilla tono="neutro">
                        {t("integraciones.proximamente")}
                      </Pastilla>
                    </div>
                  </Tarjeta>
                ))}
              </div>
            </section>
          </div>
        )}
      </Contenido>
    </>
  );
}

function TarjetaOauth({
  proveedor,
  disponible,
  integracion,
  modoEsperado,
  Icono,
}: {
  proveedor: "google" | "microsoft";
  disponible: boolean;
  integracion?: Integracion;
  modoEsperado: string;
  Icono: ComponentType<{ tamano?: number }>;
}) {
  const { t } = useIdioma();
  const clienteConsultas = useQueryClient();
  const [destinoPrueba, setDestinoPrueba] = useState("");
  const [resultado, setResultado] = useState<ResultadoPrueba | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [confirmarBaja, setConfirmarBaja] = useState(false);

  const refrescar = () =>
    clienteConsultas.invalidateQueries({ queryKey: ["integraciones"] });

  const conectada =
    integracion?.configurada === true &&
    integracion.configuracion?.modo === modoEsperado;
  const cuenta = integracion?.configuracion?.correo;
  const otraActiva =
    integracion?.configurada === true &&
    integracion.configuracion?.modo !== modoEsperado;

  const conectar = useMutation({
    mutationFn: () => iniciarOauth(proveedor),
    onSuccess: (url) => {
      window.location.href = url;
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const probar = useMutation({
    mutationFn: () =>
      probarIntegracion("CORREO", destinoPrueba || undefined),
    onSuccess: (salida) => {
      setResultado(salida);
      if (salida.exitosa) {
        refrescar();
      }
    },
    onError: (fallo) =>
      setResultado({ exitosa: false, detalle: mensajeDeError(fallo) }),
  });

  const desconectar = useMutation({
    mutationFn: () => eliminarIntegracion("CORREO"),
    onSuccess: () => {
      setConfirmarBaja(false);
      setResultado(null);
      refrescar();
    },
    onError: (fallo) => {
      setConfirmarBaja(false);
      setError(mensajeDeError(fallo));
    },
  });

  const estado: { texto: string; tono: Tono } = conectada
    ? integracion?.verificadaEn
      ? { texto: t("integraciones.verificada"), tono: "exito" }
      : { texto: t("integraciones.configurada"), tono: "informacion" }
    : { texto: t("integraciones.sinConfigurar"), tono: "neutro" };

  return (
    <Tarjeta>
      <div className="flex items-start justify-between gap-espacio-3">
        <div className="flex items-center gap-espacio-3">
          <span
            aria-hidden="true"
            className="grid size-espacio-10 shrink-0 place-items-center rounded-insignia bg-violeta-tenue text-violeta"
          >
            <Icono tamano={22} />
          </span>
          <div className="min-w-0">
            <h3 className="text-base font-semibold text-tinta">
              {t(`integraciones.catalogo.${proveedor}.nombre`)}
            </h3>
            <p className="text-micro text-tinta-media">
              {t(`integraciones.catalogo.${proveedor}.detalle`)}
            </p>
          </div>
        </div>
        <Pastilla tono={estado.tono}>{estado.texto}</Pastilla>
      </div>

      {conectada && cuenta ? (
        <p className="mt-espacio-3 text-pequeno text-tinta">
          {t("integraciones.oauth.conectadaComo", { cuenta })}
        </p>
      ) : null}
      {!conectada && otraActiva ? (
        <p className="mt-espacio-3 text-micro text-tinta-media">
          {t("integraciones.oauth.otraActiva")}
        </p>
      ) : null}

      {conectada ? (
        <Campo
          className="mt-espacio-3"
          etiqueta={t("integraciones.destinoPrueba")}
          ayuda={t("integraciones.destinoPruebaAyuda")}
          type="email"
          value={destinoPrueba}
          onChange={(e) => setDestinoPrueba(e.target.value)}
        />
      ) : null}

      {error ? (
        <p role="alert" className="mt-espacio-3 text-pequeno text-rojo-alto">
          {error}
        </p>
      ) : null}
      {resultado ? (
        <div
          role="status"
          className={`mt-espacio-3 rounded-panel border p-espacio-3 text-pequeno ${
            resultado.exitosa
              ? "border-exito-borde bg-exito-tenue text-exito-texto"
              : "border-rojo-borde bg-rojo-tenue text-rojo-texto"
          }`}
        >
          {resultado.detalle}
        </div>
      ) : null}

      <div className="mt-espacio-4 flex flex-wrap items-center gap-espacio-2">
        {conectada ? (
          <>
            <Boton
              type="button"
              variante="secundario"
              cargando={probar.isPending}
              onClick={() => probar.mutate()}
            >
              {t("integraciones.probar")}
            </Boton>
            <Boton
              type="button"
              variante="fantasma"
              className="text-rojo-alto"
              onClick={() => setConfirmarBaja(true)}
            >
              <IconoEliminar tamano={14} />
              {t("integraciones.oauth.desconectar")}
            </Boton>
          </>
        ) : disponible ? (
          <Boton
            type="button"
            variante="primario"
            cargando={conectar.isPending}
            onClick={() => conectar.mutate()}
          >
            {t(`integraciones.oauth.conectar.${proveedor}`)}
          </Boton>
        ) : (
          <p className="text-micro text-tinta-media">
            {t("integraciones.oauth.noDisponible")}
          </p>
        )}
      </div>

      {confirmarBaja ? (
        <DialogoConfirmacion
          titulo={t("integraciones.oauth.desconectarTitulo")}
          descripcion={t("integraciones.oauth.desconectarConfirmar", {
            cuenta: cuenta ?? "",
          })}
          etiquetaConfirmar={t("integraciones.oauth.desconectar")}
          cargando={desconectar.isPending}
          alConfirmar={() => desconectar.mutate()}
          alCancelar={() => setConfirmarBaja(false)}
        />
      ) : null}
    </Tarjeta>
  );
}

function TarjetaIntegracion({
  integracion,
  titulo,
  guia,
  avisoPrevio,
}: {
  integracion: Integracion;
  titulo?: string;
  guia?: string[];
  avisoPrevio?: string;
}) {
  const { t } = useIdioma();
  const clienteConsultas = useQueryClient();
  const [valores, setValores] = useState<Record<string, string>>({});
  const [habilitada, setHabilitada] = useState(integracion.habilitada);
  const [destinoPrueba, setDestinoPrueba] = useState("");
  const [resultado, setResultado] = useState<ResultadoPrueba | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [confirmarBaja, setConfirmarBaja] = useState(false);

  useEffect(() => {
    setValores(integracion.configuracion ?? {});
    setHabilitada(integracion.habilitada);
    setResultado(null);
    setError(null);
  }, [integracion]);

  const refrescar = () =>
    clienteConsultas.invalidateQueries({ queryKey: ["integraciones"] });

  const guardar = useMutation({
    mutationFn: () =>
      guardarIntegracion(integracion.tipo, valores, habilitada),
    onSuccess: () => {
      setError(null);
      setAviso(t("integraciones.guardada"));
      refrescar();
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const probar = useMutation({
    mutationFn: () =>
      probarIntegracion(
        integracion.tipo,
        integracion.tipo === "CORREO" ? destinoPrueba || undefined : undefined,
      ),
    onSuccess: (salida) => {
      setResultado(salida);
      if (salida.exitosa) {
        refrescar();
      }
    },
    onError: (fallo) =>
      setResultado({ exitosa: false, detalle: mensajeDeError(fallo) }),
  });

  const eliminar = useMutation({
    mutationFn: () => eliminarIntegracion(integracion.tipo),
    onSuccess: () => {
      setConfirmarBaja(false);
      setResultado(null);
      setAviso(t("integraciones.eliminada"));
      refrescar();
    },
    onError: (fallo) => {
      setConfirmarBaja(false);
      setError(mensajeDeError(fallo));
    },
  });

  const Icono = ICONO_TIPO[integracion.tipo];
  const estado: { texto: string; tono: Tono } = !integracion.configurada
    ? { texto: t("integraciones.sinConfigurar"), tono: "neutro" }
    : !integracion.habilitada
      ? { texto: t("integraciones.deshabilitada"), tono: "alerta" }
      : integracion.verificadaEn
        ? { texto: t("integraciones.verificada"), tono: "exito" }
        : { texto: t("integraciones.configurada"), tono: "informacion" };

  return (
    <Tarjeta>
      <div className="flex items-start justify-between gap-espacio-3">
        <div className="flex items-center gap-espacio-3">
          <span
            aria-hidden="true"
            className="grid size-espacio-10 shrink-0 place-items-center rounded-insignia bg-violeta-tenue text-violeta"
          >
            <Icono tamano={22} />
          </span>
          <div className="min-w-0">
            <h3 className="text-base font-semibold text-tinta">
              {titulo ?? t(`integraciones.nombre.${integracion.tipo}`)}
            </h3>
            <p className="text-micro text-tinta-media">
              {t(`integraciones.detalle.${integracion.tipo}`)}
            </p>
          </div>
        </div>
        <Pastilla tono={estado.tono}>{estado.texto}</Pastilla>
      </div>

      {avisoPrevio ? (
        <p className="mt-espacio-3 rounded-panel border border-informacion-borde bg-informacion-tenue p-espacio-3 text-pequeno text-tinta">
          {avisoPrevio}
        </p>
      ) : null}

      {guia ? (
        <ol className="mt-espacio-3 list-decimal space-y-espacio-1 pl-espacio-5 text-pequeno text-tinta-media">
          {guia.map((paso, indice) => (
            <li key={indice}>{paso}</li>
          ))}
        </ol>
      ) : null}

      <div className="mt-espacio-4 grid gap-espacio-3 sm:grid-cols-2">
        {integracion.campos.map((campo) =>
          campo === "seguridad" ? (
            <Selector
              key={campo}
              etiqueta={t(`integraciones.campo.${campo}`)}
              value={valores[campo] ?? "STARTTLS"}
              onChange={(e) =>
                setValores((previo) => ({ ...previo, [campo]: e.target.value }))
              }
            >
              <option value="STARTTLS">STARTTLS</option>
              <option value="SSL">SSL/TLS</option>
              <option value="NINGUNA">{t("integraciones.ninguna")}</option>
            </Selector>
          ) : (
            <Campo
              key={campo}
              etiqueta={t(`integraciones.campo.${campo}`)}
              type={integracion.secretos.includes(campo) ? "password" : "text"}
              value={valores[campo] ?? ""}
              placeholder={
                integracion.secretos.includes(campo) && integracion.configurada
                  ? "••••••••"
                  : undefined
              }
              ayuda={
                integracion.secretos.includes(campo) && integracion.configurada
                  ? t("integraciones.secretoGuardado")
                  : undefined
              }
              onChange={(e) =>
                setValores((previo) => ({ ...previo, [campo]: e.target.value }))
              }
            />
          ),
        )}
      </div>

      {integracion.tipo === "CORREO" && integracion.configurada ? (
        <Campo
          className="mt-espacio-3"
          etiqueta={t("integraciones.destinoPrueba")}
          ayuda={t("integraciones.destinoPruebaAyuda")}
          type="email"
          value={destinoPrueba}
          onChange={(e) => setDestinoPrueba(e.target.value)}
        />
      ) : null}

      <label className="mt-espacio-4 flex cursor-pointer items-center gap-espacio-2 text-pequeno text-tinta">
        <input
          type="checkbox"
          className="size-espacio-4 accent-violeta"
          checked={habilitada}
          onChange={(e) => setHabilitada(e.target.checked)}
        />
        {t("integraciones.habilitada")}
      </label>

      {aviso ? (
        <p
          role="status"
          className="mt-espacio-3 flex items-center gap-espacio-2 text-pequeno font-semibold text-exito-texto"
        >
          <IconoCheck tamano={14} />
          {aviso}
        </p>
      ) : null}
      {error ? (
        <p role="alert" className="mt-espacio-3 text-pequeno text-rojo-alto">
          {error}
        </p>
      ) : null}
      {resultado ? (
        <div
          role="status"
          className={`mt-espacio-3 rounded-panel border p-espacio-3 text-pequeno ${
            resultado.exitosa
              ? "border-exito-borde bg-exito-tenue text-exito-texto"
              : "border-rojo-borde bg-rojo-tenue text-rojo-texto"
          }`}
        >
          {resultado.detalle}
        </div>
      ) : null}

      <div className="mt-espacio-4 flex flex-wrap items-center gap-espacio-2">
        <Boton
          type="button"
          variante="primario"
          cargando={guardar.isPending}
          onClick={() => guardar.mutate()}
        >
          {t("comun.guardar")}
        </Boton>
        <Boton
          type="button"
          variante="secundario"
          cargando={probar.isPending}
          disabled={!integracion.configurada || !habilitada}
          onClick={() => probar.mutate()}
        >
          {t("integraciones.probar")}
        </Boton>
        {integracion.configurada ? (
          <Boton
            type="button"
            variante="fantasma"
            className="text-rojo-alto"
            onClick={() => setConfirmarBaja(true)}
          >
            <IconoEliminar tamano={14} />
            {t("integraciones.eliminar")}
          </Boton>
        ) : null}
      </div>

      {confirmarBaja ? (
        <DialogoConfirmacion
          titulo={t("integraciones.eliminarTitulo")}
          descripcion={t("integraciones.eliminarConfirmar", {
            nombre: t(`integraciones.nombre.${integracion.tipo}`),
          })}
          etiquetaConfirmar={t("integraciones.eliminar")}
          cargando={eliminar.isPending}
          alConfirmar={() => eliminar.mutate()}
          alCancelar={() => setConfirmarBaja(false)}
        />
      ) : null}
    </Tarjeta>
  );
}
