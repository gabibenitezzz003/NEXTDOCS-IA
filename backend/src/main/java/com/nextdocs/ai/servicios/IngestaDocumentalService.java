package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.nextdocs.ai.config.PropiedadesIngesta;
import com.nextdocs.ai.convertidores.DocumentoConverter;
import com.nextdocs.ai.entidades.ArchivoDocumento;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.PlantillaDocumental;
import com.nextdocs.ai.entidades.ReferenciaExterna;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.EstadoDocumento;
import com.nextdocs.ai.enumeraciones.OrigenDocumento;
import com.nextdocs.ai.enumeraciones.SeveridadHallazgo;
import com.nextdocs.ai.enumeraciones.TipoExcepcion;
import com.nextdocs.ai.enumeraciones.TipoEventoCanonico;
import com.nextdocs.ai.exceptions.ArchivoRechazadoException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.DocumentoModel;
import com.nextdocs.ai.modelos.NuevoDocumentoReqModel;
import com.nextdocs.ai.modelos.ResultadoEscaneoModel;
import com.nextdocs.ai.repositorios.ArchivoDocumentoRepository;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.repositorios.PlantillaDocumentalRepository;
import com.nextdocs.ai.utiles.ClaveObjeto;
import com.nextdocs.ai.utiles.ContextoCorrelacion;
import com.nextdocs.ai.utiles.Hash;
import com.nextdocs.ai.utiles.InspectorArchivo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class IngestaDocumentalService {

	public static final String ENTIDAD = "Documento";

	public static final String CODIGO_ARCHIVO_INFECTADO = "ARCHIVO_INFECTADO";

	private static final Logger log = LoggerFactory.getLogger(IngestaDocumentalService.class);

	private final DocumentoRepository documentoRepository;

	private final ArchivoDocumentoRepository archivoDocumentoRepository;

	private final PlantillaDocumentalRepository plantillaDocumentalRepository;

	private final AlmacenamientoService almacenamientoService;

	private final ColaExtraccionService colaExtraccionService;

	private final AuditoriaService auditoriaService;

	private final EventoSalidaService eventoSalidaService;

	private final PropiedadesIngesta propiedades;

	private final DocumentoConverter documentoConverter;

	private final EscaneoArchivoService escaneoArchivoService;

	private final ExcepcionDocumentalService excepcionDocumentalService;

	private final OriginalFisicoService originalFisicoService;

	public IngestaDocumentalService(DocumentoRepository documentoRepository,
			ArchivoDocumentoRepository archivoDocumentoRepository,
			PlantillaDocumentalRepository plantillaDocumentalRepository, AlmacenamientoService almacenamientoService,
			ColaExtraccionService colaExtraccionService, AuditoriaService auditoriaService,
			EventoSalidaService eventoSalidaService, PropiedadesIngesta propiedades,
			DocumentoConverter documentoConverter, EscaneoArchivoService escaneoArchivoService,
			ExcepcionDocumentalService excepcionDocumentalService, OriginalFisicoService originalFisicoService) {
		this.documentoRepository = documentoRepository;
		this.archivoDocumentoRepository = archivoDocumentoRepository;
		this.plantillaDocumentalRepository = plantillaDocumentalRepository;
		this.almacenamientoService = almacenamientoService;
		this.colaExtraccionService = colaExtraccionService;
		this.auditoriaService = auditoriaService;
		this.eventoSalidaService = eventoSalidaService;
		this.propiedades = propiedades;
		this.documentoConverter = documentoConverter;
		this.escaneoArchivoService = escaneoArchivoService;
		this.excepcionDocumentalService = excepcionDocumentalService;
		this.originalFisicoService = originalFisicoService;
	}

	@Transactional
	public DocumentoModel ingresar(Tenant tenant, Usuario usuario, MultipartFile archivo,
			NuevoDocumentoReqModel datos, String claveIdempotencia) {
		byte[] contenido = leer(archivo);
		String hashContenido = Hash.sha256(contenido);
		String claveEfectiva = claveIdempotencia == null || claveIdempotencia.isBlank() ? hashContenido
				: claveIdempotencia.trim();

		Optional<Documento> existente = documentoRepository.buscarPorClaveIdempotencia(tenant.getId(), claveEfectiva);
		if (existente.isPresent()) {
			log.info("Ingreso idempotente resuelto para la clave {}", claveEfectiva);
			return documentoConverter.aModelo(existente.get(),
					archivoDocumentoRepository.listarPorDocumento(existente.get().getId()));
		}

		validarArchivo(archivo, contenido);
		String tipoMime = InspectorArchivo.detectarTipoMime(contenido, archivo.getOriginalFilename());
		validarTipoMime(tipoMime);

		ResultadoEscaneoModel escaneo = escaneoArchivoService.escanear(contenido, archivo.getOriginalFilename());

		Documento documento = construirDocumento(tenant, usuario, archivo, datos, hashContenido, claveEfectiva);
		if (escaneo.estaInfectado()) {
			documento.setEstado(EstadoDocumento.RECHAZADO);
			documento.setObservacion("Archivo rechazado por el antivirus: " + escaneo.getAmenaza());
		}
		documentoRepository.save(documento);

		int paginas = escaneo.estaInfectado() ? 0 : InspectorArchivo.contarPaginas(contenido, tipoMime);
		String claveObjeto = ClaveObjeto.paraDocumento(tenant.getId(), documento.getId(),
				archivo.getOriginalFilename());
		if (escaneo.estaInfectado()) {
			almacenamientoService.guardarEnCuarentena(claveObjeto, contenido, tipoMime);
		} else {
			almacenamientoService.guardarDocumento(claveObjeto, contenido, tipoMime);
		}

		ArchivoDocumento archivoDocumento = construirArchivo(tenant, documento, archivo, contenido, tipoMime,
				paginas, claveObjeto);
		aplicarEscaneo(archivoDocumento, escaneo);
		archivoDocumentoRepository.save(archivoDocumento);

		if (escaneo.estaInfectado()) {
			auditoriaService.registrarFallo(tenant.getId(), AccionAuditoria.DOCUMENTO_INGRESADO, ENTIDAD,
					documento.getId(), Map.of("amenaza", escaneo.getAmenaza(), "motor", escaneo.getMotor(),
							"hash", hashContenido));
			excepcionDocumentalService.abrir(tenant, documento, TipoExcepcion.SEGURIDAD,
					SeveridadHallazgo.BLOQUEANTE, CODIGO_ARCHIVO_INFECTADO,
					"El archivo contiene la amenaza " + escaneo.getAmenaza() + " y quedo en cuarentena");
			eventoSalidaService.publicarDeDocumento(documento, TipoEventoCanonico.DOCUMENTO_RECHAZADO);
			log.warn("Archivo infectado en cuarentena: documento {} amenaza {}", documento.getId(),
					escaneo.getAmenaza());
			return documentoConverter.aModelo(documento,
					archivoDocumentoRepository.listarPorDocumento(documento.getId()));
		}

		auditoriaService.registrarConDetalle(tenant.getId(), AccionAuditoria.DOCUMENTO_INGRESADO, ENTIDAD,
				documento.getId(), Map.of("origen", documento.getOrigen(), "hash", hashContenido, "paginas", paginas,
						"escaneo", escaneo.getResultado()));
		originalFisicoService.iniciarSiCorresponde(documento);
		eventoSalidaService.publicarDeDocumento(documento, TipoEventoCanonico.DOCUMENTO_RECIBIDO);
		encolarTrasCommit(documento.getId());
		return documentoConverter.aModelo(documento, archivoDocumentoRepository.listarPorDocumento(documento.getId()));
	}

	private Documento construirDocumento(Tenant tenant, Usuario usuario, MultipartFile archivo,
			NuevoDocumentoReqModel datos, String hashContenido, String claveIdempotencia) {
		Documento documento = new Documento();
		documento.setTenant(tenant);
		documento.setEstado(EstadoDocumento.RECIBIDO);
		documento.setOrigen(datos.getOrigen() == null ? OrigenDocumento.WEB : datos.getOrigen());
		documento.setNombre(archivo.getOriginalFilename());
		documento.setHashContenido(hashContenido);
		documento.setClaveIdempotencia(claveIdempotencia);
		documento.setCorrelacionId(ContextoCorrelacion.obtenerOGenerar());
		documento.setRemitente(datos.getRemitente());
		documento.setObservacion(datos.getObservacion());
		documento.setIngresadoPor(usuario);
		documento.setReferenciaSujeto(construirReferencia(datos));
		documento.setRecibido(Instant.now());
		documento.setAlta(Instant.now());
		asignarPlantilla(tenant, datos, documento);
		return documento;
	}

	private void asignarPlantilla(Tenant tenant, NuevoDocumentoReqModel datos, Documento documento) {
		if (datos.getCodigoPlantilla() == null || datos.getCodigoPlantilla().isBlank()) {
			return;
		}
		PlantillaDocumental plantilla = plantillaDocumentalRepository
				.buscarPorCodigo(tenant.getId(), datos.getCodigoPlantilla().trim())
				.orElseThrow(() -> new ValidacionException(
						"No existe la plantilla " + datos.getCodigoPlantilla() + " en el tenant"));
		documento.setPlantilla(plantilla);
		documento.setVersionPlantilla(plantilla.getVersionPublicada());
	}

	private ReferenciaExterna construirReferencia(NuevoDocumentoReqModel datos) {
		ReferenciaExterna referencia = new ReferenciaExterna();
		referencia.setOrigen(datos.getSujetoOrigen());
		referencia.setTipoObjeto(datos.getSujetoTipoObjeto());
		referencia.setIdObjeto(datos.getSujetoIdObjeto());
		return referencia;
	}

	private ArchivoDocumento construirArchivo(Tenant tenant, Documento documento, MultipartFile archivo,
			byte[] contenido, String tipoMime, int paginas, String claveObjeto) {
		ArchivoDocumento archivoDocumento = new ArchivoDocumento();
		archivoDocumento.setTenant(tenant);
		archivoDocumento.setDocumento(documento);
		archivoDocumento.setClaveObjeto(claveObjeto);
		archivoDocumento.setBucket(almacenamientoService.bucketDocumentos());
		archivoDocumento.setNombreArchivo(archivo.getOriginalFilename());
		archivoDocumento.setTipoMime(tipoMime);
		archivoDocumento.setExtension(ClaveObjeto.extension(archivo.getOriginalFilename()));
		archivoDocumento.setTamano(contenido.length);
		archivoDocumento.setChecksum(documento.getHashContenido());
		archivoDocumento.setPaginas(paginas);
		archivoDocumento.setVersion(1);
		archivoDocumento.setOriginal(true);
		archivoDocumento.setAlta(Instant.now());
		return archivoDocumento;
	}

	private void aplicarEscaneo(ArchivoDocumento archivo, ResultadoEscaneoModel escaneo) {
		archivo.setResultadoEscaneo(escaneo.getResultado());
		archivo.setAmenazaDetectada(escaneo.getAmenaza());
		archivo.setMotorEscaneo(escaneo.getMotor() == null ? null : escaneo.getMotor().name());
		archivo.setEscaneado(escaneo.getEscaneado());
		archivo.setEnCuarentena(escaneo.estaInfectado());
		if (escaneo.estaInfectado()) {
			archivo.setBucket(almacenamientoService.bucketCuarentena());
		}
	}

	private void validarArchivo(MultipartFile archivo, byte[] contenido) {
		if (contenido.length == 0) {
			throw new ArchivoRechazadoException("ARCHIVO_VACIO", "El archivo no tiene contenido");
		}
		if (contenido.length > propiedades.getTamanoMaximoBytes()) {
			throw new ArchivoRechazadoException("TAMANO_EXCEDIDO",
					"El archivo supera el maximo de " + propiedades.getTamanoMaximoBytes() + " bytes");
		}
		String extension = ClaveObjeto.extension(archivo.getOriginalFilename());
		if (extension == null || !propiedades.getExtensionesPermitidas().contains(extension)) {
			throw new ArchivoRechazadoException("EXTENSION_NO_PERMITIDA",
					"La extension " + extension + " no esta permitida");
		}
	}

	private void validarTipoMime(String tipoMime) {
		if (tipoMime == null
				|| !propiedades.getTiposMimePermitidos().contains(tipoMime.toLowerCase(Locale.ROOT))) {
			throw new ArchivoRechazadoException("MIME_NO_PERMITIDO",
					"El contenido real del archivo no corresponde a un tipo permitido");
		}
	}

	private byte[] leer(MultipartFile archivo) {
		try {
			return archivo.getBytes();
		} catch (Exception e) {
			throw new ArchivoRechazadoException("LECTURA_FALLIDA", "No se pudo leer el archivo recibido");
		}
	}

	private void encolarTrasCommit(String documentoId) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			colaExtraccionService.encolar(documentoId);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				colaExtraccionService.encolar(documentoId);
			}
		});
	}
}
