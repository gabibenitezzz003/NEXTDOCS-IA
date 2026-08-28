package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.math.BigDecimal;

import com.nextdocs.ai.enumeraciones.SensibilidadCampo;
import com.nextdocs.ai.enumeraciones.TipoDatoCampo;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class CampoPlantillaReqModel implements Serializable {

	private static final long serialVersionUID = 6604118250873441960L;

	@NotBlank
	@Size(max = 128)
	@Pattern(regexp = "[a-zA-Z][a-zA-Z0-9]*", message = "La clave debe ser camelCase sin espacios ni guiones")
	private String clave;

	@NotBlank
	@Size(max = 256)
	private String etiqueta;

	@NotNull
	private TipoDatoCampo tipoDato;

	@Size(max = 1024)
	private String alias;

	@Size(max = 1024)
	private String descripcion;

	private boolean requerido;

	private boolean extraer = true;

	private boolean validar = true;

	private boolean comparar;

	private boolean unico;

	@Size(max = 512)
	private String expresionRegular;

	@Size(max = 256)
	private String formatoFecha;

	@Size(max = 128)
	private String catalogoReferencia;

	@DecimalMin("0.0")
	@DecimalMax("1.0")
	private BigDecimal umbralConfianza;

	private SensibilidadCampo sensibilidad;

	private int orden;
}
