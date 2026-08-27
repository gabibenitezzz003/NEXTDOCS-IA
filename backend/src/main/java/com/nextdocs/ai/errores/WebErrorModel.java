package com.nextdocs.ai.errores;

import java.time.Instant;

import org.springframework.http.HttpStatus;

import lombok.Data;

@Data
public class WebErrorModel {

	private String mensaje;

	private String ruta;

	private int codigo;

	private String estado;

	private String correlacionId;

	private Instant fecha;

	public WebErrorModel(String mensaje, String ruta, HttpStatus estado, String correlacionId) {
		this.mensaje = mensaje;
		this.ruta = ruta;
		this.codigo = estado.value();
		this.estado = estado.name();
		this.correlacionId = correlacionId;
		this.fecha = Instant.now();
	}
}
