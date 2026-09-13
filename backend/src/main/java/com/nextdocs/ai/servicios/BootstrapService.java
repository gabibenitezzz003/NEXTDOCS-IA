package com.nextdocs.ai.servicios;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import com.nextdocs.ai.config.PropiedadesBootstrap;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.enumeraciones.EstadoTenant;
import com.nextdocs.ai.exceptions.NoAutorizadoException;
import com.nextdocs.ai.modelos.TenantModel;
import com.nextdocs.ai.repositorios.TenantRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BootstrapService {

	private final TenantRepository tenantRepository;

	private final TenantService tenantService;

	private final PropiedadesBootstrap propiedades;

	public BootstrapService(TenantRepository tenantRepository, TenantService tenantService,
			PropiedadesBootstrap propiedades) {
		this.tenantRepository = tenantRepository;
		this.tenantService = tenantService;
		this.propiedades = propiedades;
	}

	@Transactional
	public TenantModel crearTenant(String secreto, String codigoTenant, String nombreTenant,
			String emailAdministrador, String claveAdministrador) {
		if (!propiedades.isHabilitado()) {
			throw new NoAutorizadoException("Bootstrap no habilitado");
		}
		if (!secretoValido(secreto)) {
			throw new NoAutorizadoException("Secreto de bootstrap invalido");
		}
		if (!tenantRepository.findByEstadoAndBajaIsNull(EstadoTenant.ACTIVO).isEmpty()) {
			throw new NoAutorizadoException("El bootstrap ya fue utilizado");
		}
		if (tenantRepository.findByCodigoAndBajaIsNull(codigoTenant).isPresent()) {
			throw new NoAutorizadoException("El codigo de tenant ya existe");
		}
		Tenant tenant = tenantService.crear(codigoTenant, nombreTenant, emailAdministrador, claveAdministrador);
		return tenantService.obtener(tenant.getId());
	}

	private boolean secretoValido(String secreto) {
		if (secreto == null || propiedades.getSecreto() == null) {
			return false;
		}
		return MessageDigest.isEqual(secreto.getBytes(StandardCharsets.UTF_8),
				propiedades.getSecreto().getBytes(StandardCharsets.UTF_8));
	}
}
