package com.nextdocs.ai.restControladores;

import com.nextdocs.ai.modelos.RegistroOrganizacionReqModel;
import com.nextdocs.ai.modelos.SesionResModel;
import com.nextdocs.ai.servicios.RegistroService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/registro")
public class RegistroRestController extends ControladorRest<RegistroRestController> {

	private final RegistroService registroService;

	public RegistroRestController(RegistroService registroService) {
		this.registroService = registroService;
	}

	@PostMapping
	public ResponseEntity<SesionResModel> registrar(@Valid @RequestBody RegistroOrganizacionReqModel datos) {
		return new ResponseEntity<>(registroService.registrar(datos), HttpStatus.CREATED);
	}
}
