package com.nextdocs.ai.restControladores;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdocs.ai.convertidores.ExcepcionConverter;
import com.nextdocs.ai.enumeraciones.DecisionRevision;
import com.nextdocs.ai.enumeraciones.EstadoDocumento;
import com.nextdocs.ai.enumeraciones.OrigenDocumento;
import com.nextdocs.ai.enumeraciones.PresenciaCampo;
import com.nextdocs.ai.enumeraciones.SeveridadHallazgo;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.CandidatoAsociacionModel;
import com.nextdocs.ai.modelos.DocumentoModel;
import com.nextdocs.ai.modelos.NuevoDocumentoReqModel;
import com.nextdocs.ai.modelos.RevisionDocumentoModel;
import com.nextdocs.ai.modelos.ResultadoAsociacionModel;
import com.nextdocs.ai.modelos.RevisionDocumentoReqModel;
import com.nextdocs.ai.servicios.AsociacionService;
import com.nextdocs.ai.servicios.DocumentoService;
import com.nextdocs.ai.servicios.IngestaDocumentalService;
import com.nextdocs.ai.servicios.RevisionDocumentalService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/documentos")
public class DocumentoRestController extends ControladorRest<DocumentoRestController> {

	public static final String CABECERA_IDEMPOTENCIA = "Idempotency-Key";

	private final IngestaDocumentalService ingestaDocumentalService;

	private final DocumentoService documentoService;

	private final RevisionDocumentalService revisionDocumentalService;

	private final AsociacionService asociacionService;

	private final ExcepcionConverter excepcionConverter;

	private final ObjectMapper objectMapper;

	public DocumentoRestController(IngestaDocumentalService ingestaDocumentalService,
			DocumentoService documentoService, RevisionDocumentalService revisionDocumentalService,
			AsociacionService asociacionService,
			ExcepcionConverter excepcionConverter, ObjectMapper objectMapper) {
		this.ingestaDocumentalService = ingestaDocumentalService;
		this.documentoService = documentoService;
		this.revisionDocumentalService = revisionDocumentalService;
		this.asociacionService = asociacionService;
		this.excepcionConverter = excepcionConverter;
		this.objectMapper = objectMapper;
	}

	@GetMapping("/configuracion")
	public ResponseEntity<Map<String, Object>> configuracion() {
		Map<String, Object> configuracion = new HashMap<>();
		configuracion.put("estados", listar(EstadoDocumento.values()));
		configuracion.put("origenes", listar(OrigenDocumento.values()));
		configuracion.put("presencias", listar(PresenciaCampo.values()));
		configuracion.put("severidades", listar(SeveridadHallazgo.values()));
		configuracion.put("decisionesRevision", listar(DecisionRevision.values()));
		return new ResponseEntity<>(configuracion, HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('documentos.escribir')")
	@PostMapping
	public ResponseEntity<DocumentoModel> ingresar(@RequestPart("archivo") MultipartFile archivo,
			@RequestPart(value = "datos", required = false) String datosJson,
			@RequestHeader(value = CABECERA_IDEMPOTENCIA, required = false) String claveIdempotencia) {
		NuevoDocumentoReqModel datos = leerDatos(datosJson);
		return new ResponseEntity<>(
				ingestaDocumentalService.ingresar(tenant(), usuario(), archivo, datos, claveIdempotencia),
				HttpStatus.CREATED);
	}

	@PreAuthorize("hasAuthority('documentos.leer')")
	@GetMapping
	public ResponseEntity<Page<DocumentoModel>> listar(
			@RequestParam(required = false) List<EstadoDocumento> estados,
			@RequestParam(required = false) OrigenDocumento origen,
			@RequestParam(required = false) String codigoPlantilla,
			@RequestParam(required = false) String texto,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
			@RequestParam(required = false, defaultValue = "true") Boolean soloRaiz,
			@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "25") int tamano,
			@RequestParam(required = false) String orden) {
		return new ResponseEntity<>(documentoService.listar(tenantId(), estados, origen, codigoPlantilla, texto, desde,
				hasta, soloRaiz, paginado(pagina, tamano, orden)), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('documentos.leer')")
	@GetMapping("/{documentoId}")
	public ResponseEntity<DocumentoModel> obtener(@PathVariable("documentoId") String documentoId) {
		return new ResponseEntity<>(documentoService.obtener(tenantId(), documentoId), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('documentos.leer')")
	@GetMapping("/{documentoId}/detalle")
	public ResponseEntity<Map<String, Object>> detalle(@PathVariable("documentoId") String documentoId) {
		return new ResponseEntity<>(documentoService.detalle(tenantId(), documentoId), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('documentos.leer')")
	@GetMapping("/{documentoId}/original")
	public ResponseEntity<Map<String, String>> original(@PathVariable("documentoId") String documentoId) {
		return new ResponseEntity<>(Map.of("url", documentoService.urlOriginal(tenantId(), documentoId)),
				HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('documentos.revisar')")
	@PostMapping("/{documentoId}/revisiones")
	public ResponseEntity<RevisionDocumentoModel> revisar(@PathVariable("documentoId") String documentoId,
			@Valid @RequestBody RevisionDocumentoReqModel datos) {
		return new ResponseEntity<>(
				revisionDocumentalService.registrar(tenantId(), documentoId, usuarioObligatorio(), datos),
				HttpStatus.CREATED);
	}

	@PreAuthorize("hasAuthority('documentos.leer')")
	@GetMapping("/{documentoId}/candidatos")
	public ResponseEntity<List<CandidatoAsociacionModel>> candidatos(
			@PathVariable("documentoId") String documentoId) {
		documentoService.buscarEntidad(tenantId(), documentoId);
		return new ResponseEntity<>(asociacionService.listar(tenantId(), documentoId), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('documentos.revisar')")
	@PostMapping("/{documentoId}/candidatos/{candidatoId}/seleccionar")
	public ResponseEntity<ResultadoAsociacionModel> seleccionarCandidato(
			@PathVariable("documentoId") String documentoId, @PathVariable("candidatoId") String candidatoId,
			@RequestBody(required = false) Map<String, String> cuerpo) {
		String motivo = cuerpo == null ? null : cuerpo.get("motivo");
		return new ResponseEntity<>(asociacionService.seleccionar(tenantId(), documentoId, candidatoId,
				usuarioObligatorio(), motivo), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('documentos.escribir')")
	@PostMapping("/{documentoId}/reprocesar")
	public ResponseEntity<DocumentoModel> reprocesar(@PathVariable("documentoId") String documentoId) {
		return new ResponseEntity<>(documentoService.reprocesar(tenantId(), documentoId), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('documentos.escribir')")
	@PostMapping("/{documentoId}/cerrar")
	public ResponseEntity<DocumentoModel> cerrar(@PathVariable("documentoId") String documentoId) {
		return new ResponseEntity<>(documentoService.cerrar(tenantId(), documentoId), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('documentos.leer')")
	@GetMapping("/resumen")
	public ResponseEntity<Map<String, Object>> resumen() {
		return new ResponseEntity<>(documentoService.resumenPorEstado(tenantId()), HttpStatus.OK);
	}

	private NuevoDocumentoReqModel leerDatos(String datosJson) {
		if (datosJson == null || datosJson.isBlank()) {
			return new NuevoDocumentoReqModel();
		}
		try {
			return objectMapper.readValue(datosJson, NuevoDocumentoReqModel.class);
		} catch (Exception e) {
			throw new ValidacionException("La parte datos no es un JSON valido");
		}
	}
}
