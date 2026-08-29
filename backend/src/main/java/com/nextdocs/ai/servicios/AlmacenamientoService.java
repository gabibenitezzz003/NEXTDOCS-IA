package com.nextdocs.ai.servicios;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;

import com.nextdocs.ai.config.PropiedadesAlmacenamiento;
import com.nextdocs.ai.exceptions.ValidacionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
public class AlmacenamientoService {

	private static final Logger log = LoggerFactory.getLogger(AlmacenamientoService.class);

	private final S3Client clienteS3;

	private final S3Presigner firmadorS3;

	private final PropiedadesAlmacenamiento propiedades;

	public AlmacenamientoService(S3Client clienteS3, S3Presigner firmadorS3,
			PropiedadesAlmacenamiento propiedades) {
		this.clienteS3 = clienteS3;
		this.firmadorS3 = firmadorS3;
		this.propiedades = propiedades;
	}

	public String guardarDocumento(String claveObjeto, byte[] contenido, String tipoMime) {
		return guardar(propiedades.getBucketDocumentos(), claveObjeto, contenido, tipoMime);
	}

	public String guardarExportacion(String claveObjeto, byte[] contenido, String tipoMime) {
		return guardar(propiedades.getBucketExportaciones(), claveObjeto, contenido, tipoMime);
	}

	public String guardarEnCuarentena(String claveObjeto, byte[] contenido, String tipoMime) {
		return guardar(propiedades.getBucketCuarentena(), claveObjeto, contenido, tipoMime);
	}

	public byte[] leerDocumento(String claveObjeto) {
		try (ResponseInputStream<GetObjectResponse> flujo = clienteS3.getObject(GetObjectRequest.builder()
				.bucket(propiedades.getBucketDocumentos()).key(claveObjeto).build())) {
			return flujo.readAllBytes();
		} catch (Exception e) {
			throw new ValidacionException("No se pudo leer el objeto " + claveObjeto);
		}
	}

	public InputStream abrirDocumento(String claveObjeto) {
		return new ByteArrayInputStream(leerDocumento(claveObjeto));
	}

	public String urlFirmada(String bucket, String claveObjeto) {
		GetObjectPresignRequest solicitud = GetObjectPresignRequest.builder()
				.signatureDuration(Duration.ofMinutes(propiedades.getDuracionUrlFirmadaMinutos()))
				.getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(claveObjeto).build())
				.build();
		return firmadorS3.presignGetObject(solicitud).url().toString();
	}

	public String urlFirmadaDocumento(String claveObjeto) {
		return urlFirmada(propiedades.getBucketDocumentos(), claveObjeto);
	}

	public void eliminarDocumento(String claveObjeto) {
		try {
			clienteS3.deleteObject(
					DeleteObjectRequest.builder().bucket(propiedades.getBucketDocumentos()).key(claveObjeto).build());
		} catch (S3Exception e) {
			log.error("No se pudo eliminar el objeto {}", claveObjeto, e);
		}
	}

	public void eliminarExportacion(String claveObjeto) {
		try {
			clienteS3.deleteObject(DeleteObjectRequest.builder().bucket(propiedades.getBucketExportaciones())
					.key(claveObjeto).build());
		} catch (S3Exception e) {
			log.error("No se pudo eliminar la exportacion {}", claveObjeto, e);
		}
	}

	public byte[] leerExportacion(String claveObjeto) {
		try (ResponseInputStream<GetObjectResponse> flujo = clienteS3.getObject(GetObjectRequest.builder()
				.bucket(propiedades.getBucketExportaciones()).key(claveObjeto).build())) {
			return flujo.readAllBytes();
		} catch (Exception e) {
			throw new ValidacionException("No se pudo leer la exportacion " + claveObjeto);
		}
	}

	public String bucketDocumentos() {
		return propiedades.getBucketDocumentos();
	}

	public String bucketCuarentena() {
		return propiedades.getBucketCuarentena();
	}

	public String bucketExportaciones() {
		return propiedades.getBucketExportaciones();
	}

	private String guardar(String bucket, String claveObjeto, byte[] contenido, String tipoMime) {
		try {
			PutObjectRequest solicitud = PutObjectRequest.builder().bucket(bucket).key(claveObjeto)
					.contentType(tipoMime).contentLength((long) contenido.length).build();
			clienteS3.putObject(solicitud, RequestBody.fromBytes(contenido));
			return claveObjeto;
		} catch (S3Exception e) {
			log.error("No se pudo guardar el objeto {} en {}", claveObjeto, bucket, e);
			throw new ValidacionException("No se pudo almacenar el archivo");
		}
	}
}
