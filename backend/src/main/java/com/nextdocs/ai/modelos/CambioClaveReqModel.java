package com.nextdocs.ai.modelos;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class CambioClaveReqModel implements Serializable {

	private static final long serialVersionUID = 2340869231670543912L;

	private String claveActual;

	@NotBlank
	@Size(min = 10, max = 128)
	private String claveNueva;
}
