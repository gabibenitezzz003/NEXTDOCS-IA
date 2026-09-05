package com.nextdocs.ai.integracion;

import static com.nextdocs.ai.integracion.FabricaDatosPrueba.campo;
import static com.nextdocs.ai.integracion.FabricaDatosPrueba.pdf;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.nextdocs.ai.entidades.CorreccionAprendida;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.entidades.ValorExtraido;
import com.nextdocs.ai.enumeraciones.DecisionRevision;
import com.nextdocs.ai.enumeraciones.PresenciaCampo;
import com.nextdocs.ai.enumeraciones.TipoDatoCampo;
import com.nextdocs.ai.modelos.DocumentoModel;
import com.nextdocs.ai.modelos.NuevoDocumentoReqModel;
import com.nextdocs.ai.modelos.RevisionDocumentoReqModel;
import com.nextdocs.ai.modelos.SolicitudExtraccionModel;
import com.nextdocs.ai.repositorios.CorreccionAprendidaRepository;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.repositorios.ValorExtraidoRepository;
import com.nextdocs.ai.servicios.AprendizajeService;
import com.nextdocs.ai.servicios.ExtractorDocumentalService;
import com.nextdocs.ai.servicios.IngestaDocumentalService;
import com.nextdocs.ai.servicios.RevisionDocumentalService;
import com.nextdocs.ai.servicios.proveedores.InstruccionExtraccion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AprendizajeIT extends PruebaIntegracion {

	private static final String LEIDO = "0001-OOO99887";

	private static final String CORREGIDO = "0001-00099887";

	@Autowired
	private FabricaDatosPrueba fabrica;

	@Autowired
	private IngestaDocumentalService ingestaDocumentalService;

	@Autowired
	private ExtractorDocumentalService extractorDocumentalService;

	@Autowired
	private RevisionDocumentalService revisionDocumentalService;

	@Autowired
	private AprendizajeService aprendizajeService;

	@Autowired
	private CorreccionAprendidaRepository correccionAprendidaRepository;

	@Autowired
	private ValorExtraidoRepository valorExtraidoRepository;

	@Autowired
	private DocumentoRepository documentoRepository;

	@Autowired
	private ProveedorPruebaService proveedor;

	private Tenant tenant;

	private Usuario administrador;

	@BeforeEach
	void prepararEscenario() {
		tenant = fabrica.crearTenant("aprende-" + UUID.randomUUID().toString().substring(0, 8));
		administrador = fabrica.administradorDe(tenant);
		fabrica.crearPlantillaPublicada(tenant, "FACTURA",
				List.of(campo("numero", TipoDatoCampo.TEXTO, true)), List.of(), new BigDecimal("0.9900"));
		proveedor.reiniciar();
		proveedor.programar("numero", PresenciaCampo.PRESENTE, LEIDO, "0.9000");
	}

	@Test
	@DisplayName("corregir a mano deja registrada la correccion con su valor leido y su valor bueno")
	void corregirDejaLaLeccion() {
		Documento documento = procesarYCorregir();

		List<CorreccionAprendida> aprendidas = correccionAprendidaRepository
				.listarPara(tenant.getId(), "FACTURA", AprendizajeService.EMISOR_GENERICO,
						AprendizajeService.EMISOR_GENERICO);
		assertThat(aprendidas).hasSize(1);
		assertThat(aprendidas.get(0).getClaveCampo()).isEqualTo("numero");
		assertThat(aprendidas.get(0).getValorLeido()).isEqualTo(LEIDO);
		assertThat(aprendidas.get(0).getValorCorregido()).isEqualTo(CORREGIDO);
		assertThat(aprendidas.get(0).getVeces()).isEqualTo(1);
		assertThat(documento.getId()).isNotBlank();
	}

	@Test
	@DisplayName("la leccion viaja al prompt como pista, aclarando que el documento manda")
	void laLeccionViajaAlPrompt() {
		procesarYCorregir();
		proveedor.reiniciar();
		proveedor.programar("numero", PresenciaCampo.PRESENTE, LEIDO, "0.9000");

		procesar();

		SolicitudExtraccionModel solicitud = proveedor.solicitudes().get(proveedor.solicitudes().size() - 1);
		assertThat(solicitud.getPistas()).isNotEmpty();
		String instruccion = InstruccionExtraccion.construir(solicitud, true);
		assertThat(instruccion).contains(LEIDO).contains(CORREGIDO);
		assertThat(instruccion).contains("gana el documento");
	}

	@Test
	@DisplayName("una sola correccion no se aplica sola: recien a partir de la segunda")
	void unaSolaVezNoSeAplica() {
		procesarYCorregir();
		proveedor.reiniciar();
		proveedor.programar("numero", PresenciaCampo.PRESENTE, LEIDO, "0.9000");

		Documento segundo = procesar();

		assertThat(valorDe(segundo))
				.as("con una sola observacion todavia no hay patron, no se toca el valor")
				.isEqualTo(LEIDO);
	}

	@Test
	@DisplayName("a partir de la segunda vez la correccion se aplica sola y queda dicho en la extraccion")
	void aLaSegundaSeAplicaSola() {
		procesarYCorregir();
		procesarYCorregir();

		proveedor.reiniciar();
		proveedor.programar("numero", PresenciaCampo.PRESENTE, LEIDO, "0.9000");
		Documento tercero = procesar();

		assertThat(valorDe(tercero)).isEqualTo(CORREGIDO);
		ValorExtraido valor = valorExtraidoRepository.listarUltimosPorDocumento(tercero.getId()).stream()
				.filter(candidato -> "numero".equals(candidato.getClaveCampo())).findFirst().orElseThrow();
		assertThat(valor.getValorAnterior())
				.as("hay que poder ver que decia el documento antes de que aprendieramos")
				.isEqualTo(LEIDO);
	}

	@Test
	@DisplayName("si el documento trae otro valor la correccion no se mete")
	void noSeAplicaAOtroValor() {
		procesarYCorregir();
		procesarYCorregir();

		proveedor.reiniciar();
		proveedor.programar("numero", PresenciaCampo.PRESENTE, "0002-00011111", "0.9500");
		Documento otro = procesar();

		assertThat(valorDe(otro)).isEqualTo("0002-00011111");
	}

	private Documento procesarYCorregir() {
		Documento documento = procesar();
		RevisionDocumentoReqModel revision = new RevisionDocumentoReqModel();
		revision.setDecision(DecisionRevision.CORREGIR);
		revision.setMotivo("El OCR confunde la O con el cero en el punto de venta");
		revision.setCorrecciones(Map.of("numero", CORREGIDO));
		revisionDocumentalService.registrar(tenant.getId(), documento.getId(), administrador, revision);
		return documentoRepository.findById(documento.getId()).orElseThrow();
	}

	private Documento procesar() {
		NuevoDocumentoReqModel datos = new NuevoDocumentoReqModel();
		datos.setCodigoPlantilla("FACTURA");
		DocumentoModel modelo = ingestaDocumentalService.ingresar(tenant, administrador,
				pdf("factura.pdf", List.of("FACTURA")), datos, UUID.randomUUID().toString());
		extractorDocumentalService.procesar(modelo.getId());
		return documentoRepository.findById(modelo.getId()).orElseThrow();
	}

	private String valorDe(Documento documento) {
		return valorExtraidoRepository.listarUltimosPorDocumento(documento.getId()).stream()
				.filter(valor -> "numero".equals(valor.getClaveCampo())).findFirst()
				.map(ValorExtraido::getValorNormalizado).orElse(null);
	}
}
