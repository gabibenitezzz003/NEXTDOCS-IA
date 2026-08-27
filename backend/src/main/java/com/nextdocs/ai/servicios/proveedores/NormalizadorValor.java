package com.nextdocs.ai.servicios.proveedores;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import com.nextdocs.ai.enumeraciones.TipoDatoCampo;

public final class NormalizadorValor {

	private static final List<DateTimeFormatter> FORMATOS_FECHA = List.of(
			DateTimeFormatter.ofPattern("yyyy-MM-dd"),
			DateTimeFormatter.ofPattern("dd/MM/yyyy"),
			DateTimeFormatter.ofPattern("dd-MM-yyyy"),
			DateTimeFormatter.ofPattern("dd.MM.yyyy"),
			DateTimeFormatter.ofPattern("yyyy/MM/dd"));

	private static final BigDecimal CONFIANZA_MINIMA = BigDecimal.ZERO;

	private static final BigDecimal CONFIANZA_MAXIMA = BigDecimal.ONE;

	private NormalizadorValor() {
	}

	public static String normalizar(String valor, TipoDatoCampo tipoDato) {
		if (valor == null || valor.isBlank()) {
			return null;
		}
		String limpio = valor.trim().replaceAll("\\s+", " ");
		if (tipoDato == null) {
			return limpio;
		}
		return switch (tipoDato) {
			case NUMERO, DECIMAL, MONEDA -> normalizarNumero(limpio);
			case FECHA, FECHA_HORA -> normalizarFecha(limpio);
			case BOOLEANO -> normalizarBooleano(limpio);
			case CUIT -> limpio.replaceAll("[^0-9]", "");
			case TELEFONO -> limpio.replaceAll("[^0-9+]", "");
			case EMAIL -> limpio.toLowerCase(Locale.ROOT);
			default -> limpio;
		};
	}

	public static BigDecimal calibrarConfianza(BigDecimal confianzaProveedor) {
		if (confianzaProveedor == null) {
			return null;
		}
		BigDecimal acotada = confianzaProveedor.max(CONFIANZA_MINIMA).min(CONFIANZA_MAXIMA);
		return acotada.setScale(4, RoundingMode.HALF_UP);
	}

	private static String normalizarNumero(String valor) {
		String soloNumero = valor.replaceAll("[^0-9,.-]", "");
		if (soloNumero.isBlank()) {
			return null;
		}
		int ultimaComa = soloNumero.lastIndexOf(',');
		int ultimoPunto = soloNumero.lastIndexOf('.');
		String sinSeparadorMiles;
		if (ultimaComa > ultimoPunto) {
			sinSeparadorMiles = soloNumero.replace(".", "").replace(',', '.');
		} else {
			sinSeparadorMiles = soloNumero.replace(",", "");
		}
		try {
			return new BigDecimal(sinSeparadorMiles).stripTrailingZeros().toPlainString();
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String normalizarFecha(String valor) {
		String candidato = valor.split("[ T]")[0];
		for (DateTimeFormatter formato : FORMATOS_FECHA) {
			try {
				return LocalDate.parse(candidato, formato).toString();
			} catch (Exception e) {
				continue;
			}
		}
		return null;
	}

	private static String normalizarBooleano(String valor) {
		String minuscula = valor.toLowerCase(Locale.ROOT);
		if (List.of("si", "sí", "true", "verdadero", "1", "x").contains(minuscula)) {
			return "true";
		}
		if (List.of("no", "false", "falso", "0").contains(minuscula)) {
			return "false";
		}
		return null;
	}
}
