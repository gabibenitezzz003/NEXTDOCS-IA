package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.ResultadoQualityGate;

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

import lombok.Data;

@Data
@Entity
@Table(name = "ejecucion_prueba_plantilla")
public class EjecucionPruebaPlantilla implements Serializable {

	private static final long serialVersionUID = 3182046619027741538L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@ManyToOne(fetch = FetchType.LAZY)
	private VersionPlantilla versionPlantilla;

	@ManyToOne(fetch = FetchType.LAZY)
	private ConjuntoPruebaPlantilla conjunto;

	@Column(length = 64, nullable = false)
	private String huellaConjunto;

	@Column(precision = 7, scale = 4, nullable = false)
	private BigDecimal exactitud;

	@Column(precision = 7, scale = 4)
	private BigDecimal exactitudReferencia;

	@Column(precision = 5, scale = 4)
	private BigDecimal factorCalibracion;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private ResultadoQualityGate resultado;

	@Column(length = 1024)
	private String motivo;

	private Instant alta;
}
