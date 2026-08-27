package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.enumeraciones.DecisionRevision;
import com.nextdocs.ai.enumeraciones.EstadoDocumento;

import lombok.Data;

@Data
public class RevisionDocumentoModel implements Serializable {

	private static final long serialVersionUID = 4470926611228033556L;

	private String id;

	private String actor;

	private DecisionRevision decision;

	private EstadoDocumento estadoAnterior;

	private EstadoDocumento estadoNuevo;

	private String motivo;

	private int cantidadCorrecciones;

	private long duracionRevisionMilisegundos;

	private Instant alta;

	private List<CambioCampoRevisionModel> cambios = new ArrayList<>();
}
