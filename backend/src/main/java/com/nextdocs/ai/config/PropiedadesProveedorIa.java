package com.nextdocs.ai.config;

import com.nextdocs.ai.enumeraciones.ProveedorDocumentalIa;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "nextdocs.proveedor-ia")
public class PropiedadesProveedorIa {

	private ProveedorDocumentalIa proveedorPorDefecto;

	private int intentosMaximos;

	private long esperaInicialMilisegundos;

	private double multiplicadorEspera;

	private long esperaMaximaMilisegundos;

	private int umbralCircuitoAbierto;

	private long duracionCircuitoAbiertoSegundos;
}
