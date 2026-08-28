package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;

import lombok.Data;

@Data
public class CasoPruebaModel implements Serializable {

	private static final long serialVersionUID = 3182046619027741540L;

	private String id;

	private String nombre;

	private String nombreArchivo;

	private String tipoMime;

	private String esperado;

	private int orden;

	private Instant alta;
}
