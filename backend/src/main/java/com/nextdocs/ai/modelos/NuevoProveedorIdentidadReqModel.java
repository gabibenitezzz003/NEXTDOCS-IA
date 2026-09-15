package com.nextdocs.ai.modelos;

import java.io.Serializable;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class NuevoProveedorIdentidadReqModel implements Serializable {

	private static final long serialVersionUID = 6612009837744012235L;

	@NotBlank
	@Size(max = 64)
	private String codigo;

	@NotBlank
	@Size(max = 128)
	private String nombre;

	@NotBlank
	@Size(max = 32)
	private String origen;

	@NotBlank
	@Size(max = 512)
	private String emisor;

	@NotBlank
	@Size(max = 512)
	private String urlJwks;

	@Size(max = 256)
	private String audiencia;

	@Size(max = 64)
	private String claimSujeto;

	@Size(max = 64)
	private String claimEmail;

	@Size(max = 64)
	private String claimNombre;

	private boolean permitirJit;

	private boolean permitirVinculoPorEmail;

	@Size(max = 64)
	private String codigoRolPorDefecto;

	private String dominiosPermitidos;

	private String origenesEmbedPermitidos;

	@Min(0)
	@Max(600)
	private int segundosVigenciaCodigo;

	@Size(max = 256)
	private String clienteId;

	@Size(max = 512)
	private String clienteSecreto;

	@Size(max = 512)
	private String urlAutorizacion;

	@Size(max = 512)
	private String urlToken;

	@Size(max = 512)
	private String alcances;
}
