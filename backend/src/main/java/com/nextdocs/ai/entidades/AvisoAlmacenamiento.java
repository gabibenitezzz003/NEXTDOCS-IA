package com.nextdocs.ai.entidades;

import java.io.Serializable;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Data;

@Data
@Entity
@Table(name = "aviso_almacenamiento", uniqueConstraints = @UniqueConstraint(columnNames = { "tenant_id", "umbral" }))
public class AvisoAlmacenamiento implements Serializable {

	private static final long serialVersionUID = 2260491175534398820L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@Column(nullable = false)
	private int umbral;

	private long bytesUsados;

	private long cuotaBytes;

	private Instant alta;
}
