package com.nextdocs.ai.restControladores;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.nextdocs.ai.enumeraciones.AccionPresupuestoCosto;
import com.nextdocs.ai.modelos.PoliticaCostoModel;
import com.nextdocs.ai.modelos.PoliticaCostoReqModel;
import com.nextdocs.ai.modelos.ResumenCostoModel;
import com.nextdocs.ai.servicios.ObservabilidadService;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/observabilidad")
public class ObservabilidadRestController extends ControladorRest<ObservabilidadRestController> {

	private final ObservabilidadService observabilidadService;

	public ObservabilidadRestController(ObservabilidadService observabilidadService) {
		this.observabilidadService = observabilidadService;
	}

	@GetMapping("/configuracion")
	public ResponseEntity<Map<String, Object>> configuracion() {
		Map<String, Object> configuracion = new HashMap<>();
		configuracion.put("accionesPresupuesto", listar(AccionPresupuestoCosto.values()));
		return new ResponseEntity<>(configuracion, HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('gobernanza.leer')")
	@GetMapping("/costo")
	public ResponseEntity<ResumenCostoModel> costo(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta) {
		return new ResponseEntity<>(observabilidadService.resumir(tenantId(), desde, hasta), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('gobernanza.leer')")
	@GetMapping("/presupuesto")
	public ResponseEntity<PoliticaCostoModel> presupuesto() {
		return new ResponseEntity<>(observabilidadService.obtenerPolitica(tenantId()), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('gobernanza.administrar')")
	@PutMapping("/presupuesto")
	public ResponseEntity<PoliticaCostoModel> actualizarPresupuesto(@Valid @RequestBody PoliticaCostoReqModel datos) {
		return new ResponseEntity<>(observabilidadService.actualizarPolitica(tenantId(), datos), HttpStatus.OK);
	}
}
