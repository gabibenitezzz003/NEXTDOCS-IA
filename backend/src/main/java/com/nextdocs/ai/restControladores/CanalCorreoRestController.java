package com.nextdocs.ai.restControladores;

import java.util.List;
import java.util.Map;

import com.nextdocs.ai.enumeraciones.EstadoBuzonCorreo;
import com.nextdocs.ai.enumeraciones.PlantillaCorreo;
import com.nextdocs.ai.enumeraciones.ResultadoAdjuntoCorreo;
import com.nextdocs.ai.enumeraciones.ResultadoMensajeCorreo;
import com.nextdocs.ai.modelos.BuzonCorreoModel;
import com.nextdocs.ai.modelos.CorrelacionCorreoModel;
import com.nextdocs.ai.modelos.LecturaBuzonModel;
import com.nextdocs.ai.modelos.MensajeCorreoModel;
import com.nextdocs.ai.modelos.MensajeSalienteModel;
import com.nextdocs.ai.modelos.NuevaCorrelacionCorreoReqModel;
import com.nextdocs.ai.modelos.NuevoBuzonCorreoReqModel;
import com.nextdocs.ai.modelos.RemitenteAutorizadoModel;
import com.nextdocs.ai.servicios.BuzonCorreoService;
import com.nextdocs.ai.servicios.TrabajadorCorreoService;
import com.nextdocs.ai.utiles.TokenCorrelacion;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
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
@RequestMapping("/api/v1/canales/correo")
public class CanalCorreoRestController extends ControladorRest<CanalCorreoRestController> {

	private final BuzonCorreoService buzonCorreoService;

	private final TrabajadorCorreoService trabajadorCorreoService;

	public CanalCorreoRestController(BuzonCorreoService buzonCorreoService,
			TrabajadorCorreoService trabajadorCorreoService) {
		this.buzonCorreoService = buzonCorreoService;
		this.trabajadorCorreoService = trabajadorCorreoService;
	}

	@GetMapping("/configuracion")
	public ResponseEntity<Map<String, Object>> configuracion() {
		return new ResponseEntity<>(Map.of("estadosBuzon", listar(EstadoBuzonCorreo.values()), "resultadosMensaje",
				listar(ResultadoMensajeCorreo.values()), "resultadosAdjunto",
				listar(ResultadoAdjuntoCorreo.values()), "plantillasSalida", listar(PlantillaCorreo.values()),
				"prefijoToken", TokenCorrelacion.PREFIJO), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('canales.administrar')")
	@PostMapping("/buzones")
	public ResponseEntity<BuzonCorreoModel> crear(@Valid @RequestBody NuevoBuzonCorreoReqModel datos) {
		return new ResponseEntity<>(buzonCorreoService.crear(tenant(), datos), HttpStatus.CREATED);
	}

	@PreAuthorize("hasAuthority('canales.leer')")
	@GetMapping("/buzones")
	public ResponseEntity<List<BuzonCorreoModel>> listar() {
		return new ResponseEntity<>(buzonCorreoService.listar(tenantId()), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('canales.leer')")
	@GetMapping("/buzones/{buzonId}")
	public ResponseEntity<BuzonCorreoModel> obtener(@PathVariable String buzonId) {
		return new ResponseEntity<>(buzonCorreoService.obtener(tenantId(), buzonId), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('canales.administrar')")
	@PostMapping("/buzones/{buzonId}/estado")
	public ResponseEntity<BuzonCorreoModel> cambiarEstado(@PathVariable String buzonId,
			@RequestParam EstadoBuzonCorreo estado) {
		return new ResponseEntity<>(buzonCorreoService.cambiarEstado(tenantId(), buzonId, estado), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('canales.administrar')")
	@PostMapping("/buzones/{buzonId}/prueba")
	public ResponseEntity<Map<String, String>> probar(@PathVariable String buzonId) {
		buzonCorreoService.probar(tenantId(), buzonId);
		return new ResponseEntity<>(Map.of("estado", "CONECTADO"), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('canales.administrar')")
	@PostMapping("/buzones/{buzonId}/lectura")
	public ResponseEntity<LecturaBuzonModel> leer(@PathVariable String buzonId) {
		buzonCorreoService.buscarEntidad(tenantId(), buzonId);
		return new ResponseEntity<>(trabajadorCorreoService.revisar(buzonId), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('canales.administrar')")
	@DeleteMapping("/buzones/{buzonId}")
	public ResponseEntity<Void> eliminar(@PathVariable String buzonId) {
		buzonCorreoService.eliminar(tenantId(), buzonId);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@PreAuthorize("hasAuthority('canales.administrar')")
	@PostMapping("/buzones/{buzonId}/remitentes")
	public ResponseEntity<RemitenteAutorizadoModel> autorizar(@PathVariable String buzonId,
			@Valid @RequestBody RemitenteAutorizadoModel datos) {
		return new ResponseEntity<>(buzonCorreoService.autorizar(tenant(), buzonId, datos), HttpStatus.CREATED);
	}

	@PreAuthorize("hasAuthority('canales.administrar')")
	@DeleteMapping("/remitentes/{remitenteId}")
	public ResponseEntity<Void> revocar(@PathVariable String remitenteId) {
		buzonCorreoService.revocar(tenantId(), remitenteId);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@PreAuthorize("hasAuthority('canales.administrar')")
	@PostMapping("/correlaciones")
	public ResponseEntity<CorrelacionCorreoModel> crearCorrelacion(
			@Valid @RequestBody NuevaCorrelacionCorreoReqModel datos) {
		return new ResponseEntity<>(buzonCorreoService.crearCorrelacion(tenant(), datos), HttpStatus.CREATED);
	}

	@PreAuthorize("hasAuthority('canales.leer')")
	@GetMapping("/correlaciones")
	public ResponseEntity<Page<CorrelacionCorreoModel>> listarCorrelaciones(
			@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamano) {
		return new ResponseEntity<>(
				buzonCorreoService.listarCorrelaciones(tenantId(), paginado(pagina, tamano, "alta,desc")),
				HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('canales.administrar')")
	@DeleteMapping("/correlaciones/{correlacionId}")
	public ResponseEntity<Void> anularCorrelacion(@PathVariable String correlacionId) {
		buzonCorreoService.anularCorrelacion(tenantId(), correlacionId);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@PreAuthorize("hasAuthority('canales.leer')")
	@GetMapping("/mensajes")
	public ResponseEntity<Page<MensajeCorreoModel>> listarMensajes(
			@RequestParam(required = false) ResultadoMensajeCorreo resultado,
			@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamano) {
		return new ResponseEntity<>(buzonCorreoService.listarMensajes(tenantId(), resultado,
				paginado(pagina, tamano, "alta,desc")), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('canales.leer')")
	@GetMapping("/mensajes/{mensajeId}")
	public ResponseEntity<MensajeCorreoModel> obtenerMensaje(@PathVariable String mensajeId) {
		return new ResponseEntity<>(buzonCorreoService.obtenerMensaje(tenantId(), mensajeId), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('canales.leer')")
	@GetMapping("/salientes")
	public ResponseEntity<Page<MensajeSalienteModel>> listarSalientes(
			@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamano) {
		return new ResponseEntity<>(
				buzonCorreoService.listarSalientes(tenantId(), paginado(pagina, tamano, "alta,desc")),
				HttpStatus.OK);
	}
}
