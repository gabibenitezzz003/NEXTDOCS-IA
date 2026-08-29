package com.nextdocs.ai.modelos;

import java.io.Serializable;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class NuevoBuzonCorreoReqModel implements Serializable {

	private static final long serialVersionUID = 7719004623380557712L;

	@NotBlank
	@Email
	@Size(max = 256)
	private String direccion;

	@NotBlank
	@Size(max = 128)
	private String nombre;

	@NotBlank
	@Size(max = 256)
	private String hostEntrada;

	@Min(1)
	@Max(65535)
	private int puertoEntrada;

	@NotBlank
	@Size(max = 256)
	private String usuarioEntrada;

	@NotBlank
	@Size(max = 256)
	private String referenciaSecretoEntrada;

	@Size(max = 128)
	private String carpeta;

	private boolean entradaSegura;

	@Size(max = 256)
	private String hostSalida;

	@Min(0)
	@Max(65535)
	private int puertoSalida;

	@Size(max = 256)
	private String usuarioSalida;

	@Size(max = 256)
	private String referenciaSecretoSalida;

	private boolean salidaSegura;

	@Size(max = 64)
	private String codigoPlantillaPorDefecto;

	private boolean exigirRemitenteAutorizado;

	private boolean exigirCorrelacion;

	private boolean acusarRecibo;

	@Min(0)
	@Max(50)
	private int maximoAdjuntosPorMensaje;
}
