package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class SolicitudExtraccionModel implements Serializable {

	private static final long serialVersionUID = 4471985502218733964L;

	private String tenantId;

	private String documentoId;

	private String nombreArchivo;

	private String tipoMime;

	private byte[] contenido;

	private int paginas;

	private String codigoPlantilla;

	private String instruccionExtraccion;

	private String versionPrompt;

	private String versionEsquema;

	private String modelo;

	private String correlacionId;

	private List<CampoEsquemaModel> campos = new ArrayList<>();

	private List<String> pistas = new ArrayList<>();
}
