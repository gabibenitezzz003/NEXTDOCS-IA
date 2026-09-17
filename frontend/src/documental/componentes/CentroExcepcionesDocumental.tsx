import { useMemo, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  obtenerExcepcionesMotor,
  resolverExcepcion,
  type ExcepcionMotor,
} from "../../api/documental";
import { mensajeDeError } from "../../api/cliente";
import {
  Boton,
  BotonIcono,
  Panel,
  Selector,
  AreaTexto,
  Tarjeta,
} from "../../componentes/Interfaz";
import { Cargando, ErrorPanel, Vacio } from "../../componentes/Estados";
import { IconoBuscar } from "../../componentes/Iconos";
import { useIdioma } from "../../contextos/ProveedorIdioma";
import { comoFecha, horasRestantes, motivoHumano } from "../dominio";
import { EtiquetaSeveridadMotor } from "./EtiquetaEstadoMotor";

const DECISIONES = ["RESUELTA", "DESCARTADA", "ESCALADA"] as const;

function Vencimiento({ vence }: { vence: string | null }) {
  const { t } = useIdioma();
  const horas = horasRestantes(vence);

  if (horas === null)
    return <span className="text-micro text-tinta-suave">-</span>;

  if (horas < 0) {
    return (
      <span className="inline-flex max-w-fit rounded-insignia bg-rojo-tenue px-espacio-2 py-espacio-1 text-micro font-medium text-rojo-alto">
        {t("documental.excepciones.vencida")}
      </span>
    );
  }

  const texto =
    horas < 1
      ? t("documental.excepciones.minutos", { valor: Math.round(horas * 60) })
      : t("documental.excepciones.horas", { valor: Math.round(horas) });

  return (
    <span
      className={`inline-flex max-w-fit rounded-insignia px-espacio-2 py-espacio-1 text-micro font-medium ${
        horas <= 2
          ? "bg-alerta-tenue text-alerta-texto"
          : "bg-lienzo text-tinta-media"
      }`}
    >
      {texto}
    </span>
  );
}

export function CentroExcepcionesDocumental({
  alAbrirDocumento,
}: {
  alAbrirDocumento: (documentoId: string) => void;
}) {
  const { t, idioma } = useIdioma();
  const clienteConsultas = useQueryClient();

  const [estado, setEstado] = useState("ABIERTA");
  const [enResolucion, setEnResolucion] = useState<ExcepcionMotor | null>(null);
  const [decision, setDecision] = useState<string>("RESUELTA");
  const [resolucion, setResolucion] = useState("");
  const [aviso, setAviso] = useState<{
    tono: "ok" | "error";
    texto: string;
  } | null>(null);

  const filtros = useMemo(() => ({ estado, limite: 100 }), [estado]);

  const excepciones = useQuery({
    queryKey: ["documental-excepciones", filtros],
    queryFn: () => obtenerExcepcionesMotor(filtros),
    staleTime: 15000,
    refetchInterval: 30000,
    placeholderData: (previo) => previo,
  });

  const cerrar = () => {
    setEnResolucion(null);
    setResolucion("");
    setDecision("RESUELTA");
  };

  const confirmar = async () => {
    if (!enResolucion) return;
    try {
      await resolverExcepcion(enResolucion.id, { decision, resolucion });
      setAviso({ tono: "ok", texto: t("documental.excepciones.resuelta") });
      cerrar();
      clienteConsultas.invalidateQueries({
        queryKey: ["documental-excepciones"],
      });
    } catch (error) {
      setAviso({ tono: "error", texto: mensajeDeError(error) });
    }
  };

  const listado = excepciones.data || [];

  return (
    <div
      className="flex h-full flex-col gap-espacio-4"
      data-testid="documental-excepciones"
    >
      {aviso ? (
        <p
          role="status"
          className={`rounded-control px-espacio-3 py-espacio-2 text-pequeno font-semibold ${
            aviso.tono === "ok"
              ? "bg-exito-tenue text-exito-texto"
              : "bg-rojo-tenue text-rojo-alto"
          }`}
        >
          {aviso.texto}
        </p>
      ) : null}

      <Tarjeta className="flex min-h-0 flex-1 flex-col" padding="p-0">
        <div className="flex flex-wrap items-end gap-espacio-3 border-b border-borde px-espacio-4 py-espacio-3">
          <Selector
            etiqueta={t("documental.excepciones.colEstado")}
            value={estado}
            onChange={(evento) => setEstado(evento.target.value)}
            className="w-44"
          >
            <option value="ABIERTA">
              {t("documental.estadoExcepcion.ABIERTA")}
            </option>
            <option value="RESUELTA">
              {t("documental.estadoExcepcion.RESUELTA")}
            </option>
            <option value="DESCARTADA">
              {t("documental.estadoExcepcion.DESCARTADA")}
            </option>
            <option value="ESCALADA">
              {t("documental.estadoExcepcion.ESCALADA")}
            </option>
            <option value="">{t("documental.bandeja.todos")}</option>
          </Selector>
        </div>

        <div className="min-h-0 flex-1 overflow-auto">
        {excepciones.isError ? (
          <div className="p-espacio-4">
            <ErrorPanel
              error={excepciones.error}
              mensaje={mensajeDeError(excepciones.error)}
              reintentar={() => excepciones.refetch()}
            />
          </div>
        ) : excepciones.isLoading ? (
          <Cargando />
        ) : !listado.length ? (
          <Vacio
            titulo={t("documental.excepciones.vacio")}
            detalle={t("documental.excepciones.vacioDetalle")}
          />
        ) : (
          <table className="w-full border-collapse">
            <thead className="sticky top-0 z-10">
              <tr className="border-b border-borde bg-lienzo">
                {[
                  "colMotivo",
                  "colSeveridad",
                  "colPrioridad",
                  "colAccion",
                  "colVence",
                  "colCreada",
                ].map((clave) => (
                  <th
                    key={clave}
                    className="px-espacio-4 py-espacio-3 text-left text-micro font-bold uppercase tracking-wider text-tinta-suave"
                  >
                    {t(`documental.excepciones.${clave}`)}
                  </th>
                ))}
                <th className="px-espacio-4 py-espacio-3 text-right text-micro font-bold uppercase tracking-wider text-tinta-suave">
                  {t("documental.bandeja.colAcciones")}
                </th>
              </tr>
            </thead>
            <tbody>
              {listado.map((excepcion) => (
                <tr
                  key={excepcion.id}
                  className="border-b border-borde last:border-b-0 hover:bg-lienzo"
                >
                  <td className="px-espacio-4 py-espacio-3">
                    <p className="text-pequeno font-semibold text-tinta">
                      {motivoHumano(excepcion.codigo_motivo, t)}
                    </p>
                    <p className="text-micro text-tinta-suave">
                      {excepcion.nombre_archivo}
                    </p>
                  </td>
                  <td className="px-espacio-4 py-espacio-3">
                    <EtiquetaSeveridadMotor severidad={excepcion.severidad} />
                  </td>
                  <td className="px-espacio-4 py-espacio-3 text-pequeno text-tinta">
                    {excepcion.prioridad}
                  </td>
                  <td className="px-espacio-4 py-espacio-3 text-pequeno text-tinta">
                    {excepcion.accion_sugerida || "-"}
                  </td>
                  <td className="px-espacio-4 py-espacio-3">
                    <Vencimiento vence={excepcion.vence_en} />
                  </td>
                  <td className="whitespace-nowrap px-espacio-4 py-espacio-3 text-pequeno text-tinta">
                    {comoFecha(excepcion.creado_en, idioma)}
                  </td>
                  <td className="px-espacio-4 py-espacio-3 text-right">
                    <div className="flex justify-end gap-espacio-1">
                      <BotonIcono
                        tamano="sm"
                        aria-label={t("documental.excepciones.abrirDocumento")}
                        onClick={() => alAbrirDocumento(excepcion.documento_id)}
                      >
                        <IconoBuscar />
                      </BotonIcono>
                      {excepcion.estado === "ABIERTA" ? (
                        <Boton
                          tamano="sm"
                          onClick={() => setEnResolucion(excepcion)}
                        >
                          {t("documental.excepciones.resolver")}
                        </Boton>
                      ) : null}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        </div>
      </Tarjeta>

      {enResolucion ? (
        <Panel
          titulo={t("documental.excepciones.resolverTitulo")}
          descripcion={enResolucion.nombre_archivo}
          alCerrar={cerrar}
          pie={
            <div className="flex justify-end gap-espacio-2">
              <Boton variante="secundario" onClick={cerrar}>
                {t("documental.visor.cancelar")}
              </Boton>
              <Boton variante="primario" onClick={() => void confirmar()}>
                {t("documental.excepciones.confirmar")}
              </Boton>
            </div>
          }
        >
          <div className="flex flex-col gap-espacio-4">
            <Selector
              etiqueta={t("documental.excepciones.decision")}
              value={decision}
              onChange={(evento) => setDecision(evento.target.value)}
            >
              {DECISIONES.map((item) => (
                <option key={item} value={item}>
                  {t(`documental.estadoExcepcion.${item}`)}
                </option>
              ))}
            </Selector>
            <AreaTexto
              etiqueta={t("documental.excepciones.resolucion")}
              value={resolucion}
              onChange={(evento) => setResolucion(evento.target.value)}
              rows={3}
            />
          </div>
        </Panel>
      ) : null}
    </div>
  );
}
