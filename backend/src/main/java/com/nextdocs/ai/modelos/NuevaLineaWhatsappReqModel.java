package com.nextdocs.ai.modelos;

import java.io.Serializable;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class NuevaLineaWhatsappReqModel implements Serializable {

	private static final long serialVersionUID = 5560884371249200637L;

	@NotBlank
	@Size(max = 128)
	private String nombre;

	@NotBlank
	@Size(max = 32)
	private String numeroTelefono;

	@NotBlank
	@Size(max = 64)
	private String identificadorNumero;

	@Size(max = 64)
	private String identificadorCuenta;

	@NotBlank
	@Size(max = 256)
	private String referenciaTokenAcceso;

	@NotBlank
	@Size(max = 256)
	private String referenciaSecretoAplicacion;

	@NotBlank
	@Size(max = 256)
	private String referenciaTokenVerificacion;

	@Size(max = 64)
	private String codigoPlantillaPorDefecto;

	private boolean exigirContactoAutorizado;

	private boolean exigirCorrelacion;

	private boolean acusarRecibo;

	@Min(0)
	@Max(30)
	private int maximoMediaPorMensaje;

	@Min(0)
	@Max(43200)
	private int minutosVentanaCorrelacion;

	@Size(max = 128)
	private String nombrePlantillaSolicitud;

	@Size(max = 16)
	private String idiomaPlantillaSolicitud;
}
