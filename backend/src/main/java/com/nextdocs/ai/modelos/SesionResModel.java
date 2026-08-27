package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class SesionResModel implements Serializable {

	private static final long serialVersionUID = 6633024485912057741L;

	private String tokenAcceso;

	private String tokenRefresco;

	private String usuarioId;

	private String email;

	private String nombre;

	private String tenantId;

	private String codigoTenant;

	private String nombreTenant;

	private List<String> permisos = new ArrayList<>();
}
