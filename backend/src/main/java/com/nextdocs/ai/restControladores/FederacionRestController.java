package com.nextdocs.ai.restControladores;

import java.util.List;

import com.nextdocs.ai.modelos.CanjeEmbedReqModel;
import com.nextdocs.ai.modelos.CodigoEmbedModel;
import com.nextdocs.ai.modelos.IntercambioFederadoReqModel;
import com.nextdocs.ai.modelos.NuevoProveedorIdentidadReqModel;
import com.nextdocs.ai.modelos.ProveedorIdentidadModel;
import com.nextdocs.ai.modelos.SesionResModel;
import com.nextdocs.ai.servicios.FederacionIdentidadService;
import com.nextdocs.ai.servicios.ProveedorIdentidadService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/federacion")
public class FederacionRestController extends ControladorRest<FederacionRestController> {

	private final FederacionIdentidadService federacionIdentidadService;

	private final ProveedorIdentidadService proveedorIdentidadService;

	public FederacionRestController(FederacionIdentidadService federacionIdentidadService,
			ProveedorIdentidadService proveedorIdentidadService) {
		this.federacionIdentidadService = federacionIdentidadService;
		this.proveedorIdentidadService = proveedorIdentidadService;
	}

	@PostMapping("/intercambio")
	public ResponseEntity<CodigoEmbedModel> intercambiar(
			@Valid @RequestBody IntercambioFederadoReqModel datos) {
		return new ResponseEntity<>(federacionIdentidadService.intercambiar(datos), HttpStatus.OK);
	}

	@PostMapping("/canje")
	public ResponseEntity<SesionResModel> canjear(@Valid @RequestBody CanjeEmbedReqModel datos) {
		return new ResponseEntity<>(federacionIdentidadService.canjear(datos.getCodigo()), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@PostMapping("/proveedores")
	public ResponseEntity<ProveedorIdentidadModel> crear(
			@Valid @RequestBody NuevoProveedorIdentidadReqModel datos) {
		return new ResponseEntity<>(proveedorIdentidadService.crear(tenant(), datos), HttpStatus.CREATED);
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@GetMapping("/proveedores")
	public ResponseEntity<List<ProveedorIdentidadModel>> listar() {
		return new ResponseEntity<>(proveedorIdentidadService.listar(tenantId()), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@PostMapping("/proveedores/{proveedorId}/activo")
	public ResponseEntity<ProveedorIdentidadModel> cambiarActivo(@PathVariable String proveedorId,
			@RequestParam boolean activo) {
		return new ResponseEntity<>(proveedorIdentidadService.cambiarActivo(tenantId(), proveedorId, activo),
				HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@DeleteMapping("/proveedores/{proveedorId}")
	public ResponseEntity<Void> eliminar(@PathVariable String proveedorId) {
		proveedorIdentidadService.eliminar(tenantId(), proveedorId);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
}
