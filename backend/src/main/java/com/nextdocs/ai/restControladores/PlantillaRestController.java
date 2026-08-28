package com.nextdocs.ai.restControladores;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.nextdocs.ai.enumeraciones.EstadoPlantilla;
import com.nextdocs.ai.enumeraciones.PoliticaOriginalFisico;
import com.nextdocs.ai.enumeraciones.SensibilidadCampo;
import com.nextdocs.ai.enumeraciones.SeveridadHallazgo;
import com.nextdocs.ai.enumeraciones.TipoDatoCampo;
import com.nextdocs.ai.enumeraciones.TipoReglaValidacion;
import com.nextdocs.ai.modelos.CampoPlantillaModel;
import com.nextdocs.ai.modelos.CampoPlantillaReqModel;
import com.nextdocs.ai.modelos.PlantillaModel;
import com.nextdocs.ai.modelos.PlantillaReqModel;
import com.nextdocs.ai.modelos.ReglaPlantillaModel;
import com.nextdocs.ai.modelos.ReglaPlantillaReqModel;
import com.nextdocs.ai.modelos.VersionPlantillaModel;
import com.nextdocs.ai.modelos.VersionPlantillaReqModel;
import com.nextdocs.ai.servicios.PlantillaService;

import jakarta.validation.Valid;

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
@RequestMapping("/api/v1/plantillas")
public class PlantillaRestController extends ControladorRest<PlantillaRestController> {

	private final PlantillaService plantillaService;

	public PlantillaRestController(PlantillaService plantillaService) {
		this.plantillaService = plantillaService;
	}

	@GetMapping("/configuracion")
	public ResponseEntity<Map<String, Object>> configuracion() {
		Map<String, Object> configuracion = new HashMap<>();
		configuracion.put("estados", listar(EstadoPlantilla.values()));
		configuracion.put("tiposDato", listar(TipoDatoCampo.values()));
		configuracion.put("tiposRegla", listar(TipoReglaValidacion.values()));
		configuracion.put("severidades", listar(SeveridadHallazgo.values()));
		configuracion.put("sensibilidades", listar(SensibilidadCampo.values()));
		configuracion.put("politicasOriginalFisico", listar(PoliticaOriginalFisico.values()));
		return new ResponseEntity<>(configuracion, HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('plantillas.leer')")
	@GetMapping
	public ResponseEntity<List<PlantillaModel>> listar(@RequestParam(required = false) String familia) {
		return new ResponseEntity<>(plantillaService.listar(tenantId(), familia), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('plantillas.leer')")
	@GetMapping("/familias")
	public ResponseEntity<List<String>> familias() {
		return new ResponseEntity<>(plantillaService.listarFamilias(tenantId()), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('plantillas.leer')")
	@GetMapping("/{plantillaId}")
	public ResponseEntity<PlantillaModel> obtener(@PathVariable("plantillaId") String plantillaId) {
		return new ResponseEntity<>(plantillaService.obtener(tenantId(), plantillaId), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('plantillas.escribir')")
	@PostMapping
	public ResponseEntity<PlantillaModel> crear(@Valid @RequestBody PlantillaReqModel datos) {
		return new ResponseEntity<>(plantillaService.crear(tenant(), usuario(), datos), HttpStatus.CREATED);
	}

	@PreAuthorize("hasAuthority('plantillas.escribir')")
	@PutMapping("/{plantillaId}")
	public ResponseEntity<PlantillaModel> actualizar(@PathVariable("plantillaId") String plantillaId,
			@Valid @RequestBody PlantillaReqModel datos) {
		return new ResponseEntity<>(plantillaService.actualizar(tenantId(), plantillaId, datos), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('plantillas.escribir')")
	@DeleteMapping("/{plantillaId}")
	public ResponseEntity<Void> eliminar(@PathVariable("plantillaId") String plantillaId) {
		plantillaService.eliminar(tenantId(), plantillaId);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@PreAuthorize("hasAuthority('plantillas.escribir')")
	@PostMapping("/{plantillaId}/versiones")
	public ResponseEntity<VersionPlantillaModel> crearVersion(@PathVariable("plantillaId") String plantillaId,
			@Valid @RequestBody VersionPlantillaReqModel datos) {
		return new ResponseEntity<>(plantillaService.crearVersion(tenant(), plantillaId, datos),
				HttpStatus.CREATED);
	}

	@PreAuthorize("hasAuthority('plantillas.publicar')")
	@PostMapping("/{plantillaId}/revertir/{versionId}")
	public ResponseEntity<VersionPlantillaModel> revertir(@PathVariable("plantillaId") String plantillaId,
			@PathVariable("versionId") String versionId) {
		return new ResponseEntity<>(
				plantillaService.revertir(tenantId(), plantillaId, versionId, usuarioObligatorio()),
				HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('plantillas.leer')")
	@GetMapping("/versiones/{versionId}")
	public ResponseEntity<VersionPlantillaModel> obtenerVersion(@PathVariable("versionId") String versionId) {
		return new ResponseEntity<>(plantillaService.obtenerVersion(tenantId(), versionId), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('plantillas.escribir')")
	@PutMapping("/versiones/{versionId}")
	public ResponseEntity<VersionPlantillaModel> actualizarVersion(@PathVariable("versionId") String versionId,
			@Valid @RequestBody VersionPlantillaReqModel datos) {
		return new ResponseEntity<>(plantillaService.actualizarVersion(tenantId(), versionId, datos),
				HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('plantillas.escribir')")
	@PostMapping("/versiones/{versionId}/estado/{destino}")
	public ResponseEntity<VersionPlantillaModel> cambiarEstado(@PathVariable("versionId") String versionId,
			@PathVariable("destino") EstadoPlantilla destino) {
		return new ResponseEntity<>(plantillaService.cambiarEstadoVersion(tenantId(), versionId, destino),
				HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('plantillas.leer')")
	@PostMapping("/versiones/{versionId}/validar")
	public ResponseEntity<Map<String, Object>> validar(@PathVariable("versionId") String versionId) {
		List<String> errores = plantillaService.validarVersion(tenantId(), versionId);
		return new ResponseEntity<>(Map.of("valida", errores.isEmpty(), "errores", errores), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('plantillas.publicar')")
	@PostMapping("/versiones/{versionId}/publicar")
	public ResponseEntity<VersionPlantillaModel> publicar(@PathVariable("versionId") String versionId) {
		return new ResponseEntity<>(plantillaService.publicar(tenantId(), versionId, usuarioObligatorio()),
				HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('plantillas.escribir')")
	@PostMapping("/versiones/{versionId}/campos")
	public ResponseEntity<CampoPlantillaModel> agregarCampo(@PathVariable("versionId") String versionId,
			@Valid @RequestBody CampoPlantillaReqModel datos) {
		return new ResponseEntity<>(plantillaService.agregarCampo(tenantId(), versionId, datos),
				HttpStatus.CREATED);
	}

	@PreAuthorize("hasAuthority('plantillas.escribir')")
	@PutMapping("/versiones/{versionId}/campos/{campoId}")
	public ResponseEntity<CampoPlantillaModel> actualizarCampo(@PathVariable("versionId") String versionId,
			@PathVariable("campoId") String campoId, @Valid @RequestBody CampoPlantillaReqModel datos) {
		return new ResponseEntity<>(plantillaService.actualizarCampo(tenantId(), versionId, campoId, datos),
				HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('plantillas.escribir')")
	@DeleteMapping("/versiones/{versionId}/campos/{campoId}")
	public ResponseEntity<Void> eliminarCampo(@PathVariable("versionId") String versionId,
			@PathVariable("campoId") String campoId) {
		plantillaService.eliminarCampo(tenantId(), versionId, campoId);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@PreAuthorize("hasAuthority('plantillas.escribir')")
	@PostMapping("/versiones/{versionId}/reglas")
	public ResponseEntity<ReglaPlantillaModel> agregarRegla(@PathVariable("versionId") String versionId,
			@Valid @RequestBody ReglaPlantillaReqModel datos) {
		return new ResponseEntity<>(plantillaService.agregarRegla(tenantId(), versionId, datos),
				HttpStatus.CREATED);
	}

	@PreAuthorize("hasAuthority('plantillas.escribir')")
	@PutMapping("/versiones/{versionId}/reglas/{reglaId}")
	public ResponseEntity<ReglaPlantillaModel> actualizarRegla(@PathVariable("versionId") String versionId,
			@PathVariable("reglaId") String reglaId, @Valid @RequestBody ReglaPlantillaReqModel datos) {
		return new ResponseEntity<>(plantillaService.actualizarRegla(tenantId(), versionId, reglaId, datos),
				HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('plantillas.escribir')")
	@DeleteMapping("/versiones/{versionId}/reglas/{reglaId}")
	public ResponseEntity<Void> eliminarRegla(@PathVariable("versionId") String versionId,
			@PathVariable("reglaId") String reglaId) {
		plantillaService.eliminarRegla(tenantId(), versionId, reglaId);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
}
