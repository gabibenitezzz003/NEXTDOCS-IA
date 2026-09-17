import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  obtenerVencimientos,
  type VencimientoFila,
} from "../../api/documental";
import { mensajeDeError } from "../../api/cliente";
import {
  BotonIcono,
  Metrica,
  Selector,
  Tarjeta,
} from "../../componentes/Interfaz";
import { Cargando, ErrorPanel, Vacio } from "../../componentes/Estados";
import { IconoBuscar } from "../../componentes/Iconos";
import { useIdioma } from "../../contextos/ProveedorIdioma";
import { FAMILIAS, comoFecha } from "../dominio";

function Cuenta({ dias }: { dias: number | null }) {
  const { t } = useIdioma();

  if (dias === null || dias === undefined || Number.isNaN(dias)) {
    return <span className="text-micro text-tinta-suave">-</span>;
  }

  if (dias < 0) {
    return (
      <span className="text-pequeno font-semibold text-rojo-alto">
        {t("documental.vencimientos.haceDias", { valor: Math.abs(dias) })}
      </span>
    );
  }

  if (dias === 0) {
    return (
      <span className="text-pequeno font-semibold text-alerta-texto">
        {t("documental.vencimientos.hoy")}
      </span>
    );
  }

  return (
    <span
      className={`text-pequeno ${dias <= 30 ? "font-bold text-tinta" : "font-medium text-tinta-media"}`}
    >
      {t("documental.vencimientos.enDias", { valor: dias })}
    </span>
  );
}

const TONOS_SITUACION: Record<string, string> = {
  VENCIDO: "bg-rojo-tenue text-rojo-alto ring-1 ring-inset ring-rojo-borde",
  POR_VENCER:
    "bg-alerta-tenue text-alerta-texto ring-1 ring-inset ring-alerta-borde",
  VIGENTE: "bg-exito-tenue text-exito-texto ring-1 ring-inset ring-exito-borde",
};

export function PanelVencimientos({
  alAbrirDocumento,
}: {
  alAbrirDocumento: (documentoId: string) => void;
}) {
  const { t, idioma } = useIdioma();

  const [familia, setFamilia] = useState("");
  const [dias, setDias] = useState(30);
  const [situacion, setSituacion] = useState("");

  const filtros = useMemo(
    () => ({ familia, dias, situacion, limite: 200 }),
    [familia, dias, situacion],
  );

  const consulta = useQuery({
    queryKey: ["documental-vencimientos", filtros],
    queryFn: () => obtenerVencimientos(filtros),
    staleTime: 15000,
    refetchInterval: 30000,
    placeholderData: (previo) => previo,
  });

  const resumen = consulta.data?.resumen || {
    VENCIDO: 0,
    POR_VENCER: 0,
    VIGENTE: 0,
  };
  const documentos = consulta.data?.documentos || [];

  const alternar = (valor: string) =>
    setSituacion((previo) => (previo === valor ? "" : valor));

  return (
    <div
      className="flex h-full flex-col gap-espacio-4"
      data-testid="documental-vencimientos"
    >
      <div className="grid grid-cols-1 gap-espacio-3 sm:grid-cols-3">
        {(
          [
            { clave: "VENCIDO", valor: resumen.VENCIDO },
            { clave: "POR_VENCER", valor: resumen.POR_VENCER },
            { clave: "VIGENTE", valor: resumen.VIGENTE },
          ] as const
        ).map((item) => (
          <button
            key={item.clave}
            type="button"
            onClick={() => alternar(item.clave)}
            aria-pressed={situacion === item.clave}
            className={`rounded-tarjeta border p-espacio-4 text-left transition-colors focus-visible:outline-foco ${
              situacion === item.clave
                ? "border-accion-primaria bg-violeta-tenue"
                : "border-borde bg-superficie hover:border-borde-fuerte"
            }`}
          >
            <Metrica
              etiqueta={
                item.clave === "POR_VENCER"
                  ? t("documental.vencimientos.porVencerEn", { valor: dias })
                  : t(`documental.vencimientos.${item.clave}`)
              }
              valor={item.valor}
              destacada={situacion === item.clave}
            />
          </button>
        ))}
      </div>

      <Tarjeta className="flex min-h-0 flex-1 flex-col" padding="p-0">
        <div className="flex flex-wrap items-end gap-espacio-3 border-b border-borde px-espacio-4 py-espacio-3">
          <Selector
            etiqueta={t("documental.vencimientos.colFamilia")}
            value={familia}
            onChange={(evento) => setFamilia(evento.target.value)}
            className="w-44"
          >
            <option value="">{t("documental.bandeja.todos")}</option>
            {FAMILIAS.map((item) => (
              <option key={item} value={item}>
                {t(`documental.familia.${item}`)}
              </option>
            ))}
          </Selector>

          <Selector
            etiqueta={t("documental.vencimientos.horizonte")}
            value={String(dias)}
            onChange={(evento) => setDias(Number(evento.target.value))}
            className="w-36"
          >
            {[15, 30, 60, 90].map((item) => (
              <option key={item} value={item}>
                {t("documental.vencimientos.enDias", { valor: item })}
              </option>
            ))}
          </Selector>
        </div>

        <div className="min-h-0 flex-1 overflow-auto">
        {consulta.isError ? (
          <div className="p-espacio-4">
            <ErrorPanel
              error={consulta.error}
              mensaje={mensajeDeError(consulta.error)}
              reintentar={() => consulta.refetch()}
            />
          </div>
        ) : consulta.isLoading ? (
          <Cargando />
        ) : !documentos.length ? (
          <Vacio
            titulo={t("documental.vencimientos.vacio")}
            detalle={t("documental.vencimientos.vacioDetalle")}
          />
        ) : (
          <table className="w-full border-collapse">
            <thead className="sticky top-0 z-10">
              <tr className="border-b border-borde bg-lienzo">
                {[
                  "colDocumento",
                  "colSituacion",
                  "colVence",
                  "colCuenta",
                  "colAsociado",
                ].map((clave) => (
                  <th
                    key={clave}
                    className="px-espacio-4 py-espacio-3 text-left text-micro font-bold uppercase tracking-wider text-tinta-suave"
                  >
                    {t(`documental.vencimientos.${clave}`)}
                  </th>
                ))}
                <th className="px-espacio-4 py-espacio-3" />
              </tr>
            </thead>
            <tbody>
              {documentos.map((fila: VencimientoFila) => (
                <tr
                  key={fila.id}
                  className="cursor-pointer border-b border-borde last:border-b-0 hover:bg-lienzo"
                  onClick={() => alAbrirDocumento(fila.id)}
                >
                  <td className="px-espacio-4 py-espacio-3">
                    <p className="truncate text-pequeno font-semibold text-tinta">
                      {fila.nombre_archivo}
                    </p>
                    <p className="text-micro text-tinta-suave">
                      {fila.plantilla_codigo || "—"}
                    </p>
                  </td>
                  <td className="px-espacio-4 py-espacio-3">
                    <span
                      className={`inline-flex whitespace-nowrap rounded-insignia px-espacio-3 py-espacio-1 text-micro font-bold uppercase ${TONOS_SITUACION[fila.situacion] ?? TONOS_SITUACION.VIGENTE}`}
                    >
                      {t(`documental.vencimientos.${fila.situacion}`)}
                    </span>
                  </td>
                  <td className="whitespace-nowrap px-espacio-4 py-espacio-3 text-pequeno text-tinta">
                    {comoFecha(fila.vence_en, idioma).split(",")[0]}
                  </td>
                  <td className="px-espacio-4 py-espacio-3">
                    <Cuenta
                      dias={
                        fila.dias_restantes === null
                          ? null
                          : Number(fila.dias_restantes)
                      }
                    />
                  </td>
                  <td className="px-espacio-4 py-espacio-3">
                    {fila.sujeto_id ? (
                      <div className="min-w-0">
                        <p className="truncate text-pequeno font-semibold text-tinta">
                          {fila.sujeto_tipo}
                        </p>
                        <p className="truncate text-micro text-tinta-suave">
                          {fila.sujeto_id}
                        </p>
                      </div>
                    ) : (
                      <span className="text-micro text-tinta-suave">
                        {t("documental.bandeja.sinAsociar")}
                      </span>
                    )}
                  </td>
                  <td className="px-espacio-4 py-espacio-3 text-right">
                    <BotonIcono
                      tamano="sm"
                      aria-label={t("documental.bandeja.abrir")}
                      onClick={(evento) => {
                        evento.stopPropagation();
                        alAbrirDocumento(fila.id);
                      }}
                    >
                      <IconoBuscar />
                    </BotonIcono>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        </div>
      </Tarjeta>
    </div>
  );
}
