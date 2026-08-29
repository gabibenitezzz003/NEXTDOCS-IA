package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class LineaWhatsappModel implements Serializable {

	private static final long serialVersionUID = 7714099533286064411L;

	private String id;

	private String nombre;

	private String numeroTelefono;

	private String identificadorNumero;

	private String identificadorCuenta;

	private String rutaWebhook;

	private String urlWebhook;

	private String estado;

	private String codigoPlantillaPorDefecto;

	private boolean exigirContactoAutorizado;

	private boolean exigirCorrelacion;

	private boolean acusarRecibo;

	private int maximoMediaPorMensaje;

	private int minutosVentanaCorrelacion;

	private String nombrePlantillaSolicitud;

	private String idiomaPlantillaSolicitud;

	private boolean puedeResponderFueraDeVentana;

	private Instant ultimoMensaje;

	private String ultimoError;

	private int fallosConsecutivos;

	private Instant alta;

	private List<ContactoWhatsappModel> contactos = new ArrayList<>();
}
