package com.nextdocs.ai.integracion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import com.nextdocs.ai.modelos.ResultadoLimiteModel;
import com.nextdocs.ai.servicios.LimiteUsoService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = { "nextdocs.limite-uso.activo=true",
		"nextdocs.limite-uso.peticionesPorMinutoPorPrincipal=5",
		"nextdocs.limite-uso.peticionesPorMinutoPorTenant=8",
		"nextdocs.limite-uso.ingestasPorMinutoPorTenant=2" })
class LimiteUsoIT extends PruebaIntegracion {

	@Autowired
	private LimiteUsoService limiteUsoService;

	@Test
	@DisplayName("deja pasar hasta el limite y rechaza a partir de ahi")
	void rechazaAlSuperarElLimite() {
		String identificador = "principal-" + UUID.randomUUID();

		for (int intento = 1; intento <= 5; intento++) {
			assertThat(limiteUsoService.consumir(LimiteUsoService.ALCANCE_PRINCIPAL, identificador, 5).isPermitido())
					.as("peticion numero " + intento).isTrue();
		}

		ResultadoLimiteModel excedido = limiteUsoService.consumir(LimiteUsoService.ALCANCE_PRINCIPAL, identificador,
				5);

		assertThat(excedido.isPermitido()).isFalse();
		assertThat(excedido.getAlcanceExcedido()).isEqualTo(LimiteUsoService.ALCANCE_PRINCIPAL);
		assertThat(excedido.getEsperaSegundos()).isBetween(1L, 60L);
	}

	@Test
	@DisplayName("informa cuantas peticiones quedan en la ventana")
	void informaLasRestantes() {
		String identificador = "principal-" + UUID.randomUUID();

		assertThat(limiteUsoService.consumir(LimiteUsoService.ALCANCE_PRINCIPAL, identificador, 3).getRestantes())
				.isEqualTo(2);
		assertThat(limiteUsoService.consumir(LimiteUsoService.ALCANCE_PRINCIPAL, identificador, 3).getRestantes())
				.isEqualTo(1);
		assertThat(limiteUsoService.consumir(LimiteUsoService.ALCANCE_PRINCIPAL, identificador, 3).getRestantes())
				.isZero();
	}

	@Test
	@DisplayName("dos identificadores distintos no comparten cuota")
	void cuotasIndependientes() {
		String uno = "principal-" + UUID.randomUUID();
		String otro = "principal-" + UUID.randomUUID();

		limiteUsoService.consumir(LimiteUsoService.ALCANCE_PRINCIPAL, uno, 1);
		assertThat(limiteUsoService.consumir(LimiteUsoService.ALCANCE_PRINCIPAL, uno, 1).isPermitido()).isFalse();
		assertThat(limiteUsoService.consumir(LimiteUsoService.ALCANCE_PRINCIPAL, otro, 1).isPermitido()).isTrue();
	}

	@Test
	@DisplayName("el mismo identificador en alcances distintos lleva contadores separados")
	void alcancesIndependientes() {
		String identificador = "tenant-" + UUID.randomUUID();

		limiteUsoService.consumir(LimiteUsoService.ALCANCE_TENANT, identificador, 1);

		assertThat(limiteUsoService.consumir(LimiteUsoService.ALCANCE_TENANT, identificador, 1).isPermitido())
				.isFalse();
		assertThat(limiteUsoService.consumir(LimiteUsoService.ALCANCE_INGESTA, identificador, 1).isPermitido())
				.isTrue();
	}

	@Test
	@DisplayName("un limite en cero o negativo desactiva el control")
	void limiteCeroNoLimita() {
		String identificador = "principal-" + UUID.randomUUID();

		for (int intento = 0; intento < 20; intento++) {
			assertThat(limiteUsoService.consumir(LimiteUsoService.ALCANCE_PRINCIPAL, identificador, 0).isPermitido())
					.isTrue();
		}
	}

	@Test
	@DisplayName("la ingesta se limita mas fuerte que la lectura")
	void laIngestaEsMasEstricta() {
		assertThat(limiteUsoService.limiteDeIngesta()).isLessThan(limiteUsoService.limitePorTenant());
		assertThat(limiteUsoService.esRutaDeIngesta("/api/v1/documentos")).isTrue();
		assertThat(limiteUsoService.esRutaDeIngesta("/api/v1/plantillas")).isFalse();
	}

	@Test
	@DisplayName("las rutas de salud y autenticacion estan exentas")
	void rutasExentas() {
		assertThat(limiteUsoService.esRutaExenta("/actuator/health")).isTrue();
		assertThat(limiteUsoService.esRutaExenta("/api/v1/autenticacion/ingresar")).isTrue();
		assertThat(limiteUsoService.esRutaExenta("/api/v1/documentos")).isFalse();
	}
}
