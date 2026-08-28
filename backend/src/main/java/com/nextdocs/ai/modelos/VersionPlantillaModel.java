package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.enumeraciones.EstadoPlantilla;
import com.nextdocs.ai.enumeraciones.PoliticaOriginalFisico;

import lombok.Data;

@Data
public class VersionPlantillaModel implements Serializable {

	private static final long serialVersionUID = 9014422850976330117L;

	private String id;

	private String plantillaId;

	private String codigoPlantilla;

	private int numero;

	private EstadoPlantilla estado;

	private boolean editable;

	private boolean esVersionPublicada;

	private BigDecimal umbralAutoaprobacion;

	private PoliticaOriginalFisico politicaOriginalFisico;

	private String versionPrompt;

	private String versionEsquema;

	private String instruccionExtraccion;

	private String notasCambio;

	private String publicadaPor;

	private Instant publicada;

	private Instant alta;

	private long bloqueoOptimista;

	private List<EstadoPlantilla> transicionesPosibles = new ArrayList<>();

	private List<CampoPlantillaModel> campos = new ArrayList<>();

	private List<ReglaPlantillaModel> reglas = new ArrayList<>();
}
