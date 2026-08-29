package com.nextdocs.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "nextdocs.whatsapp")
public class PropiedadesWhatsapp {

	private boolean activo;

	private String urlGraph;

	private String versionGraph;

	private String urlPublicaWebhook;

	private int tiempoEsperaMilisegundos;

	private long tamanoMaximoMediaBytes;

	private int diasVigenciaCorrelacion;

	private int minutosVentanaCorrelacion;

	private int minutosVentanaRespuesta;

	private int fallosParaPausar;

	private boolean exigirFirma;
}
