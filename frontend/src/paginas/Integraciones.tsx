import { useEffect, useState, type ComponentType } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
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
  IconoInfo,
  IconoTelegram,
  IconoWhatsapp,
} from "../componentes/Iconos";
import { mensajeDeError } from "../api/cliente";
import {
  eliminarIntegracion,
  guardarIntegracion,
  listarIntegraciones,
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
  const consulta = useQuery({
    queryKey: ["integraciones"],
    queryFn: listarIntegraciones,
  });

  return (
    <>
      <Encabezado
        titulo={t("integraciones.titulo")}
        descripcion={t("integraciones.descripcion")}
      />
      <Contenido>
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
          <div className="grid gap-espacio-5 xl:grid-cols-2">
            {(consulta.data ?? []).map((integracion) => (
              <TarjetaIntegracion
                key={integracion.tipo}
                integracion={integracion}
              />
            ))}
          </div>
        )}
      </Contenido>
    </>
  );
}

function TarjetaIntegracion({ integracion }: { integracion: Integracion }) {
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
              {t(`integraciones.nombre.${integracion.tipo}`)}
            </h3>
            <p className="text-micro text-tinta-media">
              {t(`integraciones.detalle.${integracion.tipo}`)}
            </p>
          </div>
        </div>
        <Pastilla tono={estado.tono}>{estado.texto}</Pastilla>
      </div>

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
