package com.nextdocs.ai.servicios.conectores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdocs.ai.clientes.FollowCliente;
import com.nextdocs.ai.entidades.ConfiguracionConector;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.enumeraciones.TipoAutenticacionConector;
import com.nextdocs.ai.exceptions.ConectorNoDisponibleException;
import com.nextdocs.ai.modelos.CandidatoAsociacionModel;
import com.nextdocs.ai.modelos.ContextoAsociacionModel;
import com.nextdocs.ai.repositorios.ConfiguracionConectorRepository;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FollowConnectorTest {

	private static final String TENANT = "tenant-prueba";

	private HttpServer servidor;

	private AtomicInteger codigoRespuesta;

	private AtomicReference<String> cuerpoRespuesta;

	private AtomicLong demoraMilisegundos;

	private AtomicReference<String> ultimaConsulta;

	private ConfiguracionConector configuracion;

	private FollowConnector conector;

	@BeforeEach
	void levantarFollowFalso() throws IOException {
		codigoRespuesta = new AtomicInteger(200);
		cuerpoRespuesta = new AtomicReference<>("{\"content\":[]}");
		demoraMilisegundos = new AtomicLong(0);
		ultimaConsulta = new AtomicReference<>();

		servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		servidor.createContext("/", intercambio -> {
			ultimaConsulta.set(intercambio.getRequestURI().toString());
			if (demoraMilisegundos.get() > 0) {
				try {
					Thread.sleep(demoraMilisegundos.get());
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			byte[] cuerpo = cuerpoRespuesta.get().getBytes(StandardCharsets.UTF_8);
			intercambio.getResponseHeaders().add("Content-Type", "application/json");
			intercambio.sendResponseHeaders(codigoRespuesta.get(), cuerpo.length);
			try (OutputStream salida = intercambio.getResponseBody()) {
				salida.write(cuerpo);
			}
		});
		servidor.start();

		ObjectMapper objectMapper = new ObjectMapper();
		configuracion = construirConfiguracion("http://127.0.0.1:" + servidor.getAddress().getPort());
		ConfiguracionConectorRepository repositorio = mock(ConfiguracionConectorRepository.class);
		when(repositorio.buscarPorCodigo(TENANT, FollowConnector.ORIGEN)).thenReturn(Optional.of(configuracion));
		conector = new FollowConnector(new FollowCliente(objectMapper), repositorio, objectMapper);
	}

	@AfterEach
	void bajarServidor() {
		servidor.stop(0);
	}

	@Test
	@DisplayName("un unico pedido con numero exacto da un candidato con puntaje 1")
	void candidatoUnicoExacto() {
		cuerpoRespuesta.set("""
				{"content":[{"id":"ped-1","nroPedido":"PED-2026-0042","nombreCliente":"Alimentos Pampa",\
				"estadoPedidoNombre":"En transito"}]}""");

		List<CandidatoAsociacionModel> candidatos = conector.buscarCandidatos(contexto("PED-2026-0042"));

		assertThat(candidatos).hasSize(1);
		assertThat(candidatos.get(0).getIdObjeto()).isEqualTo("ped-1");
		assertThat(candidatos.get(0).getOrigen()).isEqualTo("FOLLOW");
		assertThat(candidatos.get(0).getTipoObjeto()).isEqualTo("PEDIDO");
		assertThat(candidatos.get(0).getPuntaje()).isEqualByComparingTo(new BigDecimal("1.0000"));
		assertThat(candidatos.get(0).getDescripcion()).contains("Alimentos Pampa").contains("En transito");
	}

	@Test
	@DisplayName("QA1-06: dos pedidos compatibles devuelven dos candidatos, el conector no elige")
	void dosCandidatosCompatibles() {
		cuerpoRespuesta.set("""
				{"content":[{"id":"ped-1","nroPedido":"PED-2026-0042","nombreCliente":"Alimentos Pampa"},\
				{"id":"ped-2","nroPedido":"PED-2026-0042","nombreCliente":"Alimentos Pampa Sur"}]}""");

		List<CandidatoAsociacionModel> candidatos = conector.buscarCandidatos(contexto("PED-2026-0042"));

		assertThat(candidatos).hasSize(2);
		assertThat(candidatos).allMatch(c -> !c.isSeleccionado());
	}

	@Test
	@DisplayName("QA1-05: un timeout del conector es excepcion tecnica, no cero candidatos")
	void timeoutEsExcepcionNoListaVacia() {
		configuracion.setTiempoEsperaMilisegundos(300);
		demoraMilisegundos.set(2000);
		cuerpoRespuesta.set("{\"content\":[]}");

		assertThatThrownBy(() -> conector.buscarCandidatos(contexto("PED-2026-0042")))
				.isInstanceOf(ConectorNoDisponibleException.class)
				.hasMessageContaining("No se pudo contactar al conector");
	}

	@Test
	@DisplayName("QA-FOL-01: un 500 de Follow es excepcion, distinta de una busqueda sin resultados")
	void errorDelServidorEsExcepcion() {
		codigoRespuesta.set(500);
		cuerpoRespuesta.set("{\"error\":\"boom\"}");

		assertThatThrownBy(() -> conector.buscarCandidatos(contexto("PED-2026-0042")))
				.isInstanceOf(ConectorNoDisponibleException.class)
				.hasMessageContaining("codigo HTTP 500");
	}

	@Test
	@DisplayName("una busqueda sin resultados devuelve lista vacia sin lanzar excepcion")
	void sinResultadosDevuelveListaVacia() {
		cuerpoRespuesta.set("{\"content\":[]}");

		assertThat(conector.buscarCandidatos(contexto("PED-INEXISTENTE"))).isEmpty();
	}

	@Test
	@DisplayName("sin campos de busqueda en el documento no consulta a Follow")
	void sinCamposNoConsulta() {
		ContextoAsociacionModel contexto = new ContextoAsociacionModel();
		contexto.setTenantId(TENANT);
		contexto.setDocumentoId("doc-1");
		contexto.getValores().put("campoIrrelevante", "x");

		assertThat(conector.buscarCandidatos(contexto)).isEmpty();
		assertThat(ultimaConsulta.get()).isNull();
	}

	@Test
	@DisplayName("manda la clave de servicio en la cabecera configurada y el termino en la query")
	void enviaCredencialYTermino() {
		System.setProperty("FOLLOW_CLAVE_PRUEBA", "clave-follow");
		try {
			cuerpoRespuesta.set("{\"content\":[]}");
			conector.buscarCandidatos(contexto("PED-2026-0042"));
			assertThat(ultimaConsulta.get()).contains("/api/pedido/listado/estado")
					.contains("search=PED-2026-0042");
		} finally {
			System.clearProperty("FOLLOW_CLAVE_PRUEBA");
		}
	}

	@Test
	@DisplayName("sin credencial resoluble falla antes de salir a la red")
	void sinCredencialFalla() {
		configuracion.setReferenciaSecreto("env:VARIABLE_QUE_NO_EXISTE");

		assertThatThrownBy(() -> conector.buscarCandidatos(contexto("PED-2026-0042")))
				.isInstanceOf(ConectorNoDisponibleException.class)
				.hasMessageContaining("No hay credencial resoluble");
	}

	private ContextoAsociacionModel contexto(String numeroPedido) {
		ContextoAsociacionModel contexto = new ContextoAsociacionModel();
		contexto.setTenantId(TENANT);
		contexto.setDocumentoId("doc-1");
		contexto.setCodigoPlantilla("REMITO");
		contexto.getValores().put("numeroPedido", numeroPedido);
		return contexto;
	}

	private ConfiguracionConector construirConfiguracion(String urlBase) {
		Tenant tenant = new Tenant();
		tenant.setId(TENANT);
		tenant.setCodigo("demo");
		ConfiguracionConector configuracion = new ConfiguracionConector();
		configuracion.setTenant(tenant);
		configuracion.setCodigo(FollowConnector.ORIGEN);
		configuracion.setNombre("Follow");
		configuracion.setUrlBase(urlBase);
		configuracion.setTipoAutenticacion(TipoAutenticacionConector.CLAVE_API);
		configuracion.setNombreCabeceraClave("apiKey");
		configuracion.setReferenciaSecreto("literal:clave-follow");
		configuracion.setTiempoEsperaMilisegundos(5000);
		configuracion.setIntentosMaximos(1);
		configuracion.setUmbralCircuitoAbierto(3);
		configuracion.setDuracionCircuitoAbiertoSegundos(30);
		configuracion.setParametros("{\"camposBusqueda\":[\"numeroPedido\"]}");
		configuracion.setActivo(true);
		return configuracion;
	}
}
