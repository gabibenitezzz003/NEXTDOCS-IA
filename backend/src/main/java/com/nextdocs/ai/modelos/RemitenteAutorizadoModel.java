package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class RemitenteAutorizadoModel implements Serializable {

	private static final long serialVersionUID = 1130856296671003348L;

	private String id;

	@NotBlank
	@Size(max = 256)
	private String patron;

	@Size(max = 256)
	private String descripcion;

	private Instant alta;
}
