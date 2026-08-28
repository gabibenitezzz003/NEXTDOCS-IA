package com.nextdocs.ai.utiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nextdocs.ai.exceptions.ValidacionException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorUrlWebhookTest {

	@Test
	@DisplayName("HTTPS publico se acepta")
	void httpsPublico() {
		assertThat(ValidadorUrlWebhook.validar("https://8.8.8.8/hook", false).getHost()).isEqualTo("8.8.8.8");
	}

	@Test
	@DisplayName("HTTP de un host publico se rechaza")
	void httpPublico() {
		assertThatThrownBy(() -> ValidadorUrlWebhook.validar("http://8.8.8.8/hook", false))
				.isInstanceOf(ValidacionException.class)
				.hasMessageContaining("HTTPS");
	}

	@Test
	@DisplayName("localhost se rechaza si no esta habilitado")
	void localhostSinPermiso() {
		assertThatThrownBy(() -> ValidadorUrlWebhook.validar("https://localhost/hook", false))
				.isInstanceOf(ValidacionException.class)
				.hasMessageContaining("red interna");
	}

	@Test
	@DisplayName("HTTP a loopback se acepta solo si localhost esta habilitado")
	void httpLoopbackConPermiso() {
		assertThat(ValidadorUrlWebhook.validar("http://127.0.0.1:9000/hook", true).getPort()).isEqualTo(9000);
	}

	@Test
	@DisplayName("una IP privada no se acepta aunque localhost este habilitado")
	void ipPrivada() {
		assertThatThrownBy(() -> ValidadorUrlWebhook.validar("https://10.0.0.8/hook", true))
				.isInstanceOf(ValidacionException.class)
				.hasMessageContaining("red interna");
	}

	@Test
	@DisplayName("el host de metadatos se rechaza")
	void metadatos() {
		assertThatThrownBy(() -> ValidadorUrlWebhook.validar("https://metadata.google.internal/hook", false))
				.isInstanceOf(ValidacionException.class)
				.hasMessageContaining("red interna");
	}

	@Test
	@DisplayName("una URL vacia se rechaza")
	void vacia() {
		assertThatThrownBy(() -> ValidadorUrlWebhook.validar("  ", false)).isInstanceOf(ValidacionException.class);
	}
}
