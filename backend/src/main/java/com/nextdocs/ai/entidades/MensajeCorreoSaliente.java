package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.EstadoMensajeSaliente;
import com.nextdocs.ai.enumeraciones.PlantillaCorreo;

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
@Table(name = "mensaje_correo_saliente",
		indexes = { @Index(name = "ix_mensaje_correo_saliente_tenant", columnList = "tenant_id,alta") })
public class MensajeCorreoSaliente implements Serializable {

	private static final long serialVersionUID = 5527309148820076241L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "buzon_id")
	private BuzonCorreo buzon;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "correlacion_id")
	private CorrelacionCorreo correlacion;

	@Enumerated(EnumType.STRING)
	@Column(length = 48, nullable = false)
	private PlantillaCorreo plantilla;

	@Column(length = 400, nullable = false)
	private String destinatarios;

	@Column(length = 400)
	private String asunto;

	@Column(name = "identificador_mensaje", length = 400)
	private String identificadorMensaje;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private EstadoMensajeSaliente estado;

	@Column(columnDefinition = "text")
	private String detalleError;

	private Instant enviado;

	private Instant alta;
}
