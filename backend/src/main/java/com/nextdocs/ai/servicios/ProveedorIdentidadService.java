package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.nextdocs.ai.entidades.ProveedorIdentidad;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.OrigenIdentidad;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.NuevoProveedorIdentidadReqModel;
import com.nextdocs.ai.modelos.ProveedorIdentidadModel;
import com.nextdocs.ai.repositorios.ProveedorIdentidadRepository;
import com.nextdocs.ai.repositorios.RolRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProveedorIdentidadService {

	public static final String ENTIDAD = "ProveedorIdentidad";

	private final ProveedorIdentidadRepository proveedorIdentidadRepository;

	private final RolRepository rolRepository;

	private final VerificadorTokenIdpService verificadorTokenIdpService;

	private final AuditoriaService auditoriaService;

	private final com.nextdocs.ai.config.PropiedadesFederacion propiedades;

	public ProveedorIdentidadService(ProveedorIdentidadRepository proveedorIdentidadRepository,
			RolRepository rolRepository, VerificadorTokenIdpService verificadorTokenIdpService,
			AuditoriaService auditoriaService, com.nextdocs.ai.config.PropiedadesFederacion propiedades) {
		this.propiedades = propiedades;
		this.proveedorIdentidadRepository = proveedorIdentidadRepository;
		this.rolRepository = rolRepository;
		this.verificadorTokenIdpService = verificadorTokenIdpService;
		this.auditoriaService = auditoriaService;
	}

	@Transactional
	public ProveedorIdentidadModel crear(Tenant tenant, NuevoProveedorIdentidadReqModel datos) {
		String codigo = datos.getCodigo().trim().toUpperCase(java.util.Locale.ROOT);
		proveedorIdentidadRepository.buscarPorCodigo(tenant.getId(), codigo).ifPresent(existente -> {
			throw new ValidacionException("Ya existe un proveedor de identidad " + codigo + " en el tenant");
		});
		OrigenIdentidad origen = OrigenIdentidad.desde(datos.getOrigen());
		if (origen == null || origen == OrigenIdentidad.LOCAL) {
			throw new ValidacionException("El origen debe ser FOLLOW, CIMA, VALID360 u OIDC");
		}
		exigirHttps(datos.getEmisor(), "emisor");
		exigirHttps(datos.getUrlJwks(), "urlJwks");
		if (datos.getCodigoRolPorDefecto() != null && !datos.getCodigoRolPorDefecto().isBlank()) {
			rolRepository.buscarPorCodigo(tenant.getId(), datos.getCodigoRolPorDefecto().trim())
					.orElseThrow(() -> new ValidacionException(
							"El rol " + datos.getCodigoRolPorDefecto() + " no existe en el tenant"));
		}

		ProveedorIdentidad proveedor = new ProveedorIdentidad();
		proveedor.setTenant(tenant);
		proveedor.setCodigo(codigo);
		proveedor.setNombre(datos.getNombre());
		proveedor.setOrigen(origen);
		proveedor.setEmisor(datos.getEmisor().trim());
		proveedor.setUrlJwks(datos.getUrlJwks().trim());
		proveedor.setAudiencia(datos.getAudiencia());
		proveedor.setClaimSujeto(porDefecto(datos.getClaimSujeto(), "sub"));
		proveedor.setClaimEmail(porDefecto(datos.getClaimEmail(), "email"));
		proveedor.setClaimNombre(porDefecto(datos.getClaimNombre(), "name"));
		proveedor.setPermitirJit(datos.isPermitirJit());
		proveedor.setPermitirVinculoPorEmail(datos.isPermitirVinculoPorEmail());
		proveedor.setVerificaEmail(datos.isVerificaEmail());
		proveedor.setCodigoRolPorDefecto(datos.getCodigoRolPorDefecto());
		proveedor.setDominiosPermitidos(normalizarDominios(datos.getDominiosPermitidos()));
		proveedor.setOrigenesEmbedPermitidos(datos.getOrigenesEmbedPermitidos());
		proveedor.setSegundosVigenciaCodigo(datos.getSegundosVigenciaCodigo());
		configurarOauth(proveedor, datos);
		proveedor.setActivo(true);
		proveedor.setAlta(Instant.now());
		proveedorIdentidadRepository.save(proveedor);

		auditoriaService.registrarConDetalle(tenant.getId(), AccionAuditoria.PROVEEDOR_IDENTIDAD_CONFIGURADO,
				ENTIDAD, proveedor.getId(),
				Map.of("codigo", codigo, "origen", origen.name(), "emisor", proveedor.getEmisor(), "jit",
						proveedor.isPermitirJit(), "vinculoPorEmail", proveedor.isPermitirVinculoPorEmail()));
		return aModelo(proveedor);
	}

	@Transactional(readOnly = true)
	public List<ProveedorIdentidadModel> listar(String tenantId) {
		return proveedorIdentidadRepository.listarPorTenant(tenantId).stream().map(this::aModelo).toList();
	}

	@Transactional
	public ProveedorIdentidadModel cambiarActivo(String tenantId, String proveedorId, boolean activo) {
		ProveedorIdentidad proveedor = proveedorIdentidadRepository.buscarPorIdYTenant(proveedorId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(ENTIDAD, proveedorId));
		proveedor.setActivo(activo);
		proveedorIdentidadRepository.save(proveedor);
		verificadorTokenIdpService.olvidar(proveedorId);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.PROVEEDOR_IDENTIDAD_CONFIGURADO, ENTIDAD,
				proveedorId, Map.of("codigo", proveedor.getCodigo(), "activo", activo));
		return aModelo(proveedor);
	}

	@Transactional
	public void eliminar(String tenantId, String proveedorId) {
		ProveedorIdentidad proveedor = proveedorIdentidadRepository.buscarPorIdYTenant(proveedorId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(ENTIDAD, proveedorId));
		proveedor.setBaja(Instant.now());
		proveedor.setActivo(false);
		proveedorIdentidadRepository.save(proveedor);
		verificadorTokenIdpService.olvidar(proveedorId);
		auditoriaService.registrar(tenantId, AccionAuditoria.PROVEEDOR_IDENTIDAD_ELIMINADO, ENTIDAD, proveedorId);
	}

	public ProveedorIdentidadModel aModelo(ProveedorIdentidad proveedor) {
		ProveedorIdentidadModel modelo = new ProveedorIdentidadModel();
		modelo.setId(proveedor.getId());
		modelo.setCodigo(proveedor.getCodigo());
		modelo.setNombre(proveedor.getNombre());
		modelo.setOrigen(proveedor.getOrigen().name());
		modelo.setEmisor(proveedor.getEmisor());
		modelo.setUrlJwks(proveedor.getUrlJwks());
		modelo.setAudiencia(proveedor.getAudiencia());
		modelo.setClaimSujeto(proveedor.getClaimSujeto());
		modelo.setClaimEmail(proveedor.getClaimEmail());
		modelo.setClaimNombre(proveedor.getClaimNombre());
		modelo.setPermitirJit(proveedor.isPermitirJit());
		modelo.setPermitirVinculoPorEmail(proveedor.isPermitirVinculoPorEmail());
		modelo.setVerificaEmail(proveedor.isVerificaEmail());
		modelo.setCodigoRolPorDefecto(proveedor.getCodigoRolPorDefecto());
		modelo.setDominiosPermitidos(proveedor.getDominiosPermitidos());
		modelo.setOrigenesEmbedPermitidos(proveedor.getOrigenesEmbedPermitidos());
		modelo.setSegundosVigenciaCodigo(proveedor.getSegundosVigenciaCodigo());
		modelo.setClienteId(proveedor.getClienteId());
		modelo.setUrlAutorizacion(proveedor.getUrlAutorizacion());
		modelo.setUrlToken(proveedor.getUrlToken());
		modelo.setAlcances(proveedor.getAlcances());
		modelo.setOauthListo(oauthCompleto(proveedor));
		modelo.setActivo(proveedor.isActivo());
		modelo.setAlta(proveedor.getAlta());
		return modelo;
	}

	private void configurarOauth(ProveedorIdentidad proveedor, NuevoProveedorIdentidadReqModel datos) {
		boolean algunCampo = datos.getClienteId() != null || datos.getClienteSecreto() != null
				|| datos.getUrlAutorizacion() != null || datos.getUrlToken() != null
				|| datos.getAlcances() != null;
		if (!algunCampo) {
			return;
		}
		if (esVacio(datos.getClienteId()) || esVacio(datos.getClienteSecreto())
				|| esVacio(datos.getUrlAutorizacion()) || esVacio(datos.getUrlToken())) {
			throw new ValidacionException(
					"Para habilitar el login social hacen falta clienteId, clienteSecreto,"
							+ " urlAutorizacion y urlToken juntos");
		}
		exigirHttps(datos.getUrlAutorizacion(), "urlAutorizacion");
		exigirHttps(datos.getUrlToken(), "urlToken");
		proveedor.setClienteId(datos.getClienteId().trim());
		proveedor.setClienteSecreto(datos.getClienteSecreto().trim());
		proveedor.setUrlAutorizacion(datos.getUrlAutorizacion().trim());
		proveedor.setUrlToken(datos.getUrlToken().trim());
		proveedor.setAlcances(datos.getAlcances());
	}

	public static boolean oauthCompleto(ProveedorIdentidad proveedor) {
		return proveedor.getClienteId() != null && proveedor.getClienteSecreto() != null
				&& proveedor.getUrlAutorizacion() != null && proveedor.getUrlToken() != null
				&& !proveedor.getClienteId().isBlank() && !proveedor.getUrlAutorizacion().isBlank()
				&& !proveedor.getUrlToken().isBlank();
	}

	private boolean esVacio(String valor) {
		return valor == null || valor.isBlank();
	}

	private String normalizarDominios(String dominios) {
		if (dominios == null || dominios.isBlank()) {
			return dominios;
		}
		return Arrays.stream(dominios.split(",")).map(String::trim).filter(texto -> !texto.isEmpty())
				.map(dominio -> dominio.startsWith("@") ? dominio : "@" + dominio)
				.collect(Collectors.joining(","));
	}

	private void exigirHttps(String url, String campo) {
		String limpia = url == null ? "" : url.trim().toLowerCase(java.util.Locale.ROOT);
		if (limpia.startsWith("https://") || !propiedades.isExigirEmisorSeguro()) {
			return;
		}
		if (limpia.startsWith("http://localhost") || limpia.startsWith("http://127.0.0.1")) {
			return;
		}
		throw new ValidacionException("El " + campo + " debe ser https. Un IdP alcanzado por red privada"
				+ " puede ir por http, pero hay que aceptarlo a proposito con"
				+ " NEXTDOCS_FEDERACION_EXIGIR_HTTPS=false");
	}

	private String porDefecto(String valor, String alternativa) {
		return valor == null || valor.isBlank() ? alternativa : valor.trim();
	}
}
