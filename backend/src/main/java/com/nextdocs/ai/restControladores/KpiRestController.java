package com.nextdocs.ai.restControladores;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.nextdocs.ai.modelos.DocumentoModel;
import com.nextdocs.ai.modelos.KpiPlantillaModel;
import com.nextdocs.ai.modelos.KpiResumenModel;
import com.nextdocs.ai.servicios.KpiService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/kpi")
public class KpiRestController extends ControladorRest<KpiRestController> {

	private final KpiService kpiService;

	public KpiRestController(KpiService kpiService) {
		this.kpiService = kpiService;
	}

	@GetMapping("/configuracion")
	public ResponseEntity<Map<String, Object>> configuracion() {
		return new ResponseEntity<>(Map.of("indicadoresConPoblacion", KpiService.CON_POBLACION, "semaforos",
				List.of("VERDE", "AMBAR", "ROJO", "SIN_DATOS"), "saludPlantilla",
				List.of("OK", "ATENCION", "CRITICO", "SIN_DATOS")), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('documentos.leer')")
	@GetMapping("/resumen")
	public ResponseEntity<KpiResumenModel> resumen(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta) {
		return new ResponseEntity<>(kpiService.resumir(tenantId(), desde, hasta), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('documentos.leer')")
	@GetMapping("/plantillas")
	public ResponseEntity<List<KpiPlantillaModel>> plantillas(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta) {
		return new ResponseEntity<>(kpiService.porPlantilla(tenantId(), desde, hasta), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('documentos.leer')")
	@GetMapping("/poblacion")
	public ResponseEntity<List<DocumentoModel>> poblacion(@RequestParam String indicador,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta) {
		return new ResponseEntity<>(kpiService.poblacion(tenantId(), indicador, desde, hasta), HttpStatus.OK);
	}
}
