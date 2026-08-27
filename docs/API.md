# API v1 — NEXT DOC AI

Base: `http://localhost:8090`
OpenAPI: `/api-docs` · Swagger UI: `/swagger-ui.html`

## Autenticación

Dos mecanismos. Nunca conviven en la misma petición.

| Actor | Cabecera | Obtención |
|---|---|---|
| Usuario del portal | `Authorization: Bearer <jwt>` | `POST /api/v1/autenticacion/ingresar` |
| Integración externa | `X-Clave-Servicio: ndai_...` | Cuenta de servicio creada por un administrador |

Toda respuesta incluye `X-Correlacion-Id`. Si lo mandás en la petición, se propaga a los logs,
los eventos y la auditoría.

---

## Endpoints

### Autenticación

```
POST /api/v1/autenticacion/ingresar
{ "codigoTenant": "demo", "email": "admin@nextdocs.ai", "clave": "..." }
→ 200 { tokenAcceso, tokenRefresco, usuarioId, email, nombre, tenantId, codigoTenant, nombreTenant, permisos[] }

POST /api/v1/autenticacion/refrescar
{ "tokenRefresco": "..." }
→ 200 (misma forma)
```

### Documentos

```
GET  /api/v1/documentos/configuracion
→ 200 { estados[], origenes[], presencias[], severidades[], decisionesRevision[] }
```

```
POST /api/v1/documentos                                    permiso: documentos.escribir
Content-Type: multipart/form-data
Idempotency-Key: <clave>            (opcional; por defecto usa el sha256 del contenido)

  archivo : el binario
  datos   : JSON { origen, codigoPlantilla, remitente, observacion,
                   sujetoOrigen, sujetoTipoObjeto, sujetoIdObjeto }

→ 201 DocumentoModel
```

Reenviar la misma `Idempotency-Key` devuelve **el mismo documento**, no crea uno nuevo.

`sujeto*` es la referencia externa opaca. Por ejemplo `FOLLOW / PEDIDO / PED-2026-0042`.
El core no interpreta esos valores: los guarda y los emite en los eventos.

```
GET  /api/v1/documentos                                    permiso: documentos.leer
     ?estados=OBSERVADO,APROBADO &origen=WEB &codigoPlantilla=REMITO
     &texto=... &desde=2026-08-01T00:00:00Z &hasta=... &soloRaiz=true
     &pagina=0 &tamano=25 &orden=alta,desc
→ 200 Page<DocumentoModel>

GET  /api/v1/documentos/{id}                               permiso: documentos.leer
GET  /api/v1/documentos/{id}/detalle                       permiso: documentos.leer
     → { documento, extraccion, validacion, candidatos[], revisiones[], segmentos[] }
GET  /api/v1/documentos/{id}/original                      permiso: documentos.leer
     → { "url": "<url firmada, TTL 15 min>" }
GET  /api/v1/documentos/resumen                            permiso: documentos.leer
     → conteo por estado + profundidad de cola
```

```
POST /api/v1/documentos/{id}/revisiones                    permiso: documentos.revisar
{
  "decision": "APROBAR | RECHAZAR | OBSERVAR | CORREGIR | REPROCESAR",
  "motivo": "obligatorio si RECHAZAR, OBSERVAR o si sobreescribe hallazgos",
  "correcciones": { "cuitEmisor": "30123456789" },
  "hallazgosSobreescritos": ["<id>"],
  "duracionRevisionMilisegundos": 45000
}
→ 201 RevisionDocumentoModel

POST /api/v1/documentos/{id}/reprocesar                    permiso: documentos.escribir
POST /api/v1/documentos/{id}/cerrar                        permiso: documentos.escribir
```

### Excepciones

```
GET  /api/v1/excepciones/configuracion
GET  /api/v1/excepciones?estado=ABIERTA&pagina=0&tamano=25   permiso: excepciones.leer
GET  /api/v1/excepciones/{id}                                permiso: excepciones.leer
GET  /api/v1/excepciones/documento/{documentoId}             permiso: excepciones.leer
POST /api/v1/excepciones/{id}/asignar  { "responsableId": "..." }   permiso: excepciones.gestionar
POST /api/v1/excepciones/{id}/resolver { "resolucion": "..." }      permiso: excepciones.gestionar
```

---

## Permisos

| Permiso | Habilita |
|---|---|
| `documentos.leer` | Bandeja, visor, detalle, descarga del original |
| `documentos.escribir` | Ingesta, reproceso, cierre |
| `documentos.revisar` | Registrar revisiones y sobreescribir hallazgos |
| `documentos.eliminar` | Baja lógica |
| `plantillas.leer` / `plantillas.escribir` / `plantillas.publicar` | Template Studio |
| `excepciones.leer` / `excepciones.gestionar` | Exception Center |
| `gobernanza.leer` / `gobernanza.administrar` | Auditoría, retención, proveedores |
| `tenant.administrar` | Usuarios, roles, cuentas de servicio |

Roles predefinidos al crear un tenant: `ADMINISTRADOR` (todos), `OPERADOR`, `REVISOR`, `AUDITOR`.

---

## Errores

Contrato uniforme:

```json
{
  "mensaje": "No se encontro Documento con id abc",
  "ruta": "/api/v1/documentos/abc",
  "estado": "NOT_FOUND",
  "codigo": 404,
  "correlacionId": "59dbff0f892443459a71ce0cd6e9508b",
  "fecha": "2026-08-27T22:38:07Z"
}
```

Los errores de validación de campos agregan `campos: { "<campo>": "<mensaje>" }`.

| Código | Cuándo |
|---|---|
| 400 | Datos inválidos, motivo faltante en una decisión que lo exige |
| 401 | Token ausente, inválido o expirado |
| 403 | Permiso insuficiente |
| 404 | No existe **o pertenece a otro tenant** (no se distingue, a propósito) |
| 409 | Transición de estado no permitida, violación de unicidad |
| 413 | Archivo demasiado grande |
| 415 | Extensión o MIME real no permitido |
| 503 | Proveedor de IA no disponible |

---

## Webhooks

Suscribiéndose a eventos canónicos, NEXT DOC AI hace `POST` al endpoint configurado:

| Cabecera | Contenido |
|---|---|
| `X-Nextdocs-Evento` | `document.received`, `document.extracted`, `document.validated`, `document.approved`, … |
| `X-Nextdocs-Entrega` | Id de la entrega, estable entre reintentos → usalo para idempotencia |
| `X-Nextdocs-Firma` | `HMAC-SHA256(secreto, cuerpo)` en hexadecimal |

Se considera entregado con cualquier `2xx`. Ante fallo se reintenta con backoff exponencial
(tope 60 min) hasta 6 intentos, y luego queda `AGOTADO`.

Eventos canónicos emitidos hoy: `document.received`, `document.extracted`, `document.validated`,
`document.observed`, `document.approved`, `document.rejected`, `document.closed`,
`document.segmented`, `extraction.failed`, `exception.created`, `exception.resolved`.

---

## Prueba rápida

```bash
docker compose up -d
cd backend && ./mvnw spring-boot:run     # crea el tenant demo automáticamente

TOKEN=$(curl -s -X POST localhost:8090/api/v1/autenticacion/ingresar \
  -H 'Content-Type: application/json' \
  -d '{"codigoTenant":"demo","email":"admin@nextdocs.ai","clave":"nextdocs123"}' \
  | jq -r .tokenAcceso)

curl -X POST localhost:8090/api/v1/documentos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: prueba-001" \
  -F "archivo=@remito.pdf;type=application/pdf" \
  -F 'datos={"origen":"WEB","codigoPlantilla":"REMITO"};type=application/json'

curl -s "localhost:8090/api/v1/documentos/<id>/detalle" -H "Authorization: Bearer $TOKEN" | jq
```
