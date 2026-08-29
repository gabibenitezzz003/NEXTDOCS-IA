package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class SolicitudClasificacionModel implements Serializable {

	private static final long serialVersionUID = 1180463357744029128L;

	private String tenantId;

	private String documentoId;

	private String nombreArchivo;

	private String tipoMime;

	private byte[] contenido;

	private int paginas;

	private String correlacionId;

	private List<TipoCandidatoModel> candidatos = new ArrayList<>();
}
