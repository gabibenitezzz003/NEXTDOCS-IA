package com.nextdocs.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "nextdocs.bootstrap")
public class PropiedadesBootstrap {

	private boolean habilitado;

	private String secreto;
}
