package com.nextdocs.ai.modelos;

import java.io.Serializable;

import lombok.Data;

@Data
public class MediaWhatsappModel implements Serializable {

	private static final long serialVersionUID = 2201458640973305851L;

	private String id;

	private String documentoId;

	private String identificadorMedia;

	private String nombreArchivo;

	private String tipoMime;

	private long tamanoBytes;

	private String sha256;

	private String resultado;

	private String codigoRechazo;

	private String motivo;
}
