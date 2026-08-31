package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class LoteExportacionModel implements Serializable {

	private static final long serialVersionUID = 6612093357744810266L;

	private String id;

	private String estado;

	private String nombre;

	private String filtros;

	private String orden;

	private boolean incluirOriginales;

	private int cantidadDocumentos;

	private int cantidadOmitidos;

	private String nombreArchivo;

	private long tamanoBytes;

	private String sha256;

	private String detalleError;

	private String solicitadoPor;

	private Instant venceEn;

	private Instant generado;

	private Instant alta;

	private boolean descargable;

	private List<ItemExportacionModel> items = new ArrayList<>();
}
