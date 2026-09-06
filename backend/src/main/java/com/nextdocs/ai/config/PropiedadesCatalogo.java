package com.nextdocs.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "nextdocs.catalogo")
public class PropiedadesCatalogo {

	private boolean sembrarTenantsNuevos;
}
