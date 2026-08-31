package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.EstadoDocumento;

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
@Table(name = "item_exportacion", indexes = @Index(name = "ix_item_exportacion_lote", columnList = "lote_id,alta"))
public class ItemExportacion implements Serializable {

	private static final long serialVersionUID = 8815602274419036550L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lote_id")
	private LoteExportacion lote;

	@ManyToOne(fetch = FetchType.LAZY)
	private Documento documento;

	@Column(length = 80)
	private String tipoObjeto;

	@Column(length = 120)
	private String idObjeto;

	@Column(length = 80)
	private String codigoPlantilla;

	private int numeroVersionPlantilla;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado_documento", length = 32)
	private EstadoDocumento estadoDocumento;

	@Column(length = 400)
	private String nombreEnArchivo;

	private long tamanoBytes;

	@Column(length = 64)
	private String sha256;

	private int hallazgos;

	@Column(length = 200)
	private String motivoOmision;

	private Instant recibido;

	private Instant cerrado;

	private Instant alta;
}
