import { test, expect } from "@playwright/test";
import type { GrafoProceso } from "../src/api/procesos";
import { disponerGrafo } from "../src/utilidades/disposicionGrafo";

function secuencial(): GrafoProceso {
  return {
    nodos: [
      { id: "inicio", tipo: "INICIO", nombre: "Inicio" },
      { id: "a", tipo: "FORMULARIO", nombre: "Cargar datos" },
      { id: "b", tipo: "REVISION_HUMANA", nombre: "Revisar" },
      { id: "fin", tipo: "FIN", nombre: "Fin" },
    ],
    aristas: [
      { origen: "inicio", destino: "a" },
      { origen: "a", destino: "b" },
      { origen: "b", destino: "fin" },
    ],
  };
}

function conRamas(): GrafoProceso {
  return {
    nodos: [
      { id: "inicio", tipo: "INICIO", nombre: "Inicio" },
      { id: "decidir", tipo: "DECISION", nombre: "Decidir" },
      { id: "aprobar", tipo: "REVISION_HUMANA", nombre: "Aprobar" },
      { id: "rechazar", tipo: "NOTIFICACION", nombre: "Avisar rechazo" },
      { id: "fin", tipo: "FIN", nombre: "Fin" },
    ],
    aristas: [
      { origen: "inicio", destino: "decidir" },
      { origen: "decidir", destino: "aprobar", condicion: "decision=APROBADO" },
      { origen: "decidir", destino: "rechazar", condicion: "decision=RECHAZADO" },
      { origen: "aprobar", destino: "fin" },
      { origen: "rechazar", destino: "fin" },
    ],
  };
}

test("un recorrido secuencial se dibuja en una sola fila", () => {
  const d = disponerGrafo(secuencial());

  expect(d.problema).toBeNull();
  expect(d.hayRamas).toBe(false);
  expect(d.filas).toBe(1);
  expect(d.columnas).toBe(4);
  expect(d.nodos.map((n) => n.columna)).toEqual([0, 1, 2, 3]);
});

test("una rama abre dos filas y conserva las condiciones", () => {
  const d = disponerGrafo(conRamas());

  expect(d.hayRamas).toBe(true);
  expect(d.filas).toBe(2);
  const aprobar = d.nodos.find((n) => n.id === "aprobar")!;
  const rechazar = d.nodos.find((n) => n.id === "rechazar")!;
  expect(aprobar.columna).toBe(rechazar.columna);
  expect(aprobar.fila).not.toBe(rechazar.fila);
  expect(d.aristas.filter((a) => a.condicion).map((a) => a.condicion)).toEqual([
    "decision=APROBADO",
    "decision=RECHAZADO",
  ]);
});

test("el fin queda siempre en la ultima columna aunque una rama sea mas corta", () => {
  const grafo = conRamas();
  grafo.nodos.push({ id: "extra", tipo: "FORMULARIO", nombre: "Paso extra" });
  grafo.aristas = grafo.aristas.filter((a) => a.origen !== "aprobar");
  grafo.aristas.push({ origen: "aprobar", destino: "extra" });
  grafo.aristas.push({ origen: "extra", destino: "fin" });

  const d = disponerGrafo(grafo);
  const fin = d.nodos.find((n) => n.id === "fin")!;

  expect(fin.columna).toBe(d.columnas - 1);
});

test("un ciclo no cuelga el dibujo y queda avisado", () => {
  const grafo = secuencial();
  grafo.aristas.push({ origen: "b", destino: "a" });

  const d = disponerGrafo(grafo);

  expect(d.hayCiclos).toBe(true);
  expect(d.problema).toBe("diagramaProceso.problemaCiclo");
  expect(d.nodos.length).toBe(4);
  expect(d.aristas.some((a) => a.haciaAtras)).toBe(true);
});

test("una arista que apunta a un paso inexistente se ignora", () => {
  const grafo = secuencial();
  grafo.aristas.push({ origen: "b", destino: "fantasma" });

  const d = disponerGrafo(grafo);

  expect(d.aristas).toHaveLength(3);
  expect(d.nodos).toHaveLength(4);
});

test("un grafo vacio no rompe ni inventa problemas", () => {
  const d = disponerGrafo({ nodos: [], aristas: [] });

  expect(d.nodos).toHaveLength(0);
  expect(d.problema).toBeNull();
});

test("un grafo con formato invalido lo dice en vez de romper", () => {
  const d = disponerGrafo(undefined);

  expect(d.problema).toBe("diagramaProceso.problemaFormato");
});

test("pasos repetidos se reportan en vez de dibujarse mal", () => {
  const grafo = secuencial();
  grafo.nodos.push({ id: "a", tipo: "FORMULARIO", nombre: "Duplicado" });

  const d = disponerGrafo(grafo);

  expect(d.problema).toBe("diagramaProceso.problemaRepetidos");
  expect(d.nodos).toHaveLength(0);
});
