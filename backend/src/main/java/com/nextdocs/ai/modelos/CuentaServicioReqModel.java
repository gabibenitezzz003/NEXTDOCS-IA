package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class CuentaServicioReqModel implements Serializable {

	private static final long serialVersionUID = 4022670884213157436L;

	@NotBlank
	@Size(max = 128)
	private String nombre;

	@NotEmpty
	private List<String> alcances = new ArrayList<>();

	@Min(0)
	@Max(3650)
	private int diasVigencia;
}
