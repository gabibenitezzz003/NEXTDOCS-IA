package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.Data;

@Data
@Entity
@Table(name = "caso_prueba_plantilla")
public class CasoPruebaPlantilla implements Serializable {

	private static final long serialVersionUID = 2294417736512083361L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@ManyToOne(fetch = FetchType.LAZY)
	private ConjuntoPruebaPlantilla conjunto;

	@Column(length = 128, nullable = false)
	private String nombre;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb", nullable = false)
	private String esperado;

	@Column(length = 512, nullable = false)
	private String claveObjeto;

	@Column(length = 256, nullable = false)
	private String nombreArchivo;

	@Column(length = 128, nullable = false)
	private String tipoMime;

	private int orden;

	private Instant alta;

	private Instant baja;
}
