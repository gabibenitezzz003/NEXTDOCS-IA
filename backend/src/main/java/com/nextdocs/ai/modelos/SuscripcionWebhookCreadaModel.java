package com.nextdocs.ai.modelos;

import java.io.Serializable;

import lombok.Data;

@Data
public class SuscripcionWebhookCreadaModel implements Serializable {

	private static final long serialVersionUID = 9024417736512083364L;

	private SuscripcionWebhookModel suscripcion;

	private String secreto;

	private String advertencia;
}
