package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.config.PropiedadesCola;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.enumeraciones.EstadoDocumento;
import com.nextdocs.ai.repositorios.DocumentoRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "nextdocs.cola.trabajadorActivo", havingValue = "true", matchIfMissing = true)
public class TrabajadorExtraccionService {

	private static final Logger log = LoggerFactory.getLogger(TrabajadorExtraccionService.class);

	private final ColaExtraccionService colaExtraccionService;

	private final ExtractorDocumentalService extractorDocumentalService;

	private final DocumentoRepository documentoRepository;

	private final PropiedadesCola propiedades;

	public TrabajadorExtraccionService(ColaExtraccionService colaExtraccionService,
			ExtractorDocumentalService extractorDocumentalService, DocumentoRepository documentoRepository,
			PropiedadesCola propiedades) {
		this.colaExtraccionService = colaExtraccionService;
		this.extractorDocumentalService = extractorDocumentalService;
		this.documentoRepository = documentoRepository;
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

	@Scheduled(fixedDelay = 10000)
	public void medirColas() {
		colaExtraccionService.profundidad();
		colaExtraccionService.profundidadReintento();
	}

	@Scheduled(fixedDelayString = "${nextdocs.cola.intervaloRecuperacionMilisegundos:60000}")
	public void recuperarEstancados() {
		Instant limite = Instant.now().minusMillis(propiedades.getEsperaEstancadoMilisegundos());
		PageRequest paginado = PageRequest.of(0, Math.max(propiedades.getDocumentosRecuperacionPorCiclo(), 1));
		List<Documento> recibidos = documentoRepository.listarRecibidosAnteriores(limite, paginado);
		List<Documento> procesando = documentoRepository.listarEstancadosPorEstado(EstadoDocumento.PROCESANDO,
				limite, paginado);
		for (Documento documento : recibidos) {
			colaExtraccionService.encolar(documento.getId());
		}
		for (Documento documento : procesando) {
			colaExtraccionService.encolar(documento.getId());
		}
		if (!recibidos.isEmpty() || !procesando.isEmpty()) {
			log.info("Se reencolaron {} documentos recibidos y {} estancados en procesamiento",
					recibidos.size(), procesando.size());
		}
	}

	private void procesarSeguro(String documentoId) {
		try {
			extractorDocumentalService.procesar(documentoId);
		} catch (Exception e) {
			log.error("El trabajador no pudo procesar el documento {}", documentoId, e);
		}
	}
}
