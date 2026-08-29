package com.nextdocs.ai.convertidores;

import java.time.Instant;
import java.util.List;

import com.nextdocs.ai.config.PropiedadesWhatsapp;
import com.nextdocs.ai.entidades.ContactoWhatsappAutorizado;
import com.nextdocs.ai.entidades.CorrelacionWhatsapp;
import com.nextdocs.ai.entidades.LineaWhatsapp;
import com.nextdocs.ai.entidades.MediaWhatsapp;
import com.nextdocs.ai.entidades.MensajeWhatsappEntrante;
import com.nextdocs.ai.entidades.MensajeWhatsappSaliente;
import com.nextdocs.ai.modelos.ContactoWhatsappModel;
import com.nextdocs.ai.modelos.CorrelacionWhatsappModel;
import com.nextdocs.ai.modelos.LineaWhatsappModel;
import com.nextdocs.ai.modelos.MediaWhatsappModel;
import com.nextdocs.ai.modelos.MensajeWhatsappModel;
import com.nextdocs.ai.modelos.MensajeWhatsappSalienteModel;
import com.nextdocs.ai.utiles.TokenCorrelacion;

import org.springframework.stereotype.Component;

@Component
public class CanalWhatsappConverter {

	public static final String RUTA_WEBHOOK = "/api/v1/canales/whatsapp/webhook/";

	private final PropiedadesWhatsapp propiedades;

	public CanalWhatsappConverter(PropiedadesWhatsapp propiedades) {
		this.propiedades = propiedades;
	}

	public LineaWhatsappModel aModelo(LineaWhatsapp linea, List<ContactoWhatsappAutorizado> contactos) {
		LineaWhatsappModel modelo = new LineaWhatsappModel();
		modelo.setId(linea.getId());
		modelo.setNombre(linea.getNombre());
		modelo.setNumeroTelefono(linea.getNumeroTelefono());
		modelo.setIdentificadorNumero(linea.getIdentificadorNumero());
		modelo.setIdentificadorCuenta(linea.getIdentificadorCuenta());
		modelo.setRutaWebhook(linea.getRutaWebhook());
		modelo.setUrlWebhook(urlWebhookDe(linea));
		modelo.setEstado(linea.getEstado().name());
		modelo.setCodigoPlantillaPorDefecto(linea.getCodigoPlantillaPorDefecto());
		modelo.setExigirContactoAutorizado(linea.isExigirContactoAutorizado());
		modelo.setExigirCorrelacion(linea.isExigirCorrelacion());
		modelo.setAcusarRecibo(linea.isAcusarRecibo());
		modelo.setMaximoMediaPorMensaje(linea.getMaximoMediaPorMensaje());
		modelo.setMinutosVentanaCorrelacion(linea.getMinutosVentanaCorrelacion());
		modelo.setNombrePlantillaSolicitud(linea.getNombrePlantillaSolicitud());
		modelo.setIdiomaPlantillaSolicitud(linea.getIdiomaPlantillaSolicitud());
		modelo.setPuedeResponderFueraDeVentana(linea.getNombrePlantillaSolicitud() != null
				&& !linea.getNombrePlantillaSolicitud().isBlank());
		modelo.setUltimoMensaje(linea.getUltimoMensaje());
		modelo.setUltimoError(linea.getUltimoError());
		modelo.setFallosConsecutivos(linea.getFallosConsecutivos());
		modelo.setAlta(linea.getAlta());
		if (contactos != null) {
			modelo.setContactos(contactos.stream().map(this::aModelo).toList());
		}
		return modelo;
	}

	public ContactoWhatsappModel aModelo(ContactoWhatsappAutorizado contacto) {
		ContactoWhatsappModel modelo = new ContactoWhatsappModel();
		modelo.setId(contacto.getId());
		modelo.setPatron(contacto.getPatron());
		modelo.setDescripcion(contacto.getDescripcion());
		modelo.setAlta(contacto.getAlta());
		return modelo;
	}

	public CorrelacionWhatsappModel aModelo(CorrelacionWhatsapp correlacion) {
		CorrelacionWhatsappModel modelo = new CorrelacionWhatsappModel();
		modelo.setId(correlacion.getId());
		modelo.setToken(correlacion.getToken());
		modelo.setTextoParaEnviar(TokenCorrelacion.etiquetar(correlacion.getToken()));
		if (correlacion.getLinea() != null) {
			modelo.setLineaId(correlacion.getLinea().getId());
			modelo.setNumeroLinea(correlacion.getLinea().getNumeroTelefono());
		}
		if (correlacion.getSujeto() != null) {
			modelo.setSujetoOrigen(correlacion.getSujeto().getOrigen());
			modelo.setSujetoTipoObjeto(correlacion.getSujeto().getTipoObjeto());
			modelo.setSujetoIdObjeto(correlacion.getSujeto().getIdObjeto());
		}
		modelo.setCodigoPlantilla(correlacion.getCodigoPlantilla());
		modelo.setNumeroDestino(correlacion.getNumeroDestino());
		modelo.setNumeroVinculado(correlacion.getNumeroVinculado());
		modelo.setVinculadoEn(correlacion.getVinculadoEn());
		modelo.setDescripcion(correlacion.getDescripcion());
		modelo.setVenceEn(correlacion.getVenceEn());
		modelo.setVigente(correlacion.getVenceEn() == null || correlacion.getVenceEn().isAfter(Instant.now()));
		modelo.setDocumentosRecibidos(correlacion.getDocumentosRecibidos());
		modelo.setUltimoUso(correlacion.getUltimoUso());
		modelo.setAlta(correlacion.getAlta());
		return modelo;
	}

	public MensajeWhatsappModel aModelo(MensajeWhatsappEntrante mensaje, List<MediaWhatsapp> media) {
		MensajeWhatsappModel modelo = new MensajeWhatsappModel();
		modelo.setId(mensaje.getId());
		modelo.setLineaId(mensaje.getLinea() == null ? null : mensaje.getLinea().getId());
		modelo.setNumeroLinea(mensaje.getLinea() == null ? null : mensaje.getLinea().getNumeroTelefono());
		modelo.setIdentificadorMensaje(mensaje.getIdentificadorMensaje());
		modelo.setNumeroOrigen(mensaje.getNumeroOrigen());
		modelo.setNombrePerfil(mensaje.getNombrePerfil());
		modelo.setTipo(mensaje.getTipo());
		modelo.setTexto(mensaje.getTexto());
		modelo.setTokenDetectado(mensaje.getTokenDetectado());
		modelo.setCorrelacionId(mensaje.getCorrelacion() == null ? null : mensaje.getCorrelacion().getId());
		modelo.setResultado(mensaje.getResultado().name());
		modelo.setMotivo(mensaje.getMotivo());
		modelo.setMedia(mensaje.getMedia());
		modelo.setIngestados(mensaje.getIngestados());
		modelo.setRechazados(mensaje.getRechazados());
		modelo.setRecibidoEn(mensaje.getRecibidoEn());
		modelo.setAlta(mensaje.getAlta());
		if (media != null) {
			modelo.setDetalleMedia(media.stream().map(this::aModelo).toList());
		}
		return modelo;
	}

	public MediaWhatsappModel aModelo(MediaWhatsapp media) {
		MediaWhatsappModel modelo = new MediaWhatsappModel();
		modelo.setId(media.getId());
		modelo.setDocumentoId(media.getDocumento() == null ? null : media.getDocumento().getId());
		modelo.setIdentificadorMedia(media.getIdentificadorMedia());
		modelo.setNombreArchivo(media.getNombreArchivo());
		modelo.setTipoMime(media.getTipoMime());
		modelo.setTamanoBytes(media.getTamanoBytes());
		modelo.setSha256(media.getSha256());
		modelo.setResultado(media.getResultado().name());
		modelo.setCodigoRechazo(media.getCodigoRechazo());
		modelo.setMotivo(media.getMotivo());
		return modelo;
	}

	public MensajeWhatsappSalienteModel aModelo(MensajeWhatsappSaliente mensaje) {
		MensajeWhatsappSalienteModel modelo = new MensajeWhatsappSalienteModel();
		modelo.setId(mensaje.getId());
		modelo.setLineaId(mensaje.getLinea() == null ? null : mensaje.getLinea().getId());
		modelo.setCorrelacionId(mensaje.getCorrelacion() == null ? null : mensaje.getCorrelacion().getId());
		modelo.setPlantilla(mensaje.getPlantilla().name());
		modelo.setNumeroDestino(mensaje.getNumeroDestino());
		modelo.setIdentificadorMensaje(mensaje.getIdentificadorMensaje());
		modelo.setEstado(mensaje.getEstado().name());
		modelo.setDentroDeVentana(mensaje.isDentroDeVentana());
		modelo.setNombrePlantillaMeta(mensaje.getNombrePlantillaMeta());
		modelo.setCuerpo(mensaje.getCuerpo());
		modelo.setDetalleError(mensaje.getDetalleError());
		modelo.setEnviado(mensaje.getEnviado());
		modelo.setAlta(mensaje.getAlta());
		return modelo;
	}

	private String urlWebhookDe(LineaWhatsapp linea) {
		String base = propiedades.getUrlPublicaWebhook();
		if (base == null || base.isBlank()) {
			return RUTA_WEBHOOK + linea.getRutaWebhook();
		}
		String limpia = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
		return limpia + RUTA_WEBHOOK + linea.getRutaWebhook();
	}
}
