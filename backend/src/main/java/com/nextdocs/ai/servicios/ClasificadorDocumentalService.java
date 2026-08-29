package com.nextdocs.ai.servicios;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.nextdocs.ai.config.PropiedadesClasificacion;
import com.nextdocs.ai.entidades.ArchivoDocumento;
import com.nextdocs.ai.entidades.CampoPlantilla;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.PlantillaDocumental;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.OrigenTipoDocumento;
import com.nextdocs.ai.interfaces.ProveedorDocumentalIaInt;
import com.nextdocs.ai.modelos.ResultadoClasificacionModel;
import com.nextdocs.ai.modelos.SolicitudClasificacionModel;
import com.nextdocs.ai.modelos.TipoCandidatoModel;
import com.nextdocs.ai.repositorios.ArchivoDocumentoRepository;
import com.nextdocs.ai.repositorios.CampoPlantillaRepository;
import com.nextdocs.ai.repositorios.PlantillaDocumentalRepository;
import com.nextdocs.ai.servicios.proveedores.RuteadorProveedorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ClasificadorDocumentalService {

	public static final String CODIGO_TIPO_NO_RECONOCIDO = "TIPO_NO_RECONOCIDO";

	public static final String CODIGO_TIPO_INCIERTO = "TIPO_INCIERTO";

	public static final String CODIGO_SIN_CATALOGO = "CATALOGO_VACIO";

	private static final int MAXIMO_CAMPOS_CLAVE = 6;

	private static final Logger log = LoggerFactory.getLogger(ClasificadorDocumentalService.class);

	private final PlantillaDocumentalRepository plantillaDocumentalRepository;

	private final CampoPlantillaRepository campoPlantillaRepository;

	private final ArchivoDocumentoRepository archivoDocumentoRepository;

	private final AlmacenamientoService almacenamientoService;

	private final RuteadorProveedorService ruteadorProveedorService;

	private final AuditoriaService auditoriaService;

	private final PropiedadesClasificacion propiedades;

	public ClasificadorDocumentalService(PlantillaDocumentalRepository plantillaDocumentalRepository,
			CampoPlantillaRepository campoPlantillaRepository,
			ArchivoDocumentoRepository archivoDocumentoRepository, AlmacenamientoService almacenamientoService,
			RuteadorProveedorService ruteadorProveedorService, AuditoriaService auditoriaService,
			PropiedadesClasificacion propiedades) {
		this.plantillaDocumentalRepository = plantillaDocumentalRepository;
		this.campoPlantillaRepository = campoPlantillaRepository;
		this.archivoDocumentoRepository = archivoDocumentoRepository;
		this.almacenamientoService = almacenamientoService;
		this.ruteadorProveedorService = ruteadorProveedorService;
		this.auditoriaService = auditoriaService;
		this.propiedades = propiedades;
	}

	public boolean corresponde(Documento documento) {
		return propiedades.isActiva() && documento.getPlantilla() == null;
	}

	public Veredicto clasificar(Documento documento) {
		List<PlantillaDocumental> catalogo = publicadasDe(documento.getTenant().getId());
		if (catalogo.isEmpty()) {
			return Veredicto.sinCatalogo();
		}

		SolicitudClasificacionModel solicitud = construirSolicitud(documento, catalogo);
		ProveedorDocumentalIaInt proveedor = ruteadorProveedorService
				.resolverPrincipal(documento.getTenant().getId());
		ResultadoClasificacionModel resultado = proveedor.clasificar(solicitud);

		auditar(documento, resultado);

		if (resultado.esDesconocido()) {
			return Veredicto.noReconocido(resultado);
		}

		Optional<PlantillaDocumental> elegida = catalogo.stream()
				.filter(plantilla -> plantilla.getCodigo().equalsIgnoreCase(resultado.getCodigoPropuesto()))
				.findFirst();
		if (elegida.isEmpty()) {
			log.info("El clasificador propuso {} y no esta en el catalogo del tenant {}",
					resultado.getCodigoPropuesto(), documento.getTenant().getId());
			return Veredicto.noReconocido(resultado);
		}

		BigDecimal confianza = resultado.getConfianza() == null ? BigDecimal.ZERO : resultado.getConfianza();
		if (confianza.compareTo(propiedades.getConfianzaMinima()) < 0) {
			return Veredicto.incierto(resultado, elegida.get());
		}
		return Veredicto.resuelto(resultado, elegida.get());
	}

	private List<PlantillaDocumental> publicadasDe(String tenantId) {
		List<PlantillaDocumental> publicadas = new ArrayList<>();
		for (PlantillaDocumental plantilla : plantillaDocumentalRepository.listarPorTenant(tenantId, null)) {
			if (plantilla.getVersionPublicada() != null) {
				publicadas.add(plantilla);
			}
			if (publicadas.size() >= propiedades.getMaximoCandidatos()) {
				break;
			}
		}
		return publicadas;
	}

	private SolicitudClasificacionModel construirSolicitud(Documento documento,
			List<PlantillaDocumental> catalogo) {
		List<ArchivoDocumento> archivos = archivoDocumentoRepository.listarOriginales(documento.getId());
		if (archivos.isEmpty()) {
			throw new IllegalStateException("El documento " + documento.getId() + " no tiene archivo original");
		}
		ArchivoDocumento archivo = archivos.get(0);

		SolicitudClasificacionModel solicitud = new SolicitudClasificacionModel();
		solicitud.setTenantId(documento.getTenant().getId());
		solicitud.setDocumentoId(documento.getId());
		solicitud.setNombreArchivo(archivo.getNombreArchivo());
		solicitud.setTipoMime(archivo.getTipoMime());
		solicitud.setContenido(almacenamientoService.leerDocumento(archivo.getClaveObjeto()));
		solicitud.setPaginas(archivo.getPaginas());
		solicitud.setCorrelacionId(documento.getCorrelacionId());
		for (PlantillaDocumental plantilla : catalogo) {
			solicitud.getCandidatos().add(aCandidato(plantilla));
		}
		return solicitud;
	}

	private TipoCandidatoModel aCandidato(PlantillaDocumental plantilla) {
		TipoCandidatoModel candidato = new TipoCandidatoModel();
		candidato.setCodigo(plantilla.getCodigo());
		candidato.setNombre(plantilla.getNombre());
		candidato.setDescripcion(plantilla.getDescripcion());
		for (CampoPlantilla campo : campoPlantillaRepository
				.listarPorVersion(plantilla.getVersionPublicada().getId())) {
			if (!campo.isRequerido()) {
				continue;
			}
			candidato.getCamposClave().add(campo.getEtiqueta() == null ? campo.getClave() : campo.getEtiqueta());
			if (candidato.getCamposClave().size() >= MAXIMO_CAMPOS_CLAVE) {
				break;
			}
		}
		return candidato;
	}

	private void auditar(Documento documento, ResultadoClasificacionModel resultado) {
		Map<String, Object> detalle = new LinkedHashMap<>();
		detalle.put("propuesto", resultado.getCodigoPropuesto());
		detalle.put("confianza", resultado.getConfianza());
		detalle.put("umbral", propiedades.getConfianzaMinima());
		detalle.put("proveedor", resultado.getProveedor() == null ? null : resultado.getProveedor().name());
		detalle.put("modelo", resultado.getModelo());
		if (resultado.getMotivo() != null) {
			detalle.put("motivo", resultado.getMotivo());
		}
		auditoriaService.registrarConDetalle(documento.getTenant().getId(), AccionAuditoria.TIPO_CLASIFICADO,
				IngestaDocumentalService.ENTIDAD, documento.getId(), detalle);
	}

	public record Veredicto(Estado estado, PlantillaDocumental plantilla, ResultadoClasificacionModel resultado) {

		public enum Estado {
			RESUELTO,
			INCIERTO,
			NO_RECONOCIDO,
			SIN_CATALOGO
		}

		private static Veredicto resuelto(ResultadoClasificacionModel resultado, PlantillaDocumental plantilla) {
			return new Veredicto(Estado.RESUELTO, plantilla, resultado);
		}

		private static Veredicto incierto(ResultadoClasificacionModel resultado, PlantillaDocumental plantilla) {
			return new Veredicto(Estado.INCIERTO, plantilla, resultado);
		}

		private static Veredicto noReconocido(ResultadoClasificacionModel resultado) {
			return new Veredicto(Estado.NO_RECONOCIDO, null, resultado);
		}

		private static Veredicto sinCatalogo() {
			return new Veredicto(Estado.SIN_CATALOGO, null, null);
		}

		public boolean loResolvio() {
			return estado == Estado.RESUELTO;
		}

		public OrigenTipoDocumento origen() {
			return OrigenTipoDocumento.DETECTADO;
		}
	}
}
