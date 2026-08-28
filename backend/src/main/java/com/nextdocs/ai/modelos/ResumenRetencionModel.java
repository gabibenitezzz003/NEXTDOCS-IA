package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ResumenRetencionModel implements Serializable {

	private static final long serialVersionUID = 4451602283890171449L;

	private int evaluados;

	private int conservados;

	private int anonimizados;

	private int eliminados;

	private int retenidosPorRetencionLegal;

	private int omitidosSinPolitica;

	private int fallidos;

	private Instant ejecutado;

	private long duracionMilisegundos;

	private List<ResultadoRetencionModel> detalle = new ArrayList<>();
}
