package com.nextdocs.ai.restControladores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nextdocs.ai.modelos.TenantModel;
import com.nextdocs.ai.servicios.BootstrapService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class BootstrapRestControllerTest {

	private BootstrapService bootstrapService;

	private BootstrapRestController controlador;

	@BeforeEach
	void preparar() {
		bootstrapService = mock(BootstrapService.class);
		controlador = new BootstrapRestController(bootstrapService);
	}

	@Test
	@DisplayName("crea el tenant usando el secreto recibido en el header")
	void creaTenant() {
		TenantModel modelo = new TenantModel();
		modelo.setCodigo("empresa-prueba");
		when(bootstrapService.crearTenant(eq("secreto-bootstrap"), eq("empresa-prueba"), eq("Empresa de prueba"),
				eq("admin@empresa-prueba.com"), eq("clave-admin")))
				.thenReturn(modelo);

		BootstrapReqModel datos = new BootstrapReqModel();
		datos.setCodigoTenant("empresa-prueba");
		datos.setNombreTenant("Empresa de prueba");
		datos.setEmailAdministrador("admin@empresa-prueba.com");
		datos.setClaveAdministrador("clave-admin");

		ResponseEntity<TenantModel> respuesta = controlador.crearTenant("secreto-bootstrap", datos);

		assertThat(respuesta.getStatusCode().value()).isEqualTo(201);
		assertThat(respuesta.getBody()).isSameAs(modelo);
		verify(bootstrapService).crearTenant(eq("secreto-bootstrap"), eq("empresa-prueba"), eq("Empresa de prueba"),
				eq("admin@empresa-prueba.com"), eq("clave-admin"));
	}
}
