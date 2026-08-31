package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import lombok.Data;

@Data
public class RangoKpiModel implements Serializable {

	private static final long serialVersionUID = 6023481127740355019L;

	private static final int DIAS_POR_DEFECTO = 30;

	private Instant desde;

	private Instant hasta;

	private long dias;

	public static RangoKpiModel de(Instant desde, Instant hasta) {
		Instant fin = hasta == null ? Instant.now() : hasta;
		Instant inicio = desde == null ? fin.minus(DIAS_POR_DEFECTO, ChronoUnit.DAYS) : desde;
		if (inicio.isAfter(fin)) {
			Instant intercambio = inicio;
			inicio = fin;
			fin = intercambio;
		}
		RangoKpiModel rango = new RangoKpiModel();
		rango.setDesde(inicio);
		rango.setHasta(fin);
		rango.setDias(Math.max(Duration.between(inicio, fin).toDays(), 1));
		return rango;
	}

	public RangoKpiModel anterior() {
		Duration duracion = Duration.between(desde, hasta);
		return de(desde.minus(duracion), desde);
	}
}
