package com.nextdocs.ai.modelos;

import java.io.Serializable;

import lombok.Data;

@Data
public class AdjuntoCorreoModel implements Serializable {

	private static final long serialVersionUID = 2201933548760118804L;

	private String id;

	private String documentoId;

	private String nombreArchivo;

	private String tipoMime;

	private long tamanoBytes;

	private String sha256;

	private String resultado;

	private String codigoRechazo;

	private String motivo;
}
