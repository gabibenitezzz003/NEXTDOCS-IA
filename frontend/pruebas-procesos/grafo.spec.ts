import { test, expect } from "@playwright/test";
import type { GrafoProceso } from "../src/api/procesos";
import { inspeccionarGrafo, serializarGrafo } from "../src/utilidades/grafoProceso";

function lineal(cantidad: number): GrafoProceso {
  const nodos: GrafoProceso["nodos"] = [
    {
      id: "entrada-real",
      tipo: "INICIO",
      configuracion: { origen: ["uno", "dos"] },
    },
    ...Array.from({ length: cantidad }, (_, indice) => ({
      id: `paso-${indice}`,
      tipo: "FORMULARIO" as const,
      nombre: `Paso ${indice}`,
    })),
    { id: "fin-real", tipo: "FIN", configuracion: { retener: true } },
  ];
  return {
    nodos,
    aristas: nodos.slice(1).map((nodo, indice) => ({ origen: nodos[indice].id, destino: nodo.id })),
  };
}

for (const cantidad of [1, 2, 105]) {
  test(`conserva exactamente una cadena de ${cantidad} pasos`, () => {
    const grafo = lineal(cantidad);
    const analisis = inspeccionarGrafo(grafo);
    expect(analisis.bloqueo).toBeNull();
    expect(analisis.pasos).toHaveLength(cantidad);
    expect(serializarGrafo(grafo, analisis.pasos)).toEqual(grafo);
  });
}

test("preserva configuración desconocida anidada e identidad al editar", () => {
  const grafo = lineal(2);
  grafo.nodos[1].configuracion = {
    identificadores: ["doc-1"],
    adicional: { a: false, b: null },
    slaHoras: 5,
  };
  const original = structuredClone(grafo);
  const pasos = inspeccionarGrafo(grafo).pasos;
  pasos[0].nombre = "Editado";
  pasos[0].slaHoras = 8;
  const resultado = serializarGrafo(grafo, pasos);
  const esperado = structuredClone(original);
  esperado.nodos[1].nombre = "Editado";
  esperado.nodos[1].configuracion!.slaHoras = 8;
  expect(resultado).toEqual(esperado);
  expect(grafo).toEqual(original);
});

for (const caso of [
  "decisión",
  "condición",
  "ramas",
  "finales",
  "ciclo",
  "desconectado",
  "id duplicado",
]) {
  test(`bloquea ${caso} incluso si se invoca directamente el serializador`, () => {
    const grafo = lineal(2);
    if (caso === "decisión") {
      grafo.nodos[1].tipo = "DECISION";
      grafo.aristas.push({
        origen: "paso-0",
        destino: "fin-real",
        condicion: "decision=RECHAZADO",
      });
    }
    if (caso === "condición") grafo.aristas[0].condicion = "decision=APROBADO";
    if (caso === "ramas") grafo.aristas.push({ origen: "paso-0", destino: "fin-real" });
    if (caso === "finales") grafo.nodos.push({ id: "otro-fin", tipo: "FIN" });
    if (caso === "ciclo") grafo.aristas.push({ origen: "paso-1", destino: "paso-0" });
    if (caso === "desconectado") grafo.nodos.push({ id: "otro", tipo: "FORMULARIO" });
    if (caso === "id duplicado") grafo.nodos[2].id = grafo.nodos[1].id;
    const original = structuredClone(grafo);
    expect(inspeccionarGrafo(grafo).bloqueo).toBeTruthy();
    expect(() => serializarGrafo(grafo, [])).toThrow("procesos.bloqueoSecuencial");
    expect(grafo).toEqual(original);
  });
}

test("permite crear desde grafo vacío y reordenar preservando nodos originales", () => {
  expect(
    serializarGrafo({ nodos: [], aristas: [] }, [
      { id: "a", tipo: "FORMULARIO", nombre: "A" },
    ]).nodos.map((n) => n.id),
  ).toEqual(["inicio", "a", "fin"]);
  const grafo = lineal(2);
  const pasos = inspeccionarGrafo(grafo).pasos.reverse();
  const resultado = serializarGrafo(grafo, pasos);
  expect(resultado.nodos).toEqual([grafo.nodos[0], grafo.nodos[2], grafo.nodos[1], grafo.nodos[3]]);
  expect(resultado.aristas).toEqual([
    { origen: "entrada-real", destino: "paso-1" },
    { origen: "paso-1", destino: "paso-0" },
    { origen: "paso-0", destino: "fin-real" },
  ]);
});
