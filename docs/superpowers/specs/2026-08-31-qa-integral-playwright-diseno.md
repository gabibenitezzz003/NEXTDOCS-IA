# Diseño de QA integral con Playwright

## Objetivo

Incorporar una capa de aseguramiento de calidad de extremo a extremo que valide NEXT DOC AI como sistema completo y permita continuar el desarrollo del producto con una red de seguridad automatizada. La solución debe ejecutar el frontend, el backend y sus dependencias reales, producir evidencia diagnóstica y funcionar de la misma manera en local y en integración continua.

## Alcance

El primer ciclo cubre:

- Integración de Playwright mediante su interfaz de línea de comandos soportada oficialmente.
- Ejecución del frontend React y del backend Spring Boot contra PostgreSQL, Redis, MinIO y Keycloak reales.
- Preparación determinista de datos y usuarios de prueba.
- Recorridos de autenticación, autorización, navegación, documentos, revisión humana, excepciones y panel de indicadores.
- Evidencia de fallos mediante reporte HTML, trazas, capturas y vídeos.
- Integración con el flujo existente de GitHub Actions.
- Corrección de defectos encontrados por las pruebas.
- Documentación operativa y de mantenimiento en español.

El ciclo no incorpora todavía las tareas 21 a 32. Una vez estabilizada la plataforma documental, el trabajo continuará con los huecos funcionales de la tarea 15 y después con Workflow.

## Decisión sobre la acción de GitHub

No se utilizará `microsoft/playwright-github-action@v1`. El repositorio oficial de esa acción declara que está obsoleta y recomienda Playwright CLI porque la acción no puede resolver correctamente la versión instalada del navegador.

La instalación se realizará con la versión declarada en el proyecto:

```bash
npx playwright install --with-deps
npx playwright test
```

El archivo de dependencias bloqueará la versión exacta utilizada por desarrollo e integración continua.

## Arquitectura de pruebas

La solución tendrá cuatro capas:

1. Infraestructura real: PostgreSQL, Redis, MinIO y Keycloak.
2. Aplicación real: backend Spring Boot y frontend Vite.
3. Preparación de escenarios: utilidades idempotentes para crear sesiones y datos aislados.
4. Especificaciones Playwright: recorridos observables desde el navegador y validaciones de API cuando sean necesarias para preparar o confirmar estados.

Las pruebas no dependerán del orden de ejecución. Cada escenario generará sus propios identificadores y dejará el estado necesario explícitamente preparado. La paralelización se limitará cuando exista riesgo de colisión sobre recursos compartidos.

## Organización prevista

El frontend incorporará:

- `playwright.config.ts` para navegadores, servidores, tiempos, reintentos y artefactos.
- Un directorio de pruebas E2E con especificaciones nombradas en español.
- Utilidades de sesión, API y datos de prueba con responsabilidades separadas.
- Scripts para instalar navegadores, ejecutar QA y abrir el reporte.

La configuración utilizará variables de entorno para las direcciones del frontend y backend. No contendrá secretos productivos ni credenciales fuera del entorno de demostración.

## Recorridos iniciales

### Autenticación y permisos

- Ingreso válido del administrador de demostración.
- Rechazo de credenciales inválidas.
- Expiración o ausencia de sesión.
- Navegación visible conforme a permisos.

### Documentos

- Ingreso de un documento permitido.
- Visualización en bandeja y detalle.
- Descarga o apertura del original.
- Revisión humana con motivo y decisión.
- Reprocesamiento cuando el estado lo permita.

### Excepciones

- Listado y filtros.
- Apertura del documento relacionado.
- Resolución con el dato obligatorio.
- Estados de carga, vacío y error reproducibles.

### Indicadores

- Carga del resumen y del panel.
- Correspondencia entre indicador y población consultable.
- Cambio del intervalo temporal.
- Respuesta correcta cuando no existen datos.

### Calidad transversal

- Ausencia de errores inesperados de consola.
- Ausencia de solicitudes de red fallidas no contempladas.
- Contenido significativo en cada ruta.
- Accesibilidad básica de formularios, diálogos y navegación.

## Integración continua

El flujo existente mantendrá las pruebas Java y la construcción de la imagen. Se añadirá un trabajo de QA web que:

1. Prepare Node y las dependencias bloqueadas.
2. Instale los navegadores y dependencias del sistema mediante Playwright CLI.
3. Levante la infraestructura requerida.
4. Arranque el backend con configuración de prueba.
5. Arranque el frontend con la dirección del backend.
6. Espere los puntos de salud de ambos procesos.
7. Ejecute Playwright.
8. Publique reporte, trazas, capturas y vídeos aunque las pruebas fallen.

El trabajo de construcción de imagen dependerá tanto de las pruebas backend como del QA web cuando el costo total del flujo lo permita. Si el tiempo resulta excesivo, ambos trabajos podrán ejecutarse en paralelo sin reducir la cobertura.

## Tratamiento de fallos

Los tiempos de espera usarán señales de salud y condiciones observables, nunca pausas fijas como mecanismo principal. Un fallo debe conservar suficiente evidencia para distinguir entre:

- Infraestructura no disponible.
- Backend sin estado saludable.
- Frontend sin contenido o con error de ejecución.
- Contrato HTTP roto.
- Estado funcional incorrecto.
- Defecto visual o de interacción.

Las pruebas reintentables solo tendrán reintentos en integración continua. Un fallo local se mostrará inmediatamente para evitar ocultar inestabilidad.

## Estrategia para completar el producto

Después de estabilizar la base E2E, el avance seguirá bloques verticales:

1. Cerrar y documentar el alcance real de la tarea 15.
2. Completar Template Studio.
3. Completar gobernanza y trazabilidad.
4. Completar monitor de integraciones y administración.
5. Completar Archive & Export Center en el portal.
6. Diseñar e implementar Workflow Definition Service como subsistema independiente.

Cada bloque incluirá backend, frontend, migraciones cuando correspondan, pruebas unitarias, pruebas de integración y recorridos Playwright. No se marcará una tarea como terminada únicamente porque compile.

## Calidad del frontend

Los componentes nuevos seguirán separación por responsabilidad, carga de datos sin cascadas evitables, estados explícitos, accesibilidad y tipado estricto. Las pantallas grandes se dividirán cuando la nueva funcionalidad obligue a modificarlas, sin realizar refactorizaciones ajenas al recorrido trabajado.

## Calidad del backend

Los escenarios E2E complementarán, pero no sustituirán, las pruebas unitarias y de integración. Los servicios mantendrán el aislamiento por tenant, la conversión de entidades dentro de transacciones, la idempotencia y la trazabilidad. Los datos externos permanecerán detrás de adaptadores.

## Documentación y entrega

La documentación indicará:

- Requisitos locales.
- Comandos de instalación y ejecución.
- Variables de entorno.
- Estrategia de datos de prueba.
- Lectura de reportes y trazas.
- Diagnóstico de fallos frecuentes.
- Correspondencia entre recorridos y criterios de aceptación.

Los commits serán pequeños, estarán escritos en español y utilizarán la identidad Git ya configurada. Cada bloque se publicará en `origin/main` después de superar sus verificaciones y de confirmar que el remoto no contiene cambios incompatibles.

## Criterios de aceptación

- Playwright queda instalado con versión bloqueada y sin usar la acción obsoleta.
- El mismo comando principal funciona localmente y en GitHub Actions.
- El flujo E2E levanta la plataforma completa desde un entorno limpio.
- Los recorridos iniciales pasan de forma determinista.
- Un fallo genera reporte HTML, traza, captura o vídeo según corresponda.
- La compilación frontend, las pruebas backend y el QA E2E quedan documentados.
- No se incorporan secretos ni datos productivos.
- Código, archivos propios, pruebas, mensajes y documentación quedan en español y sin comentarios dentro del código.
