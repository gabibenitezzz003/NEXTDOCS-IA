package com.nextdocs.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "nextdocs.arranque")
public class PropiedadesArranque {

	private boolean crearTenantDemostracion;

	private String codigoTenant;

	private String nombreTenant;

	private String emailAdministrador;

	private String claveAdministrador;
}
