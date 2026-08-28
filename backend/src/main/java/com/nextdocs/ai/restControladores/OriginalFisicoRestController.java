package com.nextdocs.ai.restControladores;

import java.util.List;
import java.util.Map;

import com.nextdocs.ai.enumeraciones.EstadoOriginalFisico;
import com.nextdocs.ai.enumeraciones.PoliticaOriginalFisico;
import com.nextdocs.ai.modelos.OriginalFisicoModel;
import com.nextdocs.ai.modelos.OriginalFisicoReqModel;
import com.nextdocs.ai.servicios.OriginalFisicoService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/originales-fisicos")
public class OriginalFisicoRestController extends ControladorRest<OriginalFisicoRestController> {

	private final OriginalFisicoService originalFisicoService;

	public OriginalFisicoRestController(OriginalFisicoService originalFisicoService) {
		this.originalFisicoService = originalFisicoService;
	}

	@GetMapping("/configuracion")
	public ResponseEntity<Map<String, Object>> configuracion() {
		return new ResponseEntity<>(Map.of("estados", listar(EstadoOriginalFisico.values()), "politicas",
				listar(PoliticaOriginalFisico.values())), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('documentos.leer')")
	@GetMapping
	public ResponseEntity<List<OriginalFisicoModel>> listar(
			@RequestParam(required = false) EstadoOriginalFisico estado) {
		return new ResponseEntity<>(originalFisicoService.listarPorEstado(tenantId(), estado), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('documentos.leer')")
	@GetMapping("/documento/{documentoId}")
	public ResponseEntity<OriginalFisicoModel> porDocumento(@PathVariable("documentoId") String documentoId) {
		return originalFisicoService.buscarPorDocumento(tenantId(), documentoId)
				.map(modelo -> new ResponseEntity<>(modelo, HttpStatus.OK))
				.orElseGet(() -> new ResponseEntity<>(HttpStatus.NO_CONTENT));
	}

	@PreAuthorize("hasAuthority('documentos.escribir')")
	@PostMapping("/documento/{documentoId}/recibir")
	public ResponseEntity<OriginalFisicoModel> recibir(@PathVariable("documentoId") String documentoId,
			@Valid @RequestBody(required = false) OriginalFisicoReqModel datos) {
		return new ResponseEntity<>(originalFisicoService.registrarRecepcion(tenantId(), documentoId,
				usuarioObligatorio(), datos), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('documentos.escribir')")
	@PostMapping("/documento/{documentoId}/archivar")
	public ResponseEntity<OriginalFisicoModel> archivar(@PathVariable("documentoId") String documentoId,
			@Valid @RequestBody(required = false) OriginalFisicoReqModel datos) {
		return new ResponseEntity<>(originalFisicoService.registrarArchivado(tenantId(), documentoId,
				usuarioObligatorio(), datos), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('documentos.escribir')")
	@PostMapping("/documento/{documentoId}/extraviar")
	public ResponseEntity<OriginalFisicoModel> extraviar(@PathVariable("documentoId") String documentoId,
			@Valid @RequestBody OriginalFisicoReqModel datos) {
		return new ResponseEntity<>(originalFisicoService.registrarExtravio(tenantId(), documentoId,
				usuarioObligatorio(), datos), HttpStatus.OK);
	}
}
