# Empezar acá

Documento de traspaso. Si sos otra IA o un desarrollador que toma el proyecto sin contexto previo,
leé esto primero: son 5 minutos y te ahorra los errores que ya cometimos.

---

## 1. Qué es esto

**NEXT DOC AI**: plataforma de inteligencia documental. Un documento entra, sale como dato
estructurado, validado, asociado a un objeto de negocio, auditado y disponible por API o evento.

**Es un producto independiente.** Follow, CIMA y Valid360 son consumidores **opcionales** vía
conector y SSO. Ningún servicio del core puede importar modelos de Follow. Si te tienta hacerlo,
la respuesta es un adaptador nuevo detrás de `ConectorAsociacionInt`.

La especificación funcional vive **fuera del repo**, en `dev/NEXT_DOC_AI_SRP084_V7_FINAL_20260823/`.
Los documentos que mandan son `03_ARQUITECTURA...`, `N1` (PRD), `N2` (flujos) y `N3` (QA y backlog).
Los códigos tipo `QA1-03` o `SEC-04` que vas a ver en tests y commits salen del N3.

---

## 2. Levantarlo en 3 comandos

```bash
cp .env.example .env          # completá NEXTDOCS_GEMINI_CLAVE si vas a usar Gemini
docker compose up -d          # PostgreSQL 5434 · Redis 6381 · MinIO 9102
                              # GreenMail 3027/3145 · Keycloak 8089
docker compose --profile whatsapp up -d graph-falso   # opcional: Graph API simulada 8091
cd backend
docker run --rm --network host -v "$PWD":/app -v nextdocs-m2:/root/.m2 \
  -w /app maven:3.9-eclipse-temurin-21 mvn spring-boot:run
```

Arranca el tenant `demo` con `admin@nextdocs.ai` / `nextdocs123`.
Swagger en `http://localhost:8090/swagger-ui.html`.

En la máquina donde se construyó **no hay JDK ni `mvnw`**: Maven siempre corre por contenedor, y
por eso los comandos de arriba son así. Agregale `--env-file ../.env` si necesitás las claves de IA.

**Stack completo** (imagen + infra, sin JDK):

```bash
docker compose --profile app up -d --build
curl -fsS http://localhost:8090/actuator/health
```

El detalle está en [`DESPLIEGUE.md`](DESPLIEGUE.md).

---

## 3. Las reglas que no se negocian

Estas están escritas en el código y verificadas por tests. Romperlas rompe el producto,
no sólo el estilo.

| Regla | Por qué |
|---|---|
| **Prohibido escribir comentarios en el código** | Pedido explícito del dueño del proyecto. Lo que necesite explicación va a `docs/` |
| Todo en español, camelCase, estructura de paquetes de `follow-backend` | Consistencia con el equipo |
| **La confianza de lectura nunca aprueba sola** | Decide la regla. Gemini devuelve `1.0` en documentos limpios: si dejás que la confianza apruebe, aprueba todo |
| **`NO_FIGURA` ≠ `ILEGIBLE`** | No saber es distinto de saber que no está. Producen hallazgos y severidades distintas |
| **Dos candidatos van a revisión humana** | Jamás elegir en silencio |
| **Una plantilla publicada no se modifica** | Se crea una versión nueva. Los documentos históricos no cambian de versión |
| **Sobreescribir un hallazgo exige motivo** | Queda auditado con actor y hash antes/después |
| **Un timeout de conector ≠ cero candidatos** | Son resultados distintos con excepciones distintas |
| **La retención legal bloquea toda acción destructiva** | Aunque el plazo haya vencido. Es una orden judicial, no una preferencia |
| **El tenant nunca queda sin administradores activos** | El resultado sería un tenant que sólo se recupera tocando la base |
| **Una cuenta de servicio no administra el tenant** | Una credencial de integración que crea usuarios es una vía de escalada |
| **Un documento sin política de retención no vence** | Borrar por omisión es el peor error posible. Queda en el inventario para que alguien decida |
| **Ninguna acción de retención borra la auditoría** | El registro de qué se borró es justamente lo que hay que conservar |
| **Los servicios devuelven modelos, no entidades** | Ver trampa 1 abajo |

---

## 4. Trampas que ya nos costaron tiempo

### 1. `LazyInitializationException` en el borde HTTP

Pasó **tres veces**. Si un servicio devuelve una entidad JPA y el controlador la convierte a DTO,
la conversión ocurre fuera de la transacción y explota al tocar una relación lazy.

**Regla:** el servicio devuelve el modelo ya convertido, con la conversión **dentro** del
`@Transactional`. Si ves un servicio que devuelve `Documento` en vez de `DocumentoModel`, es un bug
esperando.

### 2. `OBLIGATORIO` verifica presencia, no valor

Un remito con `conformidad = false` **pasa** una regla `OBLIGATORIO` sobre `conformidad`, porque el
dato está. Nos aprobó un remito sin conformidad del receptor.

Para exigir un valor concreto usá `CATALOGO` con `{"valores":["true"]}`.

### 3. La confianza de Gemini es gruesa y auto-reportada

Medido contra `gemini-2.5-flash` real:

| Documento | Confianzas |
|---|---|
| limpio | todas `1.0` |
| con texto degradado | `0.6`, `0.7`, `0.8`, `0.95` |

Sirve como señal ordinal para detectar documentos malos. **No es una probabilidad calibrada.**
Por eso se guardan las dos: `confianzaProveedor` cruda para lineage y `confianza` calibrada para
decidir. El `factorCalibracionConfianza` de la versión sale de la última corrida gold **aprobada**
(tarea 2): exactitud del conjunto / confianza media del proveedor, acotado a `[0.10, 1.00]`.

### 4. Sólo se cierra desde `APROBADO`, `RECHAZADO` o `DIVIDIDO`

`OBSERVADO → CERRADO` y `VALIDADO → CERRADO` dan 409. Es correcto: no se cierra un documento
observado sin resolverlo, ni uno validado sin que alguien lo apruebe. Si un test tuyo falla acá,
probablemente el test esté mal, no la máquina de estados.

### 5. Testcontainers no funciona en este entorno

Docker 29.7 exige API ≥ 1.40 y el `docker-java` de Testcontainers 1.21.3 negocia 1.32. Probamos
forzar la versión por env y por `.testcontainers.properties`: no anda.

Los tests de integración usan la infra del `compose.yml` sobre una base `nextdocs_prueba` separada
que se crea sola. Si no hay infra levantada, se **saltan** con mensaje claro en vez de fallar.
Migrar de vuelta a Testcontainers, si algún día soporta esta versión, es cambiar sólo
`PruebaIntegracion`.

### 6. El bucket de cuarentena

Si tenés volúmenes de MinIO viejos, el bucket `nextdocs-cuarentena` puede no existir y la subida de
un archivo infectado falla con "No se pudo almacenar el archivo". Se arregla con
`docker compose up -d minio-init`.

### 7. Consultas con parámetros nulos en PostgreSQL

`(:desde IS NULL OR e.fecha >= :desde)` **falla** con `Instant` nulo: PostgreSQL no puede inferir el
tipo del parámetro. Con `String` y enums funciona. Ya nos rompió la consulta de auditoría. Si escribís
una consulta con fechas opcionales, pasá valores por defecto en vez de nulos.

Para filtros opcionales que se combinan, la salida limpia es una `Specification`: el predicado sólo
se agrega cuando el valor existe, así que el parámetro nulo nunca llega a PostgreSQL. Es lo que hacen
`DocumentoSpecificationBuilder` y `EventoAuditoriaSpecificationBuilder`. Los `@Query` con
`INICIO_DE_LOS_TIEMPOS` / `FIN_DE_LOS_TIEMPOS` del `EventoAuditoriaRepository` son el otro camino, y
siguen ahí para las consultas de rango simple.

### 8. Un `Assumptions.abort()` en un bloque `static` no saltea, revienta

`PruebaIntegracion` saltea la clase entera cuando no hay infraestructura. Eso **sólo funciona desde
`@BeforeAll`**: si el chequeo vive en un bloque `static`, JUnit lo envuelve en
`ExceptionInInitializerError` y cada test falla con `NoClassDefFoundError` en vez de saltarse. Nos
dio 14 tests en rojo sin ninguna causa visible. Si agregás una precondición de entorno, ponela en un
método de ciclo de vida, nunca en el inicializador estático.

### 9. Un número del panel tiene que ser el tamaño exacto de su población

Los KPI con drill-down (`documentosRecibidos`, `documentosCerrados`) usan **dos** consultas: una que
cuenta y otra que lista. Si los predicados no son idénticos, la tarjeta dice 20 y el detalle muestra
30 — que es exactamente el indicador sin explicación que el ANEXO_H prohíbe. Ya pasó: el conteo
filtraba `documentoPadre IS NULL` y usaba `alta`, y el listado ni filtraba padres ni usaba `recibido`,
así que un PDF partido en 10 sumaba 1 en la tarjeta y 11 en el detalle.

`KpiIT.elConteoCoincideConSuPoblacion` fija la invariante e ingresa a propósito un lote segmentado.
Si agregás un indicador a `KpiService.CON_POBLACION`, el test lo toma solo.

### 10. Un permiso nuevo no se le da solo a los roles que ya existen

`Permiso.todos()` sólo se lee **al crear un tenant**. Los roles predefinidos ya sembrados guardan sus
permisos en `rol_permiso`, así que agregar una constante al código no habilita nada en un entorno
existente: el endpoint devuelve 403 y el código se ve perfecto. Pasó al agregar
`documentos.exportar`. La solución es una migración que haga el `INSERT ... WHERE NOT EXISTS` sobre
los roles predefinidos, como `V13__permiso_exportar_roles_predefinidos.sql`.

Y el corolario: **nunca edites una migración ya aplicada** para meter el arreglo. Rompe el checksum
en todo entorno que la haya corrido. Siempre una migración nueva.

---

### 11. El correo saliente no puede abrir su propia transacción

`CorreoSalienteService.enviar` guarda una fila que apunta por FK a la correlación recién creada.
Estaba anotado `@Transactional(REQUIRES_NEW)` para que el registro de auditoría sobreviviera a un
rollback del llamador, y eso rompía el alta: la transacción de afuera todavía no había commiteado la
correlación, así que el `INSERT` de adentro violaba `fk_mensaje_correo_saliente_correlacion`.

Si una operación tiene que ver filas que su llamador todavía no commiteó, **no puede correr en una
transacción nueva**. O se une a la del llamador, o se difiere con `afterCommit`.

### 12. GreenMail no hace subdireccionamiento

El canal acepta el token en `casos-acme+NDA-XXXX@dominio`, que es lo que hacen Gmail y Microsoft 365
al entregar. GreenMail no: crea una casilla literal con el `+` adentro y el buzón real nunca recibe
nada. En `CanalCorreoIT` eso se simula como pasa de verdad — cabecera `To` con la etiqueta, sobre
SMTP apuntando al buzón base — con `Transport.send(mensaje, destinatarioReal)`.

---

### 13. El backend no resuelve los mismos hosts que el navegador

Un proveedor de identidad tiene dos URL que parecen la misma y no lo son. El `emisor` tiene que
coincidir **exacto** con el `iss` que viene firmado en el token, que es el que ve el navegador
(`http://localhost:8089/realms/...`). La `urlJwks` la baja el **backend**, que en Docker no llega a
`localhost` del host: necesita `http://keycloak:8089/...`. Poner las dos iguales da un 401 con
"no publica sus claves" y el código se ve perfecto.

Por eso son dos campos separados, y por eso el error de JWKS ahora incluye la URL que intentó y la
causa real en vez de un `null`.

---

### 14. `normalizar` un teléfono no es lo mismo según de dónde venga

Meta manda el número **sin `+`** (`5491133224455`), siempre con código de país, así que el canal
tiene que agregárselo. Pero aplicar esa misma tolerancia a lo que carga una persona es peligroso:
si alguien escribe `1133224455` pensando en un número argentino, prefijarle `+` produce un E.164
válido de **otro país**, y ese número termina en una lista blanca autorizando a quien no es.

Por eso hay dos funciones: `NumeroTelefono.normalizar` (tolerante, para lo que manda la red) y
`normalizarDeclarado` (exige `+` o `00`, para lo que carga un humano). El alta de línea y el número
de destino usan la segunda. Lo encontró un test que esperaba un rechazo y no lo recibía.

---

### 15. Responder un mensaje no autorizado cuesta plata

En el canal de email, avisarle al remitente no autorizado es apenas discutible. En WhatsApp es un
error: cada conversación que abre el negocio la factura Meta, y responderle a un número desconocido
le confirma que la línea está viva. Se ve recién cuando mirás la corrida en vivo y notás un saliente
que no debería existir.

Ahora el canal **no contesta** a un contacto fuera de la lista blanca. El mensaje sigue visible en la
bandeja con su motivo, que es lo que le importa al operador.

---

### 16. Declarar una propiedad de configuración y no usarla

Pasó dos veces: `exigirEmisorSeguro` en federación y `fallosParaPausar` en WhatsApp. Se ve prolijo en
el `application.yml`, y no hace absolutamente nada. Es peor que no tenerla, porque quien opera cree
que tiene un control que no existe.

Antes de cerrar una tarea: `grep` de cada propiedad nueva contra el código y confirmá que alguien la
lee. Si no la usa nadie, o la cableás o la borrás.


---

### 17. Borrar una constante de un enum que ya se guardó en la base

Al sacar la respuesta automática al contacto no autorizado quité también
`PlantillaWhatsapp.AVISO_CONTACTO_NO_AUTORIZADO`. El código quedó limpio, los 270 tests en verde, y
`GET /canales/whatsapp/salientes` empezó a devolver **500** en el entorno de desarrollo: había filas
con ese texto en `mensaje_whatsapp_saliente.plantilla` y Hibernate no puede mapearlas.

No lo vieron los tests porque cada uno arranca con un tenant nuevo y ninguno había escrito esa fila.
Lo vio la corrida en vivo, sobre una base que sí tenía historia. Y no rompe una fila: rompe **el
listado entero**, para siempre, para ese tenant.

Un `@Enumerated(EnumType.STRING)` es un contrato de datos. Sacarle un valor es una migración, no un
refactor: primero un `UPDATE` que reescriba las filas viejas, después el cambio de código. Si el
valor ya salió a producción, no se borra nunca.

En este caso las filas sucias son sólo del entorno local, porque el commit que crea `V16` ya no tiene
la constante: quien clone el repo arranca limpio. Para limpiar una base que corrió la versión
intermedia:

```sql
DELETE FROM mensaje_whatsapp_saliente WHERE plantilla = 'AVISO_CONTACTO_NO_AUTORIZADO';
```


---

## 5. Dónde está cada cosa

| Necesitás | Andá a |
|---|---|
| Qué está hecho y qué falta, en orden | [`TODO.md`](TODO.md) — **es el plan maestro, 32 tareas en 4 fases** |
| Bounded contexts, flujo del documento, decisiones | [`ARQUITECTURA.md`](ARQUITECTURA.md) |
| Endpoints, permisos, errores, webhooks | [`API.md`](API.md) |
| Imagen, Compose, CI y recuperación | [`DESPLIEGUE.md`](DESPLIEGUE.md) |
| Estado general y convenciones | [`../README.md`](../README.md) |

---

## 6. Por dónde seguir

La **Fase 1** cierra el producto vendible. Van 14 de 14.

**Hecho:** API de plantillas (1) · quality gate gold (2) · Gemini (3) · antivirus (4) · segmentación
(5) · matching (6) · gobernanza (7) · retención (8) · administración (9) · webhooks (10) · original
físico (11) · costo por tenant (12) · suite de QA (13) · imagen + CI (14)

De la **Fase 2** van 3 de 6: portal frontend (15), dashboard y panel de control (20) y
Archive & Export Center (19).

**Siguiente:** SSO y embed (16), Channel Gateway de email (17) y WhatsApp (18). Las tres necesitan
infraestructura externa para verificarse de verdad: un IdP OIDC, un servidor de correo y un proveedor
de WhatsApp Business. Levantalas en Docker antes de escribir el adaptador.

Cada tarea del `TODO.md` trae su criterio de aceptación con el código de QA del N3. No inventes el
criterio: está escrito.

---

## 7. Cosas administrativas

- **La API key de Gemini que se usó para probar debe rotarse.** Se compartió por chat durante el
  desarrollo. Está en `.env`, que sí está en `.gitignore` y nunca se subió — verificalo con
  `git check-ignore -v .env`.
- El repo se sube por **SSH** (`git@github.com:gabibenitezzz003/NEXTDOCS-IA.git`). HTTPS no tiene
  credenciales configuradas en la máquina original.
- `workflow` y `docvance-ai` se van a usar como **microservicios aparte**. No fusionarlos al core.
