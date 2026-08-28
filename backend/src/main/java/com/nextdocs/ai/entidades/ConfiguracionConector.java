package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.TipoAutenticacionConector;

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
@Table(name = "configuracion_conector", uniqueConstraints = @UniqueConstraint(columnNames = { "tenant_id", "codigo" }))
public class ConfiguracionConector implements Serializable {
	private static final long serialVersionUID = 3387105562209744318L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@Column(length = 64, nullable = false)
	private String codigo;

	@Column(length = 128, nullable = false)
	private String nombre;

	@Column(length = 512)
	private String urlBase;

	@Column(length = 256)
	private String referenciaSecreto;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private TipoAutenticacionConector tipoAutenticacion;

	@Column(length = 64)
	private String nombreCabeceraClave;

	private int tiempoEsperaMilisegundos;

	private int intentosMaximos;

	@Column(precision = 5, scale = 4)
	private BigDecimal umbralSeleccionAutomatica;

	@Column(precision = 5, scale = 4)
	private BigDecimal umbralCandidatoMinimo;

	private int umbralCircuitoAbierto;

	private int duracionCircuitoAbiertoSegundos;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private String parametros;

	private boolean activo;

	private Instant alta;

	private Instant baja;
}
