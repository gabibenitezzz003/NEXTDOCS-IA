package com.nextdocs.ai.modelos;

import java.io.Serializable;

import lombok.Data;

@Data
public class ResultadoLimiteModel implements Serializable {

	private static final long serialVersionUID = 5590123746622018834L;

	private boolean permitido;

	private String alcanceExcedido;

	private long limite;

	private long restantes;

	private long esperaSegundos;

	public static ResultadoLimiteModel permitido(long limite, long restantes, long esperaSegundos) {
		ResultadoLimiteModel modelo = new ResultadoLimiteModel();
		modelo.setPermitido(true);
		modelo.setLimite(limite);
		modelo.setRestantes(restantes);
		modelo.setEsperaSegundos(esperaSegundos);
		return modelo;
	}

	public static ResultadoLimiteModel rechazado(String alcance, long limite, long esperaSegundos) {
		ResultadoLimiteModel modelo = new ResultadoLimiteModel();
		modelo.setPermitido(false);
		modelo.setAlcanceExcedido(alcance);
		modelo.setLimite(limite);
		modelo.setRestantes(0);
		modelo.setEsperaSegundos(esperaSegundos);
		return modelo;
	}
}
