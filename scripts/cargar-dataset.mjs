#!/usr/bin/env node
import { readdir, readFile, writeFile, stat } from "node:fs/promises";
import { basename, extname, join, resolve } from "node:path";

const EXTENSIONES = new Set([".pdf", ".png", ".jpg", ".jpeg", ".tif", ".tiff", ".webp"]);
const ESTADOS_FINALES = new Set([
  "EXTRAIDO",
  "VALIDADO",
  "APROBADO",
  "OBSERVADO",
  "RECHAZADO",
  "CERRADO",
]);

const config = {
  url: process.env.NEXTDOCS_URL ?? "http://localhost:8090",
  tenant: process.env.NEXTDOCS_TENANT,
  email: process.env.NEXTDOCS_EMAIL,
  clave: process.env.NEXTDOCS_CLAVE,
  esperaMaximaMs: Number(process.env.NEXTDOCS_ESPERA_MS ?? 120000),
};

function abortar(mensaje) {
  console.error(`\n  ${mensaje}\n`);
  process.exit(1);
}

function uso() {
  console.log(`
  Carga una carpeta de documentos en NextDocs y reporta que detecto y extrajo cada uno.

    node scripts/cargar-dataset.mjs <carpeta> [--informe salida.md]

  Credenciales por variables de entorno (el script nunca las escribe a disco):

    NEXTDOCS_URL      por defecto http://localhost:8090
    NEXTDOCS_TENANT   codigo de la organizacion
    NEXTDOCS_EMAIL    usuario
    NEXTDOCS_CLAVE    clave

  Si la carpeta tiene un esperado.json con { "archivo.pdf": "FACTURA" }, el informe
  compara el tipo detectado contra el esperado y calcula el acierto.
`);
}

async function ingresar() {
  const respuesta = await fetch(`${config.url}/api/v1/autenticacion/ingresar`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      codigoTenant: config.tenant,
      email: config.email,
      clave: config.clave,
    }),
  });
  if (!respuesta.ok) {
    abortar(`No se pudo entrar (${respuesta.status}). Revisa NEXTDOCS_TENANT, NEXTDOCS_EMAIL y NEXTDOCS_CLAVE.`);
  }
  const sesion = await respuesta.json();
  return sesion.tokenAcceso;
}

async function subir(token, carpeta, archivo) {
  const contenido = await readFile(join(carpeta, archivo));
  const formulario = new FormData();
  formulario.append("archivo", new Blob([contenido]), archivo);
  formulario.append(
    "datos",
    new Blob([JSON.stringify({ origen: "WEB" })], { type: "application/json" }),
  );
  const respuesta = await fetch(`${config.url}/api/v1/documentos`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body: formulario,
  });
  if (!respuesta.ok) {
    return { error: `${respuesta.status} ${(await respuesta.text()).slice(0, 200)}` };
  }
  return { documento: await respuesta.json() };
}

async function detalle(token, documentoId) {
  const respuesta = await fetch(`${config.url}/api/v1/documentos/${documentoId}/detalle`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!respuesta.ok) return null;
  return respuesta.json();
}

async function esperarProcesamiento(token, documentoId) {
  const limite = Date.now() + config.esperaMaximaMs;
  let ultimo = null;
  while (Date.now() < limite) {
    ultimo = await detalle(token, documentoId);
    const estado = ultimo?.documento?.estado;
    if (estado && ESTADOS_FINALES.has(estado)) return ultimo;
    await new Promise((listo) => setTimeout(listo, 2000));
  }
  return ultimo;
}

function resumirCampos(extraccion) {
  const valores = extraccion?.valores ?? [];
  const conValor = valores.filter(
    (valor) => valor.valorNormalizado != null && String(valor.valorNormalizado).trim() !== "",
  );
  return { total: valores.length, conValor: conValor.length };
}

async function main() {
  const argumentos = process.argv.slice(2);
  if (!argumentos.length || argumentos.includes("--help")) {
    uso();
    process.exit(argumentos.length ? 0 : 1);
  }

  const carpeta = resolve(argumentos[0]);
  const indiceInforme = argumentos.indexOf("--informe");
  const rutaInforme = indiceInforme >= 0 ? argumentos[indiceInforme + 1] : null;

  for (const [clave, nombre] of [
    ["tenant", "NEXTDOCS_TENANT"],
    ["email", "NEXTDOCS_EMAIL"],
    ["clave", "NEXTDOCS_CLAVE"],
  ]) {
    if (!config[clave]) abortar(`Falta la variable ${nombre}. Corre con --help para ver el uso.`);
  }

  const info = await stat(carpeta).catch(() => null);
  if (!info?.isDirectory()) abortar(`${carpeta} no es una carpeta.`);

  const entradas = await readdir(carpeta);
  const archivos = entradas
    .filter((archivo) => EXTENSIONES.has(extname(archivo).toLowerCase()))
    .sort();
  if (!archivos.length) abortar(`No hay documentos con extension soportada en ${carpeta}.`);

  let esperado = {};
  if (entradas.includes("esperado.json")) {
    esperado = JSON.parse(await readFile(join(carpeta, "esperado.json"), "utf8"));
  }

  console.log(`\n  ${archivos.length} documentos en ${carpeta}`);
  console.log(`  Destino: ${config.url} (organizacion ${config.tenant})\n`);

  const token = await ingresar();
  const filas = [];

  for (const archivo of archivos) {
    process.stdout.write(`  ${archivo.padEnd(44)} `);
    const subida = await subir(token, carpeta, archivo);
    if (subida.error) {
      console.log(`fallo la carga: ${subida.error}`);
      filas.push({ archivo, error: subida.error });
      continue;
    }
    const completo = await esperarProcesamiento(token, subida.documento.id);
    const documento = completo?.documento ?? subida.documento;
    const campos = resumirCampos(completo?.extraccion);
    const fila = {
      archivo,
      id: documento.id,
      estado: documento.estado,
      tipo: documento.codigoPlantilla ?? "—",
      origen: documento.origenTipo ?? "—",
      confianza: documento.confianzaTipo ?? null,
      campos: campos.total,
      camposConValor: campos.conValor,
      hallazgos: completo?.validacion?.hallazgos?.length ?? 0,
      esperado: esperado[archivo] ?? null,
    };
    fila.acierto = fila.esperado ? fila.esperado === fila.tipo : null;
    filas.push(fila);
    const marca = fila.acierto === null ? " " : fila.acierto ? "ok" : "NO";
    console.log(
      `${String(fila.tipo).padEnd(18)} ${String(fila.origen).padEnd(10)} ` +
        `${fila.camposConValor}/${fila.campos} campos  ${marca}`,
    );
  }

  const conEsperado = filas.filter((fila) => fila.esperado);
  const aciertos = conEsperado.filter((fila) => fila.acierto).length;
  const genericos = filas.filter((fila) => fila.origen === "GENERICO").length;
  const fallidos = filas.filter((fila) => fila.error).length;

  console.log(`\n  ${filas.length - fallidos}/${filas.length} cargados`);
  console.log(`  ${genericos} cayeron al esquema generico`);
  if (conEsperado.length) {
    const porcentaje = ((aciertos / conEsperado.length) * 100).toFixed(0);
    console.log(`  ${aciertos}/${conEsperado.length} tipos correctos (${porcentaje}%)`);
  }
  console.log("");

  if (rutaInforme) {
    const lineas = [
      "# Carga de dataset",
      "",
      `- Carpeta: \`${carpeta}\``,
      `- Destino: ${config.url} (organizacion ${config.tenant})`,
      `- Fecha: ${new Date().toISOString()}`,
      `- Cargados: ${filas.length - fallidos}/${filas.length}`,
      `- Cayeron al esquema generico: ${genericos}`,
      conEsperado.length
        ? `- Tipos correctos: ${aciertos}/${conEsperado.length}`
        : "- Sin esperado.json, no se midio el acierto",
      "",
      "| Archivo | Estado | Tipo detectado | Origen | Confianza | Campos con valor | Hallazgos | Esperado |",
      "|---|---|---|---|---|---|---|---|",
      ...filas.map((fila) =>
        fila.error
          ? `| ${fila.archivo} | error | — | — | — | — | — | ${fila.esperado ?? "—"} |`
          : `| ${fila.archivo} | ${fila.estado} | ${fila.tipo} | ${fila.origen} | ` +
            `${fila.confianza ?? "—"} | ${fila.camposConValor}/${fila.campos} | ` +
            `${fila.hallazgos} | ${fila.esperado ?? "—"}${fila.acierto === false ? " (no coincide)" : ""} |`,
      ),
      "",
    ];
    await writeFile(rutaInforme, lineas.join("\n"));
    console.log(`  Informe en ${rutaInforme}\n`);
  }

  process.exit(fallidos ? 1 : 0);
}

main().catch((error) => abortar(error.stack ?? String(error)));
