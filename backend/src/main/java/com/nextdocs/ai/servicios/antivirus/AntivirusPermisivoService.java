package com.nextdocs.ai.servicios.antivirus;

import com.nextdocs.ai.enumeraciones.MotorAntivirus;
import com.nextdocs.ai.interfaces.AntivirusInt;
import com.nextdocs.ai.modelos.ResultadoEscaneoModel;

import org.springframework.stereotype.Service;

@Service
public class AntivirusPermisivoService implements AntivirusInt {

	@Override
	public MotorAntivirus motor() {
		return MotorAntivirus.PERMISIVO;
	}

	@Override
	public boolean estaDisponible() {
		return true;
	}

	@Override
	public ResultadoEscaneoModel escanear(byte[] contenido, String nombreArchivo) {
		return ResultadoEscaneoModel.noAnalizado(MotorAntivirus.PERMISIVO);
	}
}
