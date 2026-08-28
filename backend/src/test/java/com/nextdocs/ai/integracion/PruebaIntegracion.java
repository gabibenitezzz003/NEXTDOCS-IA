package com.nextdocs.ai.integracion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.Assumptions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("prueba")
public abstract class PruebaIntegracion {

	public static final String BASE_DATOS = "nextdocs_prueba";

	private static final String HOST = variable("NEXTDOCS_PRUEBA_BD_HOST", "localhost");

	private static final String PUERTO = variable("NEXTDOCS_PRUEBA_BD_PUERTO", "5434");

	private static final String USUARIO = variable("NEXTDOCS_PRUEBA_BD_USUARIO", "nextdocs");

	private static final String CLAVE = variable("NEXTDOCS_PRUEBA_BD_CLAVE", "nextdocs");

	private static final String REDIS_HOST = variable("NEXTDOCS_PRUEBA_REDIS_HOST", "localhost");

	private static final String REDIS_PUERTO = variable("NEXTDOCS_PRUEBA_REDIS_PUERTO", "6381");

	private static final String S3_ENDPOINT = variable("NEXTDOCS_PRUEBA_S3_ENDPOINT", "http://localhost:9102");

	static {
		prepararBaseDeDatos();
	}

	@DynamicPropertySource
	static void configurar(DynamicPropertyRegistry registro) {
		registro.add("spring.datasource.url", () -> urlDe(BASE_DATOS));
		registro.add("spring.datasource.username", () -> USUARIO);
		registro.add("spring.datasource.password", () -> CLAVE);
		registro.add("spring.data.redis.host", () -> REDIS_HOST);
		registro.add("spring.data.redis.port", () -> Integer.valueOf(REDIS_PUERTO));
		registro.add("nextdocs.almacenamiento.endpoint", () -> S3_ENDPOINT);
	}

	private static void prepararBaseDeDatos() {
		try (Connection conexion = DriverManager.getConnection(urlDe("postgres"), USUARIO, CLAVE);
				Statement sentencia = conexion.createStatement()) {
			try (ResultSet resultado = sentencia
					.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + BASE_DATOS + "'")) {
				if (!resultado.next()) {
					sentencia.executeUpdate("CREATE DATABASE " + BASE_DATOS);
				}
			}
		} catch (Exception e) {
			Assumptions.abort("No hay infraestructura de pruebas disponible en " + HOST + ":" + PUERTO
					+ ". Levantala con: docker compose up -d. Detalle: " + e.getMessage());
		}
	}

	private static String urlDe(String baseDatos) {
		return "jdbc:postgresql://" + HOST + ":" + PUERTO + "/" + baseDatos;
	}

	private static String variable(String nombre, String porDefecto) {
		String valor = System.getenv(nombre);
		if (valor == null || valor.isBlank()) {
			valor = System.getProperty(nombre);
		}
		return valor == null || valor.isBlank() ? porDefecto : valor;
	}
}
