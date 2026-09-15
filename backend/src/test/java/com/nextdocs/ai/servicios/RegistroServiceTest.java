package com.nextdocs.ai.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.RegistroOrganizacionReqModel;
import com.nextdocs.ai.modelos.SesionResModel;
import com.nextdocs.ai.repositorios.TenantRepository;
import com.nextdocs.ai.repositorios.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RegistroServiceTest {

	private static final String CODIGO = "mi-empresa";

	private static final String NOMBRE_ORG = "Mi Empresa S.A.";

	private static final String NOMBRE_ADMIN = "Maria Perez";

	private static final String EMAIL = "maria@mi-empresa.com";

	private static final String CLAVE = "clave-segura-de-prueba";

	private TenantRepository tenantRepository;

	private UsuarioRepository usuarioRepository;

	private TenantService tenantService;

	private AutenticacionService autenticacionService;

	private RegistroService servicio;

	@BeforeEach
	void preparar() {
		tenantRepository = mock(TenantRepository.class);
		usuarioRepository = mock(UsuarioRepository.class);
		tenantService = mock(TenantService.class);
		autenticacionService = mock(AutenticacionService.class);
		servicio = new RegistroService(tenantRepository, usuarioRepository, tenantService,
				autenticacionService);
	}

	private RegistroOrganizacionReqModel datos() {
		RegistroOrganizacionReqModel datos = new RegistroOrganizacionReqModel();
		datos.setNombreOrganizacion(NOMBRE_ORG);
		datos.setCodigoOrganizacion(CODIGO);
		datos.setNombreAdministrador(NOMBRE_ADMIN);
		datos.setEmailAdministrador(EMAIL);
		datos.setClaveAdministrador(CLAVE);
		return datos;
	}

	@Test
	@DisplayName("un registro valido crea el tenant y devuelve la sesion del administrador")
	void registroValidoCreaYLoguea() {
		Tenant creado = new Tenant();
		creado.setCodigo(CODIGO);
		Usuario admin = new Usuario();
		admin.setTenant(creado);
		admin.setEmail(EMAIL);
		SesionResModel sesion = new SesionResModel();
		when(tenantService.crear(eq(CODIGO), eq(NOMBRE_ORG), eq(EMAIL), eq(CLAVE))).thenReturn(creado);
		when(usuarioRepository.buscarPorEmail(creado.getId(), EMAIL)).thenReturn(Optional.of(admin));
		when(autenticacionService.sesionDe(admin)).thenReturn(sesion);

		SesionResModel resultado = servicio.registrar(datos());

		assertThat(resultado).isSameAs(sesion);
		assertThat(admin.getNombre()).isEqualTo(NOMBRE_ADMIN);
		verify(usuarioRepository).save(admin);
	}

	@Test
	@DisplayName("el codigo se normaliza a minusculas antes de crear el tenant")
	void normalizaCodigo() {
		RegistroOrganizacionReqModel datos = datos();
		datos.setCodigoOrganizacion("  Mi-Empresa  ");
		Tenant creado = new Tenant();
		creado.setCodigo(CODIGO);
		when(tenantService.crear(eq(CODIGO), eq(NOMBRE_ORG), eq(EMAIL), eq(CLAVE))).thenReturn(creado);
		when(usuarioRepository.buscarPorEmail(creado.getId(), EMAIL))
				.thenReturn(Optional.of(new Usuario()));
		when(autenticacionService.sesionDe(org.mockito.ArgumentMatchers.any()))
				.thenReturn(new SesionResModel());

		servicio.registrar(datos);

		verify(tenantService).crear(eq(CODIGO), eq(NOMBRE_ORG), eq(EMAIL), eq(CLAVE));
	}

	@Test
	@DisplayName("un codigo de organizacion ya usado se rechaza")
	void codigoRepetidoRechaza() {
		when(tenantRepository.findByCodigoAndBajaIsNull(CODIGO)).thenReturn(Optional.of(new Tenant()));

		assertThatThrownBy(() -> servicio.registrar(datos())).isInstanceOf(ValidacionException.class)
				.hasMessageContaining("ya esta en uso");

		verify(tenantService, never()).crear(eq(CODIGO), eq(NOMBRE_ORG), eq(EMAIL), eq(CLAVE));
	}

	@Test
	@DisplayName("un email que ya tiene cuenta en cualquier tenant se rechaza")
	void emailRepetidoRechaza() {
		when(usuarioRepository.buscarPorEmailEnCualquierTenant(EMAIL)).thenReturn(List.of(new Usuario()));

		assertThatThrownBy(() -> servicio.registrar(datos())).isInstanceOf(ValidacionException.class)
				.hasMessageContaining("Ya existe una cuenta");

		verify(tenantService, never()).crear(eq(CODIGO), eq(NOMBRE_ORG), eq(EMAIL), eq(CLAVE));
	}
}
