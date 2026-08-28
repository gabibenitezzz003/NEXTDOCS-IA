package com.nextdocs.ai.integracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.nextdocs.ai.entidades.EventoSalida;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.EstadoEntregaWebhook;
import com.nextdocs.ai.enumeraciones.TipoEventoCanonico;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.EntregaWebhookModel;
import com.nextdocs.ai.modelos.FiltroAuditoriaModel;
import com.nextdocs.ai.modelos.SaludIntegracionModel;
import com.nextdocs.ai.modelos.SuscripcionWebhookCreadaModel;
import com.nextdocs.ai.modelos.SuscripcionWebhookModel;
import com.nextdocs.ai.modelos.SuscripcionWebhookReqModel;
import com.nextdocs.ai.servicios.EntregaWebhookService;
import com.nextdocs.ai.servicios.EventoSalidaService;
import com.nextdocs.ai.servicios.GobernanzaService;
import com.nextdocs.ai.servicios.IntegracionService;
import com.nextdocs.ai.utiles.Hash;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

class IntegracionIT extends PruebaIntegracion {

	@Autowired
	private FabricaDatosPrueba fabrica;

	@Autowired
	private IntegracionService integracionService;

	@Autowired
	private EntregaWebhookService entregaWebhookService;

	@Autowired
	private EventoSalidaService eventoSalidaService;

	@Autowired
	private GobernanzaService gobernanzaService;

	private Tenant tenant;

	private HttpServer servidor;

	private AtomicInteger codigoRespuesta;

	private AtomicInteger llamadas;

	private AtomicReference<String> cuerpoRecibido;

	private AtomicReference<String> firmaRecibida;

	private AtomicReference<String> eventoRecibido;

	private AtomicReference<String> entregaRecibida;

	@BeforeEach
	void preparar() throws IOException {
		tenant = fabrica.crearTenant("t" + UUID.randomUUID().toString().substring(0, 8));
		codigoRespuesta = new AtomicInteger(200);
		llamadas = new AtomicInteger();
		cuerpoRecibido = new AtomicReference<>();
		firmaRecibida = new AtomicReference<>();
		eventoRecibido = new AtomicReference<>();
		entregaRecibida = new AtomicReference<>();
		servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		servidor.createContext("/hook", intercambio -> {
			llamadas.incrementAndGet();
			cuerpoRecibido.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			firmaRecibida.set(intercambio.getRequestHeaders().getFirst(EntregaWebhookService.CABECERA_FIRMA));
			eventoRecibido.set(intercambio.getRequestHeaders().getFirst(EntregaWebhookService.CABECERA_EVENTO));
			entregaRecibida.set(intercambio.getRequestHeaders().getFirst(EntregaWebhookService.CABECERA_ENTREGA));
			byte[] cuerpo = "ok".getBytes(StandardCharsets.UTF_8);
			intercambio.sendResponseHeaders(codigoRespuesta.get(), cuerpo.length);
			try (OutputStream salida = intercambio.getResponseBody()) {
				salida.write(cuerpo);
			}
		});
		servidor.start();
	}

	@AfterEach
	void bajar() {
		if (servidor != null) {
			servidor.stop(0);
		}
	}

	@Test
	@DisplayName("el alta de una suscripcion muestra el secreto una sola vez y el listado solo el prefijo")
	void altaMuestraSecretoUnaVez() {
		SuscripcionWebhookCreadaModel creada = suscribir("Monitor Follow", TipoEventoCanonico.DOCUMENTO_APROBADO);

		assertThat(creada.getSecreto()).startsWith("ndwh_");
		assertThat(creada.getAdvertencia()).contains("sola vez");
		assertThat(creada.getSuscripcion().getUrl()).contains("127.0.0.1");
		assertThat(creada.getSuscripcion().isActiva()).isTrue();
		assertThat(creada.getSuscripcion().getPrefijoSecreto()).isEqualTo(creada.getSecreto().substring(0, 8));

		List<SuscripcionWebhookModel> listado = integracionService.listar(tenant.getId());
		assertThat(listado).extracting(SuscripcionWebhookModel::getId)
				.contains(creada.getSuscripcion().getId());
		assertThat(listado.get(0).getPrefijoSecreto()).isEqualTo(creada.getSuscripcion().getPrefijoSecreto());
	}

	@Test
	@DisplayName("una URL HTTP publica se rechaza y una IP privada tambien")
	void urlInsegura() {
		SuscripcionWebhookReqModel http = pedido("http://8.8.8.8/hook", TipoEventoCanonico.DOCUMENTO_CERRADO);
		http.setNombre("Insegura");
		assertThatThrownBy(() -> integracionService.crear(tenant, http)).isInstanceOf(ValidacionException.class)
				.hasMessageContaining("HTTPS");

		SuscripcionWebhookReqModel privada = pedido("https://192.168.10.4/hook", TipoEventoCanonico.DOCUMENTO_CERRADO);
		privada.setNombre("Privada");
		assertThatThrownBy(() -> integracionService.crear(tenant, privada)).isInstanceOf(ValidacionException.class)
				.hasMessageContaining("red interna");
	}

	@Test
	@DisplayName("el ping entrega webhook.test firmado con HMAC y no reexpone el secreto")
	void pingFirmaHmac() {
		SuscripcionWebhookCreadaModel creada = suscribir("Ping", TipoEventoCanonico.DOCUMENTO_APROBADO);

		EntregaWebhookModel entrega = integracionService.probar(tenant.getId(), creada.getSuscripcion().getId());

		assertThat(entrega.getEstado()).isEqualTo(EstadoEntregaWebhook.ENTREGADO);
		assertThat(entrega.getTipoEvento()).isEqualTo(TipoEventoCanonico.WEBHOOK_PRUEBA);
		assertThat(entrega.getCodigoRespuesta()).isEqualTo(200);
		assertThat(llamadas.get()).isEqualTo(1);
		assertThat(eventoRecibido.get()).isEqualTo("webhook.test");
		assertThat(entregaRecibida.get()).isEqualTo(entrega.getId());
		assertThat(firmaRecibida.get()).isEqualTo(Hash.hmacSha256(creada.getSecreto(), cuerpoRecibido.get()));
		assertThat(integracionService.obtener(tenant.getId(), creada.getSuscripcion().getId()).getPrefijoSecreto())
				.isEqualTo(creada.getSecreto().substring(0, 8));
	}

	@Test
	@DisplayName("el despacho fan-out entrega el evento canonico a la suscripcion activa")
	void despachoFanOut() {
		SuscripcionWebhookCreadaModel creada = suscribir("Fan-out", TipoEventoCanonico.DOCUMENTO_APROBADO);
		EventoSalida evento = eventoSalidaService.publicar(tenant.getId(), TipoEventoCanonico.DOCUMENTO_APROBADO,
				"Documento", "doc-1", Map.of("documentoId", "doc-1", "estado", "APROBADO"));

		entregaWebhookService.distribuir(evento.getId());

		assertThat(llamadas.get()).isEqualTo(1);
		assertThat(eventoRecibido.get()).isEqualTo("document.approved");
		assertThat(firmaRecibida.get()).isEqualTo(Hash.hmacSha256(creada.getSecreto(), cuerpoRecibido.get()));
		Page<EntregaWebhookModel> entregas = integracionService.listarEntregas(tenant.getId(),
				EstadoEntregaWebhook.ENTREGADO, null, PageRequest.of(0, 10));
		assertThat(entregas.getContent()).extracting(EntregaWebhookModel::getTipoEvento)
				.contains(TipoEventoCanonico.DOCUMENTO_APROBADO);
	}

	@Test
	@DisplayName("tres fallos consecutivos pausan la suscripcion y dejan de despachar")
	void autoPausaPorFallos() {
		codigoRespuesta.set(500);
		SuscripcionWebhookCreadaModel creada = suscribir("Fragil", TipoEventoCanonico.DOCUMENTO_APROBADO);
		String id = creada.getSuscripcion().getId();

		integracionService.probar(tenant.getId(), id);
		integracionService.probar(tenant.getId(), id);
		EntregaWebhookModel tercera = integracionService.probar(tenant.getId(), id);

		SuscripcionWebhookModel pausada = integracionService.obtener(tenant.getId(), id);
		assertThat(pausada.isActiva()).isFalse();
		assertThat(pausada.isPausadaPorFallos()).isTrue();
		assertThat(pausada.getFallosConsecutivos()).isGreaterThanOrEqualTo(3);
		assertThat(tercera.getEstado()).isIn(EstadoEntregaWebhook.PENDIENTE, EstadoEntregaWebhook.AGOTADO);

		codigoRespuesta.set(200);
		EventoSalida evento = eventoSalidaService.publicar(tenant.getId(), TipoEventoCanonico.DOCUMENTO_APROBADO,
				"Documento", "doc-2", Map.of("documentoId", "doc-2"));
		int antes = llamadas.get();
		entregaWebhookService.distribuir(evento.getId());
		assertThat(llamadas.get()).isEqualTo(antes);
	}

	@Test
	@DisplayName("el reintento manual de una entrega agotada vuelve a pegarle al endpoint")
	void reintentoManual() {
		codigoRespuesta.set(500);
		SuscripcionWebhookCreadaModel creada = suscribir("Reintento", TipoEventoCanonico.DOCUMENTO_APROBADO);
		EventoSalida evento = eventoSalidaService.publicar(tenant.getId(), TipoEventoCanonico.DOCUMENTO_APROBADO,
				"Documento", "doc-3", Map.of("documentoId", "doc-3"));
		entregaWebhookService.distribuir(evento.getId());
		Page<EntregaWebhookModel> fallidas = integracionService.listarEntregas(tenant.getId(), null,
				creada.getSuscripcion().getId(), PageRequest.of(0, 10));
		EntregaWebhookModel original = fallidas.getContent().get(0);
		assertThat(original.getEstado()).isEqualTo(EstadoEntregaWebhook.PENDIENTE);

		original = integracionService.reintentar(tenant.getId(), original.getId());
		original = integracionService.reintentar(tenant.getId(), original.getId());
		assertThat(original.getEstado()).isEqualTo(EstadoEntregaWebhook.AGOTADO);

		codigoRespuesta.set(200);
		EntregaWebhookModel recuperada = integracionService.reintentar(tenant.getId(), original.getId());
		assertThat(recuperada.getEstado()).isEqualTo(EstadoEntregaWebhook.ENTREGADO);
		assertThat(recuperada.getCodigoRespuesta()).isEqualTo(200);
	}

	@Test
	@DisplayName("el monitor de salud cuenta suscripciones y entregas del tenant")
	void saludDelTenant() {
		suscribir("Salud", TipoEventoCanonico.DOCUMENTO_CERRADO);
		integracionService.probar(tenant.getId(), integracionService.listar(tenant.getId()).get(0).getId());

		SaludIntegracionModel salud = integracionService.salud(tenant.getId());
		assertThat(salud.getSuscripcionesTotales()).isEqualTo(1);
		assertThat(salud.getSuscripcionesActivas()).isEqualTo(1);
		assertThat(salud.getEntregasEntregadas()).isEqualTo(1);
		assertThat(salud.getUmbralPausa()).isEqualTo(3);
	}

	@Test
	@DisplayName("SEC-01: un tenant no ve ni toca las suscripciones de otro")
	void aislamientoPorTenant() {
		SuscripcionWebhookCreadaModel creada = suscribir("Ajena", TipoEventoCanonico.DOCUMENTO_APROBADO);
		Tenant ajeno = fabrica.crearTenant("x" + UUID.randomUUID().toString().substring(0, 8));

		assertThat(integracionService.listar(ajeno.getId())).isEmpty();
		assertThatThrownBy(() -> integracionService.obtener(ajeno.getId(), creada.getSuscripcion().getId()))
				.isInstanceOf(EntidadNoEncontradaException.class);
		assertThatThrownBy(() -> integracionService.probar(ajeno.getId(), creada.getSuscripcion().getId()))
				.isInstanceOf(EntidadNoEncontradaException.class);
		assertThat(integracionService.salud(ajeno.getId()).getSuscripcionesTotales()).isZero();
	}

	@Test
	@DisplayName("pausar, reactivar, rotar el secreto y eliminar quedan auditados")
	void cicloAdministrativoAuditado() {
		SuscripcionWebhookCreadaModel creada = suscribir("Ciclo", TipoEventoCanonico.EXCEPCION_CREADA);
		String id = creada.getSuscripcion().getId();

		integracionService.pausar(tenant.getId(), id);
		assertThat(integracionService.obtener(tenant.getId(), id).isActiva()).isFalse();
		integracionService.reactivar(tenant.getId(), id);
		assertThat(integracionService.obtener(tenant.getId(), id).isActiva()).isTrue();
		SuscripcionWebhookCreadaModel rotada = integracionService.rotarSecreto(tenant.getId(), id);
		assertThat(rotada.getSecreto()).isNotEqualTo(creada.getSecreto());
		integracionService.eliminar(tenant.getId(), id);
		assertThatThrownBy(() -> integracionService.obtener(tenant.getId(), id))
				.isInstanceOf(EntidadNoEncontradaException.class);

		FiltroAuditoriaModel filtro = new FiltroAuditoriaModel();
		filtro.setAccion(AccionAuditoria.WEBHOOK_SUSCRITO);
		assertThat(gobernanzaService.consultar(tenant.getId(), filtro, PageRequest.of(0, 20)).getContent())
				.isNotEmpty();
		filtro.setAccion(AccionAuditoria.WEBHOOK_ELIMINADO);
		assertThat(gobernanzaService.consultar(tenant.getId(), filtro, PageRequest.of(0, 20)).getContent())
				.isNotEmpty();
	}

	@Test
	@DisplayName("una entrega ya confirmada no se reintenta")
	void noReintentaEntregado() {
		SuscripcionWebhookCreadaModel creada = suscribir("Ok", TipoEventoCanonico.DOCUMENTO_APROBADO);
		EntregaWebhookModel entrega = integracionService.probar(tenant.getId(), creada.getSuscripcion().getId());
		assertThatThrownBy(() -> integracionService.reintentar(tenant.getId(), entrega.getId()))
				.isInstanceOf(ValidacionException.class);
	}

	private SuscripcionWebhookCreadaModel suscribir(String nombre, TipoEventoCanonico evento) {
		return integracionService.crear(tenant, pedido(urlLocal(), evento, nombre));
	}

	private SuscripcionWebhookReqModel pedido(String url, TipoEventoCanonico evento) {
		return pedido(url, evento, "Suscripcion");
	}

	private SuscripcionWebhookReqModel pedido(String url, TipoEventoCanonico evento, String nombre) {
		SuscripcionWebhookReqModel datos = new SuscripcionWebhookReqModel();
		datos.setNombre(nombre);
		datos.setUrl(url);
		datos.getEventos().add(evento);
		return datos;
	}

	private String urlLocal() {
		return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/hook";
	}
}
