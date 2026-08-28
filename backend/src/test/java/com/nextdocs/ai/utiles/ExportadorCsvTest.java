package com.nextdocs.ai.utiles;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExportadorCsvTest {

	@Test
	@DisplayName("el encabezado se escribe en la primera fila y cada fila termina en salto")
	void encabezadoYFilas() {
		ExportadorCsv exportador = new ExportadorCsv(List.of("fecha", "accion"));
		exportador.fila(List.of("2026-08-28T00:00:00Z", "DOCUMENTO_INGRESADO"));

		assertThat(exportador.contenido())
				.isEqualTo("fecha;accion\r\n2026-08-28T00:00:00Z;DOCUMENTO_INGRESADO\r\n");
	}

	@Test
	@DisplayName("un valor con el separador va entre comillas")
	void valorConSeparador() {
		assertThat(ExportadorCsv.celda("uno;dos")).isEqualTo("\"uno;dos\"");
	}

	@Test
	@DisplayName("las comillas internas se duplican")
	void comillasInternas() {
		assertThat(ExportadorCsv.celda("dijo \"hola\"")).isEqualTo("\"dijo \"\"hola\"\"\"");
	}

	@Test
	@DisplayName("un valor con salto de linea va entre comillas")
	void valorConSalto() {
		assertThat(ExportadorCsv.celda("primera\nsegunda")).isEqualTo("\"primera\nsegunda\"");
	}

	@Test
	@DisplayName("un valor nulo se exporta como celda vacia")
	void valorNulo() {
		assertThat(ExportadorCsv.celda(null)).isEmpty();
	}

	@Test
	@DisplayName("un valor que arranca como formula se neutraliza para que la planilla no lo ejecute")
	void formulaNeutralizada() {
		for (String peligroso : Arrays.asList("=1+1", "+1", "-1", "@SUM(A1)")) {
			assertThat(ExportadorCsv.celda(peligroso)).startsWith("'");
		}
		assertThat(ExportadorCsv.celda("=CMD|'/C calc'!A0")).isEqualTo("'=CMD|'/C calc'!A0");
	}

	@Test
	@DisplayName("un valor comun no se altera")
	void valorComun() {
		assertThat(ExportadorCsv.celda("DOCUMENTO_APROBADO")).isEqualTo("DOCUMENTO_APROBADO");
	}

	@Test
	@DisplayName("una fila escapa cada valor una sola vez")
	void filaEscapaUnaSolaVez() {
		ExportadorCsv exportador = new ExportadorCsv(List.of("detalle"));
		exportador.fila(List.of("{\"clase\":\"REMITOS\"}"));

		assertThat(exportador.contenido()).isEqualTo("detalle\r\n\"{\"\"clase\"\":\"\"REMITOS\"\"}\"\r\n");
	}

	@Test
	@DisplayName("una fila con valores nulos y no textuales no rompe la exportacion")
	void filaConNulosYObjetos() {
		ExportadorCsv exportador = new ExportadorCsv(List.of("id", "exitoso", "fecha"));
		exportador.fila(Arrays.asList(null, Boolean.TRUE, java.time.Instant.EPOCH));

		assertThat(exportador.contenido()).endsWith(";true;1970-01-01T00:00:00Z\r\n");
	}
}
