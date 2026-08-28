package com.nextdocs.ai.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import com.nextdocs.ai.config.ValidadorConfiguracionProduccion.ConfiguracionInseguraException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

class ValidadorConfiguracionProduccionTest {

	private ValidadorConfiguracionProduccion validador;

	private Map<String, Object> propiedades;

	@BeforeEach
	void preparar() {
		validador = new ValidadorConfiguracionProduccion();
		propiedades = new HashMap<>();
		propiedades.put("nextdocs.seguridad.jwtSecreto", "un-secreto-largo-y-propio-de-mas-de-32-bytes");
		propiedades.put("nextdocs.arranque.crearTenantDemostracion", "false");
		propiedades.put("nextdocs.almacenamiento.claveSecreta", "clave-propia-del-object-store");
		propiedades.put("spring.datasource.password", "clave-propia-de-la-base");
		propiedades.put("nextdocs.antivirus.motor", "CLAMAV");
	}

	@Test
	@DisplayName("sin el perfil produccion no valida nada")
	void sinPerfilNoValida() {
		propiedades.put("nextdocs.seguridad.jwtSecreto", ValidadorConfiguracionProduccion.JWT_POR_DEFECTO);

		assertThatCode(() -> validador.onApplicationEvent(evento("desarrollo"))).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("con una configuracion correcta el arranque sigue")
	void configuracionCorrecta() {
		assertThatCode(() -> validador.onApplicationEvent(evento("produccion"))).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("el secreto JWT de ejemplo frena el arranque")
	void secretoDeEjemplo() {
		propiedades.put("nextdocs.seguridad.jwtSecreto", ValidadorConfiguracionProduccion.JWT_POR_DEFECTO);

		assertThatThrownBy(() -> validador.onApplicationEvent(evento("produccion")))
				.isInstanceOf(ConfiguracionInseguraException.class)
				.hasMessageContaining("sigue siendo el valor de ejemplo");
	}

	@Test
	@DisplayName("un secreto JWT corto frena el arranque")
	void secretoCorto() {
		propiedades.put("nextdocs.seguridad.jwtSecreto", "corto");

		assertThatThrownBy(() -> validador.onApplicationEvent(evento("produccion")))
				.isInstanceOf(ConfiguracionInseguraException.class)
				.hasMessageContaining("el minimo para HS256");
	}

	@Test
	@DisplayName("el tenant de demostracion frena el arranque")
	void tenantDeDemostracion() {
		propiedades.put("nextdocs.arranque.crearTenantDemostracion", "true");

		assertThatThrownBy(() -> validador.onApplicationEvent(evento("produccion")))
				.isInstanceOf(ConfiguracionInseguraException.class)
				.hasMessageContaining("credenciales conocidas");
	}

	@Test
	@DisplayName("una clave de desarrollo conocida frena el arranque")
	void claveConocida() {
		propiedades.put("spring.datasource.password", "nextdocs");

		assertThatThrownBy(() -> validador.onApplicationEvent(evento("produccion")))
				.isInstanceOf(ConfiguracionInseguraException.class)
				.hasMessageContaining("valor de desarrollo conocido");
	}

	@Test
	@DisplayName("el antivirus permisivo frena el arranque salvo que se acepte explicitamente")
	void antivirusPermisivo() {
		propiedades.put("nextdocs.antivirus.motor", "PERMISIVO");

		assertThatThrownBy(() -> validador.onApplicationEvent(evento("produccion")))
				.isInstanceOf(ConfiguracionInseguraException.class)
				.hasMessageContaining("los archivos no se analizan");

		propiedades.put("nextdocs.antivirus.permitirSinAnalisis", "true");
		assertThatCode(() -> validador.onApplicationEvent(evento("produccion"))).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("una credencial de IA fijada por configuracion frena el arranque")
	void credencialEnClaro() {
		propiedades.put("nextdocs.proveedor-ia.claveGemini", "AQ.algo");

		assertThatThrownBy(() -> validador.onApplicationEvent(evento("produccion")))
				.isInstanceOf(ConfiguracionInseguraException.class)
				.hasMessageContaining("referenciaSecreto");
	}

	@Test
	@DisplayName("el mensaje enumera todos los problemas juntos, no de a uno")
	void enumeraTodosLosProblemas() {
		propiedades.put("nextdocs.seguridad.jwtSecreto", ValidadorConfiguracionProduccion.JWT_POR_DEFECTO);
		propiedades.put("nextdocs.arranque.crearTenantDemostracion", "true");
		propiedades.put("spring.datasource.password", "nextdocs");

		assertThatThrownBy(() -> validador.onApplicationEvent(evento("produccion")))
				.isInstanceOf(ConfiguracionInseguraException.class)
				.satisfies(error -> assertThat(error.getMessage()).contains("1. ").contains("2. ").contains("3. "));
	}

	private ApplicationEnvironmentPreparedEvent evento(String perfil) {
		MockEnvironment entorno = new MockEnvironment();
		entorno.setActiveProfiles(perfil);
		entorno.getPropertySources().addFirst(new MapPropertySource("prueba", propiedades));
		return new ApplicationEnvironmentPreparedEvent(new org.springframework.boot.DefaultBootstrapContext(),
				new SpringApplication(), new String[0], entorno);
	}
}
