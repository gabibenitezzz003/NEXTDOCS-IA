package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.EstadoEventoSalida;
import com.nextdocs.ai.enumeraciones.TipoEventoCanonico;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.Data;

@Data
@Entity
@Table(name = "evento_salida", indexes = @Index(name = "ix_evento_salida_estado_disponible", columnList = "estado,disponible_en"))
public class EventoSalida implements Serializable {
	private static final long serialVersionUID = 529951309580248164L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@Column(name = "tenant_id", length = 36)
	private String tenantId;

	@Enumerated(EnumType.STRING)
	@Column(length = 64, nullable = false)
	private TipoEventoCanonico tipoEvento;

	@Column(length = 64)
	private String tipoAgregado;

	@Column(length = 36)
	private String idAgregado;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb", nullable = false)
	private String carga;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private EstadoEventoSalida estado;

	private int intento;

	@Column(columnDefinition = "text")
	private String ultimoError;

	@Column(length = 64)
	private String correlacionId;

	private Instant disponibleEn;

	private Instant procesado;

	private Instant alta;
}
