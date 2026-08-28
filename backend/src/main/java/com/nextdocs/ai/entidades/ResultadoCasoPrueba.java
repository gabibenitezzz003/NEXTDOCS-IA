package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.math.BigDecimal;

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
@Table(name = "resultado_caso_prueba")
public class ResultadoCasoPrueba implements Serializable {

	private static final long serialVersionUID = 5519027743361208842L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private EjecucionPruebaPlantilla ejecucion;

	@ManyToOne(fetch = FetchType.LAZY)
	private CasoPruebaPlantilla caso;

	private int aciertos;

	private int total;

	@Column(precision = 7, scale = 4, nullable = false)
	private BigDecimal exactitud;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private String extraido;
}
