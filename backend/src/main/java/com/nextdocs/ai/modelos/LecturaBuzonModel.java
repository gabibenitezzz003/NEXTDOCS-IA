package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class LecturaBuzonModel implements Serializable {

	private static final long serialVersionUID = 1052447738660183397L;

	private String buzonId;

	private String direccion;

	private int mensajesLeidos;

	private int mensajesProcesados;

	private int documentosIngestados;

	private String error;

	private List<MensajeCorreoModel> mensajes = new ArrayList<>();
}
