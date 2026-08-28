package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.ResultadoEscaneo;

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

	private ResultadoEscaneo resultadoEscaneo;

	private String amenazaDetectada;

	private String motorEscaneo;

	private Instant escaneado;

	private boolean enCuarentena;

	private Instant alta;
}
