package com.nextdocs.ai.integracion;

import com.nextdocs.ai.config.PropiedadesAlmacenamiento;

import jakarta.annotation.PostConstruct;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

@Component
@Profile("prueba")
public class PreparadorBuckets {

	private final S3Client clienteS3;

	private final PropiedadesAlmacenamiento propiedades;

	public PreparadorBuckets(S3Client clienteS3, PropiedadesAlmacenamiento propiedades) {
		this.clienteS3 = clienteS3;
		this.propiedades = propiedades;
	}

	@PostConstruct
	public void crearBuckets() {
		crear(propiedades.getBucketDocumentos());
		crear(propiedades.getBucketExportaciones());
		crear(propiedades.getBucketCuarentena());
	}

	private void crear(String bucket) {
		try {
			clienteS3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
		} catch (Exception e) {
			return;
		}
	}
}
