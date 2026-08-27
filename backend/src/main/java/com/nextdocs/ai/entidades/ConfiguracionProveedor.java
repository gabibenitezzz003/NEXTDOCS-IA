package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.ProveedorDocumentalIa;

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
@Table(name = "configuracion_proveedor", uniqueConstraints = @UniqueConstraint(columnNames = { "tenant_id", "proveedor" }))
public class ConfiguracionProveedor implements Serializable {
	private static final long serialVersionUID = 1093108896264914125L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private ProveedorDocumentalIa proveedor;

	@Column(length = 128)
	private String modelo;

	@Column(length = 256)
	private String referenciaSecreto;

	@Column(length = 64)
	private String region;

	private int limitePeticionesPorMinuto;

	private int limitePaginasPorDocumento;

	private int prioridad;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private String parametros;

	private boolean activa;

	private boolean respaldo;

	private Instant alta;

	private Instant baja;
}
