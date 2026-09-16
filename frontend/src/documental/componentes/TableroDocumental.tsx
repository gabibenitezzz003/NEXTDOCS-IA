import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  obtenerEventos,
  obtenerExcepcionesMotor,
  obtenerResumenDocumental,
  type EstadoDocumentoMotor,
} from "../../api/documental";
import { mensajeDeError } from "../../api/cliente";
import { Tarjeta, CabeceraTarjeta } from "../../componentes/Interfaz";
import { Cargando, ErrorPanel } from "../../componentes/Estados";
import { useIdioma } from "../../contextos/ProveedorIdioma";
import { ESTADOS_DOCUMENTO, comoFecha, motivoHumano } from "../dominio";

const TONOS_BARRA: Record<EstadoDocumentoMotor, string> = {
  RECIBIDO: "bg-tinta-tenue",
  PROCESANDO: "bg-informacion",
  EXTRAIDO: "bg-violeta",
  VALIDADO: "bg-exito",
  OBSERVADO: "bg-alerta",
  APROBADO: "bg-exito",
  RECHAZADO: "bg-rojo-alto",
  CERRADO: "bg-grafito",
};

function Embudo({ porEstado }: { porEstado: Record<string, number> }) {
  const { t } = useIdioma();
  const maximo = Math.max(
    1,
    ...ESTADOS_DOCUMENTO.map((estado) => Number(porEstado[estado] || 0)),
  );

  return (
    <div className="flex flex-col gap-espacio-2">
      {ESTADOS_DOCUMENTO.map((estado) => {
        const cantidad = Number(porEstado[estado] || 0);
        return (
          <div key={estado} className="flex items-center gap-espacio-3">
            <span className="w-24 text-micro font-semibold uppercase text-tinta-suave">
              {t(`documental.estado.${estado}`)}
            </span>
            <div className="h-3.5 flex-1 overflow-hidden rounded-insignia bg-lienzo">
              <div
                className={`h-full rounded-insignia transition-[width] duration-200 motion-reduce:transition-none ${TONOS_BARRA[estado]}`}
                style={{ width: `${(cantidad / maximo) * 100}%` }}
              />
            </div>
            <span className="w-8 text-right text-pequeno font-semibold text-tinta">
              {cantidad}
            </span>
          </div>
        );
      })}
    </div>
  );
}

function TopMotivos() {
  const { t } = useIdioma();
  const excepciones = useQuery({
    queryKey: ["documental-excepciones", { estado: "ABIERTA", limite: 200 }],
    queryFn: () => obtenerExcepcionesMotor({ estado: "ABIERTA", limite: 200 }),
    refetchInterval: 15000,
  });

  const conteo = useMemo(() => {
    const acumulado = new Map<string, number>();
    (excepciones.data || []).forEach((excepcion) => {
      acumulado.set(
        excepcion.codigo_motivo,
        (acumulado.get(excepcion.codigo_motivo) || 0) + 1,
      );
    });
    return [...acumulado.entries()].sort((a, b) => b[1] - a[1]).slice(0, 6);
  }, [excepciones.data]);

  if (!conteo.length) {
    return (
      <p className="text-pequeno text-tinta-suave">
        {t("documental.tablero.sinExcepciones")}
      </p>
    );
  }

  const maximo = conteo[0][1];

  return (
    <div className="flex flex-col gap-espacio-2">
      {conteo.map(([motivo, cantidad]) => (
        <div key={motivo} className="flex items-center gap-espacio-3">
          <span className="w-40 truncate text-micro font-semibold uppercase text-tinta-suave">
            {motivoHumano(motivo, t)}
          </span>
          <div className="h-3.5 w-32 overflow-hidden rounded-insignia bg-lienzo">
            <div
              className="h-full rounded-insignia bg-alerta transition-[width] duration-200 motion-reduce:transition-none"
              style={{ width: `${(cantidad / maximo) * 100}%` }}
            />
          </div>
          <span className="text-pequeno font-semibold text-tinta">
            {cantidad}
          </span>
        </div>
      ))}
    </div>
  );
}

export function TableroDocumental() {
  const { t, idioma } = useIdioma();

  const resumen = useQuery({
    queryKey: ["documental-resumen"],
    queryFn: obtenerResumenDocumental,
    refetchInterval: 15000,
  });

  const eventos = useQuery({
    queryKey: ["documental-eventos", "PENDIENTE"],
    queryFn: () => obtenerEventos({ estado: "PENDIENTE", limite: 50 }),
    refetchInterval: 15000,
  });

  if (resumen.isLoading) return <Cargando />;
  if (resumen.isError) {
    return (
      <ErrorPanel
        error={resumen.error}
        mensaje={mensajeDeError(resumen.error)}
        reintentar={() => resumen.refetch()}
      />
    );
  }

  const porEstado = resumen.data || {};
  const pendientes = (eventos.data || []).slice(0, 10);

  return (
    <div
      className="grid grid-cols-1 gap-espacio-4 overflow-auto lg:grid-cols-2"
      data-testid="documental-tablero"
    >
      <Tarjeta>
        <CabeceraTarjeta titulo={t("documental.tablero.embudo")} />
        <div className="mt-espacio-4">
          <Embudo porEstado={porEstado} />
        </div>
      </Tarjeta>

      <Tarjeta>
        <CabeceraTarjeta titulo={t("documental.tablero.topMotivos")} />
        <div className="mt-espacio-4">
          <TopMotivos />
        </div>
      </Tarjeta>

      <Tarjeta className="lg:col-span-2">
        <CabeceraTarjeta titulo={t("documental.tablero.eventosPendientes")} />
        <div className="mt-espacio-4 flex flex-col">
          {pendientes.length ? (
            pendientes.map((evento) => (
              <div
                key={evento.id}
                className="flex items-center gap-espacio-3 border-b border-borde py-espacio-2 last:border-b-0"
              >
                <span className="min-w-32 text-micro text-tinta-suave">
                  {comoFecha(evento.creado_en, idioma)}
                </span>
                <span className="flex-1 text-pequeno font-semibold text-tinta">
                  {evento.tipo_evento}
                </span>
                <span className="text-micro text-tinta-suave">
                  {t("documental.tablero.intentos")} {evento.intentos ?? 0}
                </span>
              </div>
            ))
          ) : (
            <p className="text-pequeno text-tinta-suave">
              {t("documental.tablero.sinPendientes")}
            </p>
          )}
        </div>
      </Tarjeta>
    </div>
  );
}
