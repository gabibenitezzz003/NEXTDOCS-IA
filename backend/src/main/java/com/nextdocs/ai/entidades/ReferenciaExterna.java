package com.nextdocs.ai.entidades;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.Data;

@Data
@Embeddable
public class ReferenciaExterna implements Serializable {
	private static final long serialVersionUID = 2715638153012551647L;

	@Column(length = 64)
	private String origen;

	@Column(length = 64)
	private String tipoObjeto;

	@Column(length = 128)
	private String idObjeto;

	@Column(length = 128)
	private String tenantOrigen;

	public boolean estaDefinida() {
		return origen != null && tipoObjeto != null && idObjeto != null;
	}
}
