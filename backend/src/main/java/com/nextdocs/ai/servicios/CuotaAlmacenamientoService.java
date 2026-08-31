package com.nextdocs.ai.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.nextdocs.ai.entidades.AvisoAlmacenamiento;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.enumeraciones.TipoEventoCanonico;
import com.nextdocs.ai.modelos.UsoAlmacenamientoModel;
import com.nextdocs.ai.repositorios.ArchivoDocumentoRepository;
import com.nextdocs.ai.repositorios.AvisoAlmacenamientoRepository;
import com.nextdocs.ai.repositorios.LoteExportacionRepository;
import com.nextdocs.ai.repositorios.TenantRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CuotaAlmacenamientoService {

	private static final Logger log = LoggerFactory.getLogger(CuotaAlmacenamientoService.class);

	public static final List<Integer> UMBRALES = List.of(70, 85, 95, 100);

	private static final BigDecimal CIEN = new BigDecimal("100");

	private static final long KILO = 1024L;

	private final ArchivoDocumentoRepository archivoDocumentoRepository;

	private final LoteExportacionRepository loteExportacionRepository;

	private final AvisoAlmacenamientoRepository avisoAlmacenamientoRepository;

	private final TenantRepository tenantRepository;

	private final EventoSalidaService eventoSalidaService;

	public CuotaAlmacenamientoService(ArchivoDocumentoRepository archivoDocumentoRepository,
			LoteExportacionRepository loteExportacionRepository,
			AvisoAlmacenamientoRepository avisoAlmacenamientoRepository, TenantRepository tenantRepository,
			EventoSalidaService eventoSalidaService) {
		this.archivoDocumentoRepository = archivoDocumentoRepository;
		this.loteExportacionRepository = loteExportacionRepository;
		this.avisoAlmacenamientoRepository = avisoAlmacenamientoRepository;
		this.tenantRepository = tenantRepository;
		this.eventoSalidaService = eventoSalidaService;
	}

	@Transactional(readOnly = true)
	public UsoAlmacenamientoModel medir(String tenantId) {
		long originales = archivoDocumentoRepository.sumarTamanoPorTenant(tenantId);
		long exportaciones = loteExportacionRepository
				.listarPorTenant(tenantId, null, PageRequest.of(0, Integer.MAX_VALUE)).stream()
				.mapToLong(lote -> lote.getTamanoBytes()).sum();
		long cuota = tenantRepository.findById(tenantId).map(Tenant::getCuotaAlmacenamientoBytes).orElse(0L);

		UsoAlmacenamientoModel modelo = new UsoAlmacenamientoModel();
		modelo.setBytesUsados(originales + exportaciones);
		modelo.setCuotaBytes(cuota);
		modelo.setPresentacion(presentar(originales + exportaciones));

		Map<String, Long> porClase = new LinkedHashMap<>();
		porClase.put("originalesPersistentes", originales);
		porClase.put("exportacionesTemporales", exportaciones);
		modelo.setPorClase(porClase);

		if (cuota > 0) {
			BigDecimal porcentaje = BigDecimal.valueOf(modelo.getBytesUsados()).multiply(CIEN)
					.divide(BigDecimal.valueOf(cuota), 1, RoundingMode.HALF_UP);
			modelo.setPorcentaje(porcentaje);
			modelo.setUmbralAlcanzado(umbralDe(porcentaje));
		}
		return modelo;
	}

	@Scheduled(fixedDelayString = "${nextdocs.almacenamiento.intervaloAvisoMilisegundos:3600000}",
			initialDelayString = "${nextdocs.almacenamiento.retrasoInicialMilisegundos:45000}")
	public void revisarTodosLosTenants() {
		for (Tenant tenant : tenantRepository.findAll()) {
			if (tenant.getBaja() == null && tenant.getCuotaAlmacenamientoBytes() > 0) {
				revisar(tenant.getId());
			}
		}
	}

	@Transactional
	public Integer revisar(String tenantId) {
		UsoAlmacenamientoModel uso = medir(tenantId);
		Integer umbral = uso.getUmbralAlcanzado();
		List<AvisoAlmacenamiento> avisos = avisoAlmacenamientoRepository.listarPorTenant(tenantId);

		avisos.stream().filter(aviso -> umbral == null || aviso.getUmbral() > umbral)
				.forEach(avisoAlmacenamientoRepository::delete);

		if (umbral == null || avisos.stream().anyMatch(aviso -> aviso.getUmbral() == umbral)) {
			return umbral;
		}

		AvisoAlmacenamiento aviso = new AvisoAlmacenamiento();
		aviso.setTenant(tenantRepository.getReferenceById(tenantId));
		aviso.setUmbral(umbral);
		aviso.setBytesUsados(uso.getBytesUsados());
		aviso.setCuotaBytes(uso.getCuotaBytes());
		aviso.setAlta(Instant.now());
		avisoAlmacenamientoRepository.save(aviso);

		Map<String, Object> carga = new LinkedHashMap<>();
		carga.put("umbral", umbral);
		carga.put("bytesUsados", uso.getBytesUsados());
		carga.put("cuotaBytes", uso.getCuotaBytes());
		carga.put("porcentaje", uso.getPorcentaje());
		carga.put("aclaracion", "Al llegar al tope no se borra evidencia: se bloquea la ingesta nueva");
		eventoSalidaService.publicar(tenantId, TipoEventoCanonico.ALMACENAMIENTO_EN_UMBRAL, "Tenant", tenantId,
				carga);
		log.warn("El tenant {} alcanzo el {}% de su cuota de almacenamiento", tenantId, umbral);
		return umbral;
	}

	private Integer umbralDe(BigDecimal porcentaje) {
		Integer alcanzado = null;
		for (Integer umbral : UMBRALES) {
			if (porcentaje.compareTo(BigDecimal.valueOf(umbral)) >= 0) {
				alcanzado = umbral;
			}
		}
		return alcanzado;
	}

	public static String presentar(long bytes) {
		if (bytes < KILO) {
			return bytes + " B";
		}
		String[] unidades = { "KB", "MB", "GB", "TB" };
		double valor = bytes;
		int indice = -1;
		while (valor >= KILO && indice < unidades.length - 1) {
			valor /= KILO;
			indice++;
		}
		return String.format(java.util.Locale.ROOT, "%.1f %s", valor, unidades[indice]);
	}
}
