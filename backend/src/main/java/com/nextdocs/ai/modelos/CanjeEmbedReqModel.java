package com.nextdocs.ai.modelos;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class CanjeEmbedReqModel implements Serializable {

	private static final long serialVersionUID = 2019945673308814520L;

	@NotBlank
	@Size(max = 128)
	private String codigo;
}
