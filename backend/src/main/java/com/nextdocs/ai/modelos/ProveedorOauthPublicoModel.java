package com.nextdocs.ai.modelos;

import java.io.Serializable;

import lombok.Data;

@Data
public class ProveedorOauthPublicoModel implements Serializable {

	private static final long serialVersionUID = -5710868574946711397L;

	private String codigo;

	private String nombre;
}
