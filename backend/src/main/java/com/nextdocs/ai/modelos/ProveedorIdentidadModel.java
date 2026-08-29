package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;

import lombok.Data;

@Data
public class ProveedorIdentidadModel implements Serializable {

	private static final long serialVersionUID = 3308174920655127430L;

	private String id;

	private String codigo;

	private String nombre;

	private String origen;

	private String emisor;

	private String urlJwks;

	private String audiencia;

	private String claimSujeto;

	private String claimEmail;

	private String claimNombre;

	private boolean permitirJit;

	private boolean permitirVinculoPorEmail;

	private String codigoRolPorDefecto;

	private String dominiosPermitidos;

	private String origenesEmbedPermitidos;

	private int segundosVigenciaCodigo;

	private boolean activo;

	private Instant alta;
}
