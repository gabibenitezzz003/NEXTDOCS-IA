package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.TipoActor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.Data;

@Data
@Entity
@Table(name = "evento_auditoria", indexes = {
		@Index(name = "ix_auditoria_tenant_fecha", columnList = "tenant_id,fecha"),
		@Index(name = "ix_auditoria_recurso", columnList = "tenant_id,tipo_recurso,id_recurso"),
		@Index(name = "ix_auditoria_correlacion", columnList = "correlacion_id") })
public class EventoAuditoria implements Serializable {
	private static final long serialVersionUID = 506508616415280468L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@Column(name = "tenant_id", length = 36)
	private String tenantId;

	@Enumerated(EnumType.STRING)
	@Column(length = 32, nullable = false)
	private TipoActor tipoActor;

	@Column(length = 36)
	private String idActor;

	@Column(length = 256)
	private String descripcionActor;

	@Enumerated(EnumType.STRING)
	@Column(length = 64, nullable = false)
	private AccionAuditoria accion;

	@Column(length = 64)
	private String tipoRecurso;

	@Column(length = 36)
	private String idRecurso;

	@Column(length = 128)
	private String hashAntes;

	@Column(length = 128)
	private String hashDespues;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private String detalle;

	@Column(length = 64)
	private String direccionIp;

	@Column(length = 256)
	private String agenteUsuario;

	@Column(length = 64)
	private String correlacionId;

	private boolean exitoso;

	private Instant fecha;
}
