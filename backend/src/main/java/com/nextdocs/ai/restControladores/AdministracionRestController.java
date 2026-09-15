package com.nextdocs.ai.restControladores;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.nextdocs.ai.enumeraciones.EstadoUsuario;
import com.nextdocs.ai.modelos.CambioClaveReqModel;
import com.nextdocs.ai.modelos.CuentaServicioCreadaModel;
import com.nextdocs.ai.modelos.CuentaServicioModel;
import com.nextdocs.ai.modelos.CuentaServicioReqModel;
import com.nextdocs.ai.modelos.IdiomaReqModel;
import com.nextdocs.ai.modelos.RolModel;
import com.nextdocs.ai.modelos.RolReqModel;
import com.nextdocs.ai.modelos.TenantModel;
import com.nextdocs.ai.modelos.TenantReqModel;
import com.nextdocs.ai.modelos.UsuarioModel;
import com.nextdocs.ai.modelos.UsuarioReqModel;
import com.nextdocs.ai.servicios.CuentaServicioService;
import com.nextdocs.ai.servicios.RolService;
import com.nextdocs.ai.entidades.TipoPropuesto;
import com.nextdocs.ai.enumeraciones.EstadoTipoPropuesto;
import com.nextdocs.ai.servicios.SembradorCatalogoService;
import com.nextdocs.ai.servicios.TipoPropuestoService;
import com.nextdocs.ai.servicios.catalogo.CatalogoDocumentalBase;
import com.nextdocs.ai.servicios.TenantService;
import com.nextdocs.ai.servicios.UsuarioService;
import com.nextdocs.ai.utiles.Permiso;

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
@RequestMapping("/api/v1/administracion")
public class AdministracionRestController extends ControladorRest<AdministracionRestController> {

	private final UsuarioService usuarioService;

	private final RolService rolService;

	private final CuentaServicioService cuentaServicioService;

	private final TenantService tenantService;

	private final SembradorCatalogoService sembradorCatalogoService;

	private final TipoPropuestoService tipoPropuestoService;

	public AdministracionRestController(UsuarioService usuarioService, RolService rolService,
			CuentaServicioService cuentaServicioService, TenantService tenantService,
			SembradorCatalogoService sembradorCatalogoService, TipoPropuestoService tipoPropuestoService) {
		this.usuarioService = usuarioService;
		this.rolService = rolService;
		this.cuentaServicioService = cuentaServicioService;
		this.tenantService = tenantService;
		this.sembradorCatalogoService = sembradorCatalogoService;
		this.tipoPropuestoService = tipoPropuestoService;
	}

	@GetMapping("/configuracion")
	public ResponseEntity<Map<String, Object>> configuracion() {
		Map<String, Object> configuracion = new HashMap<>();
		configuracion.put("estadosUsuario", listar(EstadoUsuario.values()));
		configuracion.put("permisos", Permiso.todos());
		configuracion.put("longitudMinimaClave", UsuarioService.LONGITUD_MINIMA_CLAVE);
		return new ResponseEntity<>(configuracion, HttpStatus.OK);
	}

	@GetMapping("/perfil")
	public ResponseEntity<UsuarioModel> perfil() {
		return new ResponseEntity<>(usuarioService.obtener(tenantId(), usuarioObligatorio().getId()),
				HttpStatus.OK);
	}

	@PostMapping("/perfil/clave")
	public ResponseEntity<Void> cambiarClavePropia(@Valid @RequestBody CambioClaveReqModel datos) {
		usuarioService.cambiarClave(tenantId(), usuarioObligatorio(), datos);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@PutMapping("/perfil/idioma")
	public ResponseEntity<UsuarioModel> cambiarIdiomaPropio(@Valid @RequestBody IdiomaReqModel datos) {
		return new ResponseEntity<>(usuarioService.cambiarIdioma(tenantId(), usuarioObligatorio().getId(),
				datos.getIdioma()), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@GetMapping("/tenant")
	public ResponseEntity<TenantModel> tenantActual() {
		return new ResponseEntity<>(tenantService.obtener(tenantId()), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@PutMapping("/tenant")
	public ResponseEntity<TenantModel> actualizarTenant(@Valid @RequestBody TenantReqModel datos) {
		return new ResponseEntity<>(tenantService.actualizar(tenantId(), datos), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@PostMapping("/tenant/catalogo")
	public ResponseEntity<Map<String, Object>> sembrarCatalogo() {
		List<String> creados = sembradorCatalogoService.sembrar(tenant());
		return new ResponseEntity<>(Map.of("catalogo", CatalogoDocumentalBase.VERSION, "creados", creados,
				"yaExistian", CatalogoDocumentalBase.TIPOS.size() - creados.size()), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@GetMapping("/tipos-propuestos")
	public ResponseEntity<List<Map<String, Object>>> listarTiposPropuestos(
			@RequestParam(required = false) EstadoTipoPropuesto estado) {
		List<Map<String, Object>> respuesta = tipoPropuestoService.listar(tenantId(), estado).stream()
				.map(this::aMapa).toList();
		return new ResponseEntity<>(respuesta, HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@PostMapping("/tipos-propuestos/{propuestoId}/aprobar")
	public ResponseEntity<Map<String, Object>> aprobarTipoPropuesto(@PathVariable String propuestoId) {
		return new ResponseEntity<>(aMapa(tipoPropuestoService.aprobar(tenant(), propuestoId)), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@PostMapping("/tipos-propuestos/{propuestoId}/descartar")
	public ResponseEntity<Map<String, Object>> descartarTipoPropuesto(@PathVariable String propuestoId) {
		return new ResponseEntity<>(aMapa(tipoPropuestoService.descartar(tenantId(), propuestoId)),
				HttpStatus.OK);
	}

	private Map<String, Object> aMapa(TipoPropuesto propuesto) {
		Map<String, Object> mapa = new java.util.LinkedHashMap<>();
		mapa.put("id", propuesto.getId());
		mapa.put("codigoSugerido", propuesto.getCodigoSugerido());
		mapa.put("nombreSugerido", propuesto.getNombreSugerido());
		mapa.put("motivo", propuesto.getMotivo());
		mapa.put("veces", propuesto.getVeces());
		mapa.put("estado", propuesto.getEstado().name());
		mapa.put("codigoAprobado", propuesto.getCodigoAprobado());
		mapa.put("campos", tipoPropuestoService.camposDe(propuesto));
		mapa.put("alta", propuesto.getAlta());
		return mapa;
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@GetMapping("/usuarios")
	public ResponseEntity<Page<UsuarioModel>> listarUsuarios(@RequestParam(required = false) EstadoUsuario estado,
			@RequestParam(required = false) String texto, @RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "25") int tamano, @RequestParam(required = false) String orden) {
		return new ResponseEntity<>(usuarioService.listar(tenantId(), estado, texto,
				paginado(pagina, tamano, orden == null ? "email,asc" : orden)), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@GetMapping("/usuarios/{usuarioId}")
	public ResponseEntity<UsuarioModel> obtenerUsuario(@PathVariable("usuarioId") String usuarioId) {
		return new ResponseEntity<>(usuarioService.obtener(tenantId(), usuarioId), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@PostMapping("/usuarios")
	public ResponseEntity<UsuarioModel> crearUsuario(@Valid @RequestBody UsuarioReqModel datos) {
		return new ResponseEntity<>(usuarioService.crear(tenant(), datos), HttpStatus.CREATED);
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@PutMapping("/usuarios/{usuarioId}")
	public ResponseEntity<UsuarioModel> actualizarUsuario(@PathVariable("usuarioId") String usuarioId,
			@Valid @RequestBody UsuarioReqModel datos) {
		return new ResponseEntity<>(usuarioService.actualizar(tenantId(), usuarioId, datos), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@PutMapping("/usuarios/{usuarioId}/roles")
	public ResponseEntity<UsuarioModel> asignarRoles(@PathVariable("usuarioId") String usuarioId,
			@RequestBody Map<String, List<String>> cuerpo) {
		return new ResponseEntity<>(usuarioService.asignarRoles(tenantId(), usuarioId, cuerpo.get("roles")),
				HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@PostMapping("/usuarios/{usuarioId}/bloquear")
	public ResponseEntity<UsuarioModel> bloquearUsuario(@PathVariable("usuarioId") String usuarioId,
			@RequestBody(required = false) Map<String, String> cuerpo) {
		String motivo = cuerpo == null ? null : cuerpo.get("motivo");
		return new ResponseEntity<>(usuarioService.bloquear(tenantId(), usuarioId, usuarioObligatorio(), motivo),
				HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@PostMapping("/usuarios/{usuarioId}/desbloquear")
	public ResponseEntity<UsuarioModel> desbloquearUsuario(@PathVariable("usuarioId") String usuarioId) {
		return new ResponseEntity<>(usuarioService.desbloquear(tenantId(), usuarioId), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@PostMapping("/usuarios/{usuarioId}/clave")
	public ResponseEntity<Void> restablecerClave(@PathVariable("usuarioId") String usuarioId,
			@Valid @RequestBody CambioClaveReqModel datos) {
		usuarioService.restablecerClave(tenantId(), usuarioId, datos.getClaveNueva(), usuarioObligatorio());
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@DeleteMapping("/usuarios/{usuarioId}")
	public ResponseEntity<Void> eliminarUsuario(@PathVariable("usuarioId") String usuarioId) {
		usuarioService.eliminar(tenantId(), usuarioId, usuarioObligatorio());
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@GetMapping("/roles")
	public ResponseEntity<List<RolModel>> listarRoles() {
		return new ResponseEntity<>(rolService.listar(tenantId()), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@GetMapping("/roles/{rolId}")
	public ResponseEntity<RolModel> obtenerRol(@PathVariable("rolId") String rolId) {
		return new ResponseEntity<>(rolService.obtener(tenantId(), rolId), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@PostMapping("/roles")
	public ResponseEntity<RolModel> crearRol(@Valid @RequestBody RolReqModel datos) {
		return new ResponseEntity<>(rolService.crear(tenant(), datos), HttpStatus.CREATED);
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@PutMapping("/roles/{rolId}")
	public ResponseEntity<RolModel> actualizarRol(@PathVariable("rolId") String rolId,
			@Valid @RequestBody RolReqModel datos) {
		return new ResponseEntity<>(rolService.actualizar(tenantId(), rolId, datos), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@DeleteMapping("/roles/{rolId}")
	public ResponseEntity<Void> eliminarRol(@PathVariable("rolId") String rolId) {
		rolService.eliminar(tenantId(), rolId);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@GetMapping("/cuentas-servicio")
	public ResponseEntity<List<CuentaServicioModel>> listarCuentas() {
		return new ResponseEntity<>(cuentaServicioService.listarModelos(tenantId()), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@GetMapping("/cuentas-servicio/{cuentaId}")
	public ResponseEntity<CuentaServicioModel> obtenerCuenta(@PathVariable("cuentaId") String cuentaId) {
		return new ResponseEntity<>(cuentaServicioService.obtener(tenantId(), cuentaId), HttpStatus.OK);
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@PostMapping("/cuentas-servicio")
	public ResponseEntity<CuentaServicioCreadaModel> crearCuenta(
			@Valid @RequestBody CuentaServicioReqModel datos) {
		return new ResponseEntity<>(cuentaServicioService.crear(tenant(), datos), HttpStatus.CREATED);
	}

	@PreAuthorize("hasAuthority('tenant.administrar')")
	@PostMapping("/cuentas-servicio/{cuentaId}/revocar")
	public ResponseEntity<CuentaServicioModel> revocarCuenta(@PathVariable("cuentaId") String cuentaId,
			@RequestBody Map<String, String> cuerpo) {
		return new ResponseEntity<>(cuentaServicioService.revocar(tenantId(), cuentaId, cuerpo.get("motivo")),
				HttpStatus.OK);
	}
}
