package com.nextdocs.ai.utiles;

import static org.assertj.core.api.Assertions.assertThat;

import com.nextdocs.ai.enumeraciones.EstadoCircuito;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CircuitoConectorTest {

	@Test
	@DisplayName("arranca cerrado y deja pasar")
	void arrancaCerrado() {
		CircuitoConector circuito = new CircuitoConector(3, 60);

		assertThat(circuito.getEstado()).isEqualTo(EstadoCircuito.CERRADO);
		assertThat(circuito.permitePasar()).isTrue();
	}

	@Test
	@DisplayName("se abre recien al alcanzar el umbral de fallos consecutivos")
	void seAbreEnElUmbral() {
		CircuitoConector circuito = new CircuitoConector(3, 60);

		circuito.registrarFallo();
		circuito.registrarFallo();
		assertThat(circuito.getEstado()).isEqualTo(EstadoCircuito.CERRADO);
		assertThat(circuito.permitePasar()).isTrue();

		circuito.registrarFallo();
		assertThat(circuito.getEstado()).isEqualTo(EstadoCircuito.ABIERTO);
		assertThat(circuito.permitePasar()).isFalse();
	}

	@Test
	@DisplayName("un exito antes del umbral reinicia el contador")
	void elExitoReiniciaElContador() {
		CircuitoConector circuito = new CircuitoConector(3, 60);

		circuito.registrarFallo();
		circuito.registrarFallo();
		circuito.registrarExito();
		assertThat(circuito.getFallosConsecutivos()).isZero();

		circuito.registrarFallo();
		circuito.registrarFallo();
		assertThat(circuito.permitePasar()).isTrue();
	}

	@Test
	@DisplayName("pasado el tiempo de apertura queda semiabierto y deja pasar una sonda")
	void pasaASemiabierto() throws InterruptedException {
		CircuitoConector circuito = new CircuitoConector(1, 1);

		circuito.registrarFallo();
		assertThat(circuito.permitePasar()).isFalse();

		Thread.sleep(1100);

		assertThat(circuito.getEstado()).isEqualTo(EstadoCircuito.SEMIABIERTO);
		assertThat(circuito.permitePasar()).isTrue();
	}

	@Test
	@DisplayName("si la sonda del semiabierto falla, vuelve a abrirse de inmediato")
	void laSondaFallidaReabre() throws InterruptedException {
		CircuitoConector circuito = new CircuitoConector(2, 1);

		circuito.registrarFallo();
		circuito.registrarFallo();
		Thread.sleep(1100);
		assertThat(circuito.permitePasar()).isTrue();

		circuito.registrarFallo();

		assertThat(circuito.getEstado()).isEqualTo(EstadoCircuito.ABIERTO);
		assertThat(circuito.permitePasar()).isFalse();
	}

	@Test
	@DisplayName("si la sonda del semiabierto tiene exito, el circuito se cierra")
	void laSondaExitosaCierra() throws InterruptedException {
		CircuitoConector circuito = new CircuitoConector(2, 1);

		circuito.registrarFallo();
		circuito.registrarFallo();
		Thread.sleep(1100);
		circuito.permitePasar();
		circuito.registrarExito();

		assertThat(circuito.getEstado()).isEqualTo(EstadoCircuito.CERRADO);
		assertThat(circuito.getFallosConsecutivos()).isZero();
		assertThat(circuito.getAbiertoHasta()).isNull();
	}
}
