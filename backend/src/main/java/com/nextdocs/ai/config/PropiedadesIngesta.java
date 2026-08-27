package com.nextdocs.ai.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "nextdocs.ingesta")
public class PropiedadesIngesta {

	private long tamanoMaximoBytes;

	private List<String> tiposMimePermitidos = new ArrayList<>();

	private List<String> extensionesPermitidas = new ArrayList<>();
}
