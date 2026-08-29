package com.nextdocs.ai.servicios;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.nextdocs.ai.entidades.ArchivoDocumento;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.ItemExportacion;
import com.nextdocs.ai.entidades.LoteExportacion;
import com.nextdocs.ai.enumeraciones.EstadoLoteExportacion;
import com.nextdocs.ai.enumeraciones.TipoEventoCanonico;
import com.nextdocs.ai.repositorios.ArchivoDocumentoRepository;
import com.nextdocs.ai.repositorios.HallazgoValidacionRepository;
import com.nextdocs.ai.repositorios.ItemExportacionRepository;
import com.nextdocs.ai.repositorios.LoteExportacionRepository;
import com.nextdocs.ai.utiles.ExportadorCsv;
import com.nextdocs.ai.utiles.Hash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GeneradorExportacionService {

	private static final Logger log = LoggerFactory.getLogger(GeneradorExportacionService.class);

	public static final String NOMBRE_INDICE = "indice.csv";

	public static final String NOMBRE_MANIFIESTO = "manifiesto-sha256.txt";

	public static final String CARPETA_DOCUMENTOS = "documentos/";

	private static final List<String> ENCABEZADOS_INDICE = List.of("batch_id", "object_type", "object_id",
			"document_type", "document_version", "document_id", "created_at", "closed_at", "actors",
			"archive_filename", "size_bytes", "sha256", "status", "findings", "skipped_reason");

	private final LoteExportacionRepository loteExportacionRepository;

	private final ItemExportacionRepository itemExportacionRepository;

	private final ArchivoDocumentoRepository archivoDocumentoRepository;

	private final HallazgoValidacionRepository hallazgoValidacionRepository;

	private final AlmacenamientoService almacenamientoService;

	private final EventoSalidaService eventoSalidaService;

	public GeneradorExportacionService(LoteExportacionRepository loteExportacionRepository,
			ItemExportacionRepository itemExportacionRepository,
			ArchivoDocumentoRepository archivoDocumentoRepository,
			HallazgoValidacionRepository hallazgoValidacionRepository, AlmacenamientoService almacenamientoService,
			EventoSalidaService eventoSalidaService) {
		this.loteExportacionRepository = loteExportacionRepository;
		this.itemExportacionRepository = itemExportacionRepository;
		this.archivoDocumentoRepository = archivoDocumentoRepository;
		this.hallazgoValidacionRepository = hallazgoValidacionRepository;
		this.almacenamientoService = almacenamientoService;
		this.eventoSalidaService = eventoSalidaService;
	}

	@Transactional
	public LoteExportacion generar(String loteId) {
		LoteExportacion lote = loteExportacionRepository.findById(loteId).orElse(null);
		if (lote == null || lote.getEstado() != EstadoLoteExportacion.SOLICITADO) {
			return lote;
		}
		lote.setEstado(EstadoLoteExportacion.GENERANDO);
		loteExportacionRepository.save(lote);

		try {
			byte[] paquete = construirPaquete(lote);
			String clave = lote.getTenant().getId() + "/" + lote.getId() + "/" + lote.getNombreArchivo();
			almacenamientoService.guardarExportacion(clave, paquete, "application/zip");

			lote.setBucket(almacenamientoService.bucketExportaciones());
			lote.setClaveObjeto(clave);
			lote.setTamanoBytes(paquete.length);
			lote.setSha256(Hash.sha256(paquete));
			lote.setEstado(EstadoLoteExportacion.DISPONIBLE);
			lote.setGenerado(Instant.now());
			loteExportacionRepository.save(lote);

			eventoSalidaService.publicar(lote.getTenant().getId(), TipoEventoCanonico.EXPORTACION_LISTA,
					"LoteExportacion", lote.getId(), datosDelLote(lote));
			log.info("Lote de exportacion {} generado con {} documentos y {} bytes", lote.getId(),
					lote.getCantidadDocumentos(), lote.getTamanoBytes());
		} catch (Exception e) {
			lote.setEstado(EstadoLoteExportacion.FALLIDO);
			lote.setDetalleError(e.getClass().getSimpleName() + ": " + e.getMessage());
			loteExportacionRepository.save(lote);
			log.error("Fallo la generacion del lote de exportacion {}", lote.getId(), e);
		}
		return lote;
	}

	private byte[] construirPaquete(LoteExportacion lote) throws Exception {
		List<ItemExportacion> items = itemExportacionRepository.listarPorLote(lote.getId());
		ExportadorCsv indice = new ExportadorCsv(ENCABEZADOS_INDICE);
		List<String> manifiesto = new ArrayList<>();
		ByteArrayOutputStream salida = new ByteArrayOutputStream();

		try (ZipOutputStream zip = new ZipOutputStream(salida, StandardCharsets.UTF_8)) {
			for (ItemExportacion item : items) {
				completarItem(lote, item);
				if (item.getMotivoOmision() == null && lote.isIncluirOriginales()) {
					byte[] contenido = almacenamientoService.leerDocumento(claveDe(item));
					zip.putNextEntry(new ZipEntry(item.getNombreEnArchivo()));
					zip.write(contenido);
					zip.closeEntry();
					manifiesto.add(item.getSha256() + "  " + item.getNombreEnArchivo());
				}
				indice.fila(filaDe(lote, item));
				itemExportacionRepository.save(item);
			}

			zip.putNextEntry(new ZipEntry(NOMBRE_INDICE));
			zip.write(indice.contenido().getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();

			zip.putNextEntry(new ZipEntry(NOMBRE_MANIFIESTO));
			zip.write(String.join("\n", manifiesto).getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();
		}

		long incluidos = items.stream().filter(item -> item.getMotivoOmision() == null).count();
		lote.setCantidadDocumentos((int) incluidos);
		lote.setCantidadOmitidos(items.size() - (int) incluidos);
		return salida.toByteArray();
	}

	private void completarItem(LoteExportacion lote, ItemExportacion item) {
		Documento documento = item.getDocumento();
		item.setHallazgos((int) hallazgoValidacionRepository.contarPorDocumento(documento.getId()));

		ArchivoDocumento archivo = archivoDocumentoRepository.buscarOriginalVigente(documento.getId()).orElse(null);
		if (archivo == null) {
			item.setMotivoOmision("El documento no tiene archivo original almacenado");
			item.setNombreEnArchivo(null);
			return;
		}
		if (archivo.isEnCuarentena()) {
			item.setMotivoOmision("El archivo esta en cuarentena por el antivirus y no se exporta");
			item.setNombreEnArchivo(null);
			return;
		}
		item.setMotivoOmision(null);
		item.setTamanoBytes(archivo.getTamano());
		item.setSha256(archivo.getChecksum());
		item.setNombreEnArchivo(CARPETA_DOCUMENTOS + nombreSeguro(documento, archivo));
	}

	private String claveDe(ItemExportacion item) {
		return archivoDocumentoRepository.buscarOriginalVigente(item.getDocumento().getId()).orElseThrow()
				.getClaveObjeto();
	}

	private String nombreSeguro(Documento documento, ArchivoDocumento archivo) {
		String base = archivo.getNombreArchivo() == null ? documento.getId() : archivo.getNombreArchivo();
		String limpio = base.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\.\\.+", "_");
		return documento.getId().substring(0, 8) + "-" + limpio;
	}

	private List<Object> filaDe(LoteExportacion lote, ItemExportacion item) {
		return List.of(lote.getId(), texto(item.getTipoObjeto()), texto(item.getIdObjeto()),
				texto(item.getCodigoPlantilla()), item.getNumeroVersionPlantilla(), item.getDocumento().getId(),
				texto(item.getRecibido()), texto(item.getCerrado()), texto(actoresDe(item)),
				texto(item.getNombreEnArchivo()), item.getTamanoBytes(), texto(item.getSha256()),
				texto(item.getEstadoDocumento()), item.getHallazgos(), texto(item.getMotivoOmision()));
	}

	private String actoresDe(ItemExportacion item) {
		Documento documento = item.getDocumento();
		List<String> actores = new ArrayList<>();
		if (documento.getIngresadoPor() != null) {
			actores.add("ingreso=" + documento.getIngresadoPor().getEmail());
		}
		if (documento.getRemitente() != null && !documento.getRemitente().isBlank()) {
			actores.add("remitente=" + documento.getRemitente());
		}
		return String.join(" | ", actores);
	}

	private String texto(Object valor) {
		return valor == null ? "" : valor.toString();
	}

	private java.util.Map<String, Object> datosDelLote(LoteExportacion lote) {
		java.util.Map<String, Object> datos = new java.util.LinkedHashMap<>();
		datos.put("loteId", lote.getId());
		datos.put("nombreArchivo", lote.getNombreArchivo());
		datos.put("cantidadDocumentos", lote.getCantidadDocumentos());
		datos.put("cantidadOmitidos", lote.getCantidadOmitidos());
		datos.put("tamanoBytes", lote.getTamanoBytes());
		datos.put("sha256", lote.getSha256());
		datos.put("venceEn", lote.getVenceEn() == null ? null : lote.getVenceEn().toString());
		return datos;
	}
}
