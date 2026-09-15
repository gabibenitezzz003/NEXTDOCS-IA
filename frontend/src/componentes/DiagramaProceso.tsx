import type { GrafoProceso, TipoNodoProceso } from "../api/procesos";
import { disponerGrafo } from "../utilidades/disposicionGrafo";
import { Vacio } from "./Estados";

const ANCHO = 176;
const ALTO = 60;
const SEPARACION_X = 72;
const SEPARACION_Y = 28;
const MARGEN = 16;

const ETIQUETA_TIPO: Record<TipoNodoProceso, string> = {
  INICIO: "Inicio",
  FIN: "Fin",
  SOLICITUD_DOCUMENTO: "Documento",
  FORMULARIO: "Formulario",
  VALIDACION_IA: "Validación IA",
  REVISION_HUMANA: "Revisión",
  DECISION: "Decisión",
  TAREA_EXTERNA: "Tarea externa",
  NOTIFICACION: "Notificación",
  TEMPORIZADOR: "Espera",
  ACCION_API: "Acción de API",
  SUBPROCESO: "Subproceso",
};

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

export function DiagramaProceso({
  grafo,
  titulo = "Diagrama del recorrido",
}: {
  grafo: GrafoProceso | undefined;
  titulo?: string;
}) {
  const disposicion = disponerGrafo(grafo);

  if (!disposicion.nodos.length) {
    return (
      <Vacio
        titulo="Todavía no hay pasos que dibujar"
        detalle={
          disposicion.problema ??
          "Agregá pasos al recorrido y el diagrama se arma solo."
        }
      />
    );
  }

  const posicion = (columna: number, fila: number) => ({
    x: MARGEN + columna * (ANCHO + SEPARACION_X),
    y: MARGEN + fila * (ALTO + SEPARACION_Y),
  });

  const porId = new Map(disposicion.nodos.map((nodo) => [nodo.id, nodo]));
  const ancho =
    MARGEN * 2 + disposicion.columnas * ANCHO + (disposicion.columnas - 1) * SEPARACION_X;
  const alto =
    MARGEN * 2 + disposicion.filas * ALTO + (disposicion.filas - 1) * SEPARACION_Y;

  return (
    <div className="min-w-0">
      {disposicion.problema ? (
        <p
          role="note"
          className="mb-espacio-3 rounded-control border border-alerta-borde bg-alerta-tenue px-espacio-3 py-espacio-2 text-pequeno text-alerta-texto"
        >
          {disposicion.problema}
        </p>
      ) : null}
      <div className="overflow-auto rounded-panel border border-borde bg-lienzo p-espacio-2">
        <svg
          role="img"
          aria-label={`${titulo}: ${disposicion.nodos.length} pasos`}
          viewBox={`0 0 ${ancho} ${alto}`}
          width={ancho}
          height={alto}
          className="max-w-none"
        >
          <defs>
            <marker
              id="punta-flecha"
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

          {disposicion.aristas.map((arista, indice) => {
            const desde = porId.get(arista.origen);
            const hasta = porId.get(arista.destino);
            if (!desde || !hasta) return null;
            const a = posicion(desde.columna, desde.fila);
            const b = posicion(hasta.columna, hasta.fila);
            const x1 = a.x + ANCHO;
            const y1 = a.y + ALTO / 2;
            const x2 = b.x;
            const y2 = b.y + ALTO / 2;
            const control = Math.max(24, (x2 - x1) / 2);
            const trazo = arista.haciaAtras
              ? `M ${x1} ${y1} C ${x1 + 40} ${y1 - 40}, ${x2 - 40} ${y2 - 40}, ${x2} ${y2}`
              : `M ${x1} ${y1} C ${x1 + control} ${y1}, ${x2 - control} ${y2}, ${x2} ${y2}`;
            return (
              <g key={`${arista.origen}-${arista.destino}-${indice}`}>
                <path
                  d={trazo}
                  fill="none"
                  stroke="var(--color-borde-fuerte)"
                  strokeWidth={1.5}
                  strokeDasharray={arista.haciaAtras ? "4 3" : undefined}
                  markerEnd="url(#punta-flecha)"
                />
                {arista.condicion ? (
                  <text
                    x={(x1 + x2) / 2}
                    y={(y1 + y2) / 2 - 6}
                    textAnchor="middle"
                    className="fill-[var(--color-tinta-suave)] text-[10px]"
                  >
                    {arista.condicion}
                  </text>
                ) : null}
              </g>
            );
          })}

          {disposicion.nodos.map((nodo, indice) => {
            const { x, y } = posicion(nodo.columna, nodo.fila);
            const color = colorDe(nodo.tipo);
            return (
              <g key={nodo.id}>
                <rect
                  x={x}
                  y={y}
                  width={ANCHO}
                  height={ALTO}
                  rx={10}
                  fill={color.relleno}
                  stroke={color.borde}
                  strokeWidth={1.5}
                />
                <text
                  x={x + 12}
                  y={y + 21}
                  className="fill-[var(--color-tinta-suave)] text-[9px] uppercase"
                  style={{ letterSpacing: "0.4px" }}
                >
                  {String(indice + 1).padStart(2, "0")} · {ETIQUETA_TIPO[nodo.tipo]}
                </text>
                <text
                  x={x + 12}
                  y={y + 41}
                  className="fill-[var(--color-tinta)] text-[13px] font-semibold"
                >
                  {nodo.nombre.length > 22
                    ? nodo.nombre.slice(0, 21) + "…"
                    : nodo.nombre}
                </text>
              </g>
            );
          })}
        </svg>
      </div>
      <p className="mt-espacio-2 text-pequeno text-tinta-suave">
        {disposicion.nodos.length} pasos
        {disposicion.hayRamas ? " · el recorrido tiene ramas" : ""}
      </p>
    </div>
  );
}
