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
import jakarta.persistence.UniqueConstraint;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@Table(name = "rol", uniqueConstraints = @UniqueConstraint(columnNames = { "tenant_id", "codigo" }))
public class Rol implements Serializable {
	private static final long serialVersionUID = 4611464996845024390L;

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
	private String descripcion;

	private boolean predefinido;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "rol_permiso", joinColumns = @JoinColumn(name = "rol_id"))
	@Column(name = "permiso", length = 128)
	@EqualsAndHashCode.Exclude
	@ToString.Exclude
	private Set<String> permisos = new LinkedHashSet<>();

	private Instant alta;

	private Instant baja;
}
