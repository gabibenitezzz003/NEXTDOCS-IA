# E00 · Verificación local de baseline

Fecha: sesión actual.  
Entorno: local, sin infraestructura levantada.  
Objetivo: validar que el código compila y contar artefactos reales antes de cerrar E00.

## Comandos ejecutados y resultados

### Conteo de archivos Java

```bash
find /home/gabibenitezzz/Escritorio/NEXT\ AI/nextdocs-ai/backend/src/main/java -name "*.java" | wc -l
```

Resultado: `336`

```bash
find /home/gabibenitezzz/Escritorio/NEXT\ AI/nextdocs-ai/backend/src/test/java -name "*Test.java" -o -name "*IT.java" | wc -l
```

Resultado: `26` (8 unitarios `*Test` + 18 integración `*IT`)

```bash
find /home/gabibenitezzz/Escritorio/NEXT\ AI/workflow/src/main/java -name "*.java" | wc -l
```

Resultado: `53`

```bash
find /home/gabibenitezzz/Escritorio/NEXT\ AI/workflow/src/test/java -name "*.java" | wc -l
```

Resultado: `5`

### Conteo de migraciones Flyway

```bash
find /home/gabibenitezzz/Escritorio/NEXT\ AI/nextdocs-ai/backend/src/main/resources/db/migration -name "V*.sql" | wc -l
```

Resultado: `20`

```bash
find /home/gabibenitezzz/Escritorio/NEXT\ AI/workflow/src/main/resources/db/migration -name "V*.sql" | wc -l
```

Resultado: `2`

### Listado de controladores REST del core

```bash
find /home/gabibenitezzz/Escritorio/NEXT\ AI/nextdocs-ai/backend/src/main/java/com/nextdocs/ai/restControladores -name "*RestController.java" | sort
```

Resultado:

- `AdministracionRestController.java`
- `AutenticacionRestController.java`
- `DocumentoRestController.java`
- `ExcepcionRestController.java`
- `ExportacionRestController.java`
- `FederacionRestController.java`
- `GobernanzaRestController.java`
- `IntegracionRestController.java`
- `KpiRestController.java`
- `ObservabilidadRestController.java`
- `OriginalFisicoRestController.java`
- `PlantillaRestController.java`

Total: 12 controladores con ruta + `ControladorRest` base.

### Build del frontend

```bash
cd /home/gabibenitezzz/Escritorio/NEXT\ AI/nextdocs-ai/frontend
npm run build
```

Resultado:

```text
> nextdocs-ai-portal@0.0.1 build
> tsc -b && vite build

vite v8.2.2 building client environment for production...
transforming...
✓ 146 modules transformed.
rendering chunks...
computing gzip size...
dist/index.html                   0.95 kB │ gzip:   0.43 kB
dist/assets/index-5LR2x_0D.css   48.97 kB │ gzip:   9.12 kB
dist/assets/index-CYtYbi77.js   412.85 kB │ gzip: 126.43 kB

✓ built in 475ms
```

Estado: **PASS**. TypeScript compila y Vite empaqueta sin errores.

### Compilación del backend core

```bash
cd /home/gabibenitezzz/Escritorio/NEXT\ AI/nextdocs-ai/backend
docker run --rm --network host -v "$PWD":/app -v nextdocs-m2:/root/.m2 -w /app \
  maven:3.9-eclipse-temurin-21 mvn compile -DskipTests
```

Resultado:

```text
[INFO] Building nextdocs-ai 0.0.1-SNAPSHOT
[INFO] --- compiler:3.13.0:compile (default-compile) @ ai ---
[INFO] Nothing to compile - all classes are up to date.
[INFO] BUILD SUCCESS
```

Estado: **PASS**.

### Compilación del microservicio workflow

```bash
cd /home/gabibenitezzz/Escritorio/NEXT\ AI/workflow
docker run --rm --network host -v "$PWD":/app -v nextdocs-m2:/root/.m2 -w /app \
  maven:3.9-eclipse-temurin-21 mvn compile -DskipTests
```

Resultado:

```text
[INFO] Building nextdocs-workflow 0.0.1-SNAPSHOT
[INFO] --- compiler:3.13.0:compile (default-compile) @ workflow ---
[INFO] Nothing to compile - all classes are up to date.
[INFO] BUILD SUCCESS
```

Estado: **PASS**.

## Observaciones

- Los conteos reales son ligeramente distintos a los reportados por el baseline V11 (`336` archivos Java del core vs `~333` del subagente, 53 del workflow vs 54). Se actualiza `docs/discovery-v11.md` con los valores exactos.
- Los 247 tests reportados requieren ejecutar `mvn verify` con infraestructura levantada; no se ejecutaron en esta verificación local.
- Ninguna verificación de esta sesión modificó código productivo.
