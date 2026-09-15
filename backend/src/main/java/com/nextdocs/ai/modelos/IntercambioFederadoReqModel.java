package com.nextdocs.ai.modelos;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class IntercambioFederadoReqModel implements Serializable {

	private static final long serialVersionUID = 4471560028893017254L;

	@NotBlank
	@Size(max = 64)
	private String codigoTenant;

	@NotBlank
	@Size(max = 64)
	private String proveedor;

	@NotBlank
	private String token;

	@Size(max = 64)
	private String tipoObjeto;

	@Size(max = 128)
	private String idObjeto;

	@Size(max = 512)
	private String urlRetorno;

	private boolean tenantPropio;
}
