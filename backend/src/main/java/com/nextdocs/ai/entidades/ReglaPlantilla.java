package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.SeveridadHallazgo;
import com.nextdocs.ai.enumeraciones.TipoReglaValidacion;

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

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.Data;

@Data
@Entity
@Table(name = "regla_plantilla", uniqueConstraints = @UniqueConstraint(columnNames = { "version_plantilla_id", "codigo" }))
public class ReglaPlantilla implements Serializable {
	private static final long serialVersionUID = 7057085588611361108L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private VersionPlantilla versionPlantilla;

	@Column(length = 64, nullable = false)
	private String codigo;

	@Column(length = 256, nullable = false)
	private String nombre;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private TipoReglaValidacion tipo;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private SeveridadHallazgo severidad;

	@Column(length = 128)
	private String campoObjetivo;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private String configuracion;

	@Column(length = 512)
	private String mensaje;

	private boolean activa;

	private int orden;

	private Instant alta;

	private Instant baja;
}
