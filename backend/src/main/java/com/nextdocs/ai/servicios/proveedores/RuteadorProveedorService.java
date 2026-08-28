package com.nextdocs.ai.servicios.proveedores;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.nextdocs.ai.config.PropiedadesProveedorIa;
import com.nextdocs.ai.entidades.ConfiguracionProveedor;
import com.nextdocs.ai.enumeraciones.ProveedorDocumentalIa;
import com.nextdocs.ai.exceptions.ProveedorNoDisponibleException;
import com.nextdocs.ai.interfaces.ProveedorDocumentalIaInt;
import com.nextdocs.ai.repositorios.ConfiguracionProveedorRepository;

import org.springframework.stereotype.Service;

@Service
public class RuteadorProveedorService {

	private final Map<ProveedorDocumentalIa, ProveedorDocumentalIaInt> adaptadores = new EnumMap<>(
			ProveedorDocumentalIa.class);

	private final ConfiguracionProveedorRepository configuracionProveedorRepository;

	private final PropiedadesProveedorIa propiedades;

	public RuteadorProveedorService(List<ProveedorDocumentalIaInt> proveedores,
			ConfiguracionProveedorRepository configuracionProveedorRepository, PropiedadesProveedorIa propiedades) {
		for (ProveedorDocumentalIaInt proveedor : proveedores) {
			ProveedorDocumentalIaInt anterior = adaptadores.put(proveedor.tipo(), proveedor);
			if (anterior != null) {
				throw new IllegalStateException("Hay dos adaptadores registrados para el proveedor "
						+ proveedor.tipo() + ": " + anterior.getClass().getSimpleName() + " y "
						+ proveedor.getClass().getSimpleName()
						+ ". Cada implementacion de ProveedorDocumentalIaInt debe declarar un tipo distinto");
			}
		}
		this.configuracionProveedorRepository = configuracionProveedorRepository;
		this.propiedades = propiedades;
	}

	public ProveedorDocumentalIaInt resolverPrincipal(String tenantId) {
		List<ConfiguracionProveedor> configuraciones = configuracionProveedorRepository
				.listarActivasPrincipales(tenantId);
		for (ConfiguracionProveedor configuracion : configuraciones) {
			ProveedorDocumentalIaInt adaptador = adaptadores.get(configuracion.getProveedor());
			if (adaptador != null && adaptador.estaDisponible()) {
				return adaptador;
			}
		}
		return porDefecto();
	}

	public Optional<ProveedorDocumentalIaInt> resolverRespaldo(String tenantId, ProveedorDocumentalIa descartado) {
		List<ConfiguracionProveedor> configuraciones = configuracionProveedorRepository.listarActivasRespaldo(tenantId);
		for (ConfiguracionProveedor configuracion : configuraciones) {
			if (configuracion.getProveedor() == descartado) {
				continue;
			}
			ProveedorDocumentalIaInt adaptador = adaptadores.get(configuracion.getProveedor());
			if (adaptador != null && adaptador.estaDisponible()) {
				return Optional.of(adaptador);
			}
		}
		return Optional.empty();
	}

	public Optional<ConfiguracionProveedor> configuracionDe(String tenantId, ProveedorDocumentalIa proveedor) {
		return configuracionProveedorRepository.buscarPorProveedor(tenantId, proveedor);
	}

	private ProveedorDocumentalIaInt porDefecto() {
		ProveedorDocumentalIaInt adaptador = adaptadores.get(propiedades.getProveedorPorDefecto());
		if (adaptador == null) {
			throw new ProveedorNoDisponibleException(
					"No hay proveedor de IA configurado para " + propiedades.getProveedorPorDefecto(), false);
		}
		return adaptador;
	}
}
