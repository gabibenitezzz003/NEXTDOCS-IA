package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.nextdocs.ai.enumeraciones.DecisionRevision;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class RevisionDocumentoReqModel implements Serializable {

	private static final long serialVersionUID = 7739205428106631187L;

	@NotNull
	private DecisionRevision decision;

	@Size(max = 1024)
	private String motivo;

	private long duracionRevisionMilisegundos;

	private Map<String, String> correcciones = new LinkedHashMap<>();

	private List<String> hallazgosSobreescritos = new ArrayList<>();
}
