package com.nextdocs.ai.modelos;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class IdiomaReqModel implements Serializable {

	private static final long serialVersionUID = 7812345601982745361L;

	@NotBlank
	private String idioma;
}
