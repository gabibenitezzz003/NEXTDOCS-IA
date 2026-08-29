package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class TipoCandidatoModel implements Serializable {

	private static final long serialVersionUID = 6612348907745120033L;

	private String codigo;

	private String nombre;

	private String descripcion;

	private List<String> camposClave = new ArrayList<>();
}
