package com.nextdocs.ai.utiles;

import com.nextdocs.ai.exceptions.NoAutorizadoException;
import com.nextdocs.ai.modelos.PrincipalNextDocs;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class ContextoSeguridad {

	private ContextoSeguridad() {
	}

	public static PrincipalNextDocs principal() {
		Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacion == null || !(autenticacion.getPrincipal() instanceof PrincipalNextDocs principal)) {
			throw new NoAutorizadoException("No hay un principal autenticado en el contexto");
		}
		return principal;
	}

	public static PrincipalNextDocs principalONulo() {
		Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacion == null || !(autenticacion.getPrincipal() instanceof PrincipalNextDocs principal)) {
			return null;
		}
		return principal;
	}

	public static String tenantId() {
		return principal().getTenantId();
	}

	public static String idActor() {
		return principal().getIdActor();
	}
}
