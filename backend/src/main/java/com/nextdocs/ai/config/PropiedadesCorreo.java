package com.nextdocs.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "nextdocs.correo")
public class PropiedadesCorreo {

	private boolean activo;

	private int mensajesPorCiclo;

	private int tiempoEsperaMilisegundos;

	private int fallosParaPausar;

	private int diasVigenciaCorrelacion;

	private long tamanoMaximoAdjuntoBytes;
}
