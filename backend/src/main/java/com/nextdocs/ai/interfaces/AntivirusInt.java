package com.nextdocs.ai.interfaces;

import com.nextdocs.ai.enumeraciones.MotorAntivirus;
import com.nextdocs.ai.modelos.ResultadoEscaneoModel;

public interface AntivirusInt {

	MotorAntivirus motor();

	boolean estaDisponible();

	ResultadoEscaneoModel escanear(byte[] contenido, String nombreArchivo);
}
