package com.nextdocs.ai.integracion;

import static com.nextdocs.ai.integracion.FabricaDatosPrueba.campo;
import static com.nextdocs.ai.integracion.FabricaDatosPrueba.pdf;
import static com.nextdocs.ai.integracion.FabricaDatosPrueba.regla;
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
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.AccionRetencion;
import com.nextdocs.ai.enumeraciones.DecisionRevision;
import com.nextdocs.ai.enumeraciones.EstadoDocumento;
import com.nextdocs.ai.enumeraciones.PresenciaCampo;
import com.nextdocs.ai.enumeraciones.SeveridadHallazgo;
import com.nextdocs.ai.enumeraciones.TipoDatoCampo;
import com.nextdocs.ai.enumeraciones.TipoReglaValidacion;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.RegistroExistenteException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.DocumentoModel;
import com.nextdocs.ai.modelos.EventoAuditoriaModel;
import com.nextdocs.ai.modelos.FiltroAuditoriaModel;
import com.nextdocs.ai.modelos.NuevoDocumentoReqModel;
import com.nextdocs.ai.modelos.PoliticaRetencionModel;
import com.nextdocs.ai.modelos.PoliticaRetencionReqModel;
import com.nextdocs.ai.modelos.RevisionDocumentoReqModel;
import com.nextdocs.ai.modelos.TrazabilidadDocumentoModel;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.servicios.ExtractorDocumentalService;
import com.nextdocs.ai.servicios.GobernanzaService;
import com.nextdocs.ai.servicios.IngestaDocumentalService;
import com.nextdocs.ai.servicios.RevisionDocumentalService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class GobernanzaIT extends PruebaIntegracion {

	private static final BigDecimal UMBRAL_ALTO = new BigDecimal("0.9000");

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
	private RevisionDocumentalService revisionDocumentalService;

	@Autowired
	private GobernanzaService gobernanzaService;

	@Autowired
	private DocumentoRepository documentoRepository;

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
	@DisplayName("GOV-01: un documento aprobado reconstruye proveedor, modelo, prompt, esquema, reglas, match y revisor")
	void gov01ReconstruccionCompleta() {
		plantillaConRegla("GOV01");
		fabrica.registrarConectorPrueba(tenant, "0.9500", "0.5000");
		conector.agregarCandidato("ped-77", "Pedido 77", "1.0000");
		proveedor.programar("numeroRemito", PresenciaCampo.PRESENTE, "R-0077", "0.5000");
		Documento documento = procesar("gov01");
		aprobar(documento.getId(), "Verificado contra el remito fisico");

		TrazabilidadDocumentoModel trazabilidad = gobernanzaService.reconstruir(tenant.getId(), documento.getId());

		assertThat(trazabilidad.getFaltantes()).isEmpty();
		assertThat(trazabilidad.isCompleta()).isTrue();
		assertThat(trazabilidad.getProveedor()).isNotNull();
		assertThat(trazabilidad.getModelo()).isEqualTo("prueba-v1");
		assertThat(trazabilidad.getVersionPrompt()).isEqualTo("p1");
		assertThat(trazabilidad.getVersionEsquema()).isEqualTo("e1");
		assertThat(trazabilidad.getReglas()).extracting("codigo").contains("REMITO_OBLIGATORIO");
		assertThat(trazabilidad.getAsociacionSeleccionada()).isEqualTo("PEDIDO:ped-77");
		assertThat(trazabilidad.getRevisor()).isEqualTo(administrador.getEmail());
		assertThat(trazabilidad.getDocumento().getEstado()).isEqualTo(EstadoDocumento.APROBADO);
		assertThat(trazabilidad.getExtracciones()).hasSize(1);
		assertThat(trazabilidad.getExtracciones().get(0).getValores()).extracting("valorNormalizado")
				.contains("R-0077");
		assertThat(trazabilidad.getValidaciones()).isNotEmpty();
		assertThat(trazabilidad.getRevisiones()).hasSize(1);
		assertThat(trazabilidad.getEventos()).isNotEmpty();
	}

	@Test
	@DisplayName("GOV-01: un documento sin extraer declara que la reconstruccion esta incompleta y por que")
	void gov01ReconstruccionIncompletaSeDeclara() {
		plantillaConRegla("GOV01B");
		DocumentoModel documento = ingresar("gov01b");

		TrazabilidadDocumentoModel trazabilidad = gobernanzaService.reconstruir(tenant.getId(), documento.getId());

		assertThat(trazabilidad.isCompleta()).isFalse();
		assertThat(trazabilidad.getFaltantes()).contains("extraccion", "proveedor", "modelo", "validacion");
		assertThat(trazabilidad.getReglas()).isNotEmpty();
	}

	@Test
	@DisplayName("GOV-01: la reconstruccion de un documento aprobado sin revision humana no exige revisor")
	void gov01DocumentoAutoaprobadoNoExigeRevisor() {
		plantillaConRegla("GOV01C");
		Documento documento = procesar("gov01c");

		TrazabilidadDocumentoModel trazabilidad = gobernanzaService.reconstruir(tenant.getId(), documento.getId());

		assertThat(documento.getEstado()).isEqualTo(EstadoDocumento.APROBADO);
		assertThat(trazabilidad.getRevisor()).isNull();
		assertThat(trazabilidad.getFaltantes()).doesNotContain("revisor");
		assertThat(trazabilidad.isCompleta()).isTrue();
	}

	@Test
	@DisplayName("SEC-01: la trazabilidad de un documento de otro tenant no se puede reconstruir")
	void sec01TrazabilidadAisladaPorTenant() {
		plantillaConRegla("GOV01D");
		Documento documento = procesar("gov01d");
		Tenant ajeno = fabrica.crearTenant("x" + UUID.randomUUID().toString().substring(0, 8));

		assertThatThrownBy(() -> gobernanzaService.reconstruir(ajeno.getId(), documento.getId()))
				.isInstanceOf(EntidadNoEncontradaException.class);
	}

	@Test
	@DisplayName("SEC-01: la consulta de auditoria de un tenant no devuelve eventos de otro")
	void sec01AuditoriaAisladaPorTenant() {
		plantillaConRegla("GOV01E");
		procesar("gov01e");
		Tenant ajeno = fabrica.crearTenant("x" + UUID.randomUUID().toString().substring(0, 8));

		Page<EventoAuditoriaModel> propios = gobernanzaService.consultar(tenant.getId(),
				new FiltroAuditoriaModel(), paginado());
		Page<EventoAuditoriaModel> ajenos = gobernanzaService.consultar(ajeno.getId(), new FiltroAuditoriaModel(),
				paginado());

		assertThat(propios.getContent()).isNotEmpty();
		assertThat(ajenos.getContent()).noneMatch(evento -> AccionAuditoria.DOCUMENTO_INGRESADO == evento.getAccion()
				&& evento.getIdRecurso() != null);
	}

	@Test
	@DisplayName("el filtro por accion devuelve unicamente esa accion")
	void filtroPorAccion() {
		plantillaConRegla("GOV01F");
		procesar("gov01f");

		FiltroAuditoriaModel filtro = new FiltroAuditoriaModel();
		filtro.setAccion(AccionAuditoria.DOCUMENTO_INGRESADO);
		Page<EventoAuditoriaModel> eventos = gobernanzaService.consultar(tenant.getId(), filtro, paginado());

		assertThat(eventos.getContent()).isNotEmpty();
		assertThat(eventos.getContent()).allMatch(evento -> evento.getAccion() == AccionAuditoria.DOCUMENTO_INGRESADO);
	}

	@Test
	@DisplayName("el filtro por rango de fechas funciona con extremos presentes y ausentes")
	void filtroPorRangoDeFechas() {
		plantillaConRegla("GOV01G");
		procesar("gov01g");

		FiltroAuditoriaModel sinRango = new FiltroAuditoriaModel();
		FiltroAuditoriaModel conRango = new FiltroAuditoriaModel();
		conRango.setDesde(Instant.now().minus(1, ChronoUnit.HOURS));
		conRango.setHasta(Instant.now().plus(1, ChronoUnit.HOURS));
		FiltroAuditoriaModel futuro = new FiltroAuditoriaModel();
		futuro.setDesde(Instant.now().plus(1, ChronoUnit.DAYS));

		assertThat(gobernanzaService.consultar(tenant.getId(), sinRango, paginado()).getContent()).isNotEmpty();
		assertThat(gobernanzaService.consultar(tenant.getId(), conRango, paginado()).getContent()).isNotEmpty();
		assertThat(gobernanzaService.consultar(tenant.getId(), futuro, paginado()).getContent()).isEmpty();
	}

	@Test
	@DisplayName("la cadena de correlacion reune los eventos de un mismo procesamiento")
	void cadenaDeCorrelacion() {
		plantillaConRegla("GOV01H");
		Documento documento = procesar("gov01h");

		List<EventoAuditoriaModel> cadena = gobernanzaService.porCorrelacion(tenant.getId(),
				documento.getCorrelacionId());

		assertThat(cadena).isNotEmpty();
		assertThat(cadena).allMatch(evento -> documento.getCorrelacionId().equals(evento.getCorrelacionId()));
	}

	@Test
	@DisplayName("la exportacion CSV trae encabezados, respeta el filtro y queda auditada")
	void exportacionCsvAuditada() {
		plantillaConRegla("GOV01I");
		procesar("gov01i");

		FiltroAuditoriaModel filtro = new FiltroAuditoriaModel();
		filtro.setAccion(AccionAuditoria.DOCUMENTO_INGRESADO);
		String csv = gobernanzaService.exportarCsv(tenant.getId(), filtro);

		assertThat(csv).startsWith("fecha;accion;tipoActor");
		assertThat(csv).contains(AccionAuditoria.DOCUMENTO_INGRESADO.name());
		assertThat(csv.lines().count()).isGreaterThan(1);

		FiltroAuditoriaModel deExportaciones = new FiltroAuditoriaModel();
		deExportaciones.setAccion(AccionAuditoria.AUDITORIA_EXPORTADA);
		assertThat(gobernanzaService.consultar(tenant.getId(), deExportaciones, paginado()).getContent())
				.isNotEmpty();
	}

	@Test
	@DisplayName("el detalle JSON de un evento se escapa una sola vez en el CSV")
	void exportacionEscapaElDetalleJson() {
		gobernanzaService.crearPolitica(tenant, politicaReq("EXPORTA", 30, AccionRetencion.CONSERVAR));

		FiltroAuditoriaModel filtro = new FiltroAuditoriaModel();
		filtro.setAccion(AccionAuditoria.POLITICA_MODIFICADA);
		String csv = gobernanzaService.exportarCsv(tenant.getId(), filtro);

		assertThat(csv).contains("\"\"clase\"\"").contains("\"\"EXPORTA\"\"");
		assertThat(csv).doesNotContain("\"\"\"\"");
	}

	@Test
	@DisplayName("la exportacion de un tenant no filtra eventos de otro")
	void exportacionAisladaPorTenant() {
		plantillaConRegla("GOV01J");
		Documento documento = procesar("gov01j");
		Tenant ajeno = fabrica.crearTenant("x" + UUID.randomUUID().toString().substring(0, 8));

		assertThat(gobernanzaService.exportarCsv(ajeno.getId(), new FiltroAuditoriaModel()))
				.doesNotContain(documento.getId());
	}

	@Test
	@DisplayName("el resumen agrupa los eventos por accion")
	void resumenPorAccion() {
		plantillaConRegla("GOV01K");
		procesar("gov01k");

		assertThat(gobernanzaService.resumen(tenant.getId(), null, null))
				.containsKey("porAccion")
				.hasEntrySatisfying("total", total -> assertThat((Long) total).isPositive());
	}

	@Test
	@DisplayName("una politica de retencion duplicada para la misma clase se rechaza")
	void politicaDuplicadaSeRechaza() {
		gobernanzaService.crearPolitica(tenant, politicaReq("REMITOS", 365, AccionRetencion.CONSERVAR));

		assertThatThrownBy(
				() -> gobernanzaService.crearPolitica(tenant, politicaReq("REMITOS", 90, AccionRetencion.ELIMINAR)))
				.isInstanceOf(RegistroExistenteException.class);
	}

	@Test
	@DisplayName("modificar una politica deja el antes y el despues en la auditoria")
	void politicaModificadaQuedaAuditada() {
		PoliticaRetencionModel politica = gobernanzaService.crearPolitica(tenant,
				politicaReq("FACTURAS", 365, AccionRetencion.CONSERVAR));

		gobernanzaService.actualizarPolitica(tenant.getId(), politica.getId(),
				politicaReq("FACTURAS", 1825, AccionRetencion.ANONIMIZAR));

		assertThat(gobernanzaService.listarPoliticas(tenant.getId()))
				.filteredOn(actual -> "FACTURAS".equals(actual.getClase()))
				.allMatch(actual -> actual.getDuracionDias() == 1825
						&& actual.getAccion() == AccionRetencion.ANONIMIZAR);
		assertThat(gobernanzaService.porRecurso(tenant.getId(), GobernanzaService.ENTIDAD_POLITICA,
				politica.getId())).anyMatch(evento -> evento.getDetalle() != null
						&& evento.getDetalle().contains("MODIFICACION") && evento.getDetalle().contains("1825"));
	}

	@Test
	@DisplayName("desactivar dos veces la misma politica se rechaza")
	void politicaDesactivadaDosVeces() {
		PoliticaRetencionModel politica = gobernanzaService.crearPolitica(tenant,
				politicaReq("CONTRATOS", 3650, AccionRetencion.CONSERVAR));
		gobernanzaService.desactivarPolitica(tenant.getId(), politica.getId());

		assertThatThrownBy(() -> gobernanzaService.desactivarPolitica(tenant.getId(), politica.getId()))
				.isInstanceOf(ValidacionException.class).hasMessageContaining("ya esta inactiva");
	}

	@Test
	@DisplayName("GOV-02: activar la retencion legal exige motivo y queda auditada con actor")
	void gov02RetencionLegalExigeMotivo() {
		plantillaConRegla("GOV02");
		Documento documento = procesar("gov02");

		assertThatThrownBy(() -> gobernanzaService.cambiarRetencionLegal(tenant.getId(), documento.getId(), true,
				"  ", administrador)).isInstanceOf(ValidacionException.class).hasMessageContaining("motivo");

		gobernanzaService.cambiarRetencionLegal(tenant.getId(), documento.getId(), true, "Litigio 4821",
				administrador);

		assertThat(documentoRepository.findById(documento.getId()).orElseThrow().isRetencionLegal()).isTrue();
		assertThat(gobernanzaService.porRecurso(tenant.getId(), "Documento", documento.getId()))
				.anyMatch(evento -> evento.getAccion() == AccionAuditoria.RETENCION_LEGAL_MODIFICADA
						&& evento.getDetalle().contains("Litigio 4821"));
	}

	@Test
	@DisplayName("GOV-02: activar dos veces la retencion legal se rechaza en vez de auditar un cambio falso")
	void gov02RetencionLegalNoSeDuplica() {
		plantillaConRegla("GOV02B");
		Documento documento = procesar("gov02b");
		gobernanzaService.cambiarRetencionLegal(tenant.getId(), documento.getId(), true, "Litigio 4821",
				administrador);

		assertThatThrownBy(() -> gobernanzaService.cambiarRetencionLegal(tenant.getId(), documento.getId(), true,
				"Litigio 4821", administrador)).isInstanceOf(ValidacionException.class);
	}

	private VersionPlantilla plantillaConRegla(String codigo) {
		codigoActual = codigo;
		return fabrica.crearPlantillaPublicada(tenant, codigo,
				List.of(campo("numeroRemito", TipoDatoCampo.TEXTO, true)),
				List.of(regla("REMITO_OBLIGATORIO", TipoReglaValidacion.OBLIGATORIO, SeveridadHallazgo.BLOQUEANTE,
						"numeroRemito", null)),
				UMBRAL_ALTO);
	}

	private PoliticaRetencionReqModel politicaReq(String clase, int dias, AccionRetencion accion) {
		PoliticaRetencionReqModel datos = new PoliticaRetencionReqModel();
		datos.setClase(clase);
		datos.setDescripcion("Politica de " + clase);
		datos.setDuracionDias(dias);
		datos.setAccion(accion);
		datos.setPermiteRetencionLegal(true);
		datos.setActiva(true);
		return datos;
	}

	private DocumentoModel ingresar(String clave) {
		NuevoDocumentoReqModel datos = new NuevoDocumentoReqModel();
		datos.setCodigoPlantilla(codigoActual);
		return ingestaDocumentalService.ingresar(tenant, administrador, pdf("remito.pdf", List.of("REMITO " + clave)),
				datos, clave + "-" + UUID.randomUUID());
	}

	private Documento procesar(String clave) {
		DocumentoModel modelo = ingresar(clave);
		extractorDocumentalService.procesar(modelo.getId());
		return documentoRepository.findById(modelo.getId()).orElseThrow();
	}

	private void aprobar(String documentoId, String motivo) {
		RevisionDocumentoReqModel datos = new RevisionDocumentoReqModel();
		datos.setDecision(DecisionRevision.APROBAR);
		datos.setMotivo(motivo);
		revisionDocumentalService.registrar(tenant.getId(), documentoId, administrador, datos);
	}

	private PageRequest paginado() {
		return PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "fecha"));
	}
}
