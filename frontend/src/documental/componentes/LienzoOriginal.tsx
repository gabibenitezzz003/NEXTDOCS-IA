import { useEffect, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Document, Page, pdfjs } from "react-pdf";
import { obtenerOriginal } from "../../api/documental";
import type { DocumentoMotor } from "../../api/documental";
import { Cargando, Vacio } from "../../componentes/Estados";
import { BotonIcono } from "../../componentes/Interfaz";
import { IconoIzquierda, IconoDerecha } from "../../componentes/Iconos";
import { useIdioma } from "../../contextos/ProveedorIdioma";

import "react-pdf/dist/Page/TextLayer.css";

pdfjs.GlobalWorkerOptions.workerSrc = new URL(
  "pdfjs-dist/build/pdf.worker.min.mjs",
  import.meta.url,
).toString();

export interface Evidencia {
  pagina: number | null;
  recorte: number[] | null;
  textoFuente: string | null;
}

const esImagen = (tipoMime: string | null | undefined) =>
  String(tipoMime || "").startsWith("image/");

function Resaltado({ recorte }: { recorte: number[] | null }) {
  if (!Array.isArray(recorte) || recorte.length !== 4) return null;

  const [x, y, ancho, alto] = recorte;

  return (
    <div
      className="pointer-events-none absolute rounded-insignia border-2 border-alerta transition-all duration-100 motion-reduce:transition-none"
      style={{
        left: `${x * 100}%`,
        top: `${y * 100}%`,
        width: `${ancho * 100}%`,
        height: `${alto * 100}%`,
        backgroundColor: "rgba(217, 119, 6, 0.18)",
      }}
    />
  );
}

function VisorPdf({
  url,
  evidencia,
}: {
  url: string;
  evidencia: Evidencia | null;
}) {
  const { t } = useIdioma();
  const contenedor = useRef<HTMLDivElement>(null);

  const [totalPaginas, setTotalPaginas] = useState(0);
  const [navegacion, setNavegacion] = useState<{
    base: number | null;
    pagina: number | null;
  }>({ base: null, pagina: null });
  const [ancho, setAncho] = useState(0);

  const paginaEvidencia = evidencia?.pagina || null;
  const pagina =
    navegacion.base === paginaEvidencia && navegacion.pagina
      ? navegacion.pagina
      : (paginaEvidencia ?? 1);

  useEffect(() => {
    const nodo = contenedor.current;
    if (!nodo) return undefined;

    const observador = new ResizeObserver((entradas) => {
      const medida = entradas[0]?.contentRect?.width;
      if (medida) setAncho(Math.floor(medida) - 24);
    });

    observador.observe(nodo);
    return () => observador.disconnect();
  }, []);

  const irA = (destino: number) =>
    setNavegacion({ base: paginaEvidencia, pagina: destino });

  return (
    <div className="flex h-full flex-col">
      <div
        ref={contenedor}
        className="flex min-h-0 flex-1 justify-center overflow-auto bg-lienzo p-espacio-3"
      >
        <Document
          file={url}
          loading={<Cargando />}
          error={<Vacio titulo={t("documental.visor.originalNoDisponible")} />}
          onLoadSuccess={({ numPages }: { numPages: number }) =>
            setTotalPaginas(numPages)
          }
        >
          <div className="relative inline-block">
            <Page
              pageNumber={pagina}
              width={ancho > 0 ? ancho : undefined}
              loading={<Cargando />}
              renderAnnotationLayer={false}
            />
            {paginaEvidencia === pagina ? (
              <Resaltado recorte={evidencia?.recorte ?? null} />
            ) : null}
          </div>
        </Document>
      </div>

      <div className="flex items-center gap-espacio-2 border-t border-borde px-espacio-4 py-espacio-2">
        <BotonIcono
          tamano="sm"
          disabled={pagina <= 1}
          aria-label={t("documental.visor.paginaAnterior")}
          onClick={() => irA(pagina - 1)}
        >
          <IconoIzquierda />
        </BotonIcono>

        <span className="min-w-20 text-center text-micro text-tinta-suave">
          {t("documental.visor.pagina")} {pagina}
          {totalPaginas ? ` / ${totalPaginas}` : ""}
        </span>

        <BotonIcono
          tamano="sm"
          disabled={Boolean(totalPaginas) && pagina >= totalPaginas}
          aria-label={t("documental.visor.paginaSiguiente")}
          onClick={() => irA(pagina + 1)}
        >
          <IconoDerecha />
        </BotonIcono>

        <div className="min-w-0 flex-1">
          {evidencia?.textoFuente ? (
            <p className="truncate text-micro text-tinta-suave">
              {t("documental.visor.textoFuente")}: {evidencia.textoFuente}
            </p>
          ) : null}
        </div>
      </div>
    </div>
  );
}

export function LienzoOriginal({
  documento,
  evidencia,
}: {
  documento: DocumentoMotor | undefined;
  evidencia: Evidencia | null;
}) {
  const { t } = useIdioma();
  const original = useQuery({
    queryKey: ["documental-original", documento?.id],
    queryFn: () => obtenerOriginal(documento!.id),
    enabled: Boolean(documento?.id),
    refetchInterval: 240000,
    refetchOnWindowFocus: false,
    staleTime: 0,
    select: (datos) => datos.url,
  });

  if (original.isError) {
    return <Vacio titulo={t("documental.visor.originalNoDisponible")} />;
  }
  if (!original.data) return <Cargando />;

  if (!esImagen(documento?.tipo_mime)) {
    return <VisorPdf url={original.data} evidencia={evidencia} />;
  }

  return (
    <div className="relative h-full overflow-auto">
      <div className="relative inline-block w-full">
        <img
          src={original.data}
          alt={documento?.nombre_archivo || "documento"}
          className="block w-full"
        />
        <Resaltado recorte={evidencia?.recorte ?? null} />
      </div>
    </div>
  );
}
