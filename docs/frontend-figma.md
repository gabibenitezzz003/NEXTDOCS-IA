# Frontend y Figma

## Alcance

Esta corrección corresponde a FASE 1, CHECKPOINT 1, sobre `3c7bfab`. Sólo ajusta las
fundaciones y esta documentación. CHECKPOINT 2 y FASE 2 permanecen pausados; no se migran
páginas, shell, APIs, permisos ni transiciones.

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
columnas. `Disposicion.tsx` no se modifica; FASE 2 hará la migración explícita.

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

Después de este commit no se restaura el stash, no se inicia CHECKPOINT 2 ni FASE 2. La migración
del shell, responsive y componentes corporativos requiere autorización y validación visual aparte.
