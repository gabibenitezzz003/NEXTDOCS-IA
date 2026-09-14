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

## 3. Cargar 10–15 documentos · hecho, con Gemini real

Están los dos scripts que hacen falta, documentados en [DATASET_PRUEBA.md](DATASET_PRUEBA.md):

- `scripts/generar-dataset.mjs` arma 14 documentos argentinos en PDF con **respuesta conocida**
  (`esperado.json`), incluyendo dos con defectos plantados para ver si las reglas de validación
  se aplican, y un contrato que no está en el catálogo para ejercitar el esquema genérico;
- `scripts/cargar-dataset.mjs` los sube y arma un informe con el tipo detectado, la confianza,
  los campos extraídos y los hallazgos abiertos, midiendo el acierto contra lo esperado.

### Resultado de la corrida

15 documentos cargados sobre el tenant `demo`, con el proveedor **GEMINI** real
(`gemini-2.5-flash`, confianza 0.95–1.00). **15/15 cargados y 15/15 con el tipo correcto.**

El informe marcó 14/15 porque yo había anotado que el contrato de locación debía caer al esquema
genérico. Estaba mal: el tenant tiene 16 plantillas, entre ellas `CONTRATO_DE_LOCACION`, así que
el sistema lo clasificó bien y la respuesta esperada era mía y estaba equivocada.

El manual de AFIP de 32 páginas —un PDF real, no generado— cayó al esquema genérico como
correspondía y extrajo 5 de 9 campos. Es la confirmación del punto 2 sobre un documento real.

### Lo que la corrida no pudo comprobar

Los dos defectos plantados (CUIT con dígito verificador mal calculado y factura con fecha de
emisión futura) **no abrieron hallazgo**. La causa no es el motor: las plantillas del tenant
`demo` se cargaron a mano por API y no tienen esas reglas. La única regla que disparó fue
`MONEDA_VALIDA`, que no existe en el código y confirma que ese catálogo es data, no semilla.

Para que los defectos plantados sirvan de medición hace falta un tenant sembrado desde
`CatalogoDocumentalBase`, que sí declara `fechaNoFutura` y `cuitValido` sobre la factura.

Los documentos generados tienen formato válido pero datos ficticios. **No reemplazan
documentación real de un cliente**, que sigue siendo lo que va a decir si la detección sirve de
verdad; sirven como piso medible mientras tanto. DNI y licencia de conducir quedan fuera a
propósito, por el motivo que explica `DATASET_PRUEBA.md`.

## Lo que apareció buscando eso: la validación de CUIT no existía

Persiguiendo por qué el CUIT inválido no abría hallazgo apareció un defecto real, independiente
del tenant.

`CatalogoDocumentalBase` declara la regla `CUIT_INVALIDO` ("CUIT con digito verificador
invalido") como una regla de tipo `FORMATO` sobre cuatro plantillas: factura —y por herencia
nota de crédito y nota de débito—, constancia de CUIT, DNI y remito.

Pero `FORMATO` está implementado como una comparación contra una expresión regular, y
`cumpleFormato` devuelve `true` cuando no hay expresión configurada. El record `ReglaBase` del
catálogo **no tiene campo de configuración**, así que toda regla sembrada desde el código nace
con `configuracion = null`. No había, además, ninguna función que calculara un dígito
verificador en todo el backend.

Resultado: el producto decía validar el CUIT y no lo validaba. Una factura con un CUIT
inventado pasaba a `VALIDADO` y podía autoaprobarse.

El arreglo agrega `utiles/ValidadorCuit`, con el cálculo de módulo 11 y los dos casos borde del
dígito verificador (resto 11 → 0, resto 10 → 9), y hace que la regla `CUIT_INVALIDO` lo use en
lugar de caer en la rama de expresión regular. Van 20 casos de prueba.

No pude demostrarlo de punta a punta contra el entorno porque el tenant `demo` no usa el
catálogo del código. Queda cubierto por pruebas unitarias.

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
