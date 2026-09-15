package com.nextdocs.ai.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.config.PropiedadesFederacion;
import com.nextdocs.ai.entidades.ProveedorIdentidad;
import com.nextdocs.ai.entidades.Rol;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.enumeraciones.EstadoTenant;
import com.nextdocs.ai.enumeraciones.EstadoUsuario;
import com.nextdocs.ai.enumeraciones.OrigenIdentidad;
import com.nextdocs.ai.modelos.CodigoEmbedModel;
import com.nextdocs.ai.modelos.IntercambioFederadoReqModel;
import com.nextdocs.ai.repositorios.CodigoEmbedRepository;
import com.nextdocs.ai.repositorios.ProveedorIdentidadRepository;
import com.nextdocs.ai.repositorios.RolRepository;
import com.nextdocs.ai.repositorios.UsuarioRepository;
import com.nextdocs.ai.utiles.Permiso;

import io.jsonwebtoken.Claims;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FederacionIdentidadServiceTest {

	private static final String EMAIL = "nueva.persona@ejemplo.com";

	private ProveedorIdentidadRepository proveedorIdentidadRepository;

	private CodigoEmbedRepository codigoEmbedRepository;

	private UsuarioRepository usuarioRepository;

	private RolRepository rolRepository;

	private VerificadorTokenIdpService verificadorTokenIdpService;

	private AutenticacionService autenticacionService;

	private AuditoriaService auditoriaService;

	private TenantService tenantService;

	private FederacionIdentidadService servicio;

	private Tenant tenantProveedor;

	@BeforeEach
	void preparar() {
		proveedorIdentidadRepository = mock(ProveedorIdentidadRepository.class);
		codigoEmbedRepository = mock(CodigoEmbedRepository.class);
		usuarioRepository = mock(UsuarioRepository.class);
		rolRepository = mock(RolRepository.class);
		verificadorTokenIdpService = mock(VerificadorTokenIdpService.class);
		autenticacionService = mock(AutenticacionService.class);
		auditoriaService = mock(AuditoriaService.class);
		tenantService = mock(TenantService.class);
		servicio = new FederacionIdentidadService(proveedorIdentidadRepository, codigoEmbedRepository,
				usuarioRepository, rolRepository, verificadorTokenIdpService, autenticacionService,
				auditoriaService, tenantService, new PropiedadesFederacion());
		tenantProveedor = new Tenant();
		tenantProveedor.setId("tenant-proveedor");
		tenantProveedor.setCodigo("organizacion");
		tenantProveedor.setEstado(EstadoTenant.ACTIVO);
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	private ProveedorIdentidad proveedor(boolean verificaEmail) {
		ProveedorIdentidad proveedor = new ProveedorIdentidad();
		proveedor.setTenant(tenantProveedor);
		proveedor.setCodigo("GOOGLE");
		proveedor.setOrigen(OrigenIdentidad.OIDC);
		proveedor.setActivo(true);
		proveedor.setPermitirJit(true);
		proveedor.setPermitirVinculoPorEmail(true);
		proveedor.setVerificaEmail(verificaEmail);
		proveedor.setCodigoRolPorDefecto(Permiso.CODIGO_ROL_OPERADOR);
		proveedor.setClaimSujeto("sub");
		proveedor.setClaimEmail("email");
		proveedor.setClaimNombre("name");
		proveedor.setSegundosVigenciaCodigo(60);
		return proveedor;
	}

	private void prepararIntercambio(ProveedorIdentidad proveedor) {
		when(proveedorIdentidadRepository.buscarPorCodigoTenantYCodigo("organizacion", "GOOGLE"))
				.thenReturn(Optional.of(proveedor));
		Claims claims = mock(Claims.class);
		when(claims.get("sub")).thenReturn("sujeto-123");
		when(claims.get("email")).thenReturn(EMAIL);
		when(claims.get("name")).thenReturn("Nueva Persona");
		when(verificadorTokenIdpService.verificar(eq(proveedor), anyString())).thenReturn(claims);
	}

	private IntercambioFederadoReqModel peticion(boolean tenantPropio) {
		IntercambioFederadoReqModel peticion = new IntercambioFederadoReqModel();
		peticion.setCodigoTenant("organizacion");
		peticion.setProveedor("GOOGLE");
		peticion.setToken("token-de-prueba");
		peticion.setTenantPropio(tenantPropio);
		return peticion;
	}

	@Test
	@DisplayName("un ingreso social sin organizacion crea un portal propio con rol administrador")
	void ingresoSinOrganizacionCreaPortalPropio() {
		ProveedorIdentidad proveedor = proveedor(true);
		prepararIntercambio(proveedor);
		when(usuarioRepository.buscarPorIdentidadExternaGlobal(OrigenIdentidad.OIDC, "sujeto-123"))
				.thenReturn(Optional.empty());
		when(usuarioRepository.buscarPorEmailEnCualquierTenant(EMAIL)).thenReturn(List.of());
		Tenant personal = new Tenant();
		personal.setId("tenant-personal");
		personal.setCodigo("u-nueva-persona-a1b2c3");
		personal.setEstado(EstadoTenant.ACTIVO);
		when(tenantService.crearSinAdministrador(anyString(), anyString())).thenReturn(personal);
		Rol admin = new Rol();
		admin.setCodigo(Permiso.CODIGO_ROL_ADMINISTRADOR);
		when(rolRepository.buscarPorCodigo("tenant-personal", Permiso.CODIGO_ROL_ADMINISTRADOR))
				.thenReturn(Optional.of(admin));

		CodigoEmbedModel resultado = servicio.intercambiar(peticion(true));

		assertThat(resultado.isAprovisionado()).isTrue();
		ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarioRepository, org.mockito.Mockito.atLeastOnce()).save(capturado.capture());
		Usuario nuevo = capturado.getAllValues().get(0);
		assertThat(nuevo.getTenant()).isSameAs(personal);
		assertThat(nuevo.getRoles()).containsExactly(admin);
		assertThat(nuevo.getEmail()).isEqualTo(EMAIL);
		verify(tenantService).crearSinAdministrador(anyString(), eq("Nueva Persona"));
	}

	@Test
	@DisplayName("con organizacion escrita el usuario nuevo cae en ese tenant con el rol del proveedor")
	void ingresoConOrganizacionCaeEnEseTenant() {
		ProveedorIdentidad proveedor = proveedor(true);
		prepararIntercambio(proveedor);
		when(usuarioRepository.buscarPorIdentidadExternaGlobal(OrigenIdentidad.OIDC, "sujeto-123"))
				.thenReturn(Optional.empty());
		when(usuarioRepository.buscarPorEmailEnCualquierTenant(EMAIL)).thenReturn(List.of());
		Rol operador = new Rol();
		operador.setCodigo(Permiso.CODIGO_ROL_OPERADOR);
		when(rolRepository.buscarPorCodigo("tenant-proveedor", Permiso.CODIGO_ROL_OPERADOR))
				.thenReturn(Optional.of(operador));

		CodigoEmbedModel resultado = servicio.intercambiar(peticion(false));

		assertThat(resultado.isAprovisionado()).isTrue();
		ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarioRepository, org.mockito.Mockito.atLeastOnce()).save(capturado.capture());
		assertThat(capturado.getAllValues().get(0).getTenant()).isSameAs(tenantProveedor);
		assertThat(capturado.getAllValues().get(0).getRoles()).containsExactly(operador);
		verify(tenantService, never()).crearSinAdministrador(anyString(), anyString());
	}

	@Test
	@DisplayName("un email ya registrado en otro tenant reutiliza esa cuenta y no crea portal")
	void emailExistenteReutilizaSuTenant() {
		ProveedorIdentidad proveedor = proveedor(true);
		prepararIntercambio(proveedor);
		Tenant ajeno = new Tenant();
		ajeno.setId("tenant-ajeno");
		ajeno.setCodigo("otra-org");
		ajeno.setEstado(EstadoTenant.ACTIVO);
		Usuario existente = new Usuario();
		existente.setTenant(ajeno);
		existente.setEmail(EMAIL);
		existente.setEstado(EstadoUsuario.ACTIVO);
		existente.setOrigenIdentidad(OrigenIdentidad.LOCAL);
		when(usuarioRepository.buscarPorIdentidadExternaGlobal(OrigenIdentidad.OIDC, "sujeto-123"))
				.thenReturn(Optional.empty());
		when(usuarioRepository.buscarPorEmailEnCualquierTenant(EMAIL)).thenReturn(List.of(existente));

		CodigoEmbedModel resultado = servicio.intercambiar(peticion(true));

		assertThat(resultado.isAprovisionado()).isTrue();
		assertThat(resultado.getCodigoTenant()).isEqualTo("otra-org");
		assertThat(existente.getOrigenIdentidad()).isEqualTo(OrigenIdentidad.OIDC);
		assertThat(existente.getIdUsuarioExterno()).isEqualTo("sujeto-123");
		verify(tenantService, never()).crearSinAdministrador(anyString(), anyString());
	}

	@Test
	@DisplayName("un proveedor que no verifica emails mantiene la busqueda limitada a su tenant")
	void proveedorSinVerificacionMantieneBusquedaLocal() {
		ProveedorIdentidad proveedor = proveedor(false);
		prepararIntercambio(proveedor);
		when(usuarioRepository.buscarPorIdentidadExterna("tenant-proveedor", OrigenIdentidad.OIDC,
				"sujeto-123")).thenReturn(Optional.empty());
		when(usuarioRepository.buscarPorEmail("tenant-proveedor", EMAIL)).thenReturn(Optional.empty());
		Rol operador = new Rol();
		operador.setCodigo(Permiso.CODIGO_ROL_OPERADOR);
		when(rolRepository.buscarPorCodigo("tenant-proveedor", Permiso.CODIGO_ROL_OPERADOR))
				.thenReturn(Optional.of(operador));

		servicio.intercambiar(peticion(false));

		verify(usuarioRepository, never()).buscarPorIdentidadExternaGlobal(any(), anyString());
		verify(usuarioRepository, never()).buscarPorEmailEnCualquierTenant(anyString());
		verify(tenantService, never()).crearSinAdministrador(anyString(), anyString());
		ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarioRepository, org.mockito.Mockito.atLeastOnce()).save(capturado.capture());
		assertThat(capturado.getAllValues().get(0).getTenant()).isSameAs(tenantProveedor);
	}
}
