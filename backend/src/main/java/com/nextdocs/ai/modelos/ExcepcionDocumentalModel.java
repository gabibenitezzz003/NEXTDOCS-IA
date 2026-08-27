package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.EstadoExcepcion;
import com.nextdocs.ai.enumeraciones.PrioridadExcepcion;
import com.nextdocs.ai.enumeraciones.SeveridadHallazgo;
import com.nextdocs.ai.enumeraciones.TipoExcepcion;

import lombok.Data;

@Data
public class ExcepcionDocumentalModel implements Serializable {

	private static final long serialVersionUID = 1907745580042216394L;

	private String id;

	private String documentoId;

	private String nombreDocumento;

	private TipoExcepcion tipo;

	private SeveridadHallazgo severidad;

	private PrioridadExcepcion prioridad;

	private EstadoExcepcion estado;

	private String codigo;

	private String detalle;

	private String responsable;

	private String resueltaPor;

	private String resolucion;

	private boolean vencida;

	private Instant venceEn;

	private Instant resuelta;

	private Instant alta;
}
