package com.nextdocs.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "nextdocs.documental")
public class PropiedadesDocumental {

	private String urlBase = "";

	private String claveAdmin = "";

	private int tiempoEsperaMilisegundos = 8000;

	private int enfriamientoFalloSegundos = 60;

	public boolean provisionHabilitada() {
		return urlBase != null && !urlBase.isBlank() && claveAdmin != null && !claveAdmin.isBlank();
	}
}
