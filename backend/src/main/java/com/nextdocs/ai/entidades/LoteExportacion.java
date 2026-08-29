package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.EstadoLoteExportacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "lote_exportacion", indexes = {
		@Index(name = "ix_lote_exportacion_tenant", columnList = "tenant_id,alta"),
		@Index(name = "ix_lote_exportacion_estado", columnList = "estado,alta"),
		@Index(name = "ix_lote_exportacion_vencimiento", columnList = "estado,vence_en") })
public class LoteExportacion implements Serializable {

	private static final long serialVersionUID = 4471209835562780114L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "solicitado_por_id")
	private Usuario solicitadoPor;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private EstadoLoteExportacion estado;

	@Column(length = 180)
	private String nombre;

	@Column(columnDefinition = "text")
	private String filtros;

	@Column(length = 40)
	private String orden;

	private boolean incluirOriginales;

	private int cantidadDocumentos;

	private int cantidadOmitidos;

	@Column(length = 120)
	private String bucket;

	@Column(length = 400)
	private String claveObjeto;

	@Column(length = 200)
	private String nombreArchivo;

	private long tamanoBytes;

	@Column(length = 64)
	private String sha256;

	@Column(columnDefinition = "text")
	private String detalleError;

	private Instant venceEn;

	private Instant avisoVencimiento;

	private Instant generado;

	private Instant alta;

	private Instant baja;
}
