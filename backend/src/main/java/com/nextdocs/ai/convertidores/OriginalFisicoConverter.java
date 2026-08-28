package com.nextdocs.ai.convertidores;

import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.entidades.SeguimientoOriginalFisico;
import com.nextdocs.ai.enumeraciones.EstadoOriginalFisico;
import com.nextdocs.ai.enumeraciones.PoliticaOriginalFisico;
import com.nextdocs.ai.modelos.OriginalFisicoModel;
import com.nextdocs.ai.utiles.MaquinaEstadoOriginalFisico;

import org.springframework.stereotype.Component;

@Component
public class OriginalFisicoConverter {

	public OriginalFisicoModel aModelo(SeguimientoOriginalFisico seguimiento) {
		OriginalFisicoModel modelo = new OriginalFisicoModel();
		modelo.setId(seguimiento.getId());
		modelo.setEstado(seguimiento.getEstado());
		modelo.setPolitica(seguimiento.getPolitica());
		modelo.setUbicacion(seguimiento.getUbicacion());
		modelo.setReferenciaFisica(seguimiento.getReferenciaFisica());
		modelo.setObservacion(seguimiento.getObservacion());
		modelo.setRecibido(seguimiento.getRecibido());
		modelo.setArchivado(seguimiento.getArchivado());
		modelo.setExtraviado(seguimiento.getExtraviado());
		modelo.setAlta(seguimiento.getAlta());
		modelo.setPendiente(EstadoOriginalFisico.PENDIENTE == seguimiento.getEstado());
		modelo.setBloqueaCierre(PoliticaOriginalFisico.REQUIERE_PARA_CIERRE == seguimiento.getPolitica());
		modelo.setTransicionesPosibles(
				new ArrayList<>(MaquinaEstadoOriginalFisico.siguientes(seguimiento.getEstado())));
		if (seguimiento.getDocumento() != null) {
			modelo.setDocumentoId(seguimiento.getDocumento().getId());
			modelo.setNombreDocumento(seguimiento.getDocumento().getNombre());
		}
		if (seguimiento.getRecibidoPor() != null) {
			modelo.setRecibidoPor(seguimiento.getRecibidoPor().getEmail());
		}
		if (seguimiento.getRegistradoPor() != null) {
			modelo.setRegistradoPor(seguimiento.getRegistradoPor().getEmail());
		}
		return modelo;
	}

	public List<OriginalFisicoModel> aModelos(List<SeguimientoOriginalFisico> seguimientos) {
		List<OriginalFisicoModel> modelos = new ArrayList<>();
		for (SeguimientoOriginalFisico seguimiento : seguimientos) {
			modelos.add(aModelo(seguimiento));
		}
		return modelos;
	}
}
