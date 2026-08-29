package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.ResultadoMensajeWhatsapp;

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
@Table(name = "mensaje_whatsapp_entrante",
		uniqueConstraints = @UniqueConstraint(name = "uq_mensaje_whatsapp_entrante",
				columnNames = { "linea_id", "identificador_mensaje" }),
		indexes = { @Index(name = "ix_mensaje_whatsapp_entrante_tenant", columnList = "tenant_id,alta"),
				@Index(name = "ix_mensaje_whatsapp_entrante_origen", columnList = "linea_id,numero_origen,alta") })
public class MensajeWhatsappEntrante implements Serializable {

	private static final long serialVersionUID = 6693284015570223918L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "linea_id")
	private LineaWhatsapp linea;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "correlacion_id")
	private CorrelacionWhatsapp correlacion;

	@Column(name = "identificador_mensaje", length = 128, nullable = false)
	private String identificadorMensaje;

	@Column(name = "numero_origen", length = 32)
	private String numeroOrigen;

	@Column(length = 128)
	private String nombrePerfil;

	@Column(length = 32)
	private String tipo;

	@Column(length = 1024)
	private String texto;

	@Column(length = 32)
	private String tokenDetectado;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private ResultadoMensajeWhatsapp resultado;

	@Column(columnDefinition = "text")
	private String motivo;

	private int media;

	private int ingestados;

	private int rechazados;

	@Column(length = 36)
	private String correlacionTraza;

	private Instant recibidoEn;

	private Instant alta;
}
