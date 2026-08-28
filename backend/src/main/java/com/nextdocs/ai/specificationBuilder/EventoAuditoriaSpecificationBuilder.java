package com.nextdocs.ai.specificationBuilder;

import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.entidades.EventoAuditoria;
import com.nextdocs.ai.modelos.FiltroAuditoriaModel;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

public final class EventoAuditoriaSpecificationBuilder {

	private EventoAuditoriaSpecificationBuilder() {
	}

	public static Specification<EventoAuditoria> construir(String tenantId, FiltroAuditoriaModel filtro) {
		return (raiz, consulta, constructor) -> {
			List<Predicate> predicados = new ArrayList<>();
			predicados.add(constructor.equal(raiz.get("tenantId"), tenantId));
			if (filtro == null) {
				return constructor.and(predicados.toArray(new Predicate[0]));
			}
			if (filtro.getDesde() != null) {
				predicados.add(constructor.greaterThanOrEqualTo(raiz.get("fecha"), filtro.getDesde()));
			}
			if (filtro.getHasta() != null) {
				predicados.add(constructor.lessThanOrEqualTo(raiz.get("fecha"), filtro.getHasta()));
			}
			if (filtro.getAccion() != null) {
				predicados.add(constructor.equal(raiz.get("accion"), filtro.getAccion()));
			}
			if (filtro.getTipoRecurso() != null && !filtro.getTipoRecurso().isBlank()) {
				predicados.add(constructor.equal(raiz.get("tipoRecurso"), filtro.getTipoRecurso()));
			}
			if (filtro.getIdRecurso() != null && !filtro.getIdRecurso().isBlank()) {
				predicados.add(constructor.equal(raiz.get("idRecurso"), filtro.getIdRecurso()));
			}
			if (filtro.getTipoActor() != null) {
				predicados.add(constructor.equal(raiz.get("tipoActor"), filtro.getTipoActor()));
			}
			if (filtro.getIdActor() != null && !filtro.getIdActor().isBlank()) {
				predicados.add(constructor.equal(raiz.get("idActor"), filtro.getIdActor()));
			}
			if (filtro.getCorrelacionId() != null && !filtro.getCorrelacionId().isBlank()) {
				predicados.add(constructor.equal(raiz.get("correlacionId"), filtro.getCorrelacionId()));
			}
			if (filtro.getExitoso() != null) {
				predicados.add(constructor.equal(raiz.get("exitoso"), filtro.getExitoso()));
			}
			return constructor.and(predicados.toArray(new Predicate[0]));
		};
	}
}
