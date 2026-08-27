package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;

import lombok.Data;

@Data
public class ArchivoDocumentoModel implements Serializable {

	private static final long serialVersionUID = 8825760124430298855L;

	private String id;

	private String nombreArchivo;

	private String tipoMime;

	private String extension;

	private long tamano;

	private String checksum;

	private int paginas;

	private int version;

	private boolean original;

	private Instant alta;
}
