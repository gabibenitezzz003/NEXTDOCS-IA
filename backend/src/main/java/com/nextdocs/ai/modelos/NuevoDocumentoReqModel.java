package com.nextdocs.ai.modelos;

import java.io.Serializable;

import com.nextdocs.ai.enumeraciones.OrigenDocumento;

import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class NuevoDocumentoReqModel implements Serializable {

	private static final long serialVersionUID = 5540298117364022905L;

	private OrigenDocumento origen;

	@Size(max = 64)
	private String codigoPlantilla;

	@Size(max = 256)
	private String remitente;

	@Size(max = 1024)
	private String observacion;

	@Size(max = 64)
	private String sujetoOrigen;

	@Size(max = 64)
	private String sujetoTipoObjeto;

	@Size(max = 128)
	private String sujetoIdObjeto;
}
