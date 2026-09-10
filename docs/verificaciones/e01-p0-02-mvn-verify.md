# E01 / P0-02 · Verificación `mvn verify` del core documental

Fecha: sesión actual.
Entorno: contenedor Maven `maven:3.9-eclipse-temurin-21`, infraestructura local levantada.

## Comando ejecutado

```bash
cd /home/gabibenitezzz/Escritorio/NEXT\ AI/nextdocs-ai/backend
docker run --rm --network host \
  --env-file /home/gabibenitezzz/Escritorio/NEXT\ AI/nextdocs-ai/.env \
  -v "$PWD":/app -v nextdocs-m2:/root/.m2 -w /app \
  maven:3.9-eclipse-temurin-21 mvn verify
```

## Resultado

```text
[INFO] Tests run: 174, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time: 01:55 min
```

## Observaciones

- Se ejecutaron 174 tests (7 unitarios de DeepSeek, 10 unitarios de Gemini, 8 de Follow connector, 5 de antivirus, 5 de hash util, 4 de enum, y el resto de integración).
- Los tests de proveedores externos usan mocks; los warnings de `Gemini respondio 503`, `403 API key not valid`, `429 Quota exceeded` y `DeepSeek respondio 401/429` pertenecen a casos unitarios de manejo de errores.
- Los tests de integración cubrieron ingestas, clasificación, extracción, validación, excepciones, revisión, retención, exportación, federación y KPIS, con transiciones de documento de `RECIBIDO → PROCESANDO → EXTRAIDO → VALIDADO → APROBADO → CERRADO`.
- El proveedor Gemini **no estuvo activo** en los tests de integración: usaron `SIMULADO`. Esto se confirma porque los tests no consumieron la clave configurada en `.env`.
- La infraestructura (PostgreSQL, Redis, MinIO, Keycloak) respondió correctamente.

## Evidencia de salida

La salida completa queda en el log del shell ejecutado. El resumen confirma que el core compila, levanta y pasa la suite automatizada sin errores ni skips.

## Estado respecto al gate G-DEMO

- `mvn verify`: **PASS**.
- Falta: recorrido humano/API con documentos reales y proveedor Gemini real activado.
