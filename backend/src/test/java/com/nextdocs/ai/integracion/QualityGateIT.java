package com.nextdocs.ai.integracion;

import static com.nextdocs.ai.integracion.FabricaDatosPrueba.pdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.enumeraciones.EstadoPlantilla;
import com.nextdocs.ai.enumeraciones.PresenciaCampo;
import com.nextdocs.ai.enumeraciones.ResultadoQualityGate;
import com.nextdocs.ai.enumeraciones.TipoDatoCampo;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.CampoPlantillaReqModel;
import com.nextdocs.ai.modelos.EjecucionPruebaModel;
import com.nextdocs.ai.modelos.PlantillaModel;
import com.nextdocs.ai.modelos.PlantillaReqModel;
import com.nextdocs.ai.modelos.PoliticaQualityGateReqModel;
import com.nextdocs.ai.modelos.VersionPlantillaModel;
import com.nextdocs.ai.modelos.VersionPlantillaReqModel;
import com.nextdocs.ai.servicios.PlantillaService;
import com.nextdocs.ai.servicios.QualityGateService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

class QualityGateIT extends PruebaIntegracion {

	private static final String ESPERADO = "{\"numeroRemito\":{\"valor\":\"R-GOLD\",\"presencia\":\"PRESENTE\"}}";

	@Autowired
	private FabricaDatosPrueba fabrica;

	@Autowired
	private ProveedorPruebaService proveedor;

	@Autowired
	private PlantillaService plantillaService;

	@Autowired
	private QualityGateService qualityGateService;

	private Tenant tenant;

	private Usuario administrador;

	@BeforeEach
	void preparar() {
		proveedor.reiniciar();
		tenant = fabrica.crearTenant("t" + UUID.randomUUID().toString().substring(0, 8));
		administrador = fabrica.administradorDe(tenant);
	}

	@Test
	@DisplayName("GOV-04: con el gate exigido no se publica una version sin casos gold")
	void gov04SinCasosGold() {
		PlantillaModel plantilla = plantillaConCampo("GATE0");
		exigirGate(plantilla.getId());
		String versionId = plantilla.getVersiones().get(0).getId();

		assertThatThrownBy(() -> plantillaService.publicar(tenant.getId(), versionId, administrador))
				.isInstanceOf(ValidacionException.class)
				.hasMessageContaining("GOV-04")
				.hasMessageContaining("sin casos gold");
	}

	@Test
	@DisplayName("GOV-04: una corrida gold aprobada habilita la publicacion")
	void gov04CorridaApruebaPublicacion() {
		PlantillaModel plantilla = plantillaConCampo("GATE1");
		exigirGate(plantilla.getId());
		String versionId = plantilla.getVersiones().get(0).getId();
		agregarCaso(plantilla.getId());
		proveedor.programar("numeroRemito", PresenciaCampo.PRESENTE, "R-GOLD", "0.9000");

		EjecucionPruebaModel ejecucion = qualityGateService.ejecutar(tenant.getId(), versionId);

		assertThat(ejecucion.getResultado()).isEqualTo(ResultadoQualityGate.APROBADO);
		assertThat(ejecucion.getExactitud()).isEqualByComparingTo("1.0000");
		assertThat(ejecucion.getFactorCalibracion()).isEqualByComparingTo("1.0000");
		assertThat(plantillaService.obtenerVersion(tenant.getId(), versionId).getEstado())
				.isEqualTo(EstadoPlantilla.EN_PRUEBA);
		assertThat(plantillaService.obtenerVersion(tenant.getId(), versionId).getFactorCalibracionConfianza())
				.isEqualByComparingTo("1.0000");

		VersionPlantillaModel publicada = plantillaService.publicar(tenant.getId(), versionId, administrador);
		assertThat(publicada.getEstado()).isEqualTo(EstadoPlantilla.PUBLICADA);
	}

	@Test
	@DisplayName("una version que empeora el gold no pasa el gate ni se publica")
	void versionQueEmpeoraSeBloquea() {
		PlantillaModel plantilla = plantillaConCampo("GATE2");
		exigirGate(plantilla.getId());
		String v1 = plantilla.getVersiones().get(0).getId();
		agregarCaso(plantilla.getId());
		proveedor.programar("numeroRemito", PresenciaCampo.PRESENTE, "R-GOLD", "1.0000");
		qualityGateService.ejecutar(tenant.getId(), v1);
		plantillaService.publicar(tenant.getId(), v1, administrador);

		VersionPlantillaReqModel nueva = new VersionPlantillaReqModel();
		nueva.setVersionBaseId(v1);
		VersionPlantillaModel v2 = plantillaService.crearVersion(tenant, plantilla.getId(), nueva);
		proveedor.programar("numeroRemito", PresenciaCampo.PRESENTE, "R-MAL", "1.0000");

		EjecucionPruebaModel empeora = qualityGateService.ejecutar(tenant.getId(), v2.getId());
		assertThat(empeora.getResultado()).isEqualTo(ResultadoQualityGate.RECHAZADO);
		assertThat(empeora.getMotivo()).contains("empeora");
		assertThatThrownBy(() -> plantillaService.publicar(tenant.getId(), v2.getId(), administrador))
				.isInstanceOf(ValidacionException.class)
				.hasMessageContaining("GOV-04");
	}

	@Test
	@DisplayName("cambiar el conjunto gold invalida la corrida anterior")
	void huellaNuevaInvalidaCorrida() {
		PlantillaModel plantilla = plantillaConCampo("GATE3");
		exigirGate(plantilla.getId());
		String versionId = plantilla.getVersiones().get(0).getId();
		agregarCaso(plantilla.getId());
		proveedor.programar("numeroRemito", PresenciaCampo.PRESENTE, "R-GOLD", "1.0000");
		qualityGateService.ejecutar(tenant.getId(), versionId);

		qualityGateService.agregarCaso(tenant.getId(), plantilla.getId(), "otro", ESPERADO, archivo());

		assertThatThrownBy(() -> plantillaService.publicar(tenant.getId(), versionId, administrador))
				.isInstanceOf(ValidacionException.class)
				.hasMessageContaining("conjunto vigente");
	}

	@Test
	@DisplayName("SEC-01: un tenant no ve el conjunto gold de otro")
	void aislamientoPorTenant() {
		PlantillaModel plantilla = plantillaConCampo("GATE4");
		agregarCaso(plantilla.getId());
		Tenant ajeno = fabrica.crearTenant("x" + UUID.randomUUID().toString().substring(0, 8));

		assertThatThrownBy(() -> qualityGateService.obtenerConjunto(ajeno.getId(), plantilla.getId()))
				.isInstanceOf(EntidadNoEncontradaException.class);
		assertThatThrownBy(() -> qualityGateService.ejecutar(ajeno.getId(), plantilla.getVersiones().get(0).getId()))
				.isInstanceOf(EntidadNoEncontradaException.class);
	}

	@Test
	@DisplayName("sin exigir el gate se sigue pudiendo publicar como hasta ahora")
	void sinPoliticaNoBloquea() {
		PlantillaModel plantilla = plantillaConCampo("GATE5");
		VersionPlantillaModel publicada = plantillaService.publicar(tenant.getId(),
				plantilla.getVersiones().get(0).getId(), administrador);
		assertThat(publicada.getEstado()).isEqualTo(EstadoPlantilla.PUBLICADA);
	}

	@Test
	@DisplayName("una corrida por debajo del umbral queda rechazada")
	void umbralMinimo() {
		PlantillaModel plantilla = plantillaConCampo("GATE6");
		PoliticaQualityGateReqModel politica = new PoliticaQualityGateReqModel();
		politica.setExigirQualityGate(true);
		politica.setUmbralMinimo(new BigDecimal("0.9900"));
		qualityGateService.actualizarPolitica(tenant.getId(), plantilla.getId(), politica);
		agregarCaso(plantilla.getId());
		proveedor.programar("numeroRemito", PresenciaCampo.NO_FIGURA, null, "0.2000");

		EjecucionPruebaModel ejecucion = qualityGateService.ejecutar(tenant.getId(),
				plantilla.getVersiones().get(0).getId());
		assertThat(ejecucion.getResultado()).isEqualTo(ResultadoQualityGate.RECHAZADO);
		assertThat(ejecucion.getMotivo()).contains("umbral");
	}

	private PlantillaModel plantillaConCampo(String codigo) {
		PlantillaReqModel datos = new PlantillaReqModel();
		datos.setCodigo(codigo);
		datos.setNombre("Plantilla " + codigo);
		PlantillaModel plantilla = plantillaService.crear(tenant, administrador, datos);
		CampoPlantillaReqModel campo = new CampoPlantillaReqModel();
		campo.setClave("numeroRemito");
		campo.setEtiqueta("Numero");
		campo.setTipoDato(TipoDatoCampo.TEXTO);
		campo.setExtraer(true);
		plantillaService.agregarCampo(tenant.getId(), plantilla.getVersiones().get(0).getId(), campo);
		return plantilla;
	}

	private void exigirGate(String plantillaId) {
		PoliticaQualityGateReqModel politica = new PoliticaQualityGateReqModel();
		politica.setExigirQualityGate(true);
		politica.setUmbralMinimo(new BigDecimal("0.8000"));
		qualityGateService.actualizarPolitica(tenant.getId(), plantillaId, politica);
	}

	private void agregarCaso(String plantillaId) {
		qualityGateService.agregarCaso(tenant.getId(), plantillaId, "caso-1", ESPERADO, archivo());
	}

	private MockMultipartFile archivo() {
		return pdf("gold.pdf", List.of("Remito R-GOLD"));
	}
}
