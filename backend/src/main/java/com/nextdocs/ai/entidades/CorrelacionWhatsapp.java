package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
@Table(name = "correlacion_whatsapp",
		indexes = { @Index(name = "ix_correlacion_whatsapp_vinculo", columnList = "linea_id,numero_vinculado") })
public class CorrelacionWhatsapp implements Serializable {

	private static final long serialVersionUID = 8840261973055018842L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "linea_id")
	private LineaWhatsapp linea;

	@Column(length = 32, nullable = false, unique = true)
	private String token;

	@Embedded
	@AttributeOverrides({ @AttributeOverride(name = "origen", column = @Column(name = "sujeto_origen")),
			@AttributeOverride(name = "tipoObjeto", column = @Column(name = "sujeto_tipo_objeto")),
			@AttributeOverride(name = "idObjeto", column = @Column(name = "sujeto_id_objeto")),
			@AttributeOverride(name = "tenantOrigen", column = @Column(name = "sujeto_tenant_origen")) })
	private ReferenciaExterna sujeto;

	@Column(length = 64)
	private String codigoPlantilla;

	@Column(name = "numero_destino", length = 32)
	private String numeroDestino;

	@Column(name = "numero_vinculado", length = 32)
	private String numeroVinculado;

	private Instant vinculadoEn;

	@Column(length = 256)
	private String descripcion;

	private Instant venceEn;

	private int documentosRecibidos;

	private Instant ultimoUso;

	private Instant alta;

	private Instant baja;
}
