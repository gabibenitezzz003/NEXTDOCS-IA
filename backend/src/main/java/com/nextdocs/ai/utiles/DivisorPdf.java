package com.nextdocs.ai.utiles;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.nextdocs.ai.exceptions.ValidacionException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public final class DivisorPdf {

	private DivisorPdf() {
	}

	public record Tramo(int paginaDesde, int paginaHasta, String motivo) {

		public int cantidadPaginas() {
			return paginaHasta - paginaDesde + 1;
		}
	}

	public static List<Tramo> tramosPorPaginasFijas(int totalPaginas, int paginasPorDocumento) {
		if (paginasPorDocumento < 1) {
			throw new ValidacionException("La cantidad de paginas por documento debe ser mayor a cero");
		}
		List<Tramo> tramos = new ArrayList<>();
		for (int inicio = 1; inicio <= totalPaginas; inicio += paginasPorDocumento) {
			int fin = Math.min(inicio + paginasPorDocumento - 1, totalPaginas);
			tramos.add(new Tramo(inicio, fin, "Corte cada " + paginasPorDocumento + " pagina(s)"));
		}
		return tramos;
	}

	public static List<Tramo> tramosPorPatron(byte[] contenido, String patron) {
		Pattern compilado = compilar(patron);
		List<Integer> inicios = new ArrayList<>();
		int totalPaginas;
		try (PDDocument documento = Loader.loadPDF(contenido)) {
			totalPaginas = documento.getNumberOfPages();
			for (int pagina = 1; pagina <= totalPaginas; pagina++) {
				if (compilado.matcher(textoDePagina(documento, pagina)).find()) {
					inicios.add(pagina);
				}
			}
		} catch (Exception e) {
			throw new ValidacionException("No se pudo leer el PDF para segmentarlo: " + e.getMessage());
		}
		if (inicios.isEmpty() || inicios.get(0) != 1) {
			inicios.add(0, 1);
		}
		List<Tramo> tramos = new ArrayList<>();
		for (int i = 0; i < inicios.size(); i++) {
			int desde = inicios.get(i);
			int hasta = i + 1 < inicios.size() ? inicios.get(i + 1) - 1 : totalPaginas;
			if (hasta >= desde) {
				tramos.add(new Tramo(desde, hasta, "Coincidencia del patron de inicio en la pagina " + desde));
			}
		}
		return tramos;
	}

	public static byte[] extraer(byte[] contenido, int paginaDesde, int paginaHasta) {
		try (PDDocument origen = Loader.loadPDF(contenido); PDDocument destino = new PDDocument()) {
			int total = origen.getNumberOfPages();
			if (paginaDesde < 1 || paginaHasta > total || paginaDesde > paginaHasta) {
				throw new ValidacionException(
						"El rango de paginas " + paginaDesde + "-" + paginaHasta + " no es valido");
			}
			for (int pagina = paginaDesde; pagina <= paginaHasta; pagina++) {
				destino.importPage(origen.getPage(pagina - 1));
			}
			ByteArrayOutputStream salida = new ByteArrayOutputStream();
			destino.save(salida);
			return salida.toByteArray();
		} catch (ValidacionException e) {
			throw e;
		} catch (Exception e) {
			throw new ValidacionException("No se pudo extraer el rango de paginas: " + e.getMessage());
		}
	}

	public static String textoDePagina(PDDocument documento, int pagina) throws Exception {
		PDFTextStripper extractor = new PDFTextStripper();
		extractor.setStartPage(pagina);
		extractor.setEndPage(pagina);
		return extractor.getText(documento);
	}

	public static Pattern compilar(String patron) {
		if (patron == null || patron.isBlank()) {
			throw new ValidacionException("El patron de inicio de documento no puede estar vacio");
		}
		try {
			return Pattern.compile(patron, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
		} catch (Exception e) {
			throw new ValidacionException("El patron de inicio de documento no es una expresion regular valida");
		}
	}
}
