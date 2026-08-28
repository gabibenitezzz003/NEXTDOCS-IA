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
import com.nextdocs.ai.enumeraciones.AccionPresupuestoCosto;
import com.nextdocs.ai.enumeraciones.EstadoDocumento;
import com.nextdocs.ai.enumeraciones.PresenciaCampo;
import com.nextdocs.ai.enumeraciones.TipoDatoCampo;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.DocumentoModel;
import com.nextdocs.ai.modelos.NuevoDocumentoReqModel;
import com.nextdocs.ai.modelos.PoliticaCostoReqModel;
import com.nextdocs.ai.modelos.ResumenCostoModel;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.servicios.ExtractorDocumentalService;
import com.nextdocs.ai.servicios.IngestaDocumentalService;
import com.nextdocs.ai.servicios.ObservabilidadService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ObservabilidadIT extends PruebaIntegracion {

	@Autowired
	private FabricaDatosPrueba fabrica;

	@Autowired
	private ProveedorPruebaService proveedor;

	@Autowired
	private IngestaDocumentalService ingestaDocumentalService;

	@Autowired
	private ExtractorDocumentalService extractorDocumentalService;

	@Autowired
	private ObservabilidadService observabilidadService;

	@Autowired
	private DocumentoRepository documentoRepository;

	private Tenant tenant;

	private Usuario administrador;

	@BeforeEach
	void preparar() {
		proveedor.reiniciar();
		tenant = fabrica.crearTenant("c" + UUID.randomUUID().toString().substring(0, 8));
		administrador = fabrica.administradorDe(tenant);
	}

	@Test
	@DisplayName("el costo efectivo divide la inferencia por los documentos correctos, no por todos")
	void costoEfectivoPorDocumentoCorrecto() {
		VersionPlantilla version = plantilla("COST1");
		proveedor.programarUso(200, 80, "0.500000");
		Documento correcto = procesar(version, "ok");
		proveedor.programar("numeroRemito", PresenciaCampo.NO_FIGURA, null, "1.0");
		Documento observado = procesar(version, "obs");

		ResumenCostoModel resumen = observabilidadService.resumir(tenant.getId(), Instant.now().minus(1, ChronoUnit.HOURS),
				Instant.now().plus(1, ChronoUnit.MINUTES));

		assertThat(correcto.getEstado()).isEqualTo(EstadoDocumento.APROBADO);
		assertThat(observado.getEstado()).isEqualTo(EstadoDocumento.OBSERVADO);
		assertThat(resumen.getDocumentosRecibidos()).isEqualTo(2);
		assertThat(resumen.getDocumentosCorrectos()).isEqualTo(1);
		assertThat(resumen.getCostoInferencia()).isEqualByComparingTo("1.000000");
		assertThat(resumen.getCostoEfectivoPorDocumentoCorrecto()).isEqualByComparingTo("1.000000");
		assertThat(resumen.getTokensEntrada()).isEqualTo(400);
		assertThat(resumen.getTokensSalida()).isEqualTo(160);
		assertThat(resumen.getPorPlantilla()).extracting(fila -> fila.getClave()).contains("COST1");
		assertThat(resumen.getPorProveedor()).isNotEmpty();
	}

	@Test
	@DisplayName("sin politica de presupuesto la ingesta sigue abierta")
	void sinPoliticaNoBloquea() {
		VersionPlantilla version = plantilla("COST2");
		proveedor.programarUso(10, 5, "9.000000");
		procesar(version, "libre");
		DocumentoModel siguiente = ingestaDocumentalService.ingresar(tenant, administrador,
				pdf("remito.pdf", List.of("REMITO libre-2")), datos(version), "libre-2");
		assertThat(siguiente.getId()).isNotBlank();
	}

	@Test
	@DisplayName("BLOQUEAR_INGESTA corta el alta cuando el mes ya gasto el presupuesto")
	void presupuestoAgotadoBloqueaIngesta() {
		VersionPlantilla version = plantilla("COST3");
		PoliticaCostoReqModel politica = new PoliticaCostoReqModel();
		politica.setActivo(true);
		politica.setPresupuestoMensual(new BigDecimal("0.400000"));
		politica.setUmbralAlerta(new BigDecimal("0.5000"));
		politica.setAccionAlExceder(AccionPresupuestoCosto.BLOQUEAR_INGESTA);
		observabilidadService.actualizarPolitica(tenant.getId(), politica);
		proveedor.programarUso(10, 5, "0.500000");
		procesar(version, "caro");

		ResumenCostoModel resumen = observabilidadService.resumir(tenant.getId(), null, null);
		assertThat(resumen.isAlerta()).isTrue();
		assertThat(resumen.isBloqueaIngesta()).isTrue();
		assertThatThrownBy(() -> ingestaDocumentalService.ingresar(tenant, administrador,
				pdf("remito.pdf", List.of("REMITO caro-2")), datos(version), "caro-2"))
				.isInstanceOf(ValidacionException.class)
				.hasMessageContaining("presupuesto");
	}

	@Test
	@DisplayName("ALERTA avisa al cruzar el umbral y no corta la ingesta")
	void alertaSinBloqueo() {
		VersionPlantilla version = plantilla("COST4");
		PoliticaCostoReqModel politica = new PoliticaCostoReqModel();
		politica.setActivo(true);
		politica.setPresupuestoMensual(new BigDecimal("1.000000"));
		politica.setUmbralAlerta(new BigDecimal("0.4000"));
		politica.setAccionAlExceder(AccionPresupuestoCosto.ALERTA);
		observabilidadService.actualizarPolitica(tenant.getId(), politica);
		proveedor.programarUso(10, 5, "0.500000");
		procesar(version, "aviso");

		ResumenCostoModel resumen = observabilidadService.resumir(tenant.getId(), null, null);
		assertThat(resumen.isAlerta()).isTrue();
		assertThat(resumen.isBloqueaIngesta()).isFalse();
		DocumentoModel siguiente = ingestaDocumentalService.ingresar(tenant, administrador,
				pdf("remito.pdf", List.of("REMITO aviso-2")), datos(version), "aviso-2");
		assertThat(siguiente.getId()).isNotBlank();
	}

	@Test
	@DisplayName("la politica apagada no bloquea aunque el gasto supere el numero")
	void politicaApagadaNoBloquea() {
		VersionPlantilla version = plantilla("COST5");
		PoliticaCostoReqModel politica = new PoliticaCostoReqModel();
		politica.setActivo(false);
		politica.setPresupuestoMensual(new BigDecimal("0.010000"));
		politica.setAccionAlExceder(AccionPresupuestoCosto.BLOQUEAR_INGESTA);
		observabilidadService.actualizarPolitica(tenant.getId(), politica);
		proveedor.programarUso(10, 5, "1.000000");
		procesar(version, "apagado");
		DocumentoModel siguiente = ingestaDocumentalService.ingresar(tenant, administrador,
				pdf("remito.pdf", List.of("REMITO apagado-2")), datos(version), "apagado-2");
		assertThat(siguiente.getId()).isNotBlank();
	}

	@Test
	@DisplayName("SEC-01: un tenant no ve el gasto de otro")
	void aislamientoPorTenant() {
		VersionPlantilla version = plantilla("COST6");
		proveedor.programarUso(10, 5, "3.000000");
		procesar(version, "propio");
		Tenant ajeno = fabrica.crearTenant("z" + UUID.randomUUID().toString().substring(0, 8));

		ResumenCostoModel propio = observabilidadService.resumir(tenant.getId(), Instant.now().minus(1, ChronoUnit.HOURS),
				Instant.now().plus(1, ChronoUnit.MINUTES));
		ResumenCostoModel delAjeno = observabilidadService.resumir(ajeno.getId(), Instant.now().minus(1, ChronoUnit.HOURS),
				Instant.now().plus(1, ChronoUnit.MINUTES));

		assertThat(propio.getCostoInferencia()).isEqualByComparingTo("3.000000");
		assertThat(delAjeno.getCostoInferencia()).isEqualByComparingTo("0.000000");
		assertThat(delAjeno.getDocumentosRecibidos()).isZero();
		assertThatThrownBy(() -> observabilidadService.resumir("no-existe", null, null))
				.isInstanceOf(EntidadNoEncontradaException.class);
	}

	@Test
	@DisplayName("sin documentos correctos el costo efectivo queda vacio en vez de dividir por cero")
	void sinCorrectosNoDivide() {
		VersionPlantilla version = plantilla("COST7");
		proveedor.programar("numeroRemito", PresenciaCampo.NO_FIGURA, null, "1.0");
		proveedor.programarUso(10, 5, "0.250000");
		procesar(version, "vacio");

		ResumenCostoModel resumen = observabilidadService.resumir(tenant.getId(), Instant.now().minus(1, ChronoUnit.HOURS),
				Instant.now().plus(1, ChronoUnit.MINUTES));
		assertThat(resumen.getDocumentosCorrectos()).isZero();
		assertThat(resumen.getCostoInferencia()).isEqualByComparingTo("0.250000");
		assertThat(resumen.getCostoEfectivoPorDocumentoCorrecto()).isNull();
	}

	private VersionPlantilla plantilla(String codigo) {
		return fabrica.crearPlantillaPublicada(tenant, codigo,
				List.of(campo("numeroRemito", TipoDatoCampo.TEXTO, true)), List.of(), new BigDecimal("0.9000"));
	}

	private NuevoDocumentoReqModel datos(VersionPlantilla version) {
		NuevoDocumentoReqModel datos = new NuevoDocumentoReqModel();
		datos.setCodigoPlantilla(version.getPlantilla().getCodigo());
		return datos;
	}

	private Documento procesar(VersionPlantilla version, String clave) {
		DocumentoModel modelo = ingestaDocumentalService.ingresar(tenant, administrador,
				pdf("remito.pdf", List.of("REMITO " + clave)), datos(version), clave);
		extractorDocumentalService.procesar(modelo.getId());
		return documentoRepository.findById(modelo.getId()).orElseThrow();
	}
}
