package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.TipoActor;

import lombok.Data;

@Data
public class FiltroAuditoriaModel implements Serializable {

	private static final long serialVersionUID = 5539208874416023101L;

	private Instant desde;

	private Instant hasta;

	private AccionAuditoria accion;

	private String tipoRecurso;

	private String idRecurso;

	private TipoActor tipoActor;

	private String idActor;

	private String correlacionId;

	private Boolean exitoso;

	public boolean estaVacio() {
		return desde == null && hasta == null && accion == null && tipoRecurso == null && idRecurso == null
				&& tipoActor == null && idActor == null && correlacionId == null && exitoso == null;
	}
}
