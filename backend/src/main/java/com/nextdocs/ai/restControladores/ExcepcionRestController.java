package com.nextdocs.ai.restControladores;

import java.util.List;
import java.util.Map;

import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.enumeraciones.EstadoExcepcion;
import com.nextdocs.ai.enumeraciones.TipoExcepcion;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.modelos.ExcepcionDocumentalModel;
import com.nextdocs.ai.repositorios.UsuarioRepository;
import com.nextdocs.ai.servicios.ExcepcionDocumentalService;

import org.springframework.data.domain.Page;
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
@RequestMapping("/api/v1/excepciones")
public class ExcepcionRestController extends ControladorRest<ExcepcionRestController> {

	private final ExcepcionDocumentalService excepcionDocumentalService;

	private final UsuarioRepository usuarioRepository;

	public ExcepcionRestController(ExcepcionDocumentalService excepcionDocumentalService,
			UsuarioRepository usuarioRepository) {
		this.excepcionDocumentalService = excepcionDocumentalService;
		this.usuarioRepository = usuarioRepository;
	}

	@GetMapping("/configuracion")
	public ResponseEntity<Map<String, Object>> configuracion() {
		return new ResponseEntity<>(
				Map.of("tipos", listar(TipoExcepcion.values()), "estados", listar(EstadoExcepcion.values())),
				HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('excepciones.leer')")
	@GetMapping
	public ResponseEntity<Page<ExcepcionDocumentalModel>> listar(
			@RequestParam(required = false) EstadoExcepcion estado, @RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "25") int tamano, @RequestParam(required = false) String orden) {
		return new ResponseEntity<>(
				excepcionDocumentalService.listar(tenantId(), estado, paginado(pagina, tamano, orden)),
				HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('excepciones.leer')")
	@GetMapping("/documento/{documentoId}")
	public ResponseEntity<List<ExcepcionDocumentalModel>> porDocumento(
			@PathVariable("documentoId") String documentoId) {
		return new ResponseEntity<>(excepcionDocumentalService.listarPorDocumento(documentoId), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('excepciones.leer')")
	@GetMapping("/{excepcionId}")
	public ResponseEntity<ExcepcionDocumentalModel> obtener(@PathVariable("excepcionId") String excepcionId) {
		return new ResponseEntity<>(excepcionDocumentalService.obtener(tenantId(), excepcionId), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('excepciones.gestionar')")
	@PostMapping("/{excepcionId}/asignar")
	public ResponseEntity<ExcepcionDocumentalModel> asignar(@PathVariable("excepcionId") String excepcionId,
			@RequestBody Map<String, String> cuerpo) {
		Usuario responsable = usuarioRepository.buscarPorIdYTenant(cuerpo.get("responsableId"), tenantId())
				.orElseThrow(() -> EntidadNoEncontradaException.de("Usuario", cuerpo.get("responsableId")));
		return new ResponseEntity<>(excepcionDocumentalService.asignar(tenantId(), excepcionId, responsable),
				HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('excepciones.gestionar')")
	@PostMapping("/{excepcionId}/resolver")
	public ResponseEntity<ExcepcionDocumentalModel> resolver(@PathVariable("excepcionId") String excepcionId,
			@RequestBody Map<String, String> cuerpo) {
		return new ResponseEntity<>(excepcionDocumentalService.resolver(tenantId(), excepcionId,
				usuarioObligatorio(), cuerpo.get("resolucion")), HttpStatus.OK);
	}
}
