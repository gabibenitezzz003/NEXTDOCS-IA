package com.nextdocs.ai.servicios;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import com.nextdocs.ai.config.PropiedadesCola;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ColaExtraccionService {

	private final StringRedisTemplate redis;

	private final PropiedadesCola propiedades;

	private final AtomicLong profundidadMedida = new AtomicLong();

	private final AtomicLong profundidadReintentoMedida = new AtomicLong();

	public ColaExtraccionService(StringRedisTemplate redis, PropiedadesCola propiedades,
			MeterRegistry meterRegistry) {
		this.redis = redis;
		this.propiedades = propiedades;
		meterRegistry.gauge("nextdocs.cola.extraccion.profundidad", profundidadMedida,
				AtomicLong::doubleValue);
		meterRegistry.gauge("nextdocs.cola.extraccion.reintentos", profundidadReintentoMedida,
				AtomicLong::doubleValue);
	}

	public void encolar(String documentoId) {
		redis.opsForList().leftPush(propiedades.getNombreExtraccion(), documentoId);
	}

	public void encolarReintento(String documentoId, long esperaMilisegundos) {
		redis.opsForZSet().add(propiedades.getNombreReintento(), documentoId,
				System.currentTimeMillis() + esperaMilisegundos);
	}

	public Optional<String> desencolar() {
		return Optional.ofNullable(redis.opsForList().rightPop(propiedades.getNombreExtraccion(),
				Duration.ofMillis(propiedades.getIntervaloSondeoMilisegundos())));
	}

	public Optional<String> desencolarReintentoVencido() {
		long ahora = System.currentTimeMillis();
		return redis.opsForZSet().rangeByScore(propiedades.getNombreReintento(), 0, ahora, 0, 1).stream().findFirst()
				.filter(documentoId -> Boolean.TRUE
						.equals(redis.opsForZSet().remove(propiedades.getNombreReintento(), documentoId) > 0));
	}

	public long profundidad() {
		Long tamano = redis.opsForList().size(propiedades.getNombreExtraccion());
		long valor = tamano == null ? 0 : tamano;
		profundidadMedida.set(valor);
		return valor;
	}

	public long profundidadReintento() {
		Long tamano = redis.opsForZSet().size(propiedades.getNombreReintento());
		long valor = tamano == null ? 0 : tamano;
		profundidadReintentoMedida.set(valor);
		return valor;
	}
}
