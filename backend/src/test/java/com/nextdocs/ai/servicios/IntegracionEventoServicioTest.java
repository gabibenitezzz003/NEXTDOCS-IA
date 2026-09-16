package com.nextdocs.ai.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.nextdocs.ai.convertidores.IntegracionConverter;
import com.nextdocs.ai.entidades.EventoSalida;
import com.nextdocs.ai.enumeraciones.EstadoEventoSalida;
import com.nextdocs.ai.enumeraciones.TipoEventoCanonico;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.EventoServicioReqModel;
import com.nextdocs.ai.config.PropiedadesWebhooks;
import com.nextdocs.ai.repositorios.EntregaWebhookRepository;
import com.nextdocs.ai.repositorios.EventoSalidaRepository;
import com.nextdocs.ai.repositorios.SuscripcionWebhookRepository;

class IntegracionEventoServicioTest {

	private EventoSalidaRepository eventoSalidaRepository;

	private EventoSalidaService eventoSalidaService;

	private AuditoriaService auditoriaService;

	private IntegracionService servicio;

	@BeforeEach
	void preparar() {
		eventoSalidaRepository = Mockito.mock(EventoSalidaRepository.class);
		eventoSalidaService = Mockito.mock(EventoSalidaService.class);
		auditoriaService = Mockito.mock(AuditoriaService.class);
		servicio = new IntegracionService(Mockito.mock(SuscripcionWebhookRepository.class),
				Mockito.mock(EntregaWebhookRepository.class), eventoSalidaRepository, eventoSalidaService,
				Mockito.mock(EntregaWebhookService.class), auditoriaService,
				Mockito.mock(IntegracionConverter.class), new PropiedadesWebhooks());
	}

	@Test
	void publicarEventoServicioAceptaEventosDeProceso() {
		EventoSalida evento = new EventoSalida();
		evento.setId("evt-1");
		evento.setEstado(EstadoEventoSalida.PENDIENTE);
		when(eventoSalidaService.publicar(Mockito.eq("tenant-1"), Mockito.eq(TipoEventoCanonico.PROCESO_NOTIFICACION),
				Mockito.eq("InstanciaProceso"), Mockito.eq("instancia-9"), Mockito.any())).thenReturn(evento);

		EventoServicioReqModel datos = new EventoServicioReqModel();
		datos.setTipoEvento("process.notification");
		datos.setIdAgregado("instancia-9");
		datos.setCarga(Map.of("mensaje", "El proceso avanzo"));

		Map<String, Object> resultado = servicio.publicarEventoServicio("tenant-1", datos);

		assertThat(resultado.get("id")).isEqualTo("evt-1");
		assertThat(resultado.get("tipoEvento")).isEqualTo("process.notification");
		assertThat(resultado.get("estado")).isEqualTo("PENDIENTE");
	}

	@Test
	void publicarEventoServicioRechazaEventosDelDominioDocumental() {
		EventoServicioReqModel datos = new EventoServicioReqModel();
		datos.setTipoEvento("document.approved");

		assertThatThrownBy(() -> servicio.publicarEventoServicio("tenant-1", datos))
				.isInstanceOf(ValidacionException.class).hasMessageContaining("no es un evento de proceso");
	}

	@Test
	void publicarEventoServicioRechazaTiposDesconocidos() {
		EventoServicioReqModel datos = new EventoServicioReqModel();
		datos.setTipoEvento("inventado.raro");

		assertThatThrownBy(() -> servicio.publicarEventoServicio("tenant-1", datos))
				.isInstanceOf(ValidacionException.class);
	}
}
