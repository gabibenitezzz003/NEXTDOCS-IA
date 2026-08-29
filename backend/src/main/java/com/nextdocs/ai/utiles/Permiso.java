package com.nextdocs.ai.utiles;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class Permiso {

	public static final String DOCUMENTOS_LEER = "documentos.leer";

	public static final String DOCUMENTOS_ESCRIBIR = "documentos.escribir";

	public static final String DOCUMENTOS_REVISAR = "documentos.revisar";

	public static final String DOCUMENTOS_ELIMINAR = "documentos.eliminar";

	public static final String DOCUMENTOS_EXPORTAR = "documentos.exportar";

	public static final String PLANTILLAS_LEER = "plantillas.leer";

	public static final String PLANTILLAS_ESCRIBIR = "plantillas.escribir";

	public static final String PLANTILLAS_PUBLICAR = "plantillas.publicar";

	public static final String EXCEPCIONES_LEER = "excepciones.leer";

	public static final String EXCEPCIONES_GESTIONAR = "excepciones.gestionar";

	public static final String GOBERNANZA_LEER = "gobernanza.leer";

	public static final String GOBERNANZA_ADMINISTRAR = "gobernanza.administrar";

	public static final String TENANT_ADMINISTRAR = "tenant.administrar";

	public static final String CODIGO_ROL_ADMINISTRADOR = "ADMINISTRADOR";

	public static final String CODIGO_ROL_OPERADOR = "OPERADOR";

	public static final String CODIGO_ROL_REVISOR = "REVISOR";

	public static final String CODIGO_ROL_AUDITOR = "AUDITOR";

	private Permiso() {
	}

	public static Set<String> todos() {
		return new LinkedHashSet<>(List.of(DOCUMENTOS_LEER, DOCUMENTOS_ESCRIBIR, DOCUMENTOS_REVISAR,
				DOCUMENTOS_ELIMINAR, DOCUMENTOS_EXPORTAR, PLANTILLAS_LEER, PLANTILLAS_ESCRIBIR, PLANTILLAS_PUBLICAR, EXCEPCIONES_LEER,
				EXCEPCIONES_GESTIONAR, GOBERNANZA_LEER, GOBERNANZA_ADMINISTRAR, TENANT_ADMINISTRAR));
	}

	public static Set<String> deOperador() {
		return new LinkedHashSet<>(
				List.of(DOCUMENTOS_LEER, DOCUMENTOS_ESCRIBIR, PLANTILLAS_LEER, EXCEPCIONES_LEER));
	}

	public static Set<String> deRevisor() {
		return new LinkedHashSet<>(List.of(DOCUMENTOS_LEER, DOCUMENTOS_ESCRIBIR, DOCUMENTOS_REVISAR, PLANTILLAS_LEER,
				EXCEPCIONES_LEER, EXCEPCIONES_GESTIONAR));
	}

	public static Set<String> deAuditor() {
		return new LinkedHashSet<>(
				List.of(DOCUMENTOS_LEER, DOCUMENTOS_EXPORTAR, PLANTILLAS_LEER, EXCEPCIONES_LEER, GOBERNANZA_LEER));
	}
}
