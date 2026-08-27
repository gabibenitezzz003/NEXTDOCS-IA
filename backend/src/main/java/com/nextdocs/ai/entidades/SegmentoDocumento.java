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
@Table(name = "segmento_documento")
public class SegmentoDocumento implements Serializable {
	private static final long serialVersionUID = 8834496867036870343L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@ManyToOne(fetch = FetchType.LAZY)
	private Documento documentoPadre;

	@ManyToOne(fetch = FetchType.LAZY)
	private Documento documentoHijo;

	private int paginaDesde;

	private int paginaHasta;

	private int orden;

	@Column(length = 256)
	private String motivoCorte;

	private Instant alta;
}
