package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.nextdocs.ai.convertidores.AdministracionConverter;
import com.nextdocs.ai.entidades.Rol;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.RegistroExistenteException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.RolModel;
import com.nextdocs.ai.modelos.RolReqModel;
import com.nextdocs.ai.repositorios.RolRepository;
import com.nextdocs.ai.repositorios.UsuarioRepository;
import com.nextdocs.ai.utiles.Permiso;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RolService {

	public static final String ENTIDAD = "Rol";

	private static final Logger log = LoggerFactory.getLogger(RolService.class);

	private final RolRepository rolRepository;

	private final UsuarioRepository usuarioRepository;

	private final AuditoriaService auditoriaService;

	private final AdministracionConverter administracionConverter;

	public RolService(RolRepository rolRepository, UsuarioRepository usuarioRepository,
			AuditoriaService auditoriaService, AdministracionConverter administracionConverter) {
		this.rolRepository = rolRepository;
		this.usuarioRepository = usuarioRepository;
		this.auditoriaService = auditoriaService;
		this.administracionConverter = administracionConverter;
	}

	@Transactional(readOnly = true)
	public List<RolModel> listar(String tenantId) {
		List<RolModel> modelos = new ArrayList<>();
		for (Rol rol : rolRepository.listarPorTenant(tenantId)) {
			modelos.add(administracionConverter.aModelo(rol, usuarioRepository.contarPorRol(rol.getId())));
		}
		return modelos;
	}

	@Transactional(readOnly = true)
	public RolModel obtener(String tenantId, String rolId) {
		Rol rol = buscarEntidad(tenantId, rolId);
		return administracionConverter.aModelo(rol, usuarioRepository.contarPorRol(rolId));
	}

	@Transactional
	public RolModel crear(Tenant tenant, RolReqModel datos) {
		rolRepository.buscarPorCodigo(tenant.getId(), datos.getCodigo()).ifPresent(existente -> {
			throw new RegistroExistenteException("Ya existe un rol con el codigo " + datos.getCodigo());
		});
		Rol rol = new Rol();
		rol.setTenant(tenant);
		rol.setCodigo(datos.getCodigo());
		rol.setNombre(datos.getNombre().trim());
		rol.setDescripcion(datos.getDescripcion());
		rol.setPredefinido(false);
		rol.setPermisos(validarPermisos(datos.getPermisos()));
		rol.setAlta(Instant.now());
		rolRepository.save(rol);
		auditoriaService.registrarConDetalle(tenant.getId(), AccionAuditoria.ROL_CREADO, ENTIDAD, rol.getId(),
				Map.of("codigo", rol.getCodigo(), "permisos", new ArrayList<>(rol.getPermisos())));
		log.info("Alta del rol {} en el tenant {}", rol.getCodigo(), tenant.getCodigo());
		return administracionConverter.aModelo(rol, 0);
	}

	@Transactional
	public RolModel actualizar(String tenantId, String rolId, RolReqModel datos) {
		Rol rol = buscarEntidad(tenantId, rolId);
		exigirModificable(rol);
		List<String> anteriores = new ArrayList<>(new TreeSet<>(rol.getPermisos()));
		rol.setNombre(datos.getNombre().trim());
		rol.setDescripcion(datos.getDescripcion());
		rol.setPermisos(validarPermisos(datos.getPermisos()));
		rolRepository.save(rol);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.ROL_MODIFICADO, ENTIDAD, rolId,
				Map.of("codigo", rol.getCodigo(), "permisosAntes", anteriores, "permisosDespues",
						new ArrayList<>(new TreeSet<>(rol.getPermisos()))));
		return administracionConverter.aModelo(rol, usuarioRepository.contarPorRol(rolId));
	}

	@Transactional
	public void eliminar(String tenantId, String rolId) {
		Rol rol = buscarEntidad(tenantId, rolId);
		exigirModificable(rol);
		long asignados = usuarioRepository.contarPorRol(rolId);
		if (asignados > 0) {
			throw new ValidacionException(
					"El rol " + rol.getCodigo() + " esta asignado a " + asignados + " usuarios");
		}
		rol.setBaja(Instant.now());
		rolRepository.save(rol);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.ROL_ELIMINADO, ENTIDAD, rolId,
				Map.of("codigo", rol.getCodigo()));
	}

	@Transactional(readOnly = true)
	public Rol buscarEntidad(String tenantId, String rolId) {
		return rolRepository.buscarPorIdYTenant(rolId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(ENTIDAD, rolId));
	}

	private void exigirModificable(Rol rol) {
		if (rol.isPredefinido()) {
			throw new ValidacionException("El rol predefinido " + rol.getCodigo()
					+ " no se modifica ni se elimina. Crea un rol propio a partir de sus permisos");
		}
	}

	private Set<String> validarPermisos(List<String> permisos) {
		Set<String> validos = Permiso.todos();
		Set<String> resultado = new LinkedHashSet<>();
		for (String permiso : permisos) {
			String limpio = permiso == null ? "" : permiso.trim();
			if (!validos.contains(limpio)) {
				throw new ValidacionException("El permiso " + permiso + " no existe");
			}
			if (Permiso.INTEGRACIONES_ESCRIBIR.equals(limpio)) {
				throw new ValidacionException("El permiso " + limpio
						+ " es un alcance de cuentas de servicio y no se asigna a roles");
			}
			resultado.add(limpio);
		}
		if (resultado.isEmpty()) {
			throw new ValidacionException("El rol necesita al menos un permiso");
		}
		return resultado;
	}
}
