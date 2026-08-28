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

**Controles de ingesta, en este orden:**

1. Tamaño y extensión → 413 / 415
2. **MIME real por contenido** con Tika, no por el header ni la extensión → 415
3. **Antivirus** → si está infectado el documento se crea en estado `RECHAZADO`, el archivo va al
   bucket de cuarentena, se abre una excepción `SEGURIDAD` bloqueante y **nunca se encola**:
   el contenido malicioso jamás llega al proveedor de IA

La respuesta incluye `resultadoEscaneo`, `amenazaDetectada`, `motorEscaneo` y `enCuarentena` por archivo.

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

GET  /api/v1/documentos/{id}/candidatos                    permiso: documentos.leer
     → candidatos de asociación con puntaje, seleccionado, descartado y motivo
POST /api/v1/documentos/{id}/candidatos/{candidatoId}/seleccionar   permiso: documentos.revisar
     { "motivo": "..." }
     → selecciona uno, descarta el resto y fija la referencia externa del documento

POST /api/v1/documentos/{id}/reprocesar                    permiso: documentos.escribir
POST /api/v1/documentos/{id}/cerrar                        permiso: documentos.escribir
```

### Plantillas

```
GET  /api/v1/plantillas/configuracion
→ 200 { estados[], tiposDato[], tiposRegla[], severidades[], sensibilidades[], politicasOriginalFisico[] }

GET  /api/v1/plantillas?familia=COMEX                      permiso: plantillas.leer
GET  /api/v1/plantillas/familias                           permiso: plantillas.leer
GET  /api/v1/plantillas/{id}                               permiso: plantillas.leer
     → PlantillaModel con todas sus versiones y el estado de cada una
POST /api/v1/plantillas                                    permiso: plantillas.escribir
     { codigo, nombre, familia, descripcion }              crea la plantilla y su versión 1 en BORRADOR
PUT  /api/v1/plantillas/{id}                               permiso: plantillas.escribir
DELETE /api/v1/plantillas/{id}                             permiso: plantillas.escribir  (baja lógica)
```

**Versiones**

```
POST /api/v1/plantillas/{id}/versiones                     permiso: plantillas.escribir
     { versionBaseId, umbralAutoaprobacion, politicaOriginalFisico,
       versionPrompt, versionEsquema, instruccionExtraccion, notasCambio,
       estrategiaSegmentacion, paginasPorDocumento, patronInicioDocumento }
     → 201  crea una versión BORRADOR; si viene versionBaseId clona campos y reglas

GET  /api/v1/plantillas/versiones/{versionId}              permiso: plantillas.leer
     → VersionPlantillaModel con campos, reglas, editable y transicionesPosibles
PUT  /api/v1/plantillas/versiones/{versionId}              permiso: plantillas.escribir
POST /api/v1/plantillas/versiones/{versionId}/estado/{destino}   permiso: plantillas.escribir
POST /api/v1/plantillas/versiones/{versionId}/validar      permiso: plantillas.leer
     → { valida: bool, errores: [] }
POST /api/v1/plantillas/versiones/{versionId}/publicar     permiso: plantillas.publicar
POST /api/v1/plantillas/{id}/revertir/{versionId}          permiso: plantillas.publicar
```

**Campos y reglas** (sólo sobre versiones `BORRADOR` o `EN_PRUEBA`)

```
POST   /api/v1/plantillas/versiones/{versionId}/campos
PUT    /api/v1/plantillas/versiones/{versionId}/campos/{campoId}
DELETE /api/v1/plantillas/versiones/{versionId}/campos/{campoId}
POST   /api/v1/plantillas/versiones/{versionId}/reglas
PUT    /api/v1/plantillas/versiones/{versionId}/reglas/{reglaId}
DELETE /api/v1/plantillas/versiones/{versionId}/reglas/{reglaId}
```

**Ciclo de vida**

```
BORRADOR ⇄ EN_PRUEBA ──publicar──► PUBLICADA ──► DEPRECADA
                                       ▲              │
                                       └──revertir────┘
```

Reglas que impone la API:

- Una versión `PUBLICADA` o `DEPRECADA` es **inmutable**: agregar o editar campos y reglas devuelve 400.
  Para cambiar algo se crea una versión nueva, opcionalmente clonando la anterior.
- **Publicar valida antes**: la versión necesita al menos un campo extraíble, ninguna regla puede apuntar
  a un campo inexistente, las expresiones regulares deben compilar, los umbrales deben estar entre 0 y 1
  y la configuración JSON de cada regla debe ser válida.
- Publicar una versión **deprecata automáticamente** la que estaba publicada.
- `revertir` sólo acepta una versión `DEPRECADA` que ya estuvo publicada.
- **Los documentos históricos nunca cambian de versión.** Un documento ingresado con la v1 sigue
  apuntando a la v1 aunque después se publique la v2 (`QA-WF-03`).
- Las plantillas y sus versiones usan **bloqueo optimista**: dos ediciones concurrentes producen
  un 409 en la segunda.

**Tipos de regla y su configuración**

| Tipo | Configuración | Ejemplo |
|---|---|---|
| `OBLIGATORIO` | — | el campo debe estar `PRESENTE` (**sólo presencia, no el valor**) |
| `FORMATO` | `{"expresionRegular":"..."}` | validar patrón |
| `RANGO` | `{"minimo":"0.01","maximo":"9999"}` | numérico acotado |
| `VIGENCIA` | `{"diasTolerancia":30}` | la fecha no puede estar vencida |
| `COMPARACION_CAMPOS` | `{"campoA":"...","campoB":"..."}` | dos campos deben coincidir |
| `CATALOGO` | `{"valores":["USD","ARS"]}` | valor dentro de una lista |
| `CONFIANZA_MINIMA` | `{"minima":"0.9"}` | umbral por regla |

**Segmentación de PDF multi-documento**

Se configura por versión de plantilla. Corre **antes** de la extracción: si aplica, el padre pasa a
`DIVIDIDO` y **no se extrae**; cada hijo entra al pipeline por separado con su propio archivo.

| Estrategia | Configuración | Cuándo usarla |
|---|---|---|
| `NINGUNA` | — | el PDF es un solo documento (por defecto) |
| `PAGINAS_FIJAS` | `paginasPorDocumento` | cada documento ocupa siempre la misma cantidad de páginas |
| `PATRON_TEXTO` | `patronInicioDocumento` (regex) | un lote donde cada documento arranca con un encabezado reconocible |

Con `PATRON_TEXTO` el corte se hace donde **arranca** el siguiente documento, no en cada página:
un remito de dos páginas queda entero. Si el patrón no aparece nunca, devuelve un único tramo y no
se divide. Si el patrón no coincide en la página 1, esa página igual queda dentro del primer tramo.

Cada hijo hereda plantilla, versión, origen, remitente y referencia externa del padre, pero tiene su
propio `hashContenido`, su propia `claveIdempotencia` (`<clave del padre>#segmento-N`) y su propio
archivo en el object store. El vínculo queda en `segmento_documento` con el rango de páginas y el
motivo del corte.

> **Cuidado con `OBLIGATORIO`.** Verifica que el campo esté `PRESENTE`, no que su valor sea el esperado.
> Un remito con `conformidad = false` pasa una regla `OBLIGATORIO` sobre `conformidad`, porque el dato
> está. Para exigir un valor concreto usá `CATALOGO` con `{"valores":["true"]}`. Esta distinción es la
> diferencia entre aprobar y observar un remito sin conformidad.

### Antivirus

Se configura con `nextdocs.antivirus`. Dos motores detrás de `AntivirusInt`:

| Motor | Uso |
|---|---|
| `PERMISIVO` | por defecto. **No analiza nada** y marca `NO_ANALIZADO`. Sólo para desarrollo |
| `CLAMAV` | habla el protocolo `INSTREAM` de clamd por TCP |

`rechazarSiNoDisponible` decide qué pasa si el antivirus no responde:

- `true` (por defecto) — **fail-closed**: el archivo se rechaza con `ANTIVIRUS_NO_DISPONIBLE`.
  Si activaste el escaneo, es porque lo querés aplicado.
- `false` — fail-open: el archivo pasa marcado como `ERROR`, nunca como `LIMPIO`.

> El motor `PERMISIVO` marca `NO_ANALIZADO`, **nunca `LIMPIO`**. Un archivo sin analizar y un archivo
> analizado y limpio son cosas distintas, y la diferencia queda en la auditoría.

Para levantar ClamAV localmente: `docker compose --profile antivirus up -d clamav`.
Tarda unos minutos en descargar las firmas la primera vez.

### Asociación a objetos de negocio

El matching corre después de la validación. Los conectores se registran por tenant en
`configuracion_conector` y se consultan a través de `ConectorAsociacionInt`. El core **nunca**
conoce el sistema del otro lado; `FollowConnector` es la única clase que sabe qué es un pedido de Follow.

**Cómo se resuelve**

| Situación | Resultado | Estado del documento |
|---|---|---|
| Un candidato con puntaje ≥ `umbralSeleccionAutomatica` | `RESUELTA`, se fija la referencia | sigue su curso |
| Un candidato por debajo del umbral | `AMBIGUA` + excepción | `OBSERVADO` |
| Dos o más candidatos viables | `AMBIGUA` + excepción, **jamás elige solo** | `OBSERVADO` |
| Cero candidatos | `SIN_CANDIDATOS`, sin excepción de conector | sigue su curso |
| El conector falló | `CONECTOR_FALLIDO` + excepción `CONECTOR` | `OBSERVADO` |
| Un candidato pero otro conector falló | `AMBIGUA`, la búsqueda está incompleta | `OBSERVADO` |

> **Un timeout no es cero candidatos.** Son dos resultados distintos con dos excepciones distintas.
> Confundirlos haría que un documento quede sin asociar en silencio cuando en realidad nadie preguntó.

**Circuit breaker.** Cada par tenant + conector tiene su circuito. Tras `umbralCircuitoAbierto`
fallos consecutivos se abre por `duracionCircuitoAbiertoSegundos`; después pasa a semiabierto y
deja pasar una sonda. Si la sonda falla vuelve a abrirse de inmediato.

**Credenciales.** Igual que con los proveedores de IA: `referenciaSecreto` guarda `env:NOMBRE_VAR`,
nunca el secreto. Soporta `CLAVE_API` con cabecera configurable y `BEARER`.

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
