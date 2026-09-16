package com.nextdocs.ai.utiles;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.nextdocs.ai.enumeraciones.EstadoDocumento;
import com.nextdocs.ai.exceptions.TransicionInvalidaException;

public final class MaquinaEstadoDocumento {

	private static final Map<EstadoDocumento, Set<EstadoDocumento>> TRANSICIONES = construirTransiciones();

	private MaquinaEstadoDocumento() {
	}

	private static Map<EstadoDocumento, Set<EstadoDocumento>> construirTransiciones() {
		Map<EstadoDocumento, Set<EstadoDocumento>> mapa = new EnumMap<>(EstadoDocumento.class);
		mapa.put(EstadoDocumento.RECIBIDO,
				EnumSet.of(EstadoDocumento.PROCESANDO, EstadoDocumento.DIVIDIDO, EstadoDocumento.RECHAZADO));
		mapa.put(EstadoDocumento.PROCESANDO,
				EnumSet.of(EstadoDocumento.PROCESANDO, EstadoDocumento.EXTRAIDO, EstadoDocumento.DIVIDIDO,
						EstadoDocumento.OBSERVADO, EstadoDocumento.RECHAZADO));
		mapa.put(EstadoDocumento.EXTRAIDO,
				EnumSet.of(EstadoDocumento.VALIDADO, EstadoDocumento.OBSERVADO, EstadoDocumento.RECHAZADO));
		mapa.put(EstadoDocumento.VALIDADO, EnumSet.of(EstadoDocumento.APROBADO, EstadoDocumento.OBSERVADO,
				EstadoDocumento.RECHAZADO, EstadoDocumento.PROCESANDO));
		mapa.put(EstadoDocumento.OBSERVADO, EnumSet.of(EstadoDocumento.APROBADO, EstadoDocumento.RECHAZADO,
				EstadoDocumento.PROCESANDO, EstadoDocumento.VALIDADO));
		mapa.put(EstadoDocumento.APROBADO, EnumSet.of(EstadoDocumento.CERRADO, EstadoDocumento.OBSERVADO));
		mapa.put(EstadoDocumento.RECHAZADO, EnumSet.of(EstadoDocumento.PROCESANDO, EstadoDocumento.CERRADO));
		mapa.put(EstadoDocumento.DIVIDIDO, EnumSet.of(EstadoDocumento.CERRADO));
		mapa.put(EstadoDocumento.CERRADO, EnumSet.noneOf(EstadoDocumento.class));
		return mapa;
	}

	public static boolean permitida(EstadoDocumento origen, EstadoDocumento destino) {
		if (origen == null || destino == null) {
			return false;
		}
		return TRANSICIONES.getOrDefault(origen, EnumSet.noneOf(EstadoDocumento.class)).contains(destino);
	}

	public static void validar(EstadoDocumento origen, EstadoDocumento destino) {
		if (!permitida(origen, destino)) {
			throw new TransicionInvalidaException(origen, destino);
		}
	}

	public static Set<EstadoDocumento> siguientes(EstadoDocumento origen) {
		return EnumSet.copyOf(TRANSICIONES.getOrDefault(origen, EnumSet.noneOf(EstadoDocumento.class)));
	}

	public static boolean esFinal(EstadoDocumento estado) {
		return EstadoDocumento.CERRADO == estado;
	}
}
