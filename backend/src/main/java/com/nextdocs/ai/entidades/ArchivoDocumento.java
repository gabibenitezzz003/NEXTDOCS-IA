package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.ResultadoEscaneo;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "archivo_documento")
public class ArchivoDocumento implements Serializable {
	private static final long serialVersionUID = 1486076916440456705L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@ManyToOne(fetch = FetchType.LAZY)
	private Documento documento;

	@Column(length = 512, nullable = false)
	private String claveObjeto;

	@Column(length = 64, nullable = false)
	private String bucket;

	@Column(length = 256)
	private String nombreArchivo;

	@Column(length = 128)
	private String tipoMime;

	@Column(length = 16)
	private String extension;

	private long tamano;

	@Column(length = 128)
	private String checksum;

	private int paginas;

	private int version;

	private boolean original;

	@Column(length = 128)
	private String algoritmoCifrado;

	@Enumerated(EnumType.STRING)
	@Column(length = 32)
	private ResultadoEscaneo resultadoEscaneo;

	@Column(length = 256)
	private String amenazaDetectada;

	@Column(length = 64)
	private String motorEscaneo;

	private Instant escaneado;

	private boolean enCuarentena;

	private Instant alta;

	private Instant baja;
}
