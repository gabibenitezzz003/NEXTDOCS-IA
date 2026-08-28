package com.nextdocs.ai.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.nextdocs.ai.entidades.EjecucionExtraccion;
import com.nextdocs.ai.entidades.PoliticaCostoTenant;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.AccionPresupuestoCosto;
import com.nextdocs.ai.enumeraciones.EstadoDocumento;
import com.nextdocs.ai.enumeraciones.ProveedorDocumentalIa;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.CostoAgrupadoModel;
import com.nextdocs.ai.modelos.PoliticaCostoModel;
import com.nextdocs.ai.modelos.PoliticaCostoReqModel;
import com.nextdocs.ai.modelos.ResumenCostoModel;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.repositorios.EjecucionExtraccionRepository;
import com.nextdocs.ai.repositorios.PoliticaCostoTenantRepository;
import com.nextdocs.ai.repositorios.TenantRepository;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObservabilidadService {

	public static final String ENTIDAD = "PoliticaCostoTenant";

	public static final String METRICA_COSTO = "nextdocs.extraccion.costo";

	public static final String METRICA_TOKENS_ENTRADA = "nextdocs.extraccion.tokens.entrada";

	public static final String METRICA_TOKENS_SALIDA = "nextdocs.extraccion.tokens.salida";

	public static final String METRICA_DURACION = "nextdocs.extraccion.duracion";

	private static final List<EstadoDocumento> ESTADOS_CORRECTOS = List.of(EstadoDocumento.APROBADO,
			EstadoDocumento.CERRADO);

	private static final BigDecimal UMBRAL_POR_DEFECTO = new BigDecimal("0.8000");

	private final PoliticaCostoTenantRepository politicaCostoTenantRepository;

	private final EjecucionExtraccionRepository ejecucionExtraccionRepository;

	private final DocumentoRepository documentoRepository;

	private final TenantRepository tenantRepository;

	private final AuditoriaService auditoriaService;

	private final MeterRegistry meterRegistry;

	public ObservabilidadService(PoliticaCostoTenantRepository politicaCostoTenantRepository,
			EjecucionExtraccionRepository ejecucionExtraccionRepository, DocumentoRepository documentoRepository,
			TenantRepository tenantRepository, AuditoriaService auditoriaService, MeterRegistry meterRegistry) {
		this.politicaCostoTenantRepository = politicaCostoTenantRepository;
		this.ejecucionExtraccionRepository = ejecucionExtraccionRepository;
		this.documentoRepository = documentoRepository;
		this.tenantRepository = tenantRepository;
		this.auditoriaService = auditoriaService;
		this.meterRegistry = meterRegistry;
	}

	@Transactional(readOnly = true)
	public ResumenCostoModel resumir(String tenantId, Instant desde, Instant hasta) {
		exigirTenant(tenantId);
		Instant fin = hasta == null ? Instant.now() : hasta;
		Instant inicio = desde == null ? inicioMesUtc(fin) : desde;
		if (!inicio.isBefore(fin)) {
			throw new ValidacionException("El intervalo de costo exige desde anterior a hasta");
		}
		PoliticaCostoTenant politica = politicaCostoTenantRepository.buscarPorTenant(tenantId).orElse(null);
		Object[] totales = primera(ejecucionExtraccionRepository.totalesEntre(tenantId, inicio, fin));
		long extracciones = entero(totales, 3);
		long duracionTotal = entero(totales, 4);
		long documentosCorrectos = ejecucionExtraccionRepository.contarDocumentosCorrectosEntre(tenantId, inicio, fin,
				ESTADOS_CORRECTOS);
		BigDecimal costoInferencia = decimal(totales, 0);

		ResumenCostoModel modelo = new ResumenCostoModel();
		modelo.setDesde(inicio);
		modelo.setHasta(fin);
		modelo.setMoneda(politica == null ? "USD" : politica.getMoneda());
		modelo.setDocumentosRecibidos(documentoRepository.contarRecibidosEntre(tenantId, inicio, fin));
		modelo.setDocumentosCorrectos(documentosCorrectos);
		modelo.setExtracciones(extracciones);
		modelo.setExtraccionesCompletadas(entero(totales, 5));
		modelo.setExtraccionesFallidas(entero(totales, 6));
		modelo.setTokensEntrada(entero(totales, 1));
		modelo.setTokensSalida(entero(totales, 2));
		modelo.setCostoInferencia(costoInferencia);
		modelo.setCostoEfectivoPorDocumentoCorrecto(efectivo(costoInferencia, documentosCorrectos));
		modelo.setDuracionMediaMilisegundos(extracciones == 0 ? 0 : duracionTotal / extracciones);
		modelo.setPolitica(aModelo(politica));
		modelo.setPorProveedor(porProveedor(tenantId, inicio, fin));
		modelo.setPorPlantilla(porPlantilla(tenantId, inicio, fin));
		completarPresupuesto(modelo, politica, tenantId, fin);
		return modelo;
	}

	@Transactional(readOnly = true)
	public PoliticaCostoModel obtenerPolitica(String tenantId) {
		exigirTenant(tenantId);
		return aModelo(politicaCostoTenantRepository.buscarPorTenant(tenantId).orElse(null));
	}

	@Transactional
	public PoliticaCostoModel actualizarPolitica(String tenantId, PoliticaCostoReqModel datos) {
		Tenant tenant = exigirTenant(tenantId);
		PoliticaCostoTenant politica = politicaCostoTenantRepository.buscarPorTenant(tenantId)
				.orElseGet(() -> nueva(tenant));
		politica.setActivo(datos.isActivo());
		politica.setPresupuestoMensual(presupuestoDe(datos.getPresupuestoMensual()));
		politica.setUmbralAlerta(umbralDe(datos.getUmbralAlerta()));
		politica.setAccionAlExceder(
				datos.getAccionAlExceder() == null ? AccionPresupuestoCosto.ALERTA : datos.getAccionAlExceder());
		politica.setMoneda(monedaDe(datos.getMoneda()));
		politicaCostoTenantRepository.save(politica);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.POLITICA_MODIFICADA, ENTIDAD, politica.getId(),
				Map.of("activo", politica.isActivo(),
						"presupuestoMensual",
						politica.getPresupuestoMensual() == null ? "" : politica.getPresupuestoMensual().toPlainString(),
						"accionAlExceder", politica.getAccionAlExceder().name()));
		return aModelo(politica);
	}

	@Transactional(readOnly = true)
	public void exigirPresupuesto(String tenantId) {
		PoliticaCostoTenant politica = politicaCostoTenantRepository.buscarPorTenant(tenantId).orElse(null);
		if (!bloquea(politica, tenantId, Instant.now())) {
			return;
		}
		throw new ValidacionException(
				"El presupuesto mensual de IA del tenant esta agotado y la ingesta queda bloqueada");
	}

	public void registrarUso(EjecucionExtraccion ejecucion) {
		if (ejecucion == null || ejecucion.getTenant() == null) {
			return;
		}
		String tenant = ejecucion.getTenant().getCodigo() == null ? ejecucion.getTenant().getId()
				: ejecucion.getTenant().getCodigo();
		String proveedor = ejecucion.getProveedor() == null ? "DESCONOCIDO" : ejecucion.getProveedor().name();
		Tags etiquetas = Tags.of("tenant", tenant, "proveedor", proveedor);
		meterRegistry.counter(METRICA_COSTO, etiquetas)
				.increment(ejecucion.getCosto() == null ? 0 : ejecucion.getCosto().doubleValue());
		meterRegistry.counter(METRICA_TOKENS_ENTRADA, etiquetas).increment(ejecucion.getTokensEntrada());
		meterRegistry.counter(METRICA_TOKENS_SALIDA, etiquetas).increment(ejecucion.getTokensSalida());
		meterRegistry.counter(METRICA_DURACION, etiquetas).increment(ejecucion.getDuracionMilisegundos());
	}

	private void completarPresupuesto(ResumenCostoModel modelo, PoliticaCostoTenant politica, String tenantId,
			Instant referencia) {
		Instant inicioMes = inicioMesUtc(referencia);
		BigDecimal gastado = decimal(primera(ejecucionExtraccionRepository.totalesEntre(tenantId, inicioMes, referencia)),
				0);
		modelo.setGastadoEnPeriodoPresupuesto(gastado);
		if (politica == null || !politica.isActivo() || politica.getPresupuestoMensual() == null) {
			modelo.setPorcentajePresupuesto(null);
			modelo.setAlerta(false);
			modelo.setBloqueaIngesta(false);
			return;
		}
		BigDecimal limite = politica.getPresupuestoMensual();
		BigDecimal porcentaje = gastado.divide(limite, 4, RoundingMode.HALF_UP);
		modelo.setPorcentajePresupuesto(porcentaje);
		modelo.setAlerta(porcentaje.compareTo(politica.getUmbralAlerta()) >= 0);
		modelo.setBloqueaIngesta(bloquea(politica, tenantId, referencia));
	}

	private boolean bloquea(PoliticaCostoTenant politica, String tenantId, Instant referencia) {
		if (politica == null || !politica.isActivo() || politica.getPresupuestoMensual() == null) {
			return false;
		}
		if (politica.getAccionAlExceder() != AccionPresupuestoCosto.BLOQUEAR_INGESTA) {
			return false;
		}
		BigDecimal gastado = decimal(
				primera(ejecucionExtraccionRepository.totalesEntre(tenantId, inicioMesUtc(referencia), referencia)), 0);
		return gastado.compareTo(politica.getPresupuestoMensual()) >= 0;
	}

	private List<CostoAgrupadoModel> porProveedor(String tenantId, Instant desde, Instant hasta) {
		List<CostoAgrupadoModel> filas = new ArrayList<>();
		for (Object[] fila : ejecucionExtraccionRepository.totalesPorProveedor(tenantId, desde, hasta)) {
			CostoAgrupadoModel modelo = new CostoAgrupadoModel();
			modelo.setClave(fila[0] instanceof ProveedorDocumentalIa proveedor ? proveedor.name() : String.valueOf(fila[0]));
			modelo.setCosto(decimal(fila, 1));
			modelo.setExtracciones(entero(fila, 2));
			filas.add(modelo);
		}
		return filas;
	}

	private List<CostoAgrupadoModel> porPlantilla(String tenantId, Instant desde, Instant hasta) {
		List<CostoAgrupadoModel> filas = new ArrayList<>();
		for (Object[] fila : ejecucionExtraccionRepository.totalesPorPlantilla(tenantId, desde, hasta,
				ESTADOS_CORRECTOS)) {
			CostoAgrupadoModel modelo = new CostoAgrupadoModel();
			modelo.setClave(fila[0] == null ? "SIN_PLANTILLA" : String.valueOf(fila[0]));
			modelo.setCosto(decimal(fila, 1));
			modelo.setExtracciones(entero(fila, 2));
			modelo.setDocumentosCorrectos(entero(fila, 3));
			filas.add(modelo);
		}
		return filas;
	}

	private PoliticaCostoTenant nueva(Tenant tenant) {
		PoliticaCostoTenant politica = new PoliticaCostoTenant();
		politica.setTenant(tenant);
		politica.setUmbralAlerta(UMBRAL_POR_DEFECTO);
		politica.setAccionAlExceder(AccionPresupuestoCosto.ALERTA);
		politica.setMoneda("USD");
		politica.setAlta(Instant.now());
		return politica;
	}

	private PoliticaCostoModel aModelo(PoliticaCostoTenant politica) {
		PoliticaCostoModel modelo = new PoliticaCostoModel();
		if (politica == null) {
			modelo.setActivo(false);
			modelo.setUmbralAlerta(UMBRAL_POR_DEFECTO);
			modelo.setAccionAlExceder(AccionPresupuestoCosto.ALERTA);
			modelo.setMoneda("USD");
			return modelo;
		}
		modelo.setId(politica.getId());
		modelo.setActivo(politica.isActivo());
		modelo.setPresupuestoMensual(politica.getPresupuestoMensual());
		modelo.setUmbralAlerta(politica.getUmbralAlerta());
		modelo.setAccionAlExceder(politica.getAccionAlExceder());
		modelo.setMoneda(politica.getMoneda());
		return modelo;
	}

	private Tenant exigirTenant(String tenantId) {
		return tenantRepository.findById(tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de("Tenant", tenantId));
	}

	private BigDecimal presupuestoDe(BigDecimal presupuesto) {
		if (presupuesto == null) {
			return null;
		}
		if (presupuesto.compareTo(BigDecimal.ZERO) < 0) {
			throw new ValidacionException("El presupuesto mensual no puede ser negativo");
		}
		return presupuesto.setScale(6, RoundingMode.HALF_UP);
	}

	private BigDecimal umbralDe(BigDecimal umbral) {
		if (umbral == null) {
			return UMBRAL_POR_DEFECTO;
		}
		if (umbral.compareTo(BigDecimal.ZERO) < 0 || umbral.compareTo(BigDecimal.ONE) > 0) {
			throw new ValidacionException("El umbral de alerta del presupuesto debe estar entre 0 y 1");
		}
		return umbral.setScale(4, RoundingMode.HALF_UP);
	}

	private String monedaDe(String moneda) {
		if (moneda == null || moneda.isBlank()) {
			return "USD";
		}
		String normalizada = moneda.trim().toUpperCase();
		if (normalizada.length() > 8) {
			throw new ValidacionException("La moneda del presupuesto no puede superar 8 caracteres");
		}
		return normalizada;
	}

	private BigDecimal efectivo(BigDecimal costo, long documentosCorrectos) {
		if (documentosCorrectos <= 0) {
			return null;
		}
		return costo.divide(BigDecimal.valueOf(documentosCorrectos), 6, RoundingMode.HALF_UP);
	}

	private Instant inicioMesUtc(Instant referencia) {
		LocalDate dia = referencia.atZone(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1);
		return dia.atStartOfDay().toInstant(ZoneOffset.UTC);
	}

	private Object[] primera(List<Object[]> filas) {
		if (filas == null || filas.isEmpty() || filas.get(0) == null) {
			return new Object[] { BigDecimal.ZERO, 0L, 0L, 0L, 0L, 0L, 0L };
		}
		return filas.get(0);
	}

	private BigDecimal decimal(Object[] fila, int indice) {
		if (fila == null || indice >= fila.length || fila[indice] == null) {
			return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
		}
		if (fila[indice] instanceof BigDecimal valor) {
			return valor.setScale(6, RoundingMode.HALF_UP);
		}
		return new BigDecimal(fila[indice].toString()).setScale(6, RoundingMode.HALF_UP);
	}

	private long entero(Object[] fila, int indice) {
		if (fila == null || indice >= fila.length || fila[indice] == null) {
			return 0;
		}
		return ((Number) fila[indice]).longValue();
	}
}
