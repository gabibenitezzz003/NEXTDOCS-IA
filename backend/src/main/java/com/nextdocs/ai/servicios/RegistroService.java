package com.nextdocs.ai.servicios;

import java.util.Locale;

import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.RegistroOrganizacionReqModel;
import com.nextdocs.ai.modelos.SesionResModel;
import com.nextdocs.ai.repositorios.TenantRepository;
import com.nextdocs.ai.repositorios.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistroService {

	private final TenantRepository tenantRepository;

	private final UsuarioRepository usuarioRepository;

	private final TenantService tenantService;

	private final AutenticacionService autenticacionService;

	public RegistroService(TenantRepository tenantRepository, UsuarioRepository usuarioRepository,
			TenantService tenantService, AutenticacionService autenticacionService) {
		this.tenantRepository = tenantRepository;
		this.usuarioRepository = usuarioRepository;
		this.tenantService = tenantService;
		this.autenticacionService = autenticacionService;
	}

	@Transactional
	public SesionResModel registrar(RegistroOrganizacionReqModel datos) {
		String codigo = datos.getCodigoOrganizacion().trim().toLowerCase(Locale.ROOT);
		String email = datos.getEmailAdministrador().trim().toLowerCase(Locale.ROOT);
		if (tenantRepository.findByCodigoAndBajaIsNull(codigo).isPresent()) {
			throw new ValidacionException("El codigo de organizacion " + codigo + " ya esta en uso");
		}
		if (!usuarioRepository.buscarPorEmailEnCualquierTenant(email).isEmpty()) {
			throw new ValidacionException("Ya existe una cuenta con el email " + email);
		}
		Tenant tenant = tenantService.crear(codigo, datos.getNombreOrganizacion().trim(), email,
				datos.getClaveAdministrador());
		Usuario administrador = usuarioRepository.buscarPorEmail(tenant.getId(), email).orElseThrow();
		administrador.setNombre(datos.getNombreAdministrador().trim());
		usuarioRepository.save(administrador);
		return autenticacionService.sesionDe(administrador);
	}
}
