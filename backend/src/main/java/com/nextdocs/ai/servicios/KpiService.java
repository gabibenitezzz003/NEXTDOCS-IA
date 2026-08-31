package com.nextdocs.ai.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.nextdocs.ai.convertidores.DocumentoConverter;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.enumeraciones.EstadoDocumento;
import com.nextdocs.ai.enumeraciones.EstadoExcepcion;
import com.nextdocs.ai.enumeraciones.PresenciaCampo;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.BarraKpiModel;
import com.nextdocs.ai.modelos.DocumentoModel;
import com.nextdocs.ai.modelos.IndicadorKpiModel;
import com.nextdocs.ai.modelos.KpiPlantillaModel;
import com.nextdocs.ai.modelos.KpiResumenModel;
import com.nextdocs.ai.modelos.RangoKpiModel;
import com.nextdocs.ai.repositorios.ArchivoDocumentoRepository;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.repositorios.EntregaWebhookRepository;
import com.nextdocs.ai.repositorios.ExcepcionDocumentalRepository;
import com.nextdocs.ai.repositorios.TenantRepository;
import com.nextdocs.ai.repositorios.ValorExtraidoRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KpiService {

	public static final String RECIBIDOS = "documentosRecibidos";

	public static final String CERRADOS = "documentosCerrados";

	public static final String AUTOMATIZACION = "automatizacion";

	public static final String CUMPLIMIENTO_SLA = "cumplimientoSla";

	public static final String CICLO_P50 = "tiempoCicloP50";

	public static final String CICLO_P90 = "tiempoCicloP90";

	public static final String EXCEPCIONES_ABIERTAS = "excepcionesAbiertas";

	public static final String EXCEPCIONES_VENCIDAS = "excepcionesVencidas";

	public static final String POR_VENCER = "documentosPorVencer";

	public static final String ALMACENAMIENTO = "almacenamientoUtilizado";

	public static final String ENTREGA_EVENTOS = "entregaDeEventos";

	public static final Set<String> CON_POBLACION = Set.of(RECIBIDOS, CERRADOS, CICLO_P50, CICLO_P90);

	private static final int DIAS_VENTANA_VENCIMIENTO = 30;

	private static final int TOPE_POBLACION = 200;

	private static final BigDecimal CIEN = new BigDecimal("100");

	private final DocumentoRepository documentoRepository;

	private final ExcepcionDocumentalRepository excepcionDocumentalRepository;

	private final ValorExtraidoRepository valorExtraidoRepository;

	private final ArchivoDocumentoRepository archivoDocumentoRepository;

	private final EntregaWebhookRepository entregaWebhookRepository;

	private final TenantRepository tenantRepository;

	private final DocumentoConverter documentoConverter;

	public KpiService(DocumentoRepository documentoRepository,
			ExcepcionDocumentalRepository excepcionDocumentalRepository,
			ValorExtraidoRepository valorExtraidoRepository, ArchivoDocumentoRepository archivoDocumentoRepository,
			EntregaWebhookRepository entregaWebhookRepository, TenantRepository tenantRepository,
			DocumentoConverter documentoConverter) {
		this.documentoRepository = documentoRepository;
		this.excepcionDocumentalRepository = excepcionDocumentalRepository;
		this.valorExtraidoRepository = valorExtraidoRepository;
		this.archivoDocumentoRepository = archivoDocumentoRepository;
		this.entregaWebhookRepository = entregaWebhookRepository;
		this.tenantRepository = tenantRepository;
		this.documentoConverter = documentoConverter;
	}

	@Transactional(readOnly = true)
	public KpiResumenModel resumir(String tenantId, Instant desde, Instant hasta) {
		RangoKpiModel rango = RangoKpiModel.de(desde, hasta);
		RangoKpiModel anterior = rango.anterior();
		Instant ahora = Instant.now();

		KpiResumenModel resumen = new KpiResumenModel();
		resumen.setRango(rango);

		long recibidos = documentoRepository.contarRaicesRecibidasEntre(tenantId, rango.getDesde(),
				rango.getHasta());
		long recibidosAnterior = documentoRepository.contarRaicesRecibidasEntre(tenantId, anterior.getDesde(),
				anterior.getHasta());
		resumen.getIndicadores().add(IndicadorKpiModel.conteo(RECIBIDOS, "Documentos recibidos", recibidos,
				recibidosAnterior,
				"COUNT(documento) recibido en el rango, sin contar los segmentos de un PDF partido"));

		long cerrados = documentoRepository.contarCerradosEntre(tenantId, rango.getDesde(), rango.getHasta());
		long cerradosAnterior = documentoRepository.contarCerradosEntre(tenantId, anterior.getDesde(),
				anterior.getHasta());
		resumen.getIndicadores().add(IndicadorKpiModel.conteo(CERRADOS, "Documentos cerrados", cerrados,
				cerradosAnterior, "COUNT(documento) con estado CERRADO en el rango"));

		long sinIntervencion = documentoRepository.contarCerradosSinIntervencion(tenantId, rango.getDesde(),
				rango.getHasta());
		long sinIntervencionAnterior = documentoRepository.contarCerradosSinIntervencion(tenantId,
				anterior.getDesde(), anterior.getHasta());
		resumen.getIndicadores()
				.add(IndicadorKpiModel.porcentaje(AUTOMATIZACION, "Automatizacion", razon(sinIntervencion, cerrados),
						razon(sinIntervencionAnterior, cerradosAnterior),
						"cerrados sin ninguna revision humana / cerrados", sinIntervencion, cerrados));

		long resueltas = excepcionDocumentalRepository.contarResueltasEntre(tenantId, rango.getDesde(),
				rango.getHasta());
		long dentroDeSla = excepcionDocumentalRepository.contarResueltasDentroDeSla(tenantId, rango.getDesde(),
				rango.getHasta());
		long resueltasAnterior = excepcionDocumentalRepository.contarResueltasEntre(tenantId, anterior.getDesde(),
				anterior.getHasta());
		long dentroAnterior = excepcionDocumentalRepository.contarResueltasDentroDeSla(tenantId,
				anterior.getDesde(), anterior.getHasta());
		resumen.getIndicadores()
				.add(IndicadorKpiModel.porcentaje(CUMPLIMIENTO_SLA, "Cumplimiento SLA",
						razon(dentroDeSla, resueltas), razon(dentroAnterior, resueltasAnterior),
						"excepciones resueltas antes de su vencimiento / excepciones resueltas", dentroDeSla,
						resueltas));

		List<Long> ciclos = ciclosEnHoras(tenantId, rango);
		List<Long> ciclosAnterior = ciclosEnHoras(tenantId, anterior);
		resumen.getIndicadores().add(IndicadorKpiModel.duracion(CICLO_P50, "Tiempo de ciclo p50",
				percentil(ciclos, 50), percentil(ciclosAnterior, 50), "percentil 50 de cerrado menos recibido"));
		resumen.getIndicadores().add(IndicadorKpiModel.duracion(CICLO_P90, "Tiempo de ciclo p90",
				percentil(ciclos, 90), percentil(ciclosAnterior, 90), "percentil 90 de cerrado menos recibido"));

		long abiertas = excepcionDocumentalRepository.contarPorEstado(tenantId, EstadoExcepcion.ABIERTA)
				+ excepcionDocumentalRepository.contarPorEstado(tenantId, EstadoExcepcion.EN_CURSO);
		resumen.getIndicadores().add(IndicadorKpiModel.conteo(EXCEPCIONES_ABIERTAS, "Excepciones abiertas",
				abiertas, null, "COUNT(excepcion) en estado ABIERTA o EN_CURSO"));

		long vencidas = excepcionDocumentalRepository.contarAbiertasVencidas(tenantId, ahora);
		resumen.getIndicadores().add(IndicadorKpiModel.conteo(EXCEPCIONES_VENCIDAS, "Excepciones vencidas",
				vencidas, null, "COUNT(excepcion) sin resolver con vencimiento pasado"));

		long porVencer = documentoRepository.contarPorVencerEntre(tenantId, ahora,
				ahora.plus(DIAS_VENTANA_VENCIMIENTO, ChronoUnit.DAYS));
		resumen.getIndicadores().add(IndicadorKpiModel.conteo(POR_VENCER, "Documentos por vencer", porVencer, null,
				"COUNT(documento) con retencion venciendo en los proximos " + DIAS_VENTANA_VENCIMIENTO + " dias"));

		resumen.getIndicadores().add(indicadorAlmacenamiento(tenantId));

		long intentadas = entregaWebhookRepository.contarIntentadas(tenantId, rango.getDesde(), rango.getHasta());
		long entregadas = entregaWebhookRepository.contarEntregadas(tenantId, rango.getDesde(), rango.getHasta());
		resumen.getIndicadores()
				.add(IndicadorKpiModel.porcentaje(ENTREGA_EVENTOS, "Entrega de eventos",
						razon(entregadas, intentadas), null, "entregas ENTREGADO / entregas intentadas", entregadas,
						intentadas));

		resumen.getIndicadores()
				.forEach(indicador -> indicador.setTienePoblacion(CON_POBLACION.contains(indicador.getClave())));
		resumen.setPorEstado(conteoPorEstado(tenantId));
		return resumen;
	}

	@Transactional(readOnly = true)
	public List<KpiPlantillaModel> porPlantilla(String tenantId, Instant desde, Instant hasta) {
		RangoKpiModel rango = RangoKpiModel.de(desde, hasta);

		Map<String, KpiPlantillaModel> porCodigo = new HashMap<>();
		for (Object[] fila : documentoRepository.agruparPorPlantillaYEstado(tenantId, rango.getDesde(),
				rango.getHasta())) {
			String codigo = (String) fila[0];
			KpiPlantillaModel modelo = porCodigo.computeIfAbsent(codigo, clave -> {
				KpiPlantillaModel nuevo = new KpiPlantillaModel();
				nuevo.setCodigo(clave);
				nuevo.setNombre((String) fila[1]);
				return nuevo;
			});
			EstadoDocumento estado = (EstadoDocumento) fila[2];
			long cantidad = ((Number) fila[3]).longValue();
			modelo.getPorEstado().put(estado.name(), cantidad);
			modelo.setVolumen(modelo.getVolumen() + cantidad);
		}

		Map<String, long[]> presencias = new HashMap<>();
		for (Object[] fila : valorExtraidoRepository.agruparPresenciaPorPlantilla(tenantId, rango.getDesde(),
				rango.getHasta())) {
			String codigo = (String) fila[0];
			PresenciaCampo presencia = (PresenciaCampo) fila[1];
			long cantidad = ((Number) fila[2]).longValue();
			long[] acumulado = presencias.computeIfAbsent(codigo, clave -> new long[2]);
			acumulado[1] += cantidad;
			if (presencia == PresenciaCampo.PRESENTE) {
				acumulado[0] += cantidad;
			}
		}

		Map<String, Long> excepcionesPorPlantilla = new HashMap<>();
		for (Object[] fila : excepcionDocumentalRepository.agruparDocumentosConAbiertasPorPlantilla(tenantId,
				rango.getDesde(), rango.getHasta())) {
			excepcionesPorPlantilla.put((String) fila[0], ((Number) fila[1]).longValue());
		}

		List<KpiPlantillaModel> resultado = new ArrayList<>(porCodigo.values());
		for (KpiPlantillaModel modelo : resultado) {
			completarBarras(modelo, presencias.get(modelo.getCodigo()),
					excepcionesPorPlantilla.getOrDefault(modelo.getCodigo(), 0L));
		}
		resultado.sort(Comparator.comparingLong(KpiPlantillaModel::getVolumen).reversed());
		return resultado;
	}

	@Transactional(readOnly = true)
	public List<DocumentoModel> poblacion(String tenantId, String indicador, Instant desde, Instant hasta) {
		if (!CON_POBLACION.contains(indicador)) {
			throw new ValidacionException(
					"El indicador " + indicador + " no tiene poblacion auditable de documentos");
		}
		RangoKpiModel rango = RangoKpiModel.de(desde, hasta);
		PageRequest tope = PageRequest.of(0, TOPE_POBLACION);
		List<Documento> documentos = switch (indicador) {
			case RECIBIDOS -> documentoRepository.listarRaicesRecibidasEntre(tenantId, rango.getDesde(),
					rango.getHasta(), tope);
			case CERRADOS -> documentoRepository.listarCerradosEntre(tenantId, rango.getDesde(),
					rango.getHasta(), tope);
			default -> documentoRepository.listarCerradosConCiclo(tenantId, rango.getDesde(), rango.getHasta());
		};
		return documentoConverter.aModelos(documentos.size() > TOPE_POBLACION
				? documentos.subList(0, TOPE_POBLACION)
				: documentos);
	}

	private IndicadorKpiModel indicadorAlmacenamiento(String tenantId) {
		long usado = archivoDocumentoRepository.sumarTamanoPorTenant(tenantId);
		long cuota = tenantRepository.findById(tenantId).map(tenant -> tenant.getCuotaAlmacenamientoBytes())
				.orElse(0L);
		IndicadorKpiModel indicador = IndicadorKpiModel.porcentaje(ALMACENAMIENTO, "Almacenamiento utilizado",
				cuota > 0 ? razon(usado, cuota) : null, null, "bytes almacenados / cuota del tenant", usado, cuota);
		indicador.setDetalle(cuota > 0 ? null : "El tenant no tiene cuota de almacenamiento configurada");
		return indicador;
	}

	private void completarBarras(KpiPlantillaModel modelo, long[] presencia, long excepciones) {
		long cerrados = modelo.getPorEstado().getOrDefault(EstadoDocumento.CERRADO.name(), 0L);
		long aprobados = modelo.getPorEstado().getOrDefault(EstadoDocumento.APROBADO.name(), 0L);
		long observados = modelo.getPorEstado().getOrDefault(EstadoDocumento.OBSERVADO.name(), 0L);
		long rechazados = modelo.getPorEstado().getOrDefault(EstadoDocumento.RECHAZADO.name(), 0L);

		modelo.getBarras().add(BarraKpiModel.de("avance", "Avance",
				razon(cerrados + aprobados + rechazados, modelo.getVolumen()),
				"documentos en estado terminal o aprobado / volumen"));
		modelo.getBarras().add(BarraKpiModel.de("documentacion", "Completitud documental",
				presencia == null || presencia[1] == 0 ? null : razon(presencia[0], presencia[1]),
				"campos con presencia PRESENTE / campos extraidos"));
		modelo.getBarras().add(BarraKpiModel.de("automatizacion", "Automatizacion",
				razon(aprobados, aprobados + observados),
				"aprobados / (aprobados mas observados que necesitaron revision)"));
		modelo.getBarras().add(BarraKpiModel.de("excepciones", "Sin excepciones abiertas",
				modelo.getVolumen() == 0 ? null : invertir(razon(excepciones, modelo.getVolumen())),
				"1 menos documentos del periodo con alguna excepcion abierta / volumen del periodo"));
		modelo.setDocumentosConExcepciones(excepciones);
		modelo.setSalud(calcularSalud(modelo));
	}

	private String calcularSalud(KpiPlantillaModel modelo) {
		BigDecimal peor = null;
		for (BarraKpiModel barra : modelo.getBarras()) {
			if (barra.getValor() == null) {
				continue;
			}
			peor = peor == null ? barra.getValor() : peor.min(barra.getValor());
		}
		if (peor == null) {
			return "SIN_DATOS";
		}
		if (peor.compareTo(new BigDecimal("0.85")) >= 0) {
			return "OK";
		}
		if (peor.compareTo(new BigDecimal("0.60")) >= 0) {
			return "ATENCION";
		}
		return "CRITICO";
	}

	private Map<String, Long> conteoPorEstado(String tenantId) {
		Map<String, Long> conteo = new HashMap<>();
		for (EstadoDocumento estado : EstadoDocumento.values()) {
			conteo.put(estado.name(), documentoRepository.contarPorEstado(tenantId, estado));
		}
		return conteo;
	}

	private List<Long> ciclosEnHoras(String tenantId, RangoKpiModel rango) {
		List<Long> horas = new ArrayList<>();
		for (Documento documento : documentoRepository.listarCerradosConCiclo(tenantId, rango.getDesde(),
				rango.getHasta())) {
			horas.add(Duration.between(documento.getRecibido(), documento.getCerrado()).toHours());
		}
		horas.sort(Comparator.naturalOrder());
		return horas;
	}

	private BigDecimal percentil(List<Long> ordenados, int percentil) {
		if (ordenados.isEmpty()) {
			return null;
		}
		int indice = (int) Math.ceil((percentil / 100.0) * ordenados.size()) - 1;
		return BigDecimal.valueOf(ordenados.get(Math.max(indice, 0)));
	}

	private BigDecimal razon(long numerador, long denominador) {
		if (denominador <= 0) {
			return null;
		}
		return BigDecimal.valueOf(numerador).divide(BigDecimal.valueOf(denominador), 4, RoundingMode.HALF_UP);
	}

	private BigDecimal invertir(BigDecimal valor) {
		if (valor == null) {
			return null;
		}
		return BigDecimal.ONE.subtract(valor).max(BigDecimal.ZERO);
	}

	public static BigDecimal aPorcentaje(BigDecimal razon) {
		return razon == null ? null : razon.multiply(CIEN).setScale(1, RoundingMode.HALF_UP);
	}
}
