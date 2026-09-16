package com.nextdocs.ai.servicios;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.nextdocs.ai.config.PropiedadesWebhooks;
import com.nextdocs.ai.convertidores.IntegracionConverter;
import com.nextdocs.ai.entidades.EntregaWebhook;
import com.nextdocs.ai.entidades.EventoSalida;
import com.nextdocs.ai.entidades.SuscripcionWebhook;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.EstadoEntregaWebhook;
import com.nextdocs.ai.enumeraciones.EstadoEventoSalida;
import com.nextdocs.ai.enumeraciones.TipoEventoCanonico;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.EntregaWebhookModel;
import com.nextdocs.ai.modelos.EventoServicioReqModel;
import com.nextdocs.ai.modelos.SaludIntegracionModel;
import com.nextdocs.ai.modelos.SuscripcionWebhookCreadaModel;
import com.nextdocs.ai.modelos.SuscripcionWebhookModel;
import com.nextdocs.ai.modelos.SuscripcionWebhookReqModel;
import com.nextdocs.ai.repositorios.EntregaWebhookRepository;
import com.nextdocs.ai.repositorios.EventoSalidaRepository;
import com.nextdocs.ai.repositorios.SuscripcionWebhookRepository;
import com.nextdocs.ai.utiles.ValidadorUrlWebhook;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntegracionService {

	public static final String ENTIDAD_SUSCRIPCION = "SuscripcionWebhook";

	public static final String ENTIDAD_ENTREGA = "EntregaWebhook";

	public static final String ADVERTENCIA_SECRETO = "Este secreto se muestra una sola vez. Guardalo: las firmas HMAC lo usan y no se vuelve a exponer";

	private static final String PREFIJO_SECRETO = "ndwh";

	private static final int BYTES_SECRETO = 32;

	private final SuscripcionWebhookRepository suscripcionWebhookRepository;

	private final EntregaWebhookRepository entregaWebhookRepository;

	private final EventoSalidaRepository eventoSalidaRepository;

	private final EventoSalidaService eventoSalidaService;

	private final EntregaWebhookService entregaWebhookService;

	private final AuditoriaService auditoriaService;

	private final IntegracionConverter integracionConverter;

	private final PropiedadesWebhooks propiedades;

	private final SecureRandom aleatorio = new SecureRandom();

	public IntegracionService(SuscripcionWebhookRepository suscripcionWebhookRepository,
			EntregaWebhookRepository entregaWebhookRepository, EventoSalidaRepository eventoSalidaRepository,
			EventoSalidaService eventoSalidaService, EntregaWebhookService entregaWebhookService,
			AuditoriaService auditoriaService, IntegracionConverter integracionConverter,
			PropiedadesWebhooks propiedades) {
		this.suscripcionWebhookRepository = suscripcionWebhookRepository;
		this.entregaWebhookRepository = entregaWebhookRepository;
		this.eventoSalidaRepository = eventoSalidaRepository;
		this.eventoSalidaService = eventoSalidaService;
		this.entregaWebhookService = entregaWebhookService;
		this.auditoriaService = auditoriaService;
		this.integracionConverter = integracionConverter;
		this.propiedades = propiedades;
	}

	@Transactional(readOnly = true)
	public List<SuscripcionWebhookModel> listar(String tenantId) {
		return integracionConverter.aModelos(suscripcionWebhookRepository.listarPorTenant(tenantId));
	}

	@Transactional(readOnly = true)
	public SuscripcionWebhookModel obtener(String tenantId, String suscripcionId) {
		return integracionConverter.aModelo(exigir(tenantId, suscripcionId));
	}

	@Transactional
	public SuscripcionWebhookCreadaModel crear(Tenant tenant, SuscripcionWebhookReqModel datos) {
		String url = ValidadorUrlWebhook.validar(datos.getUrl(), propiedades.isPermitirLocalhost()).toString();
		Set<TipoEventoCanonico> eventos = validarEventos(datos.getEventos());
		String secreto = generarSecreto();
		SuscripcionWebhook suscripcion = new SuscripcionWebhook();
		suscripcion.setTenant(tenant);
		suscripcion.setNombre(datos.getNombre().trim());
		suscripcion.setUrl(url);
		suscripcion.setSecreto(secreto);
		suscripcion.setEventos(eventos);
		suscripcion.setActiva(true);
		suscripcion.setFallosConsecutivos(0);
		suscripcion.setAlta(Instant.now());
		suscripcionWebhookRepository.save(suscripcion);
		auditoriaService.registrarConDetalle(tenant.getId(), AccionAuditoria.WEBHOOK_SUSCRITO, ENTIDAD_SUSCRIPCION,
				suscripcion.getId(), Map.of("nombre", suscripcion.getNombre(), "url", url, "eventos",
						nombres(eventos)));
		return envuelto(suscripcion, secreto);
	}

	@Transactional
	public SuscripcionWebhookModel modificar(String tenantId, String suscripcionId, SuscripcionWebhookReqModel datos) {
		SuscripcionWebhook suscripcion = exigir(tenantId, suscripcionId);
		String url = ValidadorUrlWebhook.validar(datos.getUrl(), propiedades.isPermitirLocalhost()).toString();
		Set<TipoEventoCanonico> eventos = validarEventos(datos.getEventos());
		suscripcion.setNombre(datos.getNombre().trim());
		suscripcion.setUrl(url);
		suscripcion.setEventos(eventos);
		suscripcionWebhookRepository.save(suscripcion);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.WEBHOOK_MODIFICADO, ENTIDAD_SUSCRIPCION,
				suscripcion.getId(), Map.of("nombre", suscripcion.getNombre(), "url", url, "eventos",
						nombres(eventos)));
		return integracionConverter.aModelo(suscripcion);
	}

	@Transactional
	public SuscripcionWebhookModel pausar(String tenantId, String suscripcionId) {
		SuscripcionWebhook suscripcion = exigir(tenantId, suscripcionId);
		if (!suscripcion.isActiva()) {
			return integracionConverter.aModelo(suscripcion);
		}
		suscripcion.setActiva(false);
		suscripcionWebhookRepository.save(suscripcion);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.WEBHOOK_PAUSADO, ENTIDAD_SUSCRIPCION,
				suscripcion.getId(), Map.of("motivo", "manual", "url", suscripcion.getUrl()));
		return integracionConverter.aModelo(suscripcion);
	}

	@Transactional
	public SuscripcionWebhookModel reactivar(String tenantId, String suscripcionId) {
		SuscripcionWebhook suscripcion = exigir(tenantId, suscripcionId);
		suscripcion.setActiva(true);
		suscripcion.setFallosConsecutivos(0);
		suscripcionWebhookRepository.save(suscripcion);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.WEBHOOK_REACTIVADO, ENTIDAD_SUSCRIPCION,
				suscripcion.getId(), Map.of("url", suscripcion.getUrl()));
		return integracionConverter.aModelo(suscripcion);
	}

	@Transactional
	public void eliminar(String tenantId, String suscripcionId) {
		SuscripcionWebhook suscripcion = exigir(tenantId, suscripcionId);
		suscripcion.setActiva(false);
		suscripcion.setBaja(Instant.now());
		suscripcionWebhookRepository.save(suscripcion);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.WEBHOOK_ELIMINADO, ENTIDAD_SUSCRIPCION,
				suscripcion.getId(), Map.of("nombre", suscripcion.getNombre(), "url", suscripcion.getUrl()));
	}

	@Transactional
	public SuscripcionWebhookCreadaModel rotarSecreto(String tenantId, String suscripcionId) {
		SuscripcionWebhook suscripcion = exigir(tenantId, suscripcionId);
		String secreto = generarSecreto();
		suscripcion.setSecreto(secreto);
		suscripcionWebhookRepository.save(suscripcion);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.WEBHOOK_MODIFICADO, ENTIDAD_SUSCRIPCION,
				suscripcion.getId(), Map.of("rotacionSecreto", true));
		return envuelto(suscripcion, secreto);
	}

	@Transactional
	public EntregaWebhookModel probar(String tenantId, String suscripcionId) {
		SuscripcionWebhook suscripcion = exigir(tenantId, suscripcionId);
		Map<String, Object> carga = new LinkedHashMap<>();
		carga.put("tipo", TipoEventoCanonico.WEBHOOK_PRUEBA.getClave());
		carga.put("suscripcionId", suscripcion.getId());
		carga.put("nombre", suscripcion.getNombre());
		EventoSalida evento = eventoSalidaService.publicar(tenantId, TipoEventoCanonico.WEBHOOK_PRUEBA,
				ENTIDAD_SUSCRIPCION, suscripcion.getId(), carga);
		EntregaWebhook entrega = entregaWebhookService.crearYEntregar(evento, suscripcion);
		evento.setEstado(EstadoEventoSalida.ENVIADO);
		evento.setProcesado(Instant.now());
		eventoSalidaRepository.save(evento);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.WEBHOOK_PROBADO, ENTIDAD_SUSCRIPCION,
				suscripcion.getId(), Map.of("entregaId", entrega.getId(), "estado", entrega.getEstado().name(),
						"codigoRespuesta", entrega.getCodigoRespuesta()));
		return integracionConverter.aModelo(entrega, evento);
	}

	@Transactional
	public Map<String, Object> publicarEventoServicio(String tenantId, EventoServicioReqModel datos) {
		TipoEventoCanonico tipo = TipoEventoCanonico.desde(datos.getTipoEvento());
		if (tipo == null || !tipo.getClave().startsWith("process.")) {
			throw new ValidacionException(
					"El tipo de evento " + datos.getTipoEvento() + " no es un evento de proceso admitido");
		}
		EventoSalida evento = eventoSalidaService.publicar(tenantId, tipo,
				datos.getTipoAgregado() == null ? "InstanciaProceso" : datos.getTipoAgregado(),
				datos.getIdAgregado(), datos.getCarga() == null ? Map.of() : datos.getCarga());
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.EVENTO_SERVICIO_PUBLICADO,
				"EventoSalida", evento.getId(),
				Map.of("tipoEvento", tipo.getClave(), "idAgregado", String.valueOf(datos.getIdAgregado())));
		return Map.of("id", evento.getId(), "tipoEvento", tipo.getClave(), "estado", evento.getEstado().name());
	}

	@Transactional(readOnly = true)
	public Page<EntregaWebhookModel> listarEntregas(String tenantId, EstadoEntregaWebhook estado, String suscripcionId,
			Pageable paginado) {
		Page<EntregaWebhook> pagina;
		if (suscripcionId != null && !suscripcionId.isBlank()) {
			exigir(tenantId, suscripcionId);
			pagina = entregaWebhookRepository.listarPorSuscripcion(suscripcionId, paginado);
		} else if (estado != null) {
			pagina = entregaWebhookRepository.listarPorTenantYEstado(tenantId, estado, paginado);
		} else {
			pagina = entregaWebhookRepository.listarPorTenant(tenantId, paginado);
		}
		Map<String, EventoSalida> eventos = eventosDe(pagina.getContent());
		return pagina.map(entrega -> integracionConverter.aModelo(entrega, eventos.get(entrega.getEventoId())));
	}

	@Transactional
	public EntregaWebhookModel reintentar(String tenantId, String entregaId) {
		EntregaWebhook entrega = entregaWebhookRepository.buscarPorIdYTenant(entregaId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(ENTIDAD_ENTREGA, entregaId));
		if (entrega.getEstado() == EstadoEntregaWebhook.ENTREGADO) {
			throw new ValidacionException("Una entrega ya confirmada no se reintenta");
		}
		if (entrega.getEstado() == EstadoEntregaWebhook.AGOTADO) {
			entrega.setIntento(0);
		}
		entrega.setEstado(EstadoEntregaWebhook.PENDIENTE);
		entrega.setDisponibleEn(Instant.now());
		entregaWebhookRepository.save(entrega);
		entregaWebhookService.entregarPendiente(entrega.getId());
		EntregaWebhook actualizada = entregaWebhookRepository.findById(entrega.getId()).orElse(entrega);
		EventoSalida evento = eventoSalidaRepository.findById(actualizada.getEventoId()).orElse(null);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.WEBHOOK_REINTENTADO, ENTIDAD_ENTREGA,
				actualizada.getId(), Map.of("estado", actualizada.getEstado().name(), "intento",
						actualizada.getIntento()));
		return integracionConverter.aModelo(actualizada, evento);
	}

	@Transactional(readOnly = true)
	public SaludIntegracionModel salud(String tenantId) {
		SaludIntegracionModel salud = new SaludIntegracionModel();
		long activas = suscripcionWebhookRepository.contarPorTenantYActiva(tenantId, true);
		long pausadas = suscripcionWebhookRepository.contarPorTenantYActiva(tenantId, false);
		salud.setSuscripcionesActivas(activas);
		salud.setSuscripcionesPausadas(pausadas);
		salud.setSuscripcionesTotales(activas + pausadas);
		Map<EstadoEntregaWebhook, Long> porEstado = new EnumMap<>(EstadoEntregaWebhook.class);
		for (EstadoEntregaWebhook estado : EstadoEntregaWebhook.values()) {
			porEstado.put(estado, 0L);
		}
		for (Object[] fila : entregaWebhookRepository.contarPorEstado(tenantId)) {
			porEstado.put((EstadoEntregaWebhook) fila[0], (Long) fila[1]);
		}
		salud.setEntregasPendientes(porEstado.get(EstadoEntregaWebhook.PENDIENTE));
		salud.setEntregasEntregadas(porEstado.get(EstadoEntregaWebhook.ENTREGADO));
		salud.setEntregasAgotadas(porEstado.get(EstadoEntregaWebhook.AGOTADO));
		salud.setUmbralPausa(propiedades.getUmbralPausa() <= 0 ? 10 : propiedades.getUmbralPausa());
		return salud;
	}

	private SuscripcionWebhook exigir(String tenantId, String suscripcionId) {
		return suscripcionWebhookRepository.buscarPorIdYTenant(suscripcionId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(ENTIDAD_SUSCRIPCION, suscripcionId));
	}

	private Set<TipoEventoCanonico> validarEventos(List<TipoEventoCanonico> eventos) {
		if (eventos == null || eventos.isEmpty()) {
			throw new ValidacionException("La suscripcion debe declarar al menos un evento");
		}
		Set<TipoEventoCanonico> unicos = new LinkedHashSet<>();
		for (TipoEventoCanonico evento : eventos) {
			if (evento == null) {
				throw new ValidacionException("Hay un evento de webhook vacio");
			}
			unicos.add(evento);
		}
		return unicos;
	}

	private String generarSecreto() {
		byte[] material = new byte[BYTES_SECRETO];
		aleatorio.nextBytes(material);
		return PREFIJO_SECRETO + "_" + Base64.getUrlEncoder().withoutPadding().encodeToString(material);
	}

	private SuscripcionWebhookCreadaModel envuelto(SuscripcionWebhook suscripcion, String secreto) {
		SuscripcionWebhookCreadaModel creada = new SuscripcionWebhookCreadaModel();
		creada.setSuscripcion(integracionConverter.aModelo(suscripcion));
		creada.setSecreto(secreto);
		creada.setAdvertencia(ADVERTENCIA_SECRETO);
		return creada;
	}

	private List<String> nombres(Set<TipoEventoCanonico> eventos) {
		List<String> nombres = new ArrayList<>();
		for (TipoEventoCanonico evento : eventos) {
			nombres.add(evento.getClave());
		}
		return nombres;
	}

	private Map<String, EventoSalida> eventosDe(List<EntregaWebhook> entregas) {
		List<String> ids = new ArrayList<>();
		for (EntregaWebhook entrega : entregas) {
			ids.add(entrega.getEventoId());
		}
		Map<String, EventoSalida> eventos = new LinkedHashMap<>();
		if (ids.isEmpty()) {
			return eventos;
		}
		for (EventoSalida evento : eventoSalidaRepository.findAllById(ids)) {
			eventos.put(evento.getId(), evento);
		}
		return eventos;
	}
}
