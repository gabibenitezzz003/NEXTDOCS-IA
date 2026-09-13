package com.nextdocs.ai.config;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class AlmacenamientoConfig {

	private final PropiedadesAlmacenamiento propiedades;

	private final Environment entorno;

	public AlmacenamientoConfig(PropiedadesAlmacenamiento propiedades, Environment entorno) {
		this.propiedades = propiedades;
		this.entorno = entorno;
	}

	@Bean
	public S3Client clienteS3() {
		var builder = S3Client.builder()
				.region(Region.of(propiedades.getRegion()))
				.credentialsProvider(proveedorCredenciales())
				.serviceConfiguration(configuracionServicio());
		if (!esProduccion()) {
			builder.endpointOverride(URI.create(propiedades.getEndpoint()));
		}
		return builder.build();
	}

	@Bean
	public S3Presigner firmadorS3() {
		var builder = S3Presigner.builder()
				.region(Region.of(propiedades.getRegion()))
				.credentialsProvider(proveedorCredenciales())
				.serviceConfiguration(configuracionServicio());
		if (!esProduccion()) {
			builder.endpointOverride(URI.create(endpointParaFirmar()));
		}
		return builder.build();
	}

	String endpointParaFirmar() {
		String endpointPublico = propiedades.getEndpointPublico();
		return endpointPublico != null && !endpointPublico.isBlank() ? endpointPublico : propiedades.getEndpoint();
	}

	AwsCredentialsProvider proveedorCredenciales() {
		if (esProduccion()) {
			return DefaultCredentialsProvider.create();
		}
		return StaticCredentialsProvider
				.create(AwsBasicCredentials.create(propiedades.getClaveAcceso(), propiedades.getClaveSecreta()));
	}

	private boolean esProduccion() {
		return entorno.matchesProfiles(ValidadorConfiguracionProduccion.PERFIL_PRODUCCION);
	}

	private S3Configuration configuracionServicio() {
		return S3Configuration.builder().pathStyleAccessEnabled(propiedades.isRutaEstiloForzado()).build();
	}
}
