package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.nextdocs.ai.convertidores.AdministracionConverter;
import com.nextdocs.ai.entidades.Rol;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.EstadoUsuario;
import com.nextdocs.ai.enumeraciones.OrigenIdentidad;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.RegistroExistenteException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.CambioClaveReqModel;
import com.nextdocs.ai.modelos.UsuarioModel;
import com.nextdocs.ai.modelos.UsuarioReqModel;
import com.nextdocs.ai.repositorios.RolRepository;
import com.nextdocs.ai.repositorios.UsuarioRepository;
import com.nextdocs.ai.utiles.Permiso;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

	public static final String ENTIDAD = "Usuario";

	public static final int LONGITUD_MINIMA_CLAVE = 10;

	private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

	private final UsuarioRepository usuarioRepository;

	private final RolRepository rolRepository;

	private final PasswordEncoder codificadorClave;

	private final AuditoriaService auditoriaService;

	private final AdministracionConverter administracionConverter;

	public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
			PasswordEncoder codificadorClave, AuditoriaService auditoriaService,
			AdministracionConverter administracionConverter) {
		this.usuarioRepository = usuarioRepository;
		this.rolRepository = rolRepository;
		this.codificadorClave = codificadorClave;
		this.auditoriaService = auditoriaService;
		this.administracionConverter = administracionConverter;
	}

	@Transactional(readOnly = true)
	public Page<UsuarioModel> listar(String tenantId, EstadoUsuario estado, String texto, Pageable paginado) {
		return usuarioRepository.listarFiltrado(tenantId, estado, texto, paginado)
				.map(administracionConverter::aModelo);
	}

	@Transactional(readOnly = true)
	public UsuarioModel obtener(String tenantId, String usuarioId) {
		return administracionConverter.aModelo(buscarEntidad(tenantId, usuarioId));
	}

	@Transactional
	public UsuarioModel crear(Tenant tenant, UsuarioReqModel datos) {
		String email = normalizar(datos.getEmail());
		usuarioRepository.buscarPorEmail(tenant.getId(), email).ifPresent(existente -> {
			throw new RegistroExistenteException("Ya existe un usuario con el email " + email);
		});
		validarClave(datos.getClave());
		Usuario usuario = new Usuario();
		usuario.setTenant(tenant);
		usuario.setEmail(email);
		usuario.setNombre(datos.getNombre().trim());
		usuario.setClaveHash(codificadorClave.encode(datos.getClave()));
		usuario.setEstado(EstadoUsuario.ACTIVO);
		usuario.setOrigenIdentidad(OrigenIdentidad.LOCAL);
		usuario.setIdioma(datos.getIdioma() == null ? "es" : datos.getIdioma());
		usuario.setZonaHoraria(datos.getZonaHoraria() == null ? "America/Argentina/Buenos_Aires"
				: datos.getZonaHoraria());
		usuario.setRoles(resolverRoles(tenant.getId(), datos.getRoles()));
		usuario.setAlta(Instant.now());
		usuarioRepository.save(usuario);
		auditoriaService.registrarConDetalle(tenant.getId(), AccionAuditoria.USUARIO_CREADO, ENTIDAD,
				usuario.getId(), Map.of("email", email, "roles", datos.getRoles()));
		log.info("Alta del usuario {} en el tenant {}", email, tenant.getCodigo());
		return administracionConverter.aModelo(usuario);
	}

	@Transactional
	public UsuarioModel actualizar(String tenantId, String usuarioId, UsuarioReqModel datos) {
		Usuario usuario = buscarEntidad(tenantId, usuarioId);
		String email = normalizar(datos.getEmail());
		if (!usuario.getEmail().equalsIgnoreCase(email)) {
			usuarioRepository.buscarPorEmail(tenantId, email).ifPresent(existente -> {
				throw new RegistroExistenteException("Ya existe un usuario con el email " + email);
			});
			usuario.setEmail(email);
		}
		usuario.setNombre(datos.getNombre().trim());
		if (datos.getIdioma() != null) {
			usuario.setIdioma(datos.getIdioma());
		}
		if (datos.getZonaHoraria() != null) {
			usuario.setZonaHoraria(datos.getZonaHoraria());
		}
		usuarioRepository.save(usuario);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.USUARIO_MODIFICADO, ENTIDAD, usuarioId,
				Map.of("email", email, "nombre", usuario.getNombre()));
		return administracionConverter.aModelo(usuario);
	}

	@Transactional
	public UsuarioModel asignarRoles(String tenantId, String usuarioId, List<String> codigos) {
		Usuario usuario = buscarEntidad(tenantId, usuarioId);
		List<String> anteriores = usuario.getRoles().stream().map(Rol::getCodigo).sorted().toList();
		Set<Rol> nuevos = resolverRoles(tenantId, codigos);
		validarQuedanAdministradores(usuario, usuario.getEstado() == EstadoUsuario.ACTIVO && esAdministrador(nuevos));
		usuario.setRoles(nuevos);
		usuarioRepository.save(usuario);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.USUARIO_MODIFICADO, ENTIDAD, usuarioId,
				Map.of("rolesAntes", anteriores, "rolesDespues", codigos));
		return administracionConverter.aModelo(usuario);
	}

	@Transactional
	public UsuarioModel bloquear(String tenantId, String usuarioId, Usuario actor, String motivo) {
		Usuario usuario = buscarEntidad(tenantId, usuarioId);
		if (actor != null && actor.getId().equals(usuarioId)) {
			throw new ValidacionException("Un usuario no puede bloquearse a si mismo");
		}
		if (usuario.getEstado() == EstadoUsuario.BLOQUEADO) {
			throw new ValidacionException("El usuario ya esta bloqueado");
		}
		validarQuedanAdministradores(usuario, false);
		usuario.setEstado(EstadoUsuario.BLOQUEADO);
		usuarioRepository.save(usuario);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.USUARIO_BLOQUEADO, ENTIDAD, usuarioId,
				Map.of("email", usuario.getEmail(), "motivo", motivo == null ? "" : motivo));
		log.info("El usuario {} queda bloqueado", usuario.getEmail());
		return administracionConverter.aModelo(usuario);
	}

	@Transactional
	public UsuarioModel desbloquear(String tenantId, String usuarioId) {
		Usuario usuario = buscarEntidad(tenantId, usuarioId);
		if (usuario.getEstado() == EstadoUsuario.ACTIVO) {
			throw new ValidacionException("El usuario ya esta activo");
		}
		usuario.setEstado(EstadoUsuario.ACTIVO);
		usuarioRepository.save(usuario);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.USUARIO_DESBLOQUEADO, ENTIDAD, usuarioId,
				Map.of("email", usuario.getEmail()));
		return administracionConverter.aModelo(usuario);
	}

	@Transactional
	public void eliminar(String tenantId, String usuarioId, Usuario actor) {
		Usuario usuario = buscarEntidad(tenantId, usuarioId);
		if (actor != null && actor.getId().equals(usuarioId)) {
			throw new ValidacionException("Un usuario no puede eliminarse a si mismo");
		}
		validarQuedanAdministradores(usuario, false);
		usuario.setEstado(EstadoUsuario.BAJA);
		usuario.setBaja(Instant.now());
		usuarioRepository.save(usuario);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.USUARIO_ELIMINADO, ENTIDAD, usuarioId,
				Map.of("email", usuario.getEmail()));
		log.info("Baja del usuario {}", usuario.getEmail());
	}

	@Transactional
	public void cambiarClave(String tenantId, Usuario actor, CambioClaveReqModel datos) {
		Usuario usuario = buscarEntidad(tenantId, actor.getId());
		if (usuario.getClaveHash() == null
				|| !codificadorClave.matches(datos.getClaveActual(), usuario.getClaveHash())) {
			auditoriaService.registrarFallo(tenantId, AccionAuditoria.CLAVE_MODIFICADA, ENTIDAD, usuario.getId(),
					Map.of("motivo", "La clave actual no coincide"));
			throw new ValidacionException("La clave actual no es correcta");
		}
		validarClave(datos.getClaveNueva());
		if (codificadorClave.matches(datos.getClaveNueva(), usuario.getClaveHash())) {
			throw new ValidacionException("La clave nueva debe ser distinta de la actual");
		}
		usuario.setClaveHash(codificadorClave.encode(datos.getClaveNueva()));
		usuarioRepository.save(usuario);
		auditoriaService.registrar(tenantId, AccionAuditoria.CLAVE_MODIFICADA, ENTIDAD, usuario.getId());
	}

	@Transactional
	public void restablecerClave(String tenantId, String usuarioId, String claveNueva, Usuario actor) {
		Usuario usuario = buscarEntidad(tenantId, usuarioId);
		validarClave(claveNueva);
		usuario.setClaveHash(codificadorClave.encode(claveNueva));
		usuarioRepository.save(usuario);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.CLAVE_RESTABLECIDA, ENTIDAD, usuarioId,
				Map.of("email", usuario.getEmail(), "actor", actor == null ? "sistema" : actor.getEmail()));
		log.info("La clave del usuario {} fue restablecida por un administrador", usuario.getEmail());
	}

	@Transactional(readOnly = true)
	public Usuario buscarEntidad(String tenantId, String usuarioId) {
		return usuarioRepository.buscarPorIdYTenant(usuarioId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(ENTIDAD, usuarioId));
	}

	private Set<Rol> resolverRoles(String tenantId, List<String> codigos) {
		if (codigos == null || codigos.isEmpty()) {
			throw new ValidacionException("El usuario necesita al menos un rol");
		}
		Set<Rol> roles = new LinkedHashSet<>();
		for (String codigo : codigos) {
			roles.add(rolRepository.buscarPorCodigo(tenantId, codigo)
					.orElseThrow(() -> new ValidacionException("El rol " + codigo + " no existe en este tenant")));
		}
		return roles;
	}

	private void validarQuedanAdministradores(Usuario usuario, boolean seguiraSiendoAdministrador) {
		if (seguiraSiendoAdministrador || usuario.getEstado() != EstadoUsuario.ACTIVO
				|| !esAdministrador(usuario.getRoles())) {
			return;
		}
		long administradores = usuarioRepository.contarConPermiso(usuario.getTenant().getId(), EstadoUsuario.ACTIVO,
				Permiso.TENANT_ADMINISTRAR);
		if (administradores <= 1) {
			throw new ValidacionException(
					"El tenant quedaria sin ningun administrador activo. Designa otro antes de continuar");
		}
	}

	private boolean esAdministrador(Set<Rol> roles) {
		return roles.stream().anyMatch(rol -> rol.getPermisos().contains(Permiso.TENANT_ADMINISTRAR));
	}

	private void validarClave(String clave) {
		if (clave == null || clave.trim().length() < LONGITUD_MINIMA_CLAVE) {
			throw new ValidacionException("La clave debe tener al menos " + LONGITUD_MINIMA_CLAVE + " caracteres");
		}
	}

	private String normalizar(String email) {
		return email.trim().toLowerCase();
	}
}
