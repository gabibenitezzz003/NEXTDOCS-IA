package com.nextdocs.ai.convertidores;

import java.time.Instant;
import java.util.List;

import com.nextdocs.ai.entidades.AdjuntoCorreo;
import com.nextdocs.ai.entidades.BuzonCorreo;
import com.nextdocs.ai.entidades.CorrelacionCorreo;
import com.nextdocs.ai.entidades.MensajeCorreoEntrante;
import com.nextdocs.ai.entidades.MensajeCorreoSaliente;
import com.nextdocs.ai.entidades.RemitenteAutorizado;
import com.nextdocs.ai.modelos.AdjuntoCorreoModel;
import com.nextdocs.ai.modelos.BuzonCorreoModel;
import com.nextdocs.ai.modelos.CorrelacionCorreoModel;
import com.nextdocs.ai.modelos.MensajeCorreoModel;
import com.nextdocs.ai.modelos.MensajeSalienteModel;
import com.nextdocs.ai.modelos.RemitenteAutorizadoModel;
import com.nextdocs.ai.utiles.TokenCorrelacion;

import org.springframework.stereotype.Component;

@Component
public class CanalCorreoConverter {

	public BuzonCorreoModel aModelo(BuzonCorreo buzon, List<RemitenteAutorizado> remitentes) {
		BuzonCorreoModel modelo = new BuzonCorreoModel();
		modelo.setId(buzon.getId());
		modelo.setDireccion(buzon.getDireccion());
		modelo.setNombre(buzon.getNombre());
		modelo.setEstado(buzon.getEstado().name());
		modelo.setHostEntrada(buzon.getHostEntrada());
		modelo.setPuertoEntrada(buzon.getPuertoEntrada());
		modelo.setUsuarioEntrada(buzon.getUsuarioEntrada());
		modelo.setCarpeta(buzon.getCarpeta());
		modelo.setEntradaSegura(buzon.isEntradaSegura());
		modelo.setHostSalida(buzon.getHostSalida());
		modelo.setPuertoSalida(buzon.getPuertoSalida());
		modelo.setSalidaSegura(buzon.isSalidaSegura());
		modelo.setPuedeResponder(buzon.getHostSalida() != null && !buzon.getHostSalida().isBlank());
		modelo.setCodigoPlantillaPorDefecto(buzon.getCodigoPlantillaPorDefecto());
		modelo.setExigirRemitenteAutorizado(buzon.isExigirRemitenteAutorizado());
		modelo.setExigirCorrelacion(buzon.isExigirCorrelacion());
		modelo.setAcusarRecibo(buzon.isAcusarRecibo());
		modelo.setMaximoAdjuntosPorMensaje(buzon.getMaximoAdjuntosPorMensaje());
		modelo.setUltimaLectura(buzon.getUltimaLectura());
		modelo.setUltimoError(buzon.getUltimoError());
		modelo.setFallosConsecutivos(buzon.getFallosConsecutivos());
		modelo.setAlta(buzon.getAlta());
		if (remitentes != null) {
			modelo.setRemitentes(remitentes.stream().map(this::aModelo).toList());
		}
		return modelo;
	}

	public RemitenteAutorizadoModel aModelo(RemitenteAutorizado remitente) {
		RemitenteAutorizadoModel modelo = new RemitenteAutorizadoModel();
		modelo.setId(remitente.getId());
		modelo.setPatron(remitente.getPatron());
		modelo.setDescripcion(remitente.getDescripcion());
		modelo.setAlta(remitente.getAlta());
		return modelo;
	}

	public CorrelacionCorreoModel aModelo(CorrelacionCorreo correlacion) {
		CorrelacionCorreoModel modelo = new CorrelacionCorreoModel();
		modelo.setId(correlacion.getId());
		modelo.setToken(correlacion.getToken());
		modelo.setEtiquetaAsunto(TokenCorrelacion.etiquetar(correlacion.getToken()));
		modelo.setDireccionConEtiqueta(direccionConEtiqueta(correlacion));
		if (correlacion.getSujeto() != null) {
			modelo.setSujetoOrigen(correlacion.getSujeto().getOrigen());
			modelo.setSujetoTipoObjeto(correlacion.getSujeto().getTipoObjeto());
			modelo.setSujetoIdObjeto(correlacion.getSujeto().getIdObjeto());
		}
		modelo.setCodigoPlantilla(correlacion.getCodigoPlantilla());
		modelo.setDestinatario(correlacion.getDestinatario());
		modelo.setDescripcion(correlacion.getDescripcion());
		modelo.setVenceEn(correlacion.getVenceEn());
		modelo.setVigente(correlacion.getVenceEn() == null || correlacion.getVenceEn().isAfter(Instant.now()));
		modelo.setDocumentosRecibidos(correlacion.getDocumentosRecibidos());
		modelo.setUltimoUso(correlacion.getUltimoUso());
		modelo.setAlta(correlacion.getAlta());
		return modelo;
	}

	public MensajeCorreoModel aModelo(MensajeCorreoEntrante mensaje, List<AdjuntoCorreo> adjuntos) {
		MensajeCorreoModel modelo = new MensajeCorreoModel();
		modelo.setId(mensaje.getId());
		modelo.setBuzonId(mensaje.getBuzon() == null ? null : mensaje.getBuzon().getId());
		modelo.setBuzonDireccion(mensaje.getBuzon() == null ? null : mensaje.getBuzon().getDireccion());
		modelo.setIdentificadorMensaje(mensaje.getIdentificadorMensaje());
		modelo.setRemitente(mensaje.getRemitente());
		modelo.setDestinatarios(mensaje.getDestinatarios());
		modelo.setAsunto(mensaje.getAsunto());
		modelo.setTokenDetectado(mensaje.getTokenDetectado());
		modelo.setCorrelacionId(mensaje.getCorrelacion() == null ? null : mensaje.getCorrelacion().getId());
		modelo.setResultado(mensaje.getResultado().name());
		modelo.setMotivo(mensaje.getMotivo());
		modelo.setAdjuntos(mensaje.getAdjuntos());
		modelo.setIngestados(mensaje.getIngestados());
		modelo.setRechazados(mensaje.getRechazados());
		modelo.setEnviadoEn(mensaje.getEnviadoEn());
		modelo.setAlta(mensaje.getAlta());
		if (adjuntos != null) {
			modelo.setDetalleAdjuntos(adjuntos.stream().map(this::aModelo).toList());
		}
		return modelo;
	}

	public AdjuntoCorreoModel aModelo(AdjuntoCorreo adjunto) {
		AdjuntoCorreoModel modelo = new AdjuntoCorreoModel();
		modelo.setId(adjunto.getId());
		modelo.setDocumentoId(adjunto.getDocumento() == null ? null : adjunto.getDocumento().getId());
		modelo.setNombreArchivo(adjunto.getNombreArchivo());
		modelo.setTipoMime(adjunto.getTipoMime());
		modelo.setTamanoBytes(adjunto.getTamanoBytes());
		modelo.setSha256(adjunto.getSha256());
		modelo.setResultado(adjunto.getResultado().name());
		modelo.setCodigoRechazo(adjunto.getCodigoRechazo());
		modelo.setMotivo(adjunto.getMotivo());
		return modelo;
	}

	public MensajeSalienteModel aModelo(MensajeCorreoSaliente mensaje) {
		MensajeSalienteModel modelo = new MensajeSalienteModel();
		modelo.setId(mensaje.getId());
		modelo.setBuzonId(mensaje.getBuzon() == null ? null : mensaje.getBuzon().getId());
		modelo.setCorrelacionId(mensaje.getCorrelacion() == null ? null : mensaje.getCorrelacion().getId());
		modelo.setPlantilla(mensaje.getPlantilla().name());
		modelo.setDestinatarios(mensaje.getDestinatarios());
		modelo.setAsunto(mensaje.getAsunto());
		modelo.setIdentificadorMensaje(mensaje.getIdentificadorMensaje());
		modelo.setEstado(mensaje.getEstado().name());
		modelo.setDetalleError(mensaje.getDetalleError());
		modelo.setEnviado(mensaje.getEnviado());
		modelo.setAlta(mensaje.getAlta());
		return modelo;
	}

	private String direccionConEtiqueta(CorrelacionCorreo correlacion) {
		if (correlacion.getBuzon() == null || correlacion.getBuzon().getDireccion() == null) {
			return null;
		}
		String direccion = correlacion.getBuzon().getDireccion();
		int arroba = direccion.indexOf('@');
		if (arroba < 0) {
			return direccion;
		}
		return direccion.substring(0, arroba) + "+" + correlacion.getToken() + direccion.substring(arroba);
	}
}
