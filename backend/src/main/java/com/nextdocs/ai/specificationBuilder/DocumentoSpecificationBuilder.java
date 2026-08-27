package com.nextdocs.ai.specificationBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.enumeraciones.EstadoDocumento;
import com.nextdocs.ai.enumeraciones.OrigenDocumento;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

public final class DocumentoSpecificationBuilder {

	private DocumentoSpecificationBuilder() {
	}

	public static Specification<Documento> construir(String tenantId, List<EstadoDocumento> estados,
			OrigenDocumento origen, String codigoPlantilla, String texto, Instant desde, Instant hasta,
			Boolean soloRaiz) {
		return (raiz, consulta, constructor) -> {
			List<Predicate> predicados = new ArrayList<>();
			predicados.add(constructor.equal(raiz.get("tenant").get("id"), tenantId));
			predicados.add(constructor.isNull(raiz.get("baja")));
			if (estados != null && !estados.isEmpty()) {
				predicados.add(raiz.get("estado").in(estados));
			}
			if (origen != null) {
				predicados.add(constructor.equal(raiz.get("origen"), origen));
			}
			if (codigoPlantilla != null && !codigoPlantilla.isBlank()) {
				predicados.add(constructor.equal(raiz.get("plantilla").get("codigo"), codigoPlantilla));
			}
			if (texto != null && !texto.isBlank()) {
				String patron = "%" + texto.toLowerCase() + "%";
				predicados.add(constructor.or(constructor.like(constructor.lower(raiz.get("nombre")), patron),
						constructor.like(constructor.lower(raiz.get("remitente")), patron),
						constructor.like(constructor.lower(raiz.get("referenciaSujeto").get("idObjeto")), patron)));
			}
			if (desde != null) {
				predicados.add(constructor.greaterThanOrEqualTo(raiz.get("recibido"), desde));
			}
			if (hasta != null) {
				predicados.add(constructor.lessThanOrEqualTo(raiz.get("recibido"), hasta));
			}
			if (Boolean.TRUE.equals(soloRaiz)) {
				predicados.add(constructor.isNull(raiz.get("documentoPadre")));
			}
			return constructor.and(predicados.toArray(new Predicate[0]));
		};
	}
}
