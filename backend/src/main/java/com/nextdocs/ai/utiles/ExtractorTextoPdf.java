package com.nextdocs.ai.utiles;

import java.nio.charset.StandardCharsets;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExtractorTextoPdf {

	private static final Logger log = LoggerFactory.getLogger(ExtractorTextoPdf.class);

	private static final String MIME_PDF = "application/pdf";

	private static final String SEPARADOR_PAGINA = "\n\n--- pagina %d ---\n";

	private ExtractorTextoPdf() {
	}

	public static String extraer(byte[] contenido, String tipoMime) {
		if (contenido == null || contenido.length == 0) {
			return null;
		}
		if (!MIME_PDF.equals(tipoMime)) {
			return new String(contenido, StandardCharsets.UTF_8);
		}
		try (PDDocument documento = Loader.loadPDF(contenido)) {
			StringBuilder texto = new StringBuilder();
			for (int pagina = 1; pagina <= documento.getNumberOfPages(); pagina++) {
				PDFTextStripper extractor = new PDFTextStripper();
				extractor.setStartPage(pagina);
				extractor.setEndPage(pagina);
				String contenidoPagina = extractor.getText(documento);
				if (contenidoPagina == null || contenidoPagina.isBlank()) {
					continue;
				}
				texto.append(String.format(SEPARADOR_PAGINA, pagina)).append(contenidoPagina);
			}
			return texto.toString().trim();
		} catch (Exception e) {
			log.warn("No se pudo extraer la capa de texto del PDF: {}", e.getMessage());
			return null;
		}
	}

	public static boolean tieneCapaDeTexto(byte[] contenido, String tipoMime) {
		String texto = extraer(contenido, tipoMime);
		return texto != null && !texto.isBlank();
	}
}
