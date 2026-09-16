import { Suspense, lazy, useCallback, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useParams, useNavigate } from "react-router-dom";
import { obtenerEstadoDocumental } from "../api/documental";
import { useIdioma } from "../contextos/ProveedorIdioma";
import { ErrorPanel } from "../componentes/Estados";
import { mensajeDeError } from "../api/cliente";
import {
  IconoDocumentos,
  IconoExcepciones,
  IconoReloj,
  IconoPanel,
  IconoDistribuir,
  IconoAjustar,
  IconoResumen,
} from "../componentes/Iconos";
import { IndicadoresDocumental } from "../documental/componentes/IndicadoresDocumental";
import { BandejaDocumental } from "../documental/componentes/BandejaDocumental";
import { PanelVencimientos } from "../documental/componentes/PanelVencimientos";
import { CentroExcepcionesDocumental } from "../documental/componentes/CentroExcepcionesDocumental";
import { TableroDocumental } from "../documental/componentes/TableroDocumental";
import { PanelDistribucion } from "../documental/componentes/PanelDistribucion";
import { PlantillasDocumentales } from "../documental/componentes/PlantillasDocumentales";
import { ArchivoDocumentos } from "../documental/componentes/ArchivoDocumentos";
import { Cargando } from "../componentes/Estados";

const VisorInteligente = lazy(() =>
  import("../documental/componentes/VisorInteligente").then((m) => ({
    default: m.VisorInteligente,
  })),
);

const SECCIONES = [
  { id: "bandeja", etiqueta: "documental.tab.bandeja", icono: IconoResumen },
  {
    id: "vencimientos",
    etiqueta: "documental.tab.vencimientos",
    icono: IconoReloj,
  },
  {
    id: "excepciones",
    etiqueta: "documental.tab.excepciones",
    icono: IconoExcepciones,
  },
  { id: "tablero", etiqueta: "documental.tab.tablero", icono: IconoPanel },
  {
    id: "distribucion",
    etiqueta: "documental.tab.distribucion",
    icono: (props: { tamano?: number }) => (
      <IconoDistribuir eje="x" {...props} />
    ),
  },
  {
    id: "plantillas",
    etiqueta: "documental.tab.plantillas",
    icono: IconoAjustar,
  },
  {
    id: "documentos",
    etiqueta: "documental.tab.documentos",
    icono: IconoDocumentos,
  },
] as const;

type Seccion = (typeof SECCIONES)[number]["id"];

export function Documental() {
  const { t } = useIdioma();
  const { seccion } = useParams<{ seccion?: string }>();
  const navegar = useNavigate();
  const [documentoAbierto, setDocumentoAbierto] = useState<string | null>(null);

  const estadoServicio = useQuery({
    queryKey: ["documental-estado"],
    queryFn: obtenerEstadoDocumental,
    refetchInterval: 30000,
  });

  const activa: Seccion =
    SECCIONES.find((item) => item.id === seccion)?.id ?? "bandeja";

  const abrirDocumento = useCallback((documentoId: string) => {
    setDocumentoAbierto(documentoId);
  }, []);

  const volver = useCallback(() => {
    setDocumentoAbierto(null);
  }, []);

  const cambiarSeccion = (id: Seccion) => {
    setDocumentoAbierto(null);
    navegar(`/documental/${id}`);
  };

  if (estadoServicio.isError) {
    return (
      <ErrorPanel
        error={estadoServicio.error}
        mensaje={mensajeDeError(estadoServicio.error)}
        reintentar={() => estadoServicio.refetch()}
      />
    );
  }

  if (estadoServicio.data && !estadoServicio.data.habilitado) {
    return (
      <div className="rounded-panel border border-alerta-borde bg-alerta-tenue p-espacio-4">
        <p className="text-pequeno font-semibold text-alerta-texto">
          {t("documental.sinConfigurar")}
        </p>
      </div>
    );
  }

  return (
    <div
      className="flex min-h-0 flex-col gap-espacio-4"
      style={{ height: "calc(100dvh - 9rem)" }}
    >
      {!documentoAbierto ? (
        <>
          <IndicadoresDocumental />

          <div
            role="tablist"
            aria-label={t("documental.titulo")}
            className="flex gap-espacio-1 overflow-x-auto rounded-control bg-lienzo p-espacio-1"
            data-testid="documental-tabs"
          >
            {SECCIONES.map((item) => {
              const Icono = item.icono;
              return (
                <button
                  key={item.id}
                  type="button"
                  role="tab"
                  aria-selected={activa === item.id}
                  onClick={() => cambiarSeccion(item.id)}
                  data-testid={`documental-tab-${item.id}`}
                  className={`inline-flex min-h-control-pequeno shrink-0 items-center gap-espacio-2 rounded-control px-espacio-3 text-pequeno font-semibold transition-colors focus-visible:outline-foco ${
                    activa === item.id
                      ? "bg-superficie text-tinta shadow-plano"
                      : "text-tinta-suave hover:text-tinta"
                  }`}
                >
                  <Icono tamano={16} />
                  {t(item.etiqueta)}
                </button>
              );
            })}
          </div>
        </>
      ) : null}

      <div className="flex min-h-0 flex-1 flex-col">
        {documentoAbierto ? (
          <Suspense fallback={<Cargando />}>
            <VisorInteligente
              documentoId={documentoAbierto}
              alVolver={volver}
            />
          </Suspense>
        ) : activa === "bandeja" ? (
          <BandejaDocumental alAbrirDocumento={abrirDocumento} />
        ) : activa === "vencimientos" ? (
          <PanelVencimientos alAbrirDocumento={abrirDocumento} />
        ) : activa === "excepciones" ? (
          <CentroExcepcionesDocumental alAbrirDocumento={abrirDocumento} />
        ) : activa === "distribucion" ? (
          <PanelDistribucion />
        ) : activa === "tablero" ? (
          <TableroDocumental />
        ) : activa === "documentos" ? (
          <ArchivoDocumentos alAbrirDocumento={abrirDocumento} />
        ) : (
          <PlantillasDocumentales />
        )}
      </div>
    </div>
  );
}
