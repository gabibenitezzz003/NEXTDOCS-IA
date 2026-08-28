package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class UsuarioReqModel implements Serializable {

	private static final long serialVersionUID = 8823079160459152534L;

	@NotBlank
	@Email
	@Size(max = 256)
	private String email;

	@NotBlank
	@Size(max = 256)
	private String nombre;

	@Size(max = 128)
	private String clave;

	@Size(max = 16)
	private String idioma;

	@Size(max = 64)
	private String zonaHoraria;

	private List<String> roles = new ArrayList<>();
}
