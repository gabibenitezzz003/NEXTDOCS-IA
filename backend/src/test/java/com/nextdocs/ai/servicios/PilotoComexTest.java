package com.nextdocs.ai.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdocs.ai.entidades.CampoPlantilla;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.EjecucionExtraccion;
import com.nextdocs.ai.entidades.EjecucionValidacion;
import com.nextdocs.ai.entidades.HallazgoValidacion;
import com.nextdocs.ai.entidades.ReglaPlantilla;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.ValorExtraido;
import com.nextdocs.ai.entidades.VersionPlantilla;
import com.nextdocs.ai.enumeraciones.EstadoEjecucion;
import com.nextdocs.ai.enumeraciones.PresenciaCampo;
import com.nextdocs.ai.enumeraciones.ResultadoValidacion;
import com.nextdocs.ai.enumeraciones.SeveridadHallazgo;
import com.nextdocs.ai.enumeraciones.TipoReglaValidacion;
import com.nextdocs.ai.enumeraciones.TipoDatoCampo;
import com.nextdocs.ai.repositorios.CampoPlantillaRepository;
import com.nextdocs.ai.repositorios.EjecucionValidacionRepository;
import com.nextdocs.ai.repositorios.HallazgoValidacionRepository;
import com.nextdocs.ai.repositorios.ReglaPlantillaRepository;
import com.nextdocs.ai.repositorios.ValorExtraidoRepository;

@ExtendWith(MockitoExtension.class)
class PilotoComexTest {

	@Mock
	private EjecucionValidacionRepository ejecucionValidacionRepository;

	@Mock
	private HallazgoValidacionRepository hallazgoValidacionRepository;

	@Mock
	private ValorExtraidoRepository valorExtraidoRepository;

	@Mock
	private CampoPlantillaRepository campoPlantillaRepository;

	@Mock
	private ReglaPlantillaRepository reglaPlantillaRepository;

	@Captor
	private ArgumentCaptor<List<HallazgoValidacion>> captorHallazgos;

	private ValidacionDocumentalService servicio;

	private ObjectMapper objectMapper;

	private Tenant tenant;

	private VersionPlantilla version;

	private Documento documento;

	private EjecucionExtraccion extraccion;

	@BeforeEach
	void preparar() {
		objectMapper = new ObjectMapper();
		servicio = new ValidacionDocumentalService(ejecucionValidacionRepository, hallazgoValidacionRepository,
				valorExtraidoRepository, campoPlantillaRepository, reglaPlantillaRepository, objectMapper);
		when(ejecucionValidacionRepository.save(any(EjecucionValidacion.class)))
				.thenAnswer(invocacion -> invocacion.getArgument(0));
		when(hallazgoValidacionRepository.saveAll(captorHallazgos.capture())).thenReturn(new ArrayList<>());

		tenant = new Tenant();
		tenant.setId("tenant-comex");

		version = new VersionPlantilla();
		version.setId("version-comex-1");
		version.setUmbralAutoaprobacion(new BigDecimal("0.85"));

		documento = new Documento();
		documento.setId("doc-1");
		documento.setTenant(tenant);
		documento.setVersionPlantilla(version);

		extraccion = new EjecucionExtraccion();
		extraccion.setId("ext-1");
		extraccion.setTenant(tenant);
		extraccion.setDocumento(documento);
		extraccion.setVersionPlantilla(version);
		extraccion.setEstado(EstadoEjecucion.COMPLETADA);
	}

	@Test
	void caminoFelizApruebaSinHallazgos() {
		List<CampoPlantilla> campos = List.of(
				campo("numeroOperacion", "Numero de operacion", true, "[A-Z]{2,4}-[0-9]{6}"),
				campo("montoTotal", "Monto total", true, null),
				campo("fechaVencimiento", "Fecha de vencimiento", true, null));

		List<ValorExtraido> valores = List.of(
				valor("numeroOperacion", "OP-123456", "OP-123456", PresenciaCampo.PRESENTE, new BigDecimal("0.95")),
				valor("montoTotal", "15000.00", "15000.00", PresenciaCampo.PRESENTE, new BigDecimal("0.92")),
				valor("fechaVencimiento", LocalDate.now().plusDays(30).toString(),
						LocalDate.now().plusDays(30).toString(), PresenciaCampo.PRESENTE, new BigDecimal("0.90")));

		EjecucionValidacion ejecucion = ejecutar(campos, List.of(), valores);

		assertThat(ejecucion.getResultado()).isEqualTo(ResultadoValidacion.APROBADO);
		assertThat(ejecucion.isAutoaprobado()).isTrue();
		assertThat(captorHallazgos.getValue()).isEmpty();
	}

	@Test
	void faltaDocumentoRequeridoGeneraHallazgoBloqueante() {
		List<CampoPlantilla> campos = List.of(campo("numeroOperacion", "Numero de operacion", true, null));
		List<ValorExtraido> valores = List.of();

		EjecucionValidacion ejecucion = ejecutar(campos, List.of(), valores);

		assertThat(ejecucion.getResultado()).isEqualTo(ResultadoValidacion.RECHAZADO);
		assertThat(captorHallazgos.getValue()).anyMatch(h -> h.getCodigoRegla().equals("CAMPO_REQUERIDO")
				&& h.getClaveCampo().equals("numeroOperacion")
				&& h.getSeveridad() == SeveridadHallazgo.BLOQUEANTE);
	}

	@Test
	void confianzaBajaRequiereRevision() {
		List<CampoPlantilla> campos = List.of(
				campo("numeroOperacion", "Numero de operacion", true, null, new BigDecimal("0.90")));
		List<ValorExtraido> valores = List.of(
				valor("numeroOperacion", "OP-123456", "OP-123456", PresenciaCampo.PRESENTE, new BigDecimal("0.50")));

		EjecucionValidacion ejecucion = ejecutar(campos, List.of(), valores);

		assertThat(ejecucion.getResultado()).isEqualTo(ResultadoValidacion.OBSERVADO);
		assertThat(captorHallazgos.getValue()).anyMatch(h -> h.getCodigoRegla().equals("CONFIANZA_BAJA"));
	}

	@Test
	void formatoInvalidoRequiereRevision() {
		List<CampoPlantilla> campos = List.of(
				campo("numeroOperacion", "Numero de operacion", true, "[A-Z]{2,4}-[0-9]{6}"));
		List<ValorExtraido> valores = List.of(
				valor("numeroOperacion", "123", "123", PresenciaCampo.PRESENTE, new BigDecimal("0.95")));

		EjecucionValidacion ejecucion = ejecutar(campos, List.of(), valores);

		assertThat(ejecucion.getResultado()).isEqualTo(ResultadoValidacion.OBSERVADO);
		assertThat(captorHallazgos.getValue()).anyMatch(h -> h.getCodigoRegla().equals("FORMATO_INVALIDO"));
	}

	@Test
	void diferenciaEntreMontosBloquea() {
		List<CampoPlantilla> campos = List.of(
				campo("montoTotal", "Monto total", true, null),
				campo("sumaItems", "Suma de items", true, null));
		List<ReglaPlantilla> reglas = List.of(
				regla("CROSS_DOC_MONTOS", "Comparacion de montos", TipoReglaValidacion.COMPARACION_CAMPOS,
						"montoTotal", Map.of("campoA", "montoTotal", "campoB", "sumaItems")));
		List<ValorExtraido> valores = List.of(
				valor("montoTotal", "15000.00", "15000.00", PresenciaCampo.PRESENTE, new BigDecimal("0.95")),
				valor("sumaItems", "14500.00", "14500.00", PresenciaCampo.PRESENTE, new BigDecimal("0.95")));

		EjecucionValidacion ejecucion = ejecutar(campos, reglas, valores);

		assertThat(ejecucion.getResultado()).isEqualTo(ResultadoValidacion.RECHAZADO);
		assertThat(captorHallazgos.getValue()).anyMatch(h -> h.getCodigoRegla().equals("CROSS_DOC_MONTOS")
				&& h.getSeveridad() == SeveridadHallazgo.BLOQUEANTE);
	}

	@Test
	void incotermNoPermitidoBloquea() {
		List<CampoPlantilla> campos = List.of(campo("incoterm", "Incoterm", true, null));
		List<ReglaPlantilla> reglas = List.of(
				regla("INCOTERM_VALIDO", "Incoterm permitido", TipoReglaValidacion.CATALOGO, "incoterm",
						Map.of("valores", List.of("FOB", "CIF", "EXW"))));
		List<ValorExtraido> valores = List.of(
				valor("incoterm", "DDP", "DDP", PresenciaCampo.PRESENTE, new BigDecimal("0.95")));

		EjecucionValidacion ejecucion = ejecutar(campos, reglas, valores);

		assertThat(ejecucion.getResultado()).isEqualTo(ResultadoValidacion.RECHAZADO);
		assertThat(captorHallazgos.getValue()).anyMatch(h -> h.getCodigoRegla().equals("INCOTERM_VALIDO"));
	}

	@Test
	void documentoVencidoBloquea() {
		List<CampoPlantilla> campos = List.of(campo("fechaVencimiento", "Fecha de vencimiento", true, null));
		List<ReglaPlantilla> reglas = List.of(
				regla("VIGENCIA_VALIDA", "Vigencia del documento", TipoReglaValidacion.VIGENCIA, "fechaVencimiento",
						Map.of("diasTolerancia", 0)));
		List<ValorExtraido> valores = List.of(
				valor("fechaVencimiento", LocalDate.now().minusDays(5).toString(),
						LocalDate.now().minusDays(5).toString(), PresenciaCampo.PRESENTE, new BigDecimal("0.95")));

		EjecucionValidacion ejecucion = ejecutar(campos, reglas, valores);

		assertThat(ejecucion.getResultado()).isEqualTo(ResultadoValidacion.RECHAZADO);
		assertThat(captorHallazgos.getValue()).anyMatch(h -> h.getCodigoRegla().equals("VIGENCIA_VALIDA"));
	}

	@Test
	void nuevaVersionRequiereCampoAdicional() {
		List<CampoPlantilla> campos = List.of(
				campo("numeroOperacion", "Numero de operacion", true, null),
				campo("codigoSeguimiento", "Codigo de seguimiento", true, null));
		List<ValorExtraido> valores = List.of(
				valor("numeroOperacion", "OP-123456", "OP-123456", PresenciaCampo.PRESENTE, new BigDecimal("0.95")));

		EjecucionValidacion ejecucion = ejecutar(campos, List.of(), valores);

		assertThat(ejecucion.getResultado()).isEqualTo(ResultadoValidacion.RECHAZADO);
		assertThat(captorHallazgos.getValue()).anyMatch(h -> h.getClaveCampo().equals("codigoSeguimiento")
				&& h.getCodigoRegla().equals("CAMPO_REQUERIDO"));
	}

	private EjecucionValidacion ejecutar(List<CampoPlantilla> campos, List<ReglaPlantilla> reglas,
			List<ValorExtraido> valores) {
		when(campoPlantillaRepository.listarPorVersion(version.getId())).thenReturn(campos);
		when(reglaPlantillaRepository.listarActivasPorVersion(version.getId())).thenReturn(reglas);
		when(valorExtraidoRepository.listarPorEjecucion(extraccion.getId())).thenReturn(valores);
		return servicio.validar(documento, extraccion);
	}

	private CampoPlantilla campo(String clave, String etiqueta, boolean requerido, String expresionRegular) {
		return campo(clave, etiqueta, requerido, expresionRegular, null);
	}

	private CampoPlantilla campo(String clave, String etiqueta, boolean requerido, String expresionRegular,
			BigDecimal umbralConfianza) {
		CampoPlantilla campo = new CampoPlantilla();
		campo.setId(clave + "-id");
		campo.setVersionPlantilla(version);
		campo.setClave(clave);
		campo.setEtiqueta(etiqueta);
		campo.setTipoDato(TipoDatoCampo.TEXTO);
		campo.setRequerido(requerido);
		campo.setExtraer(true);
		campo.setValidar(true);
		campo.setExpresionRegular(expresionRegular);
		campo.setUmbralConfianza(umbralConfianza);
		return campo;
	}

	private ReglaPlantilla regla(String codigo, String nombre, TipoReglaValidacion tipo, String campoObjetivo,
			Map<String, Object> configuracion) {
		ReglaPlantilla regla = new ReglaPlantilla();
		regla.setId(codigo + "-id");
		regla.setVersionPlantilla(version);
		regla.setCodigo(codigo);
		regla.setNombre(nombre);
		regla.setTipo(tipo);
		regla.setCampoObjetivo(campoObjetivo);
		regla.setSeveridad(SeveridadHallazgo.BLOQUEANTE);
		regla.setActiva(true);
		try {
			regla.setConfiguracion(objectMapper.writeValueAsString(configuracion));
		} catch (Exception e) {
			regla.setConfiguracion(null);
		}
		return regla;
	}

	private ValorExtraido valor(String clave, String crudo, String normalizado, PresenciaCampo presencia,
			BigDecimal confianza) {
		ValorExtraido valor = new ValorExtraido();
		valor.setId(clave + "-valor");
		valor.setTenant(tenant);
		valor.setDocumento(documento);
		valor.setEjecucion(extraccion);
		valor.setClaveCampo(clave);
		valor.setValorCrudo(crudo);
		valor.setValorNormalizado(normalizado);
		valor.setPresencia(presencia);
		valor.setConfianza(confianza);
		return valor;
	}

}
