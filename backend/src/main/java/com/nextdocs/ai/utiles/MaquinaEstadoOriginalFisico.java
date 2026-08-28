package com.nextdocs.ai.utiles;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.nextdocs.ai.enumeraciones.EstadoOriginalFisico;
import com.nextdocs.ai.exceptions.ValidacionException;

public final class MaquinaEstadoOriginalFisico {

	private static final Map<EstadoOriginalFisico, Set<EstadoOriginalFisico>> TRANSICIONES = construir();

	private MaquinaEstadoOriginalFisico() {
	}

	private static Map<EstadoOriginalFisico, Set<EstadoOriginalFisico>> construir() {
		Map<EstadoOriginalFisico, Set<EstadoOriginalFisico>> mapa = new EnumMap<>(EstadoOriginalFisico.class);
		mapa.put(EstadoOriginalFisico.NO_REQUERIDO, EnumSet.of(EstadoOriginalFisico.PENDIENTE));
		mapa.put(EstadoOriginalFisico.PENDIENTE,
				EnumSet.of(EstadoOriginalFisico.RECIBIDO, EstadoOriginalFisico.EXTRAVIADO));
		mapa.put(EstadoOriginalFisico.RECIBIDO,
				EnumSet.of(EstadoOriginalFisico.ARCHIVADO, EstadoOriginalFisico.EXTRAVIADO));
		mapa.put(EstadoOriginalFisico.ARCHIVADO, EnumSet.of(EstadoOriginalFisico.EXTRAVIADO));
		mapa.put(EstadoOriginalFisico.EXTRAVIADO, EnumSet.of(EstadoOriginalFisico.RECIBIDO));
		return mapa;
	}

	public static boolean permitida(EstadoOriginalFisico origen, EstadoOriginalFisico destino) {
		if (origen == null || destino == null) {
			return false;
		}
		return TRANSICIONES.getOrDefault(origen, EnumSet.noneOf(EstadoOriginalFisico.class)).contains(destino);
	}

	public static void validar(EstadoOriginalFisico origen, EstadoOriginalFisico destino) {
		if (!permitida(origen, destino)) {
			throw new ValidacionException(
					"No se puede pasar el original fisico de " + origen + " a " + destino);
		}
	}

	public static boolean estaEnPoder(EstadoOriginalFisico estado) {
		return EstadoOriginalFisico.RECIBIDO == estado || EstadoOriginalFisico.ARCHIVADO == estado;
	}

	public static Set<EstadoOriginalFisico> siguientes(EstadoOriginalFisico origen) {
		return EnumSet.copyOf(TRANSICIONES.getOrDefault(origen, EnumSet.noneOf(EstadoOriginalFisico.class)));
	}
}
