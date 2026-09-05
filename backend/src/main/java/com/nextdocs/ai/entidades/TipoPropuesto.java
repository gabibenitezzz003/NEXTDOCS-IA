package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.EstadoTipoPropuesto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Data;

@Data
@Entity
@Table(name = "tipo_propuesto",
		uniqueConstraints = @UniqueConstraint(name = "uq_tipo_propuesto",
				columnNames = { "tenant_id", "codigo_sugerido" }),
		indexes = { @Index(name = "ix_tipo_propuesto_tenant", columnList = "tenant_id,estado") })
public class TipoPropuesto implements Serializable {

	private static final long serialVersionUID = 5540118376624099135L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@Column(name = "codigo_sugerido", length = 64, nullable = false)
	private String codigoSugerido;

	@Column(length = 128)
	private String nombreSugerido;

	@Column(length = 400)
	private String motivo;

	@Column(columnDefinition = "text")
	private String camposSugeridos;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ultimo_documento_id")
	private Documento ultimoDocumento;

	private int veces;

	@Enumerated(EnumType.STRING)
	@Column(length = 16, nullable = false)
	private EstadoTipoPropuesto estado;

	@Column(length = 64)
	private String codigoAprobado;

	private Instant alta;

	private Instant resuelto;
}
