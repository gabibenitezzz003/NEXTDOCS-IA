package com.nextdocs.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "nextdocs.almacenamiento")
public class PropiedadesAlmacenamiento {

	private String endpoint;

	private String region;

	private String claveAcceso;

	private String claveSecreta;

	private String bucketDocumentos;

	private String bucketExportaciones;

	private boolean rutaEstiloForzado;

	private int duracionUrlFirmadaMinutos;
}
