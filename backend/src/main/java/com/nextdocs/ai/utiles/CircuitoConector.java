package com.nextdocs.ai.utiles;

import java.time.Instant;

import com.nextdocs.ai.enumeraciones.EstadoCircuito;

public class CircuitoConector {

	private final int umbralFallos;

	private final long duracionAperturaSegundos;

	private EstadoCircuito estado = EstadoCircuito.CERRADO;

	private int fallosConsecutivos;

	private Instant abiertoHasta;

	public CircuitoConector(int umbralFallos, long duracionAperturaSegundos) {
		this.umbralFallos = Math.max(umbralFallos, 1);
		this.duracionAperturaSegundos = Math.max(duracionAperturaSegundos, 1);
	}

	public synchronized boolean permitePasar() {
		if (estado == EstadoCircuito.CERRADO) {
			return true;
		}
		if (estado == EstadoCircuito.SEMIABIERTO) {
			return true;
		}
		if (abiertoHasta != null && Instant.now().isAfter(abiertoHasta)) {
			estado = EstadoCircuito.SEMIABIERTO;
			return true;
		}
		return false;
	}

	public synchronized void registrarExito() {
		fallosConsecutivos = 0;
		estado = EstadoCircuito.CERRADO;
		abiertoHasta = null;
	}

	public synchronized void registrarFallo() {
		fallosConsecutivos++;
		if (estado == EstadoCircuito.SEMIABIERTO || fallosConsecutivos >= umbralFallos) {
			estado = EstadoCircuito.ABIERTO;
			abiertoHasta = Instant.now().plusSeconds(duracionAperturaSegundos);
		}
	}

	public synchronized EstadoCircuito getEstado() {
		if (estado == EstadoCircuito.ABIERTO && abiertoHasta != null && Instant.now().isAfter(abiertoHasta)) {
			return EstadoCircuito.SEMIABIERTO;
		}
		return estado;
	}

	public synchronized int getFallosConsecutivos() {
		return fallosConsecutivos;
	}

	public synchronized Instant getAbiertoHasta() {
		return abiertoHasta;
	}
}
