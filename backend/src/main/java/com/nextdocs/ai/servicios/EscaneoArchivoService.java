package com.nextdocs.ai.servicios;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.nextdocs.ai.config.PropiedadesAntivirus;
import com.nextdocs.ai.enumeraciones.MotorAntivirus;
import com.nextdocs.ai.exceptions.ArchivoRechazadoException;
import com.nextdocs.ai.interfaces.AntivirusInt;
import com.nextdocs.ai.modelos.ResultadoEscaneoModel;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EscaneoArchivoService {

	public static final String CODIGO_ANTIVIRUS_NO_DISPONIBLE = "ANTIVIRUS_NO_DISPONIBLE";

	private static final Logger log = LoggerFactory.getLogger(EscaneoArchivoService.class);

	private final Map<MotorAntivirus, AntivirusInt> motores = new EnumMap<>(MotorAntivirus.class);

	private final PropiedadesAntivirus propiedades;

	public EscaneoArchivoService(List<AntivirusInt> disponibles, PropiedadesAntivirus propiedades) {
		for (AntivirusInt antivirus : disponibles) {
			motores.put(antivirus.motor(), antivirus);
		}
		this.propiedades = propiedades;
	}

	@PostConstruct
	public void avisarConfiguracion() {
		if (propiedades.getMotor() == MotorAntivirus.PERMISIVO) {
			log.warn("El escaneo antivirus esta DESACTIVADO. No usar esta configuracion en produccion");
			return;
		}
		log.info("Escaneo antivirus activo con motor {} y rechazo si no esta disponible en {}",
				propiedades.getMotor(), propiedades.isRechazarSiNoDisponible());
	}

	public ResultadoEscaneoModel escanear(byte[] contenido, String nombreArchivo) {
		AntivirusInt antivirus = motores.get(propiedades.getMotor());
		if (antivirus == null) {
			return manejarNoDisponible("No hay implementacion para el motor " + propiedades.getMotor());
		}
		ResultadoEscaneoModel resultado = antivirus.escanear(contenido, nombreArchivo);
		if (resultado.getResultado() == com.nextdocs.ai.enumeraciones.ResultadoEscaneo.ERROR) {
			return manejarNoDisponible(resultado.getDetalle());
		}
		return resultado;
	}

	public boolean estaActivo() {
		return propiedades.getMotor() != MotorAntivirus.PERMISIVO;
	}

	private ResultadoEscaneoModel manejarNoDisponible(String detalle) {
		if (propiedades.isRechazarSiNoDisponible()) {
			throw new ArchivoRechazadoException(CODIGO_ANTIVIRUS_NO_DISPONIBLE,
					"No se pudo analizar el archivo y la politica exige rechazarlo: " + detalle);
		}
		log.warn("El antivirus no esta disponible y la politica permite continuar: {}", detalle);
		return ResultadoEscaneoModel.error(propiedades.getMotor(), detalle, 0);
	}
}
