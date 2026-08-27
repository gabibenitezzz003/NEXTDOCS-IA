package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.SeveridadHallazgo;

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
@Table(name = "hallazgo_validacion", indexes = @Index(name = "ix_hallazgo_validacion_ejecucion", columnList = "ejecucion_id"))
public class HallazgoValidacion implements Serializable {
	private static final long serialVersionUID = 8355998775438912331L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@ManyToOne(fetch = FetchType.LAZY)
	private EjecucionValidacion ejecucion;

	@ManyToOne(fetch = FetchType.LAZY)
	private ReglaPlantilla regla;

	@Column(length = 64)
	private String codigoRegla;

	@Column(length = 128)
	private String claveCampo;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private SeveridadHallazgo severidad;

	@Column(length = 1024)
	private String mensaje;

	@Column(columnDefinition = "text")
	private String evidencia;

	private boolean sobreescrito;

	@Column(length = 512)
	private String motivoSobreescritura;

	@ManyToOne(fetch = FetchType.LAZY)
	private Usuario sobreescritoPor;

	private Instant alta;
}
