package com.nextdocs.ai.restControladores;

import java.util.List;
import java.util.Map;

import com.nextdocs.ai.enumeraciones.EstadoLineaWhatsapp;
import com.nextdocs.ai.enumeraciones.PlantillaWhatsapp;
import com.nextdocs.ai.enumeraciones.ResultadoMediaWhatsapp;
import com.nextdocs.ai.enumeraciones.ResultadoMensajeWhatsapp;
import com.nextdocs.ai.modelos.ContactoWhatsappModel;
import com.nextdocs.ai.modelos.CorrelacionWhatsappModel;
import com.nextdocs.ai.modelos.LineaWhatsappModel;
import com.nextdocs.ai.modelos.MensajeWhatsappModel;
import com.nextdocs.ai.modelos.MensajeWhatsappSalienteModel;
import com.nextdocs.ai.modelos.NuevaCorrelacionWhatsappReqModel;
import com.nextdocs.ai.modelos.NuevaLineaWhatsappReqModel;
import com.nextdocs.ai.servicios.LineaWhatsappService;
import com.nextdocs.ai.servicios.WebhookWhatsappService;
import com.nextdocs.ai.utiles.TokenCorrelacion;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/canales/whatsapp")
public class CanalWhatsappRestController extends ControladorRest<CanalWhatsappRestController> {

	private final LineaWhatsappService lineaWhatsappService;

	private final WebhookWhatsappService webhookWhatsappService;

	public CanalWhatsappRestController(LineaWhatsappService lineaWhatsappService,
			WebhookWhatsappService webhookWhatsappService) {
		this.lineaWhatsappService = lineaWhatsappService;
		this.webhookWhatsappService = webhookWhatsappService;
	}

	@GetMapping(value = "/webhook/{ruta}", produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> verificar(@PathVariable String ruta,
			@RequestParam("hub.mode") String modo, @RequestParam("hub.verify_token") String token,
			@RequestParam("hub.challenge") String desafio) {
		return new ResponseEntity<>(webhookWhatsappService.verificarSuscripcion(ruta, modo, token, desafio),
				HttpStatus.OK);
	}

	@PostMapping("/webhook/{ruta}")
	public ResponseEntity<Map<String, Object>> recibir(@PathVariable String ruta,
			@RequestHeader(value = WebhookWhatsappService.CABECERA_FIRMA, required = false) String firma,
			@RequestBody byte[] cuerpo) {
		List<MensajeWhatsappModel> procesados = webhookWhatsappService.recibir(ruta, firma, cuerpo);
		return new ResponseEntity<>(Map.of("recibidos", procesados.size(), "mensajes", procesados), HttpStatus.OK);
	}

	@GetMapping("/configuracion")
	public ResponseEntity<Map<String, Object>> configuracion() {
		return new ResponseEntity<>(Map.of("estadosLinea", listar(EstadoLineaWhatsapp.values()),
				"resultadosMensaje", listar(ResultadoMensajeWhatsapp.values()), "resultadosMedia",
				listar(ResultadoMediaWhatsapp.values()), "plantillasSalida", listar(PlantillaWhatsapp.values()),
				"prefijoToken", TokenCorrelacion.PREFIJO), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('canales.administrar')")
	@PostMapping("/lineas")
	public ResponseEntity<LineaWhatsappModel> crear(@Valid @RequestBody NuevaLineaWhatsappReqModel datos) {
		return new ResponseEntity<>(lineaWhatsappService.crear(tenant(), datos), HttpStatus.CREATED);
	}

	@PreAuthorize("hasAuthority('canales.leer')")
	@GetMapping("/lineas")
	public ResponseEntity<List<LineaWhatsappModel>> listar() {
		return new ResponseEntity<>(lineaWhatsappService.listar(tenantId()), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('canales.leer')")
	@GetMapping("/lineas/{lineaId}")
	public ResponseEntity<LineaWhatsappModel> obtener(@PathVariable String lineaId) {
		return new ResponseEntity<>(lineaWhatsappService.obtener(tenantId(), lineaId), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('canales.administrar')")
	@PostMapping("/lineas/{lineaId}/estado")
	public ResponseEntity<LineaWhatsappModel> cambiarEstado(@PathVariable String lineaId,
			@RequestParam EstadoLineaWhatsapp estado) {
		return new ResponseEntity<>(lineaWhatsappService.cambiarEstado(tenantId(), lineaId, estado), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('canales.administrar')")
	@PostMapping("/lineas/{lineaId}/prueba")
	public ResponseEntity<Map<String, String>> probar(@PathVariable String lineaId) {
		lineaWhatsappService.probar(tenantId(), lineaId);
		return new ResponseEntity<>(Map.of("estado", "CONECTADA"), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('canales.administrar')")
	@DeleteMapping("/lineas/{lineaId}")
	public ResponseEntity<Void> eliminar(@PathVariable String lineaId) {
		lineaWhatsappService.eliminar(tenantId(), lineaId);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@PreAuthorize("hasAuthority('canales.administrar')")
	@PostMapping("/lineas/{lineaId}/contactos")
	public ResponseEntity<ContactoWhatsappModel> autorizar(@PathVariable String lineaId,
			@Valid @RequestBody ContactoWhatsappModel datos) {
		return new ResponseEntity<>(lineaWhatsappService.autorizar(tenant(), lineaId, datos), HttpStatus.CREATED);
	}

	@PreAuthorize("hasAuthority('canales.administrar')")
	@DeleteMapping("/contactos/{contactoId}")
	public ResponseEntity<Void> revocar(@PathVariable String contactoId) {
		lineaWhatsappService.revocar(tenantId(), contactoId);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@PreAuthorize("hasAuthority('canales.administrar')")
	@PostMapping("/correlaciones")
	public ResponseEntity<CorrelacionWhatsappModel> crearCorrelacion(
			@Valid @RequestBody NuevaCorrelacionWhatsappReqModel datos) {
		return new ResponseEntity<>(lineaWhatsappService.crearCorrelacion(tenant(), datos), HttpStatus.CREATED);
	}

	@PreAuthorize("hasAuthority('canales.leer')")
	@GetMapping("/correlaciones")
	public ResponseEntity<Page<CorrelacionWhatsappModel>> listarCorrelaciones(
			@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamano) {
		return new ResponseEntity<>(
				lineaWhatsappService.listarCorrelaciones(tenantId(), paginado(pagina, tamano, "alta,desc")),
				HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('canales.administrar')")
	@DeleteMapping("/correlaciones/{correlacionId}")
	public ResponseEntity<Void> anularCorrelacion(@PathVariable String correlacionId) {
		lineaWhatsappService.anularCorrelacion(tenantId(), correlacionId);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@PreAuthorize("hasAuthority('canales.leer')")
	@GetMapping("/mensajes")
	public ResponseEntity<Page<MensajeWhatsappModel>> listarMensajes(
			@RequestParam(required = false) ResultadoMensajeWhatsapp resultado,
			@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamano) {
		return new ResponseEntity<>(lineaWhatsappService.listarMensajes(tenantId(), resultado,
				paginado(pagina, tamano, "alta,desc")), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('canales.leer')")
	@GetMapping("/mensajes/{mensajeId}")
	public ResponseEntity<MensajeWhatsappModel> obtenerMensaje(@PathVariable String mensajeId) {
		return new ResponseEntity<>(lineaWhatsappService.obtenerMensaje(tenantId(), mensajeId), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('canales.leer')")
	@GetMapping("/salientes")
	public ResponseEntity<Page<MensajeWhatsappSalienteModel>> listarSalientes(
			@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamano) {
		return new ResponseEntity<>(
				lineaWhatsappService.listarSalientes(tenantId(), paginado(pagina, tamano, "alta,desc")),
				HttpStatus.OK);
	}
}
