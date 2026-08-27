package com.nextdocs.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "nextdocs.cola")
public class PropiedadesCola {

	private String nombreExtraccion;

	private String nombreReintento;

	private int concurrenciaTrabajadores;

	private long intervaloSondeoMilisegundos;
}
