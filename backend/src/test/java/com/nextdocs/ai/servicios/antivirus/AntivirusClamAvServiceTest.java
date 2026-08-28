package com.nextdocs.ai.servicios.antivirus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.nextdocs.ai.config.PropiedadesAntivirus;
import com.nextdocs.ai.enumeraciones.MotorAntivirus;
import com.nextdocs.ai.enumeraciones.ResultadoEscaneo;
import com.nextdocs.ai.exceptions.ArchivoRechazadoException;
import com.nextdocs.ai.modelos.ResultadoEscaneoModel;
import com.nextdocs.ai.servicios.EscaneoArchivoService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AntivirusClamAvServiceTest {

	private static final String EICAR = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";

	private ServerSocket servidor;

	private AtomicReference<String> respuesta;

	private AtomicReference<byte[]> contenidoRecibido;

	private AtomicBoolean cortarConexion;

	private PropiedadesAntivirus propiedades;

	private AntivirusClamAvService antivirus;

	@BeforeEach
	void levantarClamdFalso() throws IOException {
		respuesta = new AtomicReference<>("stream: OK\0");
		contenidoRecibido = new AtomicReference<>(new byte[0]);
		cortarConexion = new AtomicBoolean(false);

		servidor = new ServerSocket(0);
		Thread hilo = new Thread(this::atender);
		hilo.setDaemon(true);
		hilo.start();

		propiedades = new PropiedadesAntivirus();
		propiedades.setMotor(MotorAntivirus.CLAMAV);
		propiedades.setHost("127.0.0.1");
		propiedades.setPuerto(servidor.getLocalPort());
		propiedades.setTiempoEsperaMilisegundos(3000);
		propiedades.setTamanoBloque(4096);
		propiedades.setRechazarSiNoDisponible(true);
		antivirus = new AntivirusClamAvService(propiedades);
	}

	@AfterEach
	void bajarServidor() throws IOException {
		servidor.close();
	}

	@Test
	@DisplayName("un archivo limpio devuelve LIMPIO y el contenido llega completo a clamd")
	void archivoLimpio() {
		byte[] contenido = "contenido inofensivo de un remito".getBytes(StandardCharsets.UTF_8);
		respuesta.set("stream: OK\0");

		ResultadoEscaneoModel resultado = antivirus.escanear(contenido, "remito.pdf");

		assertThat(resultado.getResultado()).isEqualTo(ResultadoEscaneo.LIMPIO);
		assertThat(resultado.getMotor()).isEqualTo(MotorAntivirus.CLAMAV);
		assertThat(resultado.getAmenaza()).isNull();
		assertThat(contenidoRecibido.get()).isEqualTo(contenido);
	}

	@Test
	@DisplayName("SEC-04: el archivo EICAR se detecta como INFECTADO con el nombre de la amenaza")
	void archivoInfectado() {
		respuesta.set("stream: Win.Test.EICAR_HDB-1 FOUND\0");

		ResultadoEscaneoModel resultado = antivirus.escanear(EICAR.getBytes(StandardCharsets.US_ASCII),
				"eicar.com");

		assertThat(resultado.getResultado()).isEqualTo(ResultadoEscaneo.INFECTADO);
		assertThat(resultado.estaInfectado()).isTrue();
		assertThat(resultado.getAmenaza()).isEqualTo("Win.Test.EICAR_HDB-1");
	}

	@Test
	@DisplayName("un archivo grande se envia en varios bloques y llega intacto")
	void archivoEnVariosBloques() {
		byte[] contenido = new byte[20000];
		for (int i = 0; i < contenido.length; i++) {
			contenido[i] = (byte) (i % 251);
		}
		respuesta.set("stream: OK\0");

		assertThat(antivirus.escanear(contenido, "grande.pdf").esLimpio()).isTrue();
		assertThat(contenidoRecibido.get()).isEqualTo(contenido);
	}

	@Test
	@DisplayName("una respuesta que no se entiende se reporta como ERROR, nunca como LIMPIO")
	void respuestaInesperada() {
		respuesta.set("cualquier cosa\0");

		ResultadoEscaneoModel resultado = antivirus.escanear("x".getBytes(StandardCharsets.UTF_8), "x.pdf");

		assertThat(resultado.getResultado()).isEqualTo(ResultadoEscaneo.ERROR);
		assertThat(resultado.esLimpio()).isFalse();
	}

	@Test
	@DisplayName("si clamd corta la conexion el resultado es ERROR, nunca LIMPIO")
	void conexionCortada() {
		cortarConexion.set(true);

		ResultadoEscaneoModel resultado = antivirus.escanear("x".getBytes(StandardCharsets.UTF_8), "x.pdf");

		assertThat(resultado.getResultado()).isEqualTo(ResultadoEscaneo.ERROR);
	}

	@Test
	@DisplayName("con rechazarSiNoDisponible en true un fallo del antivirus rechaza el archivo")
	void politicaFailClosed() {
		cortarConexion.set(true);
		EscaneoArchivoService servicio = new EscaneoArchivoService(List.of(antivirus), propiedades);

		assertThatThrownBy(() -> servicio.escanear("x".getBytes(StandardCharsets.UTF_8), "x.pdf"))
				.isInstanceOf(ArchivoRechazadoException.class)
				.hasMessageContaining("la politica exige rechazarlo");
	}

	@Test
	@DisplayName("con rechazarSiNoDisponible en false un fallo deja pasar el archivo marcado como ERROR")
	void politicaFailOpen() {
		cortarConexion.set(true);
		propiedades.setRechazarSiNoDisponible(false);
		EscaneoArchivoService servicio = new EscaneoArchivoService(List.of(antivirus), propiedades);

		ResultadoEscaneoModel resultado = servicio.escanear("x".getBytes(StandardCharsets.UTF_8), "x.pdf");

		assertThat(resultado.getResultado()).isEqualTo(ResultadoEscaneo.ERROR);
		assertThat(resultado.estaInfectado()).isFalse();
	}

	@Test
	@DisplayName("el motor permisivo marca NO_ANALIZADO y nunca miente diciendo LIMPIO")
	void motorPermisivo() {
		AntivirusPermisivoService permisivo = new AntivirusPermisivoService();

		ResultadoEscaneoModel resultado = permisivo.escanear("x".getBytes(StandardCharsets.UTF_8), "x.pdf");

		assertThat(resultado.getResultado()).isEqualTo(ResultadoEscaneo.NO_ANALIZADO);
		assertThat(resultado.esLimpio()).isFalse();
		assertThat(resultado.estaInfectado()).isFalse();
	}

	private void atender() {
		while (!servidor.isClosed()) {
			try (Socket cliente = servidor.accept()) {
				DataInputStream entrada = new DataInputStream(cliente.getInputStream());
				byte[] comando = new byte[10];
				entrada.readFully(comando);
				if (cortarConexion.get()) {
					continue;
				}
				java.io.ByteArrayOutputStream acumulado = new java.io.ByteArrayOutputStream();
				while (true) {
					int largo = entrada.readInt();
					if (largo == 0) {
						break;
					}
					byte[] bloque = new byte[largo];
					entrada.readFully(bloque);
					acumulado.write(bloque);
				}
				contenidoRecibido.set(acumulado.toByteArray());
				OutputStream salida = cliente.getOutputStream();
				salida.write(respuesta.get().getBytes(StandardCharsets.US_ASCII));
				salida.flush();
			} catch (Exception e) {
				if (servidor.isClosed()) {
					return;
				}
			}
		}
	}
}
