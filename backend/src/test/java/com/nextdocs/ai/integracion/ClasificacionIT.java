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
import com.nextdocs.ai.enumeraciones.EstadoDocumento;
import com.nextdocs.ai.enumeraciones.OrigenTipoDocumento;
import com.nextdocs.ai.enumeraciones.TipoDatoCampo;
import com.nextdocs.ai.modelos.DocumentoModel;
import com.nextdocs.ai.modelos.NuevoDocumentoReqModel;
import com.nextdocs.ai.modelos.ResultadoClasificacionModel;
import com.nextdocs.ai.modelos.SolicitudClasificacionModel;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.repositorios.ExcepcionDocumentalRepository;
import com.nextdocs.ai.servicios.ClasificadorDocumentalService;
import com.nextdocs.ai.servicios.DocumentoService;
import com.nextdocs.ai.servicios.ExtractorDocumentalService;
import com.nextdocs.ai.servicios.IngestaDocumentalService;
import com.nextdocs.ai.servicios.proveedores.ContenidoNoConfiable;
import com.nextdocs.ai.servicios.proveedores.InstruccionClasificacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ClasificacionIT extends PruebaIntegracion {

	@Autowired
	private FabricaDatosPrueba fabrica;

	@Autowired
	private IngestaDocumentalService ingestaDocumentalService;

	@Autowired
	private ExtractorDocumentalService extractorDocumentalService;

	@Autowired
	private DocumentoRepository documentoRepository;

	@Autowired
	private ExcepcionDocumentalRepository excepcionDocumentalRepository;

	@Autowired
	private DocumentoService documentoService;

	@Autowired
	private ProveedorPruebaService proveedor;

	private Tenant tenant;

	private Usuario administrador;

	@BeforeEach
	void prepararEscenario() {
		String sufijo = UUID.randomUUID().toString().substring(0, 8);
		tenant = fabrica.crearTenant("clasifica-" + sufijo);
		administrador = fabrica.administradorDe(tenant);
		fabrica.crearPlantillaPublicada(tenant, "REMITO",
				List.of(campo("numero", TipoDatoCampo.TEXTO, true)), List.of(), new BigDecimal("0.9500"));
		fabrica.crearPlantillaPublicada(tenant, "FACTURA",
				List.of(campo("numero", TipoDatoCampo.TEXTO, true)), List.of(), new BigDecimal("0.9500"));
		proveedor.reiniciar();
	}

	@Test
	@DisplayName("un documento que llega sin tipo declarado lo resuelve el clasificador")
	void elDocumentoSinTipoSeClasificaSolo() {
		proveedor.programarClasificacion("FACTURA", new BigDecimal("0.9100"));

		Documento documento = procesar();

		assertThat(documento.getPlantilla()).isNotNull();
		assertThat(documentoService.obtener(tenant.getId(), documento.getId()).getCodigoPlantilla())
				.isEqualTo("FACTURA");
		assertThat(documento.getOrigenTipo()).isEqualTo(OrigenTipoDocumento.DETECTADO);
		assertThat(documento.getConfianzaTipo()).isEqualByComparingTo(new BigDecimal("0.9100"));
		assertThat(documento.getMotivoTipo()).isNotBlank();
		assertThat(documento.getEstado()).isNotEqualTo(EstadoDocumento.RECIBIDO);
	}

	@Test
	@DisplayName("el clasificador recibe el catalogo del tenant y la advertencia de contenido no confiable")
	void elPromptLlevaElCatalogoYLaAdvertencia() {
		proveedor.programarClasificacion("REMITO", new BigDecimal("0.9000"));

		procesar();

		assertThat(proveedor.clasificaciones()).hasSize(1);
		SolicitudClasificacionModel solicitud = proveedor.clasificaciones().get(0);
		assertThat(solicitud.getCandidatos()).extracting("codigo").contains("REMITO", "FACTURA");

		String instruccion = InstruccionClasificacion.construir(solicitud, true);
		assertThat(instruccion).contains(ContenidoNoConfiable.ADVERTENCIA);
		assertThat(instruccion).contains(ResultadoClasificacionModel.CODIGO_DESCONOCIDO);
	}

	@Test
	@DisplayName("con confianza por debajo del umbral el documento queda sin tipo y con excepcion")
	void laConfianzaBajaNoAsignaTipo() {
		proveedor.programarClasificacion("REMITO", new BigDecimal("0.4000"));

		Documento documento = procesar();

		assertThat(documento.getPlantilla()).isNull();
		assertThat(documento.getEstado()).isEqualTo(EstadoDocumento.OBSERVADO);
		assertThat(excepcionDocumentalRepository.listarPorDocumento(documento.getId()))
				.anyMatch(excepcion -> excepcion.getCodigo()
						.equals(ClasificadorDocumentalService.CODIGO_TIPO_INCIERTO));
	}

	@Test
	@DisplayName("un tipo que no esta en el catalogo del tenant no se fuerza")
	void elTipoFueraDelCatalogoNoSeFuerza() {
		proveedor.programarClasificacion("CONTRATO_DE_ALQUILER", new BigDecimal("0.9900"));

		Documento documento = procesar();

		assertThat(documento.getPlantilla()).isNull();
		assertThat(documento.getEstado()).isEqualTo(EstadoDocumento.OBSERVADO);
		assertThat(excepcionDocumentalRepository.listarPorDocumento(documento.getId()))
				.anyMatch(excepcion -> excepcion.getCodigo()
						.equals(ClasificadorDocumentalService.CODIGO_TIPO_NO_RECONOCIDO));
	}

	@Test
	@DisplayName("si el documento ya declara su tipo el clasificador no se invoca")
	void elTipoDeclaradoGana() {
		NuevoDocumentoReqModel datos = new NuevoDocumentoReqModel();
		datos.setCodigoPlantilla("REMITO");
		DocumentoModel modelo = ingestaDocumentalService.ingresar(tenant, administrador,
				pdf("remito.pdf", List.of("REMITO 1")), datos, UUID.randomUUID().toString());
		extractorDocumentalService.procesar(modelo.getId());

		Documento documento = documentoRepository.findById(modelo.getId()).orElseThrow();
		assertThat(documento.getOrigenTipo()).isEqualTo(OrigenTipoDocumento.DECLARADO);
		assertThat(documentoService.obtener(tenant.getId(), documento.getId()).getCodigoPlantilla())
				.isEqualTo("REMITO");
		assertThat(proveedor.clasificaciones()).isEmpty();
	}

	private Documento procesar() {
		DocumentoModel modelo = ingestaDocumentalService.ingresar(tenant, administrador,
				pdf("documento.pdf", List.of("CONTENIDO")), new NuevoDocumentoReqModel(),
				UUID.randomUUID().toString());
		extractorDocumentalService.procesar(modelo.getId());
		return documentoRepository.findById(modelo.getId()).orElseThrow();
	}
}
