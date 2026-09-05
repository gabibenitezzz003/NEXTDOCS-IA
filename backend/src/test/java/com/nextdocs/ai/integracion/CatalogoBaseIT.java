package com.nextdocs.ai.integracion;

import static com.nextdocs.ai.integracion.FabricaDatosPrueba.pdf;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.PlantillaDocumental;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.enumeraciones.OrigenTipoDocumento;
import com.nextdocs.ai.modelos.DocumentoModel;
import com.nextdocs.ai.modelos.NuevoDocumentoReqModel;
import com.nextdocs.ai.repositorios.CampoPlantillaRepository;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.repositorios.PlantillaDocumentalRepository;
import com.nextdocs.ai.repositorios.ReglaPlantillaRepository;
import com.nextdocs.ai.servicios.DocumentoService;
import com.nextdocs.ai.servicios.ExtractorDocumentalService;
import com.nextdocs.ai.servicios.IngestaDocumentalService;
import com.nextdocs.ai.servicios.SembradorCatalogoService;
import com.nextdocs.ai.servicios.catalogo.CatalogoDocumentalBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CatalogoBaseIT extends PruebaIntegracion {

	@Autowired
	private FabricaDatosPrueba fabrica;

	@Autowired
	private SembradorCatalogoService sembradorCatalogoService;

	@Autowired
	private PlantillaDocumentalRepository plantillaDocumentalRepository;

	@Autowired
	private CampoPlantillaRepository campoPlantillaRepository;

	@Autowired
	private ReglaPlantillaRepository reglaPlantillaRepository;

	@Autowired
	private IngestaDocumentalService ingestaDocumentalService;

	@Autowired
	private ExtractorDocumentalService extractorDocumentalService;

	@Autowired
	private DocumentoService documentoService;

	@Autowired
	private DocumentoRepository documentoRepository;

	@Autowired
	private ProveedorPruebaService proveedor;

	private Tenant tenant;

	private Usuario administrador;

	@BeforeEach
	void prepararEscenario() {
		tenant = fabrica.crearTenant("catalogo-" + UUID.randomUUID().toString().substring(0, 8));
		administrador = fabrica.administradorDe(tenant);
		proveedor.reiniciar();
	}

	@Test
	@DisplayName("el catalogo base deja los tipos publicados y listos para clasificar")
	void elCatalogoQuedaPublicado() {
		List<String> creados = sembradorCatalogoService.sembrar(tenant);

		assertThat(creados).hasSize(CatalogoDocumentalBase.TIPOS.size());
		assertThat(creados).contains("REMITO", "FACTURA", "DNI", "VTV", "CONSTANCIA_CUIT");

		for (String codigo : creados) {
			PlantillaDocumental plantilla = plantillaDocumentalRepository
					.buscarPorCodigo(tenant.getId(), codigo).orElseThrow();
			assertThat(plantilla.getVersionPublicada())
					.as("la plantilla %s tiene que quedar publicada, si no el clasificador no la ve", codigo)
					.isNotNull();
			assertThat(plantilla.getDescripcion())
					.as("la descripcion es lo que lee el clasificador para elegir el tipo")
					.isNotBlank();
			assertThat(campoPlantillaRepository.listarPorVersion(plantilla.getVersionPublicada().getId()))
					.isNotEmpty();
		}
	}

	@Test
	@DisplayName("la factura base trae los campos fiscales y sus reglas")
	void laFacturaTraeSusCamposYReglas() {
		sembradorCatalogoService.sembrar(tenant);

		PlantillaDocumental factura = plantillaDocumentalRepository
				.buscarPorCodigo(tenant.getId(), "FACTURA").orElseThrow();
		assertThat(campoPlantillaRepository.listarPorVersion(factura.getVersionPublicada().getId()))
				.extracting("clave")
				.contains("numero", "fechaEmision", "cuitEmisor", "neto", "iva", "total", "cae");
		assertThat(reglaPlantillaRepository.listarPorVersion(factura.getVersionPublicada().getId()))
				.extracting("codigo")
				.contains("CUIT_INVALIDO", "FECHA_FUTURA", "TOTAL_NO_CUADRA");
	}

	@Test
	@DisplayName("sembrar dos veces no duplica nada")
	void sembrarDosVecesEsIdempotente() {
		assertThat(sembradorCatalogoService.sembrar(tenant)).hasSize(CatalogoDocumentalBase.TIPOS.size());
		assertThat(sembradorCatalogoService.sembrar(tenant)).isEmpty();

		assertThat(plantillaDocumentalRepository.listarPorTenant(tenant.getId(), null))
				.hasSize(CatalogoDocumentalBase.TIPOS.size());
	}

	@Test
	@DisplayName("con el catalogo base un tenant nuevo clasifica sin que nadie arme una plantilla")
	void unTenantNuevoClasificaSinConfigurarNada() {
		sembradorCatalogoService.sembrar(tenant);
		proveedor.programarClasificacion("REMITO", new BigDecimal("0.9400"));

		DocumentoModel modelo = ingestaDocumentalService.ingresar(tenant, administrador,
				pdf("lo-que-sea.pdf", List.of("REMITO 0001-00099887")), new NuevoDocumentoReqModel(),
				UUID.randomUUID().toString());
		extractorDocumentalService.procesar(modelo.getId());

		Documento documento = documentoRepository.findById(modelo.getId()).orElseThrow();
		assertThat(documento.getOrigenTipo()).isEqualTo(OrigenTipoDocumento.DETECTADO);
		assertThat(documentoService.obtener(tenant.getId(), documento.getId()).getCodigoPlantilla())
				.isEqualTo("REMITO");
		assertThat(proveedor.clasificaciones().get(0).getCandidatos())
				.as("el clasificador tiene que ver todo el catalogo, no una parte")
				.hasSize(CatalogoDocumentalBase.TIPOS.size());
	}
}
