package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class BuzonCorreoModel implements Serializable {

	private static final long serialVersionUID = 3345807754621198863L;

	private String id;

	private String direccion;

	private String nombre;

	private String estado;

	private String hostEntrada;

	private int puertoEntrada;

	private String usuarioEntrada;

	private String carpeta;

	private boolean entradaSegura;

	private String hostSalida;

	private int puertoSalida;

	private boolean salidaSegura;

	private boolean puedeResponder;

	private String codigoPlantillaPorDefecto;

	private boolean exigirRemitenteAutorizado;

	private boolean exigirCorrelacion;

	private boolean acusarRecibo;

	private int maximoAdjuntosPorMensaje;

	private Instant ultimaLectura;

	private String ultimoError;

	private int fallosConsecutivos;

	private Instant alta;

	private List<RemitenteAutorizadoModel> remitentes = new ArrayList<>();
}
