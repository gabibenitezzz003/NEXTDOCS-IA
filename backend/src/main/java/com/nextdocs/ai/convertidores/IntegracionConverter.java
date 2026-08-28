package com.nextdocs.ai.convertidores;

import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.config.PropiedadesWebhooks;
import com.nextdocs.ai.entidades.EntregaWebhook;
import com.nextdocs.ai.entidades.EventoSalida;
import com.nextdocs.ai.entidades.SuscripcionWebhook;
import com.nextdocs.ai.modelos.EntregaWebhookModel;
import com.nextdocs.ai.modelos.SuscripcionWebhookModel;

import org.springframework.stereotype.Component;

@Component
public class IntegracionConverter {

	public static final int LARGO_PREFIJO_SECRETO = 8;

	private final PropiedadesWebhooks propiedades;

	public IntegracionConverter(PropiedadesWebhooks propiedades) {
		this.propiedades = propiedades;
	}

	public SuscripcionWebhookModel aModelo(SuscripcionWebhook suscripcion) {
		SuscripcionWebhookModel modelo = new SuscripcionWebhookModel();
		modelo.setId(suscripcion.getId());
		modelo.setNombre(suscripcion.getNombre());
		modelo.setUrl(suscripcion.getUrl());
		modelo.setPrefijoSecreto(prefijo(suscripcion.getSecreto()));
		modelo.setActiva(suscripcion.isActiva());
		modelo.setPausadaPorFallos(!suscripcion.isActiva() && suscripcion.getFallosConsecutivos() >= umbralPausa());
		modelo.setFallosConsecutivos(suscripcion.getFallosConsecutivos());
		modelo.setUltimaEntrega(suscripcion.getUltimaEntrega());
		modelo.setAlta(suscripcion.getAlta());
		modelo.getEventos().addAll(suscripcion.getEventos());
		return modelo;
	}

	public List<SuscripcionWebhookModel> aModelos(List<SuscripcionWebhook> suscripciones) {
		List<SuscripcionWebhookModel> modelos = new ArrayList<>();
		for (SuscripcionWebhook suscripcion : suscripciones) {
			modelos.add(aModelo(suscripcion));
		}
		return modelos;
	}

	public EntregaWebhookModel aModelo(EntregaWebhook entrega, EventoSalida evento) {
		EntregaWebhookModel modelo = new EntregaWebhookModel();
		modelo.setId(entrega.getId());
		modelo.setEventoId(entrega.getEventoId());
		modelo.setEstado(entrega.getEstado());
		modelo.setIntento(entrega.getIntento());
		modelo.setCodigoRespuesta(entrega.getCodigoRespuesta());
		modelo.setCuerpoRespuesta(entrega.getCuerpoRespuesta());
		modelo.setDuracionMilisegundos(entrega.getDuracionMilisegundos());
		modelo.setDisponibleEn(entrega.getDisponibleEn());
		modelo.setEntregado(entrega.getEntregado());
		modelo.setAlta(entrega.getAlta());
		if (entrega.getSuscripcion() != null) {
			modelo.setSuscripcionId(entrega.getSuscripcion().getId());
			modelo.setNombreSuscripcion(entrega.getSuscripcion().getNombre());
		}
		if (evento != null) {
			modelo.setTipoEvento(evento.getTipoEvento());
		}
		return modelo;
	}

	private String prefijo(String secreto) {
		if (secreto == null || secreto.length() < LARGO_PREFIJO_SECRETO) {
			return "";
		}
		return secreto.substring(0, LARGO_PREFIJO_SECRETO);
	}

	private int umbralPausa() {
		return propiedades.getUmbralPausa() <= 0 ? 10 : propiedades.getUmbralPausa();
	}
}
