package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class RolModel implements Serializable {

	private static final long serialVersionUID = 2699253106913827015L;

	private String id;

	private String codigo;

	private String nombre;

	private String descripcion;

	private boolean predefinido;

	private long cantidadUsuarios;

	private Instant alta;

	private List<String> permisos = new ArrayList<>();
}
