# Estado de NEXT DOC AI — ¿llegamos al MVP 0?

**Fecha:** 5 de septiembre de 2026
**Para:** dirección
**Pregunta que responde:** ¿podemos poner esto delante de alguien para que lo pruebe?

---

## Respuesta corta

**Sí, con una salvedad importante.**

El producto hace hoy, de punta a punta, lo que promete: alguien sube un documento, el sistema
solo decide qué tipo de documento es, le extrae los datos, los valida, marca lo que no cierra y
deja todo auditado. Eso funciona y está cubierto por pruebas automáticas contra infraestructura
real.

La salvedad es que **todavía no lo miró un ojo humano de punta a punta en el navegador**. El
backend está verificado con 247 pruebas automáticas y con pruebas manuales contra la API. La
interfaz compila, se navega y muestra datos reales, pero no hicimos un recorrido completo de
usuario mirando la pantalla. Es lo primero que hay que hacer antes de sentar a alguien a probar,
y son horas, no semanas.

**Recomendación:** cerrar los cuatro puntos de la sección "Qué falta para poder probar" antes de
la primera demo. Ninguno es una funcionalidad nueva; son verificación y pulido.

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

### Frontend

| Pantalla | Estado |
|---|---|
| Ingreso | Funciona |
| Resumen | Funciona |
| Bandeja de documentos, con el tipo detectado | Funciona |
| Visor del documento con sus datos extraídos | Funciona |
| Excepciones | Funciona |
| Panel de control con indicadores | Funciona |
| Tipos nuevos que encontró el sistema | Recién hecho, sin recorrer en pantalla |

### Qué prueba lo anterior

- **73 pruebas unitarias** y **174 de integración**, en 17 escenarios completos.
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

**Workflow y procesos.** Todo lo que es circuito de aprobación, tareas, notificaciones y firma es
Etapa 2 y no está empezado. El sistema hoy es documental, no de procesos.

**Volumen real.** Nunca se corrió una prueba de carga. No sabemos cómo se comporta con miles de
documentos por día.

**Datasets reales.** El motor de calidad existe y funciona, pero se probó con documentos de
prueba armados por nosotros, no con documentación real de un cliente.

---

## Qué falta para poder probar

Cuatro cosas, ninguna es funcionalidad nueva.

**1. Recorrer la aplicación en el navegador, de punta a punta.**
Un recorrido completo mirando la pantalla: entrar, subir, ver el tipo detectado, abrir el visor,
corregir un campo, resolver una excepción, mirar el panel. Es la verificación que falta y es la
que más riesgo saca de encima.

**2. Confirmar que el visor muestra los datos de la captura genérica.**
Sabemos que los datos **se guardan** — hay una prueba automática que lo verifica. Lo que no
confirmamos es que el visor los muestre cuando el documento entró por captura genérica. Si no lo
hace, el usuario va a ver un documento aparentemente vacío y va a concluir que no funciona.

**3. Cargar documentación real.**
Diez o quince documentos de verdad de un cliente, de tipos variados. Es lo único que va a decir
si la detección automática y la extracción sirven en serio.

**4. Rotar la clave de Gemini.**
La clave actual se compartió por chat durante el desarrollo. Nunca se subió al repositorio, está
verificado, pero hay que rotarla antes de que esto toque datos de un cliente.

**Estimación honesta:** los puntos 1, 2 y 4 son de horas. El punto 3 depende de conseguir la
documentación, y es el que más valor tiene.

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

**No hay pruebas automáticas de interfaz.** Hay un plan escrito para hacerlas con Playwright, sin
implementar. Mientras no existan, cada cambio de pantalla se verifica a mano.

**El catálogo de tipos es argentino.** Remito, factura, notas de crédito y débito, constancia de
inscripción, DNI, licencia, cédula, VTV y seguro. Para otro país hay que armar el catálogo, que
es carga de datos, no desarrollo.

---

## Conclusión

El MVP 0 está **construido**. Lo que falta es **confirmarlo con los ojos** y con documentación
real, no seguir construyendo.

Mi recomendación concreta: dedicar la próxima sesión al recorrido completo en el navegador y a
cargar documentos reales. Si eso sale bien, esto se puede mostrar. Si sale mal, va a salir mal
ahora y no delante de un cliente.
