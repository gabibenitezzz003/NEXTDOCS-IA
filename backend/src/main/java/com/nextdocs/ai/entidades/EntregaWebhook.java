package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.EstadoEntregaWebhook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Data;

@Data
@Entity
@Table(name = "entrega_webhook", uniqueConstraints = @UniqueConstraint(columnNames = { "suscripcion_id", "evento_id" }), indexes = @Index(name = "ix_entrega_webhook_estado", columnList = "estado,disponible_en"))
public class EntregaWebhook implements Serializable {
	private static final long serialVersionUID = 2790295331650125259L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@ManyToOne(fetch = FetchType.LAZY)
	private SuscripcionWebhook suscripcion;

	@Column(name = "evento_id", length = 36, nullable = false)
	private String eventoId;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private EstadoEntregaWebhook estado;

	private int intento;

	private int codigoRespuesta;

	@Column(columnDefinition = "text")
	private String cuerpoRespuesta;

	private long duracionMilisegundos;

	private Instant disponibleEn;

	private Instant entregado;

	private Instant alta;
}
