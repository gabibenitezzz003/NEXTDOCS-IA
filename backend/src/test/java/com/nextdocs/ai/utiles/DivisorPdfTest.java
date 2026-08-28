package com.nextdocs.ai.utiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.util.List;

import com.nextdocs.ai.exceptions.ValidacionException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DivisorPdfTest {

	@Test
	@DisplayName("QA1-02: un PDF de 10 remitos de una pagina da 10 tramos")
	void diezRemitosDeUnaPagina() {
		byte[] pdf = construirPdf(List.of("REMITO R-0001", "REMITO R-0002", "REMITO R-0003", "REMITO R-0004",
				"REMITO R-0005", "REMITO R-0006", "REMITO R-0007", "REMITO R-0008", "REMITO R-0009",
				"REMITO R-0010"));

		List<DivisorPdf.Tramo> tramos = DivisorPdf.tramosPorPatron(pdf, "^\\s*REMITO");

		assertThat(tramos).hasSize(10);
		assertThat(tramos.get(0).paginaDesde()).isEqualTo(1);
		assertThat(tramos.get(0).paginaHasta()).isEqualTo(1);
		assertThat(tramos.get(9).paginaDesde()).isEqualTo(10);
		assertThat(tramos.get(9).paginaHasta()).isEqualTo(10);
		assertThat(tramos).allMatch(t -> t.cantidadPaginas() == 1);
	}

	@Test
	@DisplayName("un remito de dos paginas se corta donde arranca el siguiente, no cada pagina")
	void remitosDeDosPaginas() {
		byte[] pdf = construirPdf(List.of("REMITO R-0001", "continuacion del remito 1", "REMITO R-0002",
				"continuacion del remito 2"));

		List<DivisorPdf.Tramo> tramos = DivisorPdf.tramosPorPatron(pdf, "^\\s*REMITO");

		assertThat(tramos).hasSize(2);
		assertThat(tramos.get(0).paginaDesde()).isEqualTo(1);
		assertThat(tramos.get(0).paginaHasta()).isEqualTo(2);
		assertThat(tramos.get(1).paginaDesde()).isEqualTo(3);
		assertThat(tramos.get(1).paginaHasta()).isEqualTo(4);
	}

	@Test
	@DisplayName("si el patron no aparece nunca, devuelve un unico tramo con todo el documento")
	void patronQueNoAparece() {
		byte[] pdf = construirPdf(List.of("factura A", "factura B", "factura C"));

		List<DivisorPdf.Tramo> tramos = DivisorPdf.tramosPorPatron(pdf, "^\\s*REMITO");

		assertThat(tramos).hasSize(1);
		assertThat(tramos.get(0).paginaDesde()).isEqualTo(1);
		assertThat(tramos.get(0).paginaHasta()).isEqualTo(3);
	}

	@Test
	@DisplayName("si el patron no arranca en la pagina 1, la primera pagina igual entra en un tramo")
	void patronQueNoArrancaEnLaPrimera() {
		byte[] pdf = construirPdf(List.of("caratula", "REMITO R-0001", "REMITO R-0002"));

		List<DivisorPdf.Tramo> tramos = DivisorPdf.tramosPorPatron(pdf, "^\\s*REMITO");

		assertThat(tramos).hasSize(3);
		assertThat(tramos.get(0).paginaDesde()).isEqualTo(1);
		assertThat(tramos.get(0).paginaHasta()).isEqualTo(1);
	}

	@Test
	@DisplayName("la estrategia de paginas fijas corta parejo y el ultimo tramo puede ser mas corto")
	void paginasFijas() {
		List<DivisorPdf.Tramo> tramos = DivisorPdf.tramosPorPaginasFijas(7, 2);

		assertThat(tramos).hasSize(4);
		assertThat(tramos.get(0).paginaDesde()).isEqualTo(1);
		assertThat(tramos.get(0).paginaHasta()).isEqualTo(2);
		assertThat(tramos.get(3).paginaDesde()).isEqualTo(7);
		assertThat(tramos.get(3).paginaHasta()).isEqualTo(7);
		assertThat(tramos.get(3).cantidadPaginas()).isEqualTo(1);
	}

	@Test
	@DisplayName("paginas fijas con cero paginas por documento es invalido")
	void paginasFijasInvalida() {
		assertThatThrownBy(() -> DivisorPdf.tramosPorPaginasFijas(10, 0))
				.isInstanceOf(ValidacionException.class)
				.hasMessageContaining("mayor a cero");
	}

	@Test
	@DisplayName("extraer un rango produce un PDF valido con la cantidad de paginas correcta")
	void extraerRango() throws Exception {
		byte[] pdf = construirPdf(List.of("uno", "dos", "tres", "cuatro", "cinco"));

		byte[] recorte = DivisorPdf.extraer(pdf, 2, 4);

		try (PDDocument documento = Loader.loadPDF(recorte)) {
			assertThat(documento.getNumberOfPages()).isEqualTo(3);
			assertThat(DivisorPdf.textoDePagina(documento, 1)).contains("dos");
			assertThat(DivisorPdf.textoDePagina(documento, 3)).contains("cuatro");
		}
	}

	@Test
	@DisplayName("un rango fuera de los limites del documento se rechaza")
	void rangoInvalido() {
		byte[] pdf = construirPdf(List.of("uno", "dos"));

		assertThatThrownBy(() -> DivisorPdf.extraer(pdf, 1, 5))
				.isInstanceOf(ValidacionException.class)
				.hasMessageContaining("no es valido");
	}

	@Test
	@DisplayName("un patron que no compila se rechaza con mensaje claro")
	void patronInvalido() {
		assertThatThrownBy(() -> DivisorPdf.compilar("[sin cerrar"))
				.isInstanceOf(ValidacionException.class)
				.hasMessageContaining("no es una expresion regular valida");
	}

	private byte[] construirPdf(List<String> textosPorPagina) {
		try (PDDocument documento = new PDDocument()) {
			for (String texto : textosPorPagina) {
				PDPage pagina = new PDPage(PDRectangle.A4);
				documento.addPage(pagina);
				try (PDPageContentStream contenido = new PDPageContentStream(documento, pagina)) {
					contenido.beginText();
					contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 14);
					contenido.newLineAtOffset(50, 780);
					contenido.showText(texto);
					contenido.endText();
				}
			}
			ByteArrayOutputStream salida = new ByteArrayOutputStream();
			documento.save(salida);
			return salida.toByteArray();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
