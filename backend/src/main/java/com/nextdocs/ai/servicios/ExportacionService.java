package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdocs.ai.convertidores.ExportacionConverter;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.ItemExportacion;
import com.nextdocs.ai.entidades.LoteExportacion;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.enumeraciones.EstadoLoteExportacion;
import com.nextdocs.ai.enumeraciones.OrdenExportacion;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.LoteExportacionModel;
import com.nextdocs.ai.modelos.NuevoLoteExportacionReqModel;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.repositorios.ItemExportacionRepository;
import com.nextdocs.ai.repositorios.LoteExportacionRepository;
import com.nextdocs.ai.repositorios.TenantRepository;
import com.nextdocs.ai.specificationBuilder.DocumentoSpecificationBuilder;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExportacionService {

	public static final String ENTIDAD = "LoteExportacion";

	public static final int TOPE_DOCUMENTOS = 2000;

	private static final int DIAS_VIGENCIA = 7;

	private final LoteExportacionRepository loteExportacionRepository;

	private final ItemExportacionRepository itemExportacionRepository;

	private final DocumentoRepository documentoRepository;

	private final TenantRepository tenantRepository;

	private final AlmacenamientoService almacenamientoService;

	private final ExportacionConverter exportacionConverter;

	private final ObjectMapper objectMapper;

	public ExportacionService(LoteExportacionRepository loteExportacionRepository,
			ItemExportacionRepository itemExportacionRepository, DocumentoRepository documentoRepository,
			TenantRepository tenantRepository, AlmacenamientoService almacenamientoService,
			ExportacionConverter exportacionConverter, ObjectMapper objectMapper) {
		this.loteExportacionRepository = loteExportacionRepository;
		this.itemExportacionRepository = itemExportacionRepository;
		this.documentoRepository = documentoRepository;
		this.tenantRepository = tenantRepository;
		this.almacenamientoService = almacenamientoService;
		this.exportacionConverter = exportacionConverter;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public LoteExportacionModel solicitar(String tenantId, Usuario actor, NuevoLoteExportacionReqModel datos) {
		Tenant tenant = tenantRepository.findById(tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de("Tenant", tenantId));
		OrdenExportacion orden = OrdenExportacion.desde(datos.getOrden());

		List<Documento> documentos = resolver(tenantId, datos, orden);
		if (documentos.isEmpty()) {
			throw new ValidacionException("Ningun documento coincide con los filtros pedidos");
		}
		if (documentos.size() > TOPE_DOCUMENTOS) {
			throw new ValidacionException("La exportacion supera el tope de " + TOPE_DOCUMENTOS
					+ " documentos. Acota el rango de fechas o los filtros");
		}

		Instant ahora = Instant.now();
		LoteExportacion lote = new LoteExportacion();
		lote.setTenant(tenant);
		lote.setSolicitadoPor(actor);
		lote.setEstado(EstadoLoteExportacion.SOLICITADO);
		lote.setNombre(datos.getNombre() == null || datos.getNombre().isBlank()
				? "Exportacion " + ahora.truncatedTo(ChronoUnit.SECONDS)
				: datos.getNombre().trim());
		lote.setFiltros(serializar(datos));
		lote.setOrden(orden.name());
		lote.setIncluirOriginales(datos.isIncluirOriginales());
		lote.setCantidadDocumentos(documentos.size());
		lote.setVenceEn(ahora.plus(DIAS_VIGENCIA, ChronoUnit.DAYS));
		lote.setAlta(ahora);
		loteExportacionRepository.save(lote);
		lote.setNombreArchivo("nextdocs-" + lote.getId().substring(0, 8) + ".zip");
		loteExportacionRepository.save(lote);

		for (Documento documento : documentos) {
			itemExportacionRepository.save(itemDe(lote, documento, ahora));
		}
		return exportacionConverter.aModelo(lote, List.of());
	}

	@Transactional(readOnly = true)
	public Page<LoteExportacionModel> listar(String tenantId, EstadoLoteExportacion estado, Pageable paginado) {
		return loteExportacionRepository.listarPorTenant(tenantId, estado, paginado)
				.map(lote -> exportacionConverter.aModelo(lote, null));
	}

	@Transactional(readOnly = true)
	public LoteExportacionModel obtener(String tenantId, String loteId) {
		LoteExportacion lote = buscar(tenantId, loteId);
		return exportacionConverter.aModelo(lote, itemExportacionRepository.listarPorLote(loteId));
	}

	@Transactional(readOnly = true)
	public String urlDescarga(String tenantId, String loteId) {
		LoteExportacion lote = buscar(tenantId, loteId);
		if (lote.getEstado() == EstadoLoteExportacion.VENCIDO) {
			throw new ValidacionException("El lote vencio el " + lote.getVenceEn()
					+ " y su archivo ya fue borrado. Volve a pedir la exportacion");
		}
		if (!ExportacionConverter.esDescargable(lote)) {
			throw new ValidacionException(
					"El lote esta en estado " + lote.getEstado() + " y todavia no tiene archivo para descargar");
		}
		return almacenamientoService.urlFirmada(lote.getBucket(), lote.getClaveObjeto());
	}

	private LoteExportacion buscar(String tenantId, String loteId) {
		return loteExportacionRepository.buscarPorIdYTenant(loteId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(ENTIDAD, loteId));
	}

	private List<Documento> resolver(String tenantId, NuevoLoteExportacionReqModel datos, OrdenExportacion orden) {
		Specification<Documento> especificacion = DocumentoSpecificationBuilder.construir(tenantId,
				datos.getEstados(), datos.getOrigen(), datos.getCodigoPlantilla(), datos.getTexto(),
				datos.getDesde(), datos.getHasta(), datos.getSoloRaiz());

		if (datos.getTipoObjeto() != null && !datos.getTipoObjeto().isBlank()) {
			especificacion = especificacion.and((raiz, consulta, constructor) -> constructor
					.equal(raiz.get("referenciaSujeto").get("tipoObjeto"), datos.getTipoObjeto()));
		}
		if (datos.getIdObjeto() != null && !datos.getIdObjeto().isBlank()) {
			especificacion = especificacion.and((raiz, consulta, constructor) -> constructor
					.equal(raiz.get("referenciaSujeto").get("idObjeto"), datos.getIdObjeto()));
		}

		Sort ordenamiento = orden.isAscendente() ? Sort.by(orden.getCampo()).ascending()
				: Sort.by(orden.getCampo()).descending();
		return documentoRepository.findAll(especificacion, PageRequest.of(0, TOPE_DOCUMENTOS + 1, ordenamiento))
				.getContent();
	}

	private ItemExportacion itemDe(LoteExportacion lote, Documento documento, Instant ahora) {
		ItemExportacion item = new ItemExportacion();
		item.setTenant(lote.getTenant());
		item.setLote(lote);
		item.setDocumento(documento);
		item.setEstadoDocumento(documento.getEstado());
		item.setRecibido(documento.getRecibido());
		item.setCerrado(documento.getCerrado());
		item.setAlta(ahora);
		if (documento.getPlantilla() != null) {
			item.setCodigoPlantilla(documento.getPlantilla().getCodigo());
		}
		if (documento.getVersionPlantilla() != null) {
			item.setNumeroVersionPlantilla(documento.getVersionPlantilla().getNumero());
		}
		if (documento.getReferenciaSujeto() != null) {
			item.setTipoObjeto(documento.getReferenciaSujeto().getTipoObjeto());
			item.setIdObjeto(documento.getReferenciaSujeto().getIdObjeto());
		}
		return item;
	}

	private String serializar(NuevoLoteExportacionReqModel datos) {
		try {
			return objectMapper.writeValueAsString(datos);
		} catch (Exception e) {
			return "{}";
		}
	}
}
