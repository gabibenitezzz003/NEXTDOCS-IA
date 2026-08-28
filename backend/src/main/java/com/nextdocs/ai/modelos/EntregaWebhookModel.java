package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.EstadoEntregaWebhook;
import com.nextdocs.ai.enumeraciones.TipoEventoCanonico;

import lombok.Data;

@Data
public class EntregaWebhookModel implements Serializable {

	private static final long serialVersionUID = 6619037742155083361L;

	private String id;

	private String suscripcionId;

	private String nombreSuscripcion;

	private String eventoId;

	private TipoEventoCanonico tipoEvento;

	private EstadoEntregaWebhook estado;

	private int intento;

	private int codigoRespuesta;

	private String cuerpoRespuesta;

	private long duracionMilisegundos;

	private Instant disponibleEn;

	private Instant entregado;

	private Instant alta;
}
