package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.AccionPresupuestoCosto;

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
@Table(name = "politica_costo_tenant", uniqueConstraints = @UniqueConstraint(columnNames = "tenant_id"))
public class PoliticaCostoTenant implements Serializable {

	private static final long serialVersionUID = 4188229045512093317L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	private boolean activo;

	@Column(precision = 14, scale = 6)
	private BigDecimal presupuestoMensual;

	@Column(precision = 5, scale = 4, nullable = false)
	private BigDecimal umbralAlerta;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private AccionPresupuestoCosto accionAlExceder;

	@Column(length = 8, nullable = false)
	private String moneda;

	private Instant alta;
}
