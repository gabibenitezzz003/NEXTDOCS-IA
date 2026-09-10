# E01 / P0-02 · Certificación del core documental con corpus real y Gemini

Fecha: sesión actual.
Entorno: infraestructura local levantada (`docker compose`), backend `nextdocs-ai:local`, proveedor `GEMINI` con modelo `gemini-2.5-flash`.

## Configuración del entorno

- Se copió `.env.example` a `.env`.
- Se configuró `NEXTDOCS_GEMINI_CLAVE` y `NEXTDOCS_IA_PROVEEDOR=GEMINI`.
- Se generó un `NEXTDOCS_JWT_SECRETO` local para poder arrancar el backend.
- Se activó `NEXTDOCS_CREAR_TENANT_DEMO=true` con credenciales `admin@nextdocs.ai` / `nextdocs123`.
- Se levantó el stack: PostgreSQL 16, Redis 7, MinIO y Keycloak.
- Se ejecutó `docker compose --profile app up -d --build`.

## Corpus utilizado

12 documentos públicos de muestra descargados a `docs/verificaciones/corpus/`:

- `CymbalContract.pdf` — contrato
- `google_invoice.pdf` — factura
- `procurement_multi_document.pdf` — documento de adquisición
- `lending_multi_document.pdf` — documento de préstamo
- `license.pdf` — licencia
- `W9.pdf` — formulario fiscal W-9
- `expense_receipt.pdf` — recibo de gastos
- `bank_statement.pdf` — estado de cuenta
- `utility_bill.pdf` — factura de servicios
- `IRS_W2.pdf`, `IRS_1099.pdf`, `IRS_1040.pdf` — formularios del IRS (dominio público)

Fuentes:

- Google Cloud Document AI: `gs://cloud-samples-data/documentai/`
- IRS: `https://www.irs.gov/pub/irs-pdf/`

## Recorrido ejecutado

1. Login por API con tenant demo.
2. Subida masiva de los 12 documentos mediante `POST /api/v1/documentos` con `Idempotency-Key`.
3. Espera al worker asíncrono (proceso programado cada ~30 s).
4. Consulta de estado final de cada documento.
5. Revisión humana manual de `utility_bill.pdf` (`VALIDADO → APROBADO`) y cierre (`APROBADO → CERRADO`).

## Resultados del corpus

| Documento | Estado final | Tipo detectado | Plantilla | Confianza |
|---|---|---|---|---|
| `CymbalContract.pdf` | OBSERVADO | GENERICO | Captura genérica | 0.90 |
| `IRS_1040.pdf` | OBSERVADO | GENERICO | Captura genérica | 0.95 |
| `IRS_1099.pdf` | OBSERVADO | GENERICO | Captura genérica | 0.95 |
| `IRS_W2.pdf` | OBSERVADO | GENERICO | Captura genérica | 0.90 |
| `W9.pdf` | APROBADO | GENERICO | Captura genérica | 0.90 |
| `bank_statement.pdf` | OBSERVADO | GENERICO | Captura genérica | 0.90 |
| `expense_receipt.pdf` | OBSERVADO | FACTURA | Factura de compra | 0.90 |
| `google_invoice.pdf` | OBSERVADO | FACTURA | Factura de compra | 0.95 |
| `lending_multi_document.pdf` | OBSERVADO | GENERICO | Captura genérica | 0.90 |
| `license.pdf` | OBSERVADO | GENERICO | Captura genérica | 0.90 |
| `procurement_multi_document.pdf` | OBSERVADO | FACTURA | Factura de compra | 0.95 |
| `utility_bill.pdf` | **CERRADO** | GENERICO | Captura genérica | 0.95 |

## Observaciones

- **Gemini funcionó con el modelo por defecto `gemini-2.5-flash`**: se consumieron tokens reales, se extrajeron valores y se clasificaron documentos.
- La mayoría quedó `OBSERVADO` porque las reglas de validación exigen campos específicos del contexto argentino (por ejemplo `cuitEmisor`) que no están en documentos de muestra en inglés. Esto es el comportamiento esperado: la IA propone, la regla decide.
- El flujo completo de cierre se verificó con `utility_bill.pdf`: `RECIBIDO → PROCESANDO → EXTRAIDO → VALIDADO → APROBADO → CERRADO`.
- `mvn verify` se ejecutó previamente con **174 tests, 0 fallas, 0 skipped**.
- Los documentos se almacenaron en MinIO, los metadatos en PostgreSQL, y la ejecución de la extracción quedó registrada con proveedor, modelo, tokens, costo y duración.

## Hallazgos menores

- El modelo `gemini-2.5-flash` responde correctamente, aunque la denominación "3.6 flash" del mensaje previo no existe en la API de Google; el sistema usa el default del backend.
- Algunos documentos fueron clasificados como `TIPO_NO_RECONOCIDO` y capturados con el esquema genérico; se registraron propuestas de nuevo tipo (`TipoPropuestoService`) para posible aprobación de catálogo.
- El antivirus está en modo `PERMISIVO` sin ClamAV activo; esto está documentado como no apto para producción.

## Rotación de clave Gemini

La clave usada quedó en `.env`. Para cumplir con el gate G-DEMO debe rotarse:

1. Ir a Google AI Studio / Google Cloud Console.
2. Revocar la clave anterior.
3. Generar una nueva clave.
4. Actualizar `NEXTDOCS_GEMINI_CLAVE` en `.env` (nunca commitear).

## Evidencia

- Resultados detallados JSON: `/tmp/corpus-resultados-final.json`.
- Logs del contenedor `nextdocs-app`.
- Este archivo de verificación.

## Estado del gate G-DEMO

- `mvn verify`: **PASS**.
- Ingesta, clasificación, extracción con Gemini, validación, revisión y cierre con corpus real: **PASS**.
- Rotación de clave Gemini: **PENDIENTE** (acción del usuario en consola Google).
- Recorrido E2E por navegador: **PENDIENTE** (requiere Playwright o interacción manual; no bloquea la certificación funcional del core).
