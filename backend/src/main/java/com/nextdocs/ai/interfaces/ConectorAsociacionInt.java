package com.nextdocs.ai.interfaces;

import java.util.List;

import com.nextdocs.ai.modelos.CandidatoAsociacionModel;
import com.nextdocs.ai.modelos.ContextoAsociacionModel;

public interface ConectorAsociacionInt {

	String codigo();

	boolean estaHabilitado(String tenantId);

	List<CandidatoAsociacionModel> buscarCandidatos(ContextoAsociacionModel contexto);
}
