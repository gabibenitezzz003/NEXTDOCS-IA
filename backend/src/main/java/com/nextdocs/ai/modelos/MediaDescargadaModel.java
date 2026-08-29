package com.nextdocs.ai.modelos;

import java.io.Serializable;

import lombok.Data;

@Data
public class MediaDescargadaModel implements Serializable {

	private static final long serialVersionUID = 3379084467302115934L;

	private String identificadorMedia;

	private String nombreArchivo;

	private String tipoMime;

	private long tamanoDeclarado;

	private byte[] contenido;

	private String codigoRechazo;

	private String motivo;

	public boolean estaDisponible() {
		return contenido != null && contenido.length > 0;
	}
}
