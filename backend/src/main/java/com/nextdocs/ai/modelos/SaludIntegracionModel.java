package com.nextdocs.ai.modelos;

import java.io.Serializable;

import lombok.Data;

@Data
public class SaludIntegracionModel implements Serializable {

	private static final long serialVersionUID = 5519027743361208841L;

	private long suscripcionesTotales;

	private long suscripcionesActivas;

	private long suscripcionesPausadas;

	private long entregasPendientes;

	private long entregasEntregadas;

	private long entregasAgotadas;

	private int umbralPausa;
}
