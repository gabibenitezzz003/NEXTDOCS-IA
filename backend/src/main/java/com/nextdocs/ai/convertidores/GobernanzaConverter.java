package com.nextdocs.ai.convertidores;

import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.entidades.EventoAuditoria;
import com.nextdocs.ai.entidades.PoliticaRetencion;
import com.nextdocs.ai.modelos.EventoAuditoriaModel;
import com.nextdocs.ai.modelos.PoliticaRetencionModel;

import org.springframework.stereotype.Component;

@Component
public class GobernanzaConverter {

	public EventoAuditoriaModel aModelo(EventoAuditoria evento) {
		EventoAuditoriaModel modelo = new EventoAuditoriaModel();
		modelo.setId(evento.getId());
		modelo.setTipoActor(evento.getTipoActor());
		modelo.setIdActor(evento.getIdActor());
		modelo.setDescripcionActor(evento.getDescripcionActor());
		modelo.setAccion(evento.getAccion());
		modelo.setTipoRecurso(evento.getTipoRecurso());
		modelo.setIdRecurso(evento.getIdRecurso());
		modelo.setHashAntes(evento.getHashAntes());
		modelo.setHashDespues(evento.getHashDespues());
		modelo.setDetalle(evento.getDetalle());
		modelo.setDireccionIp(evento.getDireccionIp());
		modelo.setAgenteUsuario(evento.getAgenteUsuario());
		modelo.setCorrelacionId(evento.getCorrelacionId());
		modelo.setExitoso(evento.isExitoso());
		modelo.setFecha(evento.getFecha());
		return modelo;
	}

	public List<EventoAuditoriaModel> aModelos(List<EventoAuditoria> eventos) {
		List<EventoAuditoriaModel> modelos = new ArrayList<>();
		for (EventoAuditoria evento : eventos) {
			modelos.add(aModelo(evento));
		}
		return modelos;
	}

	public PoliticaRetencionModel aModelo(PoliticaRetencion politica) {
		PoliticaRetencionModel modelo = new PoliticaRetencionModel();
		modelo.setId(politica.getId());
		modelo.setClase(politica.getClase());
		modelo.setDescripcion(politica.getDescripcion());
		modelo.setDuracionDias(politica.getDuracionDias());
		modelo.setAccion(politica.getAccion());
		modelo.setPermiteRetencionLegal(politica.isPermiteRetencionLegal());
		modelo.setActiva(politica.isActiva());
		modelo.setAlta(politica.getAlta());
		return modelo;
	}

	public List<PoliticaRetencionModel> aModelosPolitica(List<PoliticaRetencion> politicas) {
		List<PoliticaRetencionModel> modelos = new ArrayList<>();
		for (PoliticaRetencion politica : politicas) {
			modelos.add(aModelo(politica));
		}
		return modelos;
	}
}
