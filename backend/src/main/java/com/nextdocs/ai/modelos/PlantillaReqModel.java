package com.nextdocs.ai.modelos;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class PlantillaReqModel implements Serializable {

	private static final long serialVersionUID = 8873650211947724084L;

	@NotBlank
	@Size(max = 64)
	@Pattern(regexp = "[A-Z][A-Z0-9_]*", message = "El codigo debe estar en mayusculas con guion bajo")
	private String codigo;

	@NotBlank
	@Size(max = 128)
	private String nombre;

	@Size(max = 64)
	private String familia;

	@Size(max = 1024)
	private String descripcion;
}
