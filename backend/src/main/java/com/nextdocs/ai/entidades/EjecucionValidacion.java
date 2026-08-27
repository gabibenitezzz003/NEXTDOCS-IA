package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.EstadoEjecucion;
import com.nextdocs.ai.enumeraciones.ResultadoValidacion;

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
@Table(name = "ejecucion_validacion", indexes = @Index(name = "ix_ejecucion_validacion_documento", columnList = "documento_id"))
public class EjecucionValidacion implements Serializable {
	private static final long serialVersionUID = 3776452253166086897L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@ManyToOne(fetch = FetchType.LAZY)
	private Documento documento;

	@ManyToOne(fetch = FetchType.LAZY)
	private VersionPlantilla versionPlantilla;

	@ManyToOne(fetch = FetchType.LAZY)
	private EjecucionExtraccion ejecucionExtraccion;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private EstadoEjecucion estado;

	@Enumerated(EnumType.STRING)
	@Column(length = 32)
	private ResultadoValidacion resultado;

	private int cantidadHallazgos;

	private int cantidadBloqueantes;

	private boolean autoaprobado;

	@Column(length = 512)
	private String motivoResultado;

	@Column(length = 64)
	private String correlacionId;

	private Instant inicio;

	private Instant fin;

	private Instant alta;
}
