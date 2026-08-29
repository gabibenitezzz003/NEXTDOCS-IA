package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.EstadoLineaWhatsapp;

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
@Table(name = "linea_whatsapp", indexes = { @Index(name = "ix_linea_whatsapp_tenant", columnList = "tenant_id,alta") })
public class LineaWhatsapp implements Serializable {

	private static final long serialVersionUID = 3391547820116634205L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@Column(length = 128, nullable = false)
	private String nombre;

	@Column(length = 32, nullable = false)
	private String numeroTelefono;

	@Column(name = "identificador_numero", length = 64, nullable = false, unique = true)
	private String identificadorNumero;

	@Column(name = "identificador_cuenta", length = 64)
	private String identificadorCuenta;

	@Column(name = "ruta_webhook", length = 64, nullable = false, unique = true)
	private String rutaWebhook;

	@Column(length = 256, nullable = false)
	private String referenciaTokenAcceso;

	@Column(length = 256, nullable = false)
	private String referenciaSecretoAplicacion;

	@Column(length = 256, nullable = false)
	private String referenciaTokenVerificacion;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private EstadoLineaWhatsapp estado;

	@Column(length = 64)
	private String codigoPlantillaPorDefecto;

	private boolean exigirContactoAutorizado;

	private boolean exigirCorrelacion;

	private boolean acusarRecibo;

	private int maximoMediaPorMensaje;

	private int minutosVentanaCorrelacion;

	@Column(length = 128)
	private String nombrePlantillaSolicitud;

	@Column(length = 16)
	private String idiomaPlantillaSolicitud;

	private Instant ultimoMensaje;

	@Column(columnDefinition = "text")
	private String ultimoError;

	private int fallosConsecutivos;

	private Instant alta;

	private Instant baja;
}
