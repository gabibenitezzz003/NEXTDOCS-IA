package com.nextdocs.ai.utiles;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ValidadorCuitTest {

	@ParameterizedTest
	@ValueSource(strings = {
			"30-71458963-2",
			"30714589632",
			"30-70912345-5",
			"20-12345678-6",
			"27-06799455-3",
			"33-69345023-9",
	})
	void aceptaCuitConDigitoVerificadorCorrecto(String cuit) {
		assertThat(ValidadorCuit.esValido(cuit)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"30-71458963-3",
			"30-71458963-0",
			"30-70912345-4",
			"20-12345678-0",
	})
	void rechazaCuitConDigitoVerificadorEquivocado(String cuit) {
		assertThat(ValidadorCuit.esValido(cuit)).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"30-7145896-2",
			"307145896322",
			"",
			"   ",
			"no es un cuit",
			"AB-CDEFGHIJ-K",
	})
	void rechazaLoQueNoTieneOnceDigitos(String candidato) {
		assertThat(ValidadorCuit.esValido(candidato)).isFalse();
	}

	@Test
	void rechazaNulo() {
		assertThat(ValidadorCuit.esValido(null)).isFalse();
	}

	@Test
	void toleraEspaciosYPuntosAlrededorDeLosDigitos() {
		assertThat(ValidadorCuit.esValido(" 30.71458963.2 ")).isTrue();
	}

	@Test
	void elRestoOnceSeMapeaAVerificadorCero() {
		assertThat(ValidadorCuit.esValido("20-10000013-0")).isTrue();
		assertThat(ValidadorCuit.esValido("20-10000013-1")).isFalse();
	}

	@Test
	void elRestoDiezSeMapeaAVerificadorNueve() {
		assertThat(ValidadorCuit.esValido("20-10000005-9")).isTrue();
		assertThat(ValidadorCuit.esValido("20-10000005-0")).isFalse();
	}
}
