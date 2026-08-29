package com.nextdocs.ai.modelos;

import java.io.Serializable;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class NuevaCorrelacionCorreoReqModel implements Serializable {

	private static final long serialVersionUID = 4415580927336619108L;

	@NotBlank
	@Size(max = 36)
	private String buzonId;

	@NotBlank
	@Size(max = 64)
	private String sujetoOrigen;

	@NotBlank
	@Size(max = 64)
	private String sujetoTipoObjeto;

	@NotBlank
	@Size(max = 128)
	private String sujetoIdObjeto;

	@Size(max = 64)
	private String codigoPlantilla;

	@Size(max = 256)
	private String destinatario;

	@Size(max = 256)
	private String descripcion;

	@Min(1)
	@Max(365)
	private int diasVigencia;

	private boolean enviarSolicitud;
}
