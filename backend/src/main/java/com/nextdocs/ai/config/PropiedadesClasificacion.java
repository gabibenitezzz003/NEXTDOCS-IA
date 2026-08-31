package com.nextdocs.ai.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "nextdocs.clasificacion")
public class PropiedadesClasificacion {

	private boolean activa;

	private BigDecimal confianzaMinima;

	private int maximoCandidatos;
}
