package com.nextdocs.ai.modelos;

import java.io.Serializable;

import com.nextdocs.ai.enumeraciones.AccionRetencion;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class PoliticaRetencionReqModel implements Serializable {

	private static final long serialVersionUID = 7712066429318504175L;

	@NotBlank
	@Size(max = 64)
	@Pattern(regexp = "[A-Z][A-Z0-9_]*", message = "La clase debe estar en mayusculas con guion bajo")
	private String clase;

	@Size(max = 256)
	private String descripcion;

	@Min(1)
	@Max(36500)
	private int duracionDias;

	@NotNull
	private AccionRetencion accion;

	private boolean permiteRetencionLegal;

	private boolean activa;
}
