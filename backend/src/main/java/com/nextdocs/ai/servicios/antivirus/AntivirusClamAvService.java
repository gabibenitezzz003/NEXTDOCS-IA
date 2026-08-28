package com.nextdocs.ai.servicios.antivirus;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import com.nextdocs.ai.config.PropiedadesAntivirus;
import com.nextdocs.ai.enumeraciones.MotorAntivirus;
import com.nextdocs.ai.interfaces.AntivirusInt;
import com.nextdocs.ai.modelos.ResultadoEscaneoModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AntivirusClamAvService implements AntivirusInt {

	public static final String COMANDO_FLUJO = "zINSTREAM\0";

	public static final String COMANDO_PING = "zPING\0";

	private static final Logger log = LoggerFactory.getLogger(AntivirusClamAvService.class);

	private static final String SUFIJO_INFECTADO = "FOUND";

	private static final String SUFIJO_LIMPIO = "OK";

	private final PropiedadesAntivirus propiedades;

	public AntivirusClamAvService(PropiedadesAntivirus propiedades) {
		this.propiedades = propiedades;
	}

	@Override
	public MotorAntivirus motor() {
		return MotorAntivirus.CLAMAV;
	}

	@Override
	public boolean estaDisponible() {
		try (Socket socket = abrir()) {
			socket.getOutputStream().write(COMANDO_PING.getBytes(StandardCharsets.US_ASCII));
			socket.getOutputStream().flush();
			return leerRespuesta(socket.getInputStream()).contains("PONG");
		} catch (Exception e) {
			log.warn("ClamAV no responde en {}:{} ({})", propiedades.getHost(), propiedades.getPuerto(),
					e.getMessage());
			return false;
		}
	}

	@Override
	public ResultadoEscaneoModel escanear(byte[] contenido, String nombreArchivo) {
		long inicio = System.currentTimeMillis();
		try (Socket socket = abrir()) {
			enviarFlujo(socket.getOutputStream(), contenido);
			String respuesta = leerRespuesta(socket.getInputStream());
			return interpretar(respuesta, System.currentTimeMillis() - inicio);
		} catch (Exception e) {
			log.error("Fallo el escaneo antivirus de {}", nombreArchivo, e);
			return ResultadoEscaneoModel.error(MotorAntivirus.CLAMAV, e.getMessage(),
					System.currentTimeMillis() - inicio);
		}
	}

	private ResultadoEscaneoModel interpretar(String respuesta, long duracion) {
		String limpia = respuesta.replace("\0", "").trim();
		if (limpia.endsWith(SUFIJO_INFECTADO)) {
			int desde = limpia.indexOf(':');
			String amenaza = desde >= 0
					? limpia.substring(desde + 1, limpia.length() - SUFIJO_INFECTADO.length()).trim()
					: limpia;
			return ResultadoEscaneoModel.infectado(MotorAntivirus.CLAMAV, amenaza, duracion);
		}
		if (limpia.endsWith(SUFIJO_LIMPIO)) {
			return ResultadoEscaneoModel.limpio(MotorAntivirus.CLAMAV, duracion);
		}
		return ResultadoEscaneoModel.error(MotorAntivirus.CLAMAV, "Respuesta inesperada: " + limpia, duracion);
	}

	private void enviarFlujo(OutputStream salida, byte[] contenido) throws Exception {
		DataOutputStream flujo = new DataOutputStream(salida);
		flujo.write(COMANDO_FLUJO.getBytes(StandardCharsets.US_ASCII));
		flujo.flush();
		int bloque = Math.max(propiedades.getTamanoBloque(), 1024);
		for (int posicion = 0; posicion < contenido.length; posicion += bloque) {
			int largo = Math.min(bloque, contenido.length - posicion);
			flujo.writeInt(largo);
			flujo.write(contenido, posicion, largo);
		}
		flujo.writeInt(0);
		flujo.flush();
	}

	private String leerRespuesta(InputStream entrada) throws Exception {
		byte[] buffer = new byte[512];
		int leidos = entrada.read(buffer);
		return leidos <= 0 ? "" : new String(buffer, 0, leidos, StandardCharsets.US_ASCII);
	}

	private Socket abrir() throws Exception {
		Socket socket = new Socket();
		socket.connect(new InetSocketAddress(propiedades.getHost(), propiedades.getPuerto()),
				propiedades.getTiempoEsperaMilisegundos());
		socket.setSoTimeout(propiedades.getTiempoEsperaMilisegundos());
		return socket;
	}
}
