package com.nextdocs.ai.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.enumeraciones.MotorAntivirus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

public class ValidadorConfiguracionProduccion
		implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

	public static final String PERFIL_PRODUCCION = "produccion";

	public static final String JWT_POR_DEFECTO = "cambiar-este-secreto-en-produccion-minimo-32-bytes";

	public static final int LONGITUD_MINIMA_SECRETO = 32;

	private static final Logger log = LoggerFactory.getLogger(ValidadorConfiguracionProduccion.class);

	private static final List<String> CLAVES_CONOCIDAS = List.of("nextdocs", "nextdocs123", "postgres", "admin",
			"changeme", "secret");

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent evento) {
		ConfigurableEnvironment entorno = evento.getEnvironment();
		if (!List.of(entorno.getActiveProfiles()).contains(PERFIL_PRODUCCION)) {
			return;
		}
		List<String> problemas = new ArrayList<>();
		validarSecretoJwt(entorno, problemas);
		validarTenantDemostracion(entorno, problemas);
		validarClave(entorno, "nextdocs.almacenamiento.claveSecreta", "la clave secreta del object store",
				problemas);
		validarClave(entorno, "spring.datasource.password", "la clave de la base de datos", problemas);
		validarAntivirus(entorno, problemas);
		validarCredencialesEnClaro(entorno, problemas);
		validarFederacion(entorno, problemas);

		if (problemas.isEmpty()) {
			log.info("Configuracion de produccion validada");
			return;
		}
		StringBuilder mensaje = new StringBuilder(
				"El perfil produccion esta activo pero la configuracion no es apta. Corregi lo siguiente antes de exponer el servicio:");
		for (int indice = 0; indice < problemas.size(); indice++) {
			mensaje.append("\n  ").append(indice + 1).append(". ").append(problemas.get(indice));
		}
		mensaje.append("\n\nEl detalle de cada variable esta en docs/DESPLIEGUE.md");
		throw new ConfiguracionInseguraException(mensaje.toString());
	}

	private void validarFederacion(ConfigurableEnvironment entorno, List<String> problemas) {
		if (!entorno.getProperty("nextdocs.federacion.exigirEmisorSeguro", Boolean.class, true)) {
			problemas.add("NEXTDOCS_FEDERACION_EXIGIR_HTTPS=false permite registrar un proveedor de"
					+ " identidad por http: en produccion el JWKS tiene que viajar cifrado");
		}
	}

	private void validarSecretoJwt(ConfigurableEnvironment entorno, List<String> problemas) {
		String secreto = entorno.getProperty("nextdocs.seguridad.jwtSecreto");
		if (secreto == null || secreto.isBlank()) {
			problemas.add("NEXTDOCS_JWT_SECRETO no esta definido");
			return;
		}
		if (JWT_POR_DEFECTO.equals(secreto)) {
			problemas.add("NEXTDOCS_JWT_SECRETO sigue siendo el valor de ejemplo del application.yml");
			return;
		}
		int bytes = secreto.getBytes(StandardCharsets.UTF_8).length;
		if (bytes < LONGITUD_MINIMA_SECRETO) {
			problemas.add("NEXTDOCS_JWT_SECRETO tiene " + bytes + " bytes y el minimo para HS256 es "
					+ LONGITUD_MINIMA_SECRETO);
		}
	}

	private void validarTenantDemostracion(ConfigurableEnvironment entorno, List<String> problemas) {
		if (Boolean.parseBoolean(entorno.getProperty("nextdocs.arranque.crearTenantDemostracion", "false"))) {
			problemas.add("NEXTDOCS_CREAR_TENANT_DEMO esta en true y crearia un tenant con credenciales conocidas");
		}
	}

	private void validarClave(ConfigurableEnvironment entorno, String propiedad, String descripcion,
			List<String> problemas) {
		String valor = entorno.getProperty(propiedad);
		if (valor == null || valor.isBlank()) {
			problemas.add(descripcion + " no esta definida (" + propiedad + ")");
			return;
		}
		if (CLAVES_CONOCIDAS.contains(valor.toLowerCase())) {
			problemas.add(descripcion + " es un valor de desarrollo conocido (" + propiedad + ")");
		}
	}

	private void validarAntivirus(ConfigurableEnvironment entorno, List<String> problemas) {
		String motor = entorno.getProperty("nextdocs.antivirus.motor", MotorAntivirus.PERMISIVO.name());
		if (!MotorAntivirus.PERMISIVO.name().equalsIgnoreCase(motor)) {
			return;
		}
		if (Boolean.parseBoolean(entorno.getProperty("nextdocs.antivirus.permitirSinAnalisis", "false"))) {
			log.warn("El antivirus esta desactivado en produccion por decision explicita del operador");
			return;
		}
		problemas.add("NEXTDOCS_ANTIVIRUS_MOTOR es PERMISIVO: los archivos no se analizan. "
				+ "Configura CLAMAV, o aceptalo con NEXTDOCS_ANTIVIRUS_PERMITIR_SIN_ANALISIS=true");
	}

	private void validarCredencialesEnClaro(ConfigurableEnvironment entorno, List<String> problemas) {
		for (String propiedad : List.of("nextdocs.proveedor-ia.claveGemini", "nextdocs.proveedor-ia.claveDeepseek")) {
			String valor = entorno.getProperty(propiedad);
			if (valor != null && !valor.isBlank()) {
				problemas.add("La credencial " + propiedad + " no debe fijarse por configuracion. "
						+ "Usa ConfiguracionProveedor.referenciaSecreto con el prefijo env:");
			}
		}
	}

	public static class ConfiguracionInseguraException extends IllegalStateException {

		private static final long serialVersionUID = 8118437302561094428L;

		public ConfiguracionInseguraException(String mensaje) {
			super(mensaje);
		}
	}
}
