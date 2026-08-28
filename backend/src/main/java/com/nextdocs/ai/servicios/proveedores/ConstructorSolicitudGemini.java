package com.nextdocs.ai.servicios.proveedores;

import java.util.Base64;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.SolicitudExtraccionModel;

import org.springframework.stereotype.Component;

@Component
public class ConstructorSolicitudGemini {

	public static final String CAMPO_TIPO_DETECTADO = "tipoDetectado";

	public static final String CAMPO_CAMPOS = "campos";

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
		partes.addObject().put("text", InstruccionExtraccion.construir(solicitud));
		ObjectNode adjunto = partes.addObject().putObject("inline_data");
		adjunto.put("mime_type", solicitud.getTipoMime());
		adjunto.put("data", Base64.getEncoder().encodeToString(solicitud.getContenido()));

		ObjectNode configuracion = raiz.putObject("generationConfig");
		configuracion.put("temperature", temperatura);
		configuracion.put("responseMimeType", "application/json");
		configuracion.set("responseSchema", construirEsquema());
		return raiz.toString();
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
