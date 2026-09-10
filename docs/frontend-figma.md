# Frontend y Figma

## Alcance y fuentes de verdad

Modernización incremental del frontend existente de NEXT DOC AI. El código conserva la autoridad
sobre comportamiento, contratos, sesión, tenant y permisos. Figma define la presentación, con las
discrepancias documentadas antes de adaptar una pantalla.

Archivo oficial: [NEXT DOC AI — Design System & Product UI](https://www.figma.com/design/0VZRK69QjDTDy0oAaf8Uv1).
Los nodos se inspeccionaron mediante MCP, incluyendo contexto de diseño y screenshots.

Este documento acompaña exclusivamente la FASE 1, CHECKPOINT 1. El siguiente checkpoint requiere
aprobación explícita. No se modifican páginas, shell, APIs, sesión, permisos ni gráficos en este corte.

## Gestor de paquetes

El gestor canónico es **npm**: existe `frontend/package-lock.json`, no hay `pnpm-lock.yaml`, y el
workflow existente instala con `npm ci`. Las instalaciones y los scripts se ejecutan desde
`frontend/` con npm. No se agrega un segundo lockfile ni se cambia `package.json`.

## Fundaciones y compatibilidad

| Archivo | Responsabilidad |
|---|---|
| `frontend/src/estilos.css` | Entrada de Tailwind, importación de fundaciones y estilos existentes |
| `frontend/src/estilos/tokens.css` | Colores, espaciado, radios, sombras, medidas y foco |
| `frontend/src/estilos/tipografia.css` | Familias, escalas, pesos, interlineados y bases tipográficas |
| `frontend/src/utilidades/estadosDocumento.ts` | Fuente compartida de presentación documental |
| `frontend/src/componentes/Insignias.tsx` | Primer consumidor del catálogo documental |

Las declaraciones `@theme static` conservan los tokens en el CSS generado incluso cuando se
consumen mediante referencias `var(...)` desde TypeScript o todavía esperan adopción por una pantalla.
Los valores y nombres existentes de colores, radios `xl/2xl/3xl` y sombras se conservan como
compatibilidad. Las escalas objetivo se agregan con nombres semánticos; no se reemplazan globalmente
los radios y sombras que usan las páginas actuales.

### Tokens

- Paleta: grafito, violeta, rojo, fondo, blanco, texto, bordes, éxito, alerta, información y arena;
  conserva los valores actuales que coinciden con la paleta de fundaciones de Figma.
- Espacios: `--spacing-espacio-{1,2,3,4,5,6,8,10,12,16,20,24}` equivalen a
  4, 8, 12, 16, 20, 24, 32, 40, 48, 64, 80 y 96 px. Ejemplo de consumo: `gap-espacio-6`.
- Radios objetivo: `--radius-control` 12 px, `--radius-tarjeta` 20 px,
  `--radius-tarjeta-principal` 24 px, `--radius-insignia` 9999 px. Ejemplo: `rounded-control`.
- Sombras objetivo: `--shadow-superficie` (0/4/16), `--shadow-superficie-elevada` (0/10/30),
  `--shadow-panel-lateral` (0/18/48) y `--shadow-resplandor-marca` (0/6/24), en px para x/y/blur.
  Los valores de opacidad proceden de los efectos reales de Figma, sin redondearlos a una nueva
  intención visual. Las sombras anteriores continúan disponibles para consumidores existentes.
- Medidas objetivo: barra lateral 248 px, compacta 64 px, margen de contenido 32 px,
  separación de columnas 24 px y grilla de 12 columnas. Son referencias; no implementan el shell.
- Foco: grosor 2 px, separación 2 px y violeta de marca; mantiene el comportamiento visible actual.
- Degradado de marca: violeta a rojo, 100 grados; la clase existente consume esta variable.

La centralización de colores internos de `Graficos.tsx` se difiere a FASE 8: sus degradados y
concatenaciones de opacidad requieren adaptación coordinada, innecesaria para este checkpoint.

### Tipografía

| Rol | Familia | Peso | Tamaño / interlineado | Utilidades objetivo |
|---|---|---|---|---|
| KPI | Space Grotesk | 700 | 56 / 64 px | `font-titulo text-kpi` |
| H1 | Space Grotesk | 700 | 32 / 40 px | `font-titulo text-titulo-1` |
| H2 | Space Grotesk | 700 | 22 / 28 px | `font-titulo text-titulo-2` |
| H3 | Space Grotesk | 700 | 17 / 24 px | `font-titulo text-titulo-3` |
| Cuerpo | Inter | 400 | 15 / 22 px | `font-cuerpo text-cuerpo` |
| Cuerpo medio | Inter | 500 | 15 / 22 px | `font-cuerpo text-cuerpo-medio` |
| Pequeño | Inter | 400 | 13 / 18 px | `font-cuerpo text-pequeno` |
| Micro | Inter | 600 | 10 / 14 px | `font-cuerpo text-micro` |
| Código | Roboto Mono, si está disponible; monoespaciada del sistema | 400 | 13 / 18 px | `font-codigo text-codigo` |

Se conserva la carga actual de Inter y Space Grotesk en `index.html`: Google Fonts, preconnect,
`display=swap` y alternativa sin JavaScript. No se descargan fuentes ni licencias; Roboto Mono es
solamente una declaración objetivo con fallback y no genera una petición de red.

La base actual de 14 px, los títulos existentes y `.cifra` se trasladan a `tipografia.css` sin
alterar sus medidas efectivas. La escala de Figma se aplica explícitamente al modernizar cada
componente. No se afirma una mejora de CLS sin medirla. Un eventual alojamiento local de fuentes
será un cambio separado con fuente oficial, licencia, peso y beneficio verificado.

### Estados documentales

`ESTADOS_DOCUMENTALES` es el único catálogo nuevo de estados: cada entrada contiene `etiqueta`,
`tono` para las primitivas existentes y `color` como referencia CSS utilizable también en SVG.
Se verifica exhaustividad mediante `satisfies Readonly<Record<EstadoDocumento, ...>>`; agregar
un estado al contrato exige definir su presentación. El catálogo no establece transiciones,
permisos, aprobación automática ni reglas de decisión.

Los tonos siguen el grupo Document Status de Figma: recibido, dividido y cerrado neutros;
procesando y extraído informativos; validado violeta; observado alerta; aprobado éxito; rechazado rojo.
Las etiquetas y los valores internos continúan en mayúsculas. La implementación reutiliza la paleta
semántica de fundaciones y `Pastilla`; las variantes finas de insignias pertenecen al CHECKPOINT 2.

`InsigniaEstado` conserva su exportación y prop `estado`, y elimina su mapa local. Su fallback
neutro ante un valor inesperado se mantiene. Los componentes de presencia, severidad y confianza
no cambian: `NO_FIGURA` sigue siendo distinto de `ILEGIBLE`.

Resumen, Documentos, Visor y Panel podrán consultar `ESTADOS_DOCUMENTALES[estado]` y derivar sus
listas desde el catálogo, sin copiar mapas. Las insignias que esas páginas ya consumen reciben
la presentación compartida ahora. Los mapas y listas antiguos internos de las páginas permanecen
temporalmente sin migrar por restricción expresa de alcance; su sustitución se hará en cada fase.
En este checkpoint todavía pueden verse diferencias entre un gráfico antiguo y una insignia.

## Correspondencia Figma y React

| Diseño | Node ID | Archivo o consumidor |
|---|---|---|
| Fundaciones | `123:15067` | `estilos/tokens.css` |
| Escalas, radios, sombras y grilla | `123:15380` | `estilos/tokens.css` |
| Tipografía | `123:15330` | `estilos/tipografia.css` |
| Estados | `123:17548` | `utilidades/estadosDocumento.ts`, `componentes/Insignias.tsx` |
| Controles base | `123:17332` | `componentes/Interfaz.tsx`, pendiente de CHECKPOINT 2 |
| Autenticación normal | `40:2398` | `paginas/Ingresar.tsx` |
| Resumen normal | `40:3580` | `paginas/Resumen.tsx` |
| Documentos normal | `40:9138` | `paginas/Documentos.tsx` |
| Visor, campos | `40:12516` | `paginas/VisorDocumento.tsx` |
| Excepciones abiertas | `46:233` | `paginas/Excepciones.tsx` |
| Panel, 30 días | `75:335` | `paginas/Panel.tsx` |
| Tipos pendientes | `100:367` | `paginas/TiposPropuestos.tsx` |
| Plantillas de proceso, lista | `123:1699` | `paginas/Procesos.tsx` |

Las rutas y los contenedores permanecen en su ubicación actual. El visor se abre como drawer y
el estudio de procesos usa estado local dentro de `/procesos`. Las futuras extracciones separarán
primitivas, navegación, feedback y componentes de visor/panel/procesos cuando exista una frontera
real. No se agrega una arquitectura paralela ni se configura Code Connect todavía.

## Responsive previsto, todavía no implementado

Página `123:20966`: escritorio de 1280 px (`123:9956`), tablet de 1024 px con barra de 64 px
(`126:3616`), tablet de 768 px con navegación desplegable (`123:9715`) y móvil de 390 px
con navegación inferior (`123:9732`). Documentos móvil usa tarjetas (`123:9813`) y el visor
ocupa la pantalla (`123:9885`). Los anchos son objetivos de validación; este checkpoint no
redefine los breakpoints globales de Tailwind.

La implementación deberá mantener todas las funciones y todos los accesos autorizados.
Los controles ausentes del mockup se conservarán mediante una adaptación explícita, sin inventar
APIs. Los viewports de revisión serán 1440×1024, 1280×1024, 1024, 768 y 390 según sus frames;
el login de escritorio utiliza 1440×900. No hay un frame específico de procesos responsive
en el inventario auditado; se reutilizarán las reglas del sistema.

## Discrepancias registradas, fuera de esta rama visual

Por indicación expresa, no se corrigen como efecto secundario del rediseño:

- El visor agrupa reprocesar/cerrar bajo `documentos.revisar`, pero el core exige
  `documentos.escribir` para esos endpoints.
- Las query keys no incluyen tenant.
- El logout no limpia el caché de React Query.
- Resumen y Excepciones comparten una key para páginas de tamaño 5 y 25.
- Las rutas no tienen guards de permisos propios; ocultar navegación no es una barrera de API.

Otras discrepancias que requieren respetar el comportamiento actual al abordar sus pantallas:

- El frontend guarda correcciones junto con decisiones. Un botón independiente de guardado
  no puede conectarse silenciosamente a `CORREGIR`, porque el backend lo lleva a `OBSERVADO`.
- El original se abre en otra pestaña; la previsualización móvil del mockup requiere una decisión.
- Algunas instancias de Figma muestran etiquetas genéricas o pestañas ajenas al visor.
  Se conservan las acciones reales y Campos/Hallazgos/Asociación/Actividad.
- Algunos gráficos de Figma muestran series temporales, costos o tipos que no corresponden a
  los datasets actuales. No se inventan métricas ni se reinterpretan poblaciones.
- El pipeline existente de Workflow construye desde `../workflow`; esa dependencia externa debe
  resolverse explícitamente al diseñar su entorno CI, sin fusionar el microservicio al core.

## Elementos futuros excluidos

La página 16 (`134:2107`) no autoriza implementar uploader avanzado, drag & drop, subida múltiple,
nuevos gráficos temporales, exportación de indicadores a PDF, comparación personalizada, canvas
ni bifurcaciones condicionales. El responsive está incluido por instrucción explícita del usuario,
aunque esa página también lo clasifique como propuesta futura.

## Verificación

Desde `frontend/`: `npm run build` ejecuta TypeScript y Vite; `npm run test:e2e` ejecuta los dos
smoke HTTP existentes contra Workflow en `localhost:8091`. Desde el repositorio:
`git diff --check` controla errores de whitespace. Los scripts `test`, `lint` y `typecheck`
independiente no existen. No se incorporan dependencias de pruebas en este checkpoint.

Los smoke actuales no navegan por el portal ni prueban permisos o aislamiento entre tenants.
El build tampoco certifica fidelidad visual o WCAG AA. Las fases de pantallas incorporarán
comparación en navegador y pruebas de interacción; la etapa de pruebas ampliará cobertura con
Vitest/Testing Library y Playwright, y la de CI los ejecutará con npm y un entorno explícito.
