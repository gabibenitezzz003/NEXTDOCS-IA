package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.nextdocs.ai.convertidores.AdministracionConverter;
import com.nextdocs.ai.entidades.Rol;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.EstadoTenant;
import com.nextdocs.ai.enumeraciones.EstadoUsuario;
import com.nextdocs.ai.enumeraciones.OrigenIdentidad;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.RegistroExistenteException;
import com.nextdocs.ai.modelos.TenantModel;
import com.nextdocs.ai.modelos.TenantReqModel;
import com.nextdocs.ai.repositorios.ArchivoDocumentoRepository;
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

	private final ArchivoDocumentoRepository archivoDocumentoRepository;

	private final PasswordEncoder codificadorClave;

	private final AuditoriaService auditoriaService;

	private final SembradorCatalogoService sembradorCatalogoService;

	private final AdministracionConverter administracionConverter;

	public TenantService(TenantRepository tenantRepository, RolRepository rolRepository,
			UsuarioRepository usuarioRepository, ArchivoDocumentoRepository archivoDocumentoRepository,
			PasswordEncoder codificadorClave, AuditoriaService auditoriaService,
			SembradorCatalogoService sembradorCatalogoService,
			AdministracionConverter administracionConverter) {
		this.tenantRepository = tenantRepository;
		this.rolRepository = rolRepository;
		this.usuarioRepository = usuarioRepository;
		this.archivoDocumentoRepository = archivoDocumentoRepository;
		this.codificadorClave = codificadorClave;
		this.auditoriaService = auditoriaService;
		this.sembradorCatalogoService = sembradorCatalogoService;
		this.administracionConverter = administracionConverter;
	}

	@Transactional
	public Tenant crear(String codigo, String nombre, String emailAdministrador, String claveAdministrador) {
		return crear(codigo, nombre, emailAdministrador, claveAdministrador, null);
	}

	@Transactional
	public Tenant crear(String codigo, String nombre, String emailAdministrador, String claveAdministrador,
			String idTenant) {
		Tenant tenant = crearSinAdministrador(codigo, nombre, idTenant);
		crearAdministrador(tenant, rolRepository.buscarPorCodigo(tenant.getId(),
				Permiso.CODIGO_ROL_ADMINISTRADOR).orElseThrow(), emailAdministrador, claveAdministrador);
		return tenant;
	}

	@Transactional
	public Tenant crearSinAdministrador(String codigo, String nombre) {
		return crearSinAdministrador(codigo, nombre, null);
	}

	@Transactional
	public Tenant crearSinAdministrador(String codigo, String nombre, String idTenant) {
		if (tenantRepository.findByCodigoAndBajaIsNull(codigo).isPresent()) {
			throw new RegistroExistenteException("Ya existe un tenant con el codigo " + codigo);
		}
		Tenant tenant = new Tenant();
		if (idTenant != null && !idTenant.isBlank()) {
			tenant.setId(idTenant);
		}
		tenant.setCodigo(codigo);
		tenant.setNombre(nombre);
		tenant.setEstado(EstadoTenant.ACTIVO);
		tenant.setAlta(Instant.now());
		tenantRepository.save(tenant);

		crearRolesPredefinidos(tenant);
		sembradorCatalogoService.sembrarSiCorresponde(tenant);
		auditoriaService.registrar(tenant.getId(), AccionAuditoria.TENANT_CREADO, ENTIDAD, tenant.getId());
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

	@Transactional(readOnly = true)
	public TenantModel obtener(String tenantId) {
		return administracionConverter.aModelo(buscarEntidad(tenantId),
				archivoDocumentoRepository.sumarTamanoPorTenant(tenantId),
				usuarioRepository.contarPorEstado(tenantId, EstadoUsuario.ACTIVO));
	}

	@Transactional
	public TenantModel actualizar(String tenantId, TenantReqModel datos) {
		Tenant tenant = buscarEntidad(tenantId);
		Map<String, Object> antes = instantanea(tenant);
		tenant.setNombre(datos.getNombre().trim());
		tenant.setPlan(datos.getPlan());
		tenant.setRegion(datos.getRegion());
		tenant.setDominio(datos.getDominio());
		tenant.setCuotaAlmacenamientoBytes(datos.getCuotaAlmacenamientoBytes());
		tenantRepository.save(tenant);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.TENANT_MODIFICADO, ENTIDAD, tenantId,
				Map.of("antes", antes, "despues", instantanea(tenant)));
		return obtener(tenantId);
	}

	private Map<String, Object> instantanea(Tenant tenant) {
		Map<String, Object> valores = new LinkedHashMap<>();
		valores.put("nombre", String.valueOf(tenant.getNombre()));
		valores.put("plan", String.valueOf(tenant.getPlan()));
		valores.put("region", String.valueOf(tenant.getRegion()));
		valores.put("dominio", String.valueOf(tenant.getDominio()));
		valores.put("cuotaAlmacenamientoBytes", tenant.getCuotaAlmacenamientoBytes());
		return valores;
	}

	private List<Rol> crearRolesPredefinidos(Tenant tenant) {
		Rol administrador = construirRol(tenant, Permiso.CODIGO_ROL_ADMINISTRADOR, "Administrador",
				Permiso.deAdministrador());
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
