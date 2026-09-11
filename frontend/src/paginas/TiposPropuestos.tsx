import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Contenido, Encabezado } from "../componentes/Disposicion";
import { Cargando, ErrorPanel, Vacio } from "../componentes/Estados";
import { Boton, CabeceraTarjeta, GrupoSegmentado, Pastilla, Tarjeta } from "../componentes/Interfaz";
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
  const refrescar = () => clienteConsultas.invalidateQueries({ queryKey: ["tipos-propuestos"] });

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
        titulo="Tipos que encontro el sistema"
        descripcion="Documentos que no encajaron en ningun tipo conocido. Se capturaron igual, y aca decidis si vale la pena modelarlos."
        acciones={
          <GrupoSegmentado etiqueta="Estado de los tipos propuestos" opciones={FILTROS} valor={estado} alCambiar={setEstado} />
        }
      />
      <Contenido>
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

        {consulta.isPending ? (
          <Cargando filas={3} alto="h-32" />
        ) : consulta.isError ? (
          <ErrorPanel
            mensaje={mensajeDeError(consulta.error)}
            reintentar={() => consulta.refetch()}
          />
        ) : propuestos.length === 0 ? (
          <Vacio
            titulo={
              estado === "PENDIENTE"
                ? "No hay tipos nuevos esperando"
                : "No hay nada en este estado"
            }
            detalle="Cuando llegue un documento que no encaje en el catalogo, el sistema lo captura igual y propone aca el tipo que le falta."
          />
        ) : (
          <div className="space-y-4">
            {propuestos.map((propuesto) => (
              <TarjetaPropuesta
                key={propuesto.id}
                propuesto={propuesto}
                puedeAdministrar={puedeAdministrar}
                enProceso={aprobar.isPending || descartar.isPending}
                alAprobar={() => aprobar.mutate(propuesto.id)}
                alDescartar={() => descartar.mutate(propuesto.id)}
              />
            ))}
          </div>
        )}
      </Contenido>
    </>
  );
}

function TarjetaPropuesta({
  propuesto,
  puedeAdministrar,
  enProceso,
  alAprobar,
  alDescartar,
}: {
  propuesto: TipoPropuesto;
  puedeAdministrar: boolean;
  enProceso: boolean;
  alAprobar: () => void;
  alDescartar: () => void;
}) {
  const sinCampos = propuesto.campos.length === 0;

  return (
    <Tarjeta>
      <CabeceraTarjeta
        titulo={propuesto.nombreSugerido || propuesto.codigoSugerido}
        descripcion={propuesto.codigoSugerido}
        acciones={
          <>
            <Pastilla tono="informacion">
              {propuesto.veces} {propuesto.veces === 1 ? "documento" : "documentos"}
            </Pastilla>
            <Pastilla tono={TONO_ESTADO[propuesto.estado]}>{propuesto.estado}</Pastilla>
          </>
        }
      />

      {propuesto.motivo ? (
        <p className="mt-3 text-xs leading-relaxed text-tinta-suave">
          <span className="font-semibold text-tinta-media">Por que lo propone: </span>
          {propuesto.motivo}
        </p>
      ) : null}

      <div className="mt-4">
        <h4 className="mb-2 text-[10px] font-semibold uppercase tracking-wider text-tinta-suave">
          Campos que se extraerian ({propuesto.campos.length})
        </h4>
        {sinCampos ? (
          <p className="flex items-start gap-1.5 rounded-xl border border-ambar-borde bg-ambar-tenue px-3.5 py-2.5 text-xs leading-relaxed text-tinta">
            <IconoInfo tamano={13} className="mt-0.5 shrink-0" />
            El clasificador no pudo proponer campos para este tipo, asi que todavia no se puede
            aprobar: quedaria una plantilla que no extrae nada. Va a poder cuando llegue un documento
            mas claro.
          </p>
        ) : (
          <ul className="flex flex-wrap gap-1.5">
            {propuesto.campos.map((campo) => (
              <li
                key={campo.clave}
                className="inline-flex items-center gap-1.5 rounded-full bg-lienzo px-2.5 py-1 text-[11px] text-tinta-media ring-1 ring-inset ring-borde"
              >
                <span className="font-medium">{campo.etiqueta || campo.clave}</span>
                {campo.tipoDato ? (
                  <span className="text-tinta-tenue">{campo.tipoDato.toLowerCase()}</span>
                ) : null}
              </li>
            ))}
          </ul>
        )}
      </div>

      <p className="mt-3 text-[11px] text-tinta-tenue">
        Visto por primera vez {formatearFecha(propuesto.alta)}
        {propuesto.codigoAprobado ? ` · aprobado como ${propuesto.codigoAprobado}` : ""}
      </p>

      {puedeAdministrar && propuesto.estado === "PENDIENTE" ? (
        <div className="mt-4 flex flex-wrap gap-2 border-t border-borde pt-4">
          <Boton variante="primario" disabled={enProceso || sinCampos} onClick={alAprobar}>
            Agregarlo al catalogo
          </Boton>
          <Boton disabled={enProceso} onClick={alDescartar}>
            No nos sirve
          </Boton>
        </div>
      ) : null}
    </Tarjeta>
  );
}
