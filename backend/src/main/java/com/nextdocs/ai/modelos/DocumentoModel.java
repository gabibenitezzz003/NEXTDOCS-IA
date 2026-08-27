package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.enumeraciones.EstadoDocumento;
import com.nextdocs.ai.enumeraciones.OrigenDocumento;

import lombok.Data;

@Data
public class DocumentoModel implements Serializable {

	private static final long serialVersionUID = 4948013724477651509L;

	private String id;

	private EstadoDocumento estado;

	private OrigenDocumento origen;

	private String nombre;

	private String hashContenido;

	private String correlacionId;

	private String codigoPlantilla;

	private String nombrePlantilla;

	private String versionPlantillaId;

	private int numeroVersionPlantilla;

	private String documentoPadreId;

	private int paginaDesde;

	private int paginaHasta;

	private int cantidadSegmentos;

	private String sujetoOrigen;

	private String sujetoTipoObjeto;

	private String sujetoIdObjeto;

	private String remitente;

	private String observacion;

	private String ingresadoPor;

	private boolean retencionLegal;

	private Instant recibido;

	private Instant procesado;

	private Instant cerrado;

	private Instant alta;

	private List<ArchivoDocumentoModel> archivos = new ArrayList<>();

	private List<EstadoDocumento> transicionesPosibles = new ArrayList<>();
}
