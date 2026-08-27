package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import com.nextdocs.ai.enumeraciones.TipoEventoCanonico;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@Table(name = "suscripcion_webhook")
public class SuscripcionWebhook implements Serializable {
	private static final long serialVersionUID = 4974633805449383001L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@Column(length = 128, nullable = false)
	private String nombre;

	@Column(length = 1024, nullable = false)
	private String url;

	@Column(length = 256, nullable = false)
	private String secreto;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "suscripcion_webhook_evento", joinColumns = @JoinColumn(name = "suscripcion_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_evento", length = 64)
	@EqualsAndHashCode.Exclude
	@ToString.Exclude
	private Set<TipoEventoCanonico> eventos = new LinkedHashSet<>();

	private boolean activa;

	private int fallosConsecutivos;

	private Instant ultimaEntrega;

	private Instant alta;

	private Instant baja;
}
