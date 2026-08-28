package com.nextdocs.ai.integracion;

import static com.nextdocs.ai.integracion.FabricaDatosPrueba.campo;
import static com.nextdocs.ai.integracion.FabricaDatosPrueba.pdf;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.entidades.VersionPlantilla;
import com.nextdocs.ai.enumeraciones.EstadoDocumento;
import com.nextdocs.ai.enumeraciones.EstrategiaSegmentacion;
import com.nextdocs.ai.enumeraciones.ResultadoAsociacion;
import com.nextdocs.ai.enumeraciones.TipoDatoCampo;
import com.nextdocs.ai.enumeraciones.TipoExcepcion;
import com.nextdocs.ai.modelos.DocumentoModel;
import com.nextdocs.ai.modelos.NuevoDocumentoReqModel;
import com.nextdocs.ai.modelos.ResultadoAsociacionModel;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.repositorios.EjecucionExtraccionRepository;
import com.nextdocs.ai.repositorios.ExcepcionDocumentalRepository;
import com.nextdocs.ai.repositorios.SegmentoDocumentoRepository;
import com.nextdocs.ai.repositorios.ValorExtraidoRepository;
import com.nextdocs.ai.repositorios.VersionPlantillaRepository;
import com.nextdocs.ai.servicios.AsociacionService;
import com.nextdocs.ai.servicios.ExtractorDocumentalService;
import com.nextdocs.ai.servicios.IngestaDocumentalService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AsociacionYSegmentacionIT extends PruebaIntegracion {

	@Autowired
	private FabricaDatosPrueba fabrica;

	@Autowired
	private ProveedorPruebaService proveedor;

	@Autowired
	private ConectorPruebaService conector;

	@Autowired
	private IngestaDocumentalService ingestaDocumentalService;

	@Autowired
	private ExtractorDocumentalService extractorDocumentalService;

	@Autowired
	private AsociacionService asociacionService;

	@Autowired
	private DocumentoRepository documentoRepository;

	@Autowired
	private SegmentoDocumentoRepository segmentoDocumentoRepository;

	@Autowired
	private EjecucionExtraccionRepository ejecucionExtraccionRepository;

	@Autowired
	private ValorExtraidoRepository valorExtraidoRepository;

	@Autowired
	private ExcepcionDocumentalRepository excepcionDocumentalRepository;

	@Autowired
	private VersionPlantillaRepository versionPlantillaRepository;

	private Tenant tenant;

	private Usuario administrador;

	private String codigoActual;

	@BeforeEach
	void preparar() {
		proveedor.reiniciar();
		conector.reiniciar();
		tenant = fabrica.crearTenant("t" + UUID.randomUUID().toString().substring(0, 8));
		administrador = fabrica.administradorDe(tenant);
	}

	@Test
	@DisplayName("QA1-02: un PDF con 10 remitos produce un padre DIVIDIDO y 10 hijos con su rango de paginas")
	void qa102SplitDeDiezRemitos() {
		VersionPlantilla version = plantillaConSegmentacion();
		List<String> paginas = List.of("REMITO R-01", "REMITO R-02", "REMITO R-03", "REMITO R-04", "REMITO R-05",
				"REMITO R-06", "REMITO R-07", "REMITO R-08", "REMITO R-09", "REMITO R-10");

		DocumentoModel modelo = ingestaDocumentalService.ingresar(tenant, administrador,
				pdf("lote.pdf", paginas), datos(codigoActual), "qa102-" + UUID.randomUUID());
		extractorDocumentalService.procesar(modelo.getId());

		Documento padre = documentoRepository.findById(modelo.getId()).orElseThrow();
		assertThat(padre.getEstado()).isEqualTo(EstadoDocumento.DIVIDIDO);
		assertThat(padre.getCantidadSegmentos()).isEqualTo(10);
		assertThat(ejecucionExtraccionRepository.listarPorDocumento(padre.getId())).isEmpty();

		List<Documento> hijos = documentoRepository.listarSegmentos(padre.getId());
		assertThat(hijos).hasSize(10);
		for (int indice = 0; indice < hijos.size(); indice++) {
			Documento hijo = hijos.get(indice);
			assertThat(hijo.getPaginaDesde()).isEqualTo(indice + 1);
			assertThat(hijo.getPaginaHasta()).isEqualTo(indice + 1);
			assertThat(hijo.getVersionPlantilla().getId()).isEqualTo(version.getId());
			assertThat(hijo.getClaveIdempotencia()).endsWith("#segmento-" + (indice + 1));
		}
		assertThat(segmentoDocumentoRepository.listarPorPadre(padre.getId())).hasSize(10);
	}

	@Test
	@DisplayName("QA1-02: cada hijo se extrae por separado con su propia ejecucion")
	void qa102CadaHijoSeExtraeSolo() {
		VersionPlantilla version = plantillaConSegmentacion();

		DocumentoModel modelo = ingestaDocumentalService.ingresar(tenant, administrador,
				pdf("lote.pdf", List.of("REMITO A", "REMITO B", "REMITO C")), datos(codigoActual),
				"qa102b-" + UUID.randomUUID());
		extractorDocumentalService.procesar(modelo.getId());

		List<Documento> hijos = documentoRepository.listarSegmentos(modelo.getId());
		for (Documento hijo : hijos) {
			extractorDocumentalService.procesar(hijo.getId());
		}

		assertThat(proveedor.cantidadLlamadas()).isEqualTo(3);
		for (Documento hijo : hijos) {
			assertThat(ejecucionExtraccionRepository.listarPorDocumento(hijo.getId())).hasSize(1);
			assertThat(valorExtraidoRepository.listarUltimosPorDocumento(hijo.getId())).isNotEmpty();
		}
	}

	@Test
	@DisplayName("un candidato unico por encima del umbral se asocia solo y fija la referencia externa")
	void candidatoUnicoSeAsociaSolo() {
		VersionPlantilla version = plantillaSimple("REMITO_UNO");
		fabrica.registrarConectorPrueba(tenant, "0.9500", "0.5000");
		conector.agregarCandidato("ped-1", "Pedido uno", "1.0000");

		Documento documento = procesar(version, "uno");

		assertThat(documento.getReferenciaSujeto().getIdObjeto()).isEqualTo("ped-1");
		assertThat(documento.getReferenciaSujeto().getOrigen()).isEqualTo(ConectorPruebaService.CODIGO);
		assertThat(documento.getEstado()).isEqualTo(EstadoDocumento.APROBADO);
	}

	@Test
	@DisplayName("QA1-06: dos candidatos compatibles van a revision humana, el sistema no elige")
	void qa106DosCandidatos() {
		VersionPlantilla version = plantillaSimple("REMITO_DOS");
		fabrica.registrarConectorPrueba(tenant, "0.9500", "0.5000");
		conector.agregarCandidato("ped-1", "Pedido uno", "1.0000");
		conector.agregarCandidato("ped-2", "Pedido dos", "1.0000");

		Documento documento = procesar(version, "dos");

		assertThat(documento.getReferenciaSujeto()).satisfiesAnyOf(
				referencia -> assertThat(referencia).isNull(),
				referencia -> assertThat(referencia.getIdObjeto()).isNull());
		assertThat(documento.getEstado()).isEqualTo(EstadoDocumento.OBSERVADO);
		assertThat(excepcionDocumentalRepository.listarPorDocumento(documento.getId()))
				.anyMatch(excepcion -> excepcion.getTipo() == TipoExcepcion.ASOCIACION
						&& AsociacionService.CODIGO_ASOCIACION_AMBIGUA.equals(excepcion.getCodigo()));
	}

	@Test
	@DisplayName("QA1-06: la seleccion humana fija la referencia y descarta el resto")
	void qa106SeleccionHumana() {
		VersionPlantilla version = plantillaSimple("REMITO_SELECCION");
		fabrica.registrarConectorPrueba(tenant, "0.9500", "0.5000");
		conector.agregarCandidato("ped-1", "Pedido uno", "1.0000");
		conector.agregarCandidato("ped-2", "Pedido dos", "1.0000");
		Documento documento = procesar(version, "seleccion");

		List<com.nextdocs.ai.modelos.CandidatoAsociacionModel> candidatos = asociacionService
				.listar(tenant.getId(), documento.getId());
		String elegido = candidatos.stream().filter(c -> "ped-2".equals(c.getIdObjeto())).findFirst().orElseThrow()
				.getId();

		ResultadoAsociacionModel resultado = asociacionService.seleccionar(tenant.getId(), documento.getId(),
				elegido, administrador, "Verificado contra el remito fisico");

		assertThat(resultado.getResultado()).isEqualTo(ResultadoAsociacion.RESUELTA);
		assertThat(documentoRepository.findById(documento.getId()).orElseThrow().getReferenciaSujeto()
				.getIdObjeto()).isEqualTo("ped-2");
		assertThat(asociacionService.listar(tenant.getId(), documento.getId()))
				.filteredOn(c -> "ped-1".equals(c.getIdObjeto())).allMatch(c -> c.isDescartado());
	}

	@Test
	@DisplayName("QA1-05: un conector caido produce excepcion CONECTOR, distinta de cero candidatos")
	void qa105ConectorCaido() {
		VersionPlantilla version = plantillaSimple("REMITO_CAIDO");
		fabrica.registrarConectorPrueba(tenant, "0.9500", "0.5000");
		conector.programarFallo(true);

		Documento documento = procesar(version, "caido");

		assertThat(documento.getEstado()).isEqualTo(EstadoDocumento.OBSERVADO);
		assertThat(excepcionDocumentalRepository.listarPorDocumento(documento.getId()))
				.anyMatch(excepcion -> excepcion.getTipo() == TipoExcepcion.CONECTOR);
	}

	@Test
	@DisplayName("QA1-05: cero candidatos NO abre excepcion de conector y el documento sigue su curso")
	void qa105CeroCandidatosNoEsFallo() {
		VersionPlantilla version = plantillaSimple("REMITO_CERO");
		fabrica.registrarConectorPrueba(tenant, "0.9500", "0.5000");

		Documento documento = procesar(version, "cero");

		assertThat(excepcionDocumentalRepository.listarPorDocumento(documento.getId()))
				.noneMatch(excepcion -> excepcion.getTipo() == TipoExcepcion.CONECTOR);
		assertThat(documento.getEstado()).isEqualTo(EstadoDocumento.APROBADO);
	}

	@Test
	@DisplayName("QA-FOL-01: con el conector caido el nucleo documental igual extrae y valida")
	void qaFol01ElNucleoSigueSinConector() {
		VersionPlantilla version = plantillaSimple("REMITO_FOL01");
		fabrica.registrarConectorPrueba(tenant, "0.9500", "0.5000");
		conector.programarFallo(true);

		Documento documento = procesar(version, "fol01");

		assertThat(ejecucionExtraccionRepository.listarPorDocumento(documento.getId())).hasSize(1);
		assertThat(valorExtraidoRepository.listarUltimosPorDocumento(documento.getId())).isNotEmpty();
	}

	private VersionPlantilla plantillaSimple(String codigo) {
		codigoActual = codigo;
		return fabrica.crearPlantillaPublicada(tenant, codigo,
				List.of(campo("numeroRemito", TipoDatoCampo.TEXTO, true)), List.of(), new BigDecimal("0.9000"));
	}

	private VersionPlantilla plantillaConSegmentacion() {
		VersionPlantilla version = plantillaSimple("REMITO_LOTE_" + UUID.randomUUID().toString().substring(0, 6));
		version.setEstrategiaSegmentacion(EstrategiaSegmentacion.PATRON_TEXTO);
		version.setPatronInicioDocumento("^\\s*REMITO");
		return versionPlantillaRepository.save(version);
	}

	private NuevoDocumentoReqModel datos(String codigoPlantilla) {
		NuevoDocumentoReqModel datos = new NuevoDocumentoReqModel();
		datos.setCodigoPlantilla(codigoPlantilla);
		return datos;
	}

	private Documento procesar(VersionPlantilla version, String clave) {
		DocumentoModel modelo = ingestaDocumentalService.ingresar(tenant, administrador,
				pdf("remito.pdf", List.of("REMITO " + clave)), datos(codigoActual),
				clave + "-" + UUID.randomUUID());
		extractorDocumentalService.procesar(modelo.getId());
		return documentoRepository.findById(modelo.getId()).orElseThrow();
	}
}
