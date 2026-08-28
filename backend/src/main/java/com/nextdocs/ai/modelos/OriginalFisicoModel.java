package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.enumeraciones.EstadoOriginalFisico;
import com.nextdocs.ai.enumeraciones.PoliticaOriginalFisico;

import lombok.Data;

@Data
public class OriginalFisicoModel implements Serializable {

	private static final long serialVersionUID = 3958221074450663812L;

	private String id;

	private String documentoId;

	private String nombreDocumento;

	private EstadoOriginalFisico estado;

	private PoliticaOriginalFisico politica;

	private boolean bloqueaCierre;

	private boolean pendiente;

	private String ubicacion;

	private String referenciaFisica;

	private String recibidoPor;

	private String registradoPor;

	private String observacion;

	private Instant recibido;

	private Instant archivado;

	private Instant extraviado;

	private Instant alta;

	private List<EstadoOriginalFisico> transicionesPosibles = new ArrayList<>();
}
