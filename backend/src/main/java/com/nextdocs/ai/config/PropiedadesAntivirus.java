package com.nextdocs.ai.config;

import com.nextdocs.ai.enumeraciones.MotorAntivirus;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "nextdocs.antivirus")
public class PropiedadesAntivirus {

	private MotorAntivirus motor;

	private String host;

	private int puerto;

	private int tiempoEsperaMilisegundos;

	private int tamanoBloque;

	private boolean rechazarSiNoDisponible;

	private boolean permitirSinAnalisis;
}
