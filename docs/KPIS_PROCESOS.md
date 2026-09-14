# KPIs de procesos en la bandeja de instancias

El servicio workflow expone los KPIs desde antes de R4 (`KpiProcesoService`), pero ningún frontend los consumía. R4 los hace visibles arriba de la bandeja de instancias, en `/procesos` → pestaña Instancias.

## La ventana se manda como desde/hasta, no como días

El selector de la interfaz ofrece 7, 30 y 90 días, pero el endpoint no acepta un parámetro `dias`: `KpiProcesoRestController.resumen` recibe `desde` y `hasta` como `Instant` opcionales. La función `ventana(dias)` de `src/api/procesos.ts` traduce los días elegidos a ese par y manda ambos en ISO-8601 con `toISOString()`.

Mandar los dos siempre importa más de lo que parece: `throughputProceso` se calcula como instancias completadas sobre las horas de la ventana y devuelve `null` si falta cualquiera de los dos extremos. Con una sola punta el indicador queda sin dato.

## /poblacion exige indicador

`GET /api/v1/kpi-procesos/poblacion` tiene `indicador` como parámetro obligatorio. Omitirlo hoy devuelve **500**, no 400, porque la excepción de binding no está mapeada; el cliente siempre lo manda, así que no nos afecta, pero conviene saberlo antes de pegarle a mano.

Para los cuellos de botella se usa `tareasCompletadas` (constante `INDICADOR_CUELLOS`): es la población que trae `nodoId` y `duracionMinutos` calculada sobre tareas ya cerradas, que es exactamente lo que necesita la tabla.

## Los cuellos de botella se agrupan en el cliente sobre 200 filas como mucho

`KpiProcesoService.poblacion` corta la lista con `.limit(200)`. El promedio por paso se calcula sobre esa muestra, no sobre el total del período: con más de doscientas tareas completadas en la ventana, la tabla describe las primeras doscientas que devuelve el repositorio y no el universo completo.

Es aceptable para leer tendencias y detectar el paso lento, no para reportar un promedio exacto. Si en algún momento hace falta precisión, la agrupación tiene que bajar al backend como una consulta agregada.

## Por qué seis indicadores y no los catorce

`resumir` devuelve catorce indicadores. Poner los catorce arriba de la bandeja los vuelve ruido, así que la franja muestra seis, elegidos por su utilidad operativa en esta pantalla: `procesosActivos`, `tareasPendientes`, `tareasVencidas`, `slaProceso`, `tiempoCicloP50` e `instanciasCompletadas`. La lista vive en `INDICADORES_FRANJA` y el filtrado es por código, así que un indicador que el backend deje de mandar simplemente no se dibuja.

Los otros ocho siguen disponibles en la respuesta y cada uno trae su `drilldown` armado.

## Valores nulos

Varios indicadores devuelven `null` legítimamente: `slaProceso` cuando no hay tareas pendientes (división por cero), los percentiles cuando no hay muestras, `throughputProceso` sin ventana completa. La franja los dibuja como `—` con el detalle "Sin datos en la ventana" en vez de mostrar un cero que se leería como un dato real.

## Sin polling

Las bandejas de instancias y tareas refrescan cada 15 segundos. Los KPIs no: se consultan al montar y al cambiar el período. Son agregados de ventanas de días, no cambian de un segundo a otro, y cada consulta recorre instancias y tareas del tenant en memoria.

Al cambiar de período las consultas mantienen el dato anterior (`placeholderData`) mientras llega el nuevo, así la franja no parpadea a esqueletos ni repite la animación de entrada en cada toque del selector.
