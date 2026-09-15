package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import com.nextdocs.ai.entidades.Rol;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.EstadoTenant;
import com.nextdocs.ai.enumeraciones.EstadoUsuario;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.NoAutorizadoException;
import com.nextdocs.ai.modelos.AutenticacionReqModel;
import com.nextdocs.ai.modelos.SesionResModel;
import com.nextdocs.ai.repositorios.TenantRepository;
import com.nextdocs.ai.repositorios.UsuarioRepository;
import com.nextdocs.ai.servicios.jwt.TokenService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacionService {

	public static final String ENTIDAD = "Usuario";

	private static final String MENSAJE_CREDENCIALES = "Las credenciales no son validas";

	private final UsuarioRepository usuarioRepository;

	private final TenantRepository tenantRepository;

	private final TokenService tokenService;

	private final PasswordEncoder codificadorClave;

	private final AuditoriaService auditoriaService;

	public AutenticacionService(UsuarioRepository usuarioRepository, TenantRepository tenantRepository,
			TokenService tokenService, PasswordEncoder codificadorClave, AuditoriaService auditoriaService) {
		this.usuarioRepository = usuarioRepository;
		this.tenantRepository = tenantRepository;
		this.tokenService = tokenService;
		this.codificadorClave = codificadorClave;
		this.auditoriaService = auditoriaService;
	}

	@Transactional
	public SesionResModel autenticar(AutenticacionReqModel datos) {
		Tenant tenant = tenantRepository.findByCodigoAndBajaIsNull(datos.getCodigoTenant())
				.orElseThrow(() -> new NoAutorizadoException(MENSAJE_CREDENCIALES));
		if (tenant.getEstado() != EstadoTenant.ACTIVO) {
			throw new NoAutorizadoException("El tenant no esta activo");
		}
		Usuario usuario = usuarioRepository.buscarPorEmail(tenant.getId(), datos.getEmail())
				.orElseThrow(() -> new NoAutorizadoException(MENSAJE_CREDENCIALES));
		if (usuario.getEstado() != EstadoUsuario.ACTIVO || usuario.getClaveHash() == null
				|| !codificadorClave.matches(datos.getClave(), usuario.getClaveHash())) {
			auditoriaService.registrarFallo(tenant.getId(), AccionAuditoria.ACCESO_DENEGADO, ENTIDAD, usuario.getId(),
					java.util.Map.of("email", datos.getEmail()));
			throw new NoAutorizadoException(MENSAJE_CREDENCIALES);
		}
		usuario.setUltimoAcceso(Instant.now());
		usuarioRepository.save(usuario);
		auditoriaService.registrar(tenant.getId(), AccionAuditoria.USUARIO_AUTENTICADO, ENTIDAD, usuario.getId());
		return construirSesion(usuario);
	}

	@Transactional
	public SesionResModel refrescar(String tokenRefresco) {
		String usuarioId = tokenService.leerSujetoRefresco(tokenRefresco);
		Usuario usuario = usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(ENTIDAD, usuarioId));
		if (usuario.getEstado() != EstadoUsuario.ACTIVO || usuario.getBaja() != null) {
			throw new NoAutorizadoException("El usuario no esta habilitado");
		}
		return construirSesion(usuario);
	}

	public SesionResModel sesionDe(Usuario usuario) {
		return construirSesion(usuario);
	}

	private SesionResModel construirSesion(Usuario usuario) {
		Set<String> permisos = new LinkedHashSet<>();
		for (Rol rol : usuario.getRoles()) {
			permisos.addAll(rol.getPermisos());
		}
		SesionResModel sesion = new SesionResModel();
		sesion.setTokenAcceso(tokenService.generarAcceso(usuario));
		sesion.setTokenRefresco(tokenService.generarRefresco(usuario));
		sesion.setUsuarioId(usuario.getId());
		sesion.setEmail(usuario.getEmail());
		sesion.setNombre(usuario.getNombre());
		sesion.setTenantId(usuario.getTenant().getId());
		sesion.setCodigoTenant(usuario.getTenant().getCodigo());
		sesion.setNombreTenant(usuario.getTenant().getNombre());
		sesion.setIdioma(usuario.getIdioma());
		sesion.getPermisos().addAll(permisos);
		return sesion;
	}
}
