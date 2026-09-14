# Estado de NEXT DOC AI — ¿llegamos al MVP 0?

**Fecha:** 14 de septiembre de 2026
**Para:** dirección
**Pregunta que responde:** ¿podemos poner esto delante de alguien para que lo pruebe?

---

## Respuesta corta

**Sí.**

El producto hace hoy, de punta a punta, lo que promete: alguien sube un documento, el sistema
solo decide qué tipo de documento es, le extrae los datos, los valida, marca lo que no cierra y
deja todo auditado. Eso funciona y está cubierto por pruebas automáticas contra infraestructura
real.

Lo que en la versión anterior de este documento era la salvedad principal —que nadie lo había
recorrido de punta a punta— **está resuelto**: hay un recorrido automatizado que deja una captura
por paso, y se cargaron 15 documentos con Gemini real acertando el tipo en los 15.

Al motor documental se le sumó un **motor de procesos** completo, que la versión anterior de este
documento daba por no empezado. Hoy tiene 8 controladores, 48 endpoints y 107 pruebas.

Quedan dos cosas antes de exponerlo a un cliente: **rotar la clave de Gemini** y **cargar
documentación real**. Ninguna es desarrollo.

---

## Qué se puede probar hoy

### El recorrido principal

1. Un usuario entra al portal con su usuario y contraseña de la organización.
2. Sube un documento, **sin elegir nada**: ni tipo, ni plantilla, ni formulario.
3. El sistema decide solo de qué tipo es, entre diez tipos que ya vienen cargados.
4. Le extrae los datos, los valida contra reglas y calcula una confianza por campo.
5. Si algo no cierra, abre una excepción con el motivo en castellano.
6. El revisor corrige a mano lo que haga falta, y el sistema **aprende de esa corrección**.
7. Todo queda auditado: quién, cuándo, con qué modelo y con qué prompt.

### Backend

| Capacidad | Estado |
|---|---|
| Ingesta con antivirus, tipo real por contenido y control de tamaño | Funciona |
| Detección automática del tipo de documento | Funciona |
| Captura genérica cuando el tipo no está en el catálogo | Funciona |
| Propuesta de tipos nuevos, con aprobación de un administrador | Funciona |
| Extracción con Gemini, con DeepSeek como respaldo | Funciona |
| Validación con reglas y confianza por campo | Funciona |
| Revisión humana con corrección de campos y motivo obligatorio | Funciona |
| Aprendizaje a partir de las correcciones | Funciona |
| Excepciones con SLA y prioridad | Funciona |
| Auditoría completa y reconstrucción de la decisión | Funciona |
| Multi-tenant aislado en tres capas | Funciona |
| Retención, borrado por plazo y bloqueo legal | Funciona |
| Exportación en ZIP con índice y manifiesto de hashes | Funciona |
| SSO embebido con Follow, CIMA y Valid360 | Funciona |
| Webhooks firmados, con reintentos y cola de fallidos | Funciona |
| Control de costo por tenant y presupuesto mensual | Funciona |

13 controladores REST, 40 entidades, 20 migraciones de base de datos.

### Motor de procesos

Vive en el repositorio `nextdocs-workflow`, aparte del core: apagarlo no afecta captura,
extracción ni validación.

| Capacidad | Estado |
|---|---|
| Definiciones de proceso con versionado y validador de grafo | Funciona |
| Instancias fijadas a la versión con la que nacieron | Funciona |
| Tareas, responsables, decisiones y línea de tiempo | Funciona |
| Temporizadores y SLA, heredado de la plantilla o del nodo | Funciona |
| Subprocesos, con profundidad máxima | Funciona |
| IA Supervisora: reglas sobre datos de la instancia y hallazgos | Funciona |
| Indicadores de proceso con drilldown a la población | Funciona |
| Colaboración externa con enlaces de un solo uso | Funciona |

8 controladores REST, 48 endpoints, 16 entidades, 11 migraciones.

### Frontend

| Pantalla | Estado |
|---|---|
| Ingreso | Funciona |
| Resumen | Funciona |
| Bandeja de documentos, con el tipo detectado | Funciona |
| Visor del documento con sus datos extraídos | Funciona |
| Excepciones | Funciona |
| Panel de control con indicadores | Funciona |
| Tipos nuevos que encontró el sistema | Funciona |
| Procesos: definiciones, estudio y publicación | Funciona |
| Procesos: bandejas de instancias y tareas, con indicadores | Funciona |
| Procesos: reglas de la IA Supervisora | Funciona |

### Qué prueba lo anterior

- Core: **121 pruebas unitarias** ejecutadas y **175 de integración**, en 17 escenarios completos.
- Procesos: **107 pruebas**.
- Interfaz: **67 pruebas de navegador** en tres suites (36 de procesos, 28 transversales y 3 de
  humo contra los servicios levantados).
- Las de integración **no usan simulaciones de base de datos**: corren contra PostgreSQL, Redis,
  MinIO y Keycloak de verdad, levantados en contenedores.
- Cubren los casos negativos, no sólo el camino feliz: documento infectado, archivo que miente
  sobre su tipo, confianza insuficiente, dos candidatos de asociación, aislamiento entre
  organizaciones, orden de bloqueo legal.

---

## Qué NO se puede probar todavía

Esto es tan importante como lo anterior, y conviene decirlo antes de una demo y no durante.

**Canales de entrada por email y WhatsApp.** Se construyeron y se retiraron por decisión de
producto: hoy no se usan y la interfaz confundía más de lo que ayudaba. El código está en el
historial y se puede recuperar cuando haya una integración concreta que lo justifique. **Hoy los
documentos entran por el portal o por la API.**

**Volumen real.** Nunca se corrió una prueba de carga. No sabemos cómo se comporta con miles de
documentos por día. Es el P0-13 y sigue abierto.

**Documentación real de un cliente.** Se cargó un dataset propio de 15 documentos argentinos con
respuesta conocida (ver abajo), pero documentación real de un cliente todavía no.

---

## Qué falta para poder probar

Eran cuatro puntos. Quedan uno y medio. El detalle está en
[P0_02_VALIDACION.md](P0_02_VALIDACION.md).

**1. Recorrer la aplicación en el navegador · hecho.**
`pruebas-transversales/recorrido.spec.ts` recorre entrar → subir → ver el tipo detectado → abrir
el visor → corregir un campo → aprobar → resolver una excepción → mirar el panel, y deja una
captura por paso. Verifica el cuerpo real de cada petición y que no haya errores de consola ni
endpoints inesperados. No reemplaza mirar la aplicación con datos de un cliente, pero convierte
la lista de chequeo en algo que falla solo si una pantalla se rompe.

**2. El visor muestra los datos de la captura genérica · confirmado.**
Los muestra, y mejor de lo que suponíamos: además marca el documento con una pastilla y una nota
que explica por qué cayó al esquema de respaldo, y la bandeja los distingue sin abrirlos. Cubierto
por cinco pruebas.

**3. Cargar documentación · hecho con dataset propio, falta la del cliente.**
Se cargaron 15 documentos con **Gemini 2.5 Flash real** (no el simulado): 15/15 cargados y 15/15
con el tipo correcto, con confianza entre 0.95 y 1.00. Incluye un manual de AFIP de 32 páginas —un
PDF real, no generado— que cayó al esquema genérico como correspondía.

Los documentos los genera `scripts/generar-dataset.mjs` con respuesta conocida, y
`scripts/cargar-dataset.mjs` mide el acierto. Tienen formato válido pero datos ficticios: **no
reemplazan documentación real de un cliente**, que sigue siendo lo que va a decir si esto sirve
de verdad.

**4. Rotar la clave de Gemini · pendiente, decisión de dirección.**
La clave actual se compartió por chat durante el desarrollo. Nunca se subió al repositorio, está
verificado en cada commit, pero hay que rotarla antes de que esto toque datos de un cliente.

### Lo que apareció haciendo lo anterior

La validación del dígito verificador del CUIT **no existía**. El catálogo declaraba la regla sobre
seis tipos documentales, pero estaba definida de una forma que no podía disparar nunca, y no había
ninguna función que calculara un dígito verificador en el backend. Una factura con un CUIT
inventado llegaba a `VALIDADO` y podía autoaprobarse. Está corregido y cubierto por 20 pruebas.

Es exactamente el tipo de cosa que este ejercicio tenía que encontrar, y solo apareció al cargar
documentos con defectos plantados a propósito.

---

## Cómo se levanta para probar

```bash
docker compose up -d                          # PostgreSQL, Redis, MinIO, Keycloak
docker compose --profile app up -d --build    # la aplicación
cd frontend && npm run dev                    # el portal
```

Entra en `http://localhost:5175`. El detalle completo está en
[`EMPEZAR_ACA.md`](EMPEZAR_ACA.md).

Por defecto usa un proveedor de IA **simulado**, que sirve para recorrer la aplicación sin gastar
dinero ni depender de internet. Para probar con inteligencia real hay que poner
`NEXTDOCS_IA_PROVEEDOR=GEMINI` y una clave. El adaptador de Gemini está verificado contra la API
real de Google.

---

## Riesgos y deuda que conviene tener presente

**El aprendizaje y la detección automática son nuevos.** Se terminaron esta semana. Tienen
pruebas y verificación contra la API, pero poco kilometraje. Es donde más esperaría encontrar
sorpresas con documentos reales.

**Las pruebas pueden pasar en falso si la infraestructura está caída.** Lo detectamos y ya está
resuelto: hay que correrlas con `NEXTDOCS_PRUEBA_OBLIGATORIA=true`, que hace fallar en vez de
saltear. Está documentado y la integración continua ya lo usa. Lo menciono porque durante un rato
creímos tener todo en verde y no era cierto.

**Las pruebas de interfaz usan la API simulada.** Son 67 con Playwright y cubren el recorrido
completo, pero interceptan las llamadas en vez de pegarle al backend. Detectan que la pantalla se
rompa; no detectan que el backend cambie un contrato. Las 3 de humo sí pegan contra los servicios
levantados.

**Una regla mal declarada no avisa.** Lo del CUIT no fue un descuido aislado: una regla puede
quedar guardada, visible y sin poder disparar nunca. Lo mismo pasaba con las reglas de la
Supervisora sin umbral, corregido en su momento. Conviene revisar el resto del catálogo con la
misma sospecha.

**El catálogo de tipos es argentino.** Remito, factura, notas de crédito y débito, constancia de
inscripción, DNI, licencia, cédula, VTV y seguro. Para otro país hay que armar el catálogo, que
es carga de datos, no desarrollo.

---

## Conclusión

El MVP 0 está construido **y verificado**. La versión anterior de este documento pedía confirmarlo
con los ojos antes de mostrarlo: eso ya se hizo, y de paso apareció el defecto del CUIT, que es
justamente lo que se buscaba.

Queda una cosa que es decisión y no desarrollo —**rotar la clave de Gemini**— y una que depende de
conseguir el material: **documentación real de un cliente**. Esto último es lo único que todavía
puede cambiar la conclusión, porque el dataset propio, por más que tenga formato válido, lo
armamos nosotros.

Para producción, lo que falta no es funcionalidad sino operación: prueba de carga, backup y
restore probados, y runbooks. Es el P0-13.
