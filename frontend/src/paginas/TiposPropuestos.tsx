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
import { formatearFecha } from "./Documentos";
import { useSesion } from "../contextos/ProveedorSesion";
import type { EstadoTipoPropuesto, TipoPropuesto } from "../tipos/api";
import type { Tono } from "../componentes/Interfaz";

const TONO_ESTADO: Record<EstadoTipoPropuesto, Tono> = {
  PENDIENTE: "alerta",
  APROBADO: "exito",
  DESCARTADO: "neutro",
};

const FILTROS: { valor: EstadoTipoPropuesto; texto: string }[] = [
  { valor: "PENDIENTE", texto: "Pendientes" },
  { valor: "APROBADO", texto: "Aprobados" },
  { valor: "DESCARTADO", texto: "Descartados" },
];

export function TiposPropuestos() {
  const { tienePermiso } = useSesion();
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
      setAviso(
        `${propuesto.codigoSugerido} ya es un tipo del catalogo. Los proximos documentos asi se clasifican solos.`,
      );
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
      <Encabezado
        titulo="Tipos propuestos"
        descripcion="Propuestas detectadas por el sistema para ampliar el catálogo documental. Revisá el motivo y los campos sugeridos antes de decidir."
      />
      <Contenido>
        <div className="mb-espacio-5 flex flex-wrap items-center justify-between gap-espacio-3">
          <GrupoSegmentado
            etiqueta="Estado de los tipos propuestos"
            opciones={FILTROS}
            valor={estado}
            alCambiar={setEstado}
          />
          {!consulta.isPending && !consulta.isError ? (
            <p
              role="status"
              aria-atomic="true"
              className="text-pequeno text-tinta-suave"
            >
              {propuestos.length}{" "}
              {propuestos.length === 1 ? "propuesta" : "propuestas"} en este
              estado
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
              titulo="No se pudo completar la acción"
              mensaje={error}
            />
          </div>
        ) : null}

        {consulta.isPending ? (
          <Cargando filas={3} alto="h-64" />
        ) : consulta.isError ? (
          <ErrorPanel
            mensaje={mensajeDeError(consulta.error)}
            reintentar={() => consulta.refetch()}
          />
        ) : propuestos.length === 0 ? (
          <Vacio
            titulo={
              estado === "PENDIENTE"
                ? "No hay propuestas pendientes"
                : estado === "APROBADO"
                  ? "No hay propuestas aprobadas"
                  : "No hay propuestas descartadas"
            }
            detalle="No se encontraron propuestas para el estado seleccionado. Las nuevas propuestas del sistema aparecen en Pendientes."
          />
        ) : (
          <ul
            aria-label="Propuestas de tipos documentales"
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
      </Contenido>
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
            {propuesto.veces === 1 ? "documento" : "documentos"}
          </Pastilla>
          <Pastilla tono={TONO_ESTADO[propuesto.estado]}>
            {propuesto.estado}
          </Pastilla>
        </div>
      </div>

      {propuesto.motivo ? (
        <p className="mt-espacio-4 text-pequeno text-tinta-media [overflow-wrap:anywhere]">
          <span className="font-semibold">Por qué lo propone: </span>
          {propuesto.motivo}
        </p>
      ) : null}

      <div className="mt-espacio-5">
        <h3 className="mb-espacio-3 text-micro font-semibold uppercase tracking-wider text-tinta-suave">
          Campos sugeridos ({propuesto.campos.length})
        </h3>
        {sinCampos ? (
          <p
            role="note"
            className="flex items-start gap-espacio-2 rounded-control border border-alerta-borde bg-alerta-tenue p-espacio-3 text-pequeno text-tinta"
          >
            <span aria-hidden="true" className="mt-px shrink-0">
              <IconoInfo tamano={16} />
            </span>
            <span>
              No hay campos sugeridos. La aprobación requiere al menos un campo
              con clave utilizable; no se puede crear una plantilla sin datos
              para extraer.
            </span>
          </p>
        ) : (
          <ul
            aria-label={"Campos sugeridos de " + propuesto.codigoSugerido}
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
                    {campo.requerido ? "Requerido" : "Opcional"}
                  </span>
                </div>
                {campo.ejemplo != null && campo.ejemplo !== "" ? (
                  <p className="mt-espacio-2 text-pequeno text-tinta-media [overflow-wrap:anywhere]">
                    <span className="font-medium">Ejemplo: </span>
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
          <p>Visto por primera vez {formatearFecha(propuesto.alta)}</p>
          {propuesto.codigoAprobado ? (
            <p className="mt-espacio-1">
              Aprobado como{" "}
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
              aria-label={"Agregar al catálogo: " + propuesto.codigoSugerido}
              className="w-full sm:w-auto"
            >
              Agregar al catálogo
            </Boton>
            <Boton
              type="button"
              disabled={enProceso}
              cargando={descartando}
              onClick={alDescartar}
              aria-label={"Descartar propuesta: " + propuesto.codigoSugerido}
              className="w-full sm:w-auto"
            >
              Descartar
            </Boton>
          </div>
        ) : null}
      </div>
    </Tarjeta>
  );
}
