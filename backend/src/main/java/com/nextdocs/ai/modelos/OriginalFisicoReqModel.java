package com.nextdocs.ai.modelos;

import java.io.Serializable;

import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class OriginalFisicoReqModel implements Serializable {

	private static final long serialVersionUID = 8836057422195401347L;

	@Size(max = 256)
	private String ubicacion;

	@Size(max = 128)
	private String referenciaFisica;

	@Size(max = 512)
	private String observacion;
}
