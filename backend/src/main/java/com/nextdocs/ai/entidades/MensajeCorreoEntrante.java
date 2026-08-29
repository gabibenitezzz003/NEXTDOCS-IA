package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.ResultadoMensajeCorreo;

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
import jakarta.persistence.UniqueConstraint;

import lombok.Data;

@Data
@Entity
@Table(name = "mensaje_correo_entrante",
		uniqueConstraints = @UniqueConstraint(name = "uq_mensaje_correo_entrante",
				columnNames = { "buzon_id", "identificador_mensaje" }),
		indexes = { @Index(name = "ix_mensaje_correo_entrante_tenant", columnList = "tenant_id,alta") })
public class MensajeCorreoEntrante implements Serializable {

	private static final long serialVersionUID = 4460783211956028345L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "buzon_id")
	private BuzonCorreo buzon;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "correlacion_id")
	private CorrelacionCorreo correlacion;

	@Column(name = "identificador_mensaje", length = 400, nullable = false)
	private String identificadorMensaje;

	@Column(length = 256)
	private String remitente;

	@Column(length = 400)
	private String destinatarios;

	@Column(length = 400)
	private String asunto;

	@Column(length = 32)
	private String tokenDetectado;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private ResultadoMensajeCorreo resultado;

	@Column(columnDefinition = "text")
	private String motivo;

	private int adjuntos;

	private int ingestados;

	private int rechazados;

	@Column(length = 36)
	private String correlacionTraza;

	private Instant enviadoEn;

	private Instant alta;
}
