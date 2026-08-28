package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.nextdocs.ai.entidades.ArchivoDocumento;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.SegmentoDocumento;
import com.nextdocs.ai.entidades.VersionPlantilla;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.EstadoDocumento;
import com.nextdocs.ai.enumeraciones.EstrategiaSegmentacion;
import com.nextdocs.ai.repositorios.ArchivoDocumentoRepository;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.repositorios.SegmentoDocumentoRepository;
import com.nextdocs.ai.utiles.ClaveObjeto;
import com.nextdocs.ai.utiles.ContextoCorrelacion;
import com.nextdocs.ai.utiles.DivisorPdf;
import com.nextdocs.ai.utiles.Hash;
import com.nextdocs.ai.utiles.InspectorArchivo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SegmentacionDocumentalService {

	public static final String ENTIDAD = "SegmentoDocumento";

	private static final Logger log = LoggerFactory.getLogger(SegmentacionDocumentalService.class);

	private final DocumentoRepository documentoRepository;

	private final ArchivoDocumentoRepository archivoDocumentoRepository;

	private final SegmentoDocumentoRepository segmentoDocumentoRepository;

	private final AlmacenamientoService almacenamientoService;

	private final ColaExtraccionService colaExtraccionService;

	private final EstadoDocumentalService estadoDocumentalService;

	private final AuditoriaService auditoriaService;

	private final OriginalFisicoService originalFisicoService;

	public SegmentacionDocumentalService(DocumentoRepository documentoRepository,
			ArchivoDocumentoRepository archivoDocumentoRepository,
			SegmentoDocumentoRepository segmentoDocumentoRepository, AlmacenamientoService almacenamientoService,
			ColaExtraccionService colaExtraccionService, EstadoDocumentalService estadoDocumentalService,
			AuditoriaService auditoriaService, OriginalFisicoService originalFisicoService) {
		this.documentoRepository = documentoRepository;
		this.archivoDocumentoRepository = archivoDocumentoRepository;
		this.segmentoDocumentoRepository = segmentoDocumentoRepository;
		this.almacenamientoService = almacenamientoService;
		this.colaExtraccionService = colaExtraccionService;
		this.estadoDocumentalService = estadoDocumentalService;
		this.auditoriaService = auditoriaService;
		this.originalFisicoService = originalFisicoService;
	}

	public boolean corresponde(Documento documento) {
		if (documento.getDocumentoPadre() != null) {
			return false;
		}
		VersionPlantilla version = documento.getVersionPlantilla();
		if (version == null || version.getEstrategiaSegmentacion() == null
				|| version.getEstrategiaSegmentacion() == EstrategiaSegmentacion.NINGUNA) {
			return false;
		}
		List<ArchivoDocumento> archivos = archivoDocumentoRepository.listarOriginales(documento.getId());
		if (archivos.isEmpty()) {
			return false;
		}
		ArchivoDocumento archivo = archivos.get(0);
		return InspectorArchivo.esPdf(archivo.getTipoMime()) && archivo.getPaginas() > 1;
	}

	@Transactional
	public int segmentar(Documento documento) {
		VersionPlantilla version = documento.getVersionPlantilla();
		ArchivoDocumento archivo = archivoDocumentoRepository.listarOriginales(documento.getId()).get(0);
		byte[] contenido = almacenamientoService.leerDocumento(archivo.getClaveObjeto());

		List<DivisorPdf.Tramo> tramos = calcularTramos(version, contenido, archivo.getPaginas());
		if (tramos.size() <= 1) {
			log.info("El documento {} no se divide, la estrategia dio un solo tramo", documento.getId());
			return 0;
		}

		List<Documento> hijos = new ArrayList<>();
		List<SegmentoDocumento> segmentos = new ArrayList<>();
		List<ArchivoDocumento> archivos = new ArrayList<>();
		int orden = 1;
		for (DivisorPdf.Tramo tramo : tramos) {
			byte[] recorte = DivisorPdf.extraer(contenido, tramo.paginaDesde(), tramo.paginaHasta());
			Documento hijo = construirHijo(documento, tramo, orden, tramos.size(), recorte);
			documentoRepository.save(hijo);
			hijos.add(hijo);

			String claveObjeto = ClaveObjeto.paraDocumento(documento.getTenant().getId(), hijo.getId(),
					hijo.getNombre());
			almacenamientoService.guardarDocumento(claveObjeto, recorte, archivo.getTipoMime());
			archivos.add(construirArchivo(hijo, archivo, recorte, claveObjeto, tramo.cantidadPaginas(),
					claveObjeto));
			segmentos.add(construirSegmento(documento, hijo, tramo, orden));
			orden++;
		}
		archivoDocumentoRepository.saveAll(archivos);
		segmentoDocumentoRepository.saveAll(segmentos);

		documento.setCantidadSegmentos(hijos.size());
		documentoRepository.save(documento);
		estadoDocumentalService.transicionar(documento, EstadoDocumento.DIVIDIDO);

		auditoriaService.registrarConDetalle(documento.getTenant().getId(), AccionAuditoria.DOCUMENTO_SEGMENTADO,
				ENTIDAD, documento.getId(), Map.of("estrategia", version.getEstrategiaSegmentacion(), "segmentos",
						hijos.size(), "paginas", archivo.getPaginas()));

		for (Documento hijo : hijos) {
			originalFisicoService.iniciarSiCorresponde(hijo);
			colaExtraccionService.encolar(hijo.getId());
		}
		return hijos.size();
	}

	private List<DivisorPdf.Tramo> calcularTramos(VersionPlantilla version, byte[] contenido, int totalPaginas) {
		if (version.getEstrategiaSegmentacion() == EstrategiaSegmentacion.PAGINAS_FIJAS) {
			return DivisorPdf.tramosPorPaginasFijas(totalPaginas, version.getPaginasPorDocumento());
		}
		return DivisorPdf.tramosPorPatron(contenido, version.getPatronInicioDocumento());
	}

	private Documento construirHijo(Documento padre, DivisorPdf.Tramo tramo, int orden, int total, byte[] recorte) {
		Documento hijo = new Documento();
		hijo.setTenant(padre.getTenant());
		hijo.setEstado(EstadoDocumento.RECIBIDO);
		hijo.setOrigen(padre.getOrigen());
		hijo.setNombre(nombreHijo(padre.getNombre(), orden, total));
		hijo.setHashContenido(Hash.sha256(recorte));
		hijo.setClaveIdempotencia(padre.getClaveIdempotencia() + "#segmento-" + orden);
		hijo.setCorrelacionId(ContextoCorrelacion.obtenerOGenerar());
		hijo.setPlantilla(padre.getPlantilla());
		hijo.setVersionPlantilla(padre.getVersionPlantilla());
		hijo.setDocumentoPadre(padre);
		hijo.setPaginaDesde(tramo.paginaDesde());
		hijo.setPaginaHasta(tramo.paginaHasta());
		hijo.setReferenciaSujeto(padre.getReferenciaSujeto());
		hijo.setRemitente(padre.getRemitente());
		hijo.setIngresadoPor(padre.getIngresadoPor());
		hijo.setIngresadoPorCuentaServicio(padre.getIngresadoPorCuentaServicio());
		hijo.setRecibido(padre.getRecibido());
		hijo.setAlta(Instant.now());
		return hijo;
	}

	private String nombreHijo(String nombrePadre, int orden, int total) {
		String base = nombrePadre == null ? "documento" : nombrePadre;
		int punto = base.lastIndexOf('.');
		String sinExtension = punto > 0 ? base.substring(0, punto) : base;
		String extension = punto > 0 ? base.substring(punto) : "";
		return sinExtension + " (" + orden + " de " + total + ")" + extension;
	}

	private ArchivoDocumento construirArchivo(Documento hijo, ArchivoDocumento original, byte[] recorte,
			String claveObjeto, int paginas, String nombreClave) {
		ArchivoDocumento archivo = new ArchivoDocumento();
		archivo.setTenant(hijo.getTenant());
		archivo.setDocumento(hijo);
		archivo.setClaveObjeto(claveObjeto);
		archivo.setBucket(almacenamientoService.bucketDocumentos());
		archivo.setNombreArchivo(hijo.getNombre());
		archivo.setTipoMime(original.getTipoMime());
		archivo.setExtension(original.getExtension());
		archivo.setTamano(recorte.length);
		archivo.setChecksum(hijo.getHashContenido());
		archivo.setPaginas(paginas);
		archivo.setVersion(1);
		archivo.setOriginal(true);
		archivo.setAlta(Instant.now());
		return archivo;
	}

	private SegmentoDocumento construirSegmento(Documento padre, Documento hijo, DivisorPdf.Tramo tramo, int orden) {
		SegmentoDocumento segmento = new SegmentoDocumento();
		segmento.setTenant(padre.getTenant());
		segmento.setDocumentoPadre(padre);
		segmento.setDocumentoHijo(hijo);
		segmento.setPaginaDesde(tramo.paginaDesde());
		segmento.setPaginaHasta(tramo.paginaHasta());
		segmento.setOrden(orden);
		segmento.setMotivoCorte(tramo.motivo());
		segmento.setAlta(Instant.now());
		return segmento;
	}
}
