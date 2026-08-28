package com.nextdocs.ai.integracion;

import static com.nextdocs.ai.integracion.FabricaDatosPrueba.campo;
import static com.nextdocs.ai.integracion.FabricaDatosPrueba.pdf;
import static com.nextdocs.ai.integracion.FabricaDatosPrueba.regla;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.EjecucionExtraccion;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.entidades.VersionPlantilla;
import com.nextdocs.ai.enumeraciones.EstadoDocumento;
import com.nextdocs.ai.enumeraciones.EstadoExcepcion;
import com.nextdocs.ai.enumeraciones.PresenciaCampo;
import com.nextdocs.ai.enumeraciones.SeveridadHallazgo;
import com.nextdocs.ai.enumeraciones.TipoDatoCampo;
import com.nextdocs.ai.enumeraciones.TipoExcepcion;
import com.nextdocs.ai.enumeraciones.TipoReglaValidacion;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.modelos.DocumentoModel;
import com.nextdocs.ai.modelos.NuevoDocumentoReqModel;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.repositorios.EjecucionExtraccionRepository;
import com.nextdocs.ai.repositorios.ExcepcionDocumentalRepository;
import com.nextdocs.ai.repositorios.HallazgoValidacionRepository;
import com.nextdocs.ai.repositorios.EjecucionValidacionRepository;
import com.nextdocs.ai.repositorios.ValorExtraidoRepository;
import com.nextdocs.ai.servicios.DocumentoService;
import com.nextdocs.ai.servicios.ExtractorDocumentalService;
import com.nextdocs.ai.servicios.IngestaDocumentalService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

class CicloDocumentalIT extends PruebaIntegracion {

	@Autowired
	private FabricaDatosPrueba fabrica;

	@Autowired
	private ProveedorPruebaService proveedor;

	@Autowired
	private IngestaDocumentalService ingestaDocumentalService;

	@Autowired
	private ExtractorDocumentalService extractorDocumentalService;

	@Autowired
	private DocumentoService documentoService;

	@Autowired
	private DocumentoRepository documentoRepository;

	@Autowired
	private ValorExtraidoRepository valorExtraidoRepository;

	@Autowired
	private EjecucionExtraccionRepository ejecucionExtraccionRepository;

	@Autowired
	private EjecucionValidacionRepository ejecucionValidacionRepository;

	@Autowired
	private HallazgoValidacionRepository hallazgoValidacionRepository;

	@Autowired
	private ExcepcionDocumentalRepository excepcionDocumentalRepository;

	private Tenant tenant;

	private Usuario administrador;

	@BeforeEach
	void preparar() {
		proveedor.reiniciar();
		tenant = fabrica.crearTenant("t" + UUID.randomUUID().toString().substring(0, 8));
		administrador = fabrica.administradorDe(tenant);
	}

	@Test
	@DisplayName("QA1-01: el mismo archivo con la misma clave de idempotencia produce un unico documento")
	void qa101Idempotencia() {
		VersionPlantilla version = plantillaSimple("REMITO_QA101");
		NuevoDocumentoReqModel datos = datos(version);

		DocumentoModel primero = ingestaDocumentalService.ingresar(tenant, administrador,
				pdf("remito.pdf", List.of("REMITO A")), datos, "clave-repetida");
		DocumentoModel segundo = ingestaDocumentalService.ingresar(tenant, administrador,
				pdf("remito.pdf", List.of("REMITO A")), datos, "clave-repetida");

		assertThat(segundo.getId()).isEqualTo(primero.getId());
		assertThat(documentoService.listar(tenant.getId(), null, null, null, null, null, null, true,
				PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
	}

	@Test
	@DisplayName("QA1-03: confianza 1.0 en todos los campos no aprueba si una regla no se cumple")
	void qa103LaConfianzaNoAprueba() {
		VersionPlantilla version = fabrica.crearPlantillaPublicada(tenant, "REMITO_QA103",
				List.of(campo("numeroRemito", TipoDatoCampo.TEXTO, true),
						campo("conformidad", TipoDatoCampo.TEXTO, false)),
				List.of(regla("CONFORMIDAD_AFIRMATIVA", TipoReglaValidacion.CATALOGO,
						SeveridadHallazgo.REQUIERE_REVISION, "conformidad", "{\"valores\":[\"true\"]}")),
				new BigDecimal("0.5000"));
		proveedor.programar("numeroRemito", PresenciaCampo.PRESENTE, "R-0001", "1.0");
		proveedor.programar("conformidad", PresenciaCampo.PRESENTE, "false", "1.0");

		Documento documento = procesar(version, "qa103");

		assertThat(valorExtraidoRepository.listarUltimosPorDocumento(documento.getId()))
				.allMatch(valor -> valor.getConfianza().compareTo(new BigDecimal("1.0")) == 0);
		assertThat(documento.getEstado()).isEqualTo(EstadoDocumento.OBSERVADO);
		assertThat(ejecucionValidacionRepository.listarPorDocumento(documento.getId()).get(0).isAutoaprobado())
				.isFalse();
	}

	@Test
	@DisplayName("QA1-04: un 429 reintentable no pierde el documento y el reproceso lo completa")
	void qa104CuotaProveedor() {
		VersionPlantilla version = plantillaSimple("REMITO_QA104");
		proveedor.programarFallos(1, true);

		Documento documento = ingresar(version, "qa104");
		extractorDocumentalService.procesar(documento.getId());

		Documento trasFallo = documentoRepository.findById(documento.getId()).orElseThrow();
		assertThat(trasFallo.getEstado()).isEqualTo(EstadoDocumento.RECIBIDO);
		assertThat(trasFallo.getReintentosExtraccion()).isEqualTo(1);

		extractorDocumentalService.procesar(documento.getId());

		Documento trasReintento = documentoRepository.findById(documento.getId()).orElseThrow();
		assertThat(trasReintento.getEstado()).isIn(EstadoDocumento.APROBADO, EstadoDocumento.VALIDADO);
		assertThat(proveedor.cantidadLlamadas()).isEqualTo(2);
	}

	@Test
	@DisplayName("QA1-04: agotados los reintentos queda una excepcion de cuota bloqueante")
	void qa104ReintentosAgotados() {
		VersionPlantilla version = plantillaSimple("REMITO_QA104B");
		proveedor.programarFallos(10, true);

		Documento documento = ingresar(version, "qa104b");
		for (int intento = 0; intento < 3; intento++) {
			extractorDocumentalService.procesar(documento.getId());
		}

		assertThat(excepcionDocumentalRepository.listarPorDocumento(documento.getId()))
				.anyMatch(excepcion -> excepcion.getTipo() == TipoExcepcion.CUOTA_PROVEEDOR
						&& excepcion.getSeveridad() == SeveridadHallazgo.BLOQUEANTE);
	}

	@Test
	@DisplayName("QA1-07: un campo requerido ILEGIBLE bloquea, y NO_FIGURA produce un hallazgo distinto")
	void qa107IlegibleDistintoDeNoFigura() {
		VersionPlantilla version = fabrica.crearPlantillaPublicada(tenant, "REMITO_QA107",
				List.of(campo("numeroRemito", TipoDatoCampo.TEXTO, true),
						campo("observaciones", TipoDatoCampo.TEXTO, false)),
				List.of(), new BigDecimal("0.5000"));
		proveedor.programar("numeroRemito", PresenciaCampo.ILEGIBLE, null, "0.30");
		proveedor.programar("observaciones", PresenciaCampo.NO_FIGURA, null, "1.0");

		Documento documento = procesar(version, "qa107");

		var ejecucion = ejecucionValidacionRepository.listarPorDocumento(documento.getId()).get(0);
		var hallazgos = hallazgoValidacionRepository.listarPorEjecucion(ejecucion.getId());

		assertThat(hallazgos).hasSize(1);
		assertThat(hallazgos.get(0).getCodigoRegla()).isEqualTo("CAMPO_ILEGIBLE");
		assertThat(hallazgos.get(0).getSeveridad()).isEqualTo(SeveridadHallazgo.BLOQUEANTE);
		assertThat(hallazgos.get(0).getClaveCampo()).isEqualTo("numeroRemito");
	}

	@Test
	@DisplayName("QA1-07: un campo requerido que NO_FIGURA da CAMPO_REQUERIDO, no CAMPO_ILEGIBLE")
	void qa107CampoRequeridoAusente() {
		VersionPlantilla version = fabrica.crearPlantillaPublicada(tenant, "REMITO_QA107B",
				List.of(campo("numeroRemito", TipoDatoCampo.TEXTO, true)), List.of(), new BigDecimal("0.5000"));
		proveedor.programar("numeroRemito", PresenciaCampo.NO_FIGURA, null, "1.0");

		Documento documento = procesar(version, "qa107b");

		var ejecucion = ejecucionValidacionRepository.listarPorDocumento(documento.getId()).get(0);
		var hallazgos = hallazgoValidacionRepository.listarPorEjecucion(ejecucion.getId());

		assertThat(hallazgos).hasSize(1);
		assertThat(hallazgos.get(0).getCodigoRegla()).isEqualTo("CAMPO_REQUERIDO");
	}

	@Test
	@DisplayName("un documento sin hallazgos y con confianza sobre el umbral se autoaprueba")
	void autoaprobacion() {
		VersionPlantilla version = plantillaSimple("REMITO_OK");

		Documento documento = procesar(version, "ok");

		assertThat(documento.getEstado()).isEqualTo(EstadoDocumento.APROBADO);
		assertThat(ejecucionValidacionRepository.listarPorDocumento(documento.getId()).get(0).isAutoaprobado())
				.isTrue();
	}

	@Test
	@DisplayName("la confianza por debajo del umbral del campo manda a revision aunque no haya reglas")
	void confianzaBajaObserva() {
		VersionPlantilla version = fabrica.crearPlantillaPublicada(tenant, "REMITO_CONF", List.of(campoConUmbral()),
				List.of(), new BigDecimal("0.5000"));
		proveedor.programar("numeroRemito", PresenciaCampo.PRESENTE, "R-0001", "0.60");

		Documento documento = procesar(version, "conf");

		assertThat(documento.getEstado()).isEqualTo(EstadoDocumento.OBSERVADO);
		var ejecucion = ejecucionValidacionRepository.listarPorDocumento(documento.getId()).get(0);
		assertThat(hallazgoValidacionRepository.listarPorEjecucion(ejecucion.getId()).get(0).getCodigoRegla())
				.isEqualTo("CONFIANZA_BAJA");
	}

	@Test
	@DisplayName("SEC-01: un tenant no puede leer un documento de otro tenant")
	void sec01AislamientoCrossTenant() {
		VersionPlantilla version = plantillaSimple("REMITO_SEC01");
		Documento documento = ingresar(version, "sec01");

		Tenant intruso = fabrica.crearTenant("intruso" + UUID.randomUUID().toString().substring(0, 6));

		assertThatThrownBy(() -> documentoService.buscarEntidad(intruso.getId(), documento.getId()))
				.isInstanceOf(EntidadNoEncontradaException.class);
		assertThat(documentoService.listar(intruso.getId(), null, null, null, null, null, null, true,
				PageRequest.of(0, 10)).getTotalElements()).isZero();
	}

	@Test
	@DisplayName("GOV-01: la extraccion guarda proveedor, modelo y versiones de prompt y esquema")
	void gov01LinajeDeLaExtraccion() {
		VersionPlantilla version = plantillaSimple("REMITO_GOV01");

		Documento documento = procesar(version, "gov01");

		List<EjecucionExtraccion> ejecuciones = ejecucionExtraccionRepository
				.listarPorDocumento(documento.getId());
		assertThat(ejecuciones).hasSize(1);
		EjecucionExtraccion ejecucion = ejecuciones.get(0);
		assertThat(ejecucion.getProveedor()).isNotNull();
		assertThat(ejecucion.getModelo()).isEqualTo("prueba-v1");
		assertThat(ejecucion.getVersionPrompt()).isEqualTo("p1");
		assertThat(ejecucion.getVersionEsquema()).isEqualTo("e1");
		assertThat(ejecucion.getCorrelacionId()).isNotBlank();
	}

	@Test
	@DisplayName("la excepcion de validacion se deduplica en vez de acumularse en cada reproceso")
	void excepcionDeduplicada() {
		VersionPlantilla version = fabrica.crearPlantillaPublicada(tenant, "REMITO_DEDUP",
				List.of(campo("numeroRemito", TipoDatoCampo.TEXTO, true)), List.of(), new BigDecimal("0.5000"));
		proveedor.programar("numeroRemito", PresenciaCampo.NO_FIGURA, null, "1.0");

		Documento documento = ingresar(version, "dedup");
		extractorDocumentalService.procesar(documento.getId());
		documentoService.reprocesar(tenant.getId(), documento.getId());
		extractorDocumentalService.procesar(documento.getId());

		assertThat(excepcionDocumentalRepository.listarPorDocumento(documento.getId()).stream()
				.filter(excepcion -> excepcion.getEstado() != EstadoExcepcion.RESUELTA).count()).isEqualTo(1);
	}

	private VersionPlantilla plantillaSimple(String codigo) {
		return fabrica.crearPlantillaPublicada(tenant, codigo,
				List.of(campo("numeroRemito", TipoDatoCampo.TEXTO, true)), List.of(), new BigDecimal("0.9000"));
	}

	private com.nextdocs.ai.entidades.CampoPlantilla campoConUmbral() {
		var campo = campo("numeroRemito", TipoDatoCampo.TEXTO, true);
		campo.setUmbralConfianza(new BigDecimal("0.9000"));
		return campo;
	}

	private NuevoDocumentoReqModel datos(VersionPlantilla version) {
		NuevoDocumentoReqModel datos = new NuevoDocumentoReqModel();
		datos.setCodigoPlantilla(version.getPlantilla().getCodigo());
		return datos;
	}

	private Documento ingresar(VersionPlantilla version, String clave) {
		DocumentoModel modelo = ingestaDocumentalService.ingresar(tenant, administrador,
				pdf("remito.pdf", List.of("REMITO " + clave)), datos(version), clave);
		return documentoRepository.findById(modelo.getId()).orElseThrow();
	}

	private Documento procesar(VersionPlantilla version, String clave) {
		Documento documento = ingresar(version, clave);
		extractorDocumentalService.procesar(documento.getId());
		return documentoRepository.findById(documento.getId()).orElseThrow();
	}
}
