package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.nextdocs.ai.entidades.Rol;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.EstadoTenant;
import com.nextdocs.ai.enumeraciones.EstadoUsuario;
import com.nextdocs.ai.enumeraciones.OrigenIdentidad;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.RegistroExistenteException;
import com.nextdocs.ai.repositorios.RolRepository;
import com.nextdocs.ai.repositorios.TenantRepository;
import com.nextdocs.ai.repositorios.UsuarioRepository;
import com.nextdocs.ai.utiles.Permiso;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantService {

	public static final String ENTIDAD = "Tenant";

	private final TenantRepository tenantRepository;

	private final RolRepository rolRepository;

	private final UsuarioRepository usuarioRepository;

	private final PasswordEncoder codificadorClave;

	private final AuditoriaService auditoriaService;

	public TenantService(TenantRepository tenantRepository, RolRepository rolRepository,
			UsuarioRepository usuarioRepository, PasswordEncoder codificadorClave,
			AuditoriaService auditoriaService) {
		this.tenantRepository = tenantRepository;
		this.rolRepository = rolRepository;
		this.usuarioRepository = usuarioRepository;
		this.codificadorClave = codificadorClave;
		this.auditoriaService = auditoriaService;
	}

	@Transactional
	public Tenant crear(String codigo, String nombre, String emailAdministrador, String claveAdministrador) {
		if (tenantRepository.findByCodigoAndBajaIsNull(codigo).isPresent()) {
			throw new RegistroExistenteException("Ya existe un tenant con el codigo " + codigo);
		}
		Tenant tenant = new Tenant();
		tenant.setCodigo(codigo);
		tenant.setNombre(nombre);
		tenant.setEstado(EstadoTenant.ACTIVO);
		tenant.setAlta(Instant.now());
		tenantRepository.save(tenant);

		List<Rol> roles = crearRolesPredefinidos(tenant);
		crearAdministrador(tenant, roles.get(0), emailAdministrador, claveAdministrador);
		auditoriaService.registrar(tenant.getId(), AccionAuditoria.POLITICA_MODIFICADA, ENTIDAD, tenant.getId());
		return tenant;
	}

	@Transactional(readOnly = true)
	public Tenant buscarEntidad(String tenantId) {
		return tenantRepository.findById(tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(ENTIDAD, tenantId));
	}

	@Transactional(readOnly = true)
	public List<Tenant> listarActivos() {
		return tenantRepository.findByEstadoAndBajaIsNull(EstadoTenant.ACTIVO);
	}

	private List<Rol> crearRolesPredefinidos(Tenant tenant) {
		Rol administrador = construirRol(tenant, Permiso.CODIGO_ROL_ADMINISTRADOR, "Administrador",
				Permiso.todos());
		Rol operador = construirRol(tenant, Permiso.CODIGO_ROL_OPERADOR, "Operador", Permiso.deOperador());
		Rol revisor = construirRol(tenant, Permiso.CODIGO_ROL_REVISOR, "Revisor", Permiso.deRevisor());
		Rol auditor = construirRol(tenant, Permiso.CODIGO_ROL_AUDITOR, "Auditor", Permiso.deAuditor());
		return rolRepository.saveAll(List.of(administrador, operador, revisor, auditor));
	}

	private Rol construirRol(Tenant tenant, String codigo, String nombre, Set<String> permisos) {
		Rol rol = new Rol();
		rol.setTenant(tenant);
		rol.setCodigo(codigo);
		rol.setNombre(nombre);
		rol.setPredefinido(true);
		rol.setPermisos(new LinkedHashSet<>(permisos));
		rol.setAlta(Instant.now());
		return rol;
	}

	private void crearAdministrador(Tenant tenant, Rol rolAdministrador, String email, String clave) {
		Usuario usuario = new Usuario();
		usuario.setTenant(tenant);
		usuario.setEmail(email);
		usuario.setNombre("Administrador");
		usuario.setClaveHash(codificadorClave.encode(clave));
		usuario.setEstado(EstadoUsuario.ACTIVO);
		usuario.setOrigenIdentidad(OrigenIdentidad.LOCAL);
		usuario.setIdioma("es");
		usuario.setZonaHoraria("America/Argentina/Buenos_Aires");
		usuario.getRoles().add(rolAdministrador);
		usuario.setAlta(Instant.now());
		usuarioRepository.save(usuario);
	}
}
