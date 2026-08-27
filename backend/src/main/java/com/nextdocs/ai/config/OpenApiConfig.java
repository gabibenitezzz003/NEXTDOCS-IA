package com.nextdocs.ai.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	public static final String ESQUEMA_JWT = "jwt";

	public static final String ESQUEMA_CUENTA_SERVICIO = "cuentaServicio";

	@Bean
	public OpenAPI definicionApi() {
		return new OpenAPI()
				.info(new Info()
						.title("NEXT DOC AI")
						.version("v1")
						.description("Inteligencia documental y automatizacion de procesos"))
				.components(new Components()
						.addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT"))
						.addSecuritySchemes(ESQUEMA_CUENTA_SERVICIO, new SecurityScheme()
								.type(SecurityScheme.Type.APIKEY)
								.in(SecurityScheme.In.HEADER)
								.name("X-Clave-Servicio")))
				.addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT))
				.addSecurityItem(new SecurityRequirement().addList(ESQUEMA_CUENTA_SERVICIO));
	}
}
