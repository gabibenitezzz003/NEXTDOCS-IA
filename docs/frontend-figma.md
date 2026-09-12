# Frontend y Figma

## Alcance

FASE 1 quedó cerrada con las fundaciones de `1d2ae93` y las primitivas de `c53cfcc`.
FASE 2 quedó aceptada en `dd52b90` con el shell y la navegación. FASE 3 quedó aceptada en
`9b1fdd5` con Login y restauración de sesión. FASE 4 quedó aceptada en `74437b8` con Resumen.
FASE 5 trabaja únicamente la presentación de `/documentos`, conservando consultas, filtros,
búsqueda, paginación, carga individual y apertura del visor existente.

Antes de editar se preservó el trabajo incompleto de CHECKPOINT 2:

```text
stash@{0} 2a7597caa4a46330a90f8c6569b6ae26de334292
wip: checkpoint2 antes de realineacion Product UX
```

No aplicar ni eliminar ese stash durante esta fase.

## Fuentes de verdad

| Aspecto | Fuente primaria | Límite |
|---|---|---|
| Funcionalidad y negocio | Código, backend, contratos API y permisos reales | Ningún mockup cambia comportamiento |
| Producto, UX y roadmap | [Product UX CEO](https://www.figma.com/design/DerqvPxJwtevNP1lcvVpeo) | La composición no crea rutas ni endpoints |
| Sistema visual específico | [Design System oficial NEXT DOC AI](https://www.figma.com/design/0VZRK69QjDTDy0oAaf8Uv1) | Tokens, tipografía, estados y componentes |
| Referencia corporativa | Follow Design system | Reutilización cuando corresponda; no sobrescribe automáticamente NEXT DOC AI |
| Estados y responsive | Design System NEXT DOC AI cuando tenga cobertura | Código para restricciones; Product UX para jerarquía |

Product UX manda en arquitectura, navegación conceptual y composición. El Design System de
NEXT DOC AI manda en tokens, componentes, tipografía y tratamiento visual salvo contradicción
demostrable. El código manda siempre en acciones y estados del negocio. Ambos archivos fueron
inspeccionados con Figma MCP mediante metadata, screenshots, contexto y propiedades de nodos.

Product UX contiene frames conceptuales, patrones genéricos y datos ilustrativos. No es un
handoff pixel-perfect ni una fuente de datos de producción. Las coordenadas absolutas son
referencia visual; la implementación futura usará flex, grid, responsive y componentes
reutilizables.

## Follow Design system

Se verificaron nombres y component keys por MCP, entre ellos:

| Componente | Component key | Candidato React |
|---|---|---|
| Desktop/Light/Botón Grande | `abba22564cb911b0b0486f251bda34905943f83a` | `Boton` |
| Desktop/Dark/Botón Grande | `95219406befb71a767775c17ccbc88d2d525c298` | `Boton` |
| Desktop/Campo de texto | `8b174f75b72d0096f13c5d03f184e07df7830954` | `Campo` |
| Desktop/Busqueda | `e95345c2b517721a147d2c4599381f2c1c1e5414` | Búsqueda existente |
| Desktop/Chip | `f648551cfa18a74db1170fe92273490cfd0c074a` | `Pastilla`, `InsigniaEstado` |
| Mobile/Elementos barra Nav | `0ebdd83edfaf63f13af44b387eca2dfaac9320bc` | Navegación futura |

Code Connect no permitió leer propiedades internas por la restricción de plan/asiento. Por
eso no se inventan variantes, tamaños ni estados de Follow y no se afirma «validado contra
Follow». La correspondencia corporativa queda **PENDIENTE DE VALIDACIÓN CORPORATIVA**; no bloquea
esta corrección basada en evidencia directa del Design System específico.

## Fundaciones y tokens

`frontend/src/estilos.css` sigue siendo la entrada y conserva la separación. `tokens.css` ahora
separa paleta base, semántica, compatibilidad, componentes y layout. `tipografia.css` separa
roles funcionales. Los tokens nuevos no se aplican todavía a páginas.

### Confirmados por NEXT DOC AI

La fuente es Foundations `123:15067` y Spacing/Radius/Shadows/Grid `123:15380`.

| Grupo | Valores |
|---|---|
| Grafitos | `#0D0F12`, `#15181E`, `#1A1D23`, `#262A33`, `#9AA1B1` |
| Violeta | `#6C38FF`, `#5A26F0`, `#8A63FF`, `#F0ECFF`, `#DDD3FF` |
| Rojo | `#FF1E1E`, `#D81212`, `#FFECEC`, `#FFD0D0` |
| Fondo/superficie | `#F5F6F8`, `#FFFFFF`, arena `#F4E9DE` |
| Texto/borde | `#0F1116`, `#3D4453`, `#6B7285`, `#9AA1B1`, `#E6E9EF`, `#D3D8E2` |
| Semánticos | éxito `#0F9D58`, alerta `#C2760A`, info `#1D6FE0` y sus fondos documentados |
| Espaciado | 4, 8, 12, 16, 20, 24, 32, 40, 48, 64, 80, 96 px |
| Focus | anillo violeta, grosor y offset 2 px |

No se sustituyen estos valores por los de un frame Product UX. En particular, el violeta
continúa siendo `#6C38FF`, no `#6C35FF`; el lienzo continúa siendo `#F5F6F8`, no `#F8FAFC`.

### Semánticos y componentes

Se añadieron referencias funcionales: superficie, navegación, acción primaria, foco y textos
de navegación. Los controles documentan 34/42/50 px; el botón inspeccionado `123:17392` mide
42 px y tiene radio 12 px. Los radios funcionales quedan separados:

| Token | Valor | Estado |
|---|---:|---|
| `--radius-control` | 12 px | Confirmado por el Design System |
| `--radius-metrica` | 14 px | Objetivo provisional observado en UX-05 `2:321` |
| `--radius-panel` | 16 px | Objetivo provisional observado en UX-05 `2:337`, `2:341`, `2:361` |
| `--radius-panel-lateral` | 0 px | Drawer del Design System `123:20582` |
| `--radius-tarjeta` / principal | 20 / 24 px | Variantes del Design System, no universales |
| `--radius-insignia` | pill | Tratamiento de insignia |

La anatomía de drawer `123:20581` usa 18 px en su contenedor ilustrativo; el drawer operativo
`123:20582` es recto. Se conservan también sombras existentes y las cuatro sombras medidas del
Design System: superficie 0/4/16, elevada 0/10/30, lateral 0/18/48 y glow violeta 0/6/24.
Los bordes y sombras de Product UX no se convierten en reglas globales sin migrar sus superficies.

### Layout

Product UX UX-05 usa sidebar 220 px y barra superior 64 px. Se definen como objetivos futuros:
`--layout-sidebar-desktop: 220px` y `--layout-topbar-height: 64px`. El shell actual conserva
`--ancho-barra-lateral: 248px`, 64 px compactos, margen 32 px, gutters 24 px y grilla de 12
columnas como referencia de compatibilidad de FASE 1. FASE 2 aplica los objetivos de 220/64
al shell mediante sus tokens; ya no utiliza 248 px como ancho efectivo de la sidebar.

## Tipografía

La fuente es Foundations Typography `123:15330`. Se mantienen Inter y Space Grotesk; Roboto Mono
sigue siendo fallback/objetivo y no se incorpora ninguna fuente binaria.

| Rol | Familia/peso | Tamaño / línea | Estado |
|---|---|---|---|
| `text-titulo-pagina` | Space Grotesk 700 | 28 / 36 | 28 px medidos en UX-05; línea provisional porque Figma usa AUTO |
| `text-titulo-seccion` | Space Grotesk 700 | 22 / 28 | Escala confirmada |
| `text-titulo-panel` | Space Grotesk 700 | 17 / 24 | Escala confirmada |
| `text-titulo-destacado` | Space Grotesk 700 | 32 / 40 | Escala confirmada, sólo destacado |
| `text-metrica-compacta` | Space Grotesk 700 | 22 / 28 | UX-05; línea provisional |
| `text-metrica-destacada` | Space Grotesk 700 | 56 / 64 | Escala display documentada; provisional porque la muestra mide 48 px |
| `text-cuerpo` / medio | Inter 400 / 500 | 15 / 22 | Confirmado |
| `text-pequeno` | Inter 400 | 13 / 18 | Confirmado |
| `text-micro` | Inter 600 | 10 / 14 | Confirmado |
| `text-codigo` | Roboto Mono/fallback 400 | 13 / 18 | Objetivo del sistema |

No se usa 56/64 como KPI normal y no se impone 32/40 a todos los H1. La base de `body` sigue
siendo 14 px y no cambian las medidas efectivas de las páginas existentes.

## Catálogo documental

`ESTADOS_DOCUMENTALES` y el cambio de `Insignias.tsx` de `3c7bfab` se conservan. El catálogo
contiene sólo estados reales, tonos y colores; no define permisos, decisiones ni transiciones.
El grupo Document Status `123:17548` respalda neutral para recibido/dividido/cerrado, info para
procesando/extraído, violeta para validado, alerta para observado, éxito para aprobado y rojo
para rechazado. `NO_FIGURA` e `ILEGIBLE` siguen siendo distintos.

## Alcance funcional inmediato

El núcleo visual futuro se limita a superficies que ya tienen implementación funcional suficiente:
`/ingresar`, `/resumen`, `/documentos`, visor documental, `/excepciones`, `/panel`,
`/tipos-propuestos` y `/procesos` existente. `/excepciones` y `/panel` siguen siendo superficies
reales aunque Product UX no las represente individualmente con precisión.

UX-07, UX-08, UX-13, UX-15, UX-16, UX-17 global, UX-18, UX-19 y UX-20 no aparecen como pantallas
nuevas. MVP1–MVP4 (Canvas, Partners, Marketplace, licencias, Sentinel, Simulation Lab, Copilot,
Enterprise, gobernanza y recuperación) son roadmap y quedan fuera.

Workflow es otro servicio, no el core documental. Sus APIs, permisos y hallazgos no se mezclan
con los del core. No se inventan endpoints para botones conceptuales como «Guardar corrección»:
el backend actual lleva `CORREGIR` a `OBSERVADO` y no tiene un guardado neutral.

## Responsive y estados

La página `90 · Responsive & States` de Product UX estaba vacía. La referencia principal es el
Design System: 1280 `123:9956`, 1024 `126:3616`, 768 `123:9587`/`123:9715`, móvil 390
`123:9732`/`123:9813`/`123:9885`. Son puntos de revisión, no nuevos breakpoints. Estados normal,
hover, pressed, focus, disabled y loading están en `123:17332`; contenido loading/empty/error
en `123:20965`; denied/read-only en `123:6759`/`123:6889`. El código conserva las restricciones
funcionales cuando un frame omite acciones, permisos o campos.

## Verificación y siguientes fases

El gestor es npm y no se instalaron dependencias. Se ejecutan desde `frontend/`:
`npm run build` y `npm run test:e2e`; desde la raíz, `git diff --check` y `git status --short
--branch`. Los smoke sólo cubren health y Workflow con tenant; no certifican portal, permisos,
accesibilidad, fidelidad visual ni Follow.

El stash continúa intacto. FASE 2 fue aceptada en `dd52b90`. FASE 3 comprende únicamente Login
y la presentación de la restauración de sesión. Resumen, Documentos y las demás superficies
requieren autorización independiente para su rediseño interno.

## FASE 2 — Application shell y navegación

La implementación se concentra en `Disposicion.tsx`. `Navegacion` se comparte entre la barra
lateral y el drawer; ambos reciben los mismos grupos filtrados. `IdentidadLateral` comparte
la presentación del usuario y su organización. No se modifica `Aplicacion.tsx` ni se crean rutas.

| Destino | Permiso conservado |
|---|---|
| `/resumen`, `/documentos`, `/panel` | `documentos.leer` |
| `/excepciones` | `excepciones.leer` |
| `/tipos-propuestos` | `tenant.administrar` |
| `/procesos` | `plantillas.publicar` |

Los grupos vacíos se omiten. No se cambian los permisos de los endpoints ni se agregan guards.
`NavLink` conserva la coincidencia por segmento de ruta y genera `aria-current="page"` para
el módulo activo. El contexto de la topbar usa la misma tabla y reconoce prefijos delimitados
por `/`, sin confundir nombres de rutas que sólo coincidan parcialmente.

### Composición y decisiones de diseño

- Desde 80 rem, sidebar completa de `--layout-sidebar-desktop` (220 px). Marca, grupos
  Operación/Análisis/Configuración e identidad al pie. El activo usa violeta sólido del sistema.
- Entre 48 y 80 rem, navegación compacta de `--ancho-barra-lateral-compacta` (64 px), con
  nombres accesibles y títulos de enlaces. Los seis destinos siguen disponibles según permisos.
- Por debajo de 48 rem, se retira la sidebar permanente. Un botón abre el drawer con todos los
  destinos permitidos. Los umbrales se eligen por espacio disponible, no como copia de frames.
- La topbar mide `--layout-topbar-height` (64 px) y permanece visible durante el scroll.
  Muestra módulo, organización disponible y acceso al usuario. No hay cifras ni perfiles ficticios.
- El menú de usuario permite Tab, Enter/Espacio, Escape, cierre explícito y clic exterior.
  Es un desplegable con botones nativos, sin atribuirle el patrón ARIA de menú de aplicación.
  Al cerrar mediante Escape o el botón, el foco vuelve al disparador. Logout conserva
  exactamente `salir()` y la navegación a `/ingresar`.
- El drawer de navegación usa `dialog.showModal()`: el navegador gestiona modalidad, foco y
  Escape. Cierra al seleccionar un destino o pasar al breakpoint de navegación permanente.
  Su semántica y apertura lateral difieren del `Panel` documental; no se modifica esa primitiva.
- Grid con columnas `minmax(0,1fr)` y contenido con desbordamiento horizontal local permite
  que las tablas preexistentes sigan desplazándose. La altura crece con el contenido, sin 900 px
  fijos. `Encabezado` deja de ser otra barra sticky para no competir con la topbar; `Contenido`
  y `Encabezado` usan padding menor en móvil. Sus props y el contenido de páginas se conservan.

Evidencia: Product UX `2:291` para composición 220/64; Design System `126:3616` para navegación
compacta, `123:9715` para apertura lateral y `123:9813` para móvil. El drawer conceptual de
768 tiene texto genérico y la barra inferior de 390 sólo incluye tres destinos: se conserva
la dirección visual, pero se usa el inventario real completo. No se agregan Tareas, Studio,
Integraciones, Sentinel ni otros módulos conceptuales.

Los colores permanecen en el Design System NEXT DOC AI: grafito `#0D0F12`, violeta `#6C38FF`,
lienzo `#F5F6F8`, bordes y superficies existentes. No se copian `#6C35FF` ni `#F8FAFC` de UX-05.
No se agregan tokens, fuentes ni dependencias de JavaScript. La instalación autorizada de Chromium
usa el mecanismo de Playwright existente y no cambia `package.json` ni el lockfile.

### Verificación del shell

La comparación visual usa Chromium con respuestas de sesión y datos interceptadas únicamente
en el navegador de prueba. No representa datos de producción ni valida el backend. Se revisan
1440, 1024, 768 y 390 px, además de permisos reducidos, nombres largos, scroll y cierre de menús.
Los smoke HTTP de `npm run test:e2e` siguen dependiendo del Workflow real en `localhost:8091`;
su resultado se informa por separado. Esta fase no certifica WCAG completo ni validación de Follow.

La verificación en Chromium pasó en los cuatro anchos: topbar de 64 px, sidebar de
220/64/64/0 px respectivamente, seis destinos navegables, estado activo, menú de usuario,
Escape, retorno de foco, scroll largo y logout. No hubo desbordamiento horizontal del documento
en los escenarios revisados. También pasaron las sesiones con tres, uno y cero destinos
permitidos, organización larga y cierre del drawer al ampliar el viewport.

`npm run build` y `git diff --check` pasaron. `npm run test:e2e` tuvo dos fallos de conexión:
`ECONNREFUSED ::1:8091` y `ECONNREFUSED 127.0.0.1:8091`, al consultar health y procesos de
Workflow. E2E no está verde: debe repetirse con ese servicio disponible antes del PR. Este
resultado no demuestra una regresión del shell ni de las fundaciones de FASE 1.

## FASE 3 — Autenticación y Login

Se inspeccionaron mediante Figma MCP el contexto de diseño y las imágenes de UX-01 (`2:2`)
y los estados del Design System NEXT: normal `40:2398`, error `40:2473`, cargando `40:2556`,
móvil `40:2632` y restauración `40:2663`.

UX-01 es conceptual: se conserva la intención de marca, pero el login público no incorpora
su sidebar, topbar, usuario ficticio ni botones «Continuar». El contrato real exige Organización,
Email y Clave. Se mantienen el valor inicial `demo`, email y clave vacíos, campos obligatorios,
validación nativa de email, recorte de espacios en organización/email y clave sin transformación.
El endpoint, payload, proveedor de sesión, almacenamiento, refresh y destino `/resumen` no cambian.

`Ingresar.tsx` usa una composición fluida de dos columnas desde 64 rem, con fondo grafito,
marca y contenido institucional existente. Por debajo se prioriza el formulario en una columna
con marca y pie visibles. El formulario tiene un máximo de 420 px, controles del sistema de
42 px y acción principal de 50 px. La pantalla crece y permite scroll en viewports bajos.

Se reutilizan `Campo`, `Boton`, `Logotipo` y `ErrorPanel`. El único ajuste de la primitiva de
error agrega `titulo` opcional, con «No se pudo cargar» como valor por defecto; Login utiliza
«No pudimos iniciar sesión». Los consumidores anteriores mantienen su presentación y contrato.
El mensaje sigue saliendo de `mensajeDeError`, incluidos los detalles de campos del backend.

`Marca.tsx` asigna IDs de degradado únicos mediante `useId`. Las instancias simultáneas del
hero y del encabezado móvil usaban los mismos IDs SVG: cuando el hero estaba oculto, Chromium
dejaba vacío el isotipo móvil. Sólo se corrigen IDs y referencias; el dibujo, los colores y las
props de la marca permanecen iguales.

El formulario tiene nombre y descripción accesibles. El error usa `role="alert"` y queda
asociado al formulario por `aria-describedby`. Se reserva espacio para el feedback habitual;
los mensajes extensos pueden crecer y nunca se recortan. No se marca una clave como inválida
por un error general de red o sesión: la validación de campos continúa siendo la nativa del
navegador, sin inferir errores específicos a partir de un texto genérico.

Durante submit se deshabilitan los campos, `Boton` utiliza `cargando` y el formulario expone
`aria-busy`. Una referencia evita solicitudes duplicadas, incluso ante eventos consecutivos
antes del siguiente render. El estado se anuncia fuera del formulario ocupado. No hay overlay.
Email usa `autocomplete="email"` y Clave `current-password`; el código de organización usa
`off` para evitar tratarlo como nombre comercial. No se agrega mostrar contraseña ni recuperación.

En `Aplicacion.tsx` sólo cambia el JSX de la rama `cargando`: tarjeta de marca, indicador
indeterminado decorativo y «Restaurando sesión…» con `role="status"`. Se conserva la decisión
previa al routing; mientras se restaura no se renderizan Login ni el shell autenticado.

Las diferencias deliberadas con el Design System son la marca existente del producto, la
omisión de grilla/resplandores decorativos, el acento violeta sólido y las medidas consolidadas
de las primitivas. El título del formulario usa 32/40 en desktop y 28/36 en móvil. El hero adapta
los 52 px del frame al espacio disponible. El botón conserva 50 px para la acción táctil, frente
a 42 px del frame; los campos usan 42 px en lugar de copiar instancias genéricas de 66 px.
La restauración utiliza radio y sombra de `Tarjeta`, sin inventar un porcentaje de avance.
No se agregan tokens, fuentes ni dependencias. No se declara pixel-perfect ni validación de Follow.

### Verificación de autenticación

Chromium verificó 1440, 1024, 768 y 390 px con respuestas HTTP interceptadas en el navegador:
tres campos y valores iniciales, validación required/email, Tab, Enter, foco visible, payload
con los mismos recortes, bloqueo de envíos duplicados, inputs deshabilitados y dimensiones
estables durante loading y error habitual. Se comprobaron mensajes generales y detalles de
campos del contrato, conservación de valores y scroll con una altura de 480 px. No hubo
desbordamiento horizontal del documento en esos escenarios.

La restauración se verificó en 1440 y 390 px reteniendo la respuesta de refresh: se muestra el
estado de marca sin formulario ni navegación autenticada. Al responder 401 se conserva el flujo
existente hacia `/ingresar`. No se simuló un éxito como prueba de integración. El backend real
en `127.0.0.1:8090` no estaba disponible, por lo que el login exitoso real queda pendiente.

`npm run test:e2e` falló en sus dos smoke por `ECONNREFUSED ::1:8091` y
`ECONNREFUSED 127.0.0.1:8091`. Workflow debe estar disponible para repetir la suite antes del PR;
E2E no está verde y esos errores de conexión no demuestran un fallo introducido por Login.

`npm run build` y `git diff --check` pasaron. FASE 3 termina aquí, sin iniciar Resumen ni
Documentos y sin aplicar o eliminar el stash de CHECKPOINT 2.

## FASE 4 — Resumen operativo

Se inspeccionaron contexto de diseño e imágenes con Figma MCP: Product UX `2:39`; Design
System normal `40:3580`, vacío `40:3724`, cargando `126:3368` y menú de usuario `40:3801`.
El shell aceptado permanece intacto; la identidad del tenant y el menú siguen siendo suyos.

### Fuentes funcionales y poblaciones

`obtenerResumen()` consulta `GET /api/v1/documentos/resumen` con query key `["resumen"]`.
El frontend recibe `Record<string, number>`; `DocumentoRestController.resumen()` exige
`documentos.leer` y delega en `DocumentoService.resumenPorEstado(tenantId())`.
`DocumentoRepository.contarPorEstado` cuenta documentos del tenant con `baja IS NULL`, por
cada estado del enum. No filtra sólo documentos raíz ni aplica una ventana temporal.

| Dato visible | Fuente y cálculo conservados |
|---|---|
| Documentos totales | Suma de entradas del resumen, excluyendo `profundidadCola` y `profundidadReintento` |
| Requieren revisión | `Number(datos.OBSERVADO ?? 0)` |
| Aprobados | `Number(datos.APROBADO ?? 0)` |
| Recibidos | `Number(datos.RECIBIDO ?? 0)` |
| Rechazados | `Number(datos.RECHAZADO ?? 0)` |
| Porcentaje de cada destacado | `Math.round(parte / total * 100)`; con total cero, `0% del total` |
| Cola de extracción | `Number(datos.profundidadCola ?? 0)`, mostrada como detalle del total |
| Distribución | Las mismas entradas documentales, ordenadas por cantidad descendente |

La cola viene de `ColaExtraccionService.profundidad()`: tamaño de la lista Redis configurada
para el servicio, sin filtro por tenant. Se conserva el dato y se aclara «del servicio»; no se
presenta como otra población documental. La tarjeta antes titulada «En cola» pasa a «Recibidos»
porque cuenta exactamente `RECIBIDO`, no la profundidad Redis. `profundidadReintento` sigue
excluida del total y del gráfico, sin agregar un KPI nuevo.

Las excepciones mantienen query key `["excepciones", "ABIERTA", 0]`, función
`listarExcepciones("ABIERTA", 0, 5)` y `enabled: tienePermiso("excepciones.leer")`.
`GET /api/v1/excepciones?estado=ABIERTA&pagina=0&tamano=5` devuelve `Pagina<Excepcion>`.
El backend filtra tenant, `baja IS NULL` y estado, y ordena por prioridad descendente y alta.
Se muestran los mismos cinco elementos como máximo, con tipo, severidad y detalle; no se
convierte el tamaño de esa página en un KPI global ni se incluyen excepciones `EN_CURSO`.

### Presentación y decisiones

- Cinco tarjetas estáticas usan `Tarjeta`, `Metrica` compacta e `InsigniaEstado`. `OBSERVADO`
  tiene énfasis ámbar. Desaparecen auras, elevación al hover y contadores animados; se muestran
  las cantidades recibidas sin pasar por ceros animados.
- Desde 80 rem se muestran cinco KPI en una fila y dos paneles de operación. Entre 40 y
  80 rem, el total ocupa ambas columnas y los otros cuatro KPI forman una grilla de dos por dos.
  En móvil se apilan. Los paneles quedan en una columna hasta 80 rem y crecen con su contenido.
- Se mantienen exclusivamente los enlaces existentes a `/panel` y `/excepciones`. No hay
  links en KPI estáticos ni CTA de carga, por lo que no se ofrece subida a usuarios sin permiso.
- `ESTADOS_DOCUMENTALES` es la única fuente de etiquetas y colores documentales. Se eliminan
  los mapas locales de tonos; `RECIBIDO` y `DIVIDIDO` usan la presentación neutra compartida.
- `Columnas` acepta `color` opcional por barra para recibir el token del catálogo. Sin esa
  propiedad conserva el degradado y sombra anteriores, como en su consumidor `Panel.tsx`.
  El máximo, proporción y mínimo visual de barra no cambian. `min-w-0` evita que las etiquetas
  impongan un ancho de escritorio. El gráfico mantiene cantidades, agrupación y orden.
- La distribución incluye una lista textual de todos los estados y cantidades. En móvil esa
  lista reemplaza visualmente las columnas estrechas; el gráfico decorativo queda oculto al
  lector de pantalla para evitar duplicación. No se ocultan estados con cantidad cero.
- Loading usa `Cargando` con cinco espacios de KPI y dos paneles, sin mostrar ceros mientras
  la consulta está pendiente. Los ceros de una respuesta válida se mantienen como datos reales.
- Con total cero se muestra `Vacio` en la distribución. Las excepciones conservan su resultado
  independiente, incluso si todavía no hay documentos. Los errores de resumen y excepciones
  usan `ErrorPanel`, mensaje real y `refetch`; un error nunca se sustituye por un total cero.

UX-02 aporta jerarquía compacta, pero no se copian automatización, confianza global, variaciones,
actividad reciente, responsables ilustrativos ni «En revisión»/«Por revisar». Tampoco se agrega
la tabla documental conceptual o analítica de `/panel`. El Design System aporta superficies,
estados y tokens; sus instancias genéricas «Guardar»/«Borrador» no son acciones ni estados reales.
La tarjeta total queda clara y compacta para reservar el énfasis a la revisión humana. Se usan
radios funcionales de métrica/panel, sin reconstruir la sidebar de 248 px o el header de 104 px.
No se declara pixel-perfect ni validación corporativa de Follow.

### Verificación del resumen

La validación visual controlada en Chromium cubre 1440, 1024, 768 y 390 px; loading, vacío,
error completo, error de excepciones, recuperación con Reintentar y permisos reducidos. Se
contrastan cinco KPI, nueve estados, porcentajes, cola separada, cinco excepciones y navegación.
Una comparación de AST contra el commit anterior comprueba las dos queries y los cálculos de
datos, total, destacados, cola, exclusiones técnicas y porcentajes, independientemente del formato.

La integración real no está verificada: el backend en `127.0.0.1:8090` no responde.
`npm run test:e2e` falla en los dos smoke por `ECONNREFUSED ::1:8091` y
`ECONNREFUSED 127.0.0.1:8091`; debe repetirse con Workflow disponible antes del PR.
`npm run build` y `git diff --check` pasaron. No se declara E2E verde.
FASE 4 termina sin iniciar Documentos ni Visor y conserva el stash.

## FASE 5 — Bandeja documental

### Contratos inspeccionados antes de modificar

| Aspecto | Contrato funcional preservado |
|---|---|
| Consulta | `listarDocumentos(filtro)`; query key `["documentos", filtro]` |
| Endpoint | `GET /api/v1/documentos`; respuesta `Pagina<Documento>` |
| Parámetros | `estados`, `texto`, `soloRaiz: true`, `pagina`, `tamano: 25`, `orden: "alta,desc"` |
| Búsqueda | Texto local al escribir; al enviar el formulario se aplica `texto.trim()` y página 0, sin debounce |
| Semántica del backend | Coincidencia parcial sin distinguir mayúsculas sobre nombre, remitente o ID del objeto referenciado por el sujeto |
| Filtros | Selección múltiple de los nueve estados documentales; se serializan separados por comas y reinician página 0 |
| Población | `DocumentoSpecificationBuilder` exige tenant, `baja IS NULL` y documento padre nulo cuando `soloRaiz` es verdadero |
| Paginación | 25 por página, índice desde 0; total y páginas provienen de `totalElements` y `totalPages` |
| Carga | `ingresarDocumento(archivo)`; un archivo, sin selección múltiple ni cola local |
| Payload | `POST /api/v1/documentos` multipart con `archivo` y parte JSON `datos` con `origen: "WEB"`; sin plantilla elegida |
| Extensiones del selector | `.pdf,.png,.jpg,.jpeg,.tif,.tiff,.webp` |
| Validación del servidor | Extensiones anteriores y MIME real: `application/pdf`, `image/png`, `image/jpeg`, `image/tiff`, `image/webp`; se mantienen sus rechazos de contenido, tamaño y archivo vacío |
| Permisos | Backend: `documentos.leer` para GET y `documentos.escribir` para POST. El frontend conserva la condición de escritura para ofrecer la carga |
| Visor | `documentoAbierto` contiene el mismo ID y monta `VisorDocumento`; cierre mediante `setDocumentoAbierto(null)` |
| Éxito de carga | Aviso con nombre y estado recibido; invalidaciones de `["documentos"]`, `["resumen"]` y `["kpi"]` |
| Posición tras cargar | Se conservan búsqueda, filtros y página; no se abre el visor ni se inserta una fila optimista |
| Error de carga | `mensajeDeError(error)`; el selector se vacía tras seleccionar, como antes |
| Estados de consulta | `isPending`, `isError` y colección vacía se tratan por separado; Reintentar usa el mismo `refetch` |

Las cinco columnas siguen siendo Documento, Estado, Tipo detectado, Sujeto y Recibido.
Documento conserva nombre, origen y cantidad de segmentos. Tipo conserva captura genérica,
código de plantilla o «sin detectar». Sujeto conserva tipo e ID, con «—» si no está asociado.
`formatearFecha` mantiene exactamente su implementación, también utilizada por el visor.

### Diseño y diferencias deliberadas

Se inspeccionaron con Figma MCP los contextos de diseño de UX-03 `2:123` y UX-04 `2:207` del
Product UX. También se inspeccionaron contexto e imágenes del Design System: normal `40:9138`,
filtrada `40:9326`, documento ingresado `40:9444`, vacía `40:9635`, sin resultados `40:9725`,
cargando `40:9817` y error `126:3289`.

- UX-03 orienta título, acción principal y lectura de la bandeja. No se incorporan KPI,
  tendencias, insights, responsables ni estados ilustrativos. «Sujeto» conserva su entidad real.
- UX-04 sigue siendo conceptual: cargar es una acción de `/documentos`; no se crean rutas,
  validación anticipada, selección múltiple, drag & drop, lote o modal de procesamiento.
- El Design System orienta tabla con cabecera grafito, superficie clara, filtros de selección
  violeta y estados separados. Algunas instancias muestran «Guardar», filtros verticales y
  encabezados superpuestos: se adaptan al flujo real y al espacio disponible, sin copiarlos.
- Se reutilizan `Boton`, `Campo`, `Pastilla`, `InsigniaEstado`, `Tarjeta`, `Cargando`, `Vacio`
  y `ErrorPanel`. No se modifica su API ni se agregan tokens, fuentes o dependencias.
- `ESTADOS_DOCUMENTALES` determina el catálogo de filtros, sus etiquetas y la etiqueta del
  aviso de ingreso; las insignias siguen usando esa misma fuente. No hay mapas locales de tonos.
- Desde 64 rem se presenta una tabla nativa con cinco columnas de ancho proporcional. Bajo
  ese ancho se usa una lista de tarjetas: dos columnas desde 40 rem y una en móvil. Cada
  tarjeta conserva todos los datos de la fila; el nombre abre el mismo visor.
- La tabla conserva clic sobre la fila y agrega botón de nombre con foco visible, Enter y
  Espacio nativos. Las presentaciones comparten funciones locales de nombre, tipo y sujeto;
  CSS oculta la variante que no corresponde, sin duplicar consultas.
- Búsqueda tiene label asociado y región `search`; filtros usan `fieldset`, `legend` y
  botones con `aria-pressed`. La tabla tiene caption y headers `scope="col"`. La paginación
  tiene nombre accesible y botones realmente deshabilitados en los extremos.
- Carga usa `Boton` con `cargando`, dimensiones estables, `aria-busy`, bloqueo del botón y
  del selector mientras espera, y anuncio de «Subiendo documento». No se amplía su contrato.
- El éxito usa `role="status"`; el error de carga, `role="alert"`. Ambos conservan el mensaje
  funcional y los iconos decorativos quedan fuera del árbol accesible.
- Loading usa seis skeletons decorativos. Una lista vacía sin criterios muestra «Todavía no
  hay documentos»; con búsqueda o estados seleccionados muestra «No hay documentos que
  coincidan». Esto describe la consulta actual, sin una petición adicional para inferir el
  tamaño global. Limpiar filtros reutiliza los cuatro cambios locales existentes.
- El error de listado muestra `ErrorPanel` y el mensaje real, sin convertirlo en una lista
  vacía. La pantalla no agrega un CTA de carga adicional en el vacío.

No se declara pixel-perfect ni validación corporativa contra Follow. El shell y el interior
del visor permanecen sin cambios; sus mejoras posteriores quedan fuera de FASE 5.

### Verificación

La comparación de AST contra `74437b8` verifica filtro, query, tamaño de página, totales,
alternancia de estados, envío de búsqueda, selección de archivo, formatos, invalidaciones,
ID entregado al visor y función de fecha. No se modifican cliente API, sesión, tipos ni backend.

Chromium verifica normal en 1440, 1024, 768 y 390; filtrada; sin resultados; loading; vacío;
error con recuperación; carga pendiente, éxito y rechazo; usuario sin escritura; paginación
de 25; apertura del visor por teclado y clic de fila. Las respuestas se controlan en el
navegador: una carga simulada verifica presentación y peticiones frontend, no integración real.
También pasan nombres, tipos y referencias extensos sin overflow en 1024 y 390 px, datos
ausentes y navegación por Tab. `npm run build` y `git diff --check` pasan.

El browser de Chromium no estaba presente en esta sesión y se descargó con el mecanismo de
Playwright ya instalado, sin cambios en `package.json` ni lockfile.

La integración real está pendiente: `127.0.0.1:8090` rechaza la conexión. `npm run test:e2e`
falla en sus dos smoke por `ECONNREFUSED ::1:8091` y `ECONNREFUSED 127.0.0.1:8091`.
No se declara E2E verde; se debe repetir con Workflow disponible antes del PR.
FASE 5 termina sin iniciar el rediseño del visor, sin push y conservando el stash de CHECKPOINT 2.

## FASE 6 — Visor y revisión documental

Base funcional: `811d7f9`. Se moderniza `VisorDocumento.tsx`. En `Documentos.tsx`, el clic
sobre una celda enfoca el botón de apertura de esa fila antes de montar el visor: esto
permite devolver el foco a un control útil al cerrar. No cambia el ID ni la apertura.
No se modifica Excepciones, que continúa utilizando el mismo visor.

### Contratos inspeccionados y preservados

| Aspecto | Contrato real |
| --- | --- |
| Invocación | Props `documentoId` y `alCerrar`; montaje desde Documentos y Excepciones, sin rutas nuevas |
| Detalle | Query key `["documento", documentoId]`; `obtenerDetalle`; GET `/api/v1/documentos/{id}/detalle` |
| Modelo | `DetalleDocumento`: documento, extracción y validación opcionales, candidatos, revisiones, segmentos y original físico; no se agregan vistas para estos dos últimos |
| Original | Acción `urlOriginal`, GET `/api/v1/documentos/{id}/original`, respuesta `{url}`; `window.open(url, "_blank", "noopener")`; no es una query ni un preview embebido |
| Campos | Mismo orden, etiqueta/clave, valor normalizado, presencia, confianza y marca manual; edición condicionada por `documentos.revisar` |
| Correcciones | Registro local por clave; volver al valor original elimina la corrección; no se envía ninguna petición al editar |
| Revisión | POST `/api/v1/documentos/{id}/revisiones` con `decision`, `motivo.trim() || undefined` y correcciones cuando existen |
| Decisiones | Aprobar, Observar y Rechazar conservan `APROBAR`, `OBSERVAR` y `RECHAZAR`, con los mismos chequeos sobre `transicionesPosibles` |
| Reprocesar / cerrar | POST `/api/v1/documentos/{id}/reprocesar` y `/cerrar`; las condiciones de habilitación permanecen iguales |
| Asociación | Mismos candidatos, orden, puntaje sin transformar, seleccionado/descartado y selección; POST `/api/v1/documentos/{id}/candidatos/{candidatoId}/seleccionar` |
| Motivo de asociación | `motivo.trim() || "Seleccion desde el portal"`, sin cambios |
| Actividad | Mismas revisiones, orden, actor, fecha, decisión, estados, motivo y cambios; no se agregan eventos |
| Invalidación | Las cuatro claves existentes: documento con ID, documentos, excepciones y resumen |
| Éxito de revisión | Limpia correcciones y motivo, anuncia el estado devuelto e invalida consultas |
| Éxito de reproceso/cierre/selección | Conserva correcciones y motivo, muestra el aviso e invalida consultas, como antes |
| Fallos | Conserva `mensajeDeError` y los borradores locales; ErrorPanel del detalle permite `refetch` y cierre |
| Cierre del visor | Desmonta y descarta correcciones y motivo locales sin confirmación, igual que antes |

Se inspeccionaron cliente API, DTOs, controlador documental y servicio de revisión. La
confianza sigue siendo un dato de lectura, nunca una decisión. `CORREGIR` conduce a
`OBSERVADO`; no existe guardado neutral ni se agrega «Guardar corrección». El texto de
correcciones pendientes explica que se envían con la decisión.

La discrepancia histórica de permisos queda pendiente de decisión funcional: el frontend
agrupa Reprocesar y Cerrar bajo `documentos.revisar`, mientras sus endpoints exigen
`documentos.escribir`. Detalle/original requieren leer; revisión/selección requieren revisar.
No se amplían ni reducen permisos. Reprocesar conserva su habilitación incluso en un estado
final cuando no hay una mutación de decisión/reproceso/cierre pendiente.

También se conserva el placeholder histórico de motivo, que menciona «corregir». El backend
exige motivo para RECHAZAR, OBSERVAR o sobreescritura de hallazgos; una corrección adjunta a
APROBAR no equivale por sí sola a esa exigencia. No se agrega validación frontend ni se
cambia esta regla. La selección de candidato conserva su bloqueo independiente.

### Referencias y decisiones visuales

Se inspeccionaron mediante Figma MCP contexto e imagen de Product UX `2:291` y del Design
System: Campos normal `40:12516`, editados `40:12742`, captura genérica `40:12970`, Hallazgos
`40:13198`, Hallazgos vacío `40:13281`, Asociación `40:13347`, Actividad `40:13424`, estado
final `40:13519` y visor móvil `123:9885`. Se revisaron metadatos de visor y responsive para
identificar las referencias relacionadas. La inspección de Figma fue de solo lectura.

- Se mantiene un drawer modal porque las superficies actuales lo abren sin cambiar ruta.
  Desde 768 px ocupa `min(90vw, 64rem)`; en móvil, todo el ancho. El límite de 64 rem permite
  revisar datos reales con más espacio que el drawer de unos 768 px del Design System.
- Encabezado, pestañas y decisiones rodean un área central desplazable. En viewports de
  hasta 600 px de alto se desplaza el conjunto para mantener accesibles contenido y acciones.
  No se fija una altura de frame de Figma ni se altera el shell.
- Se conservan Campos, Hallazgos, Asociación y Actividad. En móvil las cuatro pestañas se
  distribuyen en dos filas; las cinco decisiones siguen disponibles según el contrato.
- Del patrón original/datos/asistencia de UX-05 se incorpora la jerarquía: original siempre
  accesible en el encabezado, datos editables como vista inicial y hallazgos próximos mediante
  pestaña. No se agregan preview, asistencia IA, SLA, KPI, confianza global ni proceso sugerido.
- El Design System orienta superficies claras, separación de campos, selección violeta,
  estados y decisiones. Se utilizan tokens actuales y primitivas compartidas, sin copiar
  instancias genéricas de «Guardar» o pestañas ajenas al contrato ni el preview vacío móvil.
  Se mantiene una cabecera clara coherente con las fundaciones actuales en lugar de copiar
  la cabecera grafito de la referencia móvil.
- Se reutilizan Boton, BotonIcono, Campo, Tarjeta, Pastilla, InsigniaEstado, InsigniaPresencia,
  InsigniaSeveridad, BarraConfianza, Cargando, Vacio y ErrorPanel. No cambian sus APIs ni se
  agregan tokens, fuentes o dependencias. `ESTADOS_DOCUMENTALES` sigue siendo la fuente
  compartida de presentación documental; la confianza conserva `role="meter"` y sus ARIA.
- Loading muestra cinco skeletons decorativos. Error del detalle no muestra decisiones,
  incluso si existe un dato previo en caché. Se distinguen ausencia de validación y validación
  sin hallazgos. Sin extracción, candidatos o actividad se muestran mensajes específicos;
  metadatos opcionales ausentes se omiten o muestran «—», sin datos ilustrativos de relleno.

No se declara pixel-perfect ni validación corporativa de propiedades internas de Follow.

### Modalidad y accesibilidad

Se usa `dialog.showModal()`, `aria-modal`, título y descripción con IDs estables. El foco
inicial está en el título; el fondo queda inerte. Tab y Shift+Tab recorren los controles
disponibles dentro del visor: se cierran explícitamente ambos extremos porque Chromium
puede llevar el foco a la interfaz del navegador al salir del último control nativo.
Escape, el botón «Cerrar visor» y el clic exterior cierran; el foco vuelve al abridor si
sigue conectado. El scroll del documento se bloquea y restaura sin alterar su valor previo.

Las pestañas tienen roles tablist/tab/tabpanel, nombres, relaciones ARIA y un único tab stop;
flechas izquierda/derecha, Home y End activan y enfocan la pestaña correspondiente. Los
campos tienen nombre y descripción asociados. Mensajes usan status/alert, iconos e
indicadores decorativos se excluyen del árbol accesible. Loading de acciones mantiene
dimensiones, comunica aria-busy y conserva exactamente los bloqueos funcionales existentes.

### Verificación y límites

La comparación de AST contra `811d7f9` confirma query, permisos, indicador de trabajo,
mutaciones, payloads, errores, invalidaciones, apertura del original, correcciones y
condiciones de habilitación. API, tipos, sesión y backend permanecen sin cambios.

Chromium verifica Campos en 1440, 1024, 768 y 390; las cuatro pestañas en 1440 y 390; footer,
loading, error y recuperación, ausencia de hallazgos/validación, datos parciales, estado
final, tres combinaciones de permisos, apertura desde Excepciones y viewport 390×400.
No hay overflow horizontal en los escenarios capturados. Se verifican foco inicial, fondo
inerte, Tab/Shift+Tab, flechas/Home/End, Escape, retorno de foco y cierre con cambios locales.
Las cinco decisiones mantienen payloads y bloqueo mientras esperan; selección conserva
motivo explícito y fallback. Original conserva URL y apertura externa; sus errores se anuncian.

Estas son pruebas frontend con respuestas controladas, no evidencia de decisiones o
asociaciones ejecutadas en backend. Script, resultados y capturas locales de esta ejecución:
`/tmp/nextdocs-visor-heyiYw/`. No se agregan fixtures al producto ni dependencias.

`npm run build` pasa. `npm run test:e2e` falla en sus dos smoke por
`ECONNREFUSED ::1:8091` y `ECONNREFUSED 127.0.0.1:8091`; Workflow no está disponible.
La API core también rechaza conexión en `127.0.0.1:8090`. La integración real queda pendiente
y E2E no está verde: debe repetirse con los servicios disponibles antes del PR.
FASE 6 se limita al visor y termina sin iniciar Excepciones, sin push y preservando el stash.
