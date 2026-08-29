package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "contacto_whatsapp_autorizado")
public class ContactoWhatsappAutorizado implements Serializable {

	private static final long serialVersionUID = 2087554103366298471L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "linea_id")
	private LineaWhatsapp linea;

	@Column(length = 64, nullable = false)
	private String patron;

	@Column(length = 256)
	private String descripcion;

	private Instant alta;

	private Instant baja;
}
