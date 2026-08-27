package com.nextdocs.ai.config;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class AlmacenamientoConfig {

	private final PropiedadesAlmacenamiento propiedades;

	public AlmacenamientoConfig(PropiedadesAlmacenamiento propiedades) {
		this.propiedades = propiedades;
	}

	@Bean
	public S3Client clienteS3() {
		return S3Client.builder()
				.endpointOverride(URI.create(propiedades.getEndpoint()))
				.region(Region.of(propiedades.getRegion()))
				.credentialsProvider(proveedorCredenciales())
				.serviceConfiguration(configuracionServicio())
				.build();
	}

	@Bean
	public S3Presigner firmadorS3() {
		return S3Presigner.builder()
				.endpointOverride(URI.create(propiedades.getEndpoint()))
				.region(Region.of(propiedades.getRegion()))
				.credentialsProvider(proveedorCredenciales())
				.serviceConfiguration(configuracionServicio())
				.build();
	}

	private StaticCredentialsProvider proveedorCredenciales() {
		return StaticCredentialsProvider
				.create(AwsBasicCredentials.create(propiedades.getClaveAcceso(), propiedades.getClaveSecreta()));
	}

	private S3Configuration configuracionServicio() {
		return S3Configuration.builder().pathStyleAccessEnabled(propiedades.isRutaEstiloForzado()).build();
	}
}
