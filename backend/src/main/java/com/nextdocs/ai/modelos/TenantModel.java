package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.EstadoTenant;

import lombok.Data;

@Data
public class TenantModel implements Serializable {

	private static final long serialVersionUID = 6607157119473254102L;

	private String id;

	private String codigo;

	private String nombre;

	private EstadoTenant estado;

	private String plan;

	private String region;

	private String dominio;

	private long cuotaAlmacenamientoBytes;

	private long almacenamientoUsadoBytes;

	private long usuariosActivos;

	private Instant alta;
}
