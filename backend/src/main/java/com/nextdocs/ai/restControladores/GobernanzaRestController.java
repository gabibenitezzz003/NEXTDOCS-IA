package com.nextdocs.ai.restControladores;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.AccionRetencion;
import com.nextdocs.ai.enumeraciones.TipoActor;
import com.nextdocs.ai.modelos.EventoAuditoriaModel;
import com.nextdocs.ai.modelos.FiltroAuditoriaModel;
import com.nextdocs.ai.modelos.PoliticaRetencionModel;
import com.nextdocs.ai.modelos.PoliticaRetencionReqModel;
import com.nextdocs.ai.modelos.TrazabilidadDocumentoModel;
import com.nextdocs.ai.servicios.GobernanzaService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@RequestMapping("/api/v1/gobernanza")
public class GobernanzaRestController extends ControladorRest<GobernanzaRestController> {

	public static final String NOMBRE_EXPORTACION = "auditoria-nextdocs.csv";

	private final GobernanzaService gobernanzaService;

	public GobernanzaRestController(GobernanzaService gobernanzaService) {
		this.gobernanzaService = gobernanzaService;
	}

	@GetMapping("/configuracion")
	public ResponseEntity<Map<String, Object>> configuracion() {
		Map<String, Object> configuracion = new HashMap<>();
		configuracion.put("acciones", listar(AccionAuditoria.values()));
		configuracion.put("tiposActor", listar(TipoActor.values()));
		configuracion.put("accionesRetencion", listar(AccionRetencion.values()));
		return new ResponseEntity<>(configuracion, HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('gobernanza.leer')")
	@GetMapping("/auditoria")
	public ResponseEntity<Page<EventoAuditoriaModel>> auditoria(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
			@RequestParam(required = false) AccionAuditoria accion,
			@RequestParam(required = false) String tipoRecurso, @RequestParam(required = false) String idRecurso,
			@RequestParam(required = false) TipoActor tipoActor, @RequestParam(required = false) String idActor,
			@RequestParam(required = false) String correlacionId, @RequestParam(required = false) Boolean exitoso,
			@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "25") int tamano,
			@RequestParam(required = false) String orden) {
		FiltroAuditoriaModel filtro = filtro(desde, hasta, accion, tipoRecurso, idRecurso, tipoActor, idActor,
				correlacionId, exitoso);
		return new ResponseEntity<>(gobernanzaService.consultar(tenantId(), filtro,
				paginado(pagina, tamano, orden == null ? "fecha,desc" : orden)), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('gobernanza.leer')")
	@GetMapping("/auditoria/resumen")
	public ResponseEntity<Map<String, Object>> resumen(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta) {
		return new ResponseEntity<>(gobernanzaService.resumen(tenantId(), desde, hasta), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('gobernanza.leer')")
	@GetMapping("/auditoria/correlacion/{correlacionId}")
	public ResponseEntity<List<EventoAuditoriaModel>> porCorrelacion(
			@PathVariable("correlacionId") String correlacionId) {
		return new ResponseEntity<>(gobernanzaService.porCorrelacion(tenantId(), correlacionId), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('gobernanza.leer')")
	@GetMapping("/auditoria/recurso/{tipoRecurso}/{idRecurso}")
	public ResponseEntity<List<EventoAuditoriaModel>> porRecurso(@PathVariable("tipoRecurso") String tipoRecurso,
			@PathVariable("idRecurso") String idRecurso) {
		return new ResponseEntity<>(gobernanzaService.porRecurso(tenantId(), tipoRecurso, idRecurso),
				HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('gobernanza.administrar')")
	@GetMapping(value = "/auditoria/exportacion", produces = "text/csv")
	public ResponseEntity<byte[]> exportarCsv(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
			@RequestParam(required = false) AccionAuditoria accion,
			@RequestParam(required = false) String tipoRecurso, @RequestParam(required = false) String idRecurso,
			@RequestParam(required = false) TipoActor tipoActor, @RequestParam(required = false) String idActor,
			@RequestParam(required = false) String correlacionId,
			@RequestParam(required = false) Boolean exitoso) {
		FiltroAuditoriaModel filtro = filtro(desde, hasta, accion, tipoRecurso, idRecurso, tipoActor, idActor,
				correlacionId, exitoso);
		byte[] contenido = gobernanzaService.exportarCsv(tenantId(), filtro).getBytes(StandardCharsets.UTF_8);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + NOMBRE_EXPORTACION + "\"")
				.contentType(MediaType.parseMediaType("text/csv; charset=UTF-8")).body(contenido);
	}

	@PreAuthorize("hasAuthority('gobernanza.administrar')")
	@GetMapping("/auditoria/exportacion/json")
	public ResponseEntity<List<EventoAuditoriaModel>> exportarJson(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
			@RequestParam(required = false) AccionAuditoria accion,
			@RequestParam(required = false) String tipoRecurso, @RequestParam(required = false) String idRecurso,
			@RequestParam(required = false) TipoActor tipoActor, @RequestParam(required = false) String idActor,
			@RequestParam(required = false) String correlacionId,
			@RequestParam(required = false) Boolean exitoso) {
		FiltroAuditoriaModel filtro = filtro(desde, hasta, accion, tipoRecurso, idRecurso, tipoActor, idActor,
				correlacionId, exitoso);
		return new ResponseEntity<>(gobernanzaService.exportarJson(tenantId(), filtro), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('gobernanza.leer')")
	@GetMapping("/documentos/{documentoId}/trazabilidad")
	public ResponseEntity<TrazabilidadDocumentoModel> trazabilidad(
			@PathVariable("documentoId") String documentoId) {
		return new ResponseEntity<>(gobernanzaService.reconstruir(tenantId(), documentoId), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('gobernanza.administrar')")
	@PostMapping("/documentos/{documentoId}/retencion-legal")
	public ResponseEntity<Map<String, Object>> retencionLegal(@PathVariable("documentoId") String documentoId,
			@RequestBody Map<String, String> cuerpo) {
		boolean activa = Boolean.parseBoolean(cuerpo.get("activa"));
		gobernanzaService.cambiarRetencionLegal(tenantId(), documentoId, activa, cuerpo.get("motivo"),
				usuarioObligatorio());
		return new ResponseEntity<>(Map.of("documentoId", documentoId, "retencionLegal", activa), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('gobernanza.leer')")
	@GetMapping("/retencion/politicas")
	public ResponseEntity<List<PoliticaRetencionModel>> politicas() {
		return new ResponseEntity<>(gobernanzaService.listarPoliticas(tenantId()), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('gobernanza.administrar')")
	@PostMapping("/retencion/politicas")
	public ResponseEntity<PoliticaRetencionModel> crearPolitica(
			@Valid @RequestBody PoliticaRetencionReqModel datos) {
		return new ResponseEntity<>(gobernanzaService.crearPolitica(tenant(), datos), HttpStatus.CREATED);
	}

	@PreAuthorize("hasAuthority('gobernanza.administrar')")
	@PutMapping("/retencion/politicas/{politicaId}")
	public ResponseEntity<PoliticaRetencionModel> actualizarPolitica(
			@PathVariable("politicaId") String politicaId, @Valid @RequestBody PoliticaRetencionReqModel datos) {
		return new ResponseEntity<>(gobernanzaService.actualizarPolitica(tenantId(), politicaId, datos),
				HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('gobernanza.administrar')")
	@DeleteMapping("/retencion/politicas/{politicaId}")
	public ResponseEntity<PoliticaRetencionModel> desactivarPolitica(
			@PathVariable("politicaId") String politicaId) {
		return new ResponseEntity<>(gobernanzaService.desactivarPolitica(tenantId(), politicaId), HttpStatus.OK);
	}

	private FiltroAuditoriaModel filtro(Instant desde, Instant hasta, AccionAuditoria accion, String tipoRecurso,
			String idRecurso, TipoActor tipoActor, String idActor, String correlacionId, Boolean exitoso) {
		FiltroAuditoriaModel filtro = new FiltroAuditoriaModel();
		filtro.setDesde(desde);
		filtro.setHasta(hasta);
		filtro.setAccion(accion);
		filtro.setTipoRecurso(tipoRecurso);
		filtro.setIdRecurso(idRecurso);
		filtro.setTipoActor(tipoActor);
		filtro.setIdActor(idActor);
		filtro.setCorrelacionId(correlacionId);
		filtro.setExitoso(exitoso);
		return filtro;
	}
}
