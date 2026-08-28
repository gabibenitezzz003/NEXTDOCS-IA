package com.nextdocs.ai.servicios;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdocs.ai.entidades.CampoPlantilla;
import com.nextdocs.ai.entidades.ReglaPlantilla;
import com.nextdocs.ai.entidades.VersionPlantilla;
import com.nextdocs.ai.enumeraciones.EstrategiaSegmentacion;
import com.nextdocs.ai.enumeraciones.TipoReglaValidacion;

import org.springframework.stereotype.Service;

@Service
public class ValidadorPlantillaService {

	private static final Set<TipoReglaValidacion> EXIGEN_CAMPO = Set.of(TipoReglaValidacion.OBLIGATORIO,
			TipoReglaValidacion.FORMATO, TipoReglaValidacion.RANGO, TipoReglaValidacion.VIGENCIA,
			TipoReglaValidacion.CATALOGO, TipoReglaValidacion.CONFIANZA_MINIMA);

	private final ObjectMapper objectMapper;

	public ValidadorPlantillaService(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public List<String> validar(VersionPlantilla version, List<CampoPlantilla> campos,
			List<ReglaPlantilla> reglas) {
		List<String> errores = new ArrayList<>();
		Set<String> claves = new HashSet<>();
		for (CampoPlantilla campo : campos) {
			claves.add(campo.getClave());
		}
		validarVersion(version, errores);
		validarCampos(campos, errores);
		validarReglas(reglas, claves, errores);
		return errores;
	}

	private void validarVersion(VersionPlantilla version, List<String> errores) {
		BigDecimal umbral = version.getUmbralAutoaprobacion();
		if (umbral != null && (umbral.compareTo(BigDecimal.ZERO) < 0 || umbral.compareTo(BigDecimal.ONE) > 0)) {
			errores.add("El umbral de autoaprobacion debe estar entre 0 y 1");
		}
		validarSegmentacion(version, errores);
	}

	private void validarSegmentacion(VersionPlantilla version, List<String> errores) {
		EstrategiaSegmentacion estrategia = version.getEstrategiaSegmentacion();
		if (estrategia == null || estrategia == EstrategiaSegmentacion.NINGUNA) {
			return;
		}
		if (estrategia == EstrategiaSegmentacion.PAGINAS_FIJAS && version.getPaginasPorDocumento() < 1) {
			errores.add("La estrategia PAGINAS_FIJAS necesita paginasPorDocumento mayor a cero");
		}
		if (estrategia == EstrategiaSegmentacion.PATRON_TEXTO) {
			String patron = version.getPatronInicioDocumento();
			if (patron == null || patron.isBlank()) {
				errores.add("La estrategia PATRON_TEXTO necesita patronInicioDocumento");
				return;
			}
			try {
				Pattern.compile(patron);
			} catch (PatternSyntaxException e) {
				errores.add("El patron de inicio de documento no es una expresion regular valida");
			}
		}
	}

	private void validarCampos(List<CampoPlantilla> campos, List<String> errores) {
		if (campos.isEmpty()) {
			errores.add("La version no tiene ningun campo definido");
			return;
		}
		boolean hayExtraible = false;
		for (CampoPlantilla campo : campos) {
			if (campo.isExtraer()) {
				hayExtraible = true;
			}
			if (campo.isRequerido() && !campo.isExtraer()) {
				errores.add("El campo " + campo.getClave() + " es requerido pero no se extrae");
			}
			validarExpresionRegular(campo, errores);
			validarUmbral(campo, errores);
		}
		if (!hayExtraible) {
			errores.add("La version no tiene ningun campo marcado para extraer");
		}
	}

	private void validarExpresionRegular(CampoPlantilla campo, List<String> errores) {
		if (campo.getExpresionRegular() == null || campo.getExpresionRegular().isBlank()) {
			return;
		}
		try {
			Pattern.compile(campo.getExpresionRegular());
		} catch (PatternSyntaxException e) {
			errores.add("La expresion regular del campo " + campo.getClave() + " no es valida");
		}
	}

	private void validarUmbral(CampoPlantilla campo, List<String> errores) {
		BigDecimal umbral = campo.getUmbralConfianza();
		if (umbral == null) {
			return;
		}
		if (umbral.compareTo(BigDecimal.ZERO) < 0 || umbral.compareTo(BigDecimal.ONE) > 0) {
			errores.add("El umbral de confianza del campo " + campo.getClave() + " debe estar entre 0 y 1");
		}
	}

	private void validarReglas(List<ReglaPlantilla> reglas, Set<String> claves, List<String> errores) {
		for (ReglaPlantilla regla : reglas) {
			if (EXIGEN_CAMPO.contains(regla.getTipo())) {
				if (regla.getCampoObjetivo() == null || regla.getCampoObjetivo().isBlank()) {
					errores.add("La regla " + regla.getCodigo() + " de tipo " + regla.getTipo()
							+ " necesita un campo objetivo");
				} else if (!claves.contains(regla.getCampoObjetivo())) {
					errores.add("La regla " + regla.getCodigo() + " apunta al campo inexistente "
							+ regla.getCampoObjetivo());
				}
			}
			validarConfiguracion(regla, claves, errores);
		}
	}

	private void validarConfiguracion(ReglaPlantilla regla, Set<String> claves, List<String> errores) {
		if (regla.getConfiguracion() == null || regla.getConfiguracion().isBlank()) {
			if (exigeConfiguracion(regla.getTipo())) {
				errores.add("La regla " + regla.getCodigo() + " de tipo " + regla.getTipo()
						+ " necesita configuracion");
			}
			return;
		}
		try {
			var nodo = objectMapper.readTree(regla.getConfiguracion());
			if (regla.getTipo() == TipoReglaValidacion.COMPARACION_CAMPOS) {
				validarComparacion(regla, nodo, claves, errores);
			}
		} catch (Exception e) {
			errores.add("La configuracion de la regla " + regla.getCodigo() + " no es un JSON valido");
		}
	}

	private void validarComparacion(ReglaPlantilla regla, com.fasterxml.jackson.databind.JsonNode nodo,
			Set<String> claves, List<String> errores) {
		for (String clave : List.of("campoA", "campoB")) {
			if (!nodo.hasNonNull(clave)) {
				errores.add("La regla " + regla.getCodigo() + " necesita " + clave + " en su configuracion");
				continue;
			}
			String referencia = nodo.get(clave).asText();
			if (!claves.contains(referencia)) {
				errores.add("La regla " + regla.getCodigo() + " referencia el campo inexistente " + referencia);
			}
		}
	}

	private boolean exigeConfiguracion(TipoReglaValidacion tipo) {
		return tipo == TipoReglaValidacion.RANGO || tipo == TipoReglaValidacion.CATALOGO
				|| tipo == TipoReglaValidacion.COMPARACION_CAMPOS
				|| tipo == TipoReglaValidacion.CONFIANZA_MINIMA;
	}
}
