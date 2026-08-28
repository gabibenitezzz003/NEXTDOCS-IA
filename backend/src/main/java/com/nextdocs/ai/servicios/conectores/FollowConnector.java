package com.nextdocs.ai.servicios.conectores;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdocs.ai.clientes.FollowCliente;
import com.nextdocs.ai.entidades.ConfiguracionConector;
import com.nextdocs.ai.exceptions.ConectorNoDisponibleException;
import com.nextdocs.ai.interfaces.ConectorAsociacionInt;
import com.nextdocs.ai.modelos.CandidatoAsociacionModel;
import com.nextdocs.ai.modelos.ContextoAsociacionModel;
import com.nextdocs.ai.repositorios.ConfiguracionConectorRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FollowConnector implements ConectorAsociacionInt {

	public static final String ORIGEN = "FOLLOW";

	public static final String TIPO_PEDIDO = "PEDIDO";

	private static final Logger log = LoggerFactory.getLogger(FollowConnector.class);

	private static final String RUTA_POR_DEFECTO = "/api/pedido/listado/estado";

	private static final String PARAMETRO_POR_DEFECTO = "search";

	private static final List<String> CAMPOS_BUSQUEDA_POR_DEFECTO = List.of("numeroPedido", "ordenCompra",
			"numeroRemito");

	private static final BigDecimal PUNTAJE_EXACTO = new BigDecimal("1.0000");

	private static final BigDecimal PUNTAJE_PARCIAL = new BigDecimal("0.7000");

	private static final BigDecimal PENALIDAD_MULTIPLES = new BigDecimal("0.9000");

	private final FollowCliente followCliente;

	private final ConfiguracionConectorRepository configuracionConectorRepository;

	private final ObjectMapper objectMapper;

	public FollowConnector(FollowCliente followCliente,
			ConfiguracionConectorRepository configuracionConectorRepository, ObjectMapper objectMapper) {
		this.followCliente = followCliente;
		this.configuracionConectorRepository = configuracionConectorRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	public String codigo() {
		return ORIGEN;
	}

	@Override
	public boolean estaHabilitado(String tenantId) {
		return configuracionConectorRepository.buscarPorCodigo(tenantId, ORIGEN)
				.filter(ConfiguracionConector::isActivo).isPresent();
	}

	@Override
	public List<CandidatoAsociacionModel> buscarCandidatos(ContextoAsociacionModel contexto) {
		ConfiguracionConector configuracion = configuracionConectorRepository
				.buscarPorCodigo(contexto.getTenantId(), ORIGEN)
				.orElseThrow(() -> new ConectorNoDisponibleException(ORIGEN,
						"El tenant no tiene configurado el conector Follow", false));
		JsonNode parametros = leerParametros(configuracion);
		Map<String, String> terminos = resolverTerminos(contexto, parametros);
		if (terminos.isEmpty()) {
			log.debug("El documento {} no tiene ningun campo de busqueda para Follow", contexto.getDocumentoId());
			return List.of();
		}

		Map<String, CandidatoAsociacionModel> porObjeto = new LinkedHashMap<>();
		for (Map.Entry<String, String> termino : terminos.entrySet()) {
			JsonNode respuesta = followCliente.buscar(configuracion, ruta(parametros), parametroBusqueda(parametros),
					termino.getValue());
			acumular(porObjeto, respuesta, termino.getKey(), termino.getValue());
		}
		return new ArrayList<>(porObjeto.values());
	}

	private void acumular(Map<String, CandidatoAsociacionModel> porObjeto, JsonNode respuesta, String campo,
			String termino) {
		for (JsonNode pedido : extraerContenido(respuesta)) {
			String id = texto(pedido, "id");
			if (id == null) {
				continue;
			}
			String numeroPedido = texto(pedido, "nroPedido");
			BigDecimal puntaje = puntuar(numeroPedido, termino);
			CandidatoAsociacionModel existente = porObjeto.get(id);
			if (existente == null) {
				porObjeto.put(id, construir(id, numeroPedido, pedido, puntaje, campo, termino));
				continue;
			}
			if (puntaje.compareTo(existente.getPuntaje()) > 0) {
				existente.setPuntaje(puntaje);
			}
			existente.setRazones(existente.getRazones() + "; " + campo + "=" + termino);
		}
	}

	private CandidatoAsociacionModel construir(String id, String numeroPedido, JsonNode pedido, BigDecimal puntaje,
			String campo, String termino) {
		CandidatoAsociacionModel candidato = new CandidatoAsociacionModel();
		candidato.setConector(ORIGEN);
		candidato.setOrigen(ORIGEN);
		candidato.setTipoObjeto(TIPO_PEDIDO);
		candidato.setIdObjeto(id);
		candidato.setPuntaje(puntaje);
		candidato.setRazones(campo + "=" + termino);
		candidato.setDescripcion(descripcion(numeroPedido, pedido));
		return candidato;
	}

	private String descripcion(String numeroPedido, JsonNode pedido) {
		StringBuilder texto = new StringBuilder();
		texto.append(numeroPedido == null ? "sin numero" : numeroPedido);
		String cliente = texto(pedido, "nombreCliente");
		if (cliente != null) {
			texto.append(" - ").append(cliente);
		}
		String estado = texto(pedido, "estadoPedidoNombre");
		if (estado != null) {
			texto.append(" (").append(estado).append(')');
		}
		return texto.toString();
	}

	private BigDecimal puntuar(String valorObjeto, String termino) {
		if (valorObjeto == null) {
			return PUNTAJE_PARCIAL;
		}
		String izquierda = normalizar(valorObjeto);
		String derecha = normalizar(termino);
		if (izquierda.equals(derecha)) {
			return PUNTAJE_EXACTO;
		}
		if (izquierda.contains(derecha) || derecha.contains(izquierda)) {
			return PUNTAJE_PARCIAL.multiply(PENALIDAD_MULTIPLES).setScale(4, RoundingMode.HALF_UP);
		}
		return PUNTAJE_PARCIAL;
	}

	private String normalizar(String valor) {
		return valor.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
	}

	private Iterable<JsonNode> extraerContenido(JsonNode respuesta) {
		if (respuesta == null) {
			return List.of();
		}
		if (respuesta.isArray()) {
			return respuesta;
		}
		JsonNode contenido = respuesta.path("content");
		if (contenido.isArray()) {
			return contenido;
		}
		return List.of();
	}

	private Map<String, String> resolverTerminos(ContextoAsociacionModel contexto, JsonNode parametros) {
		Map<String, String> terminos = new LinkedHashMap<>();
		for (String campo : camposBusqueda(parametros)) {
			String valor = contexto.getValores().get(campo);
			if (valor != null && !valor.isBlank()) {
				terminos.put(campo, valor.trim());
			}
		}
		return terminos;
	}

	private List<String> camposBusqueda(JsonNode parametros) {
		JsonNode configurados = parametros.path("camposBusqueda");
		if (!configurados.isArray() || configurados.isEmpty()) {
			return CAMPOS_BUSQUEDA_POR_DEFECTO;
		}
		List<String> campos = new ArrayList<>();
		for (JsonNode campo : configurados) {
			campos.add(campo.asText());
		}
		return campos;
	}

	private String ruta(JsonNode parametros) {
		return Optional.ofNullable(parametros.path("ruta").asText(null)).filter(valor -> !valor.isBlank())
				.orElse(RUTA_POR_DEFECTO);
	}

	private String parametroBusqueda(JsonNode parametros) {
		return Optional.ofNullable(parametros.path("parametroBusqueda").asText(null))
				.filter(valor -> !valor.isBlank()).orElse(PARAMETRO_POR_DEFECTO);
	}

	private JsonNode leerParametros(ConfiguracionConector configuracion) {
		if (configuracion.getParametros() == null || configuracion.getParametros().isBlank()) {
			return objectMapper.createObjectNode();
		}
		try {
			return objectMapper.readTree(configuracion.getParametros());
		} catch (Exception e) {
			log.warn("Los parametros del conector Follow no son un JSON valido, se ignoran");
			return objectMapper.createObjectNode();
		}
	}

	private String texto(JsonNode nodo, String campo) {
		JsonNode valor = nodo.path(campo);
		return valor.isMissingNode() || valor.isNull() ? null : valor.asText();
	}
}
