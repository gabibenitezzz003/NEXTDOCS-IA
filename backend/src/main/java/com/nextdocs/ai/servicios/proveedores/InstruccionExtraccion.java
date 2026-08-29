package com.nextdocs.ai.servicios.proveedores;

import com.nextdocs.ai.modelos.CampoEsquemaModel;
import com.nextdocs.ai.modelos.SolicitudExtraccionModel;

public final class InstruccionExtraccion {

	public static final String CAMPO_TIPO_DETECTADO = "tipoDetectado";

	public static final String CAMPO_CAMPOS = "campos";

	private static final String BASE = """
			Sos un extractor de datos documentales. Analiza el documento adjunto y devolve unicamente \
			los campos solicitados, respetando estas reglas sin excepcion:

			1. Devolve una entrada por cada clave solicitada, ni una mas ni una menos.
			2. presencia = PRESENTE si el dato figura y se lee con certeza.
			3. presencia = NO_FIGURA si el dato no aparece en el documento.
			4. presencia = ILEGIBLE si el dato aparece pero no se puede leer con certeza por calidad, \
			recorte, tachadura o superposicion. Nunca uses NO_FIGURA para un dato ilegible.
			5. Si presencia no es PRESENTE, dejá valor vacio.
			6. confianza es tu certeza real sobre ese campo, entre 0 y 1. No la infles.
			7. pagina es el numero de pagina donde encontraste el dato, empezando en 1.
			8. No inventes ni completes datos por contexto. Si no esta en el documento, es NO_FIGURA.

			Campos solicitados:
			""";

	private static final String FORMA_ESPERADA = """

			Devolve un unico objeto JSON con esta forma exacta:
			{"tipoDetectado":"<codigo>","campos":[{"clave":"<clave>","valor":"<texto>",\
			"presencia":"PRESENTE|NO_FIGURA|ILEGIBLE","confianza":<0..1>,"pagina":<entero>}]}
			""";

	private InstruccionExtraccion() {
	}

	public static String construir(SolicitudExtraccionModel solicitud) {
		return construir(solicitud, false);
	}

	public static String construir(SolicitudExtraccionModel solicitud, boolean describirFormaEsperada) {
		StringBuilder texto = new StringBuilder(ContenidoNoConfiable.ADVERTENCIA).append('\n').append(BASE);
		for (CampoEsquemaModel campo : solicitud.getCampos()) {
			texto.append("- ").append(campo.getClave()).append(" (").append(campo.getEtiqueta()).append(")");
			if (campo.getTipoDato() != null) {
				texto.append(" tipo ").append(campo.getTipoDato());
			}
			if (campo.isRequerido()) {
				texto.append(" [obligatorio]");
			}
			if (campo.getDescripcion() != null && !campo.getDescripcion().isBlank()) {
				texto.append(": ").append(campo.getDescripcion());
			}
			if (campo.getAlias() != null && !campo.getAlias().isBlank()) {
				texto.append(". Tambien puede aparecer como: ").append(campo.getAlias());
			}
			if (campo.getFormatoFecha() != null && !campo.getFormatoFecha().isBlank()) {
				texto.append(". Formato de fecha esperado: ").append(campo.getFormatoFecha());
			}
			texto.append('\n');
		}
		if (solicitud.getCodigoPlantilla() != null) {
			texto.append("\nTipo documental esperado: ").append(solicitud.getCodigoPlantilla()).append('\n');
		}
		if (solicitud.getInstruccionExtraccion() != null && !solicitud.getInstruccionExtraccion().isBlank()) {
			texto.append("\nInstrucciones adicionales del tenant:\n")
					.append(solicitud.getInstruccionExtraccion()).append('\n');
		}
		if (describirFormaEsperada) {
			texto.append(FORMA_ESPERADA);
		}
		return texto.toString();
	}
}
