#!/usr/bin/env node
import { writeFile } from "node:fs/promises";

const config = {
  core: process.env.NEXTDOCS_URL ?? "http://localhost:8090",
  workflow: process.env.NEXTDOCS_WORKFLOW_URL ?? "http://localhost:8091",
  tenant: process.env.NEXTDOCS_TENANT,
  email: process.env.NEXTDOCS_EMAIL,
  clave: process.env.NEXTDOCS_CLAVE,
  concurrencia: Number(process.env.NEXTDOCS_CONCURRENCIA ?? 4),
  segundos: Number(process.env.NEXTDOCS_SEGUNDOS ?? 30),
  calentamiento: Number(process.env.NEXTDOCS_CALENTAMIENTO ?? 5),
  timeoutMs: Number(process.env.NEXTDOCS_TIMEOUT_MS ?? 15000),
  rps: Number(process.env.NEXTDOCS_RPS ?? 4),
};

function abortar(mensaje) {
  console.error(`\n  ${mensaje}\n`);
  process.exit(1);
}

function uso() {
  console.log(`
  Mide la linea base de lectura del core y del motor de procesos.

    node scripts/baseline-carga.mjs [--informe salida.md]

  Solo golpea endpoints de lectura: no crea documentos ni gasta llamadas al
  proveedor de IA.

    NEXTDOCS_URL            core, por defecto http://localhost:8090
    NEXTDOCS_WORKFLOW_URL   procesos, por defecto http://localhost:8091
    NEXTDOCS_TENANT         codigo de la organizacion
    NEXTDOCS_EMAIL          usuario
    NEXTDOCS_CLAVE          clave
    NEXTDOCS_CONCURRENCIA   peticiones en paralelo, por defecto 4
    NEXTDOCS_RPS            techo de peticiones por segundo, por defecto 4
                            (0 lo desactiva y mide a maxima velocidad)
    NEXTDOCS_SEGUNDOS       duracion de la medicion, por defecto 30
    NEXTDOCS_CALENTAMIENTO  segundos previos que no se miden, por defecto 5

  El core limita a 300 peticiones por minuto y por usuario (5 por segundo),
  y a 1200 por minuto y por tenant. Con NEXTDOCS_RPS por encima de 5 vas a
  medir el rechazo del limitador y no la latencia real.
`);
}

async function ingresar() {
  const respuesta = await fetch(`${config.core}/api/v1/autenticacion/ingresar`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      codigoTenant: config.tenant,
      email: config.email,
      clave: config.clave,
    }),
  });
  if (!respuesta.ok) {
    abortar(`No se pudo entrar (${respuesta.status}). Revisa las credenciales.`);
  }
  const sesion = await respuesta.json();
  return { token: sesion.tokenAcceso, tenantId: sesion.tenantId };
}

function ventana(dias) {
  const hasta = new Date();
  const desde = new Date(hasta.getTime() - dias * 86400000);
  return `desde=${encodeURIComponent(desde.toISOString())}&hasta=${encodeURIComponent(hasta.toISOString())}`;
}

function escenarios(sesion) {
  return [
    { nombre: "documentos: bandeja", url: `${config.core}/api/v1/documentos?tamano=25`, servicio: "core" },
    { nombre: "documentos: resumen", url: `${config.core}/api/v1/documentos/resumen`, servicio: "core" },
    { nombre: "excepciones: bandeja", url: `${config.core}/api/v1/excepciones?pagina=0&tamano=25`, servicio: "core" },
    { nombre: "kpi documental: resumen", url: `${config.core}/api/v1/kpi/resumen`, servicio: "core" },
    { nombre: "plantillas: catalogo", url: `${config.core}/api/v1/plantillas`, servicio: "core" },
    { nombre: "procesos: definiciones", url: `${config.workflow}/api/v1/procesos`, servicio: "workflow" },
    { nombre: "procesos: instancias", url: `${config.workflow}/api/v1/instancias`, servicio: "workflow" },
    { nombre: "procesos: tareas", url: `${config.workflow}/api/v1/tareas`, servicio: "workflow" },
    {
      nombre: "kpi procesos: resumen",
      url: `${config.workflow}/api/v1/kpi-procesos?${ventana(30)}`,
      servicio: "workflow",
    },
    {
      nombre: "kpi procesos: poblacion",
      url: `${config.workflow}/api/v1/kpi-procesos/poblacion?indicador=tareasCompletadas&${ventana(30)}`,
      servicio: "workflow",
    },
  ].map((escenario) => ({
    ...escenario,
    cabeceras: {
      Authorization: `Bearer ${sesion.token}`,
      "X-Tenant-Id": sesion.tenantId,
    },
  }));
}

async function medir(escenario, muestras, midiendo) {
  const inicio = performance.now();
  let estado = 0;
  try {
    const control = AbortSignal.timeout(config.timeoutMs);
    const respuesta = await fetch(escenario.url, {
      headers: escenario.cabeceras,
      signal: control,
    });
    estado = respuesta.status;
    await respuesta.arrayBuffer();
  } catch {
    estado = 0;
  }
  const duracion = performance.now() - inicio;
  if (midiendo()) {
    muestras.push({ duracion, ok: estado >= 200 && estado < 300, estado });
  }
}

function percentil(ordenadas, fraccion) {
  if (!ordenadas.length) return null;
  const indice = Math.min(ordenadas.length - 1, Math.ceil(fraccion * ordenadas.length) - 1);
  return ordenadas[Math.max(0, indice)];
}

function resumir(nombre, servicio, muestras, segundos) {
  const duraciones = muestras.map((m) => m.duracion).sort((a, b) => a - b);
  const fallidas = muestras.filter((m) => !m.ok);
  const estados = [...new Set(fallidas.map((m) => m.estado))].sort();
  return {
    nombre,
    servicio,
    peticiones: muestras.length,
    errores: fallidas.length,
    estadosDeError: estados,
    rps: muestras.length / segundos,
    p50: percentil(duraciones, 0.5),
    p95: percentil(duraciones, 0.95),
    p99: percentil(duraciones, 0.99),
    max: duraciones.at(-1) ?? null,
  };
}

function ms(valor) {
  return valor == null ? "—" : `${valor.toFixed(0)} ms`;
}

async function correrEscenario(escenario) {
  const muestras = [];
  const arranque = performance.now();
  const finCalentamiento = arranque + config.calentamiento * 1000;
  const fin = finCalentamiento + config.segundos * 1000;
  const midiendo = () => performance.now() >= finCalentamiento;
  const intervalo = config.rps > 0 ? 1000 / config.rps : 0;
  let proxima = arranque;

  const trabajadores = Array.from({ length: config.concurrencia }, async () => {
    while (performance.now() < fin) {
      if (intervalo > 0) {
        const ahora = performance.now();
        const turno = Math.max(proxima, ahora);
        proxima = turno + intervalo;
        const espera = turno - ahora;
        if (espera > 0) await new Promise((listo) => setTimeout(listo, espera));
        if (performance.now() >= fin) break;
      }
      await medir(escenario, muestras, midiendo);
    }
  });
  await Promise.all(trabajadores);

  return resumir(escenario.nombre, escenario.servicio, muestras, config.segundos);
}

async function main() {
  const argumentos = process.argv.slice(2);
  if (argumentos.includes("--help")) {
    uso();
    process.exit(0);
  }
  const indiceInforme = argumentos.indexOf("--informe");
  const rutaInforme = indiceInforme >= 0 ? argumentos[indiceInforme + 1] : null;

  for (const [clave, nombre] of [
    ["tenant", "NEXTDOCS_TENANT"],
    ["email", "NEXTDOCS_EMAIL"],
    ["clave", "NEXTDOCS_CLAVE"],
  ]) {
    if (!config[clave]) abortar(`Falta la variable ${nombre}. Corre con --help para ver el uso.`);
  }

  const sesion = await ingresar();
  const lista = escenarios(sesion);

  console.log(`\n  Concurrencia ${config.concurrencia}, ${config.segundos}s por escenario`);
  console.log(`  (mas ${config.calentamiento}s de calentamiento que no se miden)`);
  console.log(`  Core ${config.core} · Procesos ${config.workflow}\n`);
  console.log(`  ${"Escenario".padEnd(30)} ${"req".padStart(6)} ${"rps".padStart(7)} ${"p50".padStart(9)} ${"p95".padStart(9)} ${"p99".padStart(9)}  err`);
  console.log(`  ${"-".repeat(30)} ${"-".repeat(6)} ${"-".repeat(7)} ${"-".repeat(9)} ${"-".repeat(9)} ${"-".repeat(9)}  ---`);

  const filas = [];
  for (const escenario of lista) {
    const fila = await correrEscenario(escenario);
    filas.push(fila);
    const marca = fila.errores ? `${fila.errores} (${fila.estadosDeError.join(",")})` : "0";
    console.log(
      `  ${fila.nombre.padEnd(30)} ${String(fila.peticiones).padStart(6)} ` +
        `${fila.rps.toFixed(1).padStart(7)} ${ms(fila.p50).padStart(9)} ` +
        `${ms(fila.p95).padStart(9)} ${ms(fila.p99).padStart(9)}  ${marca}`,
    );
  }

  const totalPeticiones = filas.reduce((suma, f) => suma + f.peticiones, 0);
  const totalErrores = filas.reduce((suma, f) => suma + f.errores, 0);
  const peorP95 = filas.reduce((peor, f) => (f.p95 > (peor?.p95 ?? -1) ? f : peor), null);

  console.log(`\n  ${totalPeticiones} peticiones, ${totalErrores} con error`);
  console.log(`  Peor p95: ${peorP95.nombre} con ${ms(peorP95.p95)}\n`);

  if (rutaInforme) {
    const lineas = [
      "# Linea base de carga",
      "",
      `- Fecha: ${new Date().toISOString()}`,
      `- Concurrencia: ${config.concurrencia}`,
      `- Duracion por escenario: ${config.segundos}s (mas ${config.calentamiento}s de calentamiento)`,
      `- Core: ${config.core}`,
      `- Procesos: ${config.workflow}`,
      `- Total: ${totalPeticiones} peticiones, ${totalErrores} con error`,
      "",
      "| Escenario | Servicio | Peticiones | req/s | p50 | p95 | p99 | max | Errores |",
      "|---|---|---|---|---|---|---|---|---|",
      ...filas.map(
        (f) =>
          `| ${f.nombre} | ${f.servicio} | ${f.peticiones} | ${f.rps.toFixed(1)} | ${ms(f.p50)} | ` +
          `${ms(f.p95)} | ${ms(f.p99)} | ${ms(f.max)} | ${f.errores ? `${f.errores} (${f.estadosDeError.join(", ")})` : "0"} |`,
      ),
      "",
      `Peor p95: **${peorP95.nombre}** con ${ms(peorP95.p95)}.`,
      "",
    ];
    await writeFile(rutaInforme, lineas.join("\n"));
    console.log(`  Informe en ${rutaInforme}\n`);
  }

  process.exit(totalErrores ? 1 : 0);
}

main().catch((error) => abortar(error.stack ?? String(error)));
