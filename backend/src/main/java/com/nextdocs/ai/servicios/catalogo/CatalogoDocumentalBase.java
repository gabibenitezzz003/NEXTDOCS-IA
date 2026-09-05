package com.nextdocs.ai.servicios.catalogo;

import java.math.BigDecimal;
import java.util.List;

import com.nextdocs.ai.enumeraciones.PoliticaOriginalFisico;
import com.nextdocs.ai.enumeraciones.SeveridadHallazgo;
import com.nextdocs.ai.enumeraciones.TipoDatoCampo;
import com.nextdocs.ai.enumeraciones.TipoReglaValidacion;

public final class CatalogoDocumentalBase {

	public static final String VERSION = "base-1";

	private static final BigDecimal ALTA = new BigDecimal("0.9500");

	private static final BigDecimal MEDIA = new BigDecimal("0.9000");

	private static final BigDecimal BAJA = new BigDecimal("0.8500");

	private CatalogoDocumentalBase() {
	}

	public record CampoBase(String clave, String etiqueta, TipoDatoCampo tipoDato, boolean requerido,
			BigDecimal umbral, String alias) {

		public static CampoBase de(String clave, String etiqueta, TipoDatoCampo tipoDato, boolean requerido,
				BigDecimal umbral) {
			return new CampoBase(clave, etiqueta, tipoDato, requerido, umbral, null);
		}

		public static CampoBase con(String clave, String etiqueta, TipoDatoCampo tipoDato, boolean requerido,
				BigDecimal umbral, String alias) {
			return new CampoBase(clave, etiqueta, tipoDato, requerido, umbral, alias);
		}
	}

	public record ReglaBase(String codigo, String nombre, TipoReglaValidacion tipo, SeveridadHallazgo severidad,
			String campoObjetivo, String mensaje) {
	}

	public record TipoBase(String codigo, String nombre, String familia, String descripcion,
			BigDecimal umbralAutoaprobacion, PoliticaOriginalFisico politicaFisica, List<CampoBase> campos,
			List<ReglaBase> reglas) {
	}

	private static ReglaBase vencimiento(String campo) {
		return new ReglaBase("VENCIDO", "Documento vencido", TipoReglaValidacion.VIGENCIA,
				SeveridadHallazgo.BLOQUEANTE, campo, "El documento esta vencido a la fecha de la revision");
	}

	private static ReglaBase cuitValido(String campo) {
		return new ReglaBase("CUIT_INVALIDO", "CUIT con digito verificador invalido",
				TipoReglaValidacion.FORMATO, SeveridadHallazgo.REQUIERE_REVISION, campo,
				"El CUIT no supera la validacion del digito verificador");
	}

	private static ReglaBase fechaNoFutura(String campo) {
		return new ReglaBase("FECHA_FUTURA", "Fecha de emision futura", TipoReglaValidacion.RANGO,
				SeveridadHallazgo.REQUIERE_REVISION, campo,
				"La fecha de emision es posterior a hoy, lo que no puede pasar en un documento real");
	}

	public static final TipoBase REMITO = new TipoBase("REMITO", "Remito conformado", "LOGISTICO",
			"Comprobante de entrega de mercaderia, con numero de comprobante, emisor, destinatario y conformidad",
			new BigDecimal("0.9200"), PoliticaOriginalFisico.REQUIERE_PARA_CIERRE,
			List.of(
					CampoBase.con("numero", "Numero de remito", TipoDatoCampo.TEXTO, true, MEDIA,
							"nro remito, comprobante"),
					CampoBase.de("fechaEmision", "Fecha de emision", TipoDatoCampo.FECHA, true, MEDIA),
					CampoBase.de("cuitEmisor", "CUIT del emisor", TipoDatoCampo.CUIT, true, ALTA),
					CampoBase.de("razonSocialEmisor", "Razon social del emisor", TipoDatoCampo.TEXTO, true, BAJA),
					CampoBase.de("cuitDestinatario", "CUIT del destinatario", TipoDatoCampo.CUIT, false, MEDIA),
					CampoBase.de("razonSocialDestinatario", "Razon social del destinatario", TipoDatoCampo.TEXTO,
							true, BAJA),
					CampoBase.de("domicilioEntrega", "Domicilio de entrega", TipoDatoCampo.TEXTO, true, BAJA),
					CampoBase.de("transportista", "Transportista", TipoDatoCampo.TEXTO, false, BAJA),
					CampoBase.con("nroPedido", "Numero de pedido", TipoDatoCampo.TEXTO, false, BAJA,
							"orden, OC, pedido"),
					CampoBase.de("totalBultos", "Total de bultos", TipoDatoCampo.NUMERO, false, BAJA),
					CampoBase.con("conformado", "Conformado por el destinatario", TipoDatoCampo.BOOLEANO, true,
							MEDIA, "firmado, recibido conforme"),
					CampoBase.de("observaciones", "Observaciones", TipoDatoCampo.TEXTO, false, BAJA)),
			List.of(fechaNoFutura("fechaEmision"), cuitValido("cuitEmisor"),
					new ReglaBase("REMITO_SIN_CONFORMAR", "Remito sin conformar", TipoReglaValidacion.OBLIGATORIO,
							SeveridadHallazgo.ADVERTENCIA, "conformado",
							"El remito no figura conformado por el destinatario")));

	public static final TipoBase FACTURA = new TipoBase("FACTURA", "Factura de compra o venta", "FISCAL",
			"Comprobante fiscal con letra, punto de venta, CAE, neto, IVA y total",
			ALTA, PoliticaOriginalFisico.NO_REQUIERE,
			List.of(
					CampoBase.con("numero", "Numero de comprobante", TipoDatoCampo.TEXTO, true, ALTA,
							"punto de venta y correlativo"),
					CampoBase.con("tipoComprobante", "Tipo de comprobante", TipoDatoCampo.TEXTO, true, MEDIA,
							"letra A, B, C o M"),
					CampoBase.de("fechaEmision", "Fecha de emision", TipoDatoCampo.FECHA, true, ALTA),
					CampoBase.de("cuitEmisor", "CUIT del emisor", TipoDatoCampo.CUIT, true, ALTA),
					CampoBase.de("razonSocialEmisor", "Razon social del emisor", TipoDatoCampo.TEXTO, true, BAJA),
					CampoBase.de("cuitReceptor", "CUIT del receptor", TipoDatoCampo.CUIT, false, MEDIA),
					CampoBase.de("cae", "CAE", TipoDatoCampo.TEXTO, false, MEDIA),
					CampoBase.de("neto", "Importe neto", TipoDatoCampo.MONEDA, true, ALTA),
					CampoBase.de("iva", "IVA", TipoDatoCampo.MONEDA, false, MEDIA),
					CampoBase.de("total", "Importe total", TipoDatoCampo.MONEDA, true, ALTA),
					CampoBase.con("nroOrdenCompra", "Numero de orden de compra", TipoDatoCampo.TEXTO, false, BAJA,
							"OC, orden de compra")),
			List.of(fechaNoFutura("fechaEmision"), cuitValido("cuitEmisor"),
					new ReglaBase("TOTAL_NO_CUADRA", "El total no cierra con neto mas IVA",
							TipoReglaValidacion.COMPARACION_CAMPOS, SeveridadHallazgo.REQUIERE_REVISION, "total",
							"El total no coincide con la suma del neto y el IVA")));

	public static final TipoBase NOTA_CREDITO = new TipoBase("NOTA_CREDITO", "Nota de credito", "FISCAL",
			"Comprobante fiscal que anula o reduce el importe de una factura anterior",
			ALTA, PoliticaOriginalFisico.NO_REQUIERE, FACTURA.campos(), FACTURA.reglas());

	public static final TipoBase NOTA_DEBITO = new TipoBase("NOTA_DEBITO", "Nota de debito", "FISCAL",
			"Comprobante fiscal que aumenta el importe de una factura anterior",
			ALTA, PoliticaOriginalFisico.NO_REQUIERE, FACTURA.campos(), FACTURA.reglas());

	public static final TipoBase CONSTANCIA_CUIT = new TipoBase("CONSTANCIA_CUIT",
			"Constancia de inscripcion ante ARCA", "FISCAL",
			"Constancia con CUIT, razon social, condicion frente al IVA y domicilio fiscal",
			ALTA, PoliticaOriginalFisico.NO_REQUIERE,
			List.of(
					CampoBase.de("cuit", "CUIT", TipoDatoCampo.CUIT, true, ALTA),
					CampoBase.de("razonSocial", "Razon social", TipoDatoCampo.TEXTO, true, MEDIA),
					CampoBase.de("condicionIva", "Condicion frente al IVA", TipoDatoCampo.TEXTO, true, BAJA),
					CampoBase.de("domicilioFiscal", "Domicilio fiscal", TipoDatoCampo.TEXTO, false, BAJA),
					CampoBase.de("actividadPrincipal", "Actividad principal", TipoDatoCampo.TEXTO, false, BAJA),
					CampoBase.de("fechaEmision", "Fecha de emision", TipoDatoCampo.FECHA, false, BAJA)),
			List.of(cuitValido("cuit"), fechaNoFutura("fechaEmision")));

	public static final TipoBase DNI = new TipoBase("DNI", "Documento nacional de identidad", "IDENTIDAD",
			"Documento de identidad argentino, con numero, apellido, nombre y vencimiento",
			new BigDecimal("0.9300"), PoliticaOriginalFisico.NO_REQUIERE,
			List.of(
					CampoBase.con("numeroDocumento", "Numero de documento", TipoDatoCampo.TEXTO, true, ALTA,
							"DNI, documento"),
					CampoBase.de("apellido", "Apellido", TipoDatoCampo.TEXTO, true, MEDIA),
					CampoBase.de("nombre", "Nombre", TipoDatoCampo.TEXTO, true, MEDIA),
					CampoBase.de("fechaNacimiento", "Fecha de nacimiento", TipoDatoCampo.FECHA, false, MEDIA),
					CampoBase.de("fechaVencimiento", "Fecha de vencimiento", TipoDatoCampo.FECHA, true, MEDIA),
					CampoBase.de("cuil", "CUIL", TipoDatoCampo.CUIT, false, MEDIA)),
			List.of(vencimiento("fechaVencimiento"), cuitValido("cuil")));

	public static final TipoBase LICENCIA_CONDUCIR = new TipoBase("LICENCIA_CONDUCIR",
			"Licencia nacional de conducir", "IDENTIDAD",
			"Licencia de conducir con clases habilitadas y fecha de vencimiento",
			new BigDecimal("0.9300"), PoliticaOriginalFisico.NO_REQUIERE,
			List.of(
					CampoBase.de("numeroDocumento", "Numero de documento", TipoDatoCampo.TEXTO, true, ALTA),
					CampoBase.de("apellido", "Apellido", TipoDatoCampo.TEXTO, true, MEDIA),
					CampoBase.de("nombre", "Nombre", TipoDatoCampo.TEXTO, true, MEDIA),
					CampoBase.con("clases", "Clases habilitadas", TipoDatoCampo.TEXTO, true, MEDIA,
							"categorias, habilitaciones"),
					CampoBase.de("fechaVencimiento", "Fecha de vencimiento", TipoDatoCampo.FECHA, true, MEDIA),
					CampoBase.de("jurisdiccion", "Jurisdiccion emisora", TipoDatoCampo.TEXTO, false, BAJA)),
			List.of(vencimiento("fechaVencimiento")));

	public static final TipoBase CEDULA_VEHICULAR = new TipoBase("CEDULA_VEHICULAR",
			"Cedula de identificacion del vehiculo", "VEHICULAR",
			"Cedula verde o azul, con patente, marca, modelo y titular",
			new BigDecimal("0.9200"), PoliticaOriginalFisico.NO_REQUIERE,
			List.of(
					CampoBase.con("patente", "Patente", TipoDatoCampo.TEXTO, true, ALTA, "dominio, chapa"),
					CampoBase.de("marca", "Marca", TipoDatoCampo.TEXTO, true, MEDIA),
					CampoBase.de("modelo", "Modelo", TipoDatoCampo.TEXTO, true, MEDIA),
					CampoBase.de("anio", "Anio", TipoDatoCampo.NUMERO, false, BAJA),
					CampoBase.de("titular", "Titular", TipoDatoCampo.TEXTO, true, MEDIA),
					CampoBase.de("numeroChasis", "Numero de chasis", TipoDatoCampo.TEXTO, false, BAJA),
					CampoBase.de("fechaVencimiento", "Fecha de vencimiento", TipoDatoCampo.FECHA, false, BAJA)),
			List.of(vencimiento("fechaVencimiento")));

	public static final TipoBase VTV = new TipoBase("VTV", "Verificacion tecnica vehicular", "VEHICULAR",
			"Comprobante de verificacion tecnica, con patente, resultado y vencimiento",
			new BigDecimal("0.9200"), PoliticaOriginalFisico.NO_REQUIERE,
			List.of(
					CampoBase.con("patente", "Patente", TipoDatoCampo.TEXTO, true, ALTA, "dominio"),
					CampoBase.de("fechaInspeccion", "Fecha de inspeccion", TipoDatoCampo.FECHA, true, MEDIA),
					CampoBase.de("fechaVencimiento", "Fecha de vencimiento", TipoDatoCampo.FECHA, true, MEDIA),
					CampoBase.con("resultado", "Resultado", TipoDatoCampo.TEXTO, true, MEDIA,
							"apto, condicional, rechazado"),
					CampoBase.de("numeroOblea", "Numero de oblea", TipoDatoCampo.TEXTO, false, BAJA),
					CampoBase.de("taller", "Taller", TipoDatoCampo.TEXTO, false, BAJA)),
			List.of(vencimiento("fechaVencimiento"),
					new ReglaBase("VERIFICACION_NO_APROBADA", "Verificacion no aprobada",
							TipoReglaValidacion.CATALOGO, SeveridadHallazgo.BLOQUEANTE, "resultado",
							"La verificacion tecnica no figura aprobada")));

	public static final TipoBase SEGURO_VEHICULAR = new TipoBase("SEGURO_VEHICULAR",
			"Poliza de seguro del vehiculo", "VEHICULAR",
			"Poliza con patente, aseguradora, numero de poliza, cobertura y vigencia",
			new BigDecimal("0.9200"), PoliticaOriginalFisico.NO_REQUIERE,
			List.of(
					CampoBase.con("patente", "Patente", TipoDatoCampo.TEXTO, true, MEDIA, "dominio"),
					CampoBase.de("aseguradora", "Aseguradora", TipoDatoCampo.TEXTO, true, MEDIA),
					CampoBase.de("numeroPoliza", "Numero de poliza", TipoDatoCampo.TEXTO, true, MEDIA),
					CampoBase.con("cobertura", "Cobertura", TipoDatoCampo.TEXTO, true, BAJA,
							"responsabilidad civil, todo riesgo"),
					CampoBase.de("vigenciaDesde", "Vigencia desde", TipoDatoCampo.FECHA, false, BAJA),
					CampoBase.de("fechaVencimiento", "Vigencia hasta", TipoDatoCampo.FECHA, true, MEDIA)),
			List.of(vencimiento("fechaVencimiento")));

	public static final List<TipoBase> TIPOS = List.of(REMITO, FACTURA, NOTA_CREDITO, NOTA_DEBITO,
			CONSTANCIA_CUIT, DNI, LICENCIA_CONDUCIR, CEDULA_VEHICULAR, VTV, SEGURO_VEHICULAR);
}
