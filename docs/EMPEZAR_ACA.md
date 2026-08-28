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
cd backend && ./mvnw spring-boot:run
```

Arranca el tenant `demo` con `admin@nextdocs.ai` / `nextdocs123`.
Swagger en `http://localhost:8090/swagger-ui.html`.

**Sin JDK local** (fue el caso en la máquina donde se construyó):

```bash
docker run --rm --network host --env-file ../.env \
  -v "$PWD":/app -v nextdocs-m2:/root/.m2 -w /app \
  maven:3.9-eclipse-temurin-21 mvn spring-boot:run
```

Tests: `./mvnw verify` con la infra levantada. 41 unitarios + 29 de integración.

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
decidir. El `factorCalibracionConfianza` es un ajuste provisorio a mano: la calibración de verdad
sale del dataset gold de la **tarea 2**, que está pendiente.

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

---

## 5. Dónde está cada cosa

| Necesitás | Andá a |
|---|---|
| Qué está hecho y qué falta, en orden | [`TODO.md`](TODO.md) — **es el plan maestro, 32 tareas en 4 fases** |
| Bounded contexts, flujo del documento, decisiones | [`ARQUITECTURA.md`](ARQUITECTURA.md) |
| Endpoints, permisos, errores, webhooks | [`API.md`](API.md) |
| Estado general y convenciones | [`../README.md`](../README.md) |

---

## 6. Por dónde seguir

La **Fase 1** cierra el producto vendible. Van 7 de 14.

**Hecho:** API de plantillas con ciclo de vida (1) · adaptador Gemini real (3) · antivirus con
cuarentena (4) · segmentación de PDF (5) · matching + FollowConnector (6) · original físico (11) ·
suite de QA (13)

**Siguiente, en este orden:**

1. **Tarea 7** — API de gobernanza y exportación de auditoría (`GOV-01`).
   Ya arreglamos la consulta que la sostiene, el camino está limpio.
2. **Tarea 8** — job de retención y legal hold (`GOV-02`)
3. **Tarea 9** — administración de tenant, usuarios y cuentas de servicio.
   **Desbloquea el frontend.**
4. **Tarea 10** — API de suscripciones de webhook y monitor de integraciones
5. **Tarea 2** — quality gate con dataset gold. Acá va la calibración real de la confianza
6. **Tarea 12** — observabilidad y costo por tenant
7. **Tarea 14** — Dockerfile, CI y despliegue reproducible

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
