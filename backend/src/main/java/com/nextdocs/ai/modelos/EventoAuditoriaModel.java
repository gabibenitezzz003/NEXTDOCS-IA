package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.TipoActor;

import lombok.Data;

@Data
public class EventoAuditoriaModel implements Serializable {

	private static final long serialVersionUID = 8106264573312094487L;

	private String id;

	private TipoActor tipoActor;

	private String idActor;

	private String descripcionActor;

	private AccionAuditoria accion;

	private String tipoRecurso;

	private String idRecurso;

	private String hashAntes;

	private String hashDespues;

	private String detalle;

	private String direccionIp;

	private String agenteUsuario;

	private String correlacionId;

	private boolean exitoso;

	private Instant fecha;
}
