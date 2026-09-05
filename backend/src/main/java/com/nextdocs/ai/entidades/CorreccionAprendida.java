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
import jakarta.persistence.UniqueConstraint;

import lombok.Data;

@Data
@Entity
@Table(name = "correccion_aprendida",
		uniqueConstraints = @UniqueConstraint(name = "uq_correccion_aprendida",
				columnNames = { "tenant_id", "plantilla_codigo", "emisor_clave", "clave_campo", "valor_leido" }),
		indexes = { @Index(name = "ix_correccion_aprendida_busqueda",
				columnList = "tenant_id,plantilla_codigo,emisor_clave") })
public class CorreccionAprendida implements Serializable {

	private static final long serialVersionUID = 3320874905611233078L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@Column(name = "plantilla_codigo", length = 64, nullable = false)
	private String plantillaCodigo;

	@Column(name = "emisor_clave", length = 128, nullable = false)
	private String emisorClave;

	@Column(name = "clave_campo", length = 64, nullable = false)
	private String claveCampo;

	@Column(name = "valor_leido", length = 256, nullable = false)
	private String valorLeido;

	@Column(name = "valor_corregido", length = 256)
	private String valorCorregido;

	private int veces;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ultimo_documento_id")
	private Documento ultimoDocumento;

	private Instant alta;

	private Instant actualizado;
}
