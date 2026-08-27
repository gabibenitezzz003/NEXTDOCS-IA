package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.EstadoOriginalFisico;

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

import lombok.Data;

@Data
@Entity
@Table(name = "seguimiento_original_fisico")
public class SeguimientoOriginalFisico implements Serializable {
	private static final long serialVersionUID = 4829125302254906233L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@ManyToOne(fetch = FetchType.LAZY)
	private Documento documento;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private EstadoOriginalFisico estado;

	@Column(length = 256)
	private String ubicacion;

	@Column(length = 128)
	private String referenciaFisica;

	@ManyToOne(fetch = FetchType.LAZY)
	private Usuario recibidoPor;

	@Column(length = 512)
	private String observacion;

	private Instant recibido;

	private Instant archivado;

	private Instant alta;

	private Instant baja;
}
