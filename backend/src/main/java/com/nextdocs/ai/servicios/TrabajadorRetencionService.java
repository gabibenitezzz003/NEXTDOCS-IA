package com.nextdocs.ai.servicios;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "nextdocs.retencion.trabajadorActivo", havingValue = "true", matchIfMissing = true)
public class TrabajadorRetencionService {

	private static final Logger log = LoggerFactory.getLogger(TrabajadorRetencionService.class);

	private final RetencionService retencionService;

	private final int documentosPorCiclo;

	public TrabajadorRetencionService(RetencionService retencionService,
			@Value("${nextdocs.retencion.documentosPorCiclo:100}") int documentosPorCiclo) {
		this.retencionService = retencionService;
		this.documentosPorCiclo = documentosPorCiclo;
	}

	@Scheduled(fixedDelayString = "${nextdocs.retencion.intervaloMilisegundos:3600000}",
			initialDelayString = "${nextdocs.retencion.demoraInicialMilisegundos:60000}")
	public void ejecutar() {
		try {
			retencionService.ejecutarCiclo(documentosPorCiclo);
		} catch (Exception e) {
			log.error("El ciclo de retencion fallo y se reintentara en el proximo intervalo", e);
		}
	}
}
