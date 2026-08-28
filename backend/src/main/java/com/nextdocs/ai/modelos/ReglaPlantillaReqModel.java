package com.nextdocs.ai.modelos;

import java.io.Serializable;

import com.nextdocs.ai.enumeraciones.SeveridadHallazgo;
import com.nextdocs.ai.enumeraciones.TipoReglaValidacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class ReglaPlantillaReqModel implements Serializable {

	private static final long serialVersionUID = 3341075129940602238L;

	@NotBlank
	@Size(max = 64)
	@Pattern(regexp = "[A-Z][A-Z0-9_]*", message = "El codigo debe estar en mayusculas con guion bajo")
	private String codigo;

	@NotBlank
	@Size(max = 256)
	private String nombre;

	@NotNull
	private TipoReglaValidacion tipo;

	@NotNull
	private SeveridadHallazgo severidad;

	@Size(max = 128)
	private String campoObjetivo;

	private String configuracion;

	@Size(max = 512)
	private String mensaje;

	private boolean activa = true;

	private int orden;
}
