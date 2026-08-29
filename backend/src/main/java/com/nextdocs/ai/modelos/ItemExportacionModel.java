package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;

import lombok.Data;

@Data
public class ItemExportacionModel implements Serializable {

	private static final long serialVersionUID = 3348820156691447093L;

	private String id;

	private String documentoId;

	private String tipoObjeto;

	private String idObjeto;

	private String codigoPlantilla;

	private int numeroVersionPlantilla;

	private String estadoDocumento;

	private String nombreEnArchivo;

	private long tamanoBytes;

	private String sha256;

	private int hallazgos;

	private String motivoOmision;

	private Instant recibido;

	private Instant cerrado;
}
