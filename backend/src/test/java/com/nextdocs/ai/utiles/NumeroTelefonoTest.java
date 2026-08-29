package com.nextdocs.ai.utiles;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NumeroTelefonoTest {

	@Test
	@DisplayName("normaliza los formatos que escribe la gente y descarta los que no son E.164")
	void normalizaFormatosHumanos() {
		assertThat(NumeroTelefono.normalizar("+54 9 11 3322-4455")).isEqualTo("+5491133224455");
		assertThat(NumeroTelefono.normalizar("005491133224455")).isEqualTo("+5491133224455");
		assertThat(NumeroTelefono.normalizar("5491133224455")).isEqualTo("+5491133224455");
		assertThat(NumeroTelefono.normalizar("(11) 3322.4455")).isEqualTo("+1133224455");

		assertThat(NumeroTelefono.normalizar("11-2233")).isNull();
		assertThat(NumeroTelefono.normalizar("+0491133224455")).isNull();
		assertThat(NumeroTelefono.normalizar("no soy un numero")).isNull();
		assertThat(NumeroTelefono.normalizar(null)).isNull();
	}

	@Test
	@DisplayName("lo que carga una persona exige el prefijo internacional, lo que manda la red no")
	void loQueCargaUnaPersonaExigePrefijo() {
		assertThat(NumeroTelefono.normalizarDeclarado("+54 9 11 3322-4455")).isEqualTo("+5491133224455");
		assertThat(NumeroTelefono.normalizarDeclarado("005491133224455")).isEqualTo("+5491133224455");

		assertThat(NumeroTelefono.normalizarDeclarado("1133224455")).isNull();
		assertThat(NumeroTelefono.normalizarDeclarado("(11) 3322.4455")).isNull();
		assertThat(NumeroTelefono.normalizar("5491133224455")).isEqualTo("+5491133224455");
	}

	@Test
	@DisplayName("el comodin solo habilita el prefijo declarado y nunca a un numero de otro pais")
	void elComodinRespetaElPrefijo() {
		assertThat(NumeroTelefono.coincide("+54911*", "+5491133224455")).isTrue();
		assertThat(NumeroTelefono.coincide("+54911*", "+54 9 11 3322 4455")).isTrue();

		assertThat(NumeroTelefono.coincide("+54911*", "+5492213322445")).isFalse();
		assertThat(NumeroTelefono.coincide("+54911*", "+5511933224455")).isFalse();
	}

	@Test
	@DisplayName("un patron sin comodin exige el numero exacto")
	void elPatronExactoNoAdmiteParientes() {
		assertThat(NumeroTelefono.coincide("+5491133224455", "+5491133224455")).isTrue();
		assertThat(NumeroTelefono.coincide("+5491133224455", "+54911332244551")).isFalse();
		assertThat(NumeroTelefono.coincide("+549113322445", "+5491133224455")).isFalse();
	}

	@Test
	@DisplayName("un patron mal escrito no autoriza a nadie en vez de autorizar a todos")
	void elPatronInvalidoNoAutoriza() {
		assertThat(NumeroTelefono.esPatronValido("*")).isFalse();
		assertThat(NumeroTelefono.esPatronValido("")).isFalse();
		assertThat(NumeroTelefono.esPatronValido(null)).isFalse();
		assertThat(NumeroTelefono.coincide("*", "+5491133224455")).isFalse();
		assertThat(NumeroTelefono.coincide(null, "+5491133224455")).isFalse();
		assertThat(NumeroTelefono.coincide("+54911*", null)).isFalse();
	}

	@Test
	@DisplayName("el enmascarado deja ver los ultimos cuatro digitos y nada mas")
	void enmascaraParaLosLogs() {
		assertThat(NumeroTelefono.enmascarar("+5491133224455")).isEqualTo("+*********4455");
		assertThat(NumeroTelefono.enmascarar("no es un numero")).isEqualTo("****");
	}
}
