package com.nextdocs.ai.restControladores;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.nextdocs.ai.enumeraciones.EstadoEntregaWebhook;
import com.nextdocs.ai.enumeraciones.TipoEventoCanonico;
import com.nextdocs.ai.modelos.EntregaWebhookModel;
import com.nextdocs.ai.modelos.SaludIntegracionModel;
import com.nextdocs.ai.modelos.SuscripcionWebhookCreadaModel;
import com.nextdocs.ai.modelos.SuscripcionWebhookModel;
import com.nextdocs.ai.modelos.SuscripcionWebhookReqModel;
import com.nextdocs.ai.servicios.IntegracionService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integraciones")
public class IntegracionRestController extends ControladorRest<IntegracionRestController> {

	private final IntegracionService integracionService;

	public IntegracionRestController(IntegracionService integracionService) {
		this.integracionService = integracionService;
	}

	@GetMapping("/configuracion")
	public ResponseEntity<Map<String, Object>> configuracion() {
		Map<String, Object> configuracion = new HashMap<>();
		configuracion.put("eventos", listar(TipoEventoCanonico.values()));
		configuracion.put("clavesEvento", claves());
		configuracion.put("estadosEntrega", listar(EstadoEntregaWebhook.values()));
		return new ResponseEntity<>(configuracion, HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('gobernanza.leer')")
	@GetMapping("/salud")
	public ResponseEntity<SaludIntegracionModel> salud() {
		return new ResponseEntity<>(integracionService.salud(tenantId()), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('gobernanza.leer')")
	@GetMapping("/suscripciones")
	public ResponseEntity<List<SuscripcionWebhookModel>> suscripciones() {
		return new ResponseEntity<>(integracionService.listar(tenantId()), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('gobernanza.leer')")
	@GetMapping("/suscripciones/{id}")
	public ResponseEntity<SuscripcionWebhookModel> suscripcion(@PathVariable String id) {
		return new ResponseEntity<>(integracionService.obtener(tenantId(), id), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('gobernanza.administrar')")
	@PostMapping("/suscripciones")
	public ResponseEntity<SuscripcionWebhookCreadaModel> crear(@Valid @RequestBody SuscripcionWebhookReqModel datos) {
		return new ResponseEntity<>(integracionService.crear(tenant(), datos), HttpStatus.CREATED);
	}

	@PreAuthorize("hasAuthority('gobernanza.administrar')")
	@PutMapping("/suscripciones/{id}")
	public ResponseEntity<SuscripcionWebhookModel> modificar(@PathVariable String id,
			@Valid @RequestBody SuscripcionWebhookReqModel datos) {
		return new ResponseEntity<>(integracionService.modificar(tenantId(), id, datos), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('gobernanza.administrar')")
	@PostMapping("/suscripciones/{id}/pausar")
	public ResponseEntity<SuscripcionWebhookModel> pausar(@PathVariable String id) {
		return new ResponseEntity<>(integracionService.pausar(tenantId(), id), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('gobernanza.administrar')")
	@PostMapping("/suscripciones/{id}/reactivar")
	public ResponseEntity<SuscripcionWebhookModel> reactivar(@PathVariable String id) {
		return new ResponseEntity<>(integracionService.reactivar(tenantId(), id), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('gobernanza.administrar')")
	@PostMapping("/suscripciones/{id}/secreto")
	public ResponseEntity<SuscripcionWebhookCreadaModel> rotarSecreto(@PathVariable String id) {
		return new ResponseEntity<>(integracionService.rotarSecreto(tenantId(), id), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('gobernanza.administrar')")
	@PostMapping("/suscripciones/{id}/probar")
	public ResponseEntity<EntregaWebhookModel> probar(@PathVariable String id) {
		return new ResponseEntity<>(integracionService.probar(tenantId(), id), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('gobernanza.administrar')")
	@DeleteMapping("/suscripciones/{id}")
	public ResponseEntity<Void> eliminar(@PathVariable String id) {
		integracionService.eliminar(tenantId(), id);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@PreAuthorize("hasAuthority('gobernanza.leer')")
	@GetMapping("/entregas")
	public ResponseEntity<Page<EntregaWebhookModel>> entregas(@RequestParam(required = false) EstadoEntregaWebhook estado,
			@RequestParam(required = false) String suscripcionId, @RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "25") int tamano, @RequestParam(required = false) String orden) {
		return new ResponseEntity<>(
				integracionService.listarEntregas(tenantId(), estado, suscripcionId, paginado(pagina, tamano, orden)),
				HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('gobernanza.administrar')")
	@PostMapping("/entregas/{id}/reintentar")
	public ResponseEntity<EntregaWebhookModel> reintentar(@PathVariable String id) {
		return new ResponseEntity<>(integracionService.reintentar(tenantId(), id), HttpStatus.OK);
	}

	private List<String> claves() {
		List<String> claves = new ArrayList<>();
		for (TipoEventoCanonico evento : TipoEventoCanonico.values()) {
			claves.add(evento.getClave());
		}
		return claves;
	}
}
