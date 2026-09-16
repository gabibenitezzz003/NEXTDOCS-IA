package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class EventoServicioReqModel implements Serializable {

	private static final long serialVersionUID = 1L;

	@NotBlank
	@Size(max = 128)
	private String tipoEvento;

	@Size(max = 64)
	private String tipoAgregado;

	@Size(max = 64)
	private String idAgregado;

	private Map<String, Object> carga;
}
