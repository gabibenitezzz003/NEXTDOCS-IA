package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdocs.ai.entidades.EventoAuditoria;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.TipoActor;
import com.nextdocs.ai.modelos.PrincipalNextDocs;
import com.nextdocs.ai.repositorios.EventoAuditoriaRepository;
import com.nextdocs.ai.utiles.ContextoCorrelacion;
import com.nextdocs.ai.utiles.ContextoSeguridad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService {

	private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

	private final EventoAuditoriaRepository eventoAuditoriaRepository;

	private final ObjectMapper objectMapper;

	public AuditoriaService(EventoAuditoriaRepository eventoAuditoriaRepository, ObjectMapper objectMapper) {
		this.eventoAuditoriaRepository = eventoAuditoriaRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void registrar(String tenantId, AccionAuditoria accion, String tipoRecurso, String idRecurso) {
		registrar(tenantId, accion, tipoRecurso, idRecurso, null, null, null, true);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void registrarConDetalle(String tenantId, AccionAuditoria accion, String tipoRecurso, String idRecurso,
			Map<String, Object> detalle) {
		registrar(tenantId, accion, tipoRecurso, idRecurso, detalle, null, null, true);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void registrarCambio(String tenantId, AccionAuditoria accion, String tipoRecurso, String idRecurso,
			String hashAntes, String hashDespues) {
		registrar(tenantId, accion, tipoRecurso, idRecurso, null, hashAntes, hashDespues, true);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void registrarFallo(String tenantId, AccionAuditoria accion, String tipoRecurso, String idRecurso,
			Map<String, Object> detalle) {
		registrar(tenantId, accion, tipoRecurso, idRecurso, detalle, null, null, false);
	}

	@Transactional(readOnly = true)
	public List<EventoAuditoria> reconstruir(String tenantId, String tipoRecurso, String idRecurso) {
		return eventoAuditoriaRepository.listarPorRecurso(tenantId, tipoRecurso, idRecurso);
	}

	private void registrar(String tenantId, AccionAuditoria accion, String tipoRecurso, String idRecurso,
			Map<String, Object> detalle, String hashAntes, String hashDespues, boolean exitoso) {
		try {
			EventoAuditoria evento = new EventoAuditoria();
			evento.setTenantId(tenantId);
			evento.setAccion(accion);
			evento.setTipoRecurso(tipoRecurso);
			evento.setIdRecurso(idRecurso);
			evento.setHashAntes(hashAntes);
			evento.setHashDespues(hashDespues);
			evento.setCorrelacionId(ContextoCorrelacion.obtenerOGenerar());
			evento.setExitoso(exitoso);
			evento.setFecha(Instant.now());
			completarActor(evento);
			if (detalle != null && !detalle.isEmpty()) {
				evento.setDetalle(objectMapper.writeValueAsString(detalle));
			}
			eventoAuditoriaRepository.save(evento);
		} catch (Exception e) {
			log.error("No se pudo registrar el evento de auditoria {} sobre {}", accion, idRecurso, e);
		}
	}

	private void completarActor(EventoAuditoria evento) {
		PrincipalNextDocs principal = ContextoSeguridad.principalONulo();
		if (principal == null) {
			evento.setTipoActor(TipoActor.SISTEMA);
			evento.setDescripcionActor("sistema");
			return;
		}
		evento.setTipoActor(principal.getTipoActor());
		evento.setIdActor(principal.getIdActor());
		evento.setDescripcionActor(principal.getEmail() != null ? principal.getEmail() : principal.getDescripcion());
	}
}
