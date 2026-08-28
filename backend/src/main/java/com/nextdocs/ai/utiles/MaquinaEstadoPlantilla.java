package com.nextdocs.ai.utiles;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.nextdocs.ai.enumeraciones.EstadoPlantilla;
import com.nextdocs.ai.exceptions.ValidacionException;

public final class MaquinaEstadoPlantilla {

	private static final Map<EstadoPlantilla, Set<EstadoPlantilla>> TRANSICIONES = construirTransiciones();

	private MaquinaEstadoPlantilla() {
	}

	private static Map<EstadoPlantilla, Set<EstadoPlantilla>> construirTransiciones() {
		Map<EstadoPlantilla, Set<EstadoPlantilla>> mapa = new EnumMap<>(EstadoPlantilla.class);
		mapa.put(EstadoPlantilla.BORRADOR, EnumSet.of(EstadoPlantilla.EN_PRUEBA, EstadoPlantilla.PUBLICADA));
		mapa.put(EstadoPlantilla.EN_PRUEBA, EnumSet.of(EstadoPlantilla.BORRADOR, EstadoPlantilla.PUBLICADA));
		mapa.put(EstadoPlantilla.PUBLICADA, EnumSet.of(EstadoPlantilla.DEPRECADA));
		mapa.put(EstadoPlantilla.DEPRECADA, EnumSet.of(EstadoPlantilla.PUBLICADA));
		return mapa;
	}

	public static boolean permitida(EstadoPlantilla origen, EstadoPlantilla destino) {
		if (origen == null || destino == null) {
			return false;
		}
		return TRANSICIONES.getOrDefault(origen, EnumSet.noneOf(EstadoPlantilla.class)).contains(destino);
	}

	public static void validar(EstadoPlantilla origen, EstadoPlantilla destino) {
		if (!permitida(origen, destino)) {
			throw new ValidacionException(
					"No se puede pasar una version de plantilla de " + origen + " a " + destino);
		}
	}

	public static boolean esEditable(EstadoPlantilla estado) {
		return EstadoPlantilla.BORRADOR == estado || EstadoPlantilla.EN_PRUEBA == estado;
	}

	public static void exigirEditable(EstadoPlantilla estado) {
		if (!esEditable(estado)) {
			throw new ValidacionException(
					"Una version " + estado + " es inmutable. Cree una version nueva para modificarla");
		}
	}

	public static Set<EstadoPlantilla> siguientes(EstadoPlantilla origen) {
		return EnumSet.copyOf(TRANSICIONES.getOrDefault(origen, EnumSet.noneOf(EstadoPlantilla.class)));
	}
}
