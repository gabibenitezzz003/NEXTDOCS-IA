package com.nextdocs.ai.config;

import com.nextdocs.ai.repositorios.TenantRepository;
import com.nextdocs.ai.servicios.TenantService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ArranqueInicializador {

	private static final Logger log = LoggerFactory.getLogger(ArranqueInicializador.class);

	@Bean
	public ApplicationRunner inicializarTenantDemostracion(PropiedadesArranque propiedades,
			TenantRepository tenantRepository, TenantService tenantService) {
		return argumentos -> {
			if (!propiedades.isCrearTenantDemostracion()) {
				return;
			}
			if (tenantRepository.findByCodigoAndBajaIsNull(propiedades.getCodigoTenant()).isPresent()) {
				return;
			}
			tenantService.crear(propiedades.getCodigoTenant(), propiedades.getNombreTenant(),
					propiedades.getEmailAdministrador(), propiedades.getClaveAdministrador(),
					propiedades.getIdTenant());
			log.info("Tenant de demostracion {} creado con administrador {}", propiedades.getCodigoTenant(),
					propiedades.getEmailAdministrador());
		};
	}
}
