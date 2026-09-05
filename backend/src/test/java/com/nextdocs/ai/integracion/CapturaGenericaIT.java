package com.nextdocs.ai.integracion;

import static com.nextdocs.ai.integracion.FabricaDatosPrueba.pdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.TipoPropuesto;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.enumeraciones.EstadoTipoPropuesto;
import com.nextdocs.ai.enumeraciones.OrigenTipoDocumento;
import com.nextdocs.ai.enumeraciones.TipoDatoCampo;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.CampoSugeridoModel;
import com.nextdocs.ai.modelos.DocumentoModel;
import com.nextdocs.ai.modelos.NuevoDocumentoReqModel;
import com.nextdocs.ai.repositorios.CampoPlantillaRepository;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.repositorios.ExcepcionDocumentalRepository;
import com.nextdocs.ai.repositorios.PlantillaDocumentalRepository;
import com.nextdocs.ai.repositorios.ValorExtraidoRepository;
import com.nextdocs.ai.servicios.ClasificadorDocumentalService;
import com.nextdocs.ai.servicios.DocumentoService;
import com.nextdocs.ai.servicios.ExtractorDocumentalService;
import com.nextdocs.ai.servicios.IngestaDocumentalService;
import com.nextdocs.ai.servicios.SembradorCatalogoService;
import com.nextdocs.ai.servicios.TipoPropuestoService;
import com.nextdocs.ai.servicios.catalogo.CatalogoDocumentalBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CapturaGenericaIT extends PruebaIntegracion {

	@Autowired
	private FabricaDatosPrueba fabrica;

	@Autowired
	private SembradorCatalogoService sembradorCatalogoService;

	@Autowired
	private TipoPropuestoService tipoPropuestoService;

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
	private ExcepcionDocumentalRepository excepcionDocumentalRepository;

	@Autowired
	private PlantillaDocumentalRepository plantillaDocumentalRepository;

	@Autowired
	private CampoPlantillaRepository campoPlantillaRepository;

	@Autowired
	private ProveedorPruebaService proveedor;

	private Tenant tenant;

	private Usuario administrador;

	@BeforeEach
	void prepararEscenario() {
		tenant = fabrica.crearTenant("generico-" + UUID.randomUUID().toString().substring(0, 8));
		administrador = fabrica.administradorDe(tenant);
		sembradorCatalogoService.sembrar(tenant);
		proveedor.reiniciar();
	}

	@Test
	@DisplayName("un tipo que no esta en el catalogo se captura igual con el esquema generico")
	void elTipoDesconocidoSeCapturaIgual() {
		proveedor.programarClasificacionDesconocida("Contrato de alquiler",
				List.of(campoSugerido("locador", "Locador", TipoDatoCampo.TEXTO),
						campoSugerido("canonMensual", "Canon mensual", TipoDatoCampo.MONEDA)));

		Documento documento = procesar();

		assertThat(documento.getOrigenTipo()).isEqualTo(OrigenTipoDocumento.GENERICO);
		assertThat(documentoService.obtener(tenant.getId(), documento.getId()).getCodigoPlantilla())
				.isEqualTo(CatalogoDocumentalBase.CODIGO_GENERICO);
		assertThat(valorExtraidoRepository.listarPorDocumento(documento.getId()))
				.as("el documento tiene que quedar con datos, no vacio")
				.isNotEmpty();
		assertThat(excepcionDocumentalRepository.listarPorDocumento(documento.getId()))
				.anyMatch(excepcion -> excepcion.getCodigo()
						.equals(ClasificadorDocumentalService.CODIGO_TIPO_NO_RECONOCIDO));
	}

	@Test
	@DisplayName("el tipo desconocido queda propuesto con sus campos y se cuenta cada vez que vuelve")
	void elTipoDesconocidoQuedaPropuesto() {
		proveedor.programarClasificacionDesconocida("Contrato de alquiler",
				List.of(campoSugerido("locador", "Locador", TipoDatoCampo.TEXTO)));

		procesar();
		procesar();

		List<TipoPropuesto> propuestos = tipoPropuestoService.listar(tenant.getId(),
				EstadoTipoPropuesto.PENDIENTE);
		assertThat(propuestos).hasSize(1);
		TipoPropuesto propuesto = propuestos.get(0);
		assertThat(propuesto.getCodigoSugerido()).isEqualTo("CONTRATO_DE_ALQUILER");
		assertThat(propuesto.getVeces()).isEqualTo(2);
		assertThat(tipoPropuestoService.camposDe(propuesto)).extracting("clave").contains("locador");
	}

	@Test
	@DisplayName("aprobar una propuesta crea la plantilla publicada y el clasificador ya la ve")
	void aprobarCreaLaPlantilla() {
		proveedor.programarClasificacionDesconocida("Contrato de alquiler",
				List.of(campoSugerido("locador", "Locador", TipoDatoCampo.TEXTO),
						campoSugerido("canonMensual", "Canon mensual", TipoDatoCampo.MONEDA)));
		procesar();

		TipoPropuesto propuesto = tipoPropuestoService.listar(tenant.getId(), EstadoTipoPropuesto.PENDIENTE)
				.get(0);
		tipoPropuestoService.aprobar(tenant, propuesto.getId());

		var creada = plantillaDocumentalRepository
				.buscarPorCodigo(tenant.getId(), "CONTRATO_DE_ALQUILER").orElseThrow();
		assertThat(creada.getVersionPublicada()).isNotNull();
		assertThat(creada.isClasificable()).isTrue();
		assertThat(campoPlantillaRepository.listarPorVersion(creada.getVersionPublicada().getId()))
				.extracting("clave").contains("locador", "canonMensual");
		assertThat(tipoPropuestoService.buscar(tenant.getId(), propuesto.getId()).getEstado())
				.isEqualTo(EstadoTipoPropuesto.APROBADO);
	}

	@Test
	@DisplayName("una propuesta sin campos no se aprueba: dejaria una plantilla que no extrae nada")
	void laPropuestaSinCamposNoSeAprueba() {
		proveedor.programarClasificacionDesconocida("Papel misterioso", List.of());
		procesar();

		TipoPropuesto propuesto = tipoPropuestoService.listar(tenant.getId(), EstadoTipoPropuesto.PENDIENTE)
				.get(0);
		assertThatThrownBy(() -> tipoPropuestoService.aprobar(tenant, propuesto.getId()))
				.isInstanceOf(ValidacionException.class)
				.hasMessageContaining("no extrae nada");
	}

	@Test
	@DisplayName("el esquema generico no compite como candidato en la clasificacion")
	void elGenericoNoEsCandidato() {
		proveedor.programarClasificacion("FACTURA", new BigDecimal("0.9500"));

		procesar();

		assertThat(proveedor.clasificaciones().get(0).getCandidatos())
				.extracting("codigo")
				.doesNotContain(CatalogoDocumentalBase.CODIGO_GENERICO);
	}

	@Test
	@DisplayName("con confianza baja tambien se captura, y la excepcion dice cual era el tipo probable")
	void laConfianzaBajaTambienCaptura() {
		proveedor.programarClasificacion("REMITO", new BigDecimal("0.3000"));

		Documento documento = procesar();

		assertThat(documento.getOrigenTipo()).isEqualTo(OrigenTipoDocumento.GENERICO);
		assertThat(excepcionDocumentalRepository.listarPorDocumento(documento.getId()))
				.anyMatch(excepcion -> excepcion.getCodigo()
						.equals(ClasificadorDocumentalService.CODIGO_TIPO_INCIERTO)
						&& excepcion.getDetalle().contains("REMITO"));
	}

	private CampoSugeridoModel campoSugerido(String clave, String etiqueta, TipoDatoCampo tipo) {
		CampoSugeridoModel campo = new CampoSugeridoModel();
		campo.setClave(clave);
		campo.setEtiqueta(etiqueta);
		campo.setTipoDato(tipo);
		return campo;
	}

	private Documento procesar() {
		DocumentoModel modelo = ingestaDocumentalService.ingresar(tenant, administrador,
				pdf("desconocido.pdf", List.of("CONTRATO DE ALQUILER")), new NuevoDocumentoReqModel(),
				UUID.randomUUID().toString());
		extractorDocumentalService.procesar(modelo.getId());
		return documentoRepository.findById(modelo.getId()).orElseThrow();
	}
}
