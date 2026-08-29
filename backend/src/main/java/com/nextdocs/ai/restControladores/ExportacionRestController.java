package com.nextdocs.ai.restControladores;

import java.util.List;
import java.util.Map;

import com.nextdocs.ai.enumeraciones.EstadoLoteExportacion;
import com.nextdocs.ai.enumeraciones.OrdenExportacion;
import com.nextdocs.ai.modelos.LoteExportacionModel;
import com.nextdocs.ai.modelos.NuevoLoteExportacionReqModel;
import com.nextdocs.ai.modelos.UsoAlmacenamientoModel;
import com.nextdocs.ai.servicios.CuotaAlmacenamientoService;
import com.nextdocs.ai.servicios.ExportacionService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
@RequestMapping("/api/v1/exportaciones")
public class ExportacionRestController extends ControladorRest<ExportacionRestController> {

	private final ExportacionService exportacionService;

	private final CuotaAlmacenamientoService cuotaAlmacenamientoService;

	public ExportacionRestController(ExportacionService exportacionService,
			CuotaAlmacenamientoService cuotaAlmacenamientoService) {
		this.exportacionService = exportacionService;
		this.cuotaAlmacenamientoService = cuotaAlmacenamientoService;
	}

	@GetMapping("/configuracion")
	public ResponseEntity<Map<String, Object>> configuracion() {
		return new ResponseEntity<>(Map.of("estados", List.of(EstadoLoteExportacion.values()), "ordenes",
				List.of(OrdenExportacion.values()), "topeDocumentos", ExportacionService.TOPE_DOCUMENTOS,
				"umbralesAlmacenamiento", CuotaAlmacenamientoService.UMBRALES), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('documentos.exportar')")
	@PostMapping
	public ResponseEntity<LoteExportacionModel> solicitar(@Valid @RequestBody NuevoLoteExportacionReqModel datos) {
		return new ResponseEntity<>(exportacionService.solicitar(tenantId(), usuarioObligatorio(), datos),
				HttpStatus.CREATED);
	}

	@PreAuthorize("hasAuthority('documentos.exportar')")
	@GetMapping
	public ResponseEntity<Page<LoteExportacionModel>> listar(
			@RequestParam(required = false) EstadoLoteExportacion estado,
			@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamano) {
		return new ResponseEntity<>(
				exportacionService.listar(tenantId(), estado, PageRequest.of(pagina, Math.min(tamano, 100))),
				HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('documentos.exportar')")
	@GetMapping("/{loteId}")
	public ResponseEntity<LoteExportacionModel> obtener(@PathVariable String loteId) {
		return new ResponseEntity<>(exportacionService.obtener(tenantId(), loteId), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('documentos.exportar')")
	@GetMapping("/{loteId}/descarga")
	public ResponseEntity<Map<String, String>> descargar(@PathVariable String loteId) {
		return new ResponseEntity<>(Map.of("url", exportacionService.urlDescarga(tenantId(), loteId)),
				HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('gobernanza.leer')")
	@GetMapping("/almacenamiento")
	public ResponseEntity<UsoAlmacenamientoModel> almacenamiento() {
		return new ResponseEntity<>(cuotaAlmacenamientoService.medir(tenantId()), HttpStatus.OK);
	}
}
