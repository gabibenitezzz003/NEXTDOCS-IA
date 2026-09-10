# Corpus de prueba para certificación E01 (P0-02)

Directorio: `docs/verificaciones/corpus/`

Conjunto de documentos reales de dominio público o de muestra oficial de proveedores de Document AI, usados para el recorrido humano de certificación del core documental. No contienen datos personales reales.

## Fuentes

- Google Cloud Document AI sample documents (`gs://cloud-samples-data/documentai/`). Uso permitido para pruebas y desarrollo según los términos de Google Cloud.
- Formularios del IRS de Estados Unidos (`https://www.irs.gov/pub/irs-pdf/`), dominio público del gobierno federal.

## Archivos

| Nombre | Tipo | Fuente | Páginas | Uso previsto |
|---|---|---|---|---|
| `google_invoice.pdf` | Factura comercial | Google Cloud Document AI | 1 | Clasificación + extracción de entidades |
| `procurement_multi_document.pdf` | Documento de adquisición | Google Cloud Document AI | 1 | Clasificación multi-documento |
| `CymbalContract.pdf` | Contrato | Google Cloud Document AI | 2 | Extracción de partes, plazos, montos |
| `license.pdf` | Licencia | Google Cloud Document AI | 1 | Clasificación y extracción de campos |
| `lending_multi_document.pdf` | Documento de préstamo | Google Cloud Document AI | 10 | Clasificación + segmentación |
| `W9.pdf` | Formulario fiscal W-9 | Google Cloud Document AI | 1 | Formulario estructurado |
| `expense_receipt.pdf` | Recibo de gastos | Google Cloud Document AI | 1 | Extracción de líneas y totales |
| `bank_statement.pdf` | Estado de cuenta | Google Cloud Document AI | 4 | Tablas y transacciones |
| `utility_bill.pdf` | Factura de servicios | Google Cloud Document AI | 1 | Extracción de vencimiento, totales |
| `IRS_W2.pdf` | Formulario W-2 del IRS | IRS (dominio público) | varias | Formulario estructurado |
| `IRS_1099.pdf` | Formulario 1099-MISC del IRS | IRS (dominio público) | varias | Formulario estructurado |
| `IRS_1040.pdf` | Formulario 1040 del IRS | IRS (dominio público) | varias | Formulario estructurado |

## Observaciones

- Los documentos están en inglés porque son los conjuntos de muestra públicos más accesibles y de licencia clara. Para MVP0 comercial en español se recomienda reemplazar o complementar con facturas/comprobantes argentinos una vez que se disponga de autorización de los emisores.
- Ninguno de los archivos incluye datos personales identificables reales; los ejemplos de Google usan datos ficticios y los formularios IRS son plantillas oficiales.
- El corpus cubre facturas, contratos, formularios, recibos, estados de cuenta y documentos de préstamo: suficiente para probar clasificación, extracción, segmentación y visor.

## Próximo paso

Usar este corpus en el recorrido E01: levantar infra, cargar cada documento por UI o API, revisar que la extracción y validación se completan, y comparar resultados contra expectativas.
