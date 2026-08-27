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
import jakarta.persistence.UniqueConstraint;

import lombok.Data;

@Data
@Entity
@Table(name = "plantilla_documental", uniqueConstraints = @UniqueConstraint(columnNames = { "tenant_id", "codigo" }))
public class PlantillaDocumental implements Serializable {
	private static final long serialVersionUID = 7784094156344027996L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@Column(length = 64, nullable = false)
	private String codigo;

	@Column(length = 128, nullable = false)
	private String nombre;

	@Column(length = 64)
	private String familia;

	@Column(length = 1024)
	private String descripcion;

	@ManyToOne(fetch = FetchType.LAZY)
	private VersionPlantilla versionPublicada;

	@ManyToOne(fetch = FetchType.LAZY)
	private Usuario creadoPor;

	private Instant alta;

	private Instant baja;
}
