#!/usr/bin/env node
import { mkdir, writeFile } from "node:fs/promises";
import { createRequire } from "node:module";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const raiz = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const requerir = createRequire(resolve(raiz, "frontend", "package.json"));

let chromium;
try {
  ({ chromium } = requerir("playwright"));
} catch {
  console.error(
    "\n  Falta playwright. Instalalo con: cd frontend && npm install\n",
  );
  process.exit(1);
}

const PESOS_CUIT = [5, 4, 3, 2, 7, 6, 5, 4, 3, 2];

function cuit(prefijo, base) {
  const cuerpo = `${prefijo}${base}`;
  const suma = cuerpo
    .split("")
    .reduce((total, digito, indice) => total + Number(digito) * PESOS_CUIT[indice], 0);
  const resto = 11 - (suma % 11);
  const verificador = resto === 11 ? 0 : resto === 10 ? 9 : resto;
  return `${prefijo}-${base}-${verificador}`;
}

function cuitInvalido(prefijo, base) {
  const valido = cuit(prefijo, base);
  const verificador = Number(valido.slice(-1));
  return `${valido.slice(0, -1)}${(verificador + 1) % 10}`;
}

const EMISOR = {
  razon: "Logistica del Plata S.A.",
  cuit: cuit("30", "71458963"),
  domicilio: "Av. Corrientes 1234, Piso 8, C.A.B.A.",
  iibb: "901-234567-8",
  inicio: "01/03/2015",
};

const CLIENTE = {
  razon: "Importadora Andina S.R.L.",
  cuit: cuit("30", "70912345"),
  domicilio: "Ruta 9 Km 42, Escobar, Buenos Aires",
  condicion: "IVA Responsable Inscripto",
};

const ESTILO = `
  * { box-sizing: border-box; }
  body { font-family: "DejaVu Sans", Arial, sans-serif; font-size: 11px; color: #111; margin: 0; padding: 28px 34px; }
  .marco { border: 1px solid #222; }
  .cabecera { display: flex; border-bottom: 1px solid #222; }
  .cabecera > div { padding: 10px 12px; }
  .emisor { width: 46%; border-right: 1px solid #222; }
  .letra { width: 8%; text-align: center; border-right: 1px solid #222; display: flex; flex-direction: column; align-items: center; justify-content: center; }
  .letra strong { font-size: 30px; line-height: 1; }
  .letra span { font-size: 8px; }
  .comprobante { width: 46%; }
  h1 { font-size: 15px; margin: 0 0 4px; }
  h2 { font-size: 13px; margin: 0 0 6px; }
  .fila { display: flex; gap: 18px; flex-wrap: wrap; }
  .fila > div { min-width: 150px; }
  .bloque { padding: 10px 12px; border-bottom: 1px solid #222; }
  .etiqueta { color: #555; font-size: 9px; text-transform: uppercase; letter-spacing: .4px; }
  table { width: 100%; border-collapse: collapse; margin-top: 4px; }
  th { background: #eee; text-align: left; font-size: 9px; text-transform: uppercase; padding: 5px 6px; border-bottom: 1px solid #222; }
  td { padding: 5px 6px; border-bottom: 1px solid #ddd; }
  .num { text-align: right; }
  .totales { margin-left: auto; width: 260px; }
  .totales td { border: none; padding: 3px 6px; }
  .totales .final td { border-top: 1px solid #222; font-weight: bold; font-size: 13px; padding-top: 6px; }
  .pie { padding: 10px 12px; display: flex; justify-content: space-between; font-size: 10px; }
  .sello { border: 2px solid #a00; color: #a00; display: inline-block; padding: 4px 10px; font-weight: bold; transform: rotate(-3deg); }
`;

function encabezadoFiscal(titulo, letra, codigo, numero, fecha) {
  return `
  <div class="cabecera">
    <div class="emisor">
      <h1>${EMISOR.razon}</h1>
      <div>${EMISOR.domicilio}</div>
      <div>CUIT: ${EMISOR.cuit}</div>
      <div>Ingresos Brutos: ${EMISOR.iibb}</div>
      <div>Inicio de actividades: ${EMISOR.inicio}</div>
      <div>IVA Responsable Inscripto</div>
    </div>
    <div class="letra"><strong>${letra}</strong><span>Cod. ${codigo}</span></div>
    <div class="comprobante">
      <h2>${titulo}</h2>
      <div><span class="etiqueta">Numero</span> ${numero}</div>
      <div><span class="etiqueta">Fecha de emision</span> ${fecha}</div>
    </div>
  </div>`;
}

function bloqueCliente(extra = "") {
  return `
  <div class="bloque">
    <div class="fila">
      <div><span class="etiqueta">Razon social</span><div>${CLIENTE.razon}</div></div>
      <div><span class="etiqueta">CUIT</span><div>${CLIENTE.cuit}</div></div>
      <div><span class="etiqueta">Condicion frente al IVA</span><div>${CLIENTE.condicion}</div></div>
      <div><span class="etiqueta">Domicilio</span><div>${CLIENTE.domicilio}</div></div>
      ${extra}
    </div>
  </div>`;
}

function tablaItems(items) {
  return `
  <div class="bloque">
    <table>
      <thead><tr><th>Codigo</th><th>Descripcion</th><th class="num">Cant.</th><th class="num">P. unitario</th><th class="num">Subtotal</th></tr></thead>
      <tbody>
        ${items
          .map(
            (item) => `<tr><td>${item.codigo}</td><td>${item.detalle}</td>
          <td class="num">${item.cantidad}</td>
          <td class="num">${item.unitario.toLocaleString("es-AR", { minimumFractionDigits: 2 })}</td>
          <td class="num">${(item.cantidad * item.unitario).toLocaleString("es-AR", { minimumFractionDigits: 2 })}</td></tr>`,
          )
          .join("")}
      </tbody>
    </table>
  </div>`;
}

function totales(neto, iva) {
  const total = neto + iva;
  return {
    total,
    html: `
  <div class="bloque">
    <table class="totales">
      <tr><td>Subtotal neto gravado</td><td class="num">$ ${neto.toLocaleString("es-AR", { minimumFractionDigits: 2 })}</td></tr>
      <tr><td>IVA 21%</td><td class="num">$ ${iva.toLocaleString("es-AR", { minimumFractionDigits: 2 })}</td></tr>
      <tr class="final"><td>Total</td><td class="num">$ ${total.toLocaleString("es-AR", { minimumFractionDigits: 2 })}</td></tr>
    </table>
  </div>`,
  };
}

function pieFiscal(cae, vencimiento) {
  return `<div class="pie"><span>CAE N.o ${cae}</span><span>Vencimiento del CAE: ${vencimiento}</span></div>`;
}

function factura({ letra, codigo, numero, fecha, items, neto, iva, cae, venceCae, cuitEmisor }) {
  const suma = totales(neto, iva);
  const cabecera = encabezadoFiscal("FACTURA", letra, codigo, numero, fecha).replace(
    EMISOR.cuit,
    cuitEmisor ?? EMISOR.cuit,
  );
  return `<div class="marco">${cabecera}${bloqueCliente()}${tablaItems(items)}${suma.html}${pieFiscal(cae, venceCae)}</div>`;
}

function notaCredito() {
  const suma = totales(48000, 10080);
  return `<div class="marco">
    ${encabezadoFiscal("NOTA DE CREDITO", "A", "003", "0004-00001187", "22/08/2026")}
    ${bloqueCliente(
      '<div><span class="etiqueta">Comprobante asociado</span><div>Factura A 0004-00001172</div></div>',
    )}
    ${tablaItems([
      { codigo: "DEV-114", detalle: "Devolucion parcial de mercaderia dañada en transito", cantidad: 12, unitario: 4000 },
    ])}
    ${suma.html}
    ${pieFiscal("76220118904471", "01/09/2026")}
  </div>`;
}

function notaDebito() {
  const suma = totales(15500, 3255);
  return `<div class="marco">
    ${encabezadoFiscal("NOTA DE DEBITO", "B", "007", "0004-00000342", "28/08/2026")}
    ${bloqueCliente(
      '<div><span class="etiqueta">Comprobante asociado</span><div>Factura B 0004-00000318</div></div>',
    )}
    ${tablaItems([
      { codigo: "INT-002", detalle: "Intereses por pago fuera de termino", cantidad: 1, unitario: 15500 },
    ])}
    ${suma.html}
    ${pieFiscal("76220118912004", "07/09/2026")}
  </div>`;
}

function remito({ numero, fecha, transporte, patente, items, conformado }) {
  return `<div class="marco">
    ${encabezadoFiscal("REMITO", "R", "091", numero, fecha)}
    ${bloqueCliente(
      `<div><span class="etiqueta">Transporte</span><div>${transporte}</div></div>
       <div><span class="etiqueta">Patente</span><div>${patente}</div></div>`,
    )}
    <div class="bloque">
      <table>
        <thead><tr><th>Codigo</th><th>Descripcion</th><th class="num">Bultos</th><th class="num">Kg</th></tr></thead>
        <tbody>
          ${items
            .map(
              (item) =>
                `<tr><td>${item.codigo}</td><td>${item.detalle}</td><td class="num">${item.bultos}</td><td class="num">${item.kilos}</td></tr>`,
            )
            .join("")}
        </tbody>
      </table>
    </div>
    <div class="bloque">
      <div class="fila">
        <div><span class="etiqueta">Recibido por</span><div>${conformado ? "R. Gimenez — DNI 28.114.902" : "____________________"}</div></div>
        <div><span class="etiqueta">Aclaracion</span><div>${conformado ? "Deposito Escobar" : "____________________"}</div></div>
        <div><span class="etiqueta">Fecha de recepcion</span><div>${conformado ? fecha : "____/____/________"}</div></div>
      </div>
      ${conformado ? '<div style="margin-top:8px" class="sello">CONFORMADO</div>' : ""}
    </div>
    <div class="pie"><span>Documento no valido como factura</span><span>Original — Remitente</span></div>
  </div>`;
}

function constanciaCuit() {
  return `<div class="marco">
    <div class="bloque" style="text-align:center">
      <h1>ADMINISTRACION FEDERAL DE INGRESOS PUBLICOS</h1>
      <h2>CONSTANCIA DE INSCRIPCION</h2>
      <div>Emitida el 02/09/2026</div>
    </div>
    <div class="bloque">
      <div class="fila">
        <div><span class="etiqueta">CUIT</span><div style="font-size:15px"><strong>${EMISOR.cuit}</strong></div></div>
        <div><span class="etiqueta">Denominacion</span><div>${EMISOR.razon}</div></div>
        <div><span class="etiqueta">Domicilio fiscal</span><div>${EMISOR.domicilio}</div></div>
        <div><span class="etiqueta">Fecha de contrato social</span><div>${EMISOR.inicio}</div></div>
      </div>
    </div>
    <div class="bloque">
      <table>
        <thead><tr><th>Impuesto</th><th>Descripcion</th><th>Periodo desde</th></tr></thead>
        <tbody>
          <tr><td>30</td><td>IVA</td><td>03/2015</td></tr>
          <tr><td>10</td><td>Ganancias sociedades</td><td>03/2015</td></tr>
          <tr><td>301</td><td>Empleador — aportes de seguridad social</td><td>05/2015</td></tr>
        </tbody>
      </table>
    </div>
    <div class="bloque">
      <span class="etiqueta">Actividad principal</span>
      <div>522090 — Servicios de gestion logistica para el transporte de mercaderias</div>
    </div>
    <div class="pie"><span>Datos ficticios generados para prueba</span><span>Constancia sin validez fiscal</span></div>
  </div>`;
}

function certificadoVtv() {
  return `<div class="marco">
    <div class="bloque" style="text-align:center">
      <h1>VERIFICACION TECNICA VEHICULAR</h1>
      <h2>Certificado de aprobacion</h2>
      <div>Provincia de Buenos Aires — Planta habilitada N.o 118</div>
    </div>
    <div class="bloque">
      <div class="fila">
        <div><span class="etiqueta">Dominio</span><div style="font-size:15px"><strong>AF 412 KL</strong></div></div>
        <div><span class="etiqueta">Marca y modelo</span><div>Iveco Daily 55C17</div></div>
        <div><span class="etiqueta">Año</span><div>2019</div></div>
        <div><span class="etiqueta">Tipo</span><div>Camion liviano</div></div>
        <div><span class="etiqueta">Motor</span><div>F1CE3481B1234567</div></div>
        <div><span class="etiqueta">Chasis</span><div>93ZL68B01L8412345</div></div>
      </div>
    </div>
    <div class="bloque">
      <div class="fila">
        <div><span class="etiqueta">Fecha de inspeccion</span><div>18/06/2026</div></div>
        <div><span class="etiqueta">Vencimiento</span><div>18/06/2027</div></div>
        <div><span class="etiqueta">Resultado</span><div><strong>APTO</strong></div></div>
        <div><span class="etiqueta">Oblea</span><div>B-2026-4471902</div></div>
      </div>
      <div style="margin-top:10px" class="sello">APTO PARA CIRCULAR</div>
    </div>
    <div class="pie"><span>Titular: ${EMISOR.razon}</span><span>CUIT ${EMISOR.cuit}</span></div>
  </div>`;
}

function polizaSeguro() {
  return `<div class="marco">
    <div class="bloque">
      <h1>Aseguradora del Litoral S.A.</h1>
      <h2>POLIZA DE SEGURO AUTOMOTOR</h2>
    </div>
    <div class="bloque">
      <div class="fila">
        <div><span class="etiqueta">Numero de poliza</span><div><strong>AUT-2026-884120</strong></div></div>
        <div><span class="etiqueta">Tomador</span><div>${EMISOR.razon}</div></div>
        <div><span class="etiqueta">CUIT</span><div>${EMISOR.cuit}</div></div>
        <div><span class="etiqueta">Cobertura</span><div>Responsabilidad civil + todo riesgo con franquicia</div></div>
      </div>
    </div>
    <div class="bloque">
      <div class="fila">
        <div><span class="etiqueta">Dominio</span><div>AF 412 KL</div></div>
        <div><span class="etiqueta">Vehiculo</span><div>Iveco Daily 55C17 — 2019</div></div>
        <div><span class="etiqueta">Vigencia desde</span><div>01/07/2026</div></div>
        <div><span class="etiqueta">Vigencia hasta</span><div>01/07/2027</div></div>
        <div><span class="etiqueta">Suma asegurada</span><div>$ 42.000.000,00</div></div>
        <div><span class="etiqueta">Premio mensual</span><div>$ 168.400,00</div></div>
      </div>
    </div>
    <div class="pie"><span>Superintendencia de Seguros de la Nacion — Matricula 0442</span><span>Datos ficticios</span></div>
  </div>`;
}

function cedulaVehicular() {
  return `<div class="marco">
    <div class="bloque" style="text-align:center">
      <h1>DIRECCION NACIONAL DE LOS REGISTROS NACIONALES</h1>
      <h2>CEDULA DE IDENTIFICACION DEL AUTOMOTOR</h2>
    </div>
    <div class="bloque">
      <div class="fila">
        <div><span class="etiqueta">Dominio</span><div style="font-size:15px"><strong>AF 412 KL</strong></div></div>
        <div><span class="etiqueta">Marca</span><div>Iveco</div></div>
        <div><span class="etiqueta">Modelo</span><div>Daily 55C17</div></div>
        <div><span class="etiqueta">Tipo</span><div>Chasis con cabina</div></div>
        <div><span class="etiqueta">Año de fabricacion</span><div>2019</div></div>
        <div><span class="etiqueta">Motor N.o</span><div>F1CE3481B1234567</div></div>
        <div><span class="etiqueta">Chasis N.o</span><div>93ZL68B01L8412345</div></div>
        <div><span class="etiqueta">Titular</span><div>${EMISOR.razon}</div></div>
        <div><span class="etiqueta">CUIT</span><div>${EMISOR.cuit}</div></div>
        <div><span class="etiqueta">Registro seccional</span><div>Escobar N.o 2</div></div>
        <div><span class="etiqueta">Fecha de emision</span><div>12/04/2019</div></div>
      </div>
    </div>
    <div class="pie"><span>Documento de muestra con datos ficticios</span><span>Sin validez legal</span></div>
  </div>`;
}

function contratoLocacion() {
  return `<div style="max-width:660px">
    <h1 style="text-align:center">CONTRATO DE LOCACION DE DEPOSITO</h1>
    <p>En la Ciudad Autonoma de Buenos Aires, a los 3 dias del mes de agosto de 2026, entre
    <strong>${EMISOR.razon}</strong>, CUIT ${EMISOR.cuit}, con domicilio en ${EMISOR.domicilio},
    en adelante EL LOCATARIO, y <strong>Inmobiliaria Ribera S.A.</strong>, CUIT ${cuit("30", "69874512")},
    en adelante EL LOCADOR, se conviene celebrar el presente contrato sujeto a las siguientes clausulas.</p>
    <p><strong>PRIMERA — Objeto.</strong> EL LOCADOR da en locacion el deposito sito en Ruta 9 Km 44,
    partido de Escobar, con una superficie cubierta de 1.850 metros cuadrados, destinado exclusivamente
    al acopio de mercaderia general.</p>
    <p><strong>SEGUNDA — Plazo.</strong> El plazo de locacion se fija en treinta y seis meses contados
    a partir del 1 de septiembre de 2026, venciendo en consecuencia el 31 de agosto de 2029.</p>
    <p><strong>TERCERA — Precio.</strong> El precio mensual se fija en la suma de pesos cuatro millones
    doscientos mil ($ 4.200.000), pagaderos del 1 al 10 de cada mes.</p>
    <p><strong>CUARTA — Ajuste.</strong> El canon se ajustara semestralmente conforme al Indice de
    Precios al Consumidor publicado por el INDEC.</p>
    <p><strong>QUINTA — Deposito en garantia.</strong> EL LOCATARIO entrega en este acto la suma
    equivalente a dos meses de alquiler en concepto de garantia.</p>
    <p style="margin-top:48px">_______________________&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;_______________________</p>
    <p>EL LOCADOR&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;EL LOCATARIO</p>
  </div>`;
}

const ITEMS_A = [
  { codigo: "FLE-100", detalle: "Flete Buenos Aires - Rosario, camion completo", cantidad: 4, unitario: 185000 },
  { codigo: "ALM-220", detalle: "Almacenaje en deposito fiscal, pallet por mes", cantidad: 30, unitario: 12400 },
];

const ITEMS_B = [
  { codigo: "DIS-045", detalle: "Distribucion urbana ultima milla, reparto", cantidad: 120, unitario: 4850 },
];

const DOCUMENTOS = [
  {
    archivo: "factura-a-0004-00001204.pdf",
    esperado: "FACTURA",
    nota: "Factura A completa, CUIT valido",
    html: factura({
      letra: "A", codigo: "001", numero: "0004-00001204", fecha: "05/09/2026",
      items: ITEMS_A, neto: 1112000, iva: 233520,
      cae: "76220118934502", venceCae: "15/09/2026",
    }),
  },
  {
    archivo: "factura-b-0004-00000355.pdf",
    esperado: "FACTURA",
    nota: "Factura B, otro layout de items",
    html: factura({
      letra: "B", codigo: "006", numero: "0004-00000355", fecha: "01/09/2026",
      items: ITEMS_B, neto: 582000, iva: 122220,
      cae: "76220118928814", venceCae: "11/09/2026",
    }),
  },
  {
    archivo: "factura-c-0002-00000097.pdf",
    esperado: "FACTURA",
    nota: "Factura C de monotributista, sin discriminar IVA",
    html: factura({
      letra: "C", codigo: "011", numero: "0002-00000097", fecha: "27/08/2026",
      items: [{ codigo: "SER-001", detalle: "Servicio de consultoria logistica", cantidad: 1, unitario: 340000 }],
      neto: 340000, iva: 0,
      cae: "76220118901337", venceCae: "06/09/2026",
    }),
  },
  {
    archivo: "factura-a-cuit-invalido.pdf",
    esperado: "FACTURA",
    nota: "Factura con digito verificador de CUIT mal calculado, deberia disparar CUIT_INVALIDO",
    html: factura({
      letra: "A", codigo: "001", numero: "0004-00001205", fecha: "06/09/2026",
      items: ITEMS_B, neto: 582000, iva: 122220,
      cae: "76220118934777", venceCae: "16/09/2026",
      cuitEmisor: cuitInvalido("30", "71458963"),
    }),
  },
  {
    archivo: "factura-a-fecha-futura.pdf",
    esperado: "FACTURA",
    nota: "Factura emitida con fecha posterior a hoy, deberia disparar FECHA_FUTURA",
    html: factura({
      letra: "A", codigo: "001", numero: "0004-00001299", fecha: "14/12/2027",
      items: ITEMS_A, neto: 1112000, iva: 233520,
      cae: "76220119004411", venceCae: "24/12/2027",
    }),
  },
  { archivo: "nota-credito-a-0004-00001187.pdf", esperado: "NOTA_CREDITO", nota: "Nota de credito con comprobante asociado", html: notaCredito() },
  { archivo: "nota-debito-b-0004-00000342.pdf", esperado: "NOTA_DEBITO", nota: "Nota de debito por intereses", html: notaDebito() },
  {
    archivo: "remito-0004-00009912-conformado.pdf",
    esperado: "REMITO",
    nota: "Remito conformado con firma y sello",
    html: remito({
      numero: "0004-00009912", fecha: "29/08/2026", transporte: "Transportes Ruta 9 S.R.L.", patente: "AD 771 QR",
      conformado: true,
      items: [
        { codigo: "PAL-01", detalle: "Pallets de mercaderia general", bultos: 18, kilos: "7.420" },
        { codigo: "CAJ-77", detalle: "Cajas de repuestos", bultos: 44, kilos: "1.180" },
      ],
    }),
  },
  {
    archivo: "remito-0004-00009940-sin-conformar.pdf",
    esperado: "REMITO",
    nota: "Remito sin conformar, campos de recepcion en blanco",
    html: remito({
      numero: "0004-00009940", fecha: "04/09/2026", transporte: "Logistica Norte S.A.", patente: "AE 208 MB",
      conformado: false,
      items: [{ codigo: "PAL-02", detalle: "Pallets de insumos industriales", bultos: 26, kilos: "9.860" }],
    }),
  },
  { archivo: "constancia-inscripcion-afip.pdf", esperado: "CONSTANCIA_CUIT", nota: "Constancia de inscripcion con impuestos y actividad", html: constanciaCuit() },
  { archivo: "certificado-vtv.pdf", esperado: "VTV", nota: "Certificado VTV aprobado con vencimiento", html: certificadoVtv() },
  { archivo: "poliza-seguro-automotor.pdf", esperado: "SEGURO_VEHICULAR", nota: "Poliza de seguro con vigencia y dominio", html: polizaSeguro() },
  { archivo: "cedula-vehicular.pdf", esperado: "CEDULA_VEHICULAR", nota: "Cedula del automotor", html: cedulaVehicular() },
  { archivo: "contrato-locacion-deposito.pdf", esperado: "GENERICO", nota: "Contrato: no existe en el catalogo, deberia caer al esquema generico", html: contratoLocacion() },
];

async function main() {
  const destino = resolve(process.argv[2] ?? "dataset-prueba");
  await mkdir(destino, { recursive: true });

  const navegador = await chromium.launch();
  const pagina = await navegador.newPage();
  const esperado = {};

  for (const documento of DOCUMENTOS) {
    await pagina.setContent(`<style>${ESTILO}</style>${documento.html}`, {
      waitUntil: "load",
    });
    await pagina.pdf({
      path: resolve(destino, documento.archivo),
      format: "A4",
      printBackground: true,
      margin: { top: "12mm", bottom: "12mm", left: "12mm", right: "12mm" },
    });
    esperado[documento.archivo] = documento.esperado;
    console.log(`  ${documento.archivo.padEnd(42)} ${documento.esperado.padEnd(18)} ${documento.nota}`);
  }

  await navegador.close();

  esperado["afip-manual-constancia.pdf"] = "GENERICO";

  await writeFile(resolve(destino, "esperado.json"), JSON.stringify(esperado, null, 2) + "\n");
  console.log(`\n  ${DOCUMENTOS.length} documentos generados en ${destino}`);
  console.log(`  esperado.json con ${Object.keys(esperado).length} entradas\n`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
