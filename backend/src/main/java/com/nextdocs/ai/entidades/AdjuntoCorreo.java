package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.ResultadoAdjuntoCorreo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "adjunto_correo",
		indexes = { @Index(name = "ix_adjunto_correo_mensaje", columnList = "mensaje_id,alta") })
public class AdjuntoCorreo implements Serializable {

	private static final long serialVersionUID = 7712045583962207114L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mensaje_id")
	private MensajeCorreoEntrante mensaje;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "documento_id")
	private Documento documento;

	@Column(length = 400)
	private String nombreArchivo;

	@Column(length = 128)
	private String tipoMime;

	private long tamanoBytes;

	@Column(length = 64)
	private String sha256;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private ResultadoAdjuntoCorreo resultado;

	@Column(length = 64)
	private String codigoRechazo;

	@Column(length = 400)
	private String motivo;

	private Instant alta;
}
