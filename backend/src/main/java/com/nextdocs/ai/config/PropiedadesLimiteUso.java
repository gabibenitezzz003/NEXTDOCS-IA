package com.nextdocs.ai.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "nextdocs.limite-uso")
public class PropiedadesLimiteUso {

	private boolean activo;

	private int peticionesPorMinutoPorPrincipal;

	private int peticionesPorMinutoPorTenant;

	private int ingestasPorMinutoPorTenant;

	private List<String> rutasIngesta = new ArrayList<>();

	private List<String> rutasExentas = new ArrayList<>();
}
