package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.OrigenIdentidad;

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

import lombok.Data;

@Data
@Entity
@Table(name = "proveedor_identidad",
		uniqueConstraints = @UniqueConstraint(name = "uq_proveedor_identidad", columnNames = { "tenant_id", "codigo" }))
public class ProveedorIdentidad implements Serializable {

	private static final long serialVersionUID = 5590328174460293117L;

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

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private OrigenIdentidad origen;

	@Column(length = 512, nullable = false)
	private String emisor;

	@Column(length = 512, nullable = false)
	private String urlJwks;

	@Column(length = 256)
	private String audiencia;

	@Column(length = 64, nullable = false)
	private String claimSujeto;

	@Column(length = 64, nullable = false)
	private String claimEmail;

	@Column(length = 64, nullable = false)
	private String claimNombre;

	private boolean permitirJit;

	private boolean permitirVinculoPorEmail;

	@Column(length = 64)
	private String codigoRolPorDefecto;

	@Column(columnDefinition = "text")
	private String dominiosPermitidos;

	@Column(columnDefinition = "text")
	private String origenesEmbedPermitidos;

	private int segundosVigenciaCodigo;

	private boolean activo;

	private Instant alta;

	private Instant baja;
}
