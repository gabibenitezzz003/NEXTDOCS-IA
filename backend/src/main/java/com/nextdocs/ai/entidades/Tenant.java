package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.EstadoTenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "tenant")
public class Tenant implements Serializable {
	private static final long serialVersionUID = 7247226596976358429L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@Column(length = 64, nullable = false, unique = true)
	private String codigo;

	@Column(length = 256, nullable = false)
	private String nombre;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private EstadoTenant estado;

	@Column(length = 64)
	private String plan;

	@Column(length = 64)
	private String region;

	@Column(length = 256)
	private String dominio;

	private long cuotaAlmacenamientoBytes;

	private Instant alta;

	private Instant baja;
}
