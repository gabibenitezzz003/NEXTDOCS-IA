package com.nextdocs.ai.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.nextdocs.ai.entidades.ConfiguracionConector;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.enumeraciones.TipoAutenticacionConector;
import com.nextdocs.ai.exceptions.ConectorNoDisponibleException;
import com.nextdocs.ai.repositorios.ConfiguracionConectorRepository;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DocumentalProxyServiceTest {

	private static final String TENANT = "tenant-prueba";

	private HttpServer servidor;

	private AtomicReference<String> ultimaRuta;

	private AtomicReference<String> ultimaAutorizacion;

	private AtomicReference<String> ultimoCuerpo;

	private AtomicReference<String> ultimoMetodo;

	private AtomicInteger codigoRespuesta;

	private DocumentalProxyService servicio;

	private ConfiguracionConectorRepository repositorio;

	private ProvisionadorDocumentalService provisionador;

	@BeforeEach
	void levantarDocumentalFalso() throws IOException {
		ultimaRuta = new AtomicReference<>();
		ultimaAutorizacion = new AtomicReference<>();
		ultimoCuerpo = new AtomicReference<>();
		ultimoMetodo = new AtomicReference<>();
		codigoRespuesta = new AtomicInteger(200);

		servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		servidor.createContext("/", intercambio -> {
			ultimaRuta.set(intercambio.getRequestURI().toString());
			ultimoMetodo.set(intercambio.getRequestMethod());
			ultimaAutorizacion.set(intercambio.getRequestHeaders().getFirst("Authorization"));
			ultimoCuerpo.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			byte[] cuerpo = "{\"documentos\":[]}".getBytes(StandardCharsets.UTF_8);
			intercambio.getResponseHeaders().add("Content-Type", "application/json");
			intercambio.sendResponseHeaders(codigoRespuesta.get(), cuerpo.length);
			try (OutputStream salida = intercambio.getResponseBody()) {
				salida.write(cuerpo);
			}
		});
		servidor.start();

		repositorio = mock(ConfiguracionConectorRepository.class);
		provisionador = mock(ProvisionadorDocumentalService.class);
		servicio = new DocumentalProxyService(repositorio, provisionador);
	}

	@AfterEach
	void bajarServidor() {
		servidor.stop(0);
	}

	@Test
	@DisplayName("reenvia la subruta, la consulta y la clave Bearer del conector del tenant")
	void reenviaConAutenticacion() {
		configurar("http://127.0.0.1:" + servidor.getAddress().getPort());

		HttpResponse<byte[]> respuesta = servicio.reenviar(TENANT, "GET", "/documentos",
				"estado=OBSERVADO&limite=25", null, null, null);

		assertThat(respuesta.statusCode()).isEqualTo(200);
		assertThat(new String(respuesta.body(), StandardCharsets.UTF_8)).contains("documentos");
		assertThat(ultimaRuta.get()).isEqualTo("/api/v1/documentos?estado=OBSERVADO&limite=25");
		assertThat(ultimaAutorizacion.get()).isEqualTo("Bearer clave-documental");
		assertThat(ultimoMetodo.get()).isEqualTo("GET");
	}

	@Test
	@DisplayName("un POST reenvia el cuerpo y la clave de idempotencia")
	void postConCuerpo() {
		configurar("http://127.0.0.1:" + servidor.getAddress().getPort());
		byte[] cuerpo = "{\"nombreArchivo\":\"a.pdf\"}".getBytes(StandardCharsets.UTF_8);

		servicio.reenviar(TENANT, "POST", "/documentos", null, cuerpo, "application/json", "idem-1");

		assertThat(ultimoMetodo.get()).isEqualTo("POST");
		assertThat(ultimoCuerpo.get()).isEqualTo("{\"nombreArchivo\":\"a.pdf\"}");
	}

	@Test
	@DisplayName("conserva el codigo de error del motor en vez de traducirlo")
	void conservaErrorDelMotor() {
		configurar("http://127.0.0.1:" + servidor.getAddress().getPort());
		codigoRespuesta.set(404);

		HttpResponse<byte[]> respuesta = servicio.reenviar(TENANT, "GET", "/documentos/xyz", null, null,
				null, null);

		assertThat(respuesta.statusCode()).isEqualTo(404);
	}

	@Test
	@DisplayName("un tenant sin conector activo recibe ConectorNoDisponible")
	void sinConector() {
		when(repositorio.buscarPorCodigo(TENANT, DocumentalProxyService.CODIGO_CONECTOR))
				.thenReturn(Optional.empty());

		assertThatThrownBy(
				() -> servicio.reenviar(TENANT, "GET", "/documentos", null, null, null, null))
						.isInstanceOf(ConectorNoDisponibleException.class);
		assertThat(servicio.habilitado(TENANT)).isFalse();
		verify(provisionador).provisionar(TENANT);
	}

	@Test
	@DisplayName("un conector inactivo no habilita el modulo")
	void conectorInactivo() {
		ConfiguracionConector configuracion = construir("http://127.0.0.1:1");
		configuracion.setActivo(false);
		when(repositorio.buscarPorCodigo(TENANT, DocumentalProxyService.CODIGO_CONECTOR))
				.thenReturn(Optional.of(configuracion));

		assertThat(servicio.habilitado(TENANT)).isFalse();
		assertThatThrownBy(
				() -> servicio.reenviar(TENANT, "GET", "/documentos", null, null, null, null))
						.isInstanceOf(ConectorNoDisponibleException.class);
	}

	private void configurar(String urlBase) {
		when(repositorio.buscarPorCodigo(TENANT, DocumentalProxyService.CODIGO_CONECTOR))
				.thenReturn(Optional.of(construir(urlBase)));
	}

	private ConfiguracionConector construir(String urlBase) {
		Tenant tenant = new Tenant();
		tenant.setId(TENANT);
		tenant.setCodigo("demo");
		ConfiguracionConector configuracion = new ConfiguracionConector();
		configuracion.setTenant(tenant);
		configuracion.setCodigo(DocumentalProxyService.CODIGO_CONECTOR);
		configuracion.setNombre("Motor documental");
		configuracion.setUrlBase(urlBase);
		configuracion.setTipoAutenticacion(TipoAutenticacionConector.BEARER);
		configuracion.setReferenciaSecreto("literal:clave-documental");
		configuracion.setActivo(true);
		return configuracion;
	}
}
