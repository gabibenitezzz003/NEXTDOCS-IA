package com.nextdocs.ai.servicios;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.nextdocs.ai.config.PropiedadesWebhooks;
import com.nextdocs.ai.entidades.EntregaWebhook;
import com.nextdocs.ai.entidades.EventoSalida;
import com.nextdocs.ai.entidades.SuscripcionWebhook;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.EstadoEntregaWebhook;
import com.nextdocs.ai.enumeraciones.EstadoEventoSalida;
import com.nextdocs.ai.repositorios.EntregaWebhookRepository;
import com.nextdocs.ai.repositorios.EventoSalidaRepository;
import com.nextdocs.ai.repositorios.SuscripcionWebhookRepository;
import com.nextdocs.ai.utiles.Hash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DespachadorEventosService {

	public static final String CABECERA_FIRMA = "X-Nextdocs-Firma";

	public static final String CABECERA_EVENTO = "X-Nextdocs-Evento";

	public static final String CABECERA_ENTREGA = "X-Nextdocs-Entrega";

	private static final Logger log = LoggerFactory.getLogger(DespachadorEventosService.class);

	private static final int LOTE = 50;

	private final EventoSalidaRepository eventoSalidaRepository;

	private final SuscripcionWebhookRepository suscripcionWebhookRepository;

	private final EntregaWebhookRepository entregaWebhookRepository;

	private final AuditoriaService auditoriaService;

	private final PropiedadesWebhooks propiedades;

	private final HttpClient clienteHttp;

	public DespachadorEventosService(EventoSalidaRepository eventoSalidaRepository,
			SuscripcionWebhookRepository suscripcionWebhookRepository,
			EntregaWebhookRepository entregaWebhookRepository, AuditoriaService auditoriaService,
			PropiedadesWebhooks propiedades) {
		this.eventoSalidaRepository = eventoSalidaRepository;
		this.suscripcionWebhookRepository = suscripcionWebhookRepository;
		this.entregaWebhookRepository = entregaWebhookRepository;
		this.auditoriaService = auditoriaService;
		this.propiedades = propiedades;
		this.clienteHttp = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(propiedades.getTiempoEsperaSegundos())).build();
	}

	@Scheduled(fixedDelay = 3000)
	@Transactional
	public void despachar() {
		List<EventoSalida> pendientes = eventoSalidaRepository.listarPendientes(EstadoEventoSalida.PENDIENTE,
				Instant.now(), PageRequest.of(0, LOTE));
		for (EventoSalida evento : pendientes) {
			procesar(evento);
		}
	}

	@Scheduled(fixedDelay = 10000)
	@Transactional
	public void reintentarEntregas() {
		List<EntregaWebhook> pendientes = entregaWebhookRepository.listarPendientes(EstadoEntregaWebhook.PENDIENTE,
				Instant.now(), PageRequest.of(0, LOTE));
		for (EntregaWebhook entrega : pendientes) {
			entregar(entrega, entrega.getSuscripcion(),
					eventoSalidaRepository.findById(entrega.getEventoId()).orElse(null));
		}
	}

	private void procesar(EventoSalida evento) {
		try {
			List<SuscripcionWebhook> suscripciones = suscripcionWebhookRepository
					.listarActivasPorEvento(evento.getTenantId(), evento.getTipoEvento());
			for (SuscripcionWebhook suscripcion : suscripciones) {
				crearEntregaSiCorresponde(evento, suscripcion);
			}
			evento.setEstado(EstadoEventoSalida.ENVIADO);
			evento.setProcesado(Instant.now());
			eventoSalidaRepository.save(evento);
		} catch (Exception e) {
			log.error("No se pudo despachar el evento {}", evento.getId(), e);
			evento.setIntento(evento.getIntento() + 1);
			evento.setUltimoError(e.getMessage());
			evento.setEstado(evento.getIntento() >= propiedades.getIntentosMaximos() ? EstadoEventoSalida.DESCARTADO
					: EstadoEventoSalida.PENDIENTE);
			evento.setDisponibleEn(Instant.now().plus(esperaMinutos(evento.getIntento()), ChronoUnit.MINUTES));
			eventoSalidaRepository.save(evento);
		}
	}

	private void crearEntregaSiCorresponde(EventoSalida evento, SuscripcionWebhook suscripcion) {
		if (entregaWebhookRepository.buscarPorSuscripcionYEvento(suscripcion.getId(), evento.getId()).isPresent()) {
			return;
		}
		EntregaWebhook entrega = new EntregaWebhook();
		entrega.setTenant(suscripcion.getTenant());
		entrega.setSuscripcion(suscripcion);
		entrega.setEventoId(evento.getId());
		entrega.setEstado(EstadoEntregaWebhook.PENDIENTE);
		entrega.setDisponibleEn(Instant.now());
		entrega.setAlta(Instant.now());
		entregaWebhookRepository.save(entrega);
		entregar(entrega, suscripcion, evento);
	}

	private void entregar(EntregaWebhook entrega, SuscripcionWebhook suscripcion, EventoSalida evento) {
		if (evento == null || suscripcion == null) {
			entrega.setEstado(EstadoEntregaWebhook.AGOTADO);
			entregaWebhookRepository.save(entrega);
			return;
		}
		long inicio = System.currentTimeMillis();
		entrega.setIntento(entrega.getIntento() + 1);
		try {
			HttpRequest peticion = HttpRequest.newBuilder(URI.create(suscripcion.getUrl()))
					.timeout(Duration.ofSeconds(propiedades.getTiempoEsperaSegundos()))
					.header("Content-Type", "application/json")
					.header(CABECERA_EVENTO, evento.getTipoEvento().getClave())
					.header(CABECERA_ENTREGA, entrega.getId())
					.header(CABECERA_FIRMA, Hash.hmacSha256(suscripcion.getSecreto(), evento.getCarga()))
					.POST(HttpRequest.BodyPublishers.ofString(evento.getCarga()))
					.build();
			HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());
			entrega.setCodigoRespuesta(respuesta.statusCode());
			entrega.setCuerpoRespuesta(recortar(respuesta.body()));
			if (respuesta.statusCode() >= 200 && respuesta.statusCode() < 300) {
				marcarEntregado(entrega, suscripcion);
			} else {
				marcarFallo(entrega, suscripcion, "Codigo HTTP " + respuesta.statusCode());
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			marcarFallo(entrega, suscripcion, "Entrega interrumpida");
		} catch (Exception e) {
			marcarFallo(entrega, suscripcion, e.getMessage());
		} finally {
			entrega.setDuracionMilisegundos(System.currentTimeMillis() - inicio);
			entregaWebhookRepository.save(entrega);
		}
	}

	private void marcarEntregado(EntregaWebhook entrega, SuscripcionWebhook suscripcion) {
		entrega.setEstado(EstadoEntregaWebhook.ENTREGADO);
		entrega.setEntregado(Instant.now());
		suscripcion.setFallosConsecutivos(0);
		suscripcion.setUltimaEntrega(Instant.now());
		suscripcionWebhookRepository.save(suscripcion);
		auditoriaService.registrar(suscripcion.getTenant().getId(), AccionAuditoria.WEBHOOK_ENTREGADO,
				"EntregaWebhook", entrega.getId());
	}

	private void marcarFallo(EntregaWebhook entrega, SuscripcionWebhook suscripcion, String mensaje) {
		log.warn("Fallo la entrega {} de la suscripcion {}: {}", entrega.getId(), suscripcion.getId(), mensaje);
		if (entrega.getIntento() >= propiedades.getIntentosMaximos()) {
			entrega.setEstado(EstadoEntregaWebhook.AGOTADO);
		} else {
			entrega.setEstado(EstadoEntregaWebhook.PENDIENTE);
			entrega.setDisponibleEn(Instant.now().plus(esperaMinutos(entrega.getIntento()), ChronoUnit.MINUTES));
		}
		suscripcion.setFallosConsecutivos(suscripcion.getFallosConsecutivos() + 1);
		suscripcionWebhookRepository.save(suscripcion);
	}

	private long esperaMinutos(int intento) {
		return (long) Math.min(Math.pow(2, intento), 60);
	}

	private String recortar(String cuerpo) {
		if (cuerpo == null) {
			return null;
		}
		return cuerpo.length() > 2000 ? cuerpo.substring(0, 2000) : cuerpo;
	}
}
