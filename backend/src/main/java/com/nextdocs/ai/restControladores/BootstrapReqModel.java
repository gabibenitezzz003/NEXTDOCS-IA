package com.nextdocs.ai.restControladores;

import java.io.Serializable;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class BootstrapReqModel implements Serializable {

	private static final long serialVersionUID = 1L;

	@NotBlank
	@Size(max = 64)
	private String codigoTenant;

	@NotBlank
	@Size(max = 256)
	private String nombreTenant;

	@NotBlank
	@Email
	@Size(max = 256)
	private String emailAdministrador;

	@NotBlank
	@Size(min = 12, max = 128)
	private String claveAdministrador;
}
