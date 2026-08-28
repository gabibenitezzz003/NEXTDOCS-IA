import { useQuery } from "@tanstack/react-query";
import { Encabezado } from "../componentes/Disposicion";
import { Cargando, ErrorPanel, Vacio } from "../componentes/Estados";
import { listarPlantillas } from "../api/plantillas";
import { mensajeDeError } from "../api/cliente";

export function Plantillas() {
  const consulta = useQuery({ queryKey: ["plantillas"], queryFn: listarPlantillas });

  return (
    <>
      <Encabezado
        titulo="Plantillas documentales"
        descripcion="Cada plantilla define los campos que se extraen y las reglas que deciden."
      />
      <div className="px-8 py-6">
        {consulta.isPending ? (
          <Cargando filas={4} />
        ) : consulta.isError ? (
          <ErrorPanel mensaje={mensajeDeError(consulta.error)} reintentar={() => consulta.refetch()} />
        ) : !consulta.data?.length ? (
          <Vacio
            titulo="Todavia no hay plantillas"
            detalle="Sin una plantilla publicada los documentos se ingresan pero no se extraen campos."
          />
        ) : (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {consulta.data.map((plantilla) => (
              <article key={plantilla.id} className="rounded-xl border border-borde bg-white p-5">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <h2 className="truncate font-titulo text-base text-tinta">{plantilla.nombre}</h2>
                    <p className="font-mono text-xs text-tinta-suave">{plantilla.codigo}</p>
                  </div>
                  {plantilla.numeroVersionPublicada ? (
                    <span className="shrink-0 rounded-full bg-exito-tenue px-2.5 py-0.5 text-xs font-medium text-exito ring-1 ring-exito/25">
                      v{plantilla.numeroVersionPublicada}
                    </span>
                  ) : (
                    <span className="shrink-0 rounded-full bg-alerta-tenue px-2.5 py-0.5 text-xs font-medium text-alerta ring-1 ring-alerta/25">
                      sin publicar
                    </span>
                  )}
                </div>
                {plantilla.descripcion ? (
                  <p className="mt-2 line-clamp-2 text-sm text-tinta-suave">{plantilla.descripcion}</p>
                ) : null}
                <div className="mt-3 flex items-center gap-3 text-xs text-tinta-suave">
                  {plantilla.familia ? <span>{plantilla.familia}</span> : null}
                  {plantilla.cantidadVersiones ? (
                    <span>{plantilla.cantidadVersiones} versiones</span>
                  ) : null}
                </div>
              </article>
            ))}
          </div>
        )}
      </div>
    </>
  );
}
