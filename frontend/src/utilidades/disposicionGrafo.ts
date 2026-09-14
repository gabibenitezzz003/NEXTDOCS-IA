import type { GrafoProceso, TipoNodoProceso } from "../api/procesos";

export interface NodoDispuesto {
  id: string;
  tipo: TipoNodoProceso;
  nombre: string;
  columna: number;
  fila: number;
}

export interface AristaDispuesta {
  origen: string;
  destino: string;
  condicion?: string;
  haciaAtras: boolean;
}

export interface Disposicion {
  nodos: NodoDispuesto[];
  aristas: AristaDispuesta[];
  columnas: number;
  filas: number;
  hayRamas: boolean;
  hayCiclos: boolean;
  problema: string | null;
}

const VACIA: Disposicion = {
  nodos: [],
  aristas: [],
  columnas: 0,
  filas: 0,
  hayRamas: false,
  hayCiclos: false,
  problema: null,
};

function sinDatos(problema: string): Disposicion {
  return { ...VACIA, problema };
}

export function disponerGrafo(grafo: GrafoProceso | undefined): Disposicion {
  if (!grafo || !Array.isArray(grafo.nodos) || !Array.isArray(grafo.aristas)) {
    return sinDatos("El recorrido no tiene un formato que se pueda dibujar.");
  }
  if (!grafo.nodos.length) return VACIA;

  const porId = new Map(grafo.nodos.map((nodo) => [nodo.id, nodo]));
  if (porId.size !== grafo.nodos.length) {
    return sinDatos("El recorrido tiene pasos repetidos.");
  }

  const aristas = grafo.aristas.filter(
    (arista) => porId.has(arista.origen) && porId.has(arista.destino),
  );

  const entrantes = new Map<string, string[]>();
  const salientes = new Map<string, string[]>();
  for (const nodo of grafo.nodos) {
    entrantes.set(nodo.id, []);
    salientes.set(nodo.id, []);
  }
  for (const arista of aristas) {
    salientes.get(arista.origen)!.push(arista.destino);
    entrantes.get(arista.destino)!.push(arista.origen);
  }

  const nivel = new Map<string, number>();
  for (const nodo of grafo.nodos) nivel.set(nodo.id, 0);

  let cambio = true;
  let vueltas = 0;
  const tope = grafo.nodos.length + 1;
  while (cambio && vueltas <= tope) {
    cambio = false;
    vueltas++;
    for (const arista of aristas) {
      const propuesto = nivel.get(arista.origen)! + 1;
      if (propuesto > nivel.get(arista.destino)!) {
        nivel.set(arista.destino, propuesto);
        cambio = true;
      }
    }
  }
  const hayCiclos = cambio;

  const inicio = grafo.nodos.find((nodo) => nodo.tipo === "INICIO");
  if (inicio) nivel.set(inicio.id, 0);
  const fin = grafo.nodos.find((nodo) => nodo.tipo === "FIN");
  if (fin && !hayCiclos) {
    const maximo = Math.max(...grafo.nodos.map((nodo) => nivel.get(nodo.id)!));
    nivel.set(fin.id, maximo);
  }

  const porColumna = new Map<number, string[]>();
  for (const nodo of grafo.nodos) {
    const columna = nivel.get(nodo.id)!;
    if (!porColumna.has(columna)) porColumna.set(columna, []);
    porColumna.get(columna)!.push(nodo.id);
  }

  const nodos: NodoDispuesto[] = [];
  for (const [columna, ids] of [...porColumna.entries()].sort((a, b) => a[0] - b[0])) {
    ids.forEach((id, fila) => {
      const nodo = porId.get(id)!;
      nodos.push({
        id,
        tipo: nodo.tipo,
        nombre: nodo.nombre?.trim() || nodo.tipo,
        columna,
        fila,
      });
    });
  }

  const hayRamas = grafo.nodos.some(
    (nodo) =>
      (salientes.get(nodo.id)?.length ?? 0) > 1 ||
      (entrantes.get(nodo.id)?.length ?? 0) > 1,
  );

  return {
    nodos,
    aristas: aristas.map((arista) => ({
      origen: arista.origen,
      destino: arista.destino,
      condicion: arista.condicion,
      haciaAtras: nivel.get(arista.destino)! <= nivel.get(arista.origen)!,
    })),
    columnas: Math.max(...nodos.map((nodo) => nodo.columna)) + 1,
    filas: Math.max(...[...porColumna.values()].map((ids) => ids.length)),
    hayRamas,
    hayCiclos,
    problema: hayCiclos
      ? "El recorrido tiene un ciclo. El diagrama lo dibuja igual, pero el orden de los pasos es aproximado."
      : null,
  };
}
