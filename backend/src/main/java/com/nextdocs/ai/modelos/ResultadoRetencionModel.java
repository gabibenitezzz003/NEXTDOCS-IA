package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.enumeraciones.AccionRetencion;
import com.nextdocs.ai.enumeraciones.ResultadoRetencion;

import lombok.Data;

@Data
public class ResultadoRetencionModel implements Serializable {

	private static final long serialVersionUID = 1140773625019428803L;

	private String documentoId;

	private String nombreDocumento;

	private ResultadoRetencion resultado;

	private AccionRetencion accion;

	private String clasePolitica;

	private String motivo;

	private Instant retenerHasta;

	private Instant aplicada;

	private String hashContenido;

	private int archivosEliminados;

	private int valoresAnonimizados;

	private List<String> clavesAnonimizadas = new ArrayList<>();
}
