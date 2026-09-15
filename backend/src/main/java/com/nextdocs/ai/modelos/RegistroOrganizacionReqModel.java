package com.nextdocs.ai.modelos;

import java.io.Serializable;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class RegistroOrganizacionReqModel implements Serializable {

	private static final long serialVersionUID = 1L;

	@NotBlank
	@Size(max = 256)
	private String nombreOrganizacion;

	@NotBlank
	@Size(min = 3, max = 32)
	@Pattern(regexp = "^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?$")
	private String codigoOrganizacion;

	@NotBlank
	@Size(max = 256)
	private String nombreAdministrador;

	@NotBlank
	@Email
	@Size(max = 256)
	private String emailAdministrador;

	@NotBlank
	@Size(min = 12, max = 128)
	private String claveAdministrador;
}
