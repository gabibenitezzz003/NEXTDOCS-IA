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

import lombok.Data;

@Data
@Entity
@Table(name = "conjunto_prueba_plantilla")
public class ConjuntoPruebaPlantilla implements Serializable {

	private static final long serialVersionUID = 6619037741208836411L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Tenant tenant;

	@ManyToOne(fetch = FetchType.LAZY)
	private PlantillaDocumental plantilla;

	@Column(length = 64)
	private String huella;

	private Instant alta;

	private Instant baja;
}
