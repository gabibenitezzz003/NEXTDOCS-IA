package com.nextdocs.ai.servicios.proveedores;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Locale;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdocs.ai.clientes.GeminiCliente;
import com.nextdocs.ai.entidades.ConfiguracionProveedor;
import com.nextdocs.ai.enumeraciones.PresenciaCampo;
import com.nextdocs.ai.enumeraciones.TipoDatoCampo;
import com.nextdocs.ai.enumeraciones.ProveedorDocumentalIa;
import com.nextdocs.ai.exceptions.ProveedorNoDisponibleException;
import com.nextdocs.ai.interfaces.ProveedorDocumentalIaInt;
import com.nextdocs.ai.modelos.CampoEsquemaModel;
import com.nextdocs.ai.modelos.CampoSugeridoModel;
import com.nextdocs.ai.modelos.ResultadoClasificacionModel;
import com.nextdocs.ai.modelos.SolicitudClasificacionModel;
import com.nextdocs.ai.modelos.ResultadoExtraccionModel;
import com.nextdocs.ai.modelos.SolicitudExtraccionModel;
import com.nextdocs.ai.modelos.ValorCanonicoModel;
import com.nextdocs.ai.repositorios.ConfiguracionProveedorRepository;
import com.nextdocs.ai.utiles.ResolvedorSecreto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProveedorGeminiService implements ProveedorDocumentalIaInt {

	public static final String MODELO_POR_DEFECTO = "gemini-2.5-flash";

	public static final String VARIABLE_CLAVE_GLOBAL = "NEXTDOCS_GEMINI_CLAVE";

	private static final Logger log = LoggerFactory.getLogger(ProveedorGeminiService.class);

	private static final BigDecimal MIL_MILLONES = new BigDecimal("1000000");

	private static final double TEMPERATURA_POR_DEFECTO = 0.0;

	private final GeminiCliente geminiCliente;

	private final ConstructorSolicitudGemini constructorSolicitud;

	private final ConfiguracionProveedorRepository configuracionProveedorRepository;

	private final ObjectMapper objectMapper;

	public ProveedorGeminiService(GeminiCliente geminiCliente, ConstructorSolicitudGemini constructorSolicitud,
			ConfiguracionProveedorRepository configuracionProveedorRepository, ObjectMapper objectMapper) {
		this.geminiCliente = geminiCliente;
		this.constructorSolicitud = constructorSolicitud;
		this.configuracionProveedorRepository = configuracionProveedorRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	public ProveedorDocumentalIa tipo() {
		return ProveedorDocumentalIa.GEMINI;
	}

	@Override
	public boolean estaDisponible() {
		return ResolvedorSecreto.resolver(VARIABLE_CLAVE_GLOBAL).isPresent();
	}

	@Override
	public ResultadoExtraccionModel extraer(SolicitudExtraccionModel solicitud) {
		long inicio = System.currentTimeMillis();
		Optional<ConfiguracionProveedor> configuracion = configuracionProveedorRepository
				.buscarPorProveedor(solicitud.getTenantId(), ProveedorDocumentalIa.GEMINI);
		String modelo = resolverModelo(solicitud, configuracion);
		String claveApi = resolverClave(configuracion);
		JsonNode parametros = leerParametros(configuracion);

		String cuerpo = constructorSolicitud.construir(solicitud, temperatura(parametros));
		JsonNode respuesta = geminiCliente.generarContenido(modelo, claveApi, cuerpo);

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

	@Override
	public ResultadoClasificacionModel clasificar(SolicitudClasificacionModel solicitud) {
		long inicio = System.currentTimeMillis();
		Optional<ConfiguracionProveedor> configuracion = configuracionProveedorRepository
				.buscarPorProveedor(solicitud.getTenantId(), ProveedorDocumentalIa.GEMINI);
		String modelo = configuracion.map(ConfiguracionProveedor::getModelo).filter(valor -> !valor.isBlank())
				.orElse(MODELO_POR_DEFECTO);
		String claveApi = resolverClave(configuracion);
		JsonNode parametros = leerParametros(configuracion);

		String cuerpo = constructorSolicitud.construirClasificacion(solicitud, temperatura(parametros));
		JsonNode respuesta = geminiCliente.generarContenido(modelo, claveApi, cuerpo);
		JsonNode contenido = extraerContenido(respuesta);

		ResultadoClasificacionModel resultado = new ResultadoClasificacionModel();
		resultado.setProveedor(tipo());
		resultado.setModelo(modelo);
		resultado.setCodigoPropuesto(normalizarCodigo(
				contenido.path(InstruccionClasificacion.CAMPO_TIPO).asText(null)));
		resultado.setConfianza(recortarConfianza(
				contenido.path(InstruccionClasificacion.CAMPO_CONFIANZA).asDouble(0)));
		resultado.setMotivo(recortar(contenido.path(InstruccionClasificacion.CAMPO_MOTIVO).asText(null), 400));
		resultado.setNombreSugerido(
				recortar(contenido.path(InstruccionClasificacion.CAMPO_NOMBRE_SUGERIDO).asText(null), 128));
		for (JsonNode nodo : contenido.path(InstruccionClasificacion.CAMPO_CAMPOS_SUGERIDOS)) {
			CampoSugeridoModel campo = leerCampoSugerido(nodo);
			if (campo != null) {
				resultado.getCamposSugeridos().add(campo);
			}
		}
		JsonNode uso = respuesta.path("usageMetadata");
		resultado.setTokensEntrada(uso.path("promptTokenCount").asLong(0));
		resultado.setTokensSalida(uso.path("candidatesTokenCount").asLong(0));
		resultado.setDuracionMilisegundos(System.currentTimeMillis() - inicio);
		return resultado;
	}

	private CampoSugeridoModel leerCampoSugerido(JsonNode nodo) {
		String clave = nodo.path("clave").asText(null);
		if (clave == null || clave.isBlank()) {
			return null;
		}
		CampoSugeridoModel campo = new CampoSugeridoModel();
		campo.setClave(recortar(clave, 64));
		campo.setEtiqueta(recortar(nodo.path("etiqueta").asText(clave), 128));
		campo.setTipoDato(TipoDatoCampo.desde(nodo.path("tipoDato").asText(null)));
		campo.setRequerido(nodo.path("requerido").asBoolean(false));
		campo.setEjemplo(recortar(nodo.path("ejemplo").asText(null), 256));
		return campo;
	}

	private String normalizarCodigo(String crudo) {
		if (crudo == null || crudo.isBlank()) {
			return ResultadoClasificacionModel.CODIGO_DESCONOCIDO;
		}
		return crudo.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s-]+", "_");
	}

	private BigDecimal recortarConfianza(double crudo) {
		double acotada = Math.max(0, Math.min(1, crudo));
		return BigDecimal.valueOf(acotada).setScale(4, RoundingMode.HALF_UP);
	}

	private String recortar(String valor, int largo) {
		if (valor == null || valor.isBlank()) {
			return null;
		}
		return valor.length() <= largo ? valor : valor.substring(0, largo);
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
				() -> new ProveedorNoDisponibleException("No hay credencial configurada para Gemini", false));
	}

	private JsonNode leerParametros(Optional<ConfiguracionProveedor> configuracion) {
		return configuracion.map(ConfiguracionProveedor::getParametros).filter(valor -> !valor.isBlank())
				.map(this::leerJson).orElseGet(objectMapper::createObjectNode);
	}

	private JsonNode leerJson(String texto) {
		try {
			return objectMapper.readTree(texto);
		} catch (Exception e) {
			log.warn("Los parametros de la configuracion de Gemini no son un JSON valido, se ignoran");
			return objectMapper.createObjectNode();
		}
	}

	private double temperatura(JsonNode parametros) {
		return parametros.hasNonNull("temperatura") ? parametros.get("temperatura").asDouble()
				: TEMPERATURA_POR_DEFECTO;
	}

	private void completarUso(ResultadoExtraccionModel resultado, JsonNode respuesta, JsonNode parametros) {
		JsonNode uso = respuesta.path("usageMetadata");
		resultado.setTokensEntrada(uso.path("promptTokenCount").asLong(0));
		resultado.setTokensSalida(uso.path("candidatesTokenCount").asLong(0));
		resultado.setCosto(calcularCosto(resultado.getTokensEntrada(), resultado.getTokensSalida(), parametros));
	}

	private BigDecimal calcularCosto(long tokensEntrada, long tokensSalida, JsonNode parametros) {
		if (!parametros.hasNonNull("costoPorMillonEntrada") || !parametros.hasNonNull("costoPorMillonSalida")) {
			return BigDecimal.ZERO;
		}
		BigDecimal precioEntrada = new BigDecimal(parametros.get("costoPorMillonEntrada").asText());
		BigDecimal precioSalida = new BigDecimal(parametros.get("costoPorMillonSalida").asText());
		BigDecimal costoEntrada = precioEntrada.multiply(BigDecimal.valueOf(tokensEntrada)).divide(MIL_MILLONES,
				8, RoundingMode.HALF_UP);
		BigDecimal costoSalida = precioSalida.multiply(BigDecimal.valueOf(tokensSalida)).divide(MIL_MILLONES, 8,
				RoundingMode.HALF_UP);
		return costoEntrada.add(costoSalida).setScale(6, RoundingMode.HALF_UP);
	}

	private void completarValores(ResultadoExtraccionModel resultado, JsonNode respuesta,
			SolicitudExtraccionModel solicitud, JsonNode parametros) {
		JsonNode contenido = extraerContenido(respuesta);
		resultado.setTipoDetectado(contenido.path(ConstructorSolicitudGemini.CAMPO_TIPO_DETECTADO)
				.asText(solicitud.getCodigoPlantilla()));

		Map<String, CampoEsquemaModel> esperados = new HashMap<>();
		for (CampoEsquemaModel campo : solicitud.getCampos()) {
			esperados.put(campo.getClave(), campo);
		}

		Set<String> devueltos = new LinkedHashSet<>();
		BigDecimal factor = factorCalibracion(parametros);
		for (JsonNode nodo : contenido.path(ConstructorSolicitudGemini.CAMPO_CAMPOS)) {
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
		JsonNode candidatos = respuesta.path("candidates");
		if (!candidatos.isArray() || candidatos.isEmpty()) {
			throw new ProveedorNoDisponibleException("Gemini no devolvio ningun candidato", true);
		}
		JsonNode candidato = candidatos.get(0);
		String motivoFin = candidato.path("finishReason").asText("");
		JsonNode partes = candidato.path("content").path("parts");
		if (!partes.isArray() || partes.isEmpty()) {
			throw new ProveedorNoDisponibleException(
					"Gemini no devolvio contenido, finishReason " + motivoFin, !"SAFETY".equals(motivoFin));
		}
		try {
			return objectMapper.readTree(partes.get(0).path("text").asText());
		} catch (Exception e) {
			throw new ProveedorNoDisponibleException("Gemini devolvio un JSON que no se pudo interpretar", true, e);
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

	private BigDecimal factorCalibracion(JsonNode parametros) {
		if (!parametros.hasNonNull("factorCalibracionConfianza")) {
			return BigDecimal.ONE;
		}
		return new BigDecimal(parametros.get("factorCalibracionConfianza").asText());
	}
}
