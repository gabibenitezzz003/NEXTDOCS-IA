package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.enumeraciones.EstadoDocumento;
import com.nextdocs.ai.enumeraciones.OrigenDocumento;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NuevoLoteExportacionReqModel implements Serializable {

	private static final long serialVersionUID = 7761084253300197462L;

	@Size(max = 180)
	private String nombre;

	private List<EstadoDocumento> estados = new ArrayList<>();

	private OrigenDocumento origen;

	private String codigoPlantilla;

	private String texto;

	private String tipoObjeto;

	private String idObjeto;

	private Instant desde;

	private Instant hasta;

	private Boolean soloRaiz;

	private String orden;

	private boolean incluirOriginales = true;
}
