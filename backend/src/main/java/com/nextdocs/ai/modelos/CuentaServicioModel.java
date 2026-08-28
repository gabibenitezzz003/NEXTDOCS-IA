package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class CuentaServicioModel implements Serializable {

	private static final long serialVersionUID = 5504116728834390221L;

	private String id;

	private String nombre;

	private String prefijoClave;

	private boolean activa;

	private boolean expirada;

	private Instant expira;

	private Instant ultimoUso;

	private String motivoRevocacion;

	private Instant alta;

	private List<String> alcances = new ArrayList<>();
}
