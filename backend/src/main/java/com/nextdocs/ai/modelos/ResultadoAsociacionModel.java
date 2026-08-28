package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.enumeraciones.ResultadoAsociacion;

import lombok.Data;

@Data
public class ResultadoAsociacionModel implements Serializable {

	private static final long serialVersionUID = 7708344216609238160L;

	private ResultadoAsociacion resultado;

	private int cantidadCandidatos;

	private String candidatoSeleccionadoId;

	private String motivo;

	private List<String> conectoresConsultados = new ArrayList<>();

	private List<String> conectoresFallidos = new ArrayList<>();

	private List<CandidatoAsociacionModel> candidatos = new ArrayList<>();

	public boolean requiereRevisionHumana() {
		return ResultadoAsociacion.AMBIGUA == resultado;
	}

	public boolean huboFallo() {
		return ResultadoAsociacion.CONECTOR_FALLIDO == resultado || !conectoresFallidos.isEmpty();
	}
}
