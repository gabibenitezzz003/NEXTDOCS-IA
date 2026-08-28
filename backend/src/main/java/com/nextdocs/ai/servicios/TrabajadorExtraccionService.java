package com.nextdocs.ai.servicios;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "nextdocs.cola.trabajadorActivo", havingValue = "true", matchIfMissing = true)
public class TrabajadorExtraccionService {

	private static final Logger log = LoggerFactory.getLogger(TrabajadorExtraccionService.class);

	private final ColaExtraccionService colaExtraccionService;

	private final ExtractorDocumentalService extractorDocumentalService;

	public TrabajadorExtraccionService(ColaExtraccionService colaExtraccionService,
			ExtractorDocumentalService extractorDocumentalService) {
		this.colaExtraccionService = colaExtraccionService;
		this.extractorDocumentalService = extractorDocumentalService;
	}

	@Scheduled(fixedDelayString = "${nextdocs.cola.intervaloSondeoMilisegundos:1000}")
	public void consumirCola() {
		Optional<String> documentoId = colaExtraccionService.desencolar();
		documentoId.ifPresent(this::procesarSeguro);
	}

	@Scheduled(fixedDelay = 5000)
	public void consumirReintentos() {
		Optional<String> documentoId = colaExtraccionService.desencolarReintentoVencido();
		documentoId.ifPresent(this::procesarSeguro);
	}

	private void procesarSeguro(String documentoId) {
		try {
			extractorDocumentalService.procesar(documentoId);
		} catch (Exception e) {
			log.error("El trabajador no pudo procesar el documento {}", documentoId, e);
		}
	}
}
