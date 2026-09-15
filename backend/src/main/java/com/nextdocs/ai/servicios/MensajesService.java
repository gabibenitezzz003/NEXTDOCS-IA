package com.nextdocs.ai.servicios;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class MensajesService {

	private static final String IDIOMA_DEFECTO = "es";

	private static final Pattern PARAMETRO = Pattern.compile("\\{\\d+\\}");

	private final Map<String, Map<String, String>> diccionarios = new HashMap<>();

	private final Map<String, List<Plantilla>> plantillas = new HashMap<>();

	public MensajesService(ObjectMapper mapeador) {
		for (String idioma : new String[] { "en", "pt" }) {
			Map<String, String> diccionario;
			try (InputStream entrada = new ClassPathResource("i18n/" + idioma + ".json").getInputStream()) {
				diccionario = mapeador.readValue(entrada, new TypeReference<Map<String, String>>() {
				});
			}
			catch (IOException e) {
				throw new IllegalStateException("No se pudo cargar el diccionario " + idioma, e);
			}
			diccionarios.put(idioma, diccionario);
			plantillas.put(idioma, compilar(diccionario));
		}
	}

	public String resolver(String mensaje, String acceptLanguage) {
		return resolverEnIdioma(mensaje, idiomaDe(acceptLanguage));
	}

	public String resolverEnIdioma(String mensaje, String idioma) {
		if (mensaje == null || mensaje.isBlank() || IDIOMA_DEFECTO.equals(idioma)) {
			return mensaje;
		}
		Map<String, String> diccionario = diccionarios.get(idioma);
		if (diccionario == null) {
			return mensaje;
		}
		String directo = diccionario.get(mensaje);
		if (directo != null) {
			return directo;
		}
		for (Plantilla plantilla : plantillas.get(idioma)) {
			Matcher coincidencia = plantilla.regex.matcher(mensaje);
			if (!coincidencia.matches()) {
				continue;
			}
			String resultado = plantilla.valor;
			for (int i = 0; i < plantilla.orden.size(); i++) {
				resultado = resultado.replace("{" + plantilla.orden.get(i) + "}",
						Matcher.quoteReplacement(coincidencia.group(i + 1)));
			}
			return resultado;
		}
		return mensaje;
	}

	public String idiomaDe(String acceptLanguage) {
		if (acceptLanguage != null) {
			String tag = acceptLanguage.split(",")[0].split(";")[0].trim().toLowerCase(Locale.ROOT);
			if (tag.startsWith("en")) {
				return "en";
			}
			if (tag.startsWith("pt")) {
				return "pt";
			}
		}
		return IDIOMA_DEFECTO;
	}

	private List<Plantilla> compilar(Map<String, String> diccionario) {
		List<Plantilla> lista = new ArrayList<>();
		for (Map.Entry<String, String> entrada : diccionario.entrySet()) {
			String clave = entrada.getKey();
			if (!PARAMETRO.matcher(clave).find()) {
				continue;
			}
			StringBuilder regex = new StringBuilder("^");
			Matcher parametros = PARAMETRO.matcher(clave);
			List<Integer> orden = new ArrayList<>();
			int ultimo = 0;
			while (parametros.find()) {
				regex.append(Pattern.quote(clave.substring(ultimo, parametros.start()))).append("(.+?)");
				String indice = parametros.group();
				orden.add(Integer.parseInt(indice.substring(1, indice.length() - 1)));
				ultimo = parametros.end();
			}
			regex.append(Pattern.quote(clave.substring(ultimo))).append("$");
			lista.add(new Plantilla(Pattern.compile(regex.toString()), orden, entrada.getValue()));
		}
		return lista;
	}

	private record Plantilla(Pattern regex, List<Integer> orden, String valor) {
	}
}
