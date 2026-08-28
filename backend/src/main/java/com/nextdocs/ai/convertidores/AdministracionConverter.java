package com.nextdocs.ai.convertidores;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.nextdocs.ai.entidades.CuentaServicio;
import com.nextdocs.ai.entidades.Rol;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.modelos.CuentaServicioModel;
import com.nextdocs.ai.modelos.RolModel;
import com.nextdocs.ai.modelos.TenantModel;
import com.nextdocs.ai.modelos.UsuarioModel;
import com.nextdocs.ai.utiles.Permiso;

import org.springframework.stereotype.Component;

@Component
public class AdministracionConverter {

	public UsuarioModel aModelo(Usuario usuario) {
		UsuarioModel modelo = new UsuarioModel();
		modelo.setId(usuario.getId());
		modelo.setEmail(usuario.getEmail());
		modelo.setNombre(usuario.getNombre());
		modelo.setEstado(usuario.getEstado());
		modelo.setOrigenIdentidad(usuario.getOrigenIdentidad());
		modelo.setIdUsuarioExterno(usuario.getIdUsuarioExterno());
		modelo.setIdioma(usuario.getIdioma());
		modelo.setZonaHoraria(usuario.getZonaHoraria());
		modelo.setUltimoAcceso(usuario.getUltimoAcceso());
		modelo.setAlta(usuario.getAlta());
		TreeSet<String> permisos = new TreeSet<>();
		for (Rol rol : usuario.getRoles()) {
			modelo.getRoles().add(rol.getCodigo());
			permisos.addAll(rol.getPermisos());
		}
		modelo.getPermisos().addAll(permisos);
		modelo.setAdministrador(permisos.contains(Permiso.TENANT_ADMINISTRAR));
		return modelo;
	}

	public List<UsuarioModel> aModelosUsuario(List<Usuario> usuarios) {
		List<UsuarioModel> modelos = new ArrayList<>();
		for (Usuario usuario : usuarios) {
			modelos.add(aModelo(usuario));
		}
		return modelos;
	}

	public RolModel aModelo(Rol rol, long cantidadUsuarios) {
		RolModel modelo = new RolModel();
		modelo.setId(rol.getId());
		modelo.setCodigo(rol.getCodigo());
		modelo.setNombre(rol.getNombre());
		modelo.setDescripcion(rol.getDescripcion());
		modelo.setPredefinido(rol.isPredefinido());
		modelo.setCantidadUsuarios(cantidadUsuarios);
		modelo.setAlta(rol.getAlta());
		modelo.getPermisos().addAll(new TreeSet<>(rol.getPermisos()));
		return modelo;
	}

	public CuentaServicioModel aModelo(CuentaServicio cuenta) {
		CuentaServicioModel modelo = new CuentaServicioModel();
		modelo.setId(cuenta.getId());
		modelo.setNombre(cuenta.getNombre());
		modelo.setPrefijoClave(cuenta.getPrefijoClave());
		modelo.setActiva(cuenta.isActiva());
		modelo.setExpira(cuenta.getExpira());
		modelo.setExpirada(cuenta.getExpira() != null && cuenta.getExpira().isBefore(Instant.now()));
		modelo.setUltimoUso(cuenta.getUltimoUso());
		modelo.setMotivoRevocacion(cuenta.getMotivoRevocacion());
		modelo.setAlta(cuenta.getAlta());
		modelo.getAlcances().addAll(new TreeSet<>(cuenta.getAlcances()));
		return modelo;
	}

	public List<CuentaServicioModel> aModelosCuenta(List<CuentaServicio> cuentas) {
		List<CuentaServicioModel> modelos = new ArrayList<>();
		for (CuentaServicio cuenta : cuentas) {
			modelos.add(aModelo(cuenta));
		}
		return modelos;
	}

	public TenantModel aModelo(Tenant tenant, long almacenamientoUsado, long usuariosActivos) {
		TenantModel modelo = new TenantModel();
		modelo.setId(tenant.getId());
		modelo.setCodigo(tenant.getCodigo());
		modelo.setNombre(tenant.getNombre());
		modelo.setEstado(tenant.getEstado());
		modelo.setPlan(tenant.getPlan());
		modelo.setRegion(tenant.getRegion());
		modelo.setDominio(tenant.getDominio());
		modelo.setCuotaAlmacenamientoBytes(tenant.getCuotaAlmacenamientoBytes());
		modelo.setAlmacenamientoUsadoBytes(almacenamientoUsado);
		modelo.setUsuariosActivos(usuariosActivos);
		modelo.setAlta(tenant.getAlta());
		return modelo;
	}
}
