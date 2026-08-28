package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class PlantillaModel implements Serializable {

	private static final long serialVersionUID = 1178943350226610484L;

	private String id;

	private String codigo;

	private String nombre;

	private String familia;

	private String descripcion;

	private String versionPublicadaId;

	private int numeroVersionPublicada;

	private int cantidadVersiones;

	private String creadoPor;

	private Instant alta;

	private long bloqueoOptimista;

	private List<VersionPlantillaModel> versiones = new ArrayList<>();
}
