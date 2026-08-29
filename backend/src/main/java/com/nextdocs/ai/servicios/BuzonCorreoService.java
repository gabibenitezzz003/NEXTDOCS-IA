package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.nextdocs.ai.config.PropiedadesCorreo;
import com.nextdocs.ai.convertidores.CanalCorreoConverter;
import com.nextdocs.ai.entidades.BuzonCorreo;
import com.nextdocs.ai.entidades.CorrelacionCorreo;
import com.nextdocs.ai.entidades.MensajeCorreoSaliente;
import com.nextdocs.ai.entidades.ReferenciaExterna;
import com.nextdocs.ai.entidades.RemitenteAutorizado;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.EstadoBuzonCorreo;
import com.nextdocs.ai.enumeraciones.PlantillaCorreo;
import com.nextdocs.ai.enumeraciones.ResultadoMensajeCorreo;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.BuzonCorreoModel;
import com.nextdocs.ai.modelos.CorrelacionCorreoModel;
import com.nextdocs.ai.modelos.MensajeCorreoModel;
import com.nextdocs.ai.modelos.MensajeSalienteModel;
import com.nextdocs.ai.modelos.NuevaCorrelacionCorreoReqModel;
import com.nextdocs.ai.modelos.NuevoBuzonCorreoReqModel;
import com.nextdocs.ai.modelos.RemitenteAutorizadoModel;
import com.nextdocs.ai.repositorios.AdjuntoCorreoRepository;
import com.nextdocs.ai.repositorios.BuzonCorreoRepository;
import com.nextdocs.ai.repositorios.CorrelacionCorreoRepository;
import com.nextdocs.ai.repositorios.MensajeCorreoEntranteRepository;
import com.nextdocs.ai.repositorios.MensajeCorreoSalienteRepository;
import com.nextdocs.ai.repositorios.PlantillaDocumentalRepository;
import com.nextdocs.ai.repositorios.RemitenteAutorizadoRepository;
import com.nextdocs.ai.utiles.DireccionCorreo;
import com.nextdocs.ai.utiles.ResolvedorSecreto;
import com.nextdocs.ai.utiles.TokenCorrelacion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BuzonCorreoService {

	public static final String ENTIDAD = "BuzonCorreo";

	public static final String ENTIDAD_CORRELACION = "CorrelacionCorreo";

	private final BuzonCorreoRepository buzonCorreoRepository;

	private final RemitenteAutorizadoRepository remitenteAutorizadoRepository;

	private final CorrelacionCorreoRepository correlacionCorreoRepository;

	private final MensajeCorreoEntranteRepository mensajeCorreoEntranteRepository;

	private final MensajeCorreoSalienteRepository mensajeCorreoSalienteRepository;

	private final AdjuntoCorreoRepository adjuntoCorreoRepository;

	private final PlantillaDocumentalRepository plantillaDocumentalRepository;

	private final LectorCorreoService lectorCorreoService;

	private final CorreoSalienteService correoSalienteService;

	private final AuditoriaService auditoriaService;

	private final CanalCorreoConverter canalCorreoConverter;

	private final PropiedadesCorreo propiedades;

	public BuzonCorreoService(BuzonCorreoRepository buzonCorreoRepository,
			RemitenteAutorizadoRepository remitenteAutorizadoRepository,
			CorrelacionCorreoRepository correlacionCorreoRepository,
			MensajeCorreoEntranteRepository mensajeCorreoEntranteRepository,
			MensajeCorreoSalienteRepository mensajeCorreoSalienteRepository,
			AdjuntoCorreoRepository adjuntoCorreoRepository,
			PlantillaDocumentalRepository plantillaDocumentalRepository, LectorCorreoService lectorCorreoService,
			CorreoSalienteService correoSalienteService, AuditoriaService auditoriaService,
			CanalCorreoConverter canalCorreoConverter, PropiedadesCorreo propiedades) {
		this.buzonCorreoRepository = buzonCorreoRepository;
		this.remitenteAutorizadoRepository = remitenteAutorizadoRepository;
		this.correlacionCorreoRepository = correlacionCorreoRepository;
		this.mensajeCorreoEntranteRepository = mensajeCorreoEntranteRepository;
		this.mensajeCorreoSalienteRepository = mensajeCorreoSalienteRepository;
		this.adjuntoCorreoRepository = adjuntoCorreoRepository;
		this.plantillaDocumentalRepository = plantillaDocumentalRepository;
		this.lectorCorreoService = lectorCorreoService;
		this.correoSalienteService = correoSalienteService;
		this.auditoriaService = auditoriaService;
		this.canalCorreoConverter = canalCorreoConverter;
		this.propiedades = propiedades;
	}

	@Transactional
	public BuzonCorreoModel crear(Tenant tenant, NuevoBuzonCorreoReqModel datos) {
		String direccion = DireccionCorreo.normalizar(datos.getDireccion());
		if (direccion == null) {
			throw new ValidacionException("La direccion del buzon no es valida");
		}
		buzonCorreoRepository.buscarPorDireccion(direccion).ifPresent(existente -> {
			throw new ValidacionException("La direccion " + direccion + " ya esta asignada a un buzon");
		});
		exigirSecreto(datos.getReferenciaSecretoEntrada(), "entrada");
		if (datos.getHostSalida() != null && !datos.getHostSalida().isBlank()
				&& datos.getReferenciaSecretoSalida() != null && !datos.getReferenciaSecretoSalida().isBlank()) {
			exigirSecreto(datos.getReferenciaSecretoSalida(), "salida");
		}
		exigirPlantilla(tenant, datos.getCodigoPlantillaPorDefecto());

		BuzonCorreo buzon = new BuzonCorreo();
		buzon.setTenant(tenant);
		buzon.setDireccion(direccion);
		buzon.setNombre(datos.getNombre());
		buzon.setEstado(EstadoBuzonCorreo.ACTIVO);
		buzon.setHostEntrada(datos.getHostEntrada());
		buzon.setPuertoEntrada(datos.getPuertoEntrada());
		buzon.setUsuarioEntrada(datos.getUsuarioEntrada());
		buzon.setReferenciaSecretoEntrada(datos.getReferenciaSecretoEntrada());
		buzon.setCarpeta(datos.getCarpeta() == null || datos.getCarpeta().isBlank() ? "INBOX" : datos.getCarpeta());
		buzon.setEntradaSegura(datos.isEntradaSegura());
		buzon.setHostSalida(datos.getHostSalida());
		buzon.setPuertoSalida(datos.getPuertoSalida());
		buzon.setUsuarioSalida(datos.getUsuarioSalida());
		buzon.setReferenciaSecretoSalida(datos.getReferenciaSecretoSalida());
		buzon.setSalidaSegura(datos.isSalidaSegura());
		buzon.setCodigoPlantillaPorDefecto(datos.getCodigoPlantillaPorDefecto());
		buzon.setExigirRemitenteAutorizado(datos.isExigirRemitenteAutorizado());
		buzon.setExigirCorrelacion(datos.isExigirCorrelacion());
		buzon.setAcusarRecibo(datos.isAcusarRecibo());
		buzon.setMaximoAdjuntosPorMensaje(
				datos.getMaximoAdjuntosPorMensaje() <= 0 ? 10 : datos.getMaximoAdjuntosPorMensaje());
		buzon.setAlta(Instant.now());
		buzonCorreoRepository.save(buzon);

		auditoriaService.registrarConDetalle(tenant.getId(), AccionAuditoria.BUZON_CONFIGURADO, ENTIDAD,
				buzon.getId(), Map.of("direccion", direccion, "host", buzon.getHostEntrada(), "exigeAllowlist",
						buzon.isExigirRemitenteAutorizado(), "exigeCorrelacion", buzon.isExigirCorrelacion()));
		return canalCorreoConverter.aModelo(buzon, List.of());
	}

	@Transactional(readOnly = true)
	public List<BuzonCorreoModel> listar(String tenantId) {
		return buzonCorreoRepository.listarPorTenant(tenantId).stream().map(buzon -> canalCorreoConverter
				.aModelo(buzon, remitenteAutorizadoRepository.listarPorBuzon(buzon.getId()))).toList();
	}

	@Transactional(readOnly = true)
	public BuzonCorreoModel obtener(String tenantId, String buzonId) {
		BuzonCorreo buzon = buscarEntidad(tenantId, buzonId);
		return canalCorreoConverter.aModelo(buzon, remitenteAutorizadoRepository.listarPorBuzon(buzon.getId()));
	}

	@Transactional(readOnly = true)
	public BuzonCorreo buscarEntidad(String tenantId, String buzonId) {
		return buzonCorreoRepository.buscarPorIdYTenant(buzonId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(ENTIDAD, buzonId));
	}

	@Transactional
	public BuzonCorreoModel cambiarEstado(String tenantId, String buzonId, EstadoBuzonCorreo estado) {
		BuzonCorreo buzon = buscarEntidad(tenantId, buzonId);
		buzon.setEstado(estado);
		if (estado == EstadoBuzonCorreo.ACTIVO) {
			buzon.setFallosConsecutivos(0);
			buzon.setUltimoError(null);
		}
		buzonCorreoRepository.save(buzon);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.BUZON_CONFIGURADO, ENTIDAD, buzonId,
				Map.of("direccion", buzon.getDireccion(), "estado", estado.name()));
		return canalCorreoConverter.aModelo(buzon, remitenteAutorizadoRepository.listarPorBuzon(buzonId));
	}

	@Transactional
	public void eliminar(String tenantId, String buzonId) {
		BuzonCorreo buzon = buscarEntidad(tenantId, buzonId);
		buzon.setBaja(Instant.now());
		buzon.setEstado(EstadoBuzonCorreo.PAUSADO);
		buzonCorreoRepository.save(buzon);
		auditoriaService.registrar(tenantId, AccionAuditoria.BUZON_ELIMINADO, ENTIDAD, buzonId);
	}

	@Transactional
	public void probar(String tenantId, String buzonId) {
		lectorCorreoService.verificar(buscarEntidad(tenantId, buzonId));
	}

	@Transactional
	public RemitenteAutorizadoModel autorizar(Tenant tenant, String buzonId, RemitenteAutorizadoModel datos) {
		BuzonCorreo buzon = buscarEntidad(tenant.getId(), buzonId);
		String patron = datos.getPatron() == null ? null : datos.getPatron().trim().toLowerCase();
		if (patron == null || patron.isBlank() || (!patron.startsWith("@") && !patron.contains("@"))) {
			throw new ValidacionException(
					"El patron debe ser una direccion completa o un dominio con la forma @dominio.com");
		}
		remitenteAutorizadoRepository.buscarPorPatron(buzonId, patron).ifPresent(existente -> {
			throw new ValidacionException("El patron " + patron + " ya esta autorizado en este buzon");
		});
		RemitenteAutorizado remitente = new RemitenteAutorizado();
		remitente.setTenant(tenant);
		remitente.setBuzon(buzon);
		remitente.setPatron(patron);
		remitente.setDescripcion(datos.getDescripcion());
		remitente.setAlta(Instant.now());
		remitenteAutorizadoRepository.save(remitente);
		auditoriaService.registrarConDetalle(tenant.getId(), AccionAuditoria.REMITENTE_AUTORIZADO, ENTIDAD, buzonId,
				Map.of("patron", patron, "buzon", buzon.getDireccion()));
		return canalCorreoConverter.aModelo(remitente);
	}

	@Transactional
	public void revocar(String tenantId, String remitenteId) {
		RemitenteAutorizado remitente = remitenteAutorizadoRepository.buscarPorIdYTenant(remitenteId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de("RemitenteAutorizado", remitenteId));
		remitente.setBaja(Instant.now());
		remitenteAutorizadoRepository.save(remitente);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.REMITENTE_REVOCADO, ENTIDAD,
				remitente.getBuzon().getId(), Map.of("patron", remitente.getPatron()));
	}

	@Transactional
	public CorrelacionCorreoModel crearCorrelacion(Tenant tenant, NuevaCorrelacionCorreoReqModel datos) {
		BuzonCorreo buzon = buscarEntidad(tenant.getId(), datos.getBuzonId());
		exigirPlantilla(tenant, datos.getCodigoPlantilla());
		if (datos.isEnviarSolicitud()
				&& (datos.getDestinatario() == null || datos.getDestinatario().isBlank())) {
			throw new ValidacionException("Para enviar la solicitud hace falta un destinatario");
		}

		ReferenciaExterna sujeto = new ReferenciaExterna();
		sujeto.setOrigen(datos.getSujetoOrigen());
		sujeto.setTipoObjeto(datos.getSujetoTipoObjeto());
		sujeto.setIdObjeto(datos.getSujetoIdObjeto());

		CorrelacionCorreo correlacion = new CorrelacionCorreo();
		correlacion.setTenant(tenant);
		correlacion.setBuzon(buzon);
		correlacion.setToken(TokenCorrelacion.generar());
		correlacion.setSujeto(sujeto);
		correlacion.setCodigoPlantilla(datos.getCodigoPlantilla());
		correlacion.setDestinatario(DireccionCorreo.normalizar(datos.getDestinatario()));
		correlacion.setDescripcion(datos.getDescripcion());
		correlacion.setVenceEn(Instant.now().plus(
				datos.getDiasVigencia() <= 0 ? propiedades.getDiasVigenciaCorrelacion() : datos.getDiasVigencia(),
				ChronoUnit.DAYS));
		correlacion.setAlta(Instant.now());
		correlacionCorreoRepository.save(correlacion);

		Map<String, Object> detalle = new LinkedHashMap<>();
		detalle.put("token", correlacion.getToken());
		detalle.put("buzon", buzon.getDireccion());
		detalle.put("sujeto", sujeto.getTipoObjeto() + ":" + sujeto.getIdObjeto());
		detalle.put("venceEn", correlacion.getVenceEn());
		auditoriaService.registrarConDetalle(tenant.getId(), AccionAuditoria.CORRELACION_CREADA, ENTIDAD_CORRELACION,
				correlacion.getId(), detalle);

		CorrelacionCorreoModel modelo = canalCorreoConverter.aModelo(correlacion);
		if (datos.isEnviarSolicitud()) {
			MensajeCorreoSaliente enviado = correoSalienteService.enviar(buzon, correlacion,
					PlantillaCorreo.SOLICITUD_DOCUMENTACION, correlacion.getDestinatario(),
					asuntoDeSolicitud(correlacion), cuerpoDeSolicitud(buzon, correlacion));
			modelo.setMensajeSalienteId(enviado.getId());
		}
		return modelo;
	}

	@Transactional
	public void anularCorrelacion(String tenantId, String correlacionId) {
		CorrelacionCorreo correlacion = correlacionCorreoRepository.buscarPorIdYTenant(correlacionId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(ENTIDAD_CORRELACION, correlacionId));
		correlacion.setBaja(Instant.now());
		correlacionCorreoRepository.save(correlacion);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.CORRELACION_ANULADA, ENTIDAD_CORRELACION,
				correlacionId, Map.of("token", correlacion.getToken()));
	}

	@Transactional(readOnly = true)
	public Page<CorrelacionCorreoModel> listarCorrelaciones(String tenantId, Pageable paginado) {
		return correlacionCorreoRepository.listarPorTenant(tenantId, paginado).map(canalCorreoConverter::aModelo);
	}

	@Transactional(readOnly = true)
	public Page<MensajeCorreoModel> listarMensajes(String tenantId, ResultadoMensajeCorreo resultado,
			Pageable paginado) {
		return mensajeCorreoEntranteRepository.listarPorTenant(tenantId, resultado, paginado)
				.map(mensaje -> canalCorreoConverter.aModelo(mensaje, null));
	}

	@Transactional(readOnly = true)
	public MensajeCorreoModel obtenerMensaje(String tenantId, String mensajeId) {
		return mensajeCorreoEntranteRepository.buscarPorIdYTenant(mensajeId, tenantId)
				.map(mensaje -> canalCorreoConverter.aModelo(mensaje,
						adjuntoCorreoRepository.listarPorMensaje(mensaje.getId())))
				.orElseThrow(() -> EntidadNoEncontradaException.de(IngestaCorreoService.ENTIDAD, mensajeId));
	}

	@Transactional(readOnly = true)
	public Page<MensajeSalienteModel> listarSalientes(String tenantId, Pageable paginado) {
		return mensajeCorreoSalienteRepository.listarPorTenant(tenantId, paginado).map(canalCorreoConverter::aModelo);
	}

	private void exigirSecreto(String referencia, String sentido) {
		if (ResolvedorSecreto.resolver(referencia).isEmpty()) {
			throw new ValidacionException("No se pudo resolver el secreto de " + sentido + " con la referencia "
					+ referencia + ". Cargalo como variable de entorno antes de crear el buzon");
		}
	}

	private void exigirPlantilla(Tenant tenant, String codigoPlantilla) {
		if (codigoPlantilla == null || codigoPlantilla.isBlank()) {
			return;
		}
		plantillaDocumentalRepository.buscarPorCodigo(tenant.getId(), codigoPlantilla.trim())
				.orElseThrow(() -> new ValidacionException(
						"No existe la plantilla " + codigoPlantilla + " en el tenant"));
	}

	private String asuntoDeSolicitud(CorrelacionCorreo correlacion) {
		String descripcion = correlacion.getDescripcion() == null || correlacion.getDescripcion().isBlank()
				? "Documentacion requerida" : correlacion.getDescripcion();
		return descripcion + " " + TokenCorrelacion.etiquetar(correlacion.getToken());
	}

	private String cuerpoDeSolicitud(BuzonCorreo buzon, CorrelacionCorreo correlacion) {
		StringBuilder cuerpo = new StringBuilder();
		cuerpo.append("Necesitamos la documentacion del caso ")
				.append(correlacion.getSujeto().getTipoObjeto()).append(' ')
				.append(correlacion.getSujeto().getIdObjeto()).append(".\n\n");
		cuerpo.append("Responde este correo adjuntando los archivos, sin borrar el codigo ")
				.append(TokenCorrelacion.etiquetar(correlacion.getToken())).append(" del asunto.\n");
		cuerpo.append("Tambien podes enviarlos a ").append(canalCorreoConverter.aModelo(correlacion)
				.getDireccionConEtiqueta()).append(".\n\n");
		cuerpo.append("Sin ese codigo no podemos asociar los documentos al caso y quedan para revision manual.\n");
		cuerpo.append("El pedido vence el ").append(correlacion.getVenceEn()).append(".\n");
		cuerpo.append("\nBuzon de recepcion: ").append(buzon.getDireccion()).append('\n');
		return cuerpo.toString();
	}
}
