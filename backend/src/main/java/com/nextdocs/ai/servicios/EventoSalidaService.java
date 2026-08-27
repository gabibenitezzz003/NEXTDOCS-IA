package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.EventoSalida;
import com.nextdocs.ai.enumeraciones.EstadoEventoSalida;
import com.nextdocs.ai.enumeraciones.TipoEventoCanonico;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.repositorios.EventoSalidaRepository;
import com.nextdocs.ai.utiles.ContextoCorrelacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventoSalidaService {

	public static final String AGREGADO_DOCUMENTO = "Documento";

	private final EventoSalidaRepository eventoSalidaRepository;

	private final ObjectMapper objectMapper;

	public EventoSalidaService(EventoSalidaRepository eventoSalidaRepository, ObjectMapper objectMapper) {
		this.eventoSalidaRepository = eventoSalidaRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public EventoSalida publicar(String tenantId, TipoEventoCanonico tipoEvento, String tipoAgregado,
			String idAgregado, Map<String, Object> carga) {
		try {
			EventoSalida evento = new EventoSalida();
			evento.setTenantId(tenantId);
			evento.setTipoEvento(tipoEvento);
			evento.setTipoAgregado(tipoAgregado);
			evento.setIdAgregado(idAgregado);
			evento.setCarga(objectMapper.writeValueAsString(carga));
			evento.setEstado(EstadoEventoSalida.PENDIENTE);
			evento.setCorrelacionId(ContextoCorrelacion.obtenerOGenerar());
			evento.setDisponibleEn(Instant.now());
			evento.setAlta(Instant.now());
			return eventoSalidaRepository.save(evento);
		} catch (Exception e) {
			throw new ValidacionException("No se pudo serializar el evento " + tipoEvento);
		}
	}

	@Transactional
	public EventoSalida publicarDeDocumento(Documento documento, TipoEventoCanonico tipoEvento) {
		Map<String, Object> carga = new LinkedHashMap<>();
		carga.put("documentoId", documento.getId());
		carga.put("tenantId", documento.getTenant().getId());
		carga.put("estado", documento.getEstado());
		carga.put("origen", documento.getOrigen());
		carga.put("nombre", documento.getNombre());
		carga.put("hashContenido", documento.getHashContenido());
		if (documento.getVersionPlantilla() != null) {
			carga.put("versionPlantillaId", documento.getVersionPlantilla().getId());
		}
		if (documento.getDocumentoPadre() != null) {
			carga.put("documentoPadreId", documento.getDocumentoPadre().getId());
		}
		if (documento.getReferenciaSujeto() != null && documento.getReferenciaSujeto().estaDefinida()) {
			carga.put("sujeto", Map.of("origen", documento.getReferenciaSujeto().getOrigen(), "tipoObjeto",
					documento.getReferenciaSujeto().getTipoObjeto(), "idObjeto",
					documento.getReferenciaSujeto().getIdObjeto()));
		}
		return publicar(documento.getTenant().getId(), tipoEvento, AGREGADO_DOCUMENTO, documento.getId(), carga);
	}
}
