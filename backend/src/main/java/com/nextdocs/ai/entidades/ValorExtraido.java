package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.PresenciaCampo;

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
@Table(name = "valor_extraido", indexes = {
		@Index(name = "ix_valor_extraido_ejecucion", columnList = "ejecucion_id"),
		@Index(name = "ix_valor_extraido_documento_clave", columnList = "documento_id,clave_campo") })
public class ValorExtraido implements Serializable {
	private static final long serialVersionUID = 4948033671802138786L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@ManyToOne(fetch = FetchType.LAZY)
	private Documento documento;

	@ManyToOne(fetch = FetchType.LAZY)
	private EjecucionExtraccion ejecucion;

	@Column(length = 128, nullable = false)
	private String claveCampo;

	@Column(columnDefinition = "text")
	private String valorCrudo;

	@Column(columnDefinition = "text")
	private String valorNormalizado;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private PresenciaCampo presencia;

	@Column(precision = 5, scale = 4)
	private BigDecimal confianza;

	@Column(precision = 5, scale = 4)
	private BigDecimal confianzaProveedor;

	private int evidenciaPagina;

	@Column(length = 128)
	private String evidenciaRecuadro;

	private boolean corregidoManualmente;

	@Column(columnDefinition = "text")
	private String valorAnterior;

	private boolean anonimizado;

	private Instant alta;
}
