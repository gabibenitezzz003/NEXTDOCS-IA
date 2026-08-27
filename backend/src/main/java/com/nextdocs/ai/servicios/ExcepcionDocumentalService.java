package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.convertidores.ExcepcionConverter;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.ExcepcionDocumental;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.EstadoExcepcion;
import com.nextdocs.ai.enumeraciones.PrioridadExcepcion;
import com.nextdocs.ai.enumeraciones.SeveridadHallazgo;
import com.nextdocs.ai.enumeraciones.TipoEventoCanonico;
import com.nextdocs.ai.enumeraciones.TipoExcepcion;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.modelos.ExcepcionDocumentalModel;
import com.nextdocs.ai.repositorios.ExcepcionDocumentalRepository;
import com.nextdocs.ai.utiles.ContextoCorrelacion;
import com.nextdocs.ai.utiles.Hash;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class ExcepcionDocumentalService {

	public static final String ENTIDAD = "ExcepcionDocumental";

	private static final Map<PrioridadExcepcion, Integer> HORAS_SLA = Map.of(PrioridadExcepcion.CRITICA, 4,
			PrioridadExcepcion.ALTA, 8, PrioridadExcepcion.MEDIA, 24, PrioridadExcepcion.BAJA, 72);

	private final ExcepcionDocumentalRepository excepcionDocumentalRepository;

	private final AuditoriaService auditoriaService;

	private final EventoSalidaService eventoSalidaService;

	private final ExcepcionConverter excepcionConverter;

	public ExcepcionDocumentalService(ExcepcionDocumentalRepository excepcionDocumentalRepository,
			AuditoriaService auditoriaService, EventoSalidaService eventoSalidaService,
			ExcepcionConverter excepcionConverter) {
		this.excepcionDocumentalRepository = excepcionDocumentalRepository;
		this.auditoriaService = auditoriaService;
		this.eventoSalidaService = eventoSalidaService;
		this.excepcionConverter = excepcionConverter;
	}

	@Transactional
	public ExcepcionDocumental abrir(Tenant tenant, Documento documento, TipoExcepcion tipo,
			SeveridadHallazgo severidad, String codigo, String detalle) {
		String claveDeduplicacion = claveDeduplicacion(tenant.getId(), documento, tipo, codigo);
		Optional<ExcepcionDocumental> existente = excepcionDocumentalRepository
				.findByClaveDeduplicacion(claveDeduplicacion);
		if (existente.isPresent() && existente.get().getEstado() != EstadoExcepcion.RESUELTA) {
			return existente.get();
		}
		PrioridadExcepcion prioridad = prioridadDe(severidad);
		ExcepcionDocumental excepcion = new ExcepcionDocumental();
		excepcion.setTenant(tenant);
		excepcion.setDocumento(documento);
		excepcion.setTipo(tipo);
		excepcion.setSeveridad(severidad);
		excepcion.setPrioridad(prioridad);
		excepcion.setEstado(EstadoExcepcion.ABIERTA);
		excepcion.setCodigo(codigo);
		excepcion.setDetalle(detalle);
		excepcion.setClaveDeduplicacion(claveDeduplicacion);
		excepcion.setCorrelacionId(ContextoCorrelacion.obtenerOGenerar());
		excepcion.setVenceEn(Instant.now().plus(HORAS_SLA.getOrDefault(prioridad, 24), ChronoUnit.HOURS));
		excepcion.setAlta(Instant.now());
		excepcionDocumentalRepository.save(excepcion);
		eventoSalidaService.publicar(tenant.getId(), TipoEventoCanonico.EXCEPCION_CREADA, ENTIDAD, excepcion.getId(),
				Map.of("tipo", tipo, "severidad", severidad, "codigo", codigo == null ? "" : codigo, "documentoId",
						documento == null ? "" : documento.getId()));
		return excepcion;
	}

	@Transactional
	public ExcepcionDocumentalModel asignar(String tenantId, String excepcionId, Usuario responsable) {
		ExcepcionDocumental excepcion = buscarEntidad(tenantId, excepcionId);
		excepcion.setResponsable(responsable);
		excepcion.setEstado(EstadoExcepcion.EN_CURSO);
		excepcionDocumentalRepository.save(excepcion);
		auditoriaService.registrar(tenantId, AccionAuditoria.EXCEPCION_ASIGNADA, ENTIDAD, excepcionId);
		return excepcionConverter.aModelo(excepcion);
	}

	@Transactional
	public ExcepcionDocumentalModel resolver(String tenantId, String excepcionId, Usuario actor,
			String resolucion) {
		ExcepcionDocumental excepcion = buscarEntidad(tenantId, excepcionId);
		excepcion.setEstado(EstadoExcepcion.RESUELTA);
		excepcion.setResueltaPor(actor);
		excepcion.setResolucion(resolucion);
		excepcion.setResuelta(Instant.now());
		excepcionDocumentalRepository.save(excepcion);
		auditoriaService.registrar(tenantId, AccionAuditoria.EXCEPCION_RESUELTA, ENTIDAD, excepcionId);
		eventoSalidaService.publicar(tenantId, TipoEventoCanonico.EXCEPCION_RESUELTA, ENTIDAD, excepcionId,
				Map.of("resolucion", resolucion == null ? "" : resolucion));
		return excepcionConverter.aModelo(excepcion);
	}

	@Transactional
	public void resolverAutomaticamente(String tenantId, Documento documento, TipoExcepcion tipo, String codigo) {
		String clave = claveDeduplicacion(tenantId, documento, tipo, codigo);
		excepcionDocumentalRepository.findByClaveDeduplicacion(clave).ifPresent(excepcion -> {
			if (excepcion.getEstado() == EstadoExcepcion.RESUELTA) {
				return;
			}
			excepcion.setEstado(EstadoExcepcion.RESUELTA);
			excepcion.setResolucion("Resuelta automaticamente por reproceso");
			excepcion.setResuelta(Instant.now());
			excepcionDocumentalRepository.save(excepcion);
		});
	}

	@Transactional(readOnly = true)
	public Page<ExcepcionDocumentalModel> listar(String tenantId, EstadoExcepcion estado, Pageable paginado) {
		return excepcionDocumentalRepository.listarPorTenant(tenantId, estado, paginado)
				.map(excepcionConverter::aModelo);
	}

	@Transactional(readOnly = true)
	public List<ExcepcionDocumentalModel> listarPorDocumento(String documentoId) {
		return excepcionConverter.aModelos(excepcionDocumentalRepository.listarPorDocumento(documentoId));
	}

	@Transactional(readOnly = true)
	public ExcepcionDocumentalModel obtener(String tenantId, String excepcionId) {
		return excepcionConverter.aModelo(buscarEntidad(tenantId, excepcionId));
	}

	@Transactional(readOnly = true)
	public ExcepcionDocumental buscarEntidad(String tenantId, String excepcionId) {
		return excepcionDocumentalRepository.buscarPorIdYTenant(excepcionId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(ENTIDAD, excepcionId));
	}

	private PrioridadExcepcion prioridadDe(SeveridadHallazgo severidad) {
		return switch (severidad) {
			case BLOQUEANTE -> PrioridadExcepcion.CRITICA;
			case REQUIERE_REVISION -> PrioridadExcepcion.ALTA;
			case ADVERTENCIA -> PrioridadExcepcion.MEDIA;
			case INFORMATIVO -> PrioridadExcepcion.BAJA;
		};
	}

	private String claveDeduplicacion(String tenantId, Documento documento, TipoExcepcion tipo, String codigo) {
		String base = String.join("|", tenantId, documento == null ? "sin-documento" : documento.getId(), tipo.name(),
				codigo == null ? "" : codigo);
		return Hash.sha256(base).substring(0, 64);
	}
}
