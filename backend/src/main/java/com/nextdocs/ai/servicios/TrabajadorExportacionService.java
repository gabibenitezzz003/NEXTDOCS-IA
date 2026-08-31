package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.nextdocs.ai.entidades.LoteExportacion;
import com.nextdocs.ai.enumeraciones.EstadoLoteExportacion;
import com.nextdocs.ai.enumeraciones.TipoEventoCanonico;
import com.nextdocs.ai.repositorios.LoteExportacionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrabajadorExportacionService {

	private static final Logger log = LoggerFactory.getLogger(TrabajadorExportacionService.class);

	private static final int LOTES_POR_CICLO = 3;

	private static final int DIAS_AVISO_PREVIO = 2;

	private final LoteExportacionRepository loteExportacionRepository;

	private final GeneradorExportacionService generadorExportacionService;

	private final AlmacenamientoService almacenamientoService;

	private final EventoSalidaService eventoSalidaService;

	public TrabajadorExportacionService(LoteExportacionRepository loteExportacionRepository,
			GeneradorExportacionService generadorExportacionService, AlmacenamientoService almacenamientoService,
			EventoSalidaService eventoSalidaService) {
		this.loteExportacionRepository = loteExportacionRepository;
		this.generadorExportacionService = generadorExportacionService;
		this.almacenamientoService = almacenamientoService;
		this.eventoSalidaService = eventoSalidaService;
	}

	@Scheduled(fixedDelayString = "${nextdocs.exportacion.intervaloMilisegundos:15000}",
			initialDelayString = "${nextdocs.exportacion.retrasoInicialMilisegundos:20000}")
	public void generarPendientes() {
		for (LoteExportacion lote : loteExportacionRepository
				.listarPendientes(PageRequest.of(0, LOTES_POR_CICLO))) {
			generadorExportacionService.generar(lote.getId());
		}
	}

	@Scheduled(fixedDelayString = "${nextdocs.exportacion.intervaloCicloVidaMilisegundos:3600000}",
			initialDelayString = "${nextdocs.exportacion.retrasoInicialMilisegundos:20000}")
	public void aplicarCicloDeVida() {
		avisarPorVencer();
		vencer();
	}

	@Transactional
	public int avisarPorVencer() {
		Instant ahora = Instant.now();
		Instant limite = ahora.plus(DIAS_AVISO_PREVIO, ChronoUnit.DAYS);
		List<LoteExportacion> lotes = loteExportacionRepository.listarPorVencer(ahora, limite);
		for (LoteExportacion lote : lotes) {
			lote.setAvisoVencimiento(ahora);
			loteExportacionRepository.save(lote);
			eventoSalidaService.publicar(lote.getTenant().getId(), TipoEventoCanonico.EXPORTACION_POR_VENCER,
					GeneradorExportacionService.class.getSimpleName(), lote.getId(), datos(lote));
			log.info("Lote de exportacion {} vence el {}", lote.getId(), lote.getVenceEn());
		}
		return lotes.size();
	}

	@Transactional
	public int vencer() {
		Instant ahora = Instant.now();
		List<LoteExportacion> lotes = loteExportacionRepository.listarVencidos(ahora);
		for (LoteExportacion lote : lotes) {
			borrarArchivo(lote);
			lote.setEstado(EstadoLoteExportacion.VENCIDO);
			lote.setClaveObjeto(null);
			loteExportacionRepository.save(lote);
			eventoSalidaService.publicar(lote.getTenant().getId(), TipoEventoCanonico.EXPORTACION_VENCIDA,
					GeneradorExportacionService.class.getSimpleName(), lote.getId(), datos(lote));
			log.info("Lote de exportacion {} vencido y su archivo fue borrado", lote.getId());
		}
		return lotes.size();
	}

	private void borrarArchivo(LoteExportacion lote) {
		if (lote.getClaveObjeto() == null) {
			return;
		}
		try {
			almacenamientoService.eliminarExportacion(lote.getClaveObjeto());
		} catch (Exception e) {
			log.warn("No se pudo borrar el archivo del lote {}: {}", lote.getId(), e.getMessage());
		}
	}

	private Map<String, Object> datos(LoteExportacion lote) {
		Map<String, Object> carga = new LinkedHashMap<>();
		carga.put("loteId", lote.getId());
		carga.put("nombre", lote.getNombre());
		carga.put("venceEn", lote.getVenceEn() == null ? null : lote.getVenceEn().toString());
		carga.put("cantidadDocumentos", lote.getCantidadDocumentos());
		return carga;
	}
}
