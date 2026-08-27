package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Data;

@Data
public class ContextoAsociacionModel implements Serializable {

	private static final long serialVersionUID = 9021476633508211982L;

	private String tenantId;

	private String documentoId;

	private String codigoPlantilla;

	private String correlacionId;

	private Map<String, String> valores = new LinkedHashMap<>();
}
