package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

import com.nextdocs.ai.enumeraciones.TipoActor;

import lombok.Data;

@Data
public class PrincipalNextDocs implements Serializable {

	private static final long serialVersionUID = 2748561930244857712L;

	private String idActor;

	private TipoActor tipoActor;

	private String tenantId;

	private String codigoTenant;

	private String descripcion;

	private String email;

	private Set<String> permisos = new LinkedHashSet<>();

	public boolean esCuentaServicio() {
		return TipoActor.CUENTA_SERVICIO == tipoActor;
	}

	public boolean tienePermiso(String permiso) {
		return permisos.contains(permiso);
	}
}
