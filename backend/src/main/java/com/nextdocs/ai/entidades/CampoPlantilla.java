package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.SensibilidadCampo;
import com.nextdocs.ai.enumeraciones.TipoDatoCampo;

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
@Table(name = "campo_plantilla", uniqueConstraints = @UniqueConstraint(columnNames = { "version_plantilla_id", "clave" }))
public class CampoPlantilla implements Serializable {
	private static final long serialVersionUID = 7978134087992239750L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private VersionPlantilla versionPlantilla;

	@Column(length = 128, nullable = false)
	private String clave;

	@Column(length = 256, nullable = false)
	private String etiqueta;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private TipoDatoCampo tipoDato;

	@Column(length = 1024)
	private String alias;

	@Column(length = 1024)
	private String descripcion;

	private boolean requerido;

	private boolean extraer;

	private boolean validar;

	private boolean comparar;

	private boolean unico;

	@Column(length = 512)
	private String expresionRegular;

	@Column(length = 256)
	private String formatoFecha;

	@Column(length = 128)
	private String catalogoReferencia;

	@Column(precision = 5, scale = 4)
	private BigDecimal umbralConfianza;

	@Enumerated(EnumType.STRING)
	@Column(length = 32)
	private SensibilidadCampo sensibilidad;

	private int orden;

	private Instant alta;

	private Instant baja;
}
