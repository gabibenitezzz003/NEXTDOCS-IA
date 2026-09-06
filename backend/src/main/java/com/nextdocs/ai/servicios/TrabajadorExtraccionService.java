package com.nextdocs.ai.servicios;

import java.util.Optional;

import com.nextdocs.ai.config.PropiedadesCola;

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

	private final PropiedadesCola propiedades;

	public TrabajadorExtraccionService(ColaExtraccionService colaExtraccionService,
			ExtractorDocumentalService extractorDocumentalService, PropiedadesCola propiedades) {
		this.colaExtraccionService = colaExtraccionService;
		this.extractorDocumentalService = extractorDocumentalService;
		this.propiedades = propiedades;
	}

	@Scheduled(fixedDelayString = "${nextdocs.cola.intervaloSondeoMilisegundos:1000}")
	public void consumirCola() {
		int tope = Math.max(propiedades.getDocumentosPorCiclo(), 1);
		for (int procesados = 0; procesados < tope; procesados++) {
			Optional<String> documentoId = colaExtraccionService.desencolar();
			if (documentoId.isEmpty()) {
				return;
			}
			procesarSeguro(documentoId.get());
		}
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
