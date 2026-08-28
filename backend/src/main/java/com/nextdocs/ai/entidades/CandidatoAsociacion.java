package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
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
@Table(name = "candidato_asociacion", indexes = @Index(name = "ix_candidato_asociacion_documento", columnList = "documento_id"))
public class CandidatoAsociacion implements Serializable {
	private static final long serialVersionUID = 3145558284758184117L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@ManyToOne(fetch = FetchType.LAZY)
	private Documento documento;

	@Column(length = 64, nullable = false)
	private String conector;

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "origen", column = @Column(name = "objeto_origen", length = 64)),
			@AttributeOverride(name = "tipoObjeto", column = @Column(name = "objeto_tipo", length = 64)),
			@AttributeOverride(name = "idObjeto", column = @Column(name = "objeto_id", length = 128)),
			@AttributeOverride(name = "tenantOrigen", column = @Column(name = "objeto_tenant_origen", length = 128)) })
	private ReferenciaExterna referencia;

	@Column(precision = 5, scale = 4)
	private BigDecimal puntaje;

	@Column(columnDefinition = "text")
	private String razones;

	@Column(length = 256)
	private String descripcion;

	private boolean seleccionado;

	private boolean descartado;

	@Column(length = 256)
	private String motivoSeleccion;

	@ManyToOne(fetch = FetchType.LAZY)
	private Usuario seleccionadoPor;

	private Instant alta;
}
