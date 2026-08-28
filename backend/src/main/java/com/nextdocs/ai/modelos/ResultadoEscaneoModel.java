package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;

import com.nextdocs.ai.enumeraciones.MotorAntivirus;
import com.nextdocs.ai.enumeraciones.ResultadoEscaneo;

import lombok.Data;

@Data
public class ResultadoEscaneoModel implements Serializable {

	private static final long serialVersionUID = 6693512490718840243L;

	private ResultadoEscaneo resultado;

	private MotorAntivirus motor;

	private String amenaza;

	private String detalle;

	private long duracionMilisegundos;

	private Instant escaneado;

	public boolean esLimpio() {
		return ResultadoEscaneo.LIMPIO == resultado;
	}

	public boolean estaInfectado() {
		return ResultadoEscaneo.INFECTADO == resultado;
	}

	public static ResultadoEscaneoModel limpio(MotorAntivirus motor, long duracion) {
		ResultadoEscaneoModel modelo = new ResultadoEscaneoModel();
		modelo.setResultado(ResultadoEscaneo.LIMPIO);
		modelo.setMotor(motor);
		modelo.setDuracionMilisegundos(duracion);
		modelo.setEscaneado(Instant.now());
		return modelo;
	}

	public static ResultadoEscaneoModel infectado(MotorAntivirus motor, String amenaza, long duracion) {
		ResultadoEscaneoModel modelo = new ResultadoEscaneoModel();
		modelo.setResultado(ResultadoEscaneo.INFECTADO);
		modelo.setMotor(motor);
		modelo.setAmenaza(amenaza);
		modelo.setDuracionMilisegundos(duracion);
		modelo.setEscaneado(Instant.now());
		return modelo;
	}

	public static ResultadoEscaneoModel error(MotorAntivirus motor, String detalle, long duracion) {
		ResultadoEscaneoModel modelo = new ResultadoEscaneoModel();
		modelo.setResultado(ResultadoEscaneo.ERROR);
		modelo.setMotor(motor);
		modelo.setDetalle(detalle);
		modelo.setDuracionMilisegundos(duracion);
		modelo.setEscaneado(Instant.now());
		return modelo;
	}

	public static ResultadoEscaneoModel noAnalizado(MotorAntivirus motor) {
		ResultadoEscaneoModel modelo = new ResultadoEscaneoModel();
		modelo.setResultado(ResultadoEscaneo.NO_ANALIZADO);
		modelo.setMotor(motor);
		modelo.setEscaneado(Instant.now());
		return modelo;
	}
}
