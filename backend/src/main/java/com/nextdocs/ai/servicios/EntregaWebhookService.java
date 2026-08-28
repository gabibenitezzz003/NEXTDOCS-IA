package com.nextdocs.ai.servicios;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntregaWebhookService {

	public static final String CABECERA_FIRMA = "X-Nextdocs-Firma";

	public static final String CABECERA_EVENTO = "X-Nextdocs-Evento";

	public static final String CABECERA_ENTREGA = "X-Nextdocs-Entrega";

	private static final Logger log = LoggerFactory.getLogger(EntregaWebhookService.class);

	private final EventoSalidaRepository eventoSalidaRepository;

	private final SuscripcionWebhookRepository suscripcionWebhookRepository;

	private final EntregaWebhookRepository entregaWebhookRepository;

	private final AuditoriaService auditoriaService;

	private final PropiedadesWebhooks propiedades;

	private final HttpClient clienteHttp;

	public EntregaWebhookService(EventoSalidaRepository eventoSalidaRepository,
			SuscripcionWebhookRepository suscripcionWebhookRepository,
			EntregaWebhookRepository entregaWebhookRepository, AuditoriaService auditoriaService,
			PropiedadesWebhooks propiedades) {
		this.eventoSalidaRepository = eventoSalidaRepository;
		this.suscripcionWebhookRepository = suscripcionWebhookRepository;
		this.entregaWebhookRepository = entregaWebhookRepository;
		this.auditoriaService = auditoriaService;
		this.propiedades = propiedades;
		this.clienteHttp = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(Math.max(propiedades.getTiempoEsperaSegundos(), 1))).build();
	}

	@Transactional
	public void distribuir(String eventoId) {
		EventoSalida evento = eventoSalidaRepository.findById(eventoId).orElse(null);
		if (evento == null) {
			return;
		}
		try {
			List<SuscripcionWebhook> suscripciones = suscripcionWebhookRepository
					.listarActivasPorEvento(evento.getTenantId(), evento.getTipoEvento());
			for (SuscripcionWebhook suscripcion : suscripciones) {
				EntregaWebhook entrega = crearEntregaSiCorresponde(evento, suscripcion);
				if (entrega.getEstado() == EstadoEntregaWebhook.PENDIENTE && entrega.getIntento() == 0) {
					entregar(entrega, suscripcion, evento);
				}
			}
			evento.setEstado(EstadoEventoSalida.ENVIADO);
			evento.setProcesado(Instant.now());
			eventoSalidaRepository.save(evento);
		} catch (Exception e) {
			log.error("No se pudo despachar el evento {}", evento.getId(), e);
			evento.setIntento(evento.getIntento() + 1);
			evento.setUltimoError(e.getMessage());
			evento.setEstado(evento.getIntento() >= intentosMaximos() ? EstadoEventoSalida.DESCARTADO
					: EstadoEventoSalida.PENDIENTE);
			evento.setDisponibleEn(Instant.now().plus(esperaMinutos(evento.getIntento()), ChronoUnit.MINUTES));
			eventoSalidaRepository.save(evento);
		}
	}

	@Transactional
	public EntregaWebhook entregarPendiente(String entregaId) {
		EntregaWebhook entrega = entregaWebhookRepository.findById(entregaId).orElse(null);
		if (entrega == null) {
			return null;
		}
		SuscripcionWebhook suscripcion = entrega.getSuscripcion();
		EventoSalida evento = eventoSalidaRepository.findById(entrega.getEventoId()).orElse(null);
		entregar(entrega, suscripcion, evento);
		return entrega;
	}

	@Transactional
	public EntregaWebhook crearYEntregar(EventoSalida evento, SuscripcionWebhook suscripcion) {
		EntregaWebhook entrega = crearEntregaSiCorresponde(evento, suscripcion);
		if (entrega.getEstado() == EstadoEntregaWebhook.PENDIENTE) {
			entregar(entrega, suscripcion, evento);
		}
		return entrega;
	}

	private EntregaWebhook crearEntregaSiCorresponde(EventoSalida evento, SuscripcionWebhook suscripcion) {
		return entregaWebhookRepository.buscarPorSuscripcionYEvento(suscripcion.getId(), evento.getId())
				.orElseGet(() -> {
					EntregaWebhook entrega = new EntregaWebhook();
					entrega.setTenant(suscripcion.getTenant());
					entrega.setSuscripcion(suscripcion);
					entrega.setEventoId(evento.getId());
					entrega.setEstado(EstadoEntregaWebhook.PENDIENTE);
					entrega.setDisponibleEn(Instant.now());
					entrega.setAlta(Instant.now());
					return entregaWebhookRepository.save(entrega);
				});
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
					.timeout(Duration.ofSeconds(Math.max(propiedades.getTiempoEsperaSegundos(), 1)))
					.header("Content-Type", "application/json")
					.header(CABECERA_EVENTO, evento.getTipoEvento().getClave())
					.header(CABECERA_ENTREGA, entrega.getId())
					.header(CABECERA_FIRMA, Hash.hmacSha256(suscripcion.getSecreto(), evento.getCarga()))
					.POST(HttpRequest.BodyPublishers.ofString(evento.getCarga())).build();
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
		if (entrega.getIntento() >= intentosMaximos()) {
			entrega.setEstado(EstadoEntregaWebhook.AGOTADO);
		} else {
			entrega.setEstado(EstadoEntregaWebhook.PENDIENTE);
			entrega.setDisponibleEn(Instant.now().plus(esperaMinutos(entrega.getIntento()), ChronoUnit.MINUTES));
		}
		suscripcion.setFallosConsecutivos(suscripcion.getFallosConsecutivos() + 1);
		if (suscripcion.isActiva() && suscripcion.getFallosConsecutivos() >= umbralPausa()) {
			suscripcion.setActiva(false);
			auditoriaService.registrarConDetalle(suscripcion.getTenant().getId(), AccionAuditoria.WEBHOOK_PAUSADO,
					"SuscripcionWebhook", suscripcion.getId(),
					Map.of("motivo", "fallos consecutivos", "fallos", suscripcion.getFallosConsecutivos(), "url",
							suscripcion.getUrl()));
			log.warn("La suscripcion {} se pausa tras {} fallos consecutivos", suscripcion.getId(),
					suscripcion.getFallosConsecutivos());
		}
		suscripcionWebhookRepository.save(suscripcion);
	}

	private int intentosMaximos() {
		return propiedades.getIntentosMaximos() <= 0 ? 6 : propiedades.getIntentosMaximos();
	}

	private int umbralPausa() {
		return propiedades.getUmbralPausa() <= 0 ? 10 : propiedades.getUmbralPausa();
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
