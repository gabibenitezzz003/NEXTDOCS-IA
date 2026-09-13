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

import com.nextdocs.ai.config.PropiedadesBootstrap;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.enumeraciones.EstadoTenant;
import com.nextdocs.ai.exceptions.NoAutorizadoException;
import com.nextdocs.ai.modelos.TenantModel;
import com.nextdocs.ai.repositorios.TenantRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BootstrapServiceTest {

	private static final String SECRETO = "secreto-bootstrap-de-prueba";

	private static final String CODIGO_TENANT = "empresa-prueba";

	private static final String NOMBRE_TENANT = "Empresa de prueba";

	private static final String EMAIL = "admin@empresa-prueba.com";

	private static final String CLAVE = "clave-admin-de-prueba";

	private TenantRepository tenantRepository;

	private TenantService tenantService;

	private PropiedadesBootstrap propiedades;

	private BootstrapService servicio;

	@BeforeEach
	void preparar() {
		tenantRepository = mock(TenantRepository.class);
		tenantService = mock(TenantService.class);
		propiedades = new PropiedadesBootstrap();
		propiedades.setHabilitado(true);
		propiedades.setSecreto(SECRETO);
		servicio = new BootstrapService(tenantRepository, tenantService, propiedades);
	}

	@Test
	@DisplayName("bootstrap deshabilitado rechaza la solicitud")
	void bootstrapDeshabilitadoRechaza() {
		propiedades.setHabilitado(false);

		assertThatThrownBy(() -> servicio.crearTenant(SECRETO, CODIGO_TENANT, NOMBRE_TENANT, EMAIL, CLAVE))
				.isInstanceOf(NoAutorizadoException.class);

		verify(tenantService, never()).crear(eq(CODIGO_TENANT), eq(NOMBRE_TENANT), eq(EMAIL), eq(CLAVE));
	}

	@Test
	@DisplayName("secreto incorrecto rechaza la solicitud")
	void secretoIncorrectoRechaza() {
		assertThatThrownBy(() -> servicio.crearTenant("secreto-incorrecto", CODIGO_TENANT, NOMBRE_TENANT, EMAIL,
				CLAVE))
				.isInstanceOf(NoAutorizadoException.class);

		verify(tenantService, never()).crear(eq(CODIGO_TENANT), eq(NOMBRE_TENANT), eq(EMAIL), eq(CLAVE));
	}

	@Test
	@DisplayName("tenant existente con el mismo codigo rechaza la creacion")
	void tenantExistenteRechaza() {
		Tenant existente = new Tenant();
		existente.setCodigo(CODIGO_TENANT);
		when(tenantRepository.findByCodigoAndBajaIsNull(CODIGO_TENANT)).thenReturn(Optional.of(existente));

		assertThatThrownBy(() -> servicio.crearTenant(SECRETO, CODIGO_TENANT, NOMBRE_TENANT, EMAIL, CLAVE))
				.isInstanceOf(NoAutorizadoException.class);

		verify(tenantService, never()).crear(eq(CODIGO_TENANT), eq(NOMBRE_TENANT), eq(EMAIL), eq(CLAVE));
	}

	@Test
	@DisplayName("un tenant activo con otro codigo tambien bloquea el bootstrap")
	void tenantActivoConOtroCodigoRechaza() {
		Tenant existente = new Tenant();
		existente.setCodigo("otro-tenant");
		when(tenantRepository.findByEstadoAndBajaIsNull(EstadoTenant.ACTIVO)).thenReturn(List.of(existente));

		assertThatThrownBy(() -> servicio.crearTenant(SECRETO, CODIGO_TENANT, NOMBRE_TENANT, EMAIL, CLAVE))
				.isInstanceOf(NoAutorizadoException.class);

		verify(tenantService, never()).crear(eq(CODIGO_TENANT), eq(NOMBRE_TENANT), eq(EMAIL), eq(CLAVE));
	}

	@Test
	@DisplayName("con secreto correcto y sin tenants crea el tenant y devuelve el modelo")
	void creaTenant() {
		Tenant creado = new Tenant();
		creado.setCodigo(CODIGO_TENANT);
		TenantModel modelo = new TenantModel();
		modelo.setCodigo(CODIGO_TENANT);
		when(tenantService.crear(CODIGO_TENANT, NOMBRE_TENANT, EMAIL, CLAVE)).thenReturn(creado);
		when(tenantService.obtener(creado.getId())).thenReturn(modelo);

		TenantModel resultado = servicio.crearTenant(SECRETO, CODIGO_TENANT, NOMBRE_TENANT, EMAIL, CLAVE);

		assertThat(resultado).isSameAs(modelo);
		verify(tenantService).crear(eq(CODIGO_TENANT), eq(NOMBRE_TENANT), eq(EMAIL), eq(CLAVE));
		verify(tenantService).obtener(creado.getId());
	}

	@Test
	@DisplayName("la clave del administrador se delega al servicio de tenants sin transformarla")
	void delegaClaveAdministrador() {
		Tenant creado = new Tenant();
		when(tenantService.crear(CODIGO_TENANT, NOMBRE_TENANT, EMAIL, CLAVE)).thenReturn(creado);
		when(tenantService.obtener(creado.getId())).thenReturn(new TenantModel());

		servicio.crearTenant(SECRETO, CODIGO_TENANT, NOMBRE_TENANT, EMAIL, CLAVE);

		verify(tenantService).crear(eq(CODIGO_TENANT), eq(NOMBRE_TENANT), eq(EMAIL), eq(CLAVE));
	}
}
