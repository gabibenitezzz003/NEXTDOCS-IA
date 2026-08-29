package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "remitente_autorizado")
public class RemitenteAutorizado implements Serializable {

	private static final long serialVersionUID = 8823017455112097630L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "buzon_id")
	private BuzonCorreo buzon;

	@Column(length = 256, nullable = false)
	private String patron;

	@Column(length = 256)
	private String descripcion;

	private Instant alta;

	private Instant baja;
}
