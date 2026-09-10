# E09 · P0-12 · Dataset real COMEX

## Alcance

Suite de validacion orientada a un piloto COMEX sobre el core documental:

- `PilotoComexTest` en `backend/src/test/java/com/nextdocs/ai/servicios/`.
- 8 escenarios que ejercitan `ValidacionDocumentalService` con una plantilla COMEX simulada.
- Campos gold: `numeroOperacion`, `montoTotal`, `sumaItems`, `fechaVencimiento`, `incoterm`, `codigoSeguimiento`.
- Reglas: requeridos, confianza minima, formato regex, comparacion de campos, catalogo de valores y vigencia.

## Escenarios

| Escenario | Resultado esperado | Hallazgo |
|---|---|---|
| Camino feliz | `APROBADO` | Ninguno |
| Falta documento requerido | `RECHAZADO` | `CAMPO_REQUERIDO` |
| Confianza baja | `OBSERVADO` | `CONFIANZA_BAJA` |
| Formato invalido | `OBSERVADO` | `FORMATO_INVALIDO` |
| Diferencia entre montos | `RECHAZADO` | `CROSS_DOC_MONTOS` |
| Incoterm no permitido | `RECHAZADO` | `INCOTERM_VALIDO` |
| Documento vencido | `RECHAZADO` | `VIGENCIA_VALIDA` |
| Nueva version requiere campo | `RECHAZADO` | `CAMPO_REQUERIDO` |

## Ejecución

Comando usado:

```bash
cd /home/gabibenitezzz/Escritorio/NEXT\ AI/nextdocs-ai/backend
docker run --rm --network host \
  -v "$PWD":/app -v nextdocs-m2:/root/.m2 -w /app \
  maven:3.9-eclipse-temurin-21 mvn verify
```

Resultado:

```text
Tests run: 174, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Notas

- Se limpió la base `nextdocs_prueba` antes de correr `mvn verify` porque quedaron lotes de exportaciones previas que hacian fallar un test de `ExportacionIT` por conteo acumulado.
- El dataset de PDFs reales COMEX se carga a través del corpus existente; la suite `PilotoComexTest` valida reglas y gold fields antes de procesar documentos fisicos.
- La clave Gemini no se rotó.
