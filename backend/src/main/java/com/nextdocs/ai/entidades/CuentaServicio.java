package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@Table(name = "cuenta_servicio")
public class CuentaServicio implements Serializable {
	private static final long serialVersionUID = 6200300371565133727L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@Column(length = 128, nullable = false)
	private String nombre;

	@Column(length = 32, nullable = false)
	private String prefijoClave;

	@Column(length = 128, nullable = false, unique = true)
	private String claveHash;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "cuenta_servicio_alcance", joinColumns = @JoinColumn(name = "cuenta_servicio_id"))
	@Column(name = "alcance", length = 128)
	@EqualsAndHashCode.Exclude
	@ToString.Exclude
	private Set<String> alcances = new LinkedHashSet<>();

	private boolean activa;

	private Instant expira;

	private Instant ultimoUso;

	@Column(length = 512)
	private String motivoRevocacion;

	private Instant alta;

	private Instant baja;
}
