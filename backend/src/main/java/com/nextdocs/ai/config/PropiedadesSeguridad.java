package com.nextdocs.ai.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "nextdocs.seguridad")
public class PropiedadesSeguridad {

	private String jwtSecreto;

	private String jwtEmisor;

	private int jwtDuracionMinutos;

	private int jwtDuracionRefrescoDias;

	private List<String> rutasPublicas = new ArrayList<>();
}
