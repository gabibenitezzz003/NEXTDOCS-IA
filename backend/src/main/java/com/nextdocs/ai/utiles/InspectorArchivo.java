package com.nextdocs.ai.utiles;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.tika.Tika;

public final class InspectorArchivo {

	private static final Tika TIKA = new Tika();

	private static final String MIME_PDF = "application/pdf";

	private InspectorArchivo() {
	}

	public static String detectarTipoMime(byte[] contenido, String nombreArchivo) {
		try {
			return TIKA.detect(new ByteArrayInputStream(contenido), nombreArchivo);
		} catch (IOException e) {
			return null;
		}
	}

	public static int contarPaginas(byte[] contenido, String tipoMime) {
		if (!MIME_PDF.equals(tipoMime)) {
			return 1;
		}
		try (PDDocument documento = Loader.loadPDF(contenido)) {
			return documento.getNumberOfPages();
		} catch (Exception e) {
			return 1;
		}
	}

	public static boolean esPdf(String tipoMime) {
		return MIME_PDF.equals(tipoMime);
	}
}
