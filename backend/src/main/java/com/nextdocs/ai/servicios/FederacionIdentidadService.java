package com.nextdocs.ai.servicios;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.nextdocs.ai.config.PropiedadesFederacion;
import com.nextdocs.ai.entidades.CodigoEmbed;
import com.nextdocs.ai.entidades.ProveedorIdentidad;
import com.nextdocs.ai.entidades.Rol;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.EstadoTenant;
import com.nextdocs.ai.enumeraciones.EstadoUsuario;
import com.nextdocs.ai.enumeraciones.OrigenIdentidad;
import com.nextdocs.ai.exceptions.NoAutorizadoException;
import com.nextdocs.ai.exceptions.ProhibidoException;
import com.nextdocs.ai.modelos.CodigoEmbedModel;
import com.nextdocs.ai.modelos.IntercambioFederadoReqModel;
import com.nextdocs.ai.modelos.SesionResModel;
import com.nextdocs.ai.repositorios.CodigoEmbedRepository;
import com.nextdocs.ai.repositorios.ProveedorIdentidadRepository;
import com.nextdocs.ai.repositorios.RolRepository;
import com.nextdocs.ai.repositorios.UsuarioRepository;
import com.nextdocs.ai.utiles.ContextoCorrelacion;
import com.nextdocs.ai.utiles.Hash;
import com.nextdocs.ai.utiles.Permiso;

import io.jsonwebtoken.Claims;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FederacionIdentidadService {

	public static final String ENTIDAD = "IdentidadFederada";

	private static final Logger log = LoggerFactory.getLogger(FederacionIdentidadService.class);

	private static final SecureRandom AZAR = new SecureRandom();

	private static final int BYTES_CODIGO = 32;

	private final ProveedorIdentidadRepository proveedorIdentidadRepository;

	private final CodigoEmbedRepository codigoEmbedRepository;

	private final UsuarioRepository usuarioRepository;

	private final RolRepository rolRepository;

	private final VerificadorTokenIdpService verificadorTokenIdpService;

	private final AutenticacionService autenticacionService;

	private final AuditoriaService auditoriaService;

	private final TenantService tenantService;

	private final PropiedadesFederacion propiedades;

	public FederacionIdentidadService(ProveedorIdentidadRepository proveedorIdentidadRepository,
			CodigoEmbedRepository codigoEmbedRepository, UsuarioRepository usuarioRepository,
			RolRepository rolRepository, VerificadorTokenIdpService verificadorTokenIdpService,
			AutenticacionService autenticacionService, AuditoriaService auditoriaService,
			TenantService tenantService, PropiedadesFederacion propiedades) {
		this.proveedorIdentidadRepository = proveedorIdentidadRepository;
		this.codigoEmbedRepository = codigoEmbedRepository;
		this.usuarioRepository = usuarioRepository;
		this.rolRepository = rolRepository;
		this.verificadorTokenIdpService = verificadorTokenIdpService;
		this.autenticacionService = autenticacionService;
		this.auditoriaService = auditoriaService;
		this.tenantService = tenantService;
		this.propiedades = propiedades;
	}

	@Transactional
	public CodigoEmbedModel intercambiar(IntercambioFederadoReqModel datos) {
		ProveedorIdentidad proveedor = proveedorIdentidadRepository
				.buscarPorCodigoTenantYCodigo(datos.getCodigoTenant(), datos.getProveedor())
				.filter(ProveedorIdentidad::isActivo)
				.orElseThrow(() -> new NoAutorizadoException(
						"No hay un proveedor de identidad activo llamado " + datos.getProveedor()));
		Tenant tenant = proveedor.getTenant();
		if (tenant.getEstado() != EstadoTenant.ACTIVO || tenant.getBaja() != null) {
			throw new NoAutorizadoException("El tenant no esta activo");
		}

		Claims claims = verificadorTokenIdpService.verificar(proveedor, datos.getToken());
		String sujetoExterno = textoDe(claims, proveedor.getClaimSujeto());
		if (sujetoExterno == null) {
			throw new NoAutorizadoException(
					"El token no trae el claim " + proveedor.getClaimSujeto() + " que identifica al usuario");
		}
		String email = normalizar(textoDe(claims, proveedor.getClaimEmail()));
		String nombre = textoDe(claims, proveedor.getClaimNombre());
		String urlRetorno = validarRetorno(proveedor, datos.getUrlRetorno());

		boolean aprovisionado = false;
		boolean global = proveedor.isVerificaEmail();
		Usuario usuario = global
				? usuarioRepository
						.buscarPorIdentidadExternaGlobal(proveedor.getOrigen(), sujetoExterno).orElse(null)
				: usuarioRepository
						.buscarPorIdentidadExterna(tenant.getId(), proveedor.getOrigen(), sujetoExterno)
						.orElse(null);
		if (usuario == null) {
			usuario = resolverSinIdentidad(proveedor, tenant, sujetoExterno, email, nombre,
					datos.isTenantPropio(), global);
			aprovisionado = true;
		}
		exigirUsuarioHabilitado(usuario.getTenant(), proveedor, usuario, sujetoExterno);

		usuario.setUltimoAcceso(Instant.now());
		usuarioRepository.save(usuario);

		String codigo = generarCodigo();
		CodigoEmbed registro = new CodigoEmbed();
		registro.setTenant(usuario.getTenant());
		registro.setUsuario(usuario);
		registro.setProveedor(proveedor);
		registro.setCodigoHash(Hash.sha256(codigo));
		registro.setSujetoExterno(sujetoExterno);
		registro.setAplicacionOrigen(proveedor.getOrigen().name());
		registro.setTipoObjeto(datos.getTipoObjeto());
		registro.setIdObjeto(datos.getIdObjeto());
		registro.setUrlRetorno(urlRetorno);
		registro.setVenceEn(Instant.now().plusSeconds(vigenciaDe(proveedor)));
		registro.setCorrelacionId(ContextoCorrelacion.obtenerOGenerar());
		registro.setAlta(Instant.now());
		codigoEmbedRepository.save(registro);

		auditar(usuario.getTenant(), usuario, AccionAuditoria.FEDERACION_INTERCAMBIADA, proveedor,
				sujetoExterno,
				Map.of("aprovisionado", aprovisionado, "tipoObjeto", String.valueOf(datos.getTipoObjeto()),
						"idObjeto", String.valueOf(datos.getIdObjeto())));

		CodigoEmbedModel modelo = new CodigoEmbedModel();
		modelo.setCodigo(codigo);
		modelo.setVenceEn(registro.getVenceEn());
		modelo.setSegundosVigencia(vigenciaDe(proveedor));
		modelo.setUsuarioId(usuario.getId());
		modelo.setEmail(usuario.getEmail());
		modelo.setCodigoTenant(usuario.getTenant().getCodigo());
		modelo.setAprovisionado(aprovisionado);
		modelo.setAplicacionOrigen(proveedor.getOrigen().name());
		modelo.setTipoObjeto(datos.getTipoObjeto());
		modelo.setIdObjeto(datos.getIdObjeto());
		modelo.setUrlRetorno(urlRetorno);
		return modelo;
	}

	@Transactional
	public SesionResModel canjear(String codigo) {
		if (codigo == null || codigo.isBlank()) {
			throw new NoAutorizadoException("El codigo de embed es obligatorio");
		}
		CodigoEmbed registro = codigoEmbedRepository.buscarPorHash(Hash.sha256(codigo.trim()))
				.orElseThrow(() -> new NoAutorizadoException("El codigo de embed no existe"));
		if (registro.getUsadoEn() != null) {
			log.warn("Codigo de embed reutilizado para el usuario {}", registro.getUsuario().getId());
			auditoriaService.registrarFallo(registro.getUsuario().getTenant().getId(),
					AccionAuditoria.ACCESO_DENEGADO, ENTIDAD, registro.getId(),
					Map.of("motivo", "codigo de embed reutilizado"));
			throw new NoAutorizadoException("El codigo de embed ya fue usado");
		}
		if (registro.getVenceEn() == null || registro.getVenceEn().isBefore(Instant.now())) {
			throw new NoAutorizadoException("El codigo de embed vencio");
		}

		Usuario usuario = registro.getUsuario();
		Tenant tenant = usuario.getTenant();
		if (usuario.getEstado() != EstadoUsuario.ACTIVO || usuario.getBaja() != null
				|| tenant.getEstado() != EstadoTenant.ACTIVO) {
			auditoriaService.registrarFallo(tenant.getId(), AccionAuditoria.ACCESO_DENEGADO, ENTIDAD,
					usuario.getId(), Map.of("motivo", "el usuario dejo de estar habilitado"));
			throw new ProhibidoException("El usuario no esta habilitado en NEXT DOC AI");
		}

		registro.setUsadoEn(Instant.now());
		codigoEmbedRepository.save(registro);

		Map<String, Object> detalle = new LinkedHashMap<>();
		detalle.put("aplicacionOrigen", registro.getAplicacionOrigen());
		detalle.put("sujetoExterno", registro.getSujetoExterno());
		detalle.put("tipoObjeto", registro.getTipoObjeto());
		detalle.put("idObjeto", registro.getIdObjeto());
		auditoriaService.registrarConDetalle(tenant.getId(), AccionAuditoria.FEDERACION_CANJEADA, ENTIDAD,
				usuario.getId(), detalle);
		return autenticacionService.sesionDe(usuario);
	}

	@Scheduled(fixedDelayString = "${nextdocs.federacion.intervaloLimpiezaMilisegundos:900000}",
			initialDelayString = "${nextdocs.federacion.retrasoInicialMilisegundos:60000}")
	@Transactional
	public int limpiarVencidos() {
		return codigoEmbedRepository.borrarVencidosAntesDe(Instant.now().minusSeconds(3600));
	}

	private Usuario resolverSinIdentidad(ProveedorIdentidad proveedor, Tenant tenant, String sujetoExterno,
			String email, String nombre, boolean tenantPropio, boolean global) {
		if (email == null) {
			throw new ProhibidoException("El token no trae email y no se puede aprovisionar al usuario");
		}
		exigirDominioPermitido(proveedor, email);

		Optional<Usuario> porEmail = global
				? usuarioRepository.buscarPorEmailEnCualquierTenant(email).stream()
						.filter(u -> u.getBaja() == null)
						.min(Comparator.comparing(
								u -> u.getTenant().getId().equals(tenant.getId()) ? 0 : 1))
				: usuarioRepository.buscarPorEmail(tenant.getId(), email);
		if (porEmail.isPresent()) {
			Usuario existente = porEmail.get();
			if (!proveedor.isPermitirVinculoPorEmail()) {
				throw new ProhibidoException("Ya existe un usuario con el email " + email
						+ " sin identidad federada. Vinculalo a mano o habilita el vinculo por email"
						+ " en el proveedor, que confia en que el emisor valida el correo");
			}
			existente.setOrigenIdentidad(proveedor.getOrigen());
			existente.setIdUsuarioExterno(sujetoExterno);
			log.info("Usuario {} vinculado al proveedor {}", existente.getId(), proveedor.getCodigo());
			return usuarioRepository.save(existente);
		}

		if (!proveedor.isPermitirJit()) {
			throw new ProhibidoException("El proveedor " + proveedor.getCodigo()
					+ " no permite crear usuarios automaticamente. Dalo de alta antes de federarlo");
		}

		Tenant destino = tenantPropio ? crearTenantPersonal(email, nombre) : tenant;
		Rol rol = tenantPropio
				? rolRepository
						.buscarPorCodigo(destino.getId(), Permiso.CODIGO_ROL_ADMINISTRADOR).orElseThrow()
				: rolPorDefecto(proveedor, destino);

		Usuario nuevo = new Usuario();
		nuevo.setTenant(destino);
		nuevo.setEmail(email);
		nuevo.setNombre(nombre == null || nombre.isBlank() ? email : nombre);
		nuevo.setEstado(EstadoUsuario.ACTIVO);
		nuevo.setOrigenIdentidad(proveedor.getOrigen());
		nuevo.setIdUsuarioExterno(sujetoExterno);
		nuevo.setAlta(Instant.now());
		nuevo.getRoles().add(rol);
		usuarioRepository.save(nuevo);
		auditoriaService.registrarConDetalle(destino.getId(), AccionAuditoria.USUARIO_CREADO, "Usuario",
				nuevo.getId(), Map.of("origen", proveedor.getOrigen().name(), "proveedor",
						proveedor.getCodigo(), "aprovisionamiento", "JIT", "rol", rol.getCodigo(),
						"tenantPropio", tenantPropio));
		log.info("Usuario {} aprovisionado por JIT desde {} en el tenant {}", nuevo.getEmail(),
				proveedor.getCodigo(), destino.getCodigo());
		return nuevo;
	}

	private Tenant crearTenantPersonal(String email, String nombre) {
		String base = email.substring(0, email.indexOf('@')).toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
		if (base.isBlank()) {
			base = "usuario";
		}
		if (base.length() > 20) {
			base = base.substring(0, 20);
		}
		String codigo = "u-" + base + "-" + Hash.sha256(email).substring(0, 6);
		String nombreTenant = nombre == null || nombre.isBlank() ? email : nombre;
		return tenantService.crearSinAdministrador(codigo, nombreTenant);
	}

	private Rol rolPorDefecto(ProveedorIdentidad proveedor, Tenant tenant) {
		String codigo = proveedor.getCodigoRolPorDefecto();
		if (codigo == null || codigo.isBlank()) {
			throw new ProhibidoException("El proveedor " + proveedor.getCodigo()
					+ " no define un rol por defecto y no se puede aprovisionar sin permisos explicitos");
		}
		return rolRepository.buscarPorCodigo(tenant.getId(), codigo)
				.orElseThrow(() -> new ProhibidoException(
						"El rol por defecto " + codigo + " del proveedor " + proveedor.getCodigo() + " no existe"));
	}

	private void exigirUsuarioHabilitado(Tenant tenant, ProveedorIdentidad proveedor, Usuario usuario,
			String sujetoExterno) {
		if (usuario.getEstado() == EstadoUsuario.ACTIVO && usuario.getBaja() == null) {
			return;
		}
		auditar(tenant, usuario, AccionAuditoria.ACCESO_DENEGADO, proveedor, sujetoExterno,
				Map.of("motivo", "el usuario esta " + usuario.getEstado() + " en NEXT DOC AI"));
		throw new ProhibidoException(
				"El usuario esta deshabilitado en NEXT DOC AI: la sesion del host no lo habilita");
	}

	private void exigirDominioPermitido(ProveedorIdentidad proveedor, String email) {
		List<String> dominios = separar(proveedor.getDominiosPermitidos());
		if (dominios.isEmpty()) {
			return;
		}
		String dominio = email.substring(email.indexOf('@'));
		if (dominios.stream().noneMatch(permitido -> permitido.equalsIgnoreCase(dominio))) {
			throw new ProhibidoException("El dominio " + dominio + " no esta habilitado en el proveedor "
					+ proveedor.getCodigo());
		}
	}

	private String validarRetorno(ProveedorIdentidad proveedor, String urlRetorno) {
		if (urlRetorno == null || urlRetorno.isBlank()) {
			return null;
		}
		List<String> permitidos = separar(proveedor.getOrigenesEmbedPermitidos());
		if (permitidos.isEmpty()) {
			throw new ProhibidoException("El proveedor " + proveedor.getCodigo()
					+ " no declara origenes de embed permitidos, asi que no acepta urlRetorno");
		}
		String origen;
		try {
			URI uri = URI.create(urlRetorno.trim());
			if (uri.getScheme() == null || uri.getHost() == null) {
				throw new IllegalArgumentException("url incompleta");
			}
			origen = uri.getScheme().toLowerCase(Locale.ROOT) + "://" + uri.getHost().toLowerCase(Locale.ROOT)
					+ (uri.getPort() > 0 ? ":" + uri.getPort() : "");
		}
		catch (Exception e) {
			throw new ProhibidoException("La urlRetorno no es una direccion absoluta valida");
		}
		if (permitidos.stream().noneMatch(permitido -> permitido.equalsIgnoreCase(origen))) {
			throw new ProhibidoException(
					"El origen " + origen + " no esta permitido para el proveedor " + proveedor.getCodigo());
		}
		return urlRetorno.trim();
	}

	private void auditar(Tenant tenant, Usuario usuario, AccionAuditoria accion, ProveedorIdentidad proveedor,
			String sujetoExterno, Map<String, Object> extra) {
		Map<String, Object> detalle = new LinkedHashMap<>();
		detalle.put("proveedor", proveedor.getCodigo());
		detalle.put("aplicacionOrigen", proveedor.getOrigen().name());
		detalle.put("sujetoExterno", sujetoExterno);
		detalle.putAll(extra);
		if (accion == AccionAuditoria.ACCESO_DENEGADO) {
			auditoriaService.registrarFallo(tenant.getId(), accion, ENTIDAD, usuario.getId(), detalle);
			return;
		}
		auditoriaService.registrarConDetalle(tenant.getId(), accion, ENTIDAD, usuario.getId(), detalle);
	}

	private int vigenciaDe(ProveedorIdentidad proveedor) {
		return proveedor.getSegundosVigenciaCodigo() > 0 ? proveedor.getSegundosVigenciaCodigo()
				: propiedades.getSegundosVigenciaCodigo();
	}

	private String generarCodigo() {
		byte[] bytes = new byte[BYTES_CODIGO];
		AZAR.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String textoDe(Claims claims, String clave) {
		Object valor = clave == null ? null : claims.get(clave);
		if (valor == null) {
			return null;
		}
		String texto = String.valueOf(valor).trim();
		return texto.isEmpty() ? null : texto;
	}

	private String normalizar(String email) {
		return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}

	private List<String> separar(String valores) {
		if (valores == null || valores.isBlank()) {
			return List.of();
		}
		return Arrays.stream(valores.split(",")).map(String::trim).filter(texto -> !texto.isEmpty()).toList();
	}
}
