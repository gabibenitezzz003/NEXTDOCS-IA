package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.DecisionRevision;
import com.nextdocs.ai.enumeraciones.EstadoDocumento;

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

import lombok.Data;

@Data
@Entity
@Table(name = "revision_documento", indexes = @Index(name = "ix_revision_documento_documento", columnList = "documento_id"))
public class RevisionDocumento implements Serializable {
	private static final long serialVersionUID = 2124247071634157146L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@ManyToOne(fetch = FetchType.LAZY)
	private Documento documento;

	@ManyToOne(fetch = FetchType.LAZY)
	private Usuario actor;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private DecisionRevision decision;

	@Enumerated(EnumType.STRING)
	@Column(length = 32)
	private EstadoDocumento estadoAnterior;

	@Enumerated(EnumType.STRING)
	@Column(length = 32)
	private EstadoDocumento estadoNuevo;

	@Column(length = 1024)
	private String motivo;

	private int cantidadCorrecciones;

	private long duracionRevisionMilisegundos;

	@Column(length = 64)
	private String correlacionId;

	private Instant alta;
}
