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
PUT  /api/v1/plantillas/{id}/quality-gate                  permiso: plantillas.escribir
     { exigirQualityGate, umbralMinimo }
GET  /api/v1/plantillas/{id}/conjunto-prueba               permiso: plantillas.leer
POST /api/v1/plantillas/{id}/conjunto-prueba/casos         permiso: plantillas.escribir
     multipart: archivo + esperado (JSON) + nombre
DEL  /api/v1/plantillas/conjunto-prueba/casos/{casoId}     permiso: plantillas.escribir
POST /api/v1/plantillas/versiones/{versionId}/probar       permiso: plantillas.escribir
GET  /api/v1/plantillas/versiones/{versionId}/pruebas      permiso: plantillas.leer
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
- **Quality gate (`GOV-04`).** Si la plantilla tiene `exigirQualityGate`, publicar exige una corrida
  gold **aprobada** contra el conjunto vigente. Sin casos, o con una corrida vieja (la huella cambió),
  se bloquea. El umbral por defecto es `0.80`. Una versión que **empeora** la exactitud de la publicada
  queda `RECHAZADO` y no se publica. Sin la política, publicar sigue igual que antes.
- La corrida no crea documentos de bandeja: llama al proveedor en seco, puntúa presencia y valor
  contra el esperado, deja la versión en `EN_PRUEBA` y, si aprueba, graba
  `factorCalibracionConfianza` en la versión. Ese factor se aplica a `confianza` en extracciones
  reales; `confianzaProveedor` sigue cruda.
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

### Original físico

Se activa por `politicaOriginalFisico` de la versión de plantilla. Al ingresar un documento cuya
plantilla lo pide, se crea el seguimiento en `PENDIENTE`.

| Política | Efecto |
|---|---|
| `NO_REQUIERE` | no se crea seguimiento |
| `REQUIERE_SEGUIMIENTO` | el documento **puede cerrarse digitalmente**, pero el papel queda `PENDIENTE` y visible |
| `REQUIERE_PARA_CIERRE` | el cierre se rechaza con 400 hasta que el papel esté `RECIBIDO` o `ARCHIVADO` |

```
GET  /api/v1/originales-fisicos/configuracion
GET  /api/v1/originales-fisicos?estado=PENDIENTE          permiso: documentos.leer
GET  /api/v1/originales-fisicos/documento/{documentoId}   permiso: documentos.leer
     → 204 si el documento no requiere seguimiento
POST /api/v1/originales-fisicos/documento/{id}/recibir    permiso: documentos.escribir
     { ubicacion, referenciaFisica, observacion }
POST /api/v1/originales-fisicos/documento/{id}/archivar   permiso: documentos.escribir
POST /api/v1/originales-fisicos/documento/{id}/extraviar  permiso: documentos.escribir
     { observacion }   obligatoria
```

**Ciclo del papel**

```
PENDIENTE ──► RECIBIDO ──► ARCHIVADO
     │            │             │
     └────────────┴─────────────┴──► EXTRAVIADO ──► RECIBIDO
```

Archivar un papel que nunca se recibió devuelve 400. Declararlo extraviado exige observación.

> **Cerrar el documento digital no cierra el papel.** Son dos ciclos de vida distintos y esa es
> justamente la razón de existir del seguimiento (`QA1-08`): el expediente digital puede estar
> terminado mientras el original todavía está en tránsito.

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

### Administración de tenant, usuarios y cuentas de servicio

```
GET  /api/v1/administracion/configuracion
GET  /api/v1/administracion/perfil                      cualquier usuario autenticado
POST /api/v1/administracion/perfil/clave                cualquier usuario autenticado
GET  /api/v1/administracion/tenant                      permiso: tenant.administrar
PUT  /api/v1/administracion/tenant                      permiso: tenant.administrar
GET  /api/v1/administracion/usuarios?estado&texto       permiso: tenant.administrar
POST /api/v1/administracion/usuarios                    permiso: tenant.administrar
GET  /api/v1/administracion/usuarios/{id}               permiso: tenant.administrar
PUT  /api/v1/administracion/usuarios/{id}               permiso: tenant.administrar
PUT  /api/v1/administracion/usuarios/{id}/roles         permiso: tenant.administrar
POST /api/v1/administracion/usuarios/{id}/bloquear      permiso: tenant.administrar
POST /api/v1/administracion/usuarios/{id}/desbloquear   permiso: tenant.administrar
POST /api/v1/administracion/usuarios/{id}/clave         permiso: tenant.administrar
DEL  /api/v1/administracion/usuarios/{id}               permiso: tenant.administrar
GET  /api/v1/administracion/roles                       permiso: tenant.administrar
POST /api/v1/administracion/roles                       permiso: tenant.administrar
PUT  /api/v1/administracion/roles/{id}                  permiso: tenant.administrar
DEL  /api/v1/administracion/roles/{id}                  permiso: tenant.administrar
GET  /api/v1/administracion/cuentas-servicio            permiso: tenant.administrar
POST /api/v1/administracion/cuentas-servicio            permiso: tenant.administrar
POST /api/v1/administracion/cuentas-servicio/{id}/revocar   permiso: tenant.administrar
```

**El tenant nunca se queda sin gobierno.** Bloquear, dar de baja o quitarle el rol de administrador
al **último** administrador activo devuelve 400. Es el error que deja un tenant inaccesible y sólo se
arregla por base de datos. Tampoco un usuario puede bloquearse ni eliminarse a sí mismo.

**Los roles predefinidos son inmutables.** `ADMINISTRADOR`, `OPERADOR`, `REVISOR` y `AUDITOR` no se
modifican ni se borran: se crea un rol propio a partir de sus permisos. Un rol propio con usuarios
asignados tampoco se borra, hay que reasignarlos primero.

**Los permisos se validan contra el catálogo.** Un permiso inventado en un rol o un alcance inventado
en una cuenta de servicio devuelve 400. `GET /configuracion` devuelve el catálogo completo.

**Una cuenta de servicio no puede tener `tenant.administrar`.** Administrar el tenant es una acción de
persona, no de integración. Pedirlo devuelve 400.

**La clave de una cuenta de servicio se devuelve una sola vez**, en la respuesta del alta. En base
sólo queda el sha256 y el prefijo de 12 caracteres para identificarla. Revocar exige motivo.

**Claves de usuario.** Mínimo 10 caracteres. Cambiar la propia exige la actual y rechaza repetirla; un
administrador puede restablecer la de otro sin conocerla, y eso queda auditado con su email.

**Un bloqueo tiene efecto inmediato.** Cada petición con JWT revalida contra la base que el usuario
siga activo y **relee sus permisos**. Sin eso, un usuario bloqueado seguiría entrando hasta que
expirara su token, que dura 8 horas, y el bloqueo sería una ilusión. El costo es una consulta por
petición, la misma que ya pagaban las cuentas de servicio.

### Gobernanza y auditoría

```
GET  /api/v1/gobernanza/configuracion
GET  /api/v1/gobernanza/auditoria                            permiso: gobernanza.leer
GET  /api/v1/gobernanza/auditoria/resumen                    permiso: gobernanza.leer
GET  /api/v1/gobernanza/auditoria/correlacion/{correlacionId}   permiso: gobernanza.leer
GET  /api/v1/gobernanza/auditoria/recurso/{tipo}/{id}        permiso: gobernanza.leer
GET  /api/v1/gobernanza/auditoria/exportacion                permiso: gobernanza.administrar
GET  /api/v1/gobernanza/auditoria/exportacion/json           permiso: gobernanza.administrar
GET  /api/v1/gobernanza/documentos/{id}/trazabilidad         permiso: gobernanza.leer
POST /api/v1/gobernanza/documentos/{id}/retencion-legal      permiso: gobernanza.administrar
GET  /api/v1/gobernanza/retencion/politicas                  permiso: gobernanza.leer
POST /api/v1/gobernanza/retencion/politicas                  permiso: gobernanza.administrar
PUT  /api/v1/gobernanza/retencion/politicas/{id}             permiso: gobernanza.administrar
DEL  /api/v1/gobernanza/retencion/politicas/{id}             permiso: gobernanza.administrar
GET  /api/v1/gobernanza/retencion/inventario                 permiso: gobernanza.leer
GET  /api/v1/gobernanza/retencion/vencidos                   permiso: gobernanza.leer
POST /api/v1/gobernanza/retencion/documentos/{id}/aplicar    permiso: gobernanza.administrar
```

### Integraciones (webhooks)

```
GET  /api/v1/integraciones/configuracion
GET  /api/v1/integraciones/salud                              permiso: gobernanza.leer
GET  /api/v1/integraciones/suscripciones                      permiso: gobernanza.leer
GET  /api/v1/integraciones/suscripciones/{id}                 permiso: gobernanza.leer
POST /api/v1/integraciones/suscripciones                      permiso: gobernanza.administrar
PUT  /api/v1/integraciones/suscripciones/{id}                 permiso: gobernanza.administrar
POST /api/v1/integraciones/suscripciones/{id}/pausar          permiso: gobernanza.administrar
POST /api/v1/integraciones/suscripciones/{id}/reactivar       permiso: gobernanza.administrar
POST /api/v1/integraciones/suscripciones/{id}/secreto         permiso: gobernanza.administrar
POST /api/v1/integraciones/suscripciones/{id}/probar          permiso: gobernanza.administrar
DEL  /api/v1/integraciones/suscripciones/{id}                 permiso: gobernanza.administrar
GET  /api/v1/integraciones/entregas?estado&suscripcionId      permiso: gobernanza.leer
POST /api/v1/integraciones/entregas/{id}/reintentar           permiso: gobernanza.administrar
```

No se crearon permisos nuevos: los tenants ya existentes no migran roles solos. Quien ya puede
leer gobernanza ve el monitor; quien la administra da de alta suscripciones.

**Secreto.** Se genera si no viene (`ndwh_...`). El alta y la rotación lo devuelven **una sola vez**.
El listado y el detalle sólo exponen un prefijo de 8 caracteres. Nunca viaja en eventos de auditoría.

**URL.** Tiene que ser HTTPS. Se rechazan loopback, IPs privadas, link-local, multicast y hosts
`.internal`. En desarrollo, `nextdocs.webhooks.permitirLocalhost=true` permite HTTP contra
`127.0.0.1` / `localhost` y no relaja el resto de redes internas.

**Prueba.** `POST .../probar` publica `webhook.test`, entrega en el momento (aunque el evento no
esté en la lista de la suscripción) y no fan-out a otras suscripciones.

**Auto-pausa.** Si `fallosConsecutivos` llega a `nextdocs.webhooks.umbralPausa` (10 por defecto, 3
en pruebas), la suscripción se desactiva y queda auditada como `WEBHOOK_PAUSADO`. Reactivarla pone
el contador en cero.

**Reintento.** Una entrega `PENDIENTE` se dispara ya. Una `AGOTADO` (DLQ) reinicia el presupuesto
de intentos y entrega ya. Una `ENTREGADO` se rechaza.

**El job.** `DespachadorEventosService` se apaga con `nextdocs.webhooks.despachadorActivo=false`.
La entrega real vive en `EntregaWebhookService`, que sigue disponible para la API y para los tests.

**Filtros de la consulta y de las dos exportaciones.** `desde`, `hasta` (ISO-8601), `accion`,
`tipoRecurso`, `idRecurso`, `tipoActor`, `idActor`, `correlacionId` y `exitoso`. Todos opcionales y
combinables; el orden por defecto es `fecha,desc`. La consulta pagina con `pagina`, `tamano` y
`orden`; el tamaño máximo es 200.

**Trazabilidad (`GOV-01`).** Devuelve, para un documento, todo lo necesario para reconstruir cómo
llegó a su estado: la versión de plantilla con su `versionPrompt` y `versionEsquema`, cada ejecución
de extracción con proveedor, modelo y valores, cada ejecución de validación con sus hallazgos, las
reglas vigentes en esa versión, los candidatos de asociación con el seleccionado, las revisiones
humanas con sus correcciones y la línea de tiempo de auditoría.

La respuesta trae además `completa` y `faltantes`: en vez de devolver huecos silenciosos, declara
qué eslabón no se pudo reconstruir. `completa` es `true` cuando existe rastro de cada etapa que el
documento **efectivamente atravesó**, con dos matices que evitan falsos negativos:

- El revisor sólo se exige si el documento terminó en `APROBADO`, `RECHAZADO` o `CERRADO` **y**
  ninguna validación fue autoaprobada. Un documento autoaprobado no tuvo revisor y eso es correcto.
- La asociación seleccionada sólo se exige si hubo candidatos. Un tenant sin conector no produce
  ninguno, y eso tampoco es un hueco.

Consultar la trazabilidad queda auditado como `AUDITORIA_CONSULTADA` sobre el documento: en gobernanza
importa quién miró qué.

**Exportación.** `/exportacion` devuelve `text/csv` con `Content-Disposition: attachment`;
`/exportacion/json` devuelve el mismo conjunto como arreglo JSON. Ambas quedan auditadas como
`AUDITORIA_EXPORTADA` con el formato, la cantidad exportada, los filtros usados y si la exportación
se truncó. El tope es de 50 000 eventos por exportación, recorridos en lotes de 500.

El CSV usa `;` como separador y neutraliza los valores que empiezan con `=`, `+`, `-` o `@`
anteponiéndoles una comilla simple, para que una planilla no ejecute como fórmula un dato que entró
por el nombre de un archivo.

**Retención legal.** `POST /retencion-legal` con `{ "activa": true, "motivo": "..." }`. El motivo es
obligatorio y activar dos veces devuelve 400: preferimos rechazar antes que auditar un cambio que no
ocurrió. Queda registrado como `RETENCION_LEGAL_MODIFICADA` con actor y motivo.

**Políticas de retención.** Una clase por tenant. Desactivar deja la política visible como inactiva
en vez de borrarla, para no perder el historial ni bloquear la clase.

### Retención en ejecución (`GOV-02`)

**Cuándo empieza a correr el plazo.** Al **cerrar** el documento. `retenerHasta` se fija en ese
momento como `cerrado + duracionDias` de la política cuya clase coincide con el código de plantilla.
Un documento que nunca se cierra no vence nunca, y uno cerrado sin política tampoco: aparece en el
inventario como `cerradosSinPolitica` para que alguien decida, en vez de quedar expuesto a un
borrado por omisión.

**Qué hace cada acción cuando vence el plazo.**

| Acción | Efecto |
|---|---|
| `CONSERVAR` | No borra nada. Marca el documento como tratado y deja el evento |
| `ANONIMIZAR` | Borra el archivo original del almacenamiento y reemplaza por `[ANONIMIZADO]` los valores de los campos marcados `CONFIDENCIAL` o `PERSONAL`. El documento, sus hallazgos y su auditoría quedan |
| `ELIMINAR` | Borra el archivo original y da de baja lógica el documento. El registro y la auditoría quedan |

Ninguna acción borra la auditoría. La `presencia` de un valor anonimizado **no cambia**: seguía
estando presente en el documento original y decir lo contrario falsearía el registro histórico. Lo
que marca la purga es el campo `anonimizado`.

**Legal hold.** Un documento con `retencionLegal = true` no se trata, sin importar que haya vencido.
El ciclo lo cuenta como retenido y sigue. Al levantar el hold, el documento vuelve a ser elegible en
el ciclo siguiente.

**Prueba de borrado.** Cada documento tratado deja un `RETENCION_APLICADA` con la política, la clase,
la acción, la fecha de cierre, la de vencimiento, el hash del contenido y, por cada archivo borrado,
su nombre, checksum y tamaño. Es la evidencia de qué se borró, sin conservar el contenido.

**El job.** Corre cada hora (`nextdocs.retencion.intervaloMilisegundos`), toma hasta 100 documentos
por ciclo (`documentosPorCiclo`, tope duro de 500) y se apaga con
`nextdocs.retencion.trabajadorActivo=false`. Cada ciclo deja un `RETENCION_CICLO_EJECUTADO` por
tenant con los contadores. Los documentos retenidos por orden legal **no** generan un evento
individual en cada pasada: eso inundaría la auditoría. Quedan en el contador del ciclo, y la prueba
de que el hold se respetó es que el documento nunca fue tratado más el evento de activación del hold.

`POST /retencion/documentos/{id}/aplicar` fuerza la evaluación de un documento puntual y devuelve el
resultado (`APLICADA`, `OMITIDA_POR_RETENCION_LEGAL`, `OMITIDA_SIN_POLITICA`, `OMITIDA_NO_VENCIDA`,
`OMITIDA_YA_APLICADA`) con su motivo. A diferencia del ciclo, sí audita las omisiones, porque hubo
alguien que preguntó.

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
| `gobernanza.leer` / `gobernanza.administrar` | Auditoría, retención, proveedores, webhooks y monitor de integraciones |
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
(tope 60 min) hasta 6 intentos, y luego queda `AGOTADO`. Tras `umbralPausa` fallos consecutivos
la suscripción se pausa sola.

Eventos canónicos emitidos hoy: `document.received`, `document.extracted`, `document.validated`,
`document.observed`, `document.approved`, `document.rejected`, `document.closed`,
`document.segmented`, `extraction.failed`, `exception.created`, `exception.resolved`,
`template.published`, `template.deprecated`, `connector.action.failed`, `webhook.test`.

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
