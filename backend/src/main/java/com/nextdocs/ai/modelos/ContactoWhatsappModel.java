package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class ContactoWhatsappModel implements Serializable {

	private static final long serialVersionUID = 4472106938201755430L;

	private String id;

	@NotBlank
	@Size(max = 64)
	private String patron;

	@Size(max = 256)
	private String descripcion;

	private Instant alta;
}
