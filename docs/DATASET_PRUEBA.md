# Dataset de prueba y carga masiva

Dos scripts para cerrar el punto 3 de [P0-02](P0_02_VALIDACION.md): generar un conjunto de
documentos con respuesta conocida y cargarlo midiendo qué detectó el sistema.

Los PDF no se versionan (`dataset-prueba/` está ignorado): el generador es determinista, así que
se rehacen cuando hagan falta.

## 1. Generar

```bash
node scripts/generar-dataset.mjs dataset-prueba
```

Produce 14 documentos argentinos en PDF y un `esperado.json` con el tipo que le corresponde a
cada uno. Usa Chromium vía Playwright, que ya está instalado en `frontend/`.

| Documentos | Para qué están |
|---|---|
| Factura A, B y C | Tres formatos fiscales distintos del mismo tipo |
| Nota de crédito y nota de débito | Se parecen mucho a una factura: es donde el clasificador se puede confundir |
| Remito conformado y sin conformar | El mismo tipo con y sin firma de recepción |
| Constancia de inscripción AFIP | Tipo fiscal sin importes |
| Certificado VTV, póliza de seguro y cédula vehicular | Los tres son vehiculares y comparten dominio y CUIT |
| Contrato de locación | **No existe en el catálogo**: tiene que caer al esquema genérico |
| Factura con CUIT inválido | Tiene que disparar el hallazgo `CUIT_INVALIDO` |
| Factura con fecha futura | Tiene que disparar el hallazgo `FECHA_FUTURA` |

Los datos son ficticios pero con formato válido: los CUIT tienen el dígito verificador bien
calculado (salvo el que está mal a propósito) y los totales cierran con el IVA.

Se le puede sumar documentación real copiándola a la misma carpeta y agregando su tipo esperado
al `esperado.json`. Para probar el camino genérico con un PDF real:

```bash
curl -sL -o dataset-prueba/afip-manual-constancia.pdf \
  https://www.afip.gob.ar/ws/WSCI/manual-ws-sr-ws-constancia-inscripcion.pdf
```

## 2. Cargar

```bash
export NEXTDOCS_URL=http://localhost:8090
export NEXTDOCS_TENANT=<codigo de la organizacion>
export NEXTDOCS_EMAIL=<usuario>
export NEXTDOCS_CLAVE=<clave>

node scripts/cargar-dataset.mjs dataset-prueba --informe informe-dataset.md
```

Sube cada archivo, espera a que termine de procesarse y arma una tabla con el tipo detectado, el
origen del tipo, la confianza, cuántos campos se extrajeron con valor y cuántos hallazgos se
abrieron. Cuando hay `esperado.json`, calcula el porcentaje de acierto.

Las credenciales van por variable de entorno y el script no las escribe a ningún lado. Sale con
código 1 si algún documento no se pudo cargar, así sirve en CI.

## Qué mirar en el informe

- **Acierto por tipo.** Confundir una nota de crédito con una factura es el error esperable.
- **Cuántos cayeron al genérico.** Solo el contrato debería caer ahí. Si caen facturas, el
  umbral de confianza del clasificador está demasiado alto para estos formatos.
- **Campos con valor sobre campos totales.** Mide si la extracción sirve o devuelve vacíos.
- **Los dos hallazgos plantados.** Si el CUIT inválido y la fecha futura no aparecen, las reglas
  de validación no se están aplicando.

## Sobre documentos de identidad

El catálogo incluye DNI y licencia de conducir. El dataset **no** los trae: los ejemplares reales
que circulan por internet son datos personales de gente real, y un PDF armado por nosotros no se
parece en nada a la foto de un documento, así que mediría el parecido de nuestra maqueta y no la
capacidad del clasificador. Esos dos tipos hay que probarlos con documentación real del cliente,
con su consentimiento.
