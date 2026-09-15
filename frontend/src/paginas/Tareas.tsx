import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Contenido, Encabezado } from "../componentes/Disposicion";
import { Cargando, ErrorPanel, Vacio } from "../componentes/Estados";
import { GrupoSegmentado, Selector } from "../componentes/Interfaz";
import { TarjetaTarea } from "../componentes/TarjetaTarea";
import { listarTareas, mensajeDeError } from "../api/procesos";
import { useSesion } from "../contextos/ProveedorSesion";
import { useIdioma } from "../contextos/ProveedorIdioma";

const ESTADOS_TAREA: Record<string, string[]> = {
  PENDIENTE: ["PENDIENTE"],
  VENCIDA: ["VENCIDA"],
  COMPLETADA: ["COMPLETADA"],
  CANCELADA: ["CANCELADA"],
  TODAS: ["PENDIENTE", "VENCIDA", "COMPLETADA", "CANCELADA"],
};

export function Tareas() {
  const { t } = useIdioma();
  return (
    <>
      <Encabezado
        titulo={t("tareas.titulo")}
        descripcion={t("tareas.descripcion")}
      />
      <Contenido>
        <BandejaTareas />
      </Contenido>
    </>
  );
}

function BandejaTareas() {
  const { sesion } = useSesion();
  const { t } = useIdioma();
  const [filtro, setFiltro] = useState<string>("PENDIENTE");
  const [soloMias, setSoloMias] = useState(false);

  const consulta = useQuery({
    queryKey: ["tareas", filtro],
    queryFn: () => listarTareas(ESTADOS_TAREA[filtro]),
    refetchInterval: 15000,
  });

  const tareas = (consulta.data ?? []).filter((tarea) =>
    !soloMias
      ? true
      : (tarea.asignadoA ?? "").toLowerCase() === (sesion?.email ?? "").toLowerCase(),
  );

  return (
    <>
      <div className="mb-espacio-4 flex flex-wrap items-end justify-between gap-espacio-4">
        <div className="flex flex-wrap items-end gap-espacio-4">
          <Selector
            etiqueta={t("operacion.estado")}
            value={filtro}
            onChange={(evento) => setFiltro(evento.target.value)}
          >
            <option value="PENDIENTE">{t("operacion.pendientes")}</option>
            <option value="VENCIDA">{t("operacion.vencidas")}</option>
            <option value="COMPLETADA">{t("operacion.completadas")}</option>
            <option value="CANCELADA">{t("operacion.canceladas")}</option>
            <option value="TODAS">{t("operacion.todas")}</option>
          </Selector>
          <div>
            <p className="mb-espacio-1 text-pequeno text-tinta-suave">{t("operacion.responsable")}</p>
            <GrupoSegmentado
              etiqueta={t("operacion.filtroResponsable")}
              valor={soloMias ? "mias" : "todas"}
              alCambiar={(valor) => setSoloMias(valor === "mias")}
              opciones={[
                { valor: "todas", texto: t("operacion.todas") },
                { valor: "mias", texto: t("operacion.mias") },
              ]}
            />
          </div>
        </div>
        <p className="text-pequeno text-tinta-suave">
          {t("operacion.tareasAyuda")}
        </p>
      </div>

      {consulta.isPending ? (
        <Cargando filas={3} alto="h-24" />
      ) : consulta.isError ? (
        <ErrorPanel
          mensaje={mensajeDeError(consulta.error)}
          error={consulta.error}
          reintentar={() => consulta.refetch()}
        />
      ) : tareas.length === 0 ? (
        <Vacio
          titulo={
            filtro === "PENDIENTE" || filtro === "VENCIDA"
              ? t("operacion.sinTareasResolver")
              : t("operacion.sinTareasMostrar")
          }
          detalle={t("operacion.sinTareasDetalle")}
        />
      ) : (
        <ul aria-label={t("operacion.listaTareas")} className="space-y-espacio-4">
          {tareas.map((tarea) => (
            <li key={tarea.id}>
              <TarjetaTarea tarea={tarea} conInstancia />
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
