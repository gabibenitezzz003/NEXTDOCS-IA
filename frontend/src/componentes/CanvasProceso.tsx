import { useMemo, useRef, useState } from "react";
import type { SVGProps } from "react";
import type {
  GrafoProceso,
  NodoProceso,
  TipoNodoProceso,
} from "../api/procesos";
import { disponerGrafo } from "../utilidades/disposicionGrafo";
import { avisosNodo } from "../utilidades/grafoProceso";
import { Boton, BotonIcono, Selector } from "./Interfaz";
import {
  IconoAjustar,
  IconoCheck,
  IconoDocumentos,
  IconoInfo,
  IconoMas,
  IconoMenos,
  IconoOperacion,
  IconoPanel,
  IconoProceso,
  IconoRecargar,
  IconoReloj,
  IconoSupervisora,
  IconoTareas,
} from "./Iconos";
import { useIdioma } from "../contextos/ProveedorIdioma";

const ANCHO = 180;
const ALTO = 64;
const RADIO_PUERTO = 6;
const SEP_X = 150;
const SEP_Y = 96;
const GRID = 24;

export interface SeleccionCanvas {
  tipo: "nodo" | "arista";
  id: string;
}

type Arrastre =
  | { tipo: "pan"; x0: number; y0: number; vx: number; vy: number }
  | { tipo: "nodo"; id: string; dx: number; dy: number }
  | { tipo: "conexion"; origen: string; x: number; y: number };

function posicionDe(nodo: NodoProceso): { x: number; y: number } | null {
  const posicion = nodo.configuracion?.posicion;
  if (
    posicion &&
    typeof posicion === "object" &&
    typeof (posicion as { x?: unknown }).x === "number" &&
    typeof (posicion as { y?: unknown }).y === "number"
  ) {
    return posicion as { x: number; y: number };
  }
  return null;
}

function colorDe(tipo: TipoNodoProceso): { relleno: string; borde: string } {
  if (tipo === "INICIO" || tipo === "FIN") {
    return { relleno: "var(--color-lienzo)", borde: "var(--color-borde-fuerte)" };
  }
  if (tipo === "DECISION") {
    return { relleno: "var(--color-alerta-tenue)", borde: "var(--color-alerta-borde)" };
  }
  if (tipo === "REVISION_HUMANA" || tipo === "TAREA_EXTERNA") {
    return { relleno: "var(--color-violeta-tenue)", borde: "var(--color-violeta-borde)" };
  }
  return { relleno: "var(--color-superficie)", borde: "var(--color-borde)" };
}

const TIPOS_CANVAS: TipoNodoProceso[] = [
  "SOLICITUD_DOCUMENTO",
  "FORMULARIO",
  "VALIDACION_IA",
  "REVISION_HUMANA",
  "DECISION",
  "TAREA_EXTERNA",
  "NOTIFICACION",
  "TEMPORIZADOR",
  "ACCION_API",
  "SUBPROCESO",
];

export function claveArista(arista: {
  origen: string;
  destino: string;
}): string {
  return `${arista.origen}->${arista.destino}`;
}

function IconoNodo({
  tipo,
  ...resto
}: { tipo: TipoNodoProceso } & SVGProps<SVGSVGElement>) {
  switch (tipo) {
    case "INICIO":
    case "FIN":
      return <IconoCheck {...resto} />;
    case "SOLICITUD_DOCUMENTO":
      return <IconoDocumentos {...resto} />;
    case "FORMULARIO":
      return <IconoPanel {...resto} />;
    case "VALIDACION_IA":
      return <IconoSupervisora {...resto} />;
    case "REVISION_HUMANA":
      return <IconoTareas {...resto} />;
    case "DECISION":
      return <IconoProceso {...resto} />;
    case "TAREA_EXTERNA":
      return <IconoOperacion {...resto} />;
    case "NOTIFICACION":
      return <IconoInfo {...resto} />;
    case "TEMPORIZADOR":
      return <IconoReloj {...resto} />;
    case "ACCION_API":
      return <IconoRecargar {...resto} />;
    default:
      return <IconoProceso {...resto} />;
  }
}

export function CanvasProceso({
  grafo,
  alCambiar,
  seleccion,
  alSeleccionar,
  deshabilitado = false,
  soloLectura = false,
}: {
  grafo: GrafoProceso;
  alCambiar: (grafo: GrafoProceso) => void;
  seleccion: SeleccionCanvas | null;
  alSeleccionar: (seleccion: SeleccionCanvas | null) => void;
  deshabilitado?: boolean;
  soloLectura?: boolean;
}) {
  const { t } = useIdioma();
  const svgRef = useRef<SVGSVGElement>(null);
  const [vista, setVista] = useState({ x: 40, y: 32, k: 1 });
  const [arrastre, setArrastre] = useState<Arrastre | null>(null);
  const [tipoNuevo, setTipoNuevo] = useState("");

  const automatica = useMemo(() => {
    const disposicion = disponerGrafo(grafo);
    const mapa = new Map<string, { x: number; y: number }>();
    for (const nodo of disposicion.nodos) {
      mapa.set(nodo.id, {
        x: 60 + nodo.columna * (ANCHO + SEP_X),
        y: 60 + nodo.fila * (ALTO + SEP_Y),
      });
    }
    return mapa;
  }, [grafo]);

  const posicion = (nodo: NodoProceso) =>
    posicionDe(nodo) ?? automatica.get(nodo.id) ?? { x: 60, y: 60 };

  function mundoDe(evento: { clientX: number; clientY: number }) {
    const caja = svgRef.current?.getBoundingClientRect();
    if (!caja) return { x: 0, y: 0 };
    return {
      x: (evento.clientX - caja.left - vista.x) / vista.k,
      y: (evento.clientY - caja.top - vista.y) / vista.k,
    };
  }

  function mutarNodo(id: string, mutar: (nodo: NodoProceso) => NodoProceso) {
    alCambiar({
      ...grafo,
      nodos: grafo.nodos.map((nodo) => (nodo.id === id ? mutar(nodo) : nodo)),
    });
  }

  function fijarPosicion(id: string, x: number, y: number) {
    mutarNodo(id, (nodo) => ({
      ...nodo,
      configuracion: {
        ...nodo.configuracion,
        posicion: { x: Math.round(x / 4) * 4, y: Math.round(y / 4) * 4 },
      },
    }));
  }

  function conectar(origen: string, destino: string) {
    if (origen === destino) return;
    const nodos = new Map(grafo.nodos.map((nodo) => [nodo.id, nodo]));
    const desde = nodos.get(origen);
    const hasta = nodos.get(destino);
    if (!desde || !hasta || desde.tipo === "FIN" || hasta.tipo === "INICIO") return;
    const duplicada = grafo.aristas.some(
      (arista) => arista.origen === origen && arista.destino === destino,
    );
    if (duplicada) return;
    const arista = { origen, destino };
    alCambiar({ ...grafo, aristas: [...grafo.aristas, arista] });
    alSeleccionar({ tipo: "arista", id: claveArista(arista) });
  }

  function agregarNodo(tipo: TipoNodoProceso) {
    const caja = svgRef.current?.getBoundingClientRect();
    const centro = caja
      ? mundoDe({
          clientX: caja.left + caja.width / 2,
          clientY: caja.top + caja.height / 2,
        })
      : { x: 240, y: 120 };
    const ocupadas = new Set(
      grafo.nodos.map((nodo) => {
        const p = posicion(nodo);
        return `${Math.round(p.x / 40)}:${Math.round(p.y / 40)}`;
      }),
    );
    let { x, y } = { x: centro.x - ANCHO / 2, y: centro.y - ALTO / 2 };
    while (ocupadas.has(`${Math.round(x / 40)}:${Math.round(y / 40)}`)) {
      x += GRID;
      y += GRID;
    }
    const nodo: NodoProceso = {
      id: "nodo-" + Math.random().toString(36).slice(2, 9),
      tipo,
      nombre: t(`tipoPaso.${tipo}`),
      configuracion: { posicion: { x, y } },
    };
    alCambiar({ ...grafo, nodos: [...grafo.nodos, nodo] });
    alSeleccionar({ tipo: "nodo", id: nodo.id });
  }

  function alPointerDown(evento: React.PointerEvent<SVGSVGElement>) {
    if (evento.button !== 0) return;
    svgRef.current?.setPointerCapture(evento.pointerId);
    setArrastre({
      tipo: "pan",
      x0: evento.clientX,
      y0: evento.clientY,
      vx: vista.x,
      vy: vista.y,
    });
    alSeleccionar(null);
  }

  function alPointerMove(evento: React.PointerEvent<SVGSVGElement>) {
    if (!arrastre) return;
    if (arrastre.tipo === "pan") {
      setVista((actual) => ({
        ...actual,
        x: arrastre.vx + (evento.clientX - arrastre.x0),
        y: arrastre.vy + (evento.clientY - arrastre.y0),
      }));
      return;
    }
    const punto = mundoDe(evento);
    if (arrastre.tipo === "nodo") {
      if (deshabilitado) return;
      fijarPosicion(arrastre.id, punto.x - arrastre.dx, punto.y - arrastre.dy);
      return;
    }
    setArrastre({ ...arrastre, x: punto.x, y: punto.y });
  }

  function destinoEn(punto: { x: number; y: number }, origen: string) {
    return grafo.nodos.find((nodo) => {
      if (nodo.tipo === "INICIO" || nodo.id === origen) return false;
      const p = posicion(nodo);
      return (
        punto.x >= p.x - 20 &&
        punto.x <= p.x + ANCHO + 20 &&
        punto.y >= p.y - 20 &&
        punto.y <= p.y + ALTO + 20
      );
    });
  }

  function alPointerUp(evento: React.PointerEvent<SVGSVGElement>) {
    if (arrastre?.tipo === "conexion" && !deshabilitado) {
      const destino = destinoEn(mundoDe(evento), arrastre.origen);
      if (destino) conectar(arrastre.origen, destino.id);
    }
    setArrastre(null);
  }

  function alWheel(evento: React.WheelEvent<SVGSVGElement>) {
    evento.preventDefault();
    const factor = evento.deltaY < 0 ? 1.1 : 0.9;
    setVista((actual) => {
      const k = Math.min(2, Math.max(0.4, actual.k * factor));
      const caja = svgRef.current?.getBoundingClientRect();
      if (!caja) return { ...actual, k };
      const px = evento.clientX - caja.left;
      const py = evento.clientY - caja.top;
      return {
        k,
        x: px - ((px - actual.x) / actual.k) * k,
        y: py - ((py - actual.y) / actual.k) * k,
      };
    });
  }

  function sembrarExtremos() {
    const inicio: NodoProceso = {
      id: "inicio",
      tipo: "INICIO",
      nombre: t("tipoNodo.INICIO"),
      configuracion: { posicion: { x: 80, y: 120 } },
    };
    const fin: NodoProceso = {
      id: "fin",
      tipo: "FIN",
      nombre: t("tipoNodo.FIN"),
      configuracion: { posicion: { x: 480, y: 120 } },
    };
    alCambiar({
      ...grafo,
      nodos: [inicio, fin],
      aristas: [...grafo.aristas, { origen: "inicio", destino: "fin" }],
    });
  }

  function zoom(factor: number) {
    setVista((actual) => {
      const k = Math.min(2, Math.max(0.4, actual.k * factor));
      const caja = svgRef.current?.getBoundingClientRect();
      const px = (caja?.width ?? 600) / 2;
      const py = (caja?.height ?? 400) / 2;
      return {
        k,
        x: px - ((px - actual.x) / actual.k) * k,
        y: py - ((py - actual.y) / actual.k) * k,
      };
    });
  }

  const seleccionArista =
    seleccion?.tipo === "arista" ? seleccion.id : null;
  const seleccionNodo = seleccion?.tipo === "nodo" ? seleccion.id : null;

  return (
    <div>
      {!soloLectura ? (
      <div className="mb-espacio-3 flex flex-wrap items-center gap-espacio-3">
        <Selector
          etiqueta={t("procesos.agregarPaso")}
          aria-label={t("canvas.agregarNodo")}
          value={tipoNuevo}
          disabled={deshabilitado}
          onChange={(evento) => {
            const elegido = evento.target.value;
            setTipoNuevo("");
            if (elegido) agregarNodo(elegido as TipoNodoProceso);
          }}
          className="w-full sm:max-w-xs"
        >
          <option value="">{t("canvas.agregarNodo")}</option>
          {TIPOS_CANVAS.map((tipo) => (
            <option key={tipo} value={tipo}>
              {t(`tipoPaso.${tipo}`)}
            </option>
          ))}
        </Selector>
        <p className="text-pequeno text-tinta-suave">
          {t("canvas.ayudaBreve")}
        </p>
      </div>
      ) : null}
      <div className="relative overflow-hidden rounded-panel border border-borde bg-lienzo">
        <svg
          ref={svgRef}
          role="application"
          aria-label={t("canvas.ariaRecorrido", {
            cantidad: grafo.nodos.length,
          })}
          className="block h-[30rem] w-full touch-none select-none sm:h-[34rem]"
          onPointerDown={alPointerDown}
          onPointerMove={alPointerMove}
          onPointerUp={alPointerUp}
          onWheel={alWheel}
        >
          <defs>
            <pattern
              id="canvas-grid"
              width={GRID}
              height={GRID}
              patternUnits="userSpaceOnUse"
            >
              <circle cx={1.5} cy={1.5} r={1.5} fill="var(--color-borde)" />
            </pattern>
            <marker
              id="canvas-flecha"
              viewBox="0 0 8 8"
              refX="7"
              refY="4"
              markerWidth="7"
              markerHeight="7"
              orient="auto-start-reverse"
            >
              <path d="M 0 0 L 8 4 L 0 8 z" fill="var(--color-borde-fuerte)" />
            </marker>
          </defs>
          <g transform={`translate(${vista.x} ${vista.y}) scale(${vista.k})`}>
            <rect
              x={-4000}
              y={-4000}
              width={8000}
              height={8000}
              fill="url(#canvas-grid)"
            />
            {grafo.aristas.map((arista) => {
              const origen = grafo.nodos.find((nodo) => nodo.id === arista.origen);
              const destino = grafo.nodos.find((nodo) => nodo.id === arista.destino);
              if (!origen || !destino) return null;
              const a = posicion(origen);
              const b = posicion(destino);
              const x1 = a.x + ANCHO;
              const y1 = a.y + ALTO / 2;
              const x2 = b.x;
              const y2 = b.y + ALTO / 2;
              const control = Math.max(40, Math.abs(x2 - x1) / 2);
              const trazo = `M ${x1} ${y1} C ${x1 + control} ${y1}, ${x2 - control} ${y2}, ${x2} ${y2}`;
              const clave = claveArista(arista);
              const activa = seleccionArista === clave;
              return (
                <g key={clave}>
                  <path
                    d={trazo}
                    fill="none"
                    stroke="transparent"
                    strokeWidth={14}
                    className="cursor-pointer"
                    onPointerDown={(evento) => evento.stopPropagation()}
                    onClick={() =>
                      alSeleccionar({ tipo: "arista", id: clave })
                    }
                  />
                  <path
                    d={trazo}
                    fill="none"
                    stroke={
                      activa
                        ? "var(--color-violeta)"
                        : "var(--color-borde-fuerte)"
                    }
                    strokeWidth={activa ? 2.5 : 1.5}
                    markerEnd="url(#canvas-flecha)"
                    className="pointer-events-none"
                  />
                  {arista.condicion ? (
                    <text
                      x={(x1 + x2) / 2}
                      y={(y1 + y2) / 2 - 8}
                      textAnchor="middle"
                      className="pointer-events-none fill-[var(--color-tinta-suave)] text-[10px]"
                    >
                      {arista.condicion}
                    </text>
                  ) : null}
                </g>
              );
            })}
            {arrastre?.tipo === "conexion"
              ? (() => {
                  const origen = grafo.nodos.find(
                    (nodo) => nodo.id === arrastre.origen,
                  );
                  if (!origen) return null;
                  const a = posicion(origen);
                  const x1 = a.x + ANCHO;
                  const y1 = a.y + ALTO / 2;
                  const control = Math.max(40, Math.abs(arrastre.x - x1) / 2);
                  return (
                    <path
                      d={`M ${x1} ${y1} C ${x1 + control} ${y1}, ${arrastre.x - control} ${arrastre.y}, ${arrastre.x} ${arrastre.y}`}
                      fill="none"
                      stroke="var(--color-violeta)"
                      strokeWidth={2}
                      strokeDasharray="5 4"
                      className="pointer-events-none"
                    />
                  );
                })()
              : null}
            {grafo.nodos.map((nodo) => {
              const p = posicion(nodo);
              const color = colorDe(nodo.tipo);
              const activo = seleccionNodo === nodo.id;
              const conAviso = avisosNodo(nodo).length > 0;
              const destinoConexion =
                arrastre?.tipo === "conexion" && !deshabilitado
                  ? destinoEn({ x: arrastre.x, y: arrastre.y }, arrastre.origen)
                      ?.id === nodo.id
                  : false;
              return (
                <g
                  key={nodo.id}
                  transform={`translate(${p.x} ${p.y})`}
                  className={deshabilitado ? "cursor-default" : "cursor-grab"}
                  onPointerDown={(evento) => {
                    evento.stopPropagation();
                    if (evento.button !== 0) return;
                    alSeleccionar({ tipo: "nodo", id: nodo.id });
                    svgRef.current?.setPointerCapture(evento.pointerId);
                    const punto = mundoDe(evento);
                    setArrastre({
                      tipo: "nodo",
                      id: nodo.id,
                      dx: punto.x - p.x,
                      dy: punto.y - p.y,
                    });
                  }}
                >
                  <rect
                    width={ANCHO}
                    height={ALTO}
                    rx={12}
                    fill={color.relleno}
                    stroke={
                      activo || destinoConexion
                        ? "var(--color-violeta)"
                        : color.borde
                    }
                    strokeWidth={activo ? 2.5 : destinoConexion ? 3 : 1.5}
                    strokeDasharray={destinoConexion ? "6 4" : undefined}
                  />
                  <text
                    x={14}
                    y={20}
                    className="fill-[var(--color-tinta-suave)] text-[9px] uppercase"
                    style={{ letterSpacing: "0.4px" }}
                  >
                    {t(`tipoNodo.${nodo.tipo}`)}
                  </text>
                  {conAviso ? (
                    <g className="pointer-events-none">
                      <circle
                        cx={ANCHO - 10}
                        cy={6}
                        r={9}
                        fill="var(--color-alerta)"
                        stroke="var(--color-superficie)"
                        strokeWidth={1.5}
                      />
                      <text
                        x={ANCHO - 10}
                        y={10}
                        textAnchor="middle"
                        className="fill-[var(--color-superficie)] text-[11px] font-bold"
                      >
                        !
                      </text>
                    </g>
                  ) : null}
                  <text
                    x={14}
                    y={42}
                    className="fill-[var(--color-tinta)] text-[13px] font-semibold"
                  >
                    {(nodo.nombre ?? nodo.tipo).length > 22
                      ? (nodo.nombre ?? nodo.tipo).slice(0, 21) + "…"
                      : nodo.nombre ?? nodo.tipo}
                  </text>
                  <circle
                    cx={ANCHO - 24}
                    cy={ALTO / 2}
                    r={14}
                    fill="var(--color-superficie)"
                    stroke={color.borde}
                    strokeWidth={1}
                  />
                  <IconoNodo
                    tipo={nodo.tipo}
                    x={ANCHO - 34}
                    y={ALTO / 2 - 10}
                    width={20}
                    height={20}
                    className="text-tinta-media pointer-events-none"
                  />
                  {nodo.tipo !== "INICIO" ? (
                    <circle
                      cx={0}
                      cy={ALTO / 2}
                      r={RADIO_PUERTO}
                      fill="var(--color-superficie)"
                      stroke="var(--color-borde-fuerte)"
                      strokeWidth={1.5}
                    />
                  ) : null}
                  {nodo.tipo !== "FIN" ? (
                    <circle
                      cx={ANCHO}
                      cy={ALTO / 2}
                      r={RADIO_PUERTO + 1}
                      fill="var(--color-violeta-tenue)"
                      stroke="var(--color-violeta)"
                      strokeWidth={1.5}
                      className={deshabilitado ? "" : "cursor-crosshair"}
                      onPointerDown={(evento) => {
                        evento.stopPropagation();
                        if (evento.button !== 0 || deshabilitado) return;
                        svgRef.current?.setPointerCapture(evento.pointerId);
                        const punto = mundoDe(evento);
                        setArrastre({
                          tipo: "conexion",
                          origen: nodo.id,
                          x: punto.x,
                          y: punto.y,
                        });
                      }}
                    />
                  ) : null}
                </g>
              );
            })}
          </g>
        </svg>
        {grafo.nodos.length === 0 ? (
          <div className="absolute inset-0 grid place-items-center bg-lienzo/80 p-espacio-6 text-center">
            <div className="space-y-espacio-3">
              <p className="text-pequeno text-tinta-suave">
                {soloLectura
                  ? t("canvas.vacioLectura")
                  : t("canvas.vacio")}
              </p>
              {!soloLectura && !deshabilitado ? (
                <Boton variante="primario" onClick={sembrarExtremos}>
                  {t("canvas.empezar")}
                </Boton>
              ) : null}
            </div>
          </div>
        ) : null}
        <div className="absolute bottom-espacio-3 right-espacio-3 flex gap-espacio-1 rounded-panel border border-borde bg-superficie p-espacio-1 shadow-panel">
          <BotonIcono
            variante="fantasma"
            tamano="sm"
            aria-label={t("canvas.alejar")}
            onClick={() => zoom(0.85)}
          >
            <IconoMenos tamano={14} />
          </BotonIcono>
          <BotonIcono
            variante="fantasma"
            tamano="sm"
            aria-label={t("canvas.ajustar")}
            onClick={() => setVista({ x: 40, y: 32, k: 1 })}
          >
            <IconoAjustar tamano={14} />
          </BotonIcono>
          <BotonIcono
            variante="fantasma"
            tamano="sm"
            aria-label={t("canvas.acercar")}
            onClick={() => zoom(1.18)}
          >
            <IconoMas tamano={14} />
          </BotonIcono>
        </div>
      </div>
      <p className="mt-espacio-2 text-pequeno text-tinta-suave">
        {t("canvas.pasos", { cantidad: grafo.nodos.length })}
      </p>
    </div>
  );
}


