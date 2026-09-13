package com.nextdocs.ai.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import java.net.URL;
import java.time.Duration;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

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

	@Test
	@DisplayName("el firmador firma URLs con el endpoint publico cuando esta configurado")
	void firmadorUsaEndpointPublico() {
		PropiedadesAlmacenamiento propiedades = propiedades();
		propiedades.setEndpoint("http://minio:9000");
		propiedades.setEndpointPublico("http://localhost:9102");
		AlmacenamientoConfig configuracion = new AlmacenamientoConfig(propiedades, entorno("desarrollo"));

		try (S3Presigner firmador = configuracion.firmadorS3()) {
			assertThat(urlFirmada(firmador).getHost()).isEqualTo("localhost");
			assertThat(urlFirmada(firmador).getPort()).isEqualTo(9102);
		}
	}

	@Test
	@DisplayName("sin endpoint publico el firmador cae al endpoint interno")
	void firmadorSinEndpointPublicoUsaEndpointInterno() {
		AlmacenamientoConfig configuracion = configuracion("desarrollo");

		assertThat(configuracion.endpointParaFirmar()).isEqualTo("http://localhost:9102");
	}

	@Test
	@DisplayName("en produccion el firmador no pisa el endpoint: firma contra S3 real")
	void produccionFirmadorUsaEndpointAws() {
		System.setProperty("aws.accessKeyId", "prueba");
		System.setProperty("aws.secretAccessKey", "prueba");
		AlmacenamientoConfig configuracion = configuracion("produccion");

		try (S3Presigner firmador = configuracion.firmadorS3()) {
			assertThat(urlFirmada(firmador).getHost()).endsWith("amazonaws.com");
		} finally {
			System.clearProperty("aws.accessKeyId");
			System.clearProperty("aws.secretAccessKey");
		}
	}

	private URL urlFirmada(S3Presigner firmador) {
		return firmador.presignGetObject(peticion -> peticion
						.signatureDuration(Duration.ofMinutes(5))
						.getObjectRequest(GetObjectRequest.builder().bucket("bucket").key("clave").build()))
				.url();
	}

	private AlmacenamientoConfig configuracion(String perfil) {
		return new AlmacenamientoConfig(propiedades(), entorno(perfil));
	}

	private StandardEnvironment entorno(String perfil) {
		StandardEnvironment entorno = new StandardEnvironment();
		entorno.setActiveProfiles(perfil);
		return entorno;
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
