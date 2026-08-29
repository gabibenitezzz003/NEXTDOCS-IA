package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.nextdocs.ai.config.PropiedadesWhatsapp;
import com.nextdocs.ai.convertidores.CanalWhatsappConverter;
import com.nextdocs.ai.entidades.ContactoWhatsappAutorizado;
import com.nextdocs.ai.entidades.CorrelacionWhatsapp;
import com.nextdocs.ai.entidades.LineaWhatsapp;
import com.nextdocs.ai.entidades.MensajeWhatsappSaliente;
import com.nextdocs.ai.entidades.ReferenciaExterna;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.EstadoLineaWhatsapp;
import com.nextdocs.ai.enumeraciones.PlantillaWhatsapp;
import com.nextdocs.ai.enumeraciones.ResultadoMensajeWhatsapp;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.ContactoWhatsappModel;
import com.nextdocs.ai.modelos.CorrelacionWhatsappModel;
import com.nextdocs.ai.modelos.LineaWhatsappModel;
import com.nextdocs.ai.modelos.MensajeWhatsappModel;
import com.nextdocs.ai.modelos.MensajeWhatsappSalienteModel;
import com.nextdocs.ai.modelos.NuevaCorrelacionWhatsappReqModel;
import com.nextdocs.ai.modelos.NuevaLineaWhatsappReqModel;
import com.nextdocs.ai.repositorios.ContactoWhatsappAutorizadoRepository;
import com.nextdocs.ai.repositorios.CorrelacionWhatsappRepository;
import com.nextdocs.ai.repositorios.LineaWhatsappRepository;
import com.nextdocs.ai.repositorios.MediaWhatsappRepository;
import com.nextdocs.ai.repositorios.MensajeWhatsappEntranteRepository;
import com.nextdocs.ai.repositorios.MensajeWhatsappSalienteRepository;
import com.nextdocs.ai.repositorios.PlantillaDocumentalRepository;
import com.nextdocs.ai.utiles.NumeroTelefono;
import com.nextdocs.ai.utiles.ResolvedorSecreto;
import com.nextdocs.ai.utiles.TokenCorrelacion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LineaWhatsappService {

	public static final String ENTIDAD = "LineaWhatsapp";

	public static final String ENTIDAD_CORRELACION = "CorrelacionWhatsapp";

	private final LineaWhatsappRepository lineaWhatsappRepository;

	private final ContactoWhatsappAutorizadoRepository contactoWhatsappAutorizadoRepository;

	private final CorrelacionWhatsappRepository correlacionWhatsappRepository;

	private final MensajeWhatsappEntranteRepository mensajeWhatsappEntranteRepository;

	private final MensajeWhatsappSalienteRepository mensajeWhatsappSalienteRepository;

	private final MediaWhatsappRepository mediaWhatsappRepository;

	private final PlantillaDocumentalRepository plantillaDocumentalRepository;

	private final ClienteWhatsappService clienteWhatsappService;

	private final WhatsappSalienteService whatsappSalienteService;

	private final AuditoriaService auditoriaService;

	private final CanalWhatsappConverter canalWhatsappConverter;

	private final PropiedadesWhatsapp propiedades;

	public LineaWhatsappService(LineaWhatsappRepository lineaWhatsappRepository,
			ContactoWhatsappAutorizadoRepository contactoWhatsappAutorizadoRepository,
			CorrelacionWhatsappRepository correlacionWhatsappRepository,
			MensajeWhatsappEntranteRepository mensajeWhatsappEntranteRepository,
			MensajeWhatsappSalienteRepository mensajeWhatsappSalienteRepository,
			MediaWhatsappRepository mediaWhatsappRepository,
			PlantillaDocumentalRepository plantillaDocumentalRepository,
			ClienteWhatsappService clienteWhatsappService, WhatsappSalienteService whatsappSalienteService,
			AuditoriaService auditoriaService, CanalWhatsappConverter canalWhatsappConverter,
			PropiedadesWhatsapp propiedades) {
		this.lineaWhatsappRepository = lineaWhatsappRepository;
		this.contactoWhatsappAutorizadoRepository = contactoWhatsappAutorizadoRepository;
		this.correlacionWhatsappRepository = correlacionWhatsappRepository;
		this.mensajeWhatsappEntranteRepository = mensajeWhatsappEntranteRepository;
		this.mensajeWhatsappSalienteRepository = mensajeWhatsappSalienteRepository;
		this.mediaWhatsappRepository = mediaWhatsappRepository;
		this.plantillaDocumentalRepository = plantillaDocumentalRepository;
		this.clienteWhatsappService = clienteWhatsappService;
		this.whatsappSalienteService = whatsappSalienteService;
		this.auditoriaService = auditoriaService;
		this.canalWhatsappConverter = canalWhatsappConverter;
		this.propiedades = propiedades;
	}

	@Transactional
	public LineaWhatsappModel crear(Tenant tenant, NuevaLineaWhatsappReqModel datos) {
		String numero = NumeroTelefono.normalizarDeclarado(datos.getNumeroTelefono());
		if (numero == null) {
			throw new ValidacionException("El numero " + datos.getNumeroTelefono()
					+ " no tiene formato E.164: tiene que arrancar con + y el codigo de pais");
		}
		String identificadorNumero = datos.getIdentificadorNumero().trim();
		lineaWhatsappRepository.buscarPorIdentificadorNumero(identificadorNumero).ifPresent(existente -> {
			throw new ValidacionException(
					"El identificador de numero " + identificadorNumero + " ya esta asignado a otra linea");
		});
		exigirSecreto(datos.getReferenciaTokenAcceso(), "token de acceso");
		exigirSecreto(datos.getReferenciaSecretoAplicacion(), "secreto de aplicacion");
		exigirSecreto(datos.getReferenciaTokenVerificacion(), "token de verificacion");
		exigirPlantilla(tenant, datos.getCodigoPlantillaPorDefecto());

		LineaWhatsapp linea = new LineaWhatsapp();
		linea.setTenant(tenant);
		linea.setNombre(datos.getNombre());
		linea.setNumeroTelefono(numero);
		linea.setIdentificadorNumero(identificadorNumero);
		linea.setIdentificadorCuenta(datos.getIdentificadorCuenta());
		linea.setRutaWebhook(UUID.randomUUID().toString().replace("-", ""));
		linea.setReferenciaTokenAcceso(datos.getReferenciaTokenAcceso());
		linea.setReferenciaSecretoAplicacion(datos.getReferenciaSecretoAplicacion());
		linea.setReferenciaTokenVerificacion(datos.getReferenciaTokenVerificacion());
		linea.setEstado(EstadoLineaWhatsapp.ACTIVO);
		linea.setCodigoPlantillaPorDefecto(datos.getCodigoPlantillaPorDefecto());
		linea.setExigirContactoAutorizado(datos.isExigirContactoAutorizado());
		linea.setExigirCorrelacion(datos.isExigirCorrelacion());
		linea.setAcusarRecibo(datos.isAcusarRecibo());
		linea.setMaximoMediaPorMensaje(datos.getMaximoMediaPorMensaje() <= 0 ? 10 : datos.getMaximoMediaPorMensaje());
		linea.setMinutosVentanaCorrelacion(datos.getMinutosVentanaCorrelacion() <= 0
				? propiedades.getMinutosVentanaCorrelacion() : datos.getMinutosVentanaCorrelacion());
		linea.setNombrePlantillaSolicitud(datos.getNombrePlantillaSolicitud());
		linea.setIdiomaPlantillaSolicitud(datos.getIdiomaPlantillaSolicitud());
		linea.setAlta(Instant.now());
		lineaWhatsappRepository.save(linea);

		Map<String, Object> detalle = new LinkedHashMap<>();
		detalle.put("numero", numero);
		detalle.put("identificadorNumero", identificadorNumero);
		detalle.put("exigeAllowlist", linea.isExigirContactoAutorizado());
		detalle.put("exigeCorrelacion", linea.isExigirCorrelacion());
		auditoriaService.registrarConDetalle(tenant.getId(), AccionAuditoria.LINEA_WHATSAPP_CONFIGURADA, ENTIDAD,
				linea.getId(), detalle);
		return canalWhatsappConverter.aModelo(linea, List.of());
	}

	@Transactional(readOnly = true)
	public List<LineaWhatsappModel> listar(String tenantId) {
		return lineaWhatsappRepository.listarPorTenant(tenantId).stream().map(linea -> canalWhatsappConverter
				.aModelo(linea, contactoWhatsappAutorizadoRepository.listarPorLinea(linea.getId()))).toList();
	}

	@Transactional(readOnly = true)
	public LineaWhatsappModel obtener(String tenantId, String lineaId) {
		LineaWhatsapp linea = buscarEntidad(tenantId, lineaId);
		return canalWhatsappConverter.aModelo(linea,
				contactoWhatsappAutorizadoRepository.listarPorLinea(linea.getId()));
	}

	@Transactional(readOnly = true)
	public LineaWhatsapp buscarEntidad(String tenantId, String lineaId) {
		return lineaWhatsappRepository.buscarPorIdYTenant(lineaId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(ENTIDAD, lineaId));
	}

	@Transactional
	public LineaWhatsappModel cambiarEstado(String tenantId, String lineaId, EstadoLineaWhatsapp estado) {
		LineaWhatsapp linea = buscarEntidad(tenantId, lineaId);
		linea.setEstado(estado);
		lineaWhatsappRepository.save(linea);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.LINEA_WHATSAPP_CONFIGURADA, ENTIDAD, lineaId,
				Map.of("numero", linea.getNumeroTelefono(), "estado", estado.name()));
		return canalWhatsappConverter.aModelo(linea, contactoWhatsappAutorizadoRepository.listarPorLinea(lineaId));
	}

	@Transactional
	public void eliminar(String tenantId, String lineaId) {
		LineaWhatsapp linea = buscarEntidad(tenantId, lineaId);
		linea.setBaja(Instant.now());
		linea.setEstado(EstadoLineaWhatsapp.PAUSADO);
		lineaWhatsappRepository.save(linea);
		auditoriaService.registrar(tenantId, AccionAuditoria.LINEA_WHATSAPP_ELIMINADA, ENTIDAD, lineaId);
	}

	@Transactional
	public void probar(String tenantId, String lineaId) {
		LineaWhatsapp linea = buscarEntidad(tenantId, lineaId);
		try {
			clienteWhatsappService.verificar(linea);
		}
		catch (RuntimeException e) {
			registrarFallo(linea, e.getMessage());
			throw e;
		}
		registrarLineaSana(linea);
	}

	@Transactional
	public void registrarFallo(LineaWhatsapp linea, String motivo) {
		linea.setUltimoError(motivo);
		linea.setFallosConsecutivos(linea.getFallosConsecutivos() + 1);
		if (linea.getFallosConsecutivos() >= propiedades.getFallosParaPausar()
				&& linea.getEstado() == EstadoLineaWhatsapp.ACTIVO) {
			linea.setEstado(EstadoLineaWhatsapp.ERROR);
			auditoriaService.registrarFallo(linea.getTenant().getId(),
					AccionAuditoria.LINEA_WHATSAPP_CONFIGURADA, ENTIDAD, linea.getId(),
					Map.of("numero", linea.getNumeroTelefono(), "estado", EstadoLineaWhatsapp.ERROR.name(),
							"fallosConsecutivos", linea.getFallosConsecutivos(), "motivo", motivo));
		}
		lineaWhatsappRepository.save(linea);
	}

	@Transactional
	public void registrarLineaSana(LineaWhatsapp linea) {
		if (linea.getFallosConsecutivos() == 0 && linea.getUltimoError() == null) {
			return;
		}
		linea.setFallosConsecutivos(0);
		linea.setUltimoError(null);
		lineaWhatsappRepository.save(linea);
	}

	@Transactional
	public ContactoWhatsappModel autorizar(Tenant tenant, String lineaId, ContactoWhatsappModel datos) {
		LineaWhatsapp linea = buscarEntidad(tenant.getId(), lineaId);
		String patron = NumeroTelefono.normalizarPatron(datos.getPatron());
		if (patron == null) {
			throw new ValidacionException("El patron debe ser un numero E.164 completo como +5491122334455"
					+ " o un prefijo terminado en asterisco como +54911*");
		}
		contactoWhatsappAutorizadoRepository.buscarPorPatron(lineaId, patron).ifPresent(existente -> {
			throw new ValidacionException("El patron " + patron + " ya esta autorizado en esta linea");
		});
		ContactoWhatsappAutorizado contacto = new ContactoWhatsappAutorizado();
		contacto.setTenant(tenant);
		contacto.setLinea(linea);
		contacto.setPatron(patron);
		contacto.setDescripcion(datos.getDescripcion());
		contacto.setAlta(Instant.now());
		contactoWhatsappAutorizadoRepository.save(contacto);
		auditoriaService.registrarConDetalle(tenant.getId(), AccionAuditoria.CONTACTO_WHATSAPP_AUTORIZADO, ENTIDAD,
				lineaId, Map.of("patron", patron, "linea", linea.getNumeroTelefono()));
		return canalWhatsappConverter.aModelo(contacto);
	}

	@Transactional
	public void revocar(String tenantId, String contactoId) {
		ContactoWhatsappAutorizado contacto = contactoWhatsappAutorizadoRepository
				.buscarPorIdYTenant(contactoId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de("ContactoWhatsappAutorizado", contactoId));
		contacto.setBaja(Instant.now());
		contactoWhatsappAutorizadoRepository.save(contacto);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.CONTACTO_WHATSAPP_REVOCADO, ENTIDAD,
				contacto.getLinea().getId(), Map.of("patron", contacto.getPatron()));
	}

	@Transactional
	public CorrelacionWhatsappModel crearCorrelacion(Tenant tenant, NuevaCorrelacionWhatsappReqModel datos) {
		LineaWhatsapp linea = buscarEntidad(tenant.getId(), datos.getLineaId());
		exigirPlantilla(tenant, datos.getCodigoPlantilla());
		String numeroDestino = NumeroTelefono.normalizarDeclarado(datos.getNumeroDestino());
		if (datos.getNumeroDestino() != null && !datos.getNumeroDestino().isBlank() && numeroDestino == null) {
			throw new ValidacionException("El numero de destino " + datos.getNumeroDestino()
					+ " no tiene formato E.164: tiene que arrancar con + y el codigo de pais, porque un numero"
					+ " sin prefijo internacional puede caer en otro pais");
		}
		if (datos.isEnviarSolicitud() && numeroDestino == null) {
			throw new ValidacionException("Para enviar la solicitud hace falta un numero de destino en E.164");
		}

		ReferenciaExterna sujeto = new ReferenciaExterna();
		sujeto.setOrigen(datos.getSujetoOrigen());
		sujeto.setTipoObjeto(datos.getSujetoTipoObjeto());
		sujeto.setIdObjeto(datos.getSujetoIdObjeto());

		CorrelacionWhatsapp correlacion = new CorrelacionWhatsapp();
		correlacion.setTenant(tenant);
		correlacion.setLinea(linea);
		correlacion.setToken(TokenCorrelacion.generar());
		correlacion.setSujeto(sujeto);
		correlacion.setCodigoPlantilla(datos.getCodigoPlantilla());
		correlacion.setNumeroDestino(numeroDestino);
		correlacion.setDescripcion(datos.getDescripcion());
		correlacion.setVenceEn(Instant.now().plus(
				datos.getDiasVigencia() <= 0 ? propiedades.getDiasVigenciaCorrelacion() : datos.getDiasVigencia(),
				ChronoUnit.DAYS));
		correlacion.setAlta(Instant.now());
		correlacionWhatsappRepository.save(correlacion);

		Map<String, Object> detalle = new LinkedHashMap<>();
		detalle.put("token", correlacion.getToken());
		detalle.put("linea", linea.getNumeroTelefono());
		detalle.put("sujeto", sujeto.getTipoObjeto() + ":" + sujeto.getIdObjeto());
		detalle.put("destino", NumeroTelefono.enmascarar(numeroDestino));
		detalle.put("venceEn", correlacion.getVenceEn());
		auditoriaService.registrarConDetalle(tenant.getId(), AccionAuditoria.CORRELACION_WHATSAPP_CREADA,
				ENTIDAD_CORRELACION, correlacion.getId(), detalle);

		CorrelacionWhatsappModel modelo = canalWhatsappConverter.aModelo(correlacion);
		if (datos.isEnviarSolicitud()) {
			MensajeWhatsappSaliente enviado = whatsappSalienteService.enviar(linea, correlacion,
					PlantillaWhatsapp.SOLICITUD_DOCUMENTACION, numeroDestino, cuerpoDeSolicitud(correlacion),
					List.of(correlacion.getToken()));
			modelo.setMensajeSalienteId(enviado.getId());
			correlacion.setNumeroVinculado(numeroDestino);
			correlacion.setVinculadoEn(Instant.now());
			correlacionWhatsappRepository.save(correlacion);
			modelo.setNumeroVinculado(numeroDestino);
			modelo.setVinculadoEn(correlacion.getVinculadoEn());
		}
		return modelo;
	}

	@Transactional
	public void anularCorrelacion(String tenantId, String correlacionId) {
		CorrelacionWhatsapp correlacion = correlacionWhatsappRepository.buscarPorIdYTenant(correlacionId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(ENTIDAD_CORRELACION, correlacionId));
		correlacion.setBaja(Instant.now());
		correlacionWhatsappRepository.save(correlacion);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.CORRELACION_WHATSAPP_ANULADA,
				ENTIDAD_CORRELACION, correlacionId, Map.of("token", correlacion.getToken()));
	}

	@Transactional(readOnly = true)
	public Page<CorrelacionWhatsappModel> listarCorrelaciones(String tenantId, Pageable paginado) {
		return correlacionWhatsappRepository.listarPorTenant(tenantId, paginado)
				.map(canalWhatsappConverter::aModelo);
	}

	@Transactional(readOnly = true)
	public Page<MensajeWhatsappModel> listarMensajes(String tenantId, ResultadoMensajeWhatsapp resultado,
			Pageable paginado) {
		return mensajeWhatsappEntranteRepository.listarPorTenant(tenantId, resultado, paginado)
				.map(mensaje -> canalWhatsappConverter.aModelo(mensaje, null));
	}

	@Transactional(readOnly = true)
	public MensajeWhatsappModel obtenerMensaje(String tenantId, String mensajeId) {
		return mensajeWhatsappEntranteRepository.buscarPorIdYTenant(mensajeId, tenantId)
				.map(mensaje -> canalWhatsappConverter.aModelo(mensaje,
						mediaWhatsappRepository.listarPorMensaje(mensaje.getId())))
				.orElseThrow(() -> EntidadNoEncontradaException.de(IngestaWhatsappService.ENTIDAD, mensajeId));
	}

	@Transactional(readOnly = true)
	public Page<MensajeWhatsappSalienteModel> listarSalientes(String tenantId, Pageable paginado) {
		return mensajeWhatsappSalienteRepository.listarPorTenant(tenantId, paginado)
				.map(canalWhatsappConverter::aModelo);
	}

	private void exigirSecreto(String referencia, String descripcion) {
		if (ResolvedorSecreto.resolver(referencia).isEmpty()) {
			throw new ValidacionException("No se pudo resolver el " + descripcion + " con la referencia "
					+ referencia + ". Cargalo como variable de entorno antes de crear la linea");
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

	private String cuerpoDeSolicitud(CorrelacionWhatsapp correlacion) {
		StringBuilder cuerpo = new StringBuilder();
		cuerpo.append("Necesitamos la documentacion del caso ").append(correlacion.getSujeto().getTipoObjeto())
				.append(' ').append(correlacion.getSujeto().getIdObjeto()).append(".\n\n");
		cuerpo.append("Respondé este chat adjuntando las fotos o el PDF, e incluí el codigo ")
				.append(TokenCorrelacion.etiquetar(correlacion.getToken())).append(" en el mensaje.\n\n");
		cuerpo.append("Sin ese codigo no podemos asociar los documentos al caso y quedan para revision manual.\n");
		cuerpo.append("El pedido vence el ").append(correlacion.getVenceEn()).append('.');
		return cuerpo.toString();
	}
}
