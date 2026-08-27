package com.nextdocs.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "nextdocs.webhooks")
public class PropiedadesWebhooks {

	private int intentosMaximos;

	private int tiempoEsperaSegundos;
}
