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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdocs.ai.clientes.DeepSeekCliente;
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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProveedorDeepSeekServiceTest {

	private static final String TENANT = "tenant-prueba";

	private final ObjectMapper objectMapper = new ObjectMapper();

	private HttpServer servidor;

	private AtomicInteger codigoRespuesta;

	private AtomicReference<String> cuerpoRespuesta;

	private AtomicReference<String> cuerpoRecibido;

	private ProveedorDeepSeekService proveedor;

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

		ConfiguracionProveedorRepository repositorio = mock(ConfiguracionProveedorRepository.class);
		when(repositorio.buscarPorProveedor(TENANT, ProveedorDocumentalIa.DEEPSEEK))
				.thenReturn(Optional.of(configuracion()));
		proveedor = new ProveedorDeepSeekService(new DeepSeekCliente(objectMapper, "http://no-usar", 10), repositorio,
				objectMapper);
	}

	@AfterEach
	void bajarServidor() {
		servidor.stop(0);
	}

	@Test
	@DisplayName("devuelve el mismo contrato canonico que Gemini, con presencias y normalizacion")
	void contratoCanonicoIgualAGemini() {
		responder("""
				{"clave":"numeroRemito","valor":"R-0001","presencia":"PRESENTE","confianza":0.95,"pagina":1},
				{"clave":"cuitEmisor","valor":"30-71234567-4","presencia":"PRESENTE","confianza":0.90,"pagina":1},
				{"clave":"conformidad","valor":"","presencia":"NO_FIGURA","confianza":1.0,"pagina":1}
				""");

		ResultadoExtraccionModel resultado = proveedor.extraer(solicitud());

		assertThat(resultado.getProveedor()).isEqualTo(ProveedorDocumentalIa.DEEPSEEK);
		assertThat(resultado.getValores()).hasSize(3);
		assertThat(buscar(resultado, "cuitEmisor").getValorNormalizado()).isEqualTo("30712345674");
		assertThat(buscar(resultado, "conformidad").getPresencia()).isEqualTo(PresenciaCampo.NO_FIGURA);
		assertThat(resultado.getAdvertencias()).isEmpty();
	}

	@Test
	@DisplayName("pide JSON estructurado, temperatura cero y manda el texto del documento")
	void construyeLaSolicitudEsperada() {
		responder("""
				{"clave":"numeroRemito","valor":"R-0001","presencia":"PRESENTE","confianza":0.9,"pagina":1},
				{"clave":"cuitEmisor","valor":"30712345674","presencia":"PRESENTE","confianza":0.9,"pagina":1},
				{"clave":"conformidad","valor":"SI","presencia":"PRESENTE","confianza":0.9,"pagina":1}
				""");

		proveedor.extraer(solicitud());

		String enviado = cuerpoRecibido.get();
		assertThat(enviado).contains("\"type\":\"json_object\"");
		assertThat(enviado).contains("\"temperature\":0.0");
		assertThat(enviado).contains("PRESENTE").contains("NO_FIGURA").contains("ILEGIBLE");
		assertThat(enviado).contains("REMITO DE PRUEBA");
	}

	@Test
	@DisplayName("un campo que no vuelve queda ILEGIBLE con advertencia, igual que en Gemini")
	void completaCampoFaltante() {
		responder("""
				{"clave":"numeroRemito","valor":"R-0001","presencia":"PRESENTE","confianza":0.9,"pagina":1}
				""");

		ResultadoExtraccionModel resultado = proveedor.extraer(solicitud());

		assertThat(resultado.getValores()).hasSize(3);
		assertThat(buscar(resultado, "cuitEmisor").getPresencia()).isEqualTo(PresenciaCampo.ILEGIBLE);
		assertThat(resultado.getAdvertencias()).anyMatch(a -> a.contains("no devolvio el campo cuitEmisor"));
	}

	@Test
	@DisplayName("el 429 es reintentable para que el router siga buscando alternativa")
	void cuotaExcedidaEsReintentable() {
		codigoRespuesta.set(429);
		cuerpoRespuesta.set("{\"error\":{\"message\":\"Rate limit\"}}");

		assertThatThrownBy(() -> proveedor.extraer(solicitud()))
				.isInstanceOf(ProveedorNoDisponibleException.class)
				.matches(e -> ((ProveedorNoDisponibleException) e).esReintentable())
				.hasMessageContaining(DeepSeekCliente.CODIGO_CUOTA_EXCEDIDA);
	}

	@Test
	@DisplayName("una credencial invalida no es reintentable")
	void credencialInvalidaNoEsReintentable() {
		codigoRespuesta.set(401);
		cuerpoRespuesta.set("{\"error\":{\"message\":\"Invalid key\"}}");

		assertThatThrownBy(() -> proveedor.extraer(solicitud()))
				.isInstanceOf(ProveedorNoDisponibleException.class)
				.matches(e -> !((ProveedorNoDisponibleException) e).esReintentable());
	}

	@Test
	@DisplayName("un PDF sin capa de texto se rechaza sin reintento y explica que necesita vision")
	void pdfEscaneadoSeRechazaConMensajeClaro() {
		SolicitudExtraccionModel solicitud = solicitud();
		solicitud.setContenido(pdfSinTexto());

		assertThatThrownBy(() -> proveedor.extraer(solicitud))
				.isInstanceOf(ProveedorNoDisponibleException.class)
				.matches(e -> !((ProveedorNoDisponibleException) e).esReintentable())
				.hasMessageContaining("necesita un proveedor con vision");
	}

	@Test
	@DisplayName("calcula el costo con los precios configurados")
	void calculaCosto() {
		responder("""
				{"clave":"numeroRemito","valor":"R-1","presencia":"PRESENTE","confianza":0.9,"pagina":1},
				{"clave":"cuitEmisor","valor":"30712345674","presencia":"PRESENTE","confianza":0.9,"pagina":1},
				{"clave":"conformidad","valor":"SI","presencia":"PRESENTE","confianza":0.9,"pagina":1}
				""");

		ResultadoExtraccionModel resultado = proveedor.extraer(solicitud());

		assertThat(resultado.getTokensEntrada()).isEqualTo(1000);
		assertThat(resultado.getTokensSalida()).isEqualTo(500);
		assertThat(resultado.getCosto()).isEqualByComparingTo(new BigDecimal("0.000170"));
	}

	private ConfiguracionProveedor configuracion() {
		ConfiguracionProveedor configuracion = new ConfiguracionProveedor();
		configuracion.setProveedor(ProveedorDocumentalIa.DEEPSEEK);
		configuracion.setModelo("deepseek-chat");
		configuracion.setReferenciaSecreto("literal:clave-deepseek");
		configuracion.setParametros("{\"urlBase\":\"http://127.0.0.1:" + servidor.getAddress().getPort()
				+ "\",\"costoPorMillonEntrada\":\"0.14\",\"costoPorMillonSalida\":\"0.06\"}");
		configuracion.setActiva(true);
		return configuracion;
	}

	private SolicitudExtraccionModel solicitud() {
		SolicitudExtraccionModel solicitud = new SolicitudExtraccionModel();
		solicitud.setTenantId(TENANT);
		solicitud.setDocumentoId("doc-1");
		solicitud.setNombreArchivo("remito.pdf");
		solicitud.setTipoMime("application/pdf");
		solicitud.setContenido(pdfConTexto());
		solicitud.setPaginas(1);
		solicitud.setCodigoPlantilla("REMITO");
		solicitud.setVersionPrompt("p1");
		solicitud.setVersionEsquema("e1");
		solicitud.getCampos().add(campo("numeroRemito", TipoDatoCampo.TEXTO));
		solicitud.getCampos().add(campo("cuitEmisor", TipoDatoCampo.CUIT));
		solicitud.getCampos().add(campo("conformidad", TipoDatoCampo.TEXTO));
		return solicitud;
	}

	private CampoEsquemaModel campo(String clave, TipoDatoCampo tipoDato) {
		CampoEsquemaModel campo = new CampoEsquemaModel();
		campo.setClave(clave);
		campo.setEtiqueta("Etiqueta de " + clave);
		campo.setTipoDato(tipoDato);
		campo.setRequerido(true);
		return campo;
	}

	private byte[] pdfConTexto() {
		return construirPdf("REMITO DE PRUEBA R-0001");
	}

	private byte[] pdfSinTexto() {
		try (PDDocument documento = new PDDocument()) {
			documento.addPage(new PDPage(PDRectangle.A4));
			java.io.ByteArrayOutputStream salida = new java.io.ByteArrayOutputStream();
			documento.save(salida);
			return salida.toByteArray();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private byte[] construirPdf(String texto) {
		try (PDDocument documento = new PDDocument()) {
			PDPage pagina = new PDPage(PDRectangle.A4);
			documento.addPage(pagina);
			try (PDPageContentStream contenido = new PDPageContentStream(documento, pagina)) {
				contenido.beginText();
				contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 14);
				contenido.newLineAtOffset(50, 780);
				contenido.showText(texto);
				contenido.endText();
			}
			java.io.ByteArrayOutputStream salida = new java.io.ByteArrayOutputStream();
			documento.save(salida);
			return salida.toByteArray();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void responder(String campos) {
		String contenido = "{\"tipoDetectado\":\"REMITO\",\"campos\":[" + campos.trim() + "]}";
		cuerpoRespuesta.set("""
				{"choices":[{"message":{"role":"assistant","content":%s}}],\
				"usage":{"prompt_tokens":1000,"completion_tokens":500}}""".formatted(escapar(contenido)));
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
