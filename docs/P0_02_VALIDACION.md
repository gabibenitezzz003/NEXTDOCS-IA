# P0-02 · Validación del MVP0 técnico

Los cuatro puntos que `ESTADO_MVP0.md` dejó abiertos, y en qué estado quedó cada uno.

## 1. Recorrido punta a punta · cubierto

`pruebas-transversales/recorrido.spec.ts` recorre entrar → subir → ver el tipo detectado →
abrir el visor → corregir un campo → aprobar con la corrección → resolver una excepción →
mirar el panel, y deja una captura numerada de cada paso en `capturas-recorrido/`
(carpeta ignorada por git; se regenera corriendo la prueba).

```bash
cd frontend && npx playwright test --config playwright.transversales.config.ts recorrido
```

No reemplaza mirar la aplicación con datos reales, pero convierte la lista de chequeo manual
en algo repetible que además falla si una de esas pantallas se rompe. El recorrido verifica el
cuerpo real de las peticiones, no solo que la pantalla no explote: la corrección viaja dentro
del `POST /documentos/{id}/revisiones` junto con la decisión, y la resolución de la excepción
viaja en el `POST /excepciones/{id}/resolver`.

También afirma que no hubo errores de consola, errores de página ni llamadas a endpoints
inesperados durante todo el recorrido.

## 2. El visor muestra los datos de la captura genérica · confirmado

Era la duda con más riesgo: los datos se guardan (había prueba), pero nadie había confirmado
que el visor los mostrara. **Los muestra.** Está cubierto en
`pruebas-transversales/captura-generica.spec.ts`:

- los 7 campos del esquema genérico se listan con su etiqueta, su clave y su valor;
- un campo ausente se ve vacío, no roto;
- el visor marca el documento con una pastilla `Captura genérica` y una nota que explica por
  qué cayó al esquema de respaldo, incluyendo el motivo que dejó el clasificador;
- la bandeja también marca esos documentos, así se distinguen sin abrirlos;
- un campo capturado con el esquema genérico se puede corregir y la corrección viaja con la
  decisión;
- un documento genérico sin campos dice "Sin campos extraídos" en lugar de aparentar estar roto.

El camino es el esperado: `capturarGenerico` asigna la plantilla `GENERICO` y la extracción
sigue su curso normal contra los 9 campos de ese esquema. El visor pinta
`extraccion.valores`, sin mirar de qué plantilla vienen, así que no hay nada específico del
tipo que pueda fallar.

Verifiqué que las pruebas discriminan: anulando la condición `origenTipo === "GENERICO"` en el
visor fallan exactamente las dos que miran el aviso, y siguen pasando la de la bandeja (vive en
otro archivo) y las de campos, que no dependen del aviso.

## 3. Cargar 10–15 documentos reales · pendiente, necesita los documentos

Es el único punto que no se puede cerrar desde el repositorio: hace falta documentación real de
un cliente, de tipos variados. Es también el que más valor tiene, porque es lo único que va a
decir si la clasificación automática y la extracción sirven de verdad.

## 4. Rotar la clave de Gemini · pendiente, acción manual

La clave sigue siendo la que se compartió durante el desarrollo. Nunca entró al repositorio
(está verificado en cada commit), pero hay que rotarla antes de que esto toque datos de un
cliente.

## Un detalle que apareció y no es un error

En el panel, el número grande del indicador destacado se anima desde 0 con
`requestAnimationFrame`. Una captura tomada durante esos 900 ms muestra `0` aunque el valor sea
otro. No es un defecto del producto: el contador respeta `prefers-reduced-motion`, así que el
recorrido emula ese modo y captura el valor final. Si alguien saca capturas del panel a mano,
conviene saberlo antes de reportar un cero que no existe.
