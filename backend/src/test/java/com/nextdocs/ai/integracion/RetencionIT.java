package com.nextdocs.ai.integracion;

import static com.nextdocs.ai.integracion.FabricaDatosPrueba.campo;
import static com.nextdocs.ai.integracion.FabricaDatosPrueba.pdf;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.nextdocs.ai.entidades.CampoPlantilla;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.entidades.VersionPlantilla;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.AccionRetencion;
import com.nextdocs.ai.enumeraciones.ResultadoRetencion;
import com.nextdocs.ai.enumeraciones.SensibilidadCampo;
import com.nextdocs.ai.enumeraciones.TipoDatoCampo;
import com.nextdocs.ai.modelos.DocumentoModel;
import com.nextdocs.ai.modelos.EventoAuditoriaModel;
import com.nextdocs.ai.modelos.NuevoDocumentoReqModel;
import com.nextdocs.ai.modelos.PoliticaRetencionReqModel;
import com.nextdocs.ai.modelos.ResultadoRetencionModel;
import com.nextdocs.ai.modelos.ResumenRetencionModel;
import com.nextdocs.ai.repositorios.ArchivoDocumentoRepository;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.repositorios.ValorExtraidoRepository;
import com.nextdocs.ai.servicios.DocumentoService;
import com.nextdocs.ai.servicios.ExtractorDocumentalService;
import com.nextdocs.ai.servicios.GobernanzaService;
import com.nextdocs.ai.servicios.IngestaDocumentalService;
import com.nextdocs.ai.servicios.RetencionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RetencionIT extends PruebaIntegracion {

	private static final String CAMPO_SENSIBLE = "cuitReceptor";

	private static final String CAMPO_COMUN = "numeroRemito";

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
	private DocumentoService documentoService;

	@Autowired
	private RetencionService retencionService;

	@Autowired
	private GobernanzaService gobernanzaService;

	@Autowired
	private DocumentoRepository documentoRepository;

	@Autowired
	private ArchivoDocumentoRepository archivoDocumentoRepository;

	@Autowired
	private ValorExtraidoRepository valorExtraidoRepository;

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
	@DisplayName("GOV-02: cerrar un documento fija su fecha de retencion segun la politica de su clase")
	void gov02ElCierreFijaLaFechaDeRetencion() {
		plantilla("RET01");
		politica("RET01", 365, AccionRetencion.CONSERVAR);
		Documento documento = cerrar(procesar("ret01"));

		assertThat(documento.getRetenerHasta()).isNotNull();
		assertThat(documento.getRetenerHasta())
				.isCloseTo(documento.getCerrado().plus(365, ChronoUnit.DAYS), within(2));
	}

	@Test
	@DisplayName("GOV-02: un documento cerrado sin politica no vence nunca y queda visible en el inventario")
	void gov02SinPoliticaNoVence() {
		plantilla("RET02");
		Documento documento = cerrar(procesar("ret02"));

		assertThat(documento.getRetenerHasta()).isNull();
		assertThat(retencionService.inventario(tenant.getId())).hasEntrySatisfying("cerradosSinPolitica",
				cantidad -> assertThat((Long) cantidad).isEqualTo(1L));
	}

	@Test
	@DisplayName("GOV-02: un documento vencido bajo retencion legal no se toca")
	void gov02LegalHoldBloqueaLaAccion() {
		plantilla("RET03");
		politica("RET03", 30, AccionRetencion.ELIMINAR);
		Documento documento = vencer(cerrar(procesar("ret03")));
		gobernanzaService.cambiarRetencionLegal(tenant.getId(), documento.getId(), true, "Litigio 9001",
				administrador);

		ResultadoRetencionModel resultado = retencionService.aplicar(tenant.getId(), documento.getId());

		assertThat(resultado.getResultado()).isEqualTo(ResultadoRetencion.OMITIDA_POR_RETENCION_LEGAL);
		Documento actual = documentoRepository.findById(documento.getId()).orElseThrow();
		assertThat(actual.getBaja()).isNull();
		assertThat(actual.getRetencionAplicada()).isNull();
		assertThat(archivoDocumentoRepository.listarPorDocumento(documento.getId())).isNotEmpty();
	}

	@Test
	@DisplayName("GOV-02: levantar la retencion legal habilita la accion que estaba bloqueada")
	void gov02LevantarElHoldHabilitaLaAccion() {
		plantilla("RET04");
		politica("RET04", 30, AccionRetencion.ELIMINAR);
		Documento documento = vencer(cerrar(procesar("ret04")));
		gobernanzaService.cambiarRetencionLegal(tenant.getId(), documento.getId(), true, "Litigio 9002",
				administrador);
		assertThat(retencionService.aplicar(tenant.getId(), documento.getId()).getResultado())
				.isEqualTo(ResultadoRetencion.OMITIDA_POR_RETENCION_LEGAL);

		gobernanzaService.cambiarRetencionLegal(tenant.getId(), documento.getId(), false, "Litigio 9002 cerrado",
				administrador);
		ResultadoRetencionModel resultado = retencionService.aplicar(tenant.getId(), documento.getId());

		assertThat(resultado.getResultado()).isEqualTo(ResultadoRetencion.APLICADA);
		assertThat(documentoRepository.findById(documento.getId()).orElseThrow().getBaja()).isNotNull();
	}

	@Test
	@DisplayName("GOV-02: CONSERVAR marca el documento sin borrar nada")
	void gov02Conservar() {
		plantilla("RET05");
		politica("RET05", 30, AccionRetencion.CONSERVAR);
		Documento documento = vencer(cerrar(procesar("ret05")));

		ResultadoRetencionModel resultado = retencionService.aplicar(tenant.getId(), documento.getId());

		assertThat(resultado.getResultado()).isEqualTo(ResultadoRetencion.APLICADA);
		assertThat(resultado.getAccion()).isEqualTo(AccionRetencion.CONSERVAR);
		assertThat(resultado.getArchivosEliminados()).isZero();
		Documento actual = documentoRepository.findById(documento.getId()).orElseThrow();
		assertThat(actual.getBaja()).isNull();
		assertThat(actual.getRetencionAplicada()).isNotNull();
		assertThat(archivoDocumentoRepository.listarPorDocumento(documento.getId())).isNotEmpty();
	}

	@Test
	@DisplayName("GOV-02: ANONIMIZAR borra el original y los campos sensibles, y conserva el resto")
	void gov02Anonimizar() {
		plantilla("RET06");
		politica("RET06", 30, AccionRetencion.ANONIMIZAR);
		Documento documento = vencer(cerrar(procesar("ret06")));

		ResultadoRetencionModel resultado = retencionService.aplicar(tenant.getId(), documento.getId());

		assertThat(resultado.getResultado()).isEqualTo(ResultadoRetencion.APLICADA);
		assertThat(resultado.getArchivosEliminados()).isPositive();
		assertThat(resultado.getClavesAnonimizadas()).containsExactly(CAMPO_SENSIBLE);
		assertThat(archivoDocumentoRepository.listarPorDocumento(documento.getId())).isEmpty();

		Documento actual = documentoRepository.findById(documento.getId()).orElseThrow();
		assertThat(actual.getBaja()).isNull();
		assertThat(actual.getAccionRetencionAplicada()).isEqualTo(AccionRetencion.ANONIMIZAR);

		assertThat(valorExtraidoRepository.listarPorDocumento(documento.getId()))
				.filteredOn(valor -> CAMPO_SENSIBLE.equals(valor.getClaveCampo()))
				.allMatch(valor -> valor.isAnonimizado()
						&& RetencionService.VALOR_ANONIMIZADO.equals(valor.getValorNormalizado()));
		assertThat(valorExtraidoRepository.listarPorDocumento(documento.getId()))
				.filteredOn(valor -> CAMPO_COMUN.equals(valor.getClaveCampo()))
				.allMatch(valor -> !valor.isAnonimizado()
						&& !RetencionService.VALOR_ANONIMIZADO.equals(valor.getValorNormalizado()));
	}

	@Test
	@DisplayName("GOV-02: ELIMINAR borra el original y da de baja el documento, conservando la auditoria")
	void gov02Eliminar() {
		plantilla("RET07");
		politica("RET07", 30, AccionRetencion.ELIMINAR);
		Documento documento = vencer(cerrar(procesar("ret07")));

		ResultadoRetencionModel resultado = retencionService.aplicar(tenant.getId(), documento.getId());

		assertThat(resultado.getResultado()).isEqualTo(ResultadoRetencion.APLICADA);
		assertThat(resultado.getArchivosEliminados()).isPositive();
		assertThat(archivoDocumentoRepository.listarPorDocumento(documento.getId())).isEmpty();
		assertThat(documentoRepository.findById(documento.getId()).orElseThrow().getBaja()).isNotNull();
		assertThat(gobernanzaService.porRecurso(tenant.getId(), "Documento", documento.getId())).isNotEmpty();
	}

	@Test
	@DisplayName("GOV-02: la accion deja prueba de borrado con clase, accion y hash del contenido")
	void gov02PruebaDeBorrado() {
		plantilla("RET08");
		politica("RET08", 30, AccionRetencion.ELIMINAR);
		Documento documento = vencer(cerrar(procesar("ret08")));
		String hash = documento.getHashContenido();

		retencionService.aplicar(tenant.getId(), documento.getId());

		List<EventoAuditoriaModel> eventos = gobernanzaService.porRecurso(tenant.getId(), "Documento",
				documento.getId());
		assertThat(eventos).anyMatch(evento -> evento.getAccion() == AccionAuditoria.RETENCION_APLICADA
				&& evento.getDetalle().contains("RET08") && evento.getDetalle().contains("ELIMINAR")
				&& evento.getDetalle().contains(hash) && evento.getDetalle().contains("archivosEliminados"));
	}

	@Test
	@DisplayName("GOV-02: un documento ya tratado no se vuelve a procesar en el siguiente ciclo")
	void gov02NoSeReprocesa() {
		plantilla("RET09");
		politica("RET09", 30, AccionRetencion.ANONIMIZAR);
		Documento documento = vencer(cerrar(procesar("ret09")));

		assertThat(retencionService.aplicar(tenant.getId(), documento.getId()).getResultado())
				.isEqualTo(ResultadoRetencion.APLICADA);
		ResultadoRetencionModel segunda = retencionService.aplicar(tenant.getId(), documento.getId());

		assertThat(segunda.getResultado()).isEqualTo(ResultadoRetencion.OMITIDA_YA_APLICADA);
		assertThat(retencionService.listarVencidos(tenant.getId(),
				org.springframework.data.domain.PageRequest.of(0, 10)).getContent())
				.noneMatch(modelo -> modelo.getId().equals(documento.getId()));
	}

	@Test
	@DisplayName("GOV-02: un documento que todavia no vencio no se toca")
	void gov02NoVencidoNoSeToca() {
		plantilla("RET10");
		politica("RET10", 365, AccionRetencion.ELIMINAR);
		Documento documento = cerrar(procesar("ret10"));

		ResultadoRetencionModel resultado = retencionService.aplicar(tenant.getId(), documento.getId());

		assertThat(resultado.getResultado()).isEqualTo(ResultadoRetencion.OMITIDA_NO_VENCIDA);
		assertThat(documentoRepository.findById(documento.getId()).orElseThrow().getBaja()).isNull();
	}

	@Test
	@DisplayName("GOV-02: el ciclo trata los vencidos, cuenta los retenidos y deja resumen auditado")
	void gov02CicloCompleto() {
		plantilla("RET11");
		politica("RET11", 30, AccionRetencion.ELIMINAR);
		Documento tratable = vencer(cerrar(procesar("ret11a")));
		Documento retenido = vencer(cerrar(procesar("ret11b")));
		gobernanzaService.cambiarRetencionLegal(tenant.getId(), retenido.getId(), true, "Litigio 9003",
				administrador);

		ResumenRetencionModel resumen = retencionService.ejecutarCiclo(50);

		assertThat(resumen.getEvaluados()).isGreaterThanOrEqualTo(2);
		assertThat(resumen.getEliminados()).isGreaterThanOrEqualTo(1);
		assertThat(resumen.getRetenidosPorRetencionLegal()).isGreaterThanOrEqualTo(1);
		assertThat(documentoRepository.findById(tratable.getId()).orElseThrow().getBaja()).isNotNull();
		assertThat(documentoRepository.findById(retenido.getId()).orElseThrow().getBaja()).isNull();
		assertThat(gobernanzaService.porRecurso(tenant.getId(), "Tenant", tenant.getId()))
				.anyMatch(evento -> evento.getAccion() == AccionAuditoria.RETENCION_CICLO_EJECUTADO);
	}

	@Test
	@DisplayName("SEC-01: el inventario de retencion de un tenant no cuenta documentos de otro")
	void sec01InventarioAisladoPorTenant() {
		plantilla("RET12");
		politica("RET12", 30, AccionRetencion.ELIMINAR);
		vencer(cerrar(procesar("ret12")));
		Tenant ajeno = fabrica.crearTenant("x" + UUID.randomUUID().toString().substring(0, 8));

		assertThat(retencionService.inventario(tenant.getId())).hasEntrySatisfying("vencidosPendientes",
				cantidad -> assertThat((Long) cantidad).isEqualTo(1L));
		assertThat(retencionService.inventario(ajeno.getId())).hasEntrySatisfying("vencidosPendientes",
				cantidad -> assertThat((Long) cantidad).isZero());
	}

	private VersionPlantilla plantilla(String codigo) {
		codigoActual = codigo;
		CampoPlantilla comun = campo(CAMPO_COMUN, TipoDatoCampo.TEXTO, true);
		comun.setSensibilidad(SensibilidadCampo.INTERNA);
		CampoPlantilla sensible = campo(CAMPO_SENSIBLE, TipoDatoCampo.TEXTO, false);
		sensible.setSensibilidad(SensibilidadCampo.PERSONAL);
		return fabrica.crearPlantillaPublicada(tenant, codigo, List.of(comun, sensible), List.of(),
				new BigDecimal("0.9000"));
	}

	private void politica(String clase, int dias, AccionRetencion accion) {
		PoliticaRetencionReqModel datos = new PoliticaRetencionReqModel();
		datos.setClase(clase);
		datos.setDescripcion("Politica de " + clase);
		datos.setDuracionDias(dias);
		datos.setAccion(accion);
		datos.setPermiteRetencionLegal(true);
		datos.setActiva(true);
		gobernanzaService.crearPolitica(tenant, datos);
	}

	private Documento procesar(String clave) {
		NuevoDocumentoReqModel datos = new NuevoDocumentoReqModel();
		datos.setCodigoPlantilla(codigoActual);
		DocumentoModel modelo = ingestaDocumentalService.ingresar(tenant, administrador,
				pdf("remito.pdf", List.of("REMITO " + clave)), datos, clave + "-" + UUID.randomUUID());
		extractorDocumentalService.procesar(modelo.getId());
		return documentoRepository.findById(modelo.getId()).orElseThrow();
	}

	private Documento cerrar(Documento documento) {
		documentoService.cerrar(tenant.getId(), documento.getId());
		return documentoRepository.findById(documento.getId()).orElseThrow();
	}

	private Documento vencer(Documento documento) {
		documento.setRetenerHasta(Instant.now().minus(1, ChronoUnit.DAYS));
		documentoRepository.save(documento);
		return documento;
	}

	private static org.assertj.core.data.TemporalUnitOffset within(long segundos) {
		return new org.assertj.core.data.TemporalUnitWithinOffset(segundos, ChronoUnit.SECONDS);
	}
}
