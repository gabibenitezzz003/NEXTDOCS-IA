package com.nextdocs.ai.restControladores;

import com.nextdocs.ai.modelos.TenantModel;
import com.nextdocs.ai.servicios.BootstrapService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/bootstrap")
public class BootstrapRestController extends ControladorRest<BootstrapRestController> {

	private final BootstrapService bootstrapService;

	public BootstrapRestController(BootstrapService bootstrapService) {
		this.bootstrapService = bootstrapService;
	}

	@PostMapping("/tenant")
	public ResponseEntity<TenantModel> crearTenant(@RequestHeader("X-Bootstrap-Secreto") String secreto,
			@Valid @RequestBody BootstrapReqModel datos) {
		TenantModel tenant = bootstrapService.crearTenant(secreto, datos.getCodigoTenant(), datos.getNombreTenant(),
				datos.getEmailAdministrador(), datos.getClaveAdministrador());
		return new ResponseEntity<>(tenant, HttpStatus.CREATED);
	}
}
