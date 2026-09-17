import { useEffect, useMemo, useRef, useState } from "react";
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
  IconoBifurcar,
  IconoCheck,
  IconoCopiar,
  IconoDeshacer,
  IconoDocumentos,
  IconoEliminar,
  IconoFirma,
  IconoInfo,
  IconoMas,
  IconoAlinear,
  IconoDistribuir,
  IconoMenos,
  IconoOperacion,
  IconoPanel,
  IconoProceso,
  IconoRecargar,
  IconoRehacer,
  IconoReloj,
  IconoSupervisora,
  IconoTareas,
  IconoUnir,
} from "./Iconos";
import { useIdioma } from "../contextos/ProveedorIdioma";

const ANCHO = 180;
const ALTO = 64;
const RADIO_PUERTO = 6;
const SEP_X = 150;
const SEP_Y = 96;
const GRID = 24;
const MINI_ANCHO = 168;
const MINI_ALTO = 104;
const MINI_MARGEN = 60;

export interface SeleccionCanvas {
  tipo: "nodo" | "arista" | "nodos";
  id: string;
  ids?: string[];
}

type Arrastre =
  | { tipo: "pan"; x0: number; y0: number; vx: number; vy: number }
  | {
      tipo: "nodo";
      id: string;
      dx: number;
      dy: number;
      grupo?: { id: string; x: number; y: number }[];
      x0: number;
      y0: number;
    }
  | { tipo: "conexion"; origen: string; x: number; y: number }
  | { tipo: "marquee"; x0: number; y0: number; x: number; y: number };

interface Guias {
  vertical?: number;
  horizontal?: number;
}

const TOLERANCIA_GUIA = 6;

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

interface ColorNodo {
  relleno: string;
  borde: string;
  acento: string;
  chipFondo: string;
  chipTexto: string;
}

function colorDe(tipo: TipoNodoProceso): ColorNodo {
  if (tipo === "INICIO" || tipo === "FIN") {
    return {
      relleno: "var(--color-violeta-tenue)",
      borde: "var(--color-violeta-borde)",
      acento: "var(--color-violeta)",
      chipFondo: "var(--color-violeta)",
      chipTexto: "text-blanco",
    };
  }
  if (tipo === "DECISION") {
    return {
      relleno: "var(--color-superficie)",
      borde: "var(--color-alerta-borde)",
      acento: "var(--color-alerta-texto)",
      chipFondo: "var(--color-alerta-tenue)",
      chipTexto: "text-alerta-texto",
    };
  }
  if (tipo === "PARALELO" || tipo === "UNION") {
    return {
      relleno: "var(--color-superficie)",
      borde: "var(--color-informacion-borde)",
      acento: "var(--color-informacion)",
      chipFondo: "var(--color-informacion-tenue)",
      chipTexto: "text-informacion",
    };
  }
  if (tipo === "FIRMA") {
    return {
      relleno: "var(--color-superficie)",
      borde: "var(--color-exito-borde)",
      acento: "var(--color-exito-texto)",
      chipFondo: "var(--color-exito-tenue)",
      chipTexto: "text-exito-texto",
    };
  }
  if (tipo === "REVISION_HUMANA" || tipo === "TAREA_EXTERNA") {
    return {
      relleno: "var(--color-superficie)",
      borde: "var(--color-violeta-borde)",
      acento: "var(--color-violeta)",
      chipFondo: "var(--color-violeta-tenue)",
      chipTexto: "text-accion-tonal-texto",
    };
  }
  return {
    relleno: "var(--color-superficie)",
    borde: "var(--color-borde)",
    acento: "var(--color-tinta-suave)",
    chipFondo: "var(--color-neutro-tenue)",
    chipTexto: "text-tinta-media",
  };
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
  "PARALELO",
  "UNION",
  "FIRMA",
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
    case "PARALELO":
      return <IconoBifurcar {...resto} />;
    case "UNION":
      return <IconoUnir {...resto} />;
    case "FIRMA":
      return <IconoFirma {...resto} />;
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
  alDeshacer,
  alRehacer,
  puedeDeshacer = false,
  puedeRehacer = false,
  alDuplicarNodo,
  alEliminarNodo,
  alEliminarArista,
}: {
  grafo: GrafoProceso;
  alCambiar: (grafo: GrafoProceso, continuo?: boolean) => void;
  seleccion: SeleccionCanvas | null;
  alSeleccionar: (seleccion: SeleccionCanvas | null) => void;
  deshabilitado?: boolean;
  soloLectura?: boolean;
  alDeshacer?: () => void;
  alRehacer?: () => void;
  puedeDeshacer?: boolean;
  puedeRehacer?: boolean;
  alDuplicarNodo?: (id: string) => void;
  alEliminarNodo?: (id: string) => void;
  alEliminarArista?: (clave: string) => void;
}) {
  const { t } = useIdioma();
  const svgRef = useRef<SVGSVGElement>(null);
  const [vista, setVista] = useState({ x: 40, y: 32, k: 1 });
  const [arrastre, setArrastre] = useState<Arrastre | null>(null);
  const [tipoNuevo, setTipoNuevo] = useState("");
  const [guias, setGuias] = useState<Guias>({});

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

  function fijarPosicion(id: string, x: number, y: number, continuo = false) {
    const siguiente = {
      ...grafo,
      nodos: grafo.nodos.map((nodo) =>
        nodo.id === id
          ? {
              ...nodo,
              configuracion: {
                ...nodo.configuracion,
                posicion: {
                  x: Math.round(x / 4) * 4,
                  y: Math.round(y / 4) * 4,
                },
              },
            }
          : nodo,
      ),
    };
    alCambiar(siguiente, continuo);
  }

  function conectar(origen: string, destino: string) {
    if (origen === destino) return;
    const nodos = new Map(grafo.nodos.map((nodo) => [nodo.id, nodo]));
    const desde = nodos.get(origen);
    const hasta = nodos.get(destino);
    if (!desde || !hasta || desde.tipo === "FIN" || hasta.tipo === "INICIO")
      return;
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
    let paso = 0;
    while (ocupadas.has(`${Math.round(x / 40)}:${Math.round(y / 40)}`)) {
      paso += 1;
      x = centro.x - ANCHO / 2 + paso * (GRID * 2);
      y = centro.y - ALTO / 2 + paso * (ALTO + GRID);
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
    if (evento.shiftKey && !deshabilitado) {
      const punto = mundoDe(evento);
      setArrastre({
        tipo: "marquee",
        x0: punto.x,
        y0: punto.y,
        x: punto.x,
        y: punto.y,
      });
      return;
    }
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
    if (arrastre.tipo === "marquee") {
      setArrastre({ ...arrastre, x: punto.x, y: punto.y });
      return;
    }
    if (arrastre.tipo === "nodo") {
      if (deshabilitado) return;
      if (arrastre.grupo && arrastre.grupo.length > 1) {
        const dx = punto.x - arrastre.x0;
        const dy = punto.y - arrastre.y0;
        const movimientos = new Map<string, { x: number; y: number }>();
        for (const miembro of arrastre.grupo) {
          movimientos.set(miembro.id, { x: miembro.x + dx, y: miembro.y + dy });
        }
        fijarPosiciones(movimientos, true);
        return;
      }
      const crudo = { x: punto.x - arrastre.dx, y: punto.y - arrastre.dy };
      const ajustado = guiasPara(arrastre.id, crudo.x, crudo.y);
      setGuias(ajustado.guias);
      fijarPosicion(arrastre.id, ajustado.x, ajustado.y, true);
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
    if (arrastre?.tipo === "marquee") {
      const minX = Math.min(arrastre.x0, arrastre.x);
      const maxX = Math.max(arrastre.x0, arrastre.x);
      const minY = Math.min(arrastre.y0, arrastre.y);
      const maxY = Math.max(arrastre.y0, arrastre.y);
      const dentro = grafo.nodos
        .filter((nodo) => {
          const p = posicion(nodo);
          return (
            p.x >= minX &&
            p.x + ANCHO <= maxX &&
            p.y >= minY &&
            p.y + ALTO <= maxY
          );
        })
        .map((nodo) => nodo.id);
      marcar([...marcados, ...dentro]);
    }
    setGuias({});
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

  function duplicarMarcados() {
    const elegidos = grafo.nodos.filter(
      (nodo) =>
        marcados.has(nodo.id) && nodo.tipo !== "INICIO" && nodo.tipo !== "FIN",
    );
    if (!elegidos.length) return;
    const nuevas = new Map<string, string>();
    const copias = elegidos.map((nodo) => {
      const id =
        "paso-" +
        Math.random().toString(36).slice(2, 8) +
        Date.now().toString(36);
      nuevas.set(nodo.id, id);
      const p = posicion(nodo);
      return {
        ...structuredClone(nodo),
        id,
        nombre: `${nodo.nombre ?? nodo.tipo} ${t("canvas.copia")}`,
        configuracion: {
          ...nodo.configuracion,
          posicion: { x: p.x + GRID, y: p.y + GRID },
        },
      };
    });
    const aristasCopiadas = grafo.aristas
      .filter(
        (arista) => nuevas.has(arista.origen) && nuevas.has(arista.destino),
      )
      .map((arista) => ({
        ...arista,
        origen: nuevas.get(arista.origen)!,
        destino: nuevas.get(arista.destino)!,
      }));
    alCambiar({
      ...grafo,
      nodos: [...grafo.nodos, ...copias],
      aristas: [...grafo.aristas, ...aristasCopiadas],
    });
    const ids = copias.map((nodo) => nodo.id);
    alSeleccionar(
      ids.length === 1
        ? { tipo: "nodo", id: ids[0] }
        : { tipo: "nodos", id: ids.join(","), ids },
    );
  }

  function moverSeleccion(dx: number, dy: number) {
    if (!marcados.size) return;
    const movimientos = new Map<string, { x: number; y: number }>();
    for (const nodo of grafo.nodos) {
      if (!marcados.has(nodo.id)) continue;
      const p = posicion(nodo);
      movimientos.set(nodo.id, { x: p.x + dx, y: p.y + dy });
    }
    fijarPosiciones(movimientos);
  }

  useEffect(() => {
    if (deshabilitado || soloLectura) return;
    const alTecla = (evento: KeyboardEvent) => {
      const destino = evento.target as HTMLElement | null;
      if (
        destino &&
        (["INPUT", "TEXTAREA", "SELECT"].includes(destino.tagName) ||
          destino.isContentEditable)
      )
        return;
      const paso = evento.shiftKey ? 4 : GRID;
      if (evento.key === "ArrowUp") {
        evento.preventDefault();
        moverSeleccion(0, -paso);
      } else if (evento.key === "ArrowDown") {
        evento.preventDefault();
        moverSeleccion(0, paso);
      } else if (evento.key === "ArrowLeft") {
        evento.preventDefault();
        moverSeleccion(-paso, 0);
      } else if (evento.key === "ArrowRight") {
        evento.preventDefault();
        moverSeleccion(paso, 0);
      } else if (
        (evento.ctrlKey || evento.metaKey) &&
        evento.key.toLowerCase() === "a"
      ) {
        evento.preventDefault();
        marcar(grafo.nodos.map((nodo) => nodo.id));
      } else if (
        (evento.ctrlKey || evento.metaKey) &&
        evento.key.toLowerCase() === "d"
      ) {
        evento.preventDefault();
        duplicarMarcados();
      } else if (evento.key === "Escape") {
        alSeleccionar(null);
      }
    };
    window.addEventListener("keydown", alTecla);
    return () => window.removeEventListener("keydown", alTecla);
  });

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

  const seleccionArista = seleccion?.tipo === "arista" ? seleccion.id : null;
  const marcados =
    seleccion?.tipo === "nodos"
      ? new Set(seleccion.ids ?? [])
      : new Set(seleccion?.tipo === "nodo" ? [seleccion.id] : []);
  const seleccionNodo = seleccion?.tipo === "nodo" ? seleccion.id : null;
  const nodoMarcado = seleccionNodo
    ? grafo.nodos.find((nodo) => nodo.id === seleccionNodo)
    : null;
  const posicionBarra = nodoMarcado ? posicion(nodoMarcado) : null;
  const esExtremoFijo =
    nodoMarcado?.tipo === "INICIO" || nodoMarcado?.tipo === "FIN";
  const aristaMarcada = seleccionArista
    ? grafo.aristas.find((arista) => claveArista(arista) === seleccionArista)
    : null;
  const origenMarcado = aristaMarcada
    ? grafo.nodos.find((nodo) => nodo.id === aristaMarcada.origen)
    : null;
  const destinoMarcado = aristaMarcada
    ? grafo.nodos.find((nodo) => nodo.id === aristaMarcada.destino)
    : null;
  const posicionArista =
    origenMarcado && destinoMarcado
      ? (() => {
          const a = posicion(origenMarcado);
          const b = posicion(destinoMarcado);
          return { x: (a.x + ANCHO + b.x) / 2, y: (a.y + b.y) / 2 + ALTO / 2 };
        })()
      : null;

  function marcar(ids: string[]) {
    const limpios = [...new Set(ids)].filter((id) =>
      grafo.nodos.some((nodo) => nodo.id === id),
    );
    if (limpios.length === 0) {
      alSeleccionar(null);
    } else if (limpios.length === 1) {
      alSeleccionar({ tipo: "nodo", id: limpios[0] });
    } else {
      alSeleccionar({ tipo: "nodos", id: limpios.join(","), ids: limpios });
    }
  }

  function fijarPosiciones(
    movimientos: Map<string, { x: number; y: number }>,
    continuo = false,
  ) {
    alCambiar(
      {
        ...grafo,
        nodos: grafo.nodos.map((nodo) => {
          const destino = movimientos.get(nodo.id);
          if (!destino) return nodo;
          return {
            ...nodo,
            configuracion: {
              ...nodo.configuracion,
              posicion: {
                x: Math.round(destino.x / 4) * 4,
                y: Math.round(destino.y / 4) * 4,
              },
            },
          };
        }),
      },
      continuo,
    );
  }

  function alinear(
    modo: "izquierda" | "centroX" | "derecha" | "arriba" | "centroY" | "abajo",
  ) {
    const elegidos = grafo.nodos.filter((nodo) => marcados.has(nodo.id));
    if (elegidos.length < 2) return;
    const puntos = elegidos.map((nodo) => ({ id: nodo.id, ...posicion(nodo) }));
    const movimientos = new Map<string, { x: number; y: number }>();
    if (modo === "izquierda") {
      const min = Math.min(...puntos.map((p) => p.x));
      puntos.forEach((p) => movimientos.set(p.id, { x: min, y: p.y }));
    } else if (modo === "derecha") {
      const max = Math.max(...puntos.map((p) => p.x));
      puntos.forEach((p) => movimientos.set(p.id, { x: max, y: p.y }));
    } else if (modo === "centroX") {
      const centro = puntos.reduce((suma, p) => suma + p.x, 0) / puntos.length;
      puntos.forEach((p) => movimientos.set(p.id, { x: centro, y: p.y }));
    } else if (modo === "arriba") {
      const min = Math.min(...puntos.map((p) => p.y));
      puntos.forEach((p) => movimientos.set(p.id, { x: p.x, y: min }));
    } else if (modo === "abajo") {
      const max = Math.max(...puntos.map((p) => p.y));
      puntos.forEach((p) => movimientos.set(p.id, { x: p.x, y: max }));
    } else {
      const centro = puntos.reduce((suma, p) => suma + p.y, 0) / puntos.length;
      puntos.forEach((p) => movimientos.set(p.id, { x: p.x, y: centro }));
    }
    fijarPosiciones(movimientos);
  }

  function distribuir(eje: "x" | "y") {
    const elegidos = grafo.nodos.filter((nodo) => marcados.has(nodo.id));
    if (elegidos.length < 3) return;
    const puntos = elegidos
      .map((nodo) => ({ id: nodo.id, ...posicion(nodo) }))
      .sort((a, b) => a[eje] - b[eje]);
    const primero = puntos[0];
    const ultimo = puntos[puntos.length - 1];
    const paso = (ultimo[eje] - primero[eje]) / (puntos.length - 1);
    const movimientos = new Map<string, { x: number; y: number }>();
    puntos.forEach((p, indice) =>
      movimientos.set(p.id, {
        x: eje === "x" ? primero.x + paso * indice : p.x,
        y: eje === "y" ? primero.y + paso * indice : p.y,
      }),
    );
    fijarPosiciones(movimientos);
  }

  function guiasPara(
    id: string,
    x: number,
    y: number,
  ): { x: number; y: number; guias: Guias } {
    const guiasEncontradas: Guias = {};
    let sx = x;
    let sy = y;
    for (const otro of grafo.nodos) {
      if (otro.id === id || marcados.has(otro.id)) continue;
      const p = posicion(otro);
      const candidatosX: [number, number][] = [
        [x, p.x],
        [x + ANCHO, p.x + ANCHO],
        [x + ANCHO / 2, p.x + ANCHO / 2],
        [x, p.x + ANCHO],
        [x + ANCHO, p.x],
      ];
      for (const [propio, ajeno] of candidatosX) {
        if (Math.abs(propio - ajeno) <= TOLERANCIA_GUIA) {
          sx += ajeno - propio;
          guiasEncontradas.vertical = ajeno;
          break;
        }
      }
      const candidatosY: [number, number][] = [
        [y, p.y],
        [y + ALTO, p.y + ALTO],
        [y + ALTO / 2, p.y + ALTO / 2],
        [y, p.y + ALTO],
        [y + ALTO, p.y],
      ];
      for (const [propio, ajeno] of candidatosY) {
        if (Math.abs(propio - ajeno) <= TOLERANCIA_GUIA) {
          sy += ajeno - propio;
          guiasEncontradas.horizontal = ajeno;
          break;
        }
      }
    }
    return { x: sx, y: sy, guias: guiasEncontradas };
  }

  const minimapa = useMemo(() => {
    if (!grafo.nodos.length) return null;
    const puntos = grafo.nodos.map((nodo) => posicion(nodo));
    const minX = Math.min(...puntos.map((p) => p.x)) - MINI_MARGEN;
    const minY = Math.min(...puntos.map((p) => p.y)) - MINI_MARGEN;
    const maxX = Math.max(...puntos.map((p) => p.x + ANCHO)) + MINI_MARGEN;
    const maxY = Math.max(...puntos.map((p) => p.y + ALTO)) + MINI_MARGEN;
    const escala = Math.min(
      MINI_ANCHO / (maxX - minX),
      MINI_ALTO / (maxY - minY),
    );
    const aMini = (p: { x: number; y: number }) => ({
      x: (p.x - minX) * escala,
      y: (p.y - minY) * escala,
    });
    return { escala, minX, minY, aMini };
  }, [grafo]);

  function aMiniInversa(
    mini: { escala: number; minX: number; minY: number },
    mx: number,
    my: number,
  ) {
    return { x: mx / mini.escala + mini.minX, y: my / mini.escala + mini.minY };
  }

  function irAPuntoMundo(punto: { x: number; y: number }) {
    const caja = svgRef.current?.getBoundingClientRect();
    if (!caja) return;
    setVista((actual) => ({
      ...actual,
      x: caja.width / 2 - punto.x * actual.k,
      y: caja.height / 2 - punto.y * actual.k,
    }));
  }

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
          {alDeshacer && alRehacer ? (
            <div className="flex items-center gap-espacio-1">
              <BotonIcono
                variante="fantasma"
                tamano="sm"
                aria-label={t("canvas.deshacer")}
                disabled={!puedeDeshacer || deshabilitado}
                onClick={alDeshacer}
              >
                <IconoDeshacer tamano={15} />
              </BotonIcono>
              <BotonIcono
                variante="fantasma"
                tamano="sm"
                aria-label={t("canvas.rehacer")}
                disabled={!puedeRehacer || deshabilitado}
                onClick={alRehacer}
              >
                <IconoRehacer tamano={15} />
              </BotonIcono>
            </div>
          ) : null}
          {marcados.size >= 2 ? (
            <div
              className="flex items-center gap-espacio-1"
              role="group"
              aria-label={t("canvas.alinearGrupo")}
            >
              {(
                [
                  ["izquierda", "canvas.alinearIzquierda"],
                  ["centroX", "canvas.alinearCentroX"],
                  ["derecha", "canvas.alinearDerecha"],
                  ["arriba", "canvas.alinearArriba"],
                  ["centroY", "canvas.alinearCentroY"],
                  ["abajo", "canvas.alinearAbajo"],
                ] as const
              ).map(([modo, clave]) => (
                <BotonIcono
                  key={modo}
                  variante="fantasma"
                  tamano="sm"
                  aria-label={t(clave)}
                  disabled={deshabilitado}
                  onClick={() => alinear(modo)}
                >
                  <IconoAlinear modo={modo} />
                </BotonIcono>
              ))}
              {marcados.size >= 3 ? (
                <>
                  <BotonIcono
                    variante="fantasma"
                    tamano="sm"
                    aria-label={t("canvas.distribuirHorizontal")}
                    disabled={deshabilitado}
                    onClick={() => distribuir("x")}
                  >
                    <IconoDistribuir eje="x" />
                  </BotonIcono>
                  <BotonIcono
                    variante="fantasma"
                    tamano="sm"
                    aria-label={t("canvas.distribuirVertical")}
                    disabled={deshabilitado}
                    onClick={() => distribuir("y")}
                  >
                    <IconoDistribuir eje="y" />
                  </BotonIcono>
                </>
              ) : null}
            </div>
          ) : null}
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
            <marker
              id="canvas-flecha-activa"
              viewBox="0 0 8 8"
              refX="7"
              refY="4"
              markerWidth="7"
              markerHeight="7"
              orient="auto-start-reverse"
            >
              <path d="M 0 0 L 8 4 L 0 8 z" fill="var(--color-violeta)" />
            </marker>
            <filter
              id="canvas-sombra-nodo"
              x="-20%"
              y="-20%"
              width="140%"
              height="160%"
            >
              <feDropShadow
                dx="0"
                dy="3"
                stdDeviation="5"
                floodColor="#1c1444"
                floodOpacity="0.14"
              />
            </filter>
            <filter
              id="canvas-sombra-activa"
              x="-25%"
              y="-25%"
              width="150%"
              height="170%"
            >
              <feDropShadow
                dx="0"
                dy="4"
                stdDeviation="7"
                floodColor="#6c36ff"
                floodOpacity="0.35"
              />
            </filter>
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
              const origen = grafo.nodos.find(
                (nodo) => nodo.id === arista.origen,
              );
              const destino = grafo.nodos.find(
                (nodo) => nodo.id === arista.destino,
              );
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
                    onClick={() => alSeleccionar({ tipo: "arista", id: clave })}
                  />
                  <path
                    d={trazo}
                    fill="none"
                    stroke={
                      activa
                        ? "var(--color-violeta)"
                        : "var(--color-borde-fuerte)"
                    }
                    strokeWidth={activa ? 2.5 : 1.75}
                    strokeLinecap="round"
                    markerEnd={
                      activa
                        ? "url(#canvas-flecha-activa)"
                        : "url(#canvas-flecha)"
                    }
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
              const activo = seleccionNodo === nodo.id || marcados.has(nodo.id);
              const conAviso = avisosNodo(nodo, grafo).length > 0;
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
                    if (evento.shiftKey && !deshabilitado) {
                      const siguiente = marcados.has(nodo.id)
                        ? [...marcados].filter((id) => id !== nodo.id)
                        : [...marcados, nodo.id];
                      marcar(siguiente);
                      return;
                    }
                    if (!marcados.has(nodo.id)) {
                      alSeleccionar({ tipo: "nodo", id: nodo.id });
                    }
                    svgRef.current?.setPointerCapture(evento.pointerId);
                    const punto = mundoDe(evento);
                    const grupo =
                      marcados.size > 1 && marcados.has(nodo.id)
                        ? [...marcados].map((id) => {
                            const miembro = grafo.nodos.find(
                              (actual) => actual.id === id,
                            );
                            const mp = miembro
                              ? posicion(miembro)
                              : { x: 0, y: 0 };
                            return { id, x: mp.x, y: mp.y };
                          })
                        : undefined;
                    setArrastre({
                      tipo: "nodo",
                      id: nodo.id,
                      dx: punto.x - p.x,
                      dy: punto.y - p.y,
                      grupo,
                      x0: punto.x,
                      y0: punto.y,
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
                    strokeWidth={activo ? 2.5 : destinoConexion ? 3 : 1.25}
                    strokeDasharray={destinoConexion ? "6 4" : undefined}
                    filter={
                      activo
                        ? "url(#canvas-sombra-activa)"
                        : "url(#canvas-sombra-nodo)"
                    }
                  />
                  <rect
                    x={1.5}
                    y={10}
                    width={4}
                    height={ALTO - 20}
                    rx={2}
                    fill={color.acento}
                    className="pointer-events-none"
                  />
                  <text
                    x={16}
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
                      : (nodo.nombre ?? nodo.tipo)}
                  </text>
                  <circle
                    cx={ANCHO - 24}
                    cy={ALTO / 2}
                    r={14}
                    fill={color.chipFondo}
                    stroke="none"
                  />
                  <IconoNodo
                    tipo={nodo.tipo}
                    x={ANCHO - 34}
                    y={ALTO / 2 - 10}
                    width={20}
                    height={20}
                    className={`${color.chipTexto} pointer-events-none`}
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
            {guias.vertical !== undefined ? (
              <line
                x1={guias.vertical}
                y1={-4000}
                x2={guias.vertical}
                y2={4000}
                stroke="var(--color-violeta)"
                strokeWidth={1}
                strokeDasharray="4 4"
                className="pointer-events-none"
              />
            ) : null}
            {guias.horizontal !== undefined ? (
              <line
                x1={-4000}
                y1={guias.horizontal}
                x2={4000}
                y2={guias.horizontal}
                stroke="var(--color-violeta)"
                strokeWidth={1}
                strokeDasharray="4 4"
                className="pointer-events-none"
              />
            ) : null}
            {arrastre?.tipo === "marquee" ? (
              <rect
                x={Math.min(arrastre.x0, arrastre.x)}
                y={Math.min(arrastre.y0, arrastre.y)}
                width={Math.abs(arrastre.x - arrastre.x0)}
                height={Math.abs(arrastre.y - arrastre.y0)}
                fill="var(--color-violeta-tenue)"
                stroke="var(--color-violeta)"
                strokeWidth={1}
                strokeDasharray="5 4"
                className="pointer-events-none"
              />
            ) : null}
          </g>
        </svg>
        {grafo.nodos.length === 0 ? (
          <div className="absolute inset-0 grid place-items-center bg-lienzo/80 p-espacio-6 text-center">
            <div className="space-y-espacio-3">
              <p className="text-pequeno text-tinta-suave">
                {soloLectura ? t("canvas.vacioLectura") : t("canvas.vacio")}
              </p>
              {!soloLectura && !deshabilitado ? (
                <Boton variante="primario" onClick={sembrarExtremos}>
                  {t("canvas.empezar")}
                </Boton>
              ) : null}
            </div>
          </div>
        ) : null}
        {minimapa ? (
          <svg
            aria-hidden="true"
            width={MINI_ANCHO}
            height={MINI_ALTO}
            className="absolute bottom-espacio-3 left-espacio-3 cursor-pointer rounded-panel border border-borde bg-superficie/90 shadow-panel"
            onPointerDown={(evento) => {
              const caja = evento.currentTarget.getBoundingClientRect();
              const mx = evento.clientX - caja.left;
              const my = evento.clientY - caja.top;
              const origen = grafo.nodos.length
                ? aMiniInversa(minimapa, mx, my)
                : null;
              if (origen) irAPuntoMundo(origen);
            }}
          >
            {grafo.aristas.map((arista) => {
              const origen = grafo.nodos.find((n) => n.id === arista.origen);
              const destino = grafo.nodos.find((n) => n.id === arista.destino);
              if (!origen || !destino) return null;
              const a = minimapa.aMini({
                x: posicion(origen).x + ANCHO / 2,
                y: posicion(origen).y + ALTO / 2,
              });
              const b = minimapa.aMini({
                x: posicion(destino).x + ANCHO / 2,
                y: posicion(destino).y + ALTO / 2,
              });
              return (
                <line
                  key={claveArista(arista)}
                  x1={a.x}
                  y1={a.y}
                  x2={b.x}
                  y2={b.y}
                  stroke="var(--color-borde-fuerte)"
                  strokeWidth={1}
                />
              );
            })}
            {grafo.nodos.map((nodo) => {
              const p = minimapa.aMini(posicion(nodo));
              return (
                <rect
                  key={nodo.id}
                  x={p.x}
                  y={p.y}
                  width={Math.max(4, ANCHO * minimapa.escala)}
                  height={Math.max(3, ALTO * minimapa.escala)}
                  rx={2}
                  fill={colorDe(nodo.tipo).relleno}
                  stroke={
                    seleccionNodo === nodo.id
                      ? "var(--color-violeta)"
                      : colorDe(nodo.tipo).borde
                  }
                  strokeWidth={seleccionNodo === nodo.id ? 1.5 : 0.8}
                />
              );
            })}
            {(() => {
              const caja = svgRef.current?.getBoundingClientRect();
              if (!caja) return null;
              const vp = minimapa.aMini({
                x: -vista.x / vista.k,
                y: -vista.y / vista.k,
              });
              return (
                <rect
                  x={vp.x}
                  y={vp.y}
                  width={(caja.width / vista.k) * minimapa.escala}
                  height={(caja.height / vista.k) * minimapa.escala}
                  fill="var(--color-violeta-tenue)"
                  fillOpacity={0.35}
                  stroke="var(--color-violeta)"
                  strokeWidth={1}
                  rx={2}
                  className="pointer-events-none"
                />
              );
            })()}
          </svg>
        ) : null}
        {aristaMarcada && posicionArista && !deshabilitado && !soloLectura ? (
          <div
            className="absolute z-10 flex -translate-x-1/2 -translate-y-1/2 items-center rounded-control border border-borde bg-superficie p-espacio-1 shadow-elevado"
            style={{
              left: posicionArista.x * vista.k + vista.x,
              top: posicionArista.y * vista.k + vista.y,
            }}
            onPointerDown={(evento) => evento.stopPropagation()}
          >
            <BotonIcono
              variante="fantasma"
              tamano="sm"
              aria-label={t("canvas.eliminarConexion")}
              title={t("canvas.eliminarConexion")}
              disabled={!alEliminarArista}
              onClick={() => alEliminarArista?.(seleccionArista!)}
            >
              <IconoEliminar tamano={14} />
            </BotonIcono>
          </div>
        ) : null}
        {nodoMarcado && posicionBarra && !deshabilitado && !soloLectura ? (
          <div
            className="absolute z-10 flex -translate-x-1/2 -translate-y-full items-center gap-espacio-1 rounded-control border border-borde bg-superficie p-espacio-1 shadow-elevado"
            style={{
              left:
                posicionBarra.x * vista.k + vista.x + (ANCHO * vista.k) / 2,
              top: posicionBarra.y * vista.k + vista.y - 8,
            }}
            onPointerDown={(evento) => evento.stopPropagation()}
          >
            <BotonIcono
              variante="fantasma"
              tamano="sm"
              aria-label={t("canvas.duplicarPaso")}
              title={t("canvas.duplicarPaso")}
              disabled={!alDuplicarNodo || esExtremoFijo}
              onClick={() => alDuplicarNodo?.(nodoMarcado.id)}
            >
              <IconoCopiar tamano={14} />
            </BotonIcono>
            <BotonIcono
              variante="fantasma"
              tamano="sm"
              aria-label={t("canvas.eliminarPaso")}
              title={t("canvas.eliminarPaso")}
              disabled={!alEliminarNodo || esExtremoFijo}
              onClick={() => alEliminarNodo?.(nodoMarcado.id)}
            >
              <IconoEliminar tamano={14} />
            </BotonIcono>
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
