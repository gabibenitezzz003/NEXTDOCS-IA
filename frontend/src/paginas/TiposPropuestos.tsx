import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Contenido, Encabezado } from "../componentes/Disposicion";
import { Cargando, ErrorPanel, Vacio } from "../componentes/Estados";
import {
  Boton,
  GrupoSegmentado,
  Pastilla,
  Tarjeta,
} from "../componentes/Interfaz";
import { IconoCheck, IconoInfo } from "../componentes/Iconos";
import {
  aprobarTipoPropuesto,
  descartarTipoPropuesto,
  listarTiposPropuestos,
} from "../api/tiposPropuestos";
import { mensajeDeError } from "../api/cliente";
import { formatearFecha } from "../utilidades/fechas";
import { useIdioma } from "../contextos/ProveedorIdioma";
import { useSesion } from "../contextos/ProveedorSesion";
import type { EstadoTipoPropuesto, TipoPropuesto } from "../tipos/api";
import type { Tono } from "../componentes/Interfaz";

const TONO_ESTADO: Record<EstadoTipoPropuesto, Tono> = {
  PENDIENTE: "alerta",
  APROBADO: "exito",
  DESCARTADO: "neutro",
};

const CLAVE_ESTADO: Record<EstadoTipoPropuesto, string> = {
  PENDIENTE: "tiposPropuestos.estadoPendiente",
  APROBADO: "tiposPropuestos.estadoAprobado",
  DESCARTADO: "tiposPropuestos.estadoDescartado",
};

const FILTROS: { valor: EstadoTipoPropuesto; texto: string }[] = [
  { valor: "PENDIENTE", texto: "tiposPropuestos.pendientes" },
  { valor: "APROBADO", texto: "tiposPropuestos.aprobados" },
  { valor: "DESCARTADO", texto: "tiposPropuestos.descartados" },
];

export function TiposPropuestos() {
  const { t } = useIdioma();
  return (
    <>
      <Encabezado
        titulo={t("tiposPropuestos.titulo")}
        descripcion={t("tiposPropuestos.descripcion")}
      />
      <Contenido>
        <ContenidoTiposPropuestos />
      </Contenido>
    </>
  );
}

export function ContenidoTiposPropuestos() {
  const { tienePermiso } = useSesion();
  const { t } = useIdioma();
  const clienteConsultas = useQueryClient();
  const [estado, setEstado] = useState<EstadoTipoPropuesto>("PENDIENTE");
  const [aviso, setAviso] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const puedeAdministrar = tienePermiso("tenant.administrar");

  const consulta = useQuery({
    queryKey: ["tipos-propuestos", estado],
    queryFn: () => listarTiposPropuestos(estado),
  });
  const refrescar = () =>
    clienteConsultas.invalidateQueries({ queryKey: ["tipos-propuestos"] });

  const aprobar = useMutation({
    mutationFn: aprobarTipoPropuesto,
    onSuccess: (propuesto) => {
      setError(null);
      setAviso(t("tiposPropuestos.aprobadoAviso", { codigo: propuesto.codigoSugerido }));
      refrescar();
    },
    onError: (fallo) => {
      setAviso(null);
      setError(mensajeDeError(fallo));
    },
  });

  const descartar = useMutation({
    mutationFn: descartarTipoPropuesto,
    onSuccess: () => {
      setError(null);
      setAviso(null);
      refrescar();
    },
    onError: (fallo) => setError(mensajeDeError(fallo)),
  });

  const propuestos = consulta.data ?? [];

  return (
    <>
        <div className="mb-espacio-5 flex flex-wrap items-center justify-between gap-espacio-3">
          <GrupoSegmentado
            etiqueta={t("tiposPropuestos.grupoEstado")}
            opciones={FILTROS.map((filtro) => ({ ...filtro, texto: t(filtro.texto) }))}
            valor={estado}
            alCambiar={setEstado}
          />
          {!consulta.isPending && !consulta.isError ? (
            <p
              role="status"
              aria-atomic="true"
              className="text-pequeno text-tinta-suave"
            >
              {t("tiposPropuestos.conteo", {
                cantidad: propuestos.length,
                etiqueta:
                  propuestos.length === 1
                    ? t("tiposPropuestos.unaPropuesta")
                    : t("tiposPropuestos.muchasPropuestas"),
              })}
            </p>
          ) : null}
        </div>
        {aviso ? (
          <div
            role="status"
            aria-atomic="true"
            className="mb-espacio-4 flex items-start gap-espacio-2 rounded-panel border border-exito-borde bg-exito-tenue p-espacio-4 text-pequeno text-exito-texto"
          >
            <span aria-hidden="true" className="mt-px shrink-0">
              <IconoCheck tamano={16} />
            </span>
            <p className="min-w-0 [overflow-wrap:anywhere]">{aviso}</p>
          </div>
        ) : null}
        {error ? (
          <div className="mb-espacio-4">
            <ErrorPanel
              titulo={t("tiposPropuestos.errorAccion")}
              mensaje={error}
            />
          </div>
        ) : null}

        {consulta.isPending ? (
          <Cargando filas={3} alto="h-64" />
        ) : consulta.isError ? (
          <ErrorPanel
            mensaje={mensajeDeError(consulta.error)}
          error={consulta.error}
            reintentar={() => consulta.refetch()}
          />
        ) : propuestos.length === 0 ? (
          <Vacio
            titulo={
              estado === "PENDIENTE"
                ? t("tiposPropuestos.sinPendientes")
                : estado === "APROBADO"
                  ? t("tiposPropuestos.sinAprobadas")
                  : t("tiposPropuestos.sinDescartadas")
            }
            detalle={t("tiposPropuestos.vacioDetalle")}
          />
        ) : (
          <ul
            aria-label={t("tiposPropuestos.lista")}
            className="space-y-espacio-4"
          >
            {propuestos.map((propuesto) => (
              <li key={propuesto.id} className="min-w-0">
                <TarjetaPropuesta
                  propuesto={propuesto}
                  puedeAdministrar={puedeAdministrar}
                  enProceso={aprobar.isPending || descartar.isPending}
                  aprobando={
                    aprobar.isPending && aprobar.variables === propuesto.id
                  }
                  descartando={
                    descartar.isPending && descartar.variables === propuesto.id
                  }
                  alAprobar={() => aprobar.mutate(propuesto.id)}
                  alDescartar={() => descartar.mutate(propuesto.id)}
                />
              </li>
            ))}
          </ul>
        )}
    </>
  );
}

function TarjetaPropuesta({
  propuesto,
  puedeAdministrar,
  enProceso,
  aprobando,
  descartando,
  alAprobar,
  alDescartar,
}: {
  propuesto: TipoPropuesto;
  puedeAdministrar: boolean;
  enProceso: boolean;
  aprobando: boolean;
  descartando: boolean;
  alAprobar: () => void;
  alDescartar: () => void;
}) {
  const { t } = useIdioma();
  const sinCampos = propuesto.campos.length === 0;

  return (
    <Tarjeta>
      <div className="flex min-w-0 flex-wrap items-start justify-between gap-espacio-3">
        <div className="min-w-0 flex-1 basis-64">
          <h2 className="font-titulo text-titulo-panel [overflow-wrap:anywhere]">
            {propuesto.nombreSugerido || propuesto.codigoSugerido}
          </h2>
          <p className="mt-espacio-1 text-pequeno text-tinta-suave [overflow-wrap:anywhere]">
            {propuesto.codigoSugerido}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-espacio-2">
          <Pastilla tono="informacion">
            {propuesto.veces}{" "}
            {propuesto.veces === 1
              ? t("tiposPropuestos.unDocumento")
              : t("tiposPropuestos.muchosDocumentos")}
          </Pastilla>
          <Pastilla tono={TONO_ESTADO[propuesto.estado]}>
            {t(CLAVE_ESTADO[propuesto.estado])}
          </Pastilla>
        </div>
      </div>

      {propuesto.motivo ? (
        <p className="mt-espacio-4 text-pequeno text-tinta-media [overflow-wrap:anywhere]">
          <span className="font-semibold">{t("tiposPropuestos.porQueLoPropone")}</span>
          {propuesto.motivo}
        </p>
      ) : null}

      <div className="mt-espacio-5">
        <h3 className="mb-espacio-3 text-micro font-semibold uppercase tracking-wider text-tinta-suave">
          {t("tiposPropuestos.camposSugeridos", { cantidad: propuesto.campos.length })}
        </h3>
        {sinCampos ? (
          <p
            role="note"
            className="flex items-start gap-espacio-2 rounded-control border border-alerta-borde bg-alerta-tenue p-espacio-3 text-pequeno text-tinta"
          >
            <span aria-hidden="true" className="mt-px shrink-0">
              <IconoInfo tamano={16} />
            </span>
            <span>{t("tiposPropuestos.sinCampos")}</span>
          </p>
        ) : (
          <ul
            aria-label={t("tiposPropuestos.camposSugeridosDe", { codigo: propuesto.codigoSugerido })}
            className="grid min-w-0 gap-espacio-3 sm:grid-cols-2 xl:grid-cols-3"
          >
            {propuesto.campos.map((campo) => (
              <li
                key={campo.clave}
                className="min-w-0 rounded-control border border-borde bg-lienzo p-espacio-3 text-pequeno"
              >
                <p className="font-semibold [overflow-wrap:anywhere]">
                  {campo.etiqueta || campo.clave}
                </p>
                {campo.etiqueta && campo.etiqueta !== campo.clave ? (
                  <p className="mt-espacio-1 text-micro text-tinta-suave [overflow-wrap:anywhere]">
                    {campo.clave}
                  </p>
                ) : null}
                <div className="mt-espacio-2 flex flex-wrap items-center gap-espacio-2">
                  {campo.tipoDato ? (
                    <Pastilla>{campo.tipoDato.toLowerCase()}</Pastilla>
                  ) : null}
                  <span className="text-micro text-tinta-suave">
                    {campo.requerido
                      ? t("tiposPropuestos.requerido")
                      : t("tiposPropuestos.opcional")}
                  </span>
                </div>
                {campo.ejemplo != null && campo.ejemplo !== "" ? (
                  <p className="mt-espacio-2 text-pequeno text-tinta-media [overflow-wrap:anywhere]">
                    <span className="font-medium">{t("tiposPropuestos.ejemplo")}</span>
                    {campo.ejemplo}
                  </p>
                ) : null}
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="mt-espacio-5 flex flex-wrap items-center justify-between gap-espacio-4 border-t border-borde pt-espacio-4">
        <div className="min-w-0 text-pequeno text-tinta-suave [overflow-wrap:anywhere]">
          <p>{t("tiposPropuestos.vistoPrimeraVez", { fecha: formatearFecha(propuesto.alta) })}</p>
          {propuesto.codigoAprobado ? (
            <p className="mt-espacio-1">
              {t("tiposPropuestos.aprobadoComo")}
              <span className="font-semibold text-tinta">
                {propuesto.codigoAprobado}
              </span>
            </p>
          ) : null}
        </div>
        {puedeAdministrar && propuesto.estado === "PENDIENTE" ? (
          <div className="flex w-full flex-wrap gap-espacio-2 sm:w-auto">
            <Boton
              type="button"
              variante="primario"
              disabled={enProceso || sinCampos}
              cargando={aprobando}
              onClick={alAprobar}
              aria-label={t("tiposPropuestos.agregarCatalogoAria", { codigo: propuesto.codigoSugerido })}
              className="w-full sm:w-auto"
            >
              {t("tiposPropuestos.agregarCatalogo")}
            </Boton>
            <Boton
              type="button"
              disabled={enProceso}
              cargando={descartando}
              onClick={alDescartar}
              aria-label={t("tiposPropuestos.descartarAria", { codigo: propuesto.codigoSugerido })}
              className="w-full sm:w-auto"
            >
              {t("tiposPropuestos.descartar")}
            </Boton>
          </div>
        ) : null}
      </div>
    </Tarjeta>
  );
}
