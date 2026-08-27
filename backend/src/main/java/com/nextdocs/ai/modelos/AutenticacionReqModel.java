package com.nextdocs.ai.modelos;

import java.io.Serializable;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class AutenticacionReqModel implements Serializable {

	private static final long serialVersionUID = 7796124855036918302L;

	@NotBlank
	@Size(max = 64)
	private String codigoTenant;

	@NotBlank
	@Email
	@Size(max = 256)
	private String email;

	@NotBlank
	@Size(max = 128)
	private String clave;
}
