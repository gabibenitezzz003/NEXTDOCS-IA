package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.SeveridadHallazgo;

import lombok.Data;

@Data
public class HallazgoValidacionModel implements Serializable {

	private static final long serialVersionUID = 2201995563390417458L;

	private String id;

	private String codigoRegla;

	private String claveCampo;

	private SeveridadHallazgo severidad;

	private String mensaje;

	private String evidencia;

	private boolean sobreescrito;

	private String motivoSobreescritura;

	private String sobreescritoPor;

	private Instant alta;
}
