package com.nextdocs.ai.integracion;

import static com.nextdocs.ai.integracion.FabricaDatosPrueba.campo;
import static com.nextdocs.ai.integracion.FabricaDatosPrueba.pdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.entidades.VersionPlantilla;
import com.nextdocs.ai.enumeraciones.DecisionRevision;
import com.nextdocs.ai.enumeraciones.EstadoDocumento;
import com.nextdocs.ai.enumeraciones.EstrategiaSegmentacion;
import com.nextdocs.ai.enumeraciones.PresenciaCampo;
import com.nextdocs.ai.enumeraciones.TipoDatoCampo;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.BarraKpiModel;
import com.nextdocs.ai.modelos.DocumentoModel;
import com.nextdocs.ai.modelos.IndicadorKpiModel;
import com.nextdocs.ai.modelos.KpiPlantillaModel;
import com.nextdocs.ai.modelos.KpiResumenModel;
import com.nextdocs.ai.modelos.NuevoDocumentoReqModel;
import com.nextdocs.ai.modelos.RevisionDocumentoReqModel;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.repositorios.VersionPlantillaRepository;
import com.nextdocs.ai.servicios.DocumentoService;
import com.nextdocs.ai.servicios.ExtractorDocumentalService;
import com.nextdocs.ai.servicios.IngestaDocumentalService;
import com.nextdocs.ai.servicios.KpiService;
import com.nextdocs.ai.servicios.RevisionDocumentalService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class KpiIT extends PruebaIntegracion {

	@Autowired
	private FabricaDatosPrueba fabrica;

	@Autowired
	private ProveedorPruebaService proveedor;

	@Autowired
	private KpiService kpiService;

	@Autowired
	private IngestaDocumentalService ingestaDocumentalService;

	@Autowired
	private ExtractorDocumentalService extractorDocumentalService;

	@Autowired
	private RevisionDocumentalService revisionDocumentalService;

	@Autowired
	private DocumentoService documentoService;

	@Autowired
	private DocumentoRepository documentoRepository;

	@Autowired
	private VersionPlantillaRepository versionPlantillaRepository;

	private Tenant tenant;

	private Usuario administrador;

	private VersionPlantilla version;

	@BeforeEach
	void preparar() {
		proveedor.reiniciar();
		tenant = fabrica.crearTenant("t" + UUID.randomUUID().toString().substring(0, 8));
		administrador = fabrica.administradorDe(tenant);
		version = fabrica.crearPlantillaPublicada(tenant, "KPI_" + UUID.randomUUID().toString().substring(0, 6),
				List.of(campo("numeroRemito", TipoDatoCampo.TEXTO, true)), List.of(), new BigDecimal("0.9000"));
	}

	@Test
	@DisplayName("con el tenant vacio todos los indicadores existen y ninguno divide por cero")
	void tenantSinDatos() {
		KpiResumenModel resumen = kpiService.resumir(tenant.getId(), null, null);

		assertThat(resumen.getIndicadores()).hasSize(11);
		assertThat(buscar(resumen, KpiService.RECIBIDOS).getValor()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(buscar(resumen, KpiService.AUTOMATIZACION).getValor()).isNull();
		assertThat(buscar(resumen, KpiService.CICLO_P50).getValor()).isNull();
		assertThat(buscar(resumen, KpiService.CUMPLIMIENTO_SLA).getValor()).isNull();
	}

	@Test
	@DisplayName("documentos recibidos cuenta lo ingresado dentro del rango")
	void cuentaRecibidos() {
		ingresar("uno");
		ingresar("dos");
		ingresar("tres");

		KpiResumenModel resumen = kpiService.resumir(tenant.getId(), null, null);

		assertThat(buscar(resumen, KpiService.RECIBIDOS).getValor()).isEqualByComparingTo(new BigDecimal("3"));
	}

	@Test
	@DisplayName("automatizacion es cerrados sin revision humana sobre cerrados")
	void automatizacion() {
		proveedor.programar("numeroRemito", PresenciaCampo.PRESENTE, "R-MANUAL", "0.30");
		Documento manual = procesar("manual");
		assertThat(manual.getEstado()).isEqualTo(EstadoDocumento.VALIDADO);
		aprobar(manual.getId());
		documentoService.cerrar(tenant.getId(), manual.getId());

		proveedor.reiniciar();
		Documento automatico = procesar("auto");
		assertThat(automatico.getEstado()).isEqualTo(EstadoDocumento.APROBADO);
		documentoService.cerrar(tenant.getId(), automatico.getId());

		IndicadorKpiModel indicador = buscar(kpiService.resumir(tenant.getId(), null, null),
				KpiService.AUTOMATIZACION);

		assertThat(indicador.getDenominador()).isEqualTo(2);
		assertThat(indicador.getNumerador()).isEqualTo(1);
		assertThat(indicador.getValor()).isEqualByComparingTo(new BigDecimal("50.0"));
		assertThat(indicador.getUnidad()).isEqualTo(IndicadorKpiModel.UNIDAD_PORCENTAJE);
	}

	@Test
	@DisplayName("cada indicador expone su formula, para que ningun numero quede sin explicacion")
	void todosTienenFormula() {
		KpiResumenModel resumen = kpiService.resumir(tenant.getId(), null, null);

		assertThat(resumen.getIndicadores()).allSatisfy(indicador -> {
			assertThat(indicador.getClave()).isNotBlank();
			assertThat(indicador.getEtiqueta()).isNotBlank();
			assertThat(indicador.getFormula()).isNotBlank();
			assertThat(indicador.getUnidad()).isNotBlank();
		});
	}

	@Test
	@DisplayName("el rango por defecto son 30 dias y el periodo anterior tiene la misma longitud")
	void rangoPorDefecto() {
		KpiResumenModel resumen = kpiService.resumir(tenant.getId(), null, null);

		assertThat(resumen.getRango().getDias()).isEqualTo(30);
		assertThat(resumen.getRango().anterior().getDias()).isEqualTo(30);
		assertThat(resumen.getRango().anterior().getHasta()).isEqualTo(resumen.getRango().getDesde());
	}

	@Test
	@DisplayName("un rango invertido se corrige en vez de devolver todo en cero sin avisar")
	void rangoInvertido() {
		Instant ahora = Instant.now();
		KpiResumenModel resumen = kpiService.resumir(tenant.getId(), ahora, ahora.minus(7, ChronoUnit.DAYS));

		assertThat(resumen.getRango().getDesde()).isBefore(resumen.getRango().getHasta());
		assertThat(resumen.getRango().getDias()).isEqualTo(7);
	}

	@Test
	@DisplayName("un documento fuera del rango no cuenta")
	void respetaElRango() {
		ingresar("dentro");
		Instant futuro = Instant.now().plus(1, ChronoUnit.DAYS);

		KpiResumenModel resumen = kpiService.resumir(tenant.getId(), futuro, futuro.plus(1, ChronoUnit.DAYS));

		assertThat(buscar(resumen, KpiService.RECIBIDOS).getValor()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	@DisplayName("sin periodo anterior con datos la tendencia queda SIN_COMPARACION y no inventa un porcentaje")
	void comparaConElPeriodoAnterior() {
		ingresar("uno");
		ingresar("dos");

		IndicadorKpiModel recibidos = buscar(kpiService.resumir(tenant.getId(), null, null), KpiService.RECIBIDOS);

		assertThat(recibidos.getValorAnterior()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(recibidos.getVariacion()).isNull();
		assertThat(recibidos.getTendencia()).isEqualTo("SIN_COMPARACION");
	}

	@Test
	@DisplayName("las barras por plantilla salen del volumen real y traen formula y semaforo")
	void barrasPorPlantilla() {
		procesar("uno");
		procesar("dos");

		List<KpiPlantillaModel> plantillas = kpiService.porPlantilla(tenant.getId(), null, null);

		assertThat(plantillas).hasSize(1);
		KpiPlantillaModel plantilla = plantillas.get(0);
		assertThat(plantilla.getCodigo()).isEqualTo(version.getPlantilla().getCodigo());
		assertThat(plantilla.getVolumen()).isEqualTo(2);
		assertThat(plantilla.getPorEstado()).containsEntry(EstadoDocumento.APROBADO.name(), 2L);
		assertThat(plantilla.getBarras()).hasSize(4);
		assertThat(plantilla.getBarras()).allSatisfy(barra -> {
			assertThat(barra.getFormula()).isNotBlank();
			assertThat(barra.getSemaforo()).isIn("VERDE", "AMBAR", "ROJO", "SIN_DATOS");
		});
		assertThat(plantilla.getSalud()).isIn("OK", "ATENCION", "CRITICO", "SIN_DATOS");
	}

	@Test
	@DisplayName("varias excepciones sobre un mismo documento no sacan la barra fuera de 0 a 100")
	void lasBarrasNuncaSeSalenDeEscala() {
		proveedor.programar("numeroRemito", PresenciaCampo.NO_FIGURA, null, "0.20");
		procesar("uno");
		procesar("dos");

		KpiPlantillaModel plantilla = kpiService.porPlantilla(tenant.getId(), null, null).get(0);

		assertThat(plantilla.getDocumentosConExcepciones()).isLessThanOrEqualTo(plantilla.getVolumen());
		assertThat(plantilla.getBarras()).allSatisfy(barra -> {
			if (barra.getValor() != null) {
				assertThat(barra.getValor()).isBetween(BigDecimal.ZERO, BigDecimal.ONE);
				assertThat(barra.getPorcentaje()).isBetween(BigDecimal.ZERO, new BigDecimal("100.0"));
			}
		});
	}

	@Test
	@DisplayName("la completitud documental cae a cero cuando el campo sale ILEGIBLE")
	void completitudDocumental() {
		proveedor.programar("numeroRemito", PresenciaCampo.ILEGIBLE, null, "0.30");
		procesar("ilegible");

		BarraKpiModel documentacion = barra(kpiService.porPlantilla(tenant.getId(), null, null).get(0),
				"documentacion");

		assertThat(documentacion.getValor()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(documentacion.getSemaforo()).isEqualTo("ROJO");
	}

	@Test
	@DisplayName("la poblacion de un indicador devuelve los documentos que lo componen")
	void poblacionAuditable() {
		DocumentoModel uno = ingresar("uno");
		DocumentoModel dos = ingresar("dos");

		List<DocumentoModel> poblacion = kpiService.poblacion(tenant.getId(), KpiService.RECIBIDOS, null, null);

		assertThat(poblacion).extracting(DocumentoModel::getId).containsExactlyInAnyOrder(uno.getId(), dos.getId());
	}

	@Test
	@DisplayName("pedir la poblacion de un indicador que no la tiene se rechaza con un mensaje claro")
	void poblacionNoDisponible() {
		assertThatThrownBy(() -> kpiService.poblacion(tenant.getId(), KpiService.ALMACENAMIENTO, null, null))
				.isInstanceOf(ValidacionException.class)
				.hasMessageContaining("no tiene poblacion auditable");
	}

	@Test
	@DisplayName("el numero de la tarjeta es exactamente el tamano de su poblacion, tambien con PDF segmentado")
	void elConteoCoincideConSuPoblacion() {
		ingresar("uno");
		ingresar("dos");
		Documento padre = ingresarLoteSegmentado();
		assertThat(padre.getEstado()).isEqualTo(EstadoDocumento.DIVIDIDO);
		assertThat(documentoRepository.listarSegmentos(padre.getId())).hasSize(3);

		KpiResumenModel resumen = kpiService.resumir(tenant.getId(), null, null);
		assertThat(buscar(resumen, KpiService.RECIBIDOS).getValor()).isEqualByComparingTo(new BigDecimal("3"));

		for (IndicadorKpiModel indicador : resumen.getIndicadores()) {
			if (!indicador.isTienePoblacion() || !IndicadorKpiModel.UNIDAD_CONTEO.equals(indicador.getUnidad())) {
				continue;
			}
			List<DocumentoModel> poblacion = kpiService.poblacion(tenant.getId(), indicador.getClave(), null, null);
			assertThat(poblacion)
					.describedAs("la poblacion de %s no coincide con el valor mostrado", indicador.getClave())
					.hasSize(indicador.getValor().intValue());
		}
	}

	@Test
	@DisplayName("el flag tienePoblacion no se desincroniza del drill down que el servicio realmente resuelve")
	void elFlagDeDrillDownNoMiente() {
		ingresar("uno");

		for (IndicadorKpiModel indicador : kpiService.resumir(tenant.getId(), null, null).getIndicadores()) {
			if (indicador.isTienePoblacion()) {
				assertThat(kpiService.poblacion(tenant.getId(), indicador.getClave(), null, null)).isNotNull();
			} else {
				assertThatThrownBy(
						() -> kpiService.poblacion(tenant.getId(), indicador.getClave(), null, null))
						.isInstanceOf(ValidacionException.class);
			}
		}
	}

	@Test
	@DisplayName("SEC-01: los KPI de un tenant no incluyen documentos de otro")
	void aislamientoPorTenant() {
		ingresar("propio");
		Tenant otro = fabrica.crearTenant("o" + UUID.randomUUID().toString().substring(0, 8));

		assertThat(buscar(kpiService.resumir(tenant.getId(), null, null), KpiService.RECIBIDOS).getValor())
				.isEqualByComparingTo(BigDecimal.ONE);
		assertThat(buscar(kpiService.resumir(otro.getId(), null, null), KpiService.RECIBIDOS).getValor())
				.isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(kpiService.porPlantilla(otro.getId(), null, null)).isEmpty();
		assertThat(kpiService.poblacion(otro.getId(), KpiService.RECIBIDOS, null, null)).isEmpty();
	}

	private IndicadorKpiModel buscar(KpiResumenModel resumen, String clave) {
		return resumen.getIndicadores().stream().filter(indicador -> clave.equals(indicador.getClave())).findFirst()
				.orElseThrow(() -> new IllegalStateException("Falta el indicador " + clave));
	}

	private BarraKpiModel barra(KpiPlantillaModel plantilla, String clave) {
		return plantilla.getBarras().stream().filter(elemento -> clave.equals(elemento.getClave())).findFirst()
				.orElseThrow(() -> new IllegalStateException("Falta la barra " + clave));
	}

	private DocumentoModel ingresar(String clave) {
		NuevoDocumentoReqModel datos = new NuevoDocumentoReqModel();
		datos.setCodigoPlantilla(version.getPlantilla().getCodigo());
		return ingestaDocumentalService.ingresar(tenant, administrador, pdf("remito.pdf", List.of("REMITO " + clave)),
				datos, clave + "-" + UUID.randomUUID());
	}

	private Documento procesar(String clave) {
		DocumentoModel modelo = ingresar(clave);
		extractorDocumentalService.procesar(modelo.getId());
		return documentoRepository.findById(modelo.getId()).orElseThrow();
	}

	private Documento ingresarLoteSegmentado() {
		VersionPlantilla lote = fabrica.crearPlantillaPublicada(tenant,
				"LOTE_" + UUID.randomUUID().toString().substring(0, 6),
				List.of(campo("numeroRemito", TipoDatoCampo.TEXTO, true)), List.of(), new BigDecimal("0.9000"));
		lote.setEstrategiaSegmentacion(EstrategiaSegmentacion.PATRON_TEXTO);
		lote.setPatronInicioDocumento("^\\s*REMITO");
		versionPlantillaRepository.save(lote);

		NuevoDocumentoReqModel datos = new NuevoDocumentoReqModel();
		datos.setCodigoPlantilla(lote.getPlantilla().getCodigo());
		DocumentoModel modelo = ingestaDocumentalService.ingresar(tenant, administrador,
				pdf("lote.pdf", List.of("REMITO A", "REMITO B", "REMITO C")), datos, "lote-" + UUID.randomUUID());
		extractorDocumentalService.procesar(modelo.getId());
		return documentoRepository.findById(modelo.getId()).orElseThrow();
	}

	private void aprobar(String documentoId) {
		RevisionDocumentoReqModel datos = new RevisionDocumentoReqModel();
		datos.setDecision(DecisionRevision.APROBAR);
		datos.setMotivo("Revision manual dentro de la prueba de KPI");
		revisionDocumentalService.registrar(tenant.getId(), documentoId, administrador, datos);
	}
}
