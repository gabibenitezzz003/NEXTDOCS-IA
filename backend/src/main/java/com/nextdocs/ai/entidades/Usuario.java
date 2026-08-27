package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import com.nextdocs.ai.enumeraciones.EstadoUsuario;
import com.nextdocs.ai.enumeraciones.OrigenIdentidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@Table(name = "usuario", uniqueConstraints = @UniqueConstraint(columnNames = { "tenant_id", "email" }))
public class Usuario implements Serializable {
	private static final long serialVersionUID = 4911878975303306644L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@Column(length = 256, nullable = false)
	private String email;

	@Column(length = 256, nullable = false)
	private String nombre;

	@Column(length = 256)
	private String claveHash;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private EstadoUsuario estado;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private OrigenIdentidad origenIdentidad;

	@Column(length = 128)
	private String idUsuarioExterno;

	@Column(length = 16)
	private String idioma;

	@Column(length = 64)
	private String zonaHoraria;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "usuario_rol", joinColumns = @JoinColumn(name = "usuario_id"), inverseJoinColumns = @JoinColumn(name = "rol_id"))
	@EqualsAndHashCode.Exclude
	@ToString.Exclude
	private Set<Rol> roles = new LinkedHashSet<>();

	private Instant ultimoAcceso;

	private Instant alta;

	private Instant baja;
}
