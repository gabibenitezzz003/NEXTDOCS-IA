package com.nextdocs.ai.servicios;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdocs.ai.entidades.CampoPlantilla;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.EjecucionExtraccion;
import com.nextdocs.ai.entidades.EjecucionValidacion;
import com.nextdocs.ai.entidades.HallazgoValidacion;
import com.nextdocs.ai.entidades.ReglaPlantilla;
import com.nextdocs.ai.entidades.ValorExtraido;
import com.nextdocs.ai.entidades.VersionPlantilla;
import com.nextdocs.ai.enumeraciones.EstadoEjecucion;
import com.nextdocs.ai.enumeraciones.PresenciaCampo;
import com.nextdocs.ai.enumeraciones.ResultadoValidacion;
import com.nextdocs.ai.enumeraciones.SeveridadHallazgo;
import com.nextdocs.ai.repositorios.CampoPlantillaRepository;
import com.nextdocs.ai.repositorios.EjecucionValidacionRepository;
import com.nextdocs.ai.repositorios.HallazgoValidacionRepository;
import com.nextdocs.ai.utiles.ValidadorCuit;
import com.nextdocs.ai.repositorios.ReglaPlantillaRepository;
import com.nextdocs.ai.repositorios.ValorExtraidoRepository;
import com.nextdocs.ai.utiles.ContextoCorrelacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ValidacionDocumentalService {

	public static final String CODIGO_CAMPO_REQUERIDO = "CAMPO_REQUERIDO";

	public static final String CODIGO_CAMPO_ILEGIBLE = "CAMPO_ILEGIBLE";

	public static final String CODIGO_CONFIANZA_BAJA = "CONFIANZA_BAJA";

	public static final String CODIGO_CUIT_INVALIDO = "CUIT_INVALIDO";

	private static final Logger log = LoggerFactory.getLogger(ValidacionDocumentalService.class);

	private final EjecucionValidacionRepository ejecucionValidacionRepository;

	private final HallazgoValidacionRepository hallazgoValidacionRepository;

	private final ValorExtraidoRepository valorExtraidoRepository;

	private final CampoPlantillaRepository campoPlantillaRepository;

	private final ReglaPlantillaRepository reglaPlantillaRepository;

	private final ObjectMapper objectMapper;

	public ValidacionDocumentalService(EjecucionValidacionRepository ejecucionValidacionRepository,
			HallazgoValidacionRepository hallazgoValidacionRepository, ValorExtraidoRepository valorExtraidoRepository,
			CampoPlantillaRepository campoPlantillaRepository, ReglaPlantillaRepository reglaPlantillaRepository,
			ObjectMapper objectMapper) {
		this.ejecucionValidacionRepository = ejecucionValidacionRepository;
		this.hallazgoValidacionRepository = hallazgoValidacionRepository;
		this.valorExtraidoRepository = valorExtraidoRepository;
		this.campoPlantillaRepository = campoPlantillaRepository;
		this.reglaPlantillaRepository = reglaPlantillaRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public EjecucionValidacion validar(Documento documento, EjecucionExtraccion ejecucionExtraccion) {
		VersionPlantilla version = documento.getVersionPlantilla();
		EjecucionValidacion ejecucion = new EjecucionValidacion();
		ejecucion.setTenant(documento.getTenant());
		ejecucion.setDocumento(documento);
		ejecucion.setVersionPlantilla(version);
		ejecucion.setEjecucionExtraccion(ejecucionExtraccion);
		ejecucion.setEstado(EstadoEjecucion.EJECUTANDO);
		ejecucion.setCorrelacionId(ContextoCorrelacion.obtenerOGenerar());
		ejecucion.setInicio(Instant.now());
		ejecucion.setAlta(Instant.now());
		ejecucionValidacionRepository.save(ejecucion);

		List<ValorExtraido> valores = valorExtraidoRepository.listarPorEjecucion(ejecucionExtraccion.getId());
		Map<String, ValorExtraido> porClave = new HashMap<>();
		for (ValorExtraido valor : valores) {
			porClave.put(valor.getClaveCampo(), valor);
		}

		List<HallazgoValidacion> hallazgos = new ArrayList<>();
		if (version != null) {
			hallazgos.addAll(evaluarCampos(ejecucion, campoPlantillaRepository.listarPorVersion(version.getId()),
					porClave));
			hallazgos.addAll(evaluarReglas(ejecucion, reglaPlantillaRepository.listarActivasPorVersion(version.getId()),
					porClave));
		}
		hallazgoValidacionRepository.saveAll(hallazgos);

		int bloqueantes = (int) hallazgos.stream().filter(h -> h.getSeveridad() == SeveridadHallazgo.BLOQUEANTE)
				.count();
		int requierenRevision = (int) hallazgos.stream()
				.filter(h -> h.getSeveridad() == SeveridadHallazgo.REQUIERE_REVISION).count();

		ejecucion.setCantidadHallazgos(hallazgos.size());
		ejecucion.setCantidadBloqueantes(bloqueantes);
		ejecucion.setResultado(resolverResultado(bloqueantes, requierenRevision));
		ejecucion.setAutoaprobado(esAutoaprobable(ejecucion.getResultado(), version, porClave));
		ejecucion.setMotivoResultado(motivo(bloqueantes, requierenRevision));
		ejecucion.setEstado(EstadoEjecucion.COMPLETADA);
		ejecucion.setFin(Instant.now());
		ejecucionValidacionRepository.save(ejecucion);
		return ejecucion;
	}

	private ResultadoValidacion resolverResultado(int bloqueantes, int requierenRevision) {
		if (bloqueantes > 0) {
			return ResultadoValidacion.RECHAZADO;
		}
		if (requierenRevision > 0) {
			return ResultadoValidacion.OBSERVADO;
		}
		return ResultadoValidacion.APROBADO;
	}

	private boolean esAutoaprobable(ResultadoValidacion resultado, VersionPlantilla version,
			Map<String, ValorExtraido> porClave) {
		if (resultado != ResultadoValidacion.APROBADO || version == null
				|| version.getUmbralAutoaprobacion() == null) {
			return false;
		}
		BigDecimal umbral = version.getUmbralAutoaprobacion();
		for (ValorExtraido valor : porClave.values()) {
			if (valor.getPresencia() != PresenciaCampo.PRESENTE) {
				continue;
			}
			if (valor.getConfianza() == null || valor.getConfianza().compareTo(umbral) < 0) {
				return false;
			}
		}
		return true;
	}

	private String motivo(int bloqueantes, int requierenRevision) {
		if (bloqueantes > 0) {
			return bloqueantes + " hallazgo(s) bloqueante(s)";
		}
		if (requierenRevision > 0) {
			return requierenRevision + " hallazgo(s) requieren revision humana";
		}
		return "Sin hallazgos";
	}

	private List<HallazgoValidacion> evaluarCampos(EjecucionValidacion ejecucion, List<CampoPlantilla> campos,
			Map<String, ValorExtraido> porClave) {
		List<HallazgoValidacion> hallazgos = new ArrayList<>();
		for (CampoPlantilla campo : campos) {
			if (!campo.isValidar()) {
				continue;
			}
			ValorExtraido valor = porClave.get(campo.getClave());
			if (valor == null || valor.getPresencia() == PresenciaCampo.NO_FIGURA) {
				if (campo.isRequerido()) {
					hallazgos.add(construir(ejecucion, null, CODIGO_CAMPO_REQUERIDO, campo.getClave(),
							SeveridadHallazgo.BLOQUEANTE,
							"El campo " + campo.getEtiqueta() + " no figura en el documento"));
				}
				continue;
			}
			if (valor.getPresencia() == PresenciaCampo.ILEGIBLE) {
				hallazgos.add(construir(ejecucion, null, CODIGO_CAMPO_ILEGIBLE, campo.getClave(),
						campo.isRequerido() ? SeveridadHallazgo.BLOQUEANTE : SeveridadHallazgo.REQUIERE_REVISION,
						"El campo " + campo.getEtiqueta() + " esta presente pero es ilegible"));
				continue;
			}
			hallazgos.addAll(evaluarConfianza(ejecucion, campo, valor));
			hallazgos.addAll(evaluarExpresionRegular(ejecucion, campo, valor));
		}
		return hallazgos;
	}

	private List<HallazgoValidacion> evaluarConfianza(EjecucionValidacion ejecucion, CampoPlantilla campo,
			ValorExtraido valor) {
		if (campo.getUmbralConfianza() == null || valor.getConfianza() == null
				|| valor.getConfianza().compareTo(campo.getUmbralConfianza()) >= 0) {
			return List.of();
		}
		return List.of(construir(ejecucion, null, CODIGO_CONFIANZA_BAJA, campo.getClave(),
				SeveridadHallazgo.REQUIERE_REVISION, "La confianza de lectura de " + campo.getEtiqueta() + " es "
						+ valor.getConfianza() + " y el umbral configurado es " + campo.getUmbralConfianza()));
	}

	private List<HallazgoValidacion> evaluarExpresionRegular(EjecucionValidacion ejecucion, CampoPlantilla campo,
			ValorExtraido valor) {
		if (campo.getExpresionRegular() == null || campo.getExpresionRegular().isBlank()) {
			return List.of();
		}
		String candidato = valor.getValorNormalizado() == null ? valor.getValorCrudo() : valor.getValorNormalizado();
		if (candidato == null) {
			return List.of();
		}
		try {
			if (Pattern.matches(campo.getExpresionRegular(), candidato)) {
				return List.of();
			}
		} catch (PatternSyntaxException e) {
			log.error("Expresion regular invalida en el campo {}", campo.getClave(), e);
			return List.of();
		}
		return List.of(construir(ejecucion, null, "FORMATO_INVALIDO", campo.getClave(),
				SeveridadHallazgo.REQUIERE_REVISION,
				"El valor de " + campo.getEtiqueta() + " no cumple el formato esperado"));
	}

	private List<HallazgoValidacion> evaluarReglas(EjecucionValidacion ejecucion, List<ReglaPlantilla> reglas,
			Map<String, ValorExtraido> porClave) {
		List<HallazgoValidacion> hallazgos = new ArrayList<>();
		for (ReglaPlantilla regla : reglas) {
			if (!cumple(regla, porClave)) {
				hallazgos.add(construir(ejecucion, regla, regla.getCodigo(), regla.getCampoObjetivo(),
						regla.getSeveridad(),
						regla.getMensaje() == null ? "No se cumple la regla " + regla.getNombre()
								: regla.getMensaje()));
			}
		}
		return hallazgos;
	}

	private boolean cumple(ReglaPlantilla regla, Map<String, ValorExtraido> porClave) {
		ValorExtraido valor = porClave.get(regla.getCampoObjetivo());
		if (CODIGO_CUIT_INVALIDO.equals(regla.getCodigo())) {
			return cumpleCuit(valor);
		}
		JsonNode configuracion = leerConfiguracion(regla);
		return switch (regla.getTipo()) {
			case OBLIGATORIO -> valor != null && valor.getPresencia() == PresenciaCampo.PRESENTE;
			case FORMATO -> cumpleFormato(valor, configuracion);
			case RANGO -> cumpleRango(valor, configuracion);
			case VIGENCIA -> cumpleVigencia(valor, configuracion);
			case COMPARACION_CAMPOS -> cumpleComparacion(porClave, configuracion);
			case CATALOGO -> cumpleCatalogo(valor, configuracion);
			case CONFIANZA_MINIMA -> cumpleConfianza(valor, configuracion);
			case EXPRESION -> true;
		};
	}

	private boolean cumpleCuit(ValorExtraido valor) {
		String candidato = normalizado(valor);
		if (candidato == null || candidato.isBlank()) {
			return true;
		}
		return ValidadorCuit.esValido(candidato);
	}

	private boolean cumpleFormato(ValorExtraido valor, JsonNode configuracion) {
		if (valor == null || configuracion == null || !configuracion.hasNonNull("expresionRegular")) {
			return true;
		}
		String candidato = normalizado(valor);
		try {
			return candidato != null && Pattern.matches(configuracion.get("expresionRegular").asText(), candidato);
		} catch (PatternSyntaxException e) {
			return true;
		}
	}

	private boolean cumpleRango(ValorExtraido valor, JsonNode configuracion) {
		String candidato = normalizado(valor);
		if (candidato == null || configuracion == null) {
			return true;
		}
		try {
			BigDecimal numero = new BigDecimal(candidato);
			if (configuracion.hasNonNull("minimo")
					&& numero.compareTo(new BigDecimal(configuracion.get("minimo").asText())) < 0) {
				return false;
			}
			return !configuracion.hasNonNull("maximo")
					|| numero.compareTo(new BigDecimal(configuracion.get("maximo").asText())) <= 0;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private boolean cumpleVigencia(ValorExtraido valor, JsonNode configuracion) {
		String candidato = normalizado(valor);
		if (candidato == null) {
			return true;
		}
		try {
			LocalDate fecha = LocalDate.parse(candidato);
			int diasTolerancia = configuracion != null && configuracion.hasNonNull("diasTolerancia")
					? configuracion.get("diasTolerancia").asInt()
					: 0;
			return !fecha.plusDays(diasTolerancia).isBefore(LocalDate.now());
		} catch (Exception e) {
			return false;
		}
	}

	private boolean cumpleComparacion(Map<String, ValorExtraido> porClave, JsonNode configuracion) {
		if (configuracion == null || !configuracion.hasNonNull("campoA") || !configuracion.hasNonNull("campoB")) {
			return true;
		}
		String a = normalizado(porClave.get(configuracion.get("campoA").asText()));
		String b = normalizado(porClave.get(configuracion.get("campoB").asText()));
		if (a == null || b == null) {
			return true;
		}
		return a.equalsIgnoreCase(b);
	}

	private boolean cumpleCatalogo(ValorExtraido valor, JsonNode configuracion) {
		String candidato = normalizado(valor);
		if (candidato == null || configuracion == null || !configuracion.has("valores")) {
			return true;
		}
		for (JsonNode permitido : configuracion.get("valores")) {
			if (permitido.asText().equalsIgnoreCase(candidato)) {
				return true;
			}
		}
		return false;
	}

	private boolean cumpleConfianza(ValorExtraido valor, JsonNode configuracion) {
		if (valor == null || configuracion == null || !configuracion.hasNonNull("minima")) {
			return true;
		}
		return valor.getConfianza() != null
				&& valor.getConfianza().compareTo(new BigDecimal(configuracion.get("minima").asText())) >= 0;
	}

	private String normalizado(ValorExtraido valor) {
		if (valor == null || valor.getPresencia() != PresenciaCampo.PRESENTE) {
			return null;
		}
		return valor.getValorNormalizado() == null ? valor.getValorCrudo() : valor.getValorNormalizado();
	}

	private JsonNode leerConfiguracion(ReglaPlantilla regla) {
		if (regla.getConfiguracion() == null || regla.getConfiguracion().isBlank()) {
			return null;
		}
		try {
			return objectMapper.readTree(regla.getConfiguracion());
		} catch (Exception e) {
			log.error("Configuracion invalida en la regla {}", regla.getCodigo(), e);
			return null;
		}
	}

	private HallazgoValidacion construir(EjecucionValidacion ejecucion, ReglaPlantilla regla, String codigo,
			String claveCampo, SeveridadHallazgo severidad, String mensaje) {
		HallazgoValidacion hallazgo = new HallazgoValidacion();
		hallazgo.setTenant(ejecucion.getTenant());
		hallazgo.setEjecucion(ejecucion);
		hallazgo.setRegla(regla);
		hallazgo.setCodigoRegla(codigo);
		hallazgo.setClaveCampo(claveCampo);
		hallazgo.setSeveridad(severidad);
		hallazgo.setMensaje(mensaje);
		hallazgo.setAlta(Instant.now());
		return hallazgo;
	}
}
