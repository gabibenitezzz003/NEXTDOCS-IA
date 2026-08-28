package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.util.List;

import com.nextdocs.ai.entidades.EntregaWebhook;
import com.nextdocs.ai.entidades.EventoSalida;
import com.nextdocs.ai.enumeraciones.EstadoEntregaWebhook;
import com.nextdocs.ai.enumeraciones.EstadoEventoSalida;
import com.nextdocs.ai.repositorios.EntregaWebhookRepository;
import com.nextdocs.ai.repositorios.EventoSalidaRepository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "nextdocs.webhooks.despachadorActivo", havingValue = "true", matchIfMissing = true)
public class DespachadorEventosService {

	public static final String CABECERA_FIRMA = EntregaWebhookService.CABECERA_FIRMA;

	public static final String CABECERA_EVENTO = EntregaWebhookService.CABECERA_EVENTO;

	public static final String CABECERA_ENTREGA = EntregaWebhookService.CABECERA_ENTREGA;

	private static final int LOTE = 50;

	private final EventoSalidaRepository eventoSalidaRepository;

	private final EntregaWebhookRepository entregaWebhookRepository;

	private final EntregaWebhookService entregaWebhookService;

	public DespachadorEventosService(EventoSalidaRepository eventoSalidaRepository,
			EntregaWebhookRepository entregaWebhookRepository, EntregaWebhookService entregaWebhookService) {
		this.eventoSalidaRepository = eventoSalidaRepository;
		this.entregaWebhookRepository = entregaWebhookRepository;
		this.entregaWebhookService = entregaWebhookService;
	}

	@Scheduled(fixedDelay = 3000)
	public void despachar() {
		List<EventoSalida> pendientes = eventoSalidaRepository.listarPendientes(EstadoEventoSalida.PENDIENTE,
				Instant.now(), PageRequest.of(0, LOTE));
		for (EventoSalida evento : pendientes) {
			entregaWebhookService.distribuir(evento.getId());
		}
	}

	@Scheduled(fixedDelay = 10000)
	public void reintentarEntregas() {
		List<EntregaWebhook> pendientes = entregaWebhookRepository.listarPendientes(EstadoEntregaWebhook.PENDIENTE,
				Instant.now(), PageRequest.of(0, LOTE));
		for (EntregaWebhook entrega : pendientes) {
			entregaWebhookService.entregarPendiente(entrega.getId());
		}
	}
}
