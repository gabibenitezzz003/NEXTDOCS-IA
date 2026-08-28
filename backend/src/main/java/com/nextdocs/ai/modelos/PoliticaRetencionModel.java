package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.AccionRetencion;

import lombok.Data;

@Data
public class PoliticaRetencionModel implements Serializable {

	private static final long serialVersionUID = 3068849257741623390L;

	private String id;

	private String clase;

	private String descripcion;

	private int duracionDias;

	private AccionRetencion accion;

	private boolean permiteRetencionLegal;

	private boolean activa;

	private Instant alta;
}
