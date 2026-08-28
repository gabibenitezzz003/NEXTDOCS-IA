package com.nextdocs.ai.modelos;

import java.io.Serializable;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class TenantReqModel implements Serializable {

	private static final long serialVersionUID = 8010224539027261650L;

	@NotBlank
	@Size(max = 256)
	private String nombre;

	@Size(max = 64)
	private String plan;

	@Size(max = 64)
	private String region;

	@Size(max = 256)
	private String dominio;

	@Min(0)
	private long cuotaAlmacenamientoBytes;
}
