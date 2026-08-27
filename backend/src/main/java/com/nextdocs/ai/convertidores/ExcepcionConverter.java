package com.nextdocs.ai.convertidores;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.entidades.CambioCampoRevision;
import com.nextdocs.ai.entidades.ExcepcionDocumental;
import com.nextdocs.ai.entidades.RevisionDocumento;
import com.nextdocs.ai.enumeraciones.EstadoExcepcion;
import com.nextdocs.ai.modelos.CambioCampoRevisionModel;
import com.nextdocs.ai.modelos.ExcepcionDocumentalModel;
import com.nextdocs.ai.modelos.RevisionDocumentoModel;

import org.springframework.stereotype.Component;

@Component
public class ExcepcionConverter {

	public ExcepcionDocumentalModel aModelo(ExcepcionDocumental excepcion) {
		ExcepcionDocumentalModel modelo = new ExcepcionDocumentalModel();
		modelo.setId(excepcion.getId());
		modelo.setTipo(excepcion.getTipo());
		modelo.setSeveridad(excepcion.getSeveridad());
		modelo.setPrioridad(excepcion.getPrioridad());
		modelo.setEstado(excepcion.getEstado());
		modelo.setCodigo(excepcion.getCodigo());
		modelo.setDetalle(excepcion.getDetalle());
		modelo.setResolucion(excepcion.getResolucion());
		modelo.setVenceEn(excepcion.getVenceEn());
		modelo.setResuelta(excepcion.getResuelta());
		modelo.setAlta(excepcion.getAlta());
		modelo.setVencida(estaVencida(excepcion));
		if (excepcion.getDocumento() != null) {
			modelo.setDocumentoId(excepcion.getDocumento().getId());
			modelo.setNombreDocumento(excepcion.getDocumento().getNombre());
		}
		if (excepcion.getResponsable() != null) {
			modelo.setResponsable(excepcion.getResponsable().getEmail());
		}
		if (excepcion.getResueltaPor() != null) {
			modelo.setResueltaPor(excepcion.getResueltaPor().getEmail());
		}
		return modelo;
	}

	public RevisionDocumentoModel aModelo(RevisionDocumento revision, List<CambioCampoRevision> cambios) {
		RevisionDocumentoModel modelo = new RevisionDocumentoModel();
		modelo.setId(revision.getId());
		modelo.setDecision(revision.getDecision());
		modelo.setEstadoAnterior(revision.getEstadoAnterior());
		modelo.setEstadoNuevo(revision.getEstadoNuevo());
		modelo.setMotivo(revision.getMotivo());
		modelo.setCantidadCorrecciones(revision.getCantidadCorrecciones());
		modelo.setDuracionRevisionMilisegundos(revision.getDuracionRevisionMilisegundos());
		modelo.setAlta(revision.getAlta());
		if (revision.getActor() != null) {
			modelo.setActor(revision.getActor().getEmail());
		}
		if (cambios != null) {
			for (CambioCampoRevision cambio : cambios) {
				CambioCampoRevisionModel cambioModelo = new CambioCampoRevisionModel();
				cambioModelo.setClaveCampo(cambio.getClaveCampo());
				cambioModelo.setValorAnterior(cambio.getValorAnterior());
				cambioModelo.setValorNuevo(cambio.getValorNuevo());
				cambioModelo.setMotivo(cambio.getMotivo());
				modelo.getCambios().add(cambioModelo);
			}
		}
		return modelo;
	}

	public List<ExcepcionDocumentalModel> aModelos(List<ExcepcionDocumental> excepciones) {
		List<ExcepcionDocumentalModel> modelos = new ArrayList<>();
		for (ExcepcionDocumental excepcion : excepciones) {
			modelos.add(aModelo(excepcion));
		}
		return modelos;
	}

	private boolean estaVencida(ExcepcionDocumental excepcion) {
		return excepcion.getVenceEn() != null && excepcion.getEstado() != EstadoExcepcion.RESUELTA
				&& excepcion.getVenceEn().isBefore(Instant.now());
	}
}
