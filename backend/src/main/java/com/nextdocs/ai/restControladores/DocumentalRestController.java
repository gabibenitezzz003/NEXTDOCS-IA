package com.nextdocs.ai.restControladores;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import com.nextdocs.ai.servicios.DocumentalProxyService;
import com.nextdocs.ai.utiles.Permiso;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/documental")
public class DocumentalRestController extends ControladorRest<DocumentalRestController> {

	private static final String PREFIJO = "/api/v1/documental";

	private final DocumentalProxyService documentalProxyService;

	public DocumentalRestController(DocumentalProxyService documentalProxyService) {
		this.documentalProxyService = documentalProxyService;
	}

	@GetMapping("/estado")
	public ResponseEntity<Map<String, Object>> estado() {
		return ResponseEntity.ok(Map.of("habilitado", documentalProxyService.habilitado(tenantId())));
	}

	@GetMapping("/**")
	@PreAuthorize("hasAuthority('" + Permiso.DOCUMENTOS_LEER + "')")
	public ResponseEntity<byte[]> leer(HttpServletRequest pedido) throws IOException {
		return reenviar("GET", pedido, null);
	}

	@PostMapping("/**")
	@PreAuthorize("hasAuthority('" + Permiso.DOCUMENTOS_ESCRIBIR + "')")
	public ResponseEntity<byte[]> crear(HttpServletRequest pedido) throws IOException {
		return reenviar("POST", pedido, pedido.getInputStream().readAllBytes());
	}

	@PutMapping("/**")
	@PreAuthorize("hasAuthority('" + Permiso.DOCUMENTOS_ESCRIBIR + "')")
	public ResponseEntity<byte[]> reemplazar(HttpServletRequest pedido) throws IOException {
		return reenviar("PUT", pedido, pedido.getInputStream().readAllBytes());
	}

	@PatchMapping("/**")
	@PreAuthorize("hasAuthority('" + Permiso.DOCUMENTOS_ESCRIBIR + "')")
	public ResponseEntity<byte[]> ajustar(HttpServletRequest pedido) throws IOException {
		return reenviar("PATCH", pedido, pedido.getInputStream().readAllBytes());
	}

	@DeleteMapping("/**")
	@PreAuthorize("hasAuthority('" + Permiso.DOCUMENTOS_ESCRIBIR + "')")
	public ResponseEntity<byte[]> borrar(HttpServletRequest pedido) throws IOException {
		return reenviar("DELETE", pedido, null);
	}

	private ResponseEntity<byte[]> reenviar(String metodo, HttpServletRequest pedido, byte[] cuerpo) {
		String subruta = pedido.getRequestURI().substring(PREFIJO.length());
		HttpResponse<byte[]> respuesta = documentalProxyService.reenviar(tenantId(), metodo, subruta,
				pedido.getQueryString(), cuerpo, pedido.getContentType(),
				pedido.getHeader("Idempotency-Key"));

		HttpHeaders cabeceras = new HttpHeaders();
		String tipo = respuesta.headers().firstValue("Content-Type").orElse(MediaType.APPLICATION_JSON_VALUE);
		cabeceras.setContentType(MediaType.parseMediaType(tipo.split(";")[0].trim()));
		return new ResponseEntity<>(respuesta.body(), cabeceras, HttpStatus.valueOf(respuesta.statusCode()));
	}
}
