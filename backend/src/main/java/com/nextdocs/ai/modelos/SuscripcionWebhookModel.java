package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.enumeraciones.TipoEventoCanonico;

import lombok.Data;

@Data
public class SuscripcionWebhookModel implements Serializable {

	private static final long serialVersionUID = 3182046619027741539L;

	private String id;

	private String nombre;

	private String url;

	private String prefijoSecreto;

	private boolean activa;

	private boolean pausadaPorFallos;

	private int fallosConsecutivos;

	private Instant ultimaEntrega;

	private Instant alta;

	private List<TipoEventoCanonico> eventos = new ArrayList<>();
}
