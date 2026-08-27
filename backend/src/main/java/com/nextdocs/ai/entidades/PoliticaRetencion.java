package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.AccionRetencion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Data;

@Data
@Entity
@Table(name = "politica_retencion", uniqueConstraints = @UniqueConstraint(columnNames = { "tenant_id", "clase" }))
public class PoliticaRetencion implements Serializable {
	private static final long serialVersionUID = 2327314677627760916L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@Column(length = 64, nullable = false)
	private String clase;

	@Column(length = 256)
	private String descripcion;

	private int duracionDias;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private AccionRetencion accion;

	private boolean permiteRetencionLegal;

	private boolean activa;

	private Instant alta;

	private Instant baja;
}
