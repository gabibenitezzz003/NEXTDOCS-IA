package com.nextdocs.ai.modelos;

import java.io.Serializable;

import com.nextdocs.ai.enumeraciones.SeveridadHallazgo;
import com.nextdocs.ai.enumeraciones.TipoReglaValidacion;

import lombok.Data;

@Data
public class ReglaPlantillaModel implements Serializable {

	private static final long serialVersionUID = 7749350226118004745L;

	private String id;

	private String codigo;

	private String nombre;

	private TipoReglaValidacion tipo;

	private SeveridadHallazgo severidad;

	private String campoObjetivo;

	private String configuracion;

	private String mensaje;

	private boolean activa;

	private int orden;
}
