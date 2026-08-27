package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.EstadoEjecucion;
import com.nextdocs.ai.enumeraciones.ProveedorDocumentalIa;

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
@Table(name = "ejecucion_extraccion", indexes = @Index(name = "ix_ejecucion_extraccion_documento", columnList = "documento_id"))
public class EjecucionExtraccion implements Serializable {
	private static final long serialVersionUID = 6715770323798181182L;

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

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private ProveedorDocumentalIa proveedor;

	@Column(length = 128)
	private String modelo;

	@Column(length = 64)
	private String versionPrompt;

	@Column(length = 64)
	private String versionEsquema;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private EstadoEjecucion estado;

	private int intento;

	private long tokensEntrada;

	private long tokensSalida;

	private int paginasProcesadas;

	@Column(precision = 12, scale = 6)
	private BigDecimal costo;

	@Column(length = 8)
	private String monedaCosto;

	private long duracionMilisegundos;

	@Column(length = 64)
	private String codigoError;

	@Column(columnDefinition = "text")
	private String mensajeError;

	@Column(length = 64)
	private String correlacionId;

	private Instant inicio;

	private Instant fin;

	private Instant alta;
}
