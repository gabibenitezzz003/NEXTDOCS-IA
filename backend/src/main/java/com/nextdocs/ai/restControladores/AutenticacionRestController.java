package com.nextdocs.ai.restControladores;

import java.util.Map;

import com.nextdocs.ai.modelos.AutenticacionReqModel;
import com.nextdocs.ai.modelos.SesionResModel;
import com.nextdocs.ai.servicios.AutenticacionService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/autenticacion")
public class AutenticacionRestController extends ControladorRest<AutenticacionRestController> {

	private final AutenticacionService autenticacionService;

	public AutenticacionRestController(AutenticacionService autenticacionService) {
		this.autenticacionService = autenticacionService;
	}

	@PostMapping("/ingresar")
	public ResponseEntity<SesionResModel> ingresar(@Valid @RequestBody AutenticacionReqModel datos) {
		return new ResponseEntity<>(autenticacionService.autenticar(datos), HttpStatus.OK);
	}

	@PostMapping("/refrescar")
	public ResponseEntity<SesionResModel> refrescar(@RequestBody Map<String, String> cuerpo) {
		return new ResponseEntity<>(autenticacionService.refrescar(cuerpo.get("tokenRefresco")), HttpStatus.OK);
	}
}
