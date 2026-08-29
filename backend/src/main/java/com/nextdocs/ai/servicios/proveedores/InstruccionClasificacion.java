package com.nextdocs.ai.servicios.proveedores;

import com.nextdocs.ai.modelos.ResultadoClasificacionModel;
import com.nextdocs.ai.modelos.SolicitudClasificacionModel;
import com.nextdocs.ai.modelos.TipoCandidatoModel;

public final class InstruccionClasificacion {

	public static final String CAMPO_TIPO = "tipo";

	public static final String CAMPO_CONFIANZA = "confianza";

	public static final String CAMPO_MOTIVO = "motivo";

	public static final String CAMPO_NOMBRE_SUGERIDO = "nombreSugerido";

	public static final String CAMPO_CAMPOS_SUGERIDOS = "camposSugeridos";

	private static final String BASE = """
			Sos un clasificador de documentos comerciales y legales argentinos. Mira el documento \
			adjunto y deci a cual de los tipos conocidos corresponde, con estas reglas:

			1. Elegi un unico codigo de la lista de tipos conocidos.
			2. Si no corresponde con claridad a ninguno, devolve el codigo DESCONOCIDO. Preferi \
			DESCONOCIDO antes que forzar un tipo que no es: un tipo mal elegido hace que se extraigan \
			los campos equivocados.
			3. confianza es tu certeza real entre 0 y 1. No la infles. Un documento borroso, cortado o \
			ambiguo tiene confianza baja aunque adivines bien el tipo.
			4. motivo explica en una linea que viste para decidir eso.
			5. Si devolves DESCONOCIDO, proponé ademas un nombre para el tipo y la lista de campos que \
			convendria extraer de un documento asi, con su clave en camelCase y su tipo de dato entre \
			TEXTO, NUMERO, FECHA, BOOLEANO, CUIT, CUIL, IMPORTE, PORCENTAJE o LISTA.

			Tipos conocidos:
			""";

	private static final String FORMA_ESPERADA = """

			Devolve un unico objeto JSON con esta forma exacta:
			{"tipo":"<codigo o DESCONOCIDO>","confianza":<0..1>,"motivo":"<una linea>",\
			"nombreSugerido":"<solo si tipo es DESCONOCIDO>",\
			"camposSugeridos":[{"clave":"<camelCase>","etiqueta":"<texto>","tipoDato":"TEXTO",\
			"requerido":true,"ejemplo":"<lo que leiste>"}]}
			""";

	private InstruccionClasificacion() {
	}

	public static String construir(SolicitudClasificacionModel solicitud) {
		return construir(solicitud, false);
	}

	public static String construir(SolicitudClasificacionModel solicitud, boolean describirFormaEsperada) {
		StringBuilder texto = new StringBuilder(ContenidoNoConfiable.ADVERTENCIA).append('\n').append(BASE);
		for (TipoCandidatoModel candidato : solicitud.getCandidatos()) {
			texto.append("- ").append(candidato.getCodigo());
			if (candidato.getNombre() != null && !candidato.getNombre().isBlank()) {
				texto.append(" (").append(candidato.getNombre()).append(')');
			}
			if (candidato.getDescripcion() != null && !candidato.getDescripcion().isBlank()) {
				texto.append(": ").append(candidato.getDescripcion());
			}
			if (!candidato.getCamposClave().isEmpty()) {
				texto.append(". Suele traer: ").append(String.join(", ", candidato.getCamposClave()));
			}
			texto.append('\n');
		}
		texto.append("- ").append(ResultadoClasificacionModel.CODIGO_DESCONOCIDO)
				.append(": no encaja con ninguno de los anteriores\n");
		if (solicitud.getNombreArchivo() != null && !solicitud.getNombreArchivo().isBlank()) {
			texto.append("\nNombre del archivo, que es una pista debil y puede mentir: ")
					.append(solicitud.getNombreArchivo()).append('\n');
		}
		if (describirFormaEsperada) {
			texto.append(FORMA_ESPERADA);
		}
		return texto.toString();
	}
}
