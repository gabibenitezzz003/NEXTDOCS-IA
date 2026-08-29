package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.EstadoBuzonCorreo;

import jakarta.persistence.Column;
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

import lombok.Data;

@Data
@Entity
@Table(name = "buzon_correo", indexes = { @Index(name = "ix_buzon_correo_tenant", columnList = "tenant_id,alta") })
public class BuzonCorreo implements Serializable {

	private static final long serialVersionUID = 6118374290551237744L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@Column(length = 256, nullable = false, unique = true)
	private String direccion;

	@Column(length = 128, nullable = false)
	private String nombre;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private EstadoBuzonCorreo estado;

	@Column(length = 256, nullable = false)
	private String hostEntrada;

	private int puertoEntrada;

	@Column(length = 256, nullable = false)
	private String usuarioEntrada;

	@Column(length = 256, nullable = false)
	private String referenciaSecretoEntrada;

	@Column(length = 128, nullable = false)
	private String carpeta;

	private boolean entradaSegura;

	@Column(length = 256)
	private String hostSalida;

	private int puertoSalida;

	@Column(length = 256)
	private String usuarioSalida;

	@Column(length = 256)
	private String referenciaSecretoSalida;

	private boolean salidaSegura;

	@Column(length = 64)
	private String codigoPlantillaPorDefecto;

	private boolean exigirRemitenteAutorizado;

	private boolean exigirCorrelacion;

	private boolean acusarRecibo;

	private int maximoAdjuntosPorMensaje;

	private Instant ultimaLectura;

	@Column(columnDefinition = "text")
	private String ultimoError;

	private int fallosConsecutivos;

	private Instant alta;

	private Instant baja;
}
