package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.util.Map;

import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.enumeraciones.EstadoDocumento;
import com.nextdocs.ai.enumeraciones.TipoEventoCanonico;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.utiles.MaquinaEstadoDocumento;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EstadoDocumentalService {

	private static final Logger log = LoggerFactory.getLogger(EstadoDocumentalService.class);

	private static final Map<EstadoDocumento, TipoEventoCanonico> EVENTOS = Map.of(
			EstadoDocumento.EXTRAIDO, TipoEventoCanonico.DOCUMENTO_EXTRAIDO,
			EstadoDocumento.VALIDADO, TipoEventoCanonico.DOCUMENTO_VALIDADO,
			EstadoDocumento.OBSERVADO, TipoEventoCanonico.DOCUMENTO_OBSERVADO,
			EstadoDocumento.APROBADO, TipoEventoCanonico.DOCUMENTO_APROBADO,
			EstadoDocumento.RECHAZADO, TipoEventoCanonico.DOCUMENTO_RECHAZADO,
			EstadoDocumento.CERRADO, TipoEventoCanonico.DOCUMENTO_CERRADO,
			EstadoDocumento.DIVIDIDO, TipoEventoCanonico.DOCUMENTO_SEGMENTADO);

	private final DocumentoRepository documentoRepository;

	private final EventoSalidaService eventoSalidaService;

	public EstadoDocumentalService(DocumentoRepository documentoRepository, EventoSalidaService eventoSalidaService) {
		this.documentoRepository = documentoRepository;
		this.eventoSalidaService = eventoSalidaService;
	}

	@Transactional
	public Documento transicionar(Documento documento, EstadoDocumento destino) {
		EstadoDocumento origen = documento.getEstado();
		MaquinaEstadoDocumento.validar(origen, destino);
		documento.setEstado(destino);
		if (destino == EstadoDocumento.PROCESANDO) {
			documento.setProcesado(Instant.now());
		}
		if (destino == EstadoDocumento.CERRADO) {
			documento.setCerrado(Instant.now());
		}
		documentoRepository.save(documento);
		log.info("Documento {} paso de {} a {}", documento.getId(), origen, destino);
		TipoEventoCanonico evento = EVENTOS.get(destino);
		if (evento != null) {
			eventoSalidaService.publicarDeDocumento(documento, evento);
		}
		return documento;
	}

	public boolean puedeTransicionar(Documento documento, EstadoDocumento destino) {
		return MaquinaEstadoDocumento.permitida(documento.getEstado(), destino);
	}
}
