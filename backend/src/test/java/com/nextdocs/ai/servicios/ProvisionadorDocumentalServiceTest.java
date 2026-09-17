package com.nextdocs.ai.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdocs.ai.config.PropiedadesDocumental;
import com.nextdocs.ai.entidades.ConfiguracionConector;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.enumeraciones.TipoAutenticacionConector;
import com.nextdocs.ai.repositorios.ConfiguracionConectorRepository;
import com.nextdocs.ai.repositorios.TenantRepository;
import com.nextdocs.ai.utiles.ResolvedorSecreto;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProvisionadorDocumentalServiceTest {

	private static final String TENANT_ID = "11111111-2222-3333-4444-555555555555";

	private HttpServer servidor;

	private AtomicReference<String> ultimaAutorizacion;

	private AtomicReference<String> ultimoCuerpo;

	private AtomicInteger pedidosRecibidos;

	private AtomicInteger codigoRespuesta;

	private AtomicReference<String> cuerpoRespuesta;

	private ProvisionadorDocumentalService servicio;

	private ConfiguracionConectorRepository conectorRepository;

	private TenantRepository tenantRepository;

	private AuditoriaService auditoriaService;

	@BeforeEach
	void levantarMotorFalso() throws IOException {
		ultimaAutorizacion = new AtomicReference<>();
		ultimoCuerpo = new AtomicReference<>();
		pedidosRecibidos = new AtomicInteger();
		codigoRespuesta = new AtomicInteger(201);
		cuerpoRespuesta = new AtomicReference<>("{\"inquilinoId\":\"" + TENANT_ID
				+ "\",\"clave\":\"ndk_prueba_0123456789abcdef\"}");

		servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		servidor.createContext("/api/v1/admin/inquilinos", intercambio -> {
			pedidosRecibidos.incrementAndGet();
			ultimaAutorizacion.set(intercambio.getRequestHeaders().getFirst("Authorization"));
			ultimoCuerpo.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			byte[] cuerpo = cuerpoRespuesta.get().getBytes(StandardCharsets.UTF_8);
			intercambio.getResponseHeaders().add("Content-Type", "application/json");
			intercambio.sendResponseHeaders(codigoRespuesta.get(), cuerpo.length);
			try (OutputStream salida = intercambio.getResponseBody()) {
				salida.write(cuerpo);
			}
		});
		servidor.start();

		conectorRepository = mock(ConfiguracionConectorRepository.class);
		tenantRepository = mock(TenantRepository.class);
		auditoriaService = mock(AuditoriaService.class);

		PropiedadesDocumental propiedades = new PropiedadesDocumental();
		propiedades.setUrlBase("http://127.0.0.1:" + servidor.getAddress().getPort());
		propiedades.setClaveAdmin("clave-plataforma");

		servicio = new ProvisionadorDocumentalService(propiedades, conectorRepository, tenantRepository,
				auditoriaService, new ObjectMapper());
	}

	@AfterEach
	void bajarServidor() {
		servidor.stop(0);
	}

	@Test
	@DisplayName("provisiona el conector con la clave que devuelve el motor")
	void provisionaConector() {
		Tenant tenant = construirTenant();
		when(conectorRepository.buscarPorCodigo(TENANT_ID, DocumentalProxyService.CODIGO_CONECTOR))
				.thenReturn(Optional.empty());

		boolean resultado = servicio.provisionar(tenant);

		assertThat(resultado).isTrue();
		assertThat(ultimaAutorizacion.get()).isEqualTo("Bearer clave-plataforma");
		assertThat(ultimoCuerpo.get()).contains(TENANT_ID).contains("Empresa Demo");

		ArgumentCaptor<ConfiguracionConector> capturador = ArgumentCaptor.forClass(ConfiguracionConector.class);
		verify(conectorRepository).save(capturador.capture());
		ConfiguracionConector guardado = capturador.getValue();
		assertThat(guardado.getCodigo()).isEqualTo(DocumentalProxyService.CODIGO_CONECTOR);
		assertThat(guardado.getTipoAutenticacion()).isEqualTo(TipoAutenticacionConector.BEARER);
		assertThat(guardado.getReferenciaSecreto())
				.isEqualTo(ResolvedorSecreto.PREFIJO_LITERAL + "ndk_prueba_0123456789abcdef");
		assertThat(guardado.isActivo()).isTrue();
	}

	@Test
	@DisplayName("no reprovisiona cuando ya existe un conector activo")
	void noReprovisionaConectorActivo() {
		ConfiguracionConector existente = new ConfiguracionConector();
		existente.setActivo(true);
		when(conectorRepository.buscarPorCodigo(TENANT_ID, DocumentalProxyService.CODIGO_CONECTOR))
				.thenReturn(Optional.of(existente));

		assertThat(servicio.provisionar(construirTenant())).isTrue();
		assertThat(pedidosRecibidos.get()).isZero();
		verify(conectorRepository, never()).save(any());
	}

	@Test
	@DisplayName("sin clave de plataforma no intenta provisionar")
	void sinClaveAdminNoProvisiona() {
		PropiedadesDocumental propiedades = new PropiedadesDocumental();
		ProvisionadorDocumentalService deshabilitado = new ProvisionadorDocumentalService(propiedades,
				conectorRepository, tenantRepository, auditoriaService, new ObjectMapper());
		when(conectorRepository.buscarPorCodigo(TENANT_ID, DocumentalProxyService.CODIGO_CONECTOR))
				.thenReturn(Optional.empty());

		assertThat(deshabilitado.provisionar(construirTenant())).isFalse();
		assertThat(pedidosRecibidos.get()).isZero();
	}

	@Test
	@DisplayName("un fallo del motor devuelve falso y enfria los reintentos")
	void falloMotorEnfria() {
		codigoRespuesta.set(500);
		when(conectorRepository.buscarPorCodigo(TENANT_ID, DocumentalProxyService.CODIGO_CONECTOR))
				.thenReturn(Optional.empty());
		Tenant tenant = construirTenant();

		assertThat(servicio.provisionar(tenant)).isFalse();
		assertThat(servicio.provisionar(tenant)).isFalse();
		assertThat(pedidosRecibidos.get()).isEqualTo(1);
		verify(conectorRepository, never()).save(any());
	}

	@Test
	@DisplayName("una respuesta sin credencial valida se considera fallo")
	void respuestaSinClaveFalla() {
		cuerpoRespuesta.set("{\"inquilinoId\":\"" + TENANT_ID + "\"}");
		when(conectorRepository.buscarPorCodigo(TENANT_ID, DocumentalProxyService.CODIGO_CONECTOR))
				.thenReturn(Optional.empty());

		assertThat(servicio.provisionar(construirTenant())).isFalse();
		verify(conectorRepository, never()).save(any());
	}

	@Test
	@DisplayName("por tenantId resuelve el tenant y actualiza un conector existente inactivo")
	void porTenantIdActualizaExistente() {
		Tenant tenant = construirTenant();
		ConfiguracionConector existente = new ConfiguracionConector();
		existente.setActivo(false);
		when(conectorRepository.buscarPorCodigo(TENANT_ID, DocumentalProxyService.CODIGO_CONECTOR))
				.thenReturn(Optional.of(existente));
		when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));

		assertThat(servicio.provisionar(TENANT_ID)).isTrue();

		verify(conectorRepository).save(existente);
		assertThat(existente.getReferenciaSecreto())
				.isEqualTo(ResolvedorSecreto.PREFIJO_LITERAL + "ndk_prueba_0123456789abcdef");
		assertThat(existente.isActivo()).isTrue();
	}

	private Tenant construirTenant() {
		Tenant tenant = new Tenant();
		tenant.setId(TENANT_ID);
		tenant.setCodigo("demo");
		tenant.setNombre("Empresa Demo");
		return tenant;
	}
}
