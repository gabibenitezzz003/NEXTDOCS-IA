package com.nextdocs.ai.repositorios;

import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.enumeraciones.EstadoTenant;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, String> {

	Optional<Tenant> findByCodigoAndBajaIsNull(String codigo);

	Optional<Tenant> findByDominioAndBajaIsNull(String dominio);

	List<Tenant> findByEstadoAndBajaIsNull(EstadoTenant estado);
}
