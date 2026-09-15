package com.nextdocs.ai.servicios;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.PlantillaDocumental;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.TipoPropuesto;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.EstadoTipoPropuesto;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.CampoSugeridoModel;
import com.nextdocs.ai.modelos.ResultadoClasificacionModel;
import com.nextdocs.ai.enumeraciones.PoliticaOriginalFisico;
import com.nextdocs.ai.enumeraciones.TipoDatoCampo;
import com.nextdocs.ai.repositorios.PlantillaDocumentalRepository;
import com.nextdocs.ai.repositorios.TipoPropuestoRepository;
import com.nextdocs.ai.servicios.catalogo.CatalogoDocumentalBase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TipoPropuestoService {

	public static final String ENTIDAD = "TipoPropuesto";

	private static final int MAXIMO_CAMPOS = 30;

	private static final BigDecimal UMBRAL_PROPUESTO = new BigDecimal("0.8500");

	private static final BigDecimal UMBRAL_AUTOAPROBACION = new BigDecimal("0.9900");

	private static final Logger log = LoggerFactory.getLogger(TipoPropuestoService.class);

	private final TipoPropuestoRepository tipoPropuestoRepository;

	private final SembradorCatalogoService sembradorCatalogoService;

	private final PlantillaDocumentalRepository plantillaDocumentalRepository;

	private final AuditoriaService auditoriaService;

	private final ObjectMapper objectMapper;

	public TipoPropuestoService(TipoPropuestoRepository tipoPropuestoRepository,
			SembradorCatalogoService sembradorCatalogoService,
			PlantillaDocumentalRepository plantillaDocumentalRepository, AuditoriaService auditoriaService,
			ObjectMapper objectMapper) {
		this.tipoPropuestoRepository = tipoPropuestoRepository;
		this.sembradorCatalogoService = sembradorCatalogoService;
		this.plantillaDocumentalRepository = plantillaDocumentalRepository;
		this.auditoriaService = auditoriaService;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public TipoPropuesto registrar(Documento documento, ResultadoClasificacionModel resultado) {
		String codigo = codigoDe(resultado);
		if (codigo == null) {
			return null;
		}
		TipoPropuesto propuesto = tipoPropuestoRepository
				.buscarPorCodigo(documento.getTenant().getId(), codigo)
				.orElseGet(() -> nuevo(documento, codigo, resultado));
		propuesto.setVeces(propuesto.getVeces() + 1);
		propuesto.setUltimoDocumento(documento);
		if (propuesto.getEstado() != EstadoTipoPropuesto.APROBADO
				&& propuesto.getCamposSugeridos() == null
				&& !resultado.getCamposSugeridos().isEmpty()) {
			propuesto.setCamposSugeridos(serializar(resultado.getCamposSugeridos()));
		}
		tipoPropuestoRepository.save(propuesto);
		log.info("Tipo propuesto {} en el tenant {}, visto {} veces", codigo,
				documento.getTenant().getId(), propuesto.getVeces());
		return propuesto;
	}

	@Transactional(readOnly = true)
	public List<TipoPropuesto> listar(String tenantId, EstadoTipoPropuesto estado) {
		return tipoPropuestoRepository.listarPorTenant(tenantId, estado);
	}

	@Transactional
	public TipoPropuesto descartar(String tenantId, String propuestoId) {
		TipoPropuesto propuesto = buscar(tenantId, propuestoId);
		propuesto.setEstado(EstadoTipoPropuesto.DESCARTADO);
		propuesto.setResuelto(Instant.now());
		tipoPropuestoRepository.save(propuesto);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.PLANTILLA_DEPRECADA, ENTIDAD,
				propuestoId, Map.of("codigoSugerido", propuesto.getCodigoSugerido()));
		return propuesto;
	}

	@Transactional(readOnly = true)
	public TipoPropuesto buscar(String tenantId, String propuestoId) {
		return tipoPropuestoRepository.buscarPorIdYTenant(propuestoId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(ENTIDAD, propuestoId));
	}

	@Transactional
	public PlantillaDocumental crearAutomatico(Documento documento, ResultadoClasificacionModel resultado) {
		TipoPropuesto propuesto = registrar(documento, resultado);
		if (propuesto == null) {
			return null;
		}
		Tenant tenant = documento.getTenant();
		if (propuesto.getCodigoAprobado() != null) {
			return plantillaDocumentalRepository
					.buscarPorCodigo(tenant.getId(), propuesto.getCodigoAprobado()).orElse(null);
		}
		List<CatalogoDocumentalBase.CampoBase> campos = camposUtilizables(propuesto);
		if (campos.isEmpty()) {
			return null;
		}
		CatalogoDocumentalBase.TipoBase tipo = tipoDe(propuesto, campos);
		try {
			sembradorCatalogoService.crearTipo(tenant, tipo);
		} catch (ValidacionException e) {
			log.info("La plantilla {} ya existia en el tenant {}; se reutiliza", tipo.codigo(),
					tenant.getId());
		}
		marcarAprobado(propuesto, tipo.codigo());
		auditoriaService.registrarConDetalle(tenant.getId(), AccionAuditoria.PLANTILLA_PUBLICADA, ENTIDAD,
				propuesto.getId(), Map.of("codigo", tipo.codigo(), "campos", campos.size(),
						"vecesVisto", propuesto.getVeces(), "automatico", true));
		log.info("Tipo {} creado automaticamente en el tenant {} a partir del documento {}",
				tipo.codigo(), tenant.getId(), documento.getId());
		return plantillaDocumentalRepository.buscarPorCodigo(tenant.getId(), tipo.codigo()).orElse(null);
	}

	@Transactional
	public TipoPropuesto aprobar(Tenant tenant, String propuestoId) {
		TipoPropuesto propuesto = buscar(tenant.getId(), propuestoId);
		if (propuesto.getEstado() == EstadoTipoPropuesto.APROBADO) {
			throw new ValidacionException("El tipo " + propuesto.getCodigoSugerido() + " ya fue aprobado");
		}
		List<CatalogoDocumentalBase.CampoBase> campos = camposUtilizables(propuesto);
		if (campos.isEmpty()) {
			throw new ValidacionException("La propuesta " + propuesto.getCodigoSugerido()
					+ " no trae campos sugeridos, asi que aprobarla dejaria una plantilla que no extrae nada");
		}

		CatalogoDocumentalBase.TipoBase tipo = tipoDe(propuesto, campos);
		sembradorCatalogoService.crearTipo(tenant, tipo);

		marcarAprobado(propuesto, tipo.codigo());
		auditoriaService.registrarConDetalle(tenant.getId(), AccionAuditoria.PLANTILLA_PUBLICADA, ENTIDAD,
				propuesto.getId(), Map.of("codigo", tipo.codigo(), "campos", campos.size(),
						"vecesVisto", propuesto.getVeces()));
		log.info("Tipo propuesto {} aprobado como plantilla en el tenant {}", tipo.codigo(), tenant.getId());
		return propuesto;
	}

	private List<CatalogoDocumentalBase.CampoBase> camposUtilizables(TipoPropuesto propuesto) {
		List<CatalogoDocumentalBase.CampoBase> campos = new ArrayList<>();
		for (CampoSugeridoModel sugerido : camposDe(propuesto)) {
			String clave = sugerido.getClave() == null ? null : sugerido.getClave().trim();
			if (clave == null || clave.isBlank()) {
				continue;
			}
			campos.add(CatalogoDocumentalBase.CampoBase.de(clave,
					sugerido.getEtiqueta() == null ? clave : sugerido.getEtiqueta(),
					sugerido.getTipoDato() == null ? TipoDatoCampo.TEXTO : sugerido.getTipoDato(),
					sugerido.isRequerido(), UMBRAL_PROPUESTO));
		}
		return campos;
	}

	private CatalogoDocumentalBase.TipoBase tipoDe(TipoPropuesto propuesto,
			List<CatalogoDocumentalBase.CampoBase> campos) {
		return new CatalogoDocumentalBase.TipoBase(propuesto.getCodigoSugerido(),
				propuesto.getNombreSugerido() == null ? propuesto.getCodigoSugerido()
						: propuesto.getNombreSugerido(),
				"PROPUESTO",
				propuesto.getMotivo() == null ? "Tipo propuesto por el clasificador" : propuesto.getMotivo(),
				UMBRAL_AUTOAPROBACION, PoliticaOriginalFisico.NO_REQUIERE, campos, List.of());
	}

	@Transactional
	public void marcarAprobado(TipoPropuesto propuesto, String codigoPlantilla) {
		propuesto.setEstado(EstadoTipoPropuesto.APROBADO);
		propuesto.setCodigoAprobado(codigoPlantilla);
		propuesto.setResuelto(Instant.now());
		tipoPropuestoRepository.save(propuesto);
	}

	public List<CampoSugeridoModel> camposDe(TipoPropuesto propuesto) {
		if (propuesto.getCamposSugeridos() == null || propuesto.getCamposSugeridos().isBlank()) {
			return List.of();
		}
		try {
			return objectMapper.readerForListOf(CampoSugeridoModel.class)
					.readValue(propuesto.getCamposSugeridos());
		} catch (Exception e) {
			throw new ValidacionException("Los campos sugeridos del tipo " + propuesto.getCodigoSugerido()
					+ " no se pudieron leer: " + e.getMessage());
		}
	}

	public static String normalizarCodigo(String crudo) {
		if (crudo == null || crudo.isBlank()) {
			return null;
		}
		String limpio = java.text.Normalizer.normalize(crudo.trim(), java.text.Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toUpperCase(Locale.ROOT)
				.replaceAll("[^A-Z0-9]+", "_")
				.replaceAll("^_+|_+$", "");
		if (limpio.isBlank()) {
			return null;
		}
		return limpio.length() <= 64 ? limpio : limpio.substring(0, 64);
	}

	private TipoPropuesto nuevo(Documento documento, String codigo, ResultadoClasificacionModel resultado) {
		TipoPropuesto propuesto = new TipoPropuesto();
		propuesto.setTenant(documento.getTenant());
		propuesto.setCodigoSugerido(codigo);
		propuesto.setNombreSugerido(resultado.getNombreSugerido());
		propuesto.setMotivo(resultado.getMotivo());
		propuesto.setEstado(EstadoTipoPropuesto.PENDIENTE);
		propuesto.setAlta(Instant.now());
		return propuesto;
	}

	private String codigoDe(ResultadoClasificacionModel resultado) {
		String desdeNombre = normalizarCodigo(resultado.getNombreSugerido());
		if (desdeNombre != null) {
			return desdeNombre;
		}
		String desdeTipo = normalizarCodigo(resultado.getCodigoPropuesto());
		return ResultadoClasificacionModel.CODIGO_DESCONOCIDO.equals(desdeTipo) ? null : desdeTipo;
	}

	private String serializar(List<CampoSugeridoModel> campos) {
		try {
			return objectMapper.writeValueAsString(
					campos.size() <= MAXIMO_CAMPOS ? campos : campos.subList(0, MAXIMO_CAMPOS));
		} catch (Exception e) {
			log.warn("No se pudieron serializar los campos sugeridos: {}", e.getMessage());
			return null;
		}
	}
}
