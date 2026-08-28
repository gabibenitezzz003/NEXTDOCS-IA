package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class RolReqModel implements Serializable {

	private static final long serialVersionUID = 3049160279905613504L;

	@NotBlank
	@Size(max = 64)
	@Pattern(regexp = "[A-Z][A-Z0-9_]*", message = "El codigo debe estar en mayusculas con guion bajo")
	private String codigo;

	@NotBlank
	@Size(max = 128)
	private String nombre;

	@Size(max = 512)
	private String descripcion;

	@NotEmpty
	private List<String> permisos = new ArrayList<>();
}
