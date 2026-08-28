package com.nextdocs.ai.servicios.proveedores;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nextdocs.ai.clientes.DeepSeekCliente;
import com.nextdocs.ai.entidades.ConfiguracionProveedor;
import com.nextdocs.ai.enumeraciones.PresenciaCampo;
import com.nextdocs.ai.enumeraciones.ProveedorDocumentalIa;
import com.nextdocs.ai.exceptions.ProveedorNoDisponibleException;
import com.nextdocs.ai.interfaces.ProveedorDocumentalIaInt;
import com.nextdocs.ai.modelos.CampoEsquemaModel;
import com.nextdocs.ai.modelos.ResultadoExtraccionModel;
import com.nextdocs.ai.modelos.SolicitudExtraccionModel;
import com.nextdocs.ai.modelos.ValorCanonicoModel;
import com.nextdocs.ai.repositorios.ConfiguracionProveedorRepository;
import com.nextdocs.ai.utiles.ExtractorTextoPdf;
import com.nextdocs.ai.utiles.ResolvedorSecreto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProveedorDeepSeekService implements ProveedorDocumentalIaInt {

	public static final String MODELO_POR_DEFECTO = "deepseek-chat";

	public static final String VARIABLE_CLAVE_GLOBAL = "NEXTDOCS_DEEPSEEK_CLAVE";

	public static final String VARIABLE_URL_GLOBAL = "NEXTDOCS_DEEPSEEK_URL";

	private static final Logger log = LoggerFactory.getLogger(ProveedorDeepSeekService.class);

	private static final BigDecimal UN_MILLON = new BigDecimal("1000000");

	private static final int CARACTERES_MAXIMOS = 120000;

	private final DeepSeekCliente deepSeekCliente;

	private final ConfiguracionProveedorRepository configuracionProveedorRepository;

	private final ObjectMapper objectMapper;

	public ProveedorDeepSeekService(DeepSeekCliente deepSeekCliente,
			ConfiguracionProveedorRepository configuracionProveedorRepository, ObjectMapper objectMapper) {
		this.deepSeekCliente = deepSeekCliente;
		this.configuracionProveedorRepository = configuracionProveedorRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	public ProveedorDocumentalIa tipo() {
		return ProveedorDocumentalIa.DEEPSEEK;
	}

	@Override
	public boolean estaDisponible() {
		return ResolvedorSecreto.resolver(VARIABLE_CLAVE_GLOBAL).isPresent();
	}

	@Override
	public ResultadoExtraccionModel extraer(SolicitudExtraccionModel solicitud) {
		long inicio = System.currentTimeMillis();
		Optional<ConfiguracionProveedor> configuracion = configuracionProveedorRepository
				.buscarPorProveedor(solicitud.getTenantId(), ProveedorDocumentalIa.DEEPSEEK);
		String modelo = resolverModelo(solicitud, configuracion);
		String claveApi = resolverClave(configuracion);
		JsonNode parametros = leerParametros(configuracion);
		String texto = extraerTexto(solicitud);

		JsonNode respuesta = deepSeekCliente.completar(urlBase(parametros), claveApi,
				construirCuerpo(solicitud, modelo, texto, parametros));

		ResultadoExtraccionModel resultado = new ResultadoExtraccionModel();
		resultado.setProveedor(tipo());
		resultado.setModelo(modelo);
		resultado.setVersionPrompt(solicitud.getVersionPrompt());
		resultado.setVersionEsquema(solicitud.getVersionEsquema());
		resultado.setPaginasProcesadas(Math.max(solicitud.getPaginas(), 1));
		resultado.setMonedaCosto("USD");
		completarUso(resultado, respuesta, parametros);
		completarValores(resultado, respuesta, solicitud, parametros);
		resultado.setDuracionMilisegundos(System.currentTimeMillis() - inicio);
		return resultado;
	}

	private String extraerTexto(SolicitudExtraccionModel solicitud) {
		String texto = ExtractorTextoPdf.extraer(solicitud.getContenido(), solicitud.getTipoMime());
		if (texto == null || texto.isBlank()) {
			throw new ProveedorNoDisponibleException(
					"DeepSeek opera sobre la capa de texto del documento y este no tiene texto extraible. "
							+ "Un documento escaneado necesita un proveedor con vision",
					false);
		}
		return texto.length() > CARACTERES_MAXIMOS ? texto.substring(0, CARACTERES_MAXIMOS) : texto;
	}

	private String construirCuerpo(SolicitudExtraccionModel solicitud, String modelo, String texto,
			JsonNode parametros) {
		ObjectNode raiz = objectMapper.createObjectNode();
		raiz.put("model", modelo);
		raiz.put("temperature", parametros.hasNonNull("temperatura") ? parametros.get("temperatura").asDouble() : 0.0);
		raiz.putObject("response_format").put("type", "json_object");

		ArrayNode mensajes = raiz.putArray("messages");
		ObjectNode sistema = mensajes.addObject();
		sistema.put("role", "system");
		sistema.put("content", InstruccionExtraccion.construir(solicitud, true));
		ObjectNode usuario = mensajes.addObject();
		usuario.put("role", "user");
		usuario.put("content", "Contenido del documento:\n\n" + texto);
		return raiz.toString();
	}

	private void completarUso(ResultadoExtraccionModel resultado, JsonNode respuesta, JsonNode parametros) {
		JsonNode uso = respuesta.path("usage");
		resultado.setTokensEntrada(uso.path("prompt_tokens").asLong(0));
		resultado.setTokensSalida(uso.path("completion_tokens").asLong(0));
		resultado.setCosto(calcularCosto(resultado.getTokensEntrada(), resultado.getTokensSalida(), parametros));
	}

	private BigDecimal calcularCosto(long tokensEntrada, long tokensSalida, JsonNode parametros) {
		if (!parametros.hasNonNull("costoPorMillonEntrada") || !parametros.hasNonNull("costoPorMillonSalida")) {
			return BigDecimal.ZERO;
		}
		BigDecimal entrada = new BigDecimal(parametros.get("costoPorMillonEntrada").asText())
				.multiply(BigDecimal.valueOf(tokensEntrada)).divide(UN_MILLON, 8, RoundingMode.HALF_UP);
		BigDecimal salida = new BigDecimal(parametros.get("costoPorMillonSalida").asText())
				.multiply(BigDecimal.valueOf(tokensSalida)).divide(UN_MILLON, 8, RoundingMode.HALF_UP);
		return entrada.add(salida).setScale(6, RoundingMode.HALF_UP);
	}

	private void completarValores(ResultadoExtraccionModel resultado, JsonNode respuesta,
			SolicitudExtraccionModel solicitud, JsonNode parametros) {
		JsonNode contenido = extraerContenido(respuesta);
		resultado.setTipoDetectado(contenido.path(InstruccionExtraccion.CAMPO_TIPO_DETECTADO)
				.asText(solicitud.getCodigoPlantilla()));

		Map<String, CampoEsquemaModel> esperados = new HashMap<>();
		for (CampoEsquemaModel campo : solicitud.getCampos()) {
			esperados.put(campo.getClave(), campo);
		}

		Set<String> devueltos = new LinkedHashSet<>();
		BigDecimal factor = factorCalibracion(parametros);
		for (JsonNode nodo : contenido.path(InstruccionExtraccion.CAMPO_CAMPOS)) {
			String clave = nodo.path("clave").asText(null);
			CampoEsquemaModel esperado = esperados.get(clave);
			if (esperado == null) {
				resultado.getAdvertencias().add("El proveedor devolvio el campo no solicitado " + clave);
				continue;
			}
			devueltos.add(clave);
			resultado.getValores().add(construirValor(nodo, esperado, factor));
		}
		for (Map.Entry<String, CampoEsquemaModel> pendiente : esperados.entrySet()) {
			if (devueltos.contains(pendiente.getKey())) {
				continue;
			}
			resultado.getAdvertencias().add("El proveedor no devolvio el campo " + pendiente.getKey());
			resultado.getValores().add(valorAusente(pendiente.getValue()));
		}
	}

	private JsonNode extraerContenido(JsonNode respuesta) {
		JsonNode opciones = respuesta.path("choices");
		if (!opciones.isArray() || opciones.isEmpty()) {
			throw new ProveedorNoDisponibleException("DeepSeek no devolvio ninguna opcion", true);
		}
		String texto = opciones.get(0).path("message").path("content").asText(null);
		if (texto == null || texto.isBlank()) {
			throw new ProveedorNoDisponibleException("DeepSeek devolvio una respuesta vacia", true);
		}
		try {
			return objectMapper.readTree(texto);
		} catch (Exception e) {
			throw new ProveedorNoDisponibleException("DeepSeek devolvio un JSON que no se pudo interpretar", true, e);
		}
	}

	private ValorCanonicoModel construirValor(JsonNode nodo, CampoEsquemaModel esperado, BigDecimal factor) {
		PresenciaCampo presencia = PresenciaCampo.desde(nodo.path("presencia").asText());
		ValorCanonicoModel valor = new ValorCanonicoModel();
		valor.setClaveCampo(esperado.getClave());
		valor.setPresencia(presencia == null ? PresenciaCampo.ILEGIBLE : presencia);
		valor.setEvidenciaPagina(Math.max(nodo.path("pagina").asInt(1), 1));

		BigDecimal confianzaProveedor = NormalizadorValor
				.calibrarConfianza(BigDecimal.valueOf(nodo.path("confianza").asDouble(0)));
		valor.setConfianzaProveedor(confianzaProveedor);
		valor.setConfianza(NormalizadorValor
				.calibrarConfianza(confianzaProveedor == null ? null : confianzaProveedor.multiply(factor)));

		if (valor.getPresencia() == PresenciaCampo.PRESENTE) {
			String crudo = nodo.path("valor").asText(null);
			valor.setValorCrudo(crudo);
			valor.setValorNormalizado(NormalizadorValor.normalizar(crudo, esperado.getTipoDato()));
			if (valor.getValorNormalizado() == null && crudo != null) {
				valor.setPresencia(PresenciaCampo.ILEGIBLE);
			}
		}
		return valor;
	}

	private ValorCanonicoModel valorAusente(CampoEsquemaModel esperado) {
		ValorCanonicoModel valor = new ValorCanonicoModel();
		valor.setClaveCampo(esperado.getClave());
		valor.setPresencia(PresenciaCampo.ILEGIBLE);
		valor.setConfianza(BigDecimal.ZERO);
		valor.setConfianzaProveedor(BigDecimal.ZERO);
		return valor;
	}

	private String resolverModelo(SolicitudExtraccionModel solicitud, Optional<ConfiguracionProveedor> configuracion) {
		if (solicitud.getModelo() != null && !solicitud.getModelo().isBlank()) {
			return solicitud.getModelo();
		}
		return configuracion.map(ConfiguracionProveedor::getModelo).filter(valor -> !valor.isBlank())
				.orElse(MODELO_POR_DEFECTO);
	}

	private String resolverClave(Optional<ConfiguracionProveedor> configuracion) {
		Optional<String> desdeConfiguracion = configuracion.map(ConfiguracionProveedor::getReferenciaSecreto)
				.flatMap(ResolvedorSecreto::resolver);
		if (desdeConfiguracion.isPresent()) {
			return desdeConfiguracion.get();
		}
		return ResolvedorSecreto.resolver(VARIABLE_CLAVE_GLOBAL).orElseThrow(
				() -> new ProveedorNoDisponibleException("No hay credencial configurada para DeepSeek", false));
	}

	private String urlBase(JsonNode parametros) {
		if (parametros.hasNonNull("urlBase")) {
			return parametros.get("urlBase").asText();
		}
		return ResolvedorSecreto.resolver(VARIABLE_URL_GLOBAL).orElse(null);
	}

	private JsonNode leerParametros(Optional<ConfiguracionProveedor> configuracion) {
		return configuracion.map(ConfiguracionProveedor::getParametros).filter(valor -> !valor.isBlank())
				.map(this::leerJson).orElseGet(objectMapper::createObjectNode);
	}

	private JsonNode leerJson(String texto) {
		try {
			return objectMapper.readTree(texto);
		} catch (Exception e) {
			log.warn("Los parametros de la configuracion de DeepSeek no son un JSON valido, se ignoran");
			return objectMapper.createObjectNode();
		}
	}

	private BigDecimal factorCalibracion(JsonNode parametros) {
		if (!parametros.hasNonNull("factorCalibracionConfianza")) {
			return BigDecimal.ONE;
		}
		return new BigDecimal(parametros.get("factorCalibracionConfianza").asText());
	}
}
