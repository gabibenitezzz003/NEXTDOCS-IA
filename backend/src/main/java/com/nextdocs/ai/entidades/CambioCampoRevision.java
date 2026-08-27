package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import jakarta.persistence.Column;
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
@Table(name = "cambio_campo_revision")
public class CambioCampoRevision implements Serializable {
	private static final long serialVersionUID = 6188927423287662605L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@ManyToOne(fetch = FetchType.LAZY)
	private RevisionDocumento revision;

	@Column(length = 128, nullable = false)
	private String claveCampo;

	@Column(columnDefinition = "text")
	private String valorAnterior;

	@Column(columnDefinition = "text")
	private String valorNuevo;

	@Column(length = 512)
	private String motivo;

	private Instant alta;
}
