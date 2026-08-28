package com.nextdocs.ai.servicios;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.nextdocs.ai.entidades.ConfiguracionConector;
import com.nextdocs.ai.utiles.CircuitoConector;

import org.springframework.stereotype.Service;

@Service
public class RegistroCircuitosService {

	private final Map<String, CircuitoConector> circuitos = new ConcurrentHashMap<>();

	public CircuitoConector obtener(ConfiguracionConector configuracion) {
		String clave = clave(configuracion.getTenant().getId(), configuracion.getCodigo());
		return circuitos.computeIfAbsent(clave, ignorado -> new CircuitoConector(
				configuracion.getUmbralCircuitoAbierto(), configuracion.getDuracionCircuitoAbiertoSegundos()));
	}

	public CircuitoConector obtener(String tenantId, String codigo, int umbralFallos, long duracionSegundos) {
		return circuitos.computeIfAbsent(clave(tenantId, codigo),
				ignorado -> new CircuitoConector(umbralFallos, duracionSegundos));
	}

	public void reiniciar(String tenantId, String codigo) {
		circuitos.remove(clave(tenantId, codigo));
	}

	private String clave(String tenantId, String codigo) {
		return tenantId + "|" + codigo;
	}
}
