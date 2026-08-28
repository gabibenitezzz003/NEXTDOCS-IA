package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.enumeraciones.TipoEventoCanonico;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class SuscripcionWebhookReqModel implements Serializable {

	private static final long serialVersionUID = 4471028836519207741L;

	@NotBlank
	@Size(max = 128)
	private String nombre;

	@NotBlank
	@Size(max = 1024)
	private String url;

	@NotEmpty
	private List<TipoEventoCanonico> eventos = new ArrayList<>();
}
