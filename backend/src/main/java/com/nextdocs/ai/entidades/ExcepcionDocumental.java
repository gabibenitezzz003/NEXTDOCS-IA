package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.EstadoExcepcion;
import com.nextdocs.ai.enumeraciones.PrioridadExcepcion;
import com.nextdocs.ai.enumeraciones.SeveridadHallazgo;
import com.nextdocs.ai.enumeraciones.TipoExcepcion;

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
@Table(name = "excepcion_documental", indexes = {
		@Index(name = "ix_excepcion_tenant_estado", columnList = "tenant_id,estado"),
		@Index(name = "ix_excepcion_documento", columnList = "documento_id") })
public class ExcepcionDocumental implements Serializable {
	private static final long serialVersionUID = 4466344541184686022L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@ManyToOne(fetch = FetchType.LAZY)
	private Documento documento;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private TipoExcepcion tipo;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private SeveridadHallazgo severidad;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private PrioridadExcepcion prioridad;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private EstadoExcepcion estado;

	@Column(length = 64)
	private String codigo;

	@Column(length = 1024)
	private String detalle;

	@ManyToOne(fetch = FetchType.LAZY)
	private Usuario responsable;

	@ManyToOne(fetch = FetchType.LAZY)
	private Usuario resueltaPor;

	@Column(length = 1024)
	private String resolucion;

	private Instant venceEn;

	private Instant resuelta;

	@Column(length = 128, unique = true)
	private String claveDeduplicacion;

	@Column(length = 64)
	private String correlacionId;

	private Instant alta;

	private Instant baja;
}
