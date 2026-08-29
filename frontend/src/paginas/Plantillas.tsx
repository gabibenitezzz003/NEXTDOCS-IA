import { useQuery } from "@tanstack/react-query";
import { Contenido, Encabezado } from "../componentes/Disposicion";
import { CargandoTarjetas, ErrorPanel, Vacio } from "../componentes/Estados";
import { Pastilla } from "../componentes/Interfaz";
import { IconoPlantillas } from "../componentes/Iconos";
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

      <Contenido>
        {consulta.isPending ? (
          <CargandoTarjetas cantidad={6} />
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
              <article
                key={plantilla.id}
                className="group relative overflow-hidden rounded-3xl border border-borde bg-white p-5 relieve elevar hover:border-violeta-borde"
              >
                <span className="absolute inset-x-0 top-0 h-0.5 scale-x-0 bg-gradient-to-r from-violeta to-rojo transition-transform duration-300 group-hover:scale-x-100" />

                <div className="flex items-start justify-between gap-3">
                  <div className="flex min-w-0 items-start gap-3">
                    <span className="mt-0.5 flex size-9 shrink-0 items-center justify-center rounded-xl bg-lienzo text-tinta-suave ring-1 ring-borde transition group-hover:bg-violeta-tenue group-hover:text-violeta group-hover:ring-violeta-borde">
                      <IconoPlantillas tamano={17} />
                    </span>
                    <div className="min-w-0">
                      <h2 className="truncate font-titulo text-[15px] text-tinta">{plantilla.nombre}</h2>
                      <p className="mt-0.5 truncate font-mono text-[11px] text-tinta-tenue">
                        {plantilla.codigo}
                      </p>
                    </div>
                  </div>
                  {plantilla.numeroVersionPublicada ? (
                    <Pastilla tono="exito">v{plantilla.numeroVersionPublicada}</Pastilla>
                  ) : (
                    <Pastilla tono="alerta">sin publicar</Pastilla>
                  )}
                </div>

                {plantilla.descripcion ? (
                  <p className="mt-3 line-clamp-2 text-sm leading-relaxed text-tinta-suave">
                    {plantilla.descripcion}
                  </p>
                ) : null}

                <div className="mt-4 flex flex-wrap items-center gap-2 border-t border-borde pt-3 text-xs text-tinta-suave">
                  {plantilla.familia ? (
                    <span className="rounded-md bg-lienzo px-2 py-0.5 font-medium ring-1 ring-inset ring-borde">
                      {plantilla.familia}
                    </span>
                  ) : null}
                  {plantilla.cantidadVersiones ? (
                    <span className="tabular-nums">{plantilla.cantidadVersiones} versiones</span>
                  ) : null}
                </div>
              </article>
            ))}
          </div>
        )}
      </Contenido>
    </>
  );
}
