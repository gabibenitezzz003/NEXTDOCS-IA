import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { Logotipo } from "../componentes/Marca";
import { AreaTexto, Boton, Campo, Pastilla, Tarjeta } from "../componentes/Interfaz";
import { Cargando } from "../componentes/Estados";
import { AlternarTema } from "../componentes/Tema";
import { SelectorIdioma } from "../componentes/Idioma";
import {
  IconoCheck,
  IconoEnlaceExterno,
  IconoInfo,
} from "../componentes/Iconos";
import { useIdioma } from "../contextos/ProveedorIdioma";
import {
  obtenerEnlaceExterno,
  usarEnlaceExterno,
  mensajeDeError,
} from "../api/procesos";
import { formatearFecha } from "./Documentos";

type TonoPastilla = "neutro" | "exito" | "rojo" | "alerta" | "informacion";

function tonoEstadoEnlace(estado?: string): TonoPastilla {
  switch (estado) {
    case "ACTIVO":
      return "exito";
    case "EXPIRADO":
      return "alerta";
    case "REVOCADO":
    case "USADO":
      return "rojo";
    default:
      return "neutro";
  }
}

export function Externo() {
  const { t } = useIdioma();
  const { token = "" } = useParams<{ token: string }>();
  const [decision, setDecision] = useState<"APROBADO" | "RECHAZADO" | null>(null);
  const [motivo, setMotivo] = useState("");
  const [observaciones, setObservaciones] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [enviada, setEnviada] = useState<"APROBADO" | "RECHAZADO" | null>(null);

  const consulta = useQuery({
    queryKey: ["enlaceExterno", token],
    queryFn: () => obtenerEnlaceExterno(token),
    enabled: Boolean(token),
    retry: false,
  });

  const completar = useMutation({
    mutationFn: (eleccion: "APROBADO" | "RECHAZADO") =>
      usarEnlaceExterno(token, {
        decision: eleccion,
        motivo: motivo.trim() || undefined,
        datos: observaciones.trim()
          ? { observaciones: observaciones.trim() }
          : undefined,
      }),
    onSuccess: (_resultado, eleccion) => {
      setError(null);
      setEnviada(eleccion);
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const enlace = consulta.data;
  const enlaceActivo = enlace?.estado === "ACTIVO";
  const tareaAbierta =
    enlace?.estadoTarea === "PENDIENTE" || enlace?.estadoTarea === "VENCIDA";
  const puedeResponder = Boolean(enlaceActivo && tareaAbierta && !enviada);

  return (
    <main className="flex min-h-dvh flex-col items-center bg-lienzo px-espacio-4 py-espacio-8">
      <div className="flex w-full max-w-[560px] items-center justify-between">
        <Logotipo />
        <div className="flex items-center gap-espacio-2">
          <SelectorIdioma />
          <AlternarTema />
        </div>
      </div>

      <div className="mt-espacio-8 w-full max-w-[560px]">
        {consulta.isPending ? (
          <Tarjeta padding="px-espacio-6 py-espacio-6">
            <Cargando filas={4} alto="h-10" />
          </Tarjeta>
        ) : consulta.isError || !enlace ? (
          <Tarjeta padding="px-espacio-6 py-espacio-8" className="text-center">
            <div className="mx-auto flex size-espacio-10 items-center justify-center rounded-insignia bg-rojo-tenue text-rojo-alto">
              <IconoInfo tamano={22} />
            </div>
            <h1 className="mt-espacio-4 font-titulo text-titulo-seccion text-tinta">
              {t("externo.invalidoTitulo")}
            </h1>
            <p className="mt-espacio-2 text-pequeno text-tinta-suave">
              {t("externo.invalidoDescripcion")}
            </p>
          </Tarjeta>
        ) : (
          <Tarjeta padding="px-espacio-6 py-espacio-6">
            <div className="flex flex-wrap items-center justify-between gap-espacio-3">
              <div className="flex items-center gap-espacio-3">
                <span className="flex size-espacio-8 items-center justify-center rounded-control bg-violeta-tenue text-violeta">
                  <IconoEnlaceExterno tamano={16} />
                </span>
                <h1 className="font-titulo text-titulo-seccion text-tinta">
                  {t("externo.titulo")}
                </h1>
              </div>
              <Pastilla tono={tonoEstadoEnlace(enlace.estado)}>
                {t(`estadoEnlace.${enlace.estado ?? "ACTIVO"}`)}
              </Pastilla>
            </div>

            <p className="mt-espacio-2 text-pequeno text-tinta-suave">
              {t("externo.descripcion")}
            </p>

            <dl className="mt-espacio-5 space-y-espacio-3 rounded-panel border border-borde bg-lienzo px-espacio-4 py-espacio-4">
              <div className="flex flex-wrap justify-between gap-espacio-2">
                <dt className="text-pequeno text-tinta-suave">{t("externo.proceso")}</dt>
                <dd className="text-pequeno font-semibold text-tinta">
                  {enlace.nombreProceso?.trim() || enlace.codigoProceso || "—"}
                </dd>
              </div>
              <div className="flex flex-wrap justify-between gap-espacio-2">
                <dt className="text-pequeno text-tinta-suave">{t("externo.paso")}</dt>
                <dd className="text-pequeno font-semibold text-tinta">
                  {enlace.nombrePaso || "—"}
                </dd>
              </div>
              {enlace.vencimientoTarea ? (
                <div className="flex flex-wrap justify-between gap-espacio-2">
                  <dt className="text-pequeno text-tinta-suave">
                    {t("externo.venceTarea")}
                  </dt>
                  <dd className="text-pequeno font-semibold text-tinta">
                    {formatearFecha(enlace.vencimientoTarea)}
                  </dd>
                </div>
              ) : null}
              {enlace.expiracion ? (
                <div className="flex flex-wrap justify-between gap-espacio-2">
                  <dt className="text-pequeno text-tinta-suave">
                    {t("externo.venceEnlace")}
                  </dt>
                  <dd className="text-pequeno font-semibold text-tinta">
                    {formatearFecha(enlace.expiracion)}
                  </dd>
                </div>
              ) : null}
            </dl>

            {enviada ? (
              <div className="mt-espacio-6 flex flex-col items-center gap-espacio-3 text-center">
                <div className="flex size-espacio-10 items-center justify-center rounded-insignia bg-exito-tenue text-exito-texto">
                  <IconoCheck tamano={22} />
                </div>
                <h2 className="font-titulo text-titulo-panel text-tinta">
                  {t("externo.exitoTitulo")}
                </h2>
                <p className="text-pequeno text-tinta-suave">
                  {t("externo.exitoDescripcion")}
                </p>
                <Pastilla
                  tono={enviada === "APROBADO" ? "exito" : "rojo"}
                  solido
                >
                  {t(`estadoHallazgo.${enviada}`)}
                </Pastilla>
              </div>
            ) : !puedeResponder ? (
              <div className="mt-espacio-6 rounded-panel border border-borde bg-superficie px-espacio-4 py-espacio-4 text-center">
                <p className="text-pequeno font-semibold text-tinta">
                  {enlace.estado === "REVOCADO"
                    ? t("externo.revocado")
                    : enlace.estado === "EXPIRADO"
                      ? t("externo.expirado")
                      : enlace.estado === "USADO" || enlace.completada
                        ? t("externo.usado")
                        : t("externo.cerrada")}
                </p>
                {enlace.decision ? (
                  <p className="mt-espacio-2 text-pequeno text-tinta-suave">
                    {t("externo.respuestaRegistrada", {
                      decision: t(`estadoHallazgo.${enlace.decision}`),
                    })}
                    {enlace.motivo ? ` · ${enlace.motivo}` : ""}
                  </p>
                ) : null}
              </div>
            ) : (
              <div className="mt-espacio-6 space-y-espacio-4">
                {!decision ? (
                  <div className="grid gap-espacio-3 sm:grid-cols-2">
                    <Boton
                      variante="primario"
                      tamano="lg"
                      onClick={() => setDecision("APROBADO")}
                    >
                      {t("operacion.aprobado")}
                    </Boton>
                    <Boton
                      variante="secundario"
                      tamano="lg"
                      onClick={() => setDecision("RECHAZADO")}
                    >
                      {t("operacion.rechazado")}
                    </Boton>
                  </div>
                ) : (
                  <>
                    <p className="text-pequeno font-semibold text-tinta">
                      {t("externo.elegiste", {
                        decision: t(`estadoHallazgo.${decision}`),
                      })}
                    </p>
                    {decision === "RECHAZADO" ? (
                      <Campo
                        etiqueta={t("operacion.motivoRechazo")}
                        placeholder={t("operacion.motivoRechazoPlaceholder")}
                        value={motivo}
                        onChange={(evento) => setMotivo(evento.target.value)}
                      />
                    ) : null}
                    <AreaTexto
                      etiqueta={t("externo.observaciones")}
                      placeholder={t("externo.observacionesPlaceholder")}
                      value={observaciones}
                      onChange={(evento) => setObservaciones(evento.target.value)}
                      rows={3}
                    />
                    {error ? (
                      <div
                        role="alert"
                        className="rounded-panel border border-rojo-borde bg-rojo-tenue px-espacio-4 py-espacio-3 text-pequeno text-rojo-alto"
                      >
                        {error}
                      </div>
                    ) : null}
                    <div className="flex justify-end gap-espacio-3">
                      <Boton
                        variante="fantasma"
                        onClick={() => {
                          setDecision(null);
                          setError(null);
                        }}
                      >
                        {t("comun.volver")}
                      </Boton>
                      <Boton
                        variante={decision === "RECHAZADO" ? "peligro" : "primario"}
                        cargando={completar.isPending}
                        disabled={decision === "RECHAZADO" && !motivo.trim()}
                        onClick={() => completar.mutate(decision)}
                      >
                        {t("comun.confirmar")}
                      </Boton>
                    </div>
                  </>
                )}
              </div>
            )}
          </Tarjeta>
        )}
      </div>
    </main>
  );
}
