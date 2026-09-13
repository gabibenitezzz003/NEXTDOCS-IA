package com.nextdocs.ai.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;

class AlmacenamientoConfigTest {

	@Test
	@DisplayName("en produccion las credenciales salen de la cadena por defecto (rol IAM)")
	void produccionUsaCredencialesDeIam() {
		AlmacenamientoConfig configuracion = configuracion("produccion");

		AwsCredentialsProvider proveedor = configuracion.proveedorCredenciales();

		assertThat(proveedor).isInstanceOf(DefaultCredentialsProvider.class);
	}

	@Test
	@DisplayName("fuera de produccion las credenciales son las del object store local")
	void desarrolloUsaCredencialesEstaticas() {
		AlmacenamientoConfig configuracion = configuracion("desarrollo");

		AwsCredentialsProvider proveedor = configuracion.proveedorCredenciales();

		assertThat(proveedor).isInstanceOf(StaticCredentialsProvider.class);
	}

	@Test
	@DisplayName("en produccion no se pisa el endpoint: se resuelve S3 real")
	void produccionUsaEndpointAws() {
		AlmacenamientoConfig configuracion = configuracion("produccion");

		try (S3Client cliente = configuracion.clienteS3()) {
			assertThat(cliente.serviceClientConfiguration().endpointOverride()).isEmpty();
		}
	}

	@Test
	@DisplayName("fuera de produccion el endpoint apunta al MinIO local")
	void desarrolloUsaEndpointMinio() {
		AlmacenamientoConfig configuracion = configuracion("desarrollo");

		try (S3Client cliente = configuracion.clienteS3()) {
			assertThat(cliente.serviceClientConfiguration().endpointOverride())
					.isPresent()
					.get()
					.hasToString("http://localhost:9102");
		}
	}

	private AlmacenamientoConfig configuracion(String perfil) {
		StandardEnvironment entorno = new StandardEnvironment();
		entorno.setActiveProfiles(perfil);
		return new AlmacenamientoConfig(propiedades(), entorno);
	}

	private PropiedadesAlmacenamiento propiedades() {
		PropiedadesAlmacenamiento propiedades = new PropiedadesAlmacenamiento();
		propiedades.setEndpoint("http://localhost:9102");
		propiedades.setRegion("us-east-1");
		propiedades.setClaveAcceso("nextdocs");
		propiedades.setClaveSecreta("nextdocs123");
		propiedades.setRutaEstiloForzado(true);
		return propiedades;
	}
}
