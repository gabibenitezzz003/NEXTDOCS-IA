package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.EstadoPlantilla;
import com.nextdocs.ai.enumeraciones.PoliticaOriginalFisico;

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
import jakarta.persistence.Version;
import jakarta.persistence.UniqueConstraint;

import lombok.Data;

@Data
@Entity
@Table(name = "version_plantilla", uniqueConstraints = @UniqueConstraint(columnNames = { "plantilla_id", "numero" }))
public class VersionPlantilla implements Serializable {
	private static final long serialVersionUID = 4345634464964051498L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@ManyToOne(fetch = FetchType.LAZY)
	private PlantillaDocumental plantilla;

	private int numero;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private EstadoPlantilla estado;

	@Column(precision = 5, scale = 4)
	private BigDecimal umbralAutoaprobacion;

	@Enumerated(EnumType.STRING)
	@Column(length = 32)
	private PoliticaOriginalFisico politicaOriginalFisico;

	@Column(length = 64)
	private String versionPrompt;

	@Column(length = 64)
	private String versionEsquema;

	@Column(columnDefinition = "text")
	private String instruccionExtraccion;

	@Column(length = 1024)
	private String notasCambio;

	@ManyToOne(fetch = FetchType.LAZY)
	private Usuario publicadaPor;

	private Instant publicada;

	@Version
	private long bloqueoOptimista;

	private Instant alta;

	private Instant baja;
}
