package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "codigo_embed", indexes = { @Index(name = "ix_codigo_embed_tenant", columnList = "tenant_id,alta") })
public class CodigoEmbed implements Serializable {

	private static final long serialVersionUID = 8830174552096631204L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@ManyToOne(fetch = FetchType.LAZY)
	private Usuario usuario;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "proveedor_id")
	private ProveedorIdentidad proveedor;

	@Column(length = 64, nullable = false, unique = true)
	private String codigoHash;

	@Column(length = 128)
	private String sujetoExterno;

	@Column(length = 64)
	private String aplicacionOrigen;

	@Column(length = 64)
	private String tipoObjeto;

	@Column(length = 128)
	private String idObjeto;

	@Column(length = 512)
	private String urlRetorno;

	private Instant venceEn;

	private Instant usadoEn;

	@Column(length = 64)
	private String correlacionId;

	private Instant alta;
}
