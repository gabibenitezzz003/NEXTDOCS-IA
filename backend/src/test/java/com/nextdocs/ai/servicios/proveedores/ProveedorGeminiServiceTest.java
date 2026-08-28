package com.nextdocs.ai.servicios.proveedores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdocs.ai.clientes.GeminiCliente;
import com.nextdocs.ai.entidades.ConfiguracionProveedor;
import com.nextdocs.ai.enumeraciones.PresenciaCampo;
import com.nextdocs.ai.enumeraciones.ProveedorDocumentalIa;
import com.nextdocs.ai.enumeraciones.TipoDatoCampo;
import com.nextdocs.ai.exceptions.ProveedorNoDisponibleException;
import com.nextdocs.ai.modelos.CampoEsquemaModel;
import com.nextdocs.ai.modelos.ResultadoExtraccionModel;
import com.nextdocs.ai.modelos.SolicitudExtraccionModel;
import com.nextdocs.ai.modelos.ValorCanonicoModel;
import com.nextdocs.ai.repositorios.ConfiguracionProveedorRepository;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProveedorGeminiServiceTest {

	private static final String TENANT = "tenant-prueba";

	private static final String CLAVE = "clave-de-prueba";

	private final ObjectMapper objectMapper = new ObjectMapper();

	private HttpServer servidor;

	private AtomicInteger codigoRespuesta;

	private AtomicReference<String> cuerpoRespuesta;

	private AtomicReference<String> cuerpoRecibido;

	private ProveedorGeminiService proveedor;

	@BeforeEach
	void levantarServidor() throws IOException {
		codigoRespuesta = new AtomicInteger(200);
		cuerpoRespuesta = new AtomicReference<>("{}");
		cuerpoRecibido = new AtomicReference<>();

		servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		servidor.createContext("/", intercambio -> {
			cuerpoRecibido.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			byte[] cuerpo = cuerpoRespuesta.get().getBytes(StandardCharsets.UTF_8);
			intercambio.getResponseHeaders().add("Content-Type", "application/json");
			intercambio.sendResponseHeaders(codigoRespuesta.get(), cuerpo.length);
			try (OutputStream salida = intercambio.getResponseBody()) {
				salida.write(cuerpo);
			}
		});
		servidor.start();

		System.setProperty(ProveedorGeminiService.VARIABLE_CLAVE_GLOBAL, CLAVE);

		String urlBase = "http://127.0.0.1:" + servidor.getAddress().getPort();
		GeminiCliente cliente = new GeminiCliente(objectMapper, urlBase, 10);
		ConfiguracionProveedorRepository repositorio = mock(ConfiguracionProveedorRepository.class);
		when(repositorio.buscarPorProveedor(TENANT, ProveedorDocumentalIa.GEMINI))
				.thenReturn(Optional.of(configuracion()));
		proveedor = new ProveedorGeminiService(cliente, new ConstructorSolicitudGemini(objectMapper), repositorio,
				objectMapper);
	}

	@AfterEach
	void bajarServidor() {
		servidor.stop(0);
		System.clearProperty(ProveedorGeminiService.VARIABLE_CLAVE_GLOBAL);
	}

	@Test
	@DisplayName("distingue PRESENTE, NO_FIGURA e ILEGIBLE y normaliza el valor segun el tipo de dato")
	void distinguePresencias() {
		responderConCampos("""
				{"clave":"numeroRemito","valor":"0001-00012345","presencia":"PRESENTE","confianza":0.97,"pagina":1},
				{"clave":"cuitEmisor","valor":"30-12345678-9","presencia":"PRESENTE","confianza":0.93,"pagina":1},
				{"clave":"conformidad","valor":"","presencia":"NO_FIGURA","confianza":0.99,"pagina":1},
				{"clave":"fechaEmision","valor":"","presencia":"ILEGIBLE","confianza":0.21,"pagina":2}
				""");

		ResultadoExtraccionModel resultado = proveedor.extraer(solicitud());

		assertThat(resultado.getProveedor()).isEqualTo(ProveedorDocumentalIa.GEMINI);
		assertThat(resultado.getValores()).hasSize(4);
		assertThat(buscar(resultado, "numeroRemito").getPresencia()).isEqualTo(PresenciaCampo.PRESENTE);
		assertThat(buscar(resultado, "cuitEmisor").getValorNormalizado()).isEqualTo("30123456789");
		assertThat(buscar(resultado, "conformidad").getPresencia()).isEqualTo(PresenciaCampo.NO_FIGURA);
		assertThat(buscar(resultado, "fechaEmision").getPresencia()).isEqualTo(PresenciaCampo.ILEGIBLE);
		assertThat(resultado.getAdvertencias()).isEmpty();
	}

	@Test
	@DisplayName("registra el uso de tokens y calcula el costo con los precios configurados")
	void calculaCosto() {
		responderConCampos(
				"""
						{"clave":"numeroRemito","valor":"A-1","presencia":"PRESENTE","confianza":0.9,"pagina":1},
						{"clave":"cuitEmisor","valor":"30123456789","presencia":"PRESENTE","confianza":0.9,"pagina":1},
						{"clave":"conformidad","valor":"SI","presencia":"PRESENTE","confianza":0.9,"pagina":1},
						{"clave":"fechaEmision","valor":"2026-08-20","presencia":"PRESENTE","confianza":0.9,"pagina":1}
						""");

		ResultadoExtraccionModel resultado = proveedor.extraer(solicitud());

		assertThat(resultado.getTokensEntrada()).isEqualTo(1000);
		assertThat(resultado.getTokensSalida()).isEqualTo(500);
		assertThat(resultado.getCosto()).isEqualByComparingTo(new BigDecimal("0.000400"));
		assertThat(resultado.getMonedaCosto()).isEqualTo("USD");
	}

	@Test
	@DisplayName("aplica el factor de calibracion y conserva la confianza cruda del proveedor")
	void calibraConfianza() {
		responderConCampos("""
				{"clave":"numeroRemito","valor":"A-1","presencia":"PRESENTE","confianza":0.90,"pagina":1},
				{"clave":"cuitEmisor","valor":"30123456789","presencia":"PRESENTE","confianza":0.90,"pagina":1},
				{"clave":"conformidad","valor":"SI","presencia":"PRESENTE","confianza":0.90,"pagina":1},
				{"clave":"fechaEmision","valor":"2026-08-20","presencia":"PRESENTE","confianza":0.90,"pagina":1}
				""");

		ValorCanonicoModel valor = buscar(proveedor.extraer(solicitud()), "numeroRemito");

		assertThat(valor.getConfianzaProveedor()).isEqualByComparingTo(new BigDecimal("0.9000"));
		assertThat(valor.getConfianza()).isEqualByComparingTo(new BigDecimal("0.8100"));
	}

	@Test
	@DisplayName("un campo que el proveedor no devuelve queda ILEGIBLE con advertencia, nunca NO_FIGURA")
	void completaCampoFaltante() {
		responderConCampos("""
				{"clave":"numeroRemito","valor":"A-1","presencia":"PRESENTE","confianza":0.9,"pagina":1}
				""");

		ResultadoExtraccionModel resultado = proveedor.extraer(solicitud());

		assertThat(resultado.getValores()).hasSize(4);
		assertThat(buscar(resultado, "cuitEmisor").getPresencia()).isEqualTo(PresenciaCampo.ILEGIBLE);
		assertThat(buscar(resultado, "cuitEmisor").getConfianza()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(resultado.getAdvertencias()).anyMatch(a -> a.contains("no devolvio el campo cuitEmisor"));
	}

	@Test
	@DisplayName("descarta un campo que no fue solicitado y lo deja como advertencia")
	void descartaCampoNoSolicitado() {
		responderConCampos("""
				{"clave":"numeroRemito","valor":"A-1","presencia":"PRESENTE","confianza":0.9,"pagina":1},
				{"clave":"cuitEmisor","valor":"30123456789","presencia":"PRESENTE","confianza":0.9,"pagina":1},
				{"clave":"conformidad","valor":"SI","presencia":"PRESENTE","confianza":0.9,"pagina":1},
				{"clave":"fechaEmision","valor":"2026-08-20","presencia":"PRESENTE","confianza":0.9,"pagina":1},
				{"clave":"campoInventado","valor":"x","presencia":"PRESENTE","confianza":0.9,"pagina":1}
				""");

		ResultadoExtraccionModel resultado = proveedor.extraer(solicitud());

		assertThat(resultado.getValores()).hasSize(4);
		assertThat(resultado.getValores()).noneMatch(v -> "campoInventado".equals(v.getClaveCampo()));
		assertThat(resultado.getAdvertencias()).anyMatch(a -> a.contains("campoInventado"));
	}

	@Test
	@DisplayName("un valor que no se puede normalizar degrada la presencia a ILEGIBLE")
	void degradaValorNoNormalizable() {
		responderConCampos("""
				{"clave":"numeroRemito","valor":"A-1","presencia":"PRESENTE","confianza":0.9,"pagina":1},
				{"clave":"cuitEmisor","valor":"30123456789","presencia":"PRESENTE","confianza":0.9,"pagina":1},
				{"clave":"conformidad","valor":"SI","presencia":"PRESENTE","confianza":0.9,"pagina":1},
				{"clave":"fechaEmision","valor":"no se lee","presencia":"PRESENTE","confianza":0.9,"pagina":1}
				""");

		assertThat(buscar(proveedor.extraer(solicitud()), "fechaEmision").getPresencia())
				.isEqualTo(PresenciaCampo.ILEGIBLE);
	}

	@Test
	@DisplayName("el 429 de cuota es reintentable para que el router use respaldo y backoff")
	void cuotaExcedidaEsReintentable() {
		codigoRespuesta.set(429);
		cuerpoRespuesta.set("{\"error\":{\"message\":\"Quota exceeded\"}}");

		assertThatThrownBy(() -> proveedor.extraer(solicitud()))
				.isInstanceOf(ProveedorNoDisponibleException.class)
				.matches(e -> ((ProveedorNoDisponibleException) e).esReintentable())
				.hasMessageContaining(GeminiCliente.CODIGO_CUOTA_EXCEDIDA);
	}

	@Test
	@DisplayName("una credencial invalida no es reintentable para no consumir la cola de reintentos")
	void credencialInvalidaNoEsReintentable() {
		codigoRespuesta.set(403);
		cuerpoRespuesta.set("{\"error\":{\"message\":\"API key not valid\"}}");

		assertThatThrownBy(() -> proveedor.extraer(solicitud()))
				.isInstanceOf(ProveedorNoDisponibleException.class)
				.matches(e -> !((ProveedorNoDisponibleException) e).esReintentable())
				.hasMessageContaining(GeminiCliente.CODIGO_CREDENCIAL_INVALIDA);
	}

	@Test
	@DisplayName("un 500 del proveedor es reintentable")
	void erroresDelServidorSonReintentables() {
		codigoRespuesta.set(503);
		cuerpoRespuesta.set("{\"error\":{\"message\":\"Service unavailable\"}}");

		assertThatThrownBy(() -> proveedor.extraer(solicitud()))
				.isInstanceOf(ProveedorNoDisponibleException.class)
				.matches(e -> ((ProveedorNoDisponibleException) e).esReintentable());
	}

	@Test
	@DisplayName("la solicitud lleva el esquema estructurado, temperatura cero y el documento adjunto")
	void construyeLaSolicitudEsperada() {
		responderConCampos("""
				{"clave":"numeroRemito","valor":"A-1","presencia":"PRESENTE","confianza":0.9,"pagina":1},
				{"clave":"cuitEmisor","valor":"30123456789","presencia":"PRESENTE","confianza":0.9,"pagina":1},
				{"clave":"conformidad","valor":"SI","presencia":"PRESENTE","confianza":0.9,"pagina":1},
				{"clave":"fechaEmision","valor":"2026-08-20","presencia":"PRESENTE","confianza":0.9,"pagina":1}
				""");

		proveedor.extraer(solicitud());

		String enviado = cuerpoRecibido.get();
		assertThat(enviado).contains("\"responseMimeType\":\"application/json\"");
		assertThat(enviado).contains("\"temperature\":0.0");
		assertThat(enviado).contains("\"inline_data\"");
		assertThat(enviado).contains("PRESENTE").contains("NO_FIGURA").contains("ILEGIBLE");
		assertThat(enviado).contains("numeroRemito").contains("cuitEmisor");
	}

	private ConfiguracionProveedor configuracion() {
		ConfiguracionProveedor configuracion = new ConfiguracionProveedor();
		configuracion.setProveedor(ProveedorDocumentalIa.GEMINI);
		configuracion.setModelo("gemini-2.5-flash");
		configuracion.setReferenciaSecreto("literal:" + CLAVE);
		configuracion.setParametros("""
				{"costoPorMillonEntrada":"0.10","costoPorMillonSalida":"0.60",\
				"factorCalibracionConfianza":"0.90"}""");
		configuracion.setActiva(true);
		return configuracion;
	}

	private SolicitudExtraccionModel solicitud() {
		SolicitudExtraccionModel solicitud = new SolicitudExtraccionModel();
		solicitud.setTenantId(TENANT);
		solicitud.setDocumentoId("doc-1");
		solicitud.setNombreArchivo("remito.pdf");
		solicitud.setTipoMime("application/pdf");
		solicitud.setContenido("contenido de prueba".getBytes(StandardCharsets.UTF_8));
		solicitud.setPaginas(2);
		solicitud.setCodigoPlantilla("REMITO");
		solicitud.setVersionPrompt("p1");
		solicitud.setVersionEsquema("e1");
		solicitud.getCampos().add(campo("numeroRemito", "Numero de remito", TipoDatoCampo.TEXTO, true));
		solicitud.getCampos().add(campo("cuitEmisor", "CUIT del emisor", TipoDatoCampo.CUIT, true));
		solicitud.getCampos().add(campo("conformidad", "Conformidad", TipoDatoCampo.BOOLEANO, false));
		solicitud.getCampos().add(campo("fechaEmision", "Fecha de emision", TipoDatoCampo.FECHA, true));
		return solicitud;
	}

	private CampoEsquemaModel campo(String clave, String etiqueta, TipoDatoCampo tipoDato, boolean requerido) {
		CampoEsquemaModel campo = new CampoEsquemaModel();
		campo.setClave(clave);
		campo.setEtiqueta(etiqueta);
		campo.setTipoDato(tipoDato);
		campo.setRequerido(requerido);
		return campo;
	}

	private void responderConCampos(String campos) {
		String contenido = "{\"tipoDetectado\":\"REMITO\",\"campos\":[" + campos.trim() + "]}";
		cuerpoRespuesta.set("""
				{"candidates":[{"finishReason":"STOP","content":{"parts":[{"text":%s}]}}],\
				"usageMetadata":{"promptTokenCount":1000,"candidatesTokenCount":500,"totalTokenCount":1500}}"""
				.formatted(escapar(contenido)));
	}

	private String escapar(String texto) {
		try {
			return objectMapper.writeValueAsString(texto);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private ValorCanonicoModel buscar(ResultadoExtraccionModel resultado, String clave) {
		return resultado.getValores().stream().filter(v -> clave.equals(v.getClaveCampo())).findFirst()
				.orElseThrow(() -> new IllegalStateException("No vino el campo " + clave));
	}
}
