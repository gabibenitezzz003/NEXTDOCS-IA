package com.nextdocs.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "nextdocs.federacion")
public class PropiedadesFederacion {

	private int tiempoEsperaMilisegundos;

	private int segundosCacheJwks;

	private int toleranciaRelojSegundos;

	private int segundosVigenciaCodigo;

	private boolean exigirEmisorSeguro;
}
