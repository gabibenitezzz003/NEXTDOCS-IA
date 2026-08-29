package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.EstadoMensajeSaliente;
import com.nextdocs.ai.enumeraciones.PlantillaWhatsapp;

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
@Table(name = "mensaje_whatsapp_saliente",
		indexes = { @Index(name = "ix_mensaje_whatsapp_saliente_tenant", columnList = "tenant_id,alta") })
public class MensajeWhatsappSaliente implements Serializable {

	private static final long serialVersionUID = 9012683375440129903L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "linea_id")
	private LineaWhatsapp linea;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "correlacion_id")
	private CorrelacionWhatsapp correlacion;

	@Enumerated(EnumType.STRING)
	@Column(length = 48, nullable = false)
	private PlantillaWhatsapp plantilla;

	@Column(name = "numero_destino", length = 32, nullable = false)
	private String numeroDestino;

	@Column(name = "identificador_mensaje", length = 128)
	private String identificadorMensaje;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private EstadoMensajeSaliente estado;

	private boolean dentroDeVentana;

	@Column(length = 128)
	private String nombrePlantillaMeta;

	@Column(columnDefinition = "text")
	private String cuerpo;

	@Column(columnDefinition = "text")
	private String detalleError;

	private Instant enviado;

	private Instant alta;
}
