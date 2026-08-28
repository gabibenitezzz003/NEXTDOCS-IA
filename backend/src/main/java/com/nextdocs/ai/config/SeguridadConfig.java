package com.nextdocs.ai.config;

import com.nextdocs.ai.filtros.FiltroAutenticacion;
import com.nextdocs.ai.filtros.FiltroCorrelacion;
import com.nextdocs.ai.filtros.FiltroLimiteUso;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SeguridadConfig {

	private final PropiedadesSeguridad propiedades;

	private final FiltroAutenticacion filtroAutenticacion;

	private final FiltroCorrelacion filtroCorrelacion;

	private final FiltroLimiteUso filtroLimiteUso;

	public SeguridadConfig(PropiedadesSeguridad propiedades, FiltroAutenticacion filtroAutenticacion,
			FiltroCorrelacion filtroCorrelacion, FiltroLimiteUso filtroLimiteUso) {
		this.propiedades = propiedades;
		this.filtroAutenticacion = filtroAutenticacion;
		this.filtroCorrelacion = filtroCorrelacion;
		this.filtroLimiteUso = filtroLimiteUso;
	}

	@Bean
	public PasswordEncoder codificadorClave() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain cadenaFiltros(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.cors(cors -> cors.configurationSource(configuracionCors()))
				.sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(peticiones -> peticiones
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(propiedades.getRutasPublicas().toArray(new String[0])).permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(filtroCorrelacion, UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(filtroAutenticacion, UsernamePasswordAuthenticationFilter.class)
				.addFilterAfter(filtroLimiteUso, FiltroAutenticacion.class);
		return http.build();
	}

	@Bean
	public CorsConfigurationSource configuracionCors() {
		CorsConfiguration configuracion = new CorsConfiguration();
		configuracion.setAllowedOriginPatterns(List.of("*"));
		configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuracion.setAllowedHeaders(List.of("*"));
		configuracion.setExposedHeaders(List.of("X-Correlacion-Id"));
		configuracion.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
		fuente.registerCorsConfiguration("/**", configuracion);
		return fuente;
	}
}
