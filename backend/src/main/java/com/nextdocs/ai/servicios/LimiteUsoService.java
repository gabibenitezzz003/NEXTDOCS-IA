package com.nextdocs.ai.servicios;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.nextdocs.ai.config.PropiedadesLimiteUso;
import com.nextdocs.ai.modelos.ResultadoLimiteModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class LimiteUsoService {

	public static final String ALCANCE_PRINCIPAL = "principal";

	public static final String ALCANCE_TENANT = "tenant";

	public static final String ALCANCE_INGESTA = "ingesta";

	private static final String PREFIJO = "nextdocs:limite:";

	private static final Duration VENTANA = Duration.ofMinutes(1);

	private static final Logger log = LoggerFactory.getLogger(LimiteUsoService.class);

	private final StringRedisTemplate redis;

	private final PropiedadesLimiteUso propiedades;

	public LimiteUsoService(StringRedisTemplate redis, PropiedadesLimiteUso propiedades) {
		this.redis = redis;
		this.propiedades = propiedades;
	}

	public boolean estaActivo() {
		return propiedades.isActivo();
	}

	public boolean esRutaExenta(String ruta) {
		return coincide(ruta, propiedades.getRutasExentas());
	}

	public boolean esRutaDeIngesta(String ruta) {
		return coincide(ruta, propiedades.getRutasIngesta());
	}

	public ResultadoLimiteModel consumir(String alcance, String identificador, int limite) {
		if (limite <= 0) {
			return ResultadoLimiteModel.permitido(limite, limite, VENTANA.getSeconds());
		}
		long ventana = Instant.now().getEpochSecond() / VENTANA.getSeconds();
		String clave = PREFIJO + alcance + ":" + identificador + ":" + ventana;
		try {
			Long consumidas = redis.opsForValue().increment(clave);
			if (consumidas == null) {
				return ResultadoLimiteModel.permitido(limite, limite, VENTANA.getSeconds());
			}
			if (consumidas == 1L) {
				redis.expire(clave, VENTANA);
			}
			long restantes = Math.max(limite - consumidas, 0);
			long esperaSegundos = segundosHastaLaProximaVentana();
			if (consumidas > limite) {
				return ResultadoLimiteModel.rechazado(alcance, limite, esperaSegundos);
			}
			return ResultadoLimiteModel.permitido(limite, restantes, esperaSegundos);
		} catch (Exception e) {
			log.error("No se pudo consultar el limite de uso, se deja pasar la peticion", e);
			return ResultadoLimiteModel.permitido(limite, limite, VENTANA.getSeconds());
		}
	}

	public int limitePorPrincipal() {
		return propiedades.getPeticionesPorMinutoPorPrincipal();
	}

	public int limitePorTenant() {
		return propiedades.getPeticionesPorMinutoPorTenant();
	}

	public int limiteDeIngesta() {
		return propiedades.getIngestasPorMinutoPorTenant();
	}

	private long segundosHastaLaProximaVentana() {
		long segundos = VENTANA.getSeconds();
		return segundos - (Instant.now().getEpochSecond() % segundos);
	}

	private boolean coincide(String ruta, List<String> patrones) {
		if (ruta == null) {
			return false;
		}
		for (String patron : patrones) {
			if (patron.endsWith("**") && ruta.startsWith(patron.substring(0, patron.length() - 2))) {
				return true;
			}
			if (patron.equals(ruta)) {
				return true;
			}
		}
		return false;
	}
}
