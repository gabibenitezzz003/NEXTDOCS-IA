package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.enumeraciones.ProveedorDocumentalIa;

import lombok.Data;

@Data
public class TrazabilidadDocumentoModel implements Serializable {

	private static final long serialVersionUID = 6294570318825440917L;

	private DocumentoModel documento;

	private String codigoPlantilla;

	private String nombrePlantilla;

	private String versionPlantillaId;

	private int numeroVersionPlantilla;

	private String versionPrompt;

	private String versionEsquema;

	private BigDecimal umbralAutoaprobacion;

	private ProveedorDocumentalIa proveedor;

	private String modelo;

	private String asociacionSeleccionada;

	private String revisor;

	private boolean completa;

	private Instant generada;

	private List<String> faltantes = new ArrayList<>();

	private List<EjecucionExtraccionModel> extracciones = new ArrayList<>();

	private List<EjecucionValidacionModel> validaciones = new ArrayList<>();

	private List<ReglaPlantillaModel> reglas = new ArrayList<>();

	private List<CandidatoAsociacionModel> candidatos = new ArrayList<>();

	private List<RevisionDocumentoModel> revisiones = new ArrayList<>();

	private List<EventoAuditoriaModel> eventos = new ArrayList<>();
}
