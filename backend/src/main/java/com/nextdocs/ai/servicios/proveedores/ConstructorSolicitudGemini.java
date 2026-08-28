package com.nextdocs.ai.servicios.proveedores;

import java.util.Base64;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.CampoEsquemaModel;
import com.nextdocs.ai.modelos.SolicitudExtraccionModel;

import org.springframework.stereotype.Component;

@Component
public class ConstructorSolicitudGemini {

	public static final String CAMPO_TIPO_DETECTADO = "tipoDetectado";

	public static final String CAMPO_CAMPOS = "campos";

	private static final String INSTRUCCION_BASE = """
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

	private final ObjectMapper objectMapper;

	public ConstructorSolicitudGemini(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public String construir(SolicitudExtraccionModel solicitud, double temperatura) {
		if (solicitud.getCampos().isEmpty()) {
			throw new ValidacionException("No se puede extraer sin un esquema de campos definido");
		}
		ObjectNode raiz = objectMapper.createObjectNode();
		ArrayNode contenidos = raiz.putArray("contents");
		ObjectNode contenido = contenidos.addObject();
		ArrayNode partes = contenido.putArray("parts");
		partes.addObject().put("text", construirInstruccion(solicitud));
		ObjectNode adjunto = partes.addObject().putObject("inline_data");
		adjunto.put("mime_type", solicitud.getTipoMime());
		adjunto.put("data", Base64.getEncoder().encodeToString(solicitud.getContenido()));

		ObjectNode configuracion = raiz.putObject("generationConfig");
		configuracion.put("temperature", temperatura);
		configuracion.put("responseMimeType", "application/json");
		configuracion.set("responseSchema", construirEsquema());
		return raiz.toString();
	}

	private String construirInstruccion(SolicitudExtraccionModel solicitud) {
		StringBuilder texto = new StringBuilder(INSTRUCCION_BASE);
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
		return texto.toString();
	}

	private ObjectNode construirEsquema() {
		ObjectNode esquema = objectMapper.createObjectNode();
		esquema.put("type", "OBJECT");
		ObjectNode propiedades = esquema.putObject("properties");
		propiedades.putObject(CAMPO_TIPO_DETECTADO).put("type", "STRING");

		ObjectNode campos = propiedades.putObject(CAMPO_CAMPOS);
		campos.put("type", "ARRAY");
		ObjectNode elemento = campos.putObject("items");
		elemento.put("type", "OBJECT");
		ObjectNode propiedadesElemento = elemento.putObject("properties");
		propiedadesElemento.putObject("clave").put("type", "STRING");
		propiedadesElemento.putObject("valor").put("type", "STRING");
		ObjectNode presencia = propiedadesElemento.putObject("presencia");
		presencia.put("type", "STRING");
		ArrayNode valoresPresencia = presencia.putArray("enum");
		for (String valor : List.of("PRESENTE", "NO_FIGURA", "ILEGIBLE")) {
			valoresPresencia.add(valor);
		}
		propiedadesElemento.putObject("confianza").put("type", "NUMBER");
		propiedadesElemento.putObject("pagina").put("type", "INTEGER");
		ArrayNode requeridos = elemento.putArray("required");
		for (String requerido : List.of("clave", "presencia", "confianza")) {
			requeridos.add(requerido);
		}

		esquema.putArray("required").add(CAMPO_CAMPOS);
		return esquema;
	}
}
