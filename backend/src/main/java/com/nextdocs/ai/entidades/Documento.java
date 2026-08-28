package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.AccionRetencion;
import com.nextdocs.ai.enumeraciones.EstadoDocumento;
import com.nextdocs.ai.enumeraciones.OrigenDocumento;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Data;

@Data
@Entity
@Table(name = "documento", uniqueConstraints = @UniqueConstraint(columnNames = { "tenant_id", "clave_idempotencia" }), indexes = {
		@Index(name = "ix_documento_tenant_estado", columnList = "tenant_id,estado"),
		@Index(name = "ix_documento_tenant_alta", columnList = "tenant_id,alta"),
		@Index(name = "ix_documento_hash", columnList = "tenant_id,hash_contenido") })
public class Documento implements Serializable {
	private static final long serialVersionUID = 3482790226316438742L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private EstadoDocumento estado;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private OrigenDocumento origen;

	@Column(length = 256)
	private String nombre;

	@Column(length = 128)
	private String hashContenido;

	@Column(length = 128)
	private String claveIdempotencia;

	@Column(length = 64)
	private String correlacionId;

	@ManyToOne(fetch = FetchType.LAZY)
	private PlantillaDocumental plantilla;

	@ManyToOne(fetch = FetchType.LAZY)
	private VersionPlantilla versionPlantilla;

	@ManyToOne(fetch = FetchType.LAZY)
	private Documento documentoPadre;

	private int paginaDesde;

	private int paginaHasta;

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "origen", column = @Column(name = "sujeto_origen", length = 64)),
			@AttributeOverride(name = "tipoObjeto", column = @Column(name = "sujeto_tipo_objeto", length = 64)),
			@AttributeOverride(name = "idObjeto", column = @Column(name = "sujeto_id_objeto", length = 128)),
			@AttributeOverride(name = "tenantOrigen", column = @Column(name = "sujeto_tenant_origen", length = 128)) })
	private ReferenciaExterna referenciaSujeto;

	@Column(length = 256)
	private String remitente;

	@Column(length = 1024)
	private String observacion;

	private int cantidadSegmentos;

	private int reintentosExtraccion;

	@ManyToOne(fetch = FetchType.LAZY)
	private Usuario ingresadoPor;

	@ManyToOne(fetch = FetchType.LAZY)
	private CuentaServicio ingresadoPorCuentaServicio;

	private Instant recibido;

	private Instant procesado;

	private Instant cerrado;

	private Instant retenerHasta;

	private boolean retencionLegal;

	private Instant retencionAplicada;

	@Enumerated(EnumType.STRING)
	@Column(length = 32)
	private AccionRetencion accionRetencionAplicada;

	private Instant alta;

	private Instant baja;
}
