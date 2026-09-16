package com.nextdocs.ai.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.nextdocs.ai.convertidores.AdministracionConverter;
import com.nextdocs.ai.entidades.Rol;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.RolModel;
import com.nextdocs.ai.modelos.RolReqModel;
import com.nextdocs.ai.repositorios.RolRepository;
import com.nextdocs.ai.repositorios.UsuarioRepository;
import com.nextdocs.ai.utiles.Permiso;

class RolServiceTest {

	private RolRepository rolRepository;

	private RolService servicio;

	@BeforeEach
	void preparar() {
		rolRepository = Mockito.mock(RolRepository.class);
		AdministracionConverter converter = Mockito.mock(AdministracionConverter.class);
		when(converter.aModelo(any(Rol.class), any(Long.class))).thenReturn(new RolModel());
		when(rolRepository.buscarPorCodigo(any(), any())).thenReturn(Optional.empty());
		servicio = new RolService(rolRepository, Mockito.mock(UsuarioRepository.class),
				Mockito.mock(AuditoriaService.class), converter);
	}

	@Test
	void elAlcanceDeServicioNoSeAsignaARoles() {
		RolReqModel datos = new RolReqModel();
		datos.setCodigo("OPERADOR_EVENTOS");
		datos.setNombre("Operador de eventos");
		datos.setPermisos(List.of(Permiso.DOCUMENTOS_LEER, Permiso.INTEGRACIONES_ESCRIBIR));

		assertThatThrownBy(() -> servicio.crear(tenant(), datos))
				.isInstanceOf(ValidacionException.class)
				.hasMessageContaining("cuentas de servicio");
	}

	@Test
	void elPermisoDeServicioEstaDisponibleParaCuentasPeroNoParaRoles() {
		assertThat(Permiso.todos()).contains(Permiso.INTEGRACIONES_ESCRIBIR);
		assertThat(Permiso.deAdministrador()).doesNotContain(Permiso.INTEGRACIONES_ESCRIBIR);
	}

	private Tenant tenant() {
		Tenant tenant = new Tenant();
		tenant.setId("tenant-1");
		return tenant;
	}
}
