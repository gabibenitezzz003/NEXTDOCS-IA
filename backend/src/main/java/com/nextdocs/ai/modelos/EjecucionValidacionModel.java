package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.enumeraciones.EstadoEjecucion;
import com.nextdocs.ai.enumeraciones.ResultadoValidacion;

import lombok.Data;

@Data
public class EjecucionValidacionModel implements Serializable {

	private static final long serialVersionUID = 5117304488260139673L;

	private String id;

	private EstadoEjecucion estado;

	private ResultadoValidacion resultado;

	private int cantidadHallazgos;

	private int cantidadBloqueantes;

	private boolean autoaprobado;

	private String motivoResultado;

	private Instant inicio;

	private Instant fin;

	private List<HallazgoValidacionModel> hallazgos = new ArrayList<>();
}
