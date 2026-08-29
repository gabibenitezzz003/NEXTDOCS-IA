package com.nextdocs.ai.convertidores;

import java.time.Instant;
import java.util.List;

import com.nextdocs.ai.entidades.ItemExportacion;
import com.nextdocs.ai.entidades.LoteExportacion;
import com.nextdocs.ai.enumeraciones.EstadoLoteExportacion;
import com.nextdocs.ai.modelos.ItemExportacionModel;
import com.nextdocs.ai.modelos.LoteExportacionModel;

import org.springframework.stereotype.Component;

@Component
public class ExportacionConverter {

	public LoteExportacionModel aModelo(LoteExportacion lote, List<ItemExportacion> items) {
		LoteExportacionModel modelo = new LoteExportacionModel();
		modelo.setId(lote.getId());
		modelo.setEstado(lote.getEstado().name());
		modelo.setNombre(lote.getNombre());
		modelo.setFiltros(lote.getFiltros());
		modelo.setOrden(lote.getOrden());
		modelo.setIncluirOriginales(lote.isIncluirOriginales());
		modelo.setCantidadDocumentos(lote.getCantidadDocumentos());
		modelo.setCantidadOmitidos(lote.getCantidadOmitidos());
		modelo.setNombreArchivo(lote.getNombreArchivo());
		modelo.setTamanoBytes(lote.getTamanoBytes());
		modelo.setSha256(lote.getSha256());
		modelo.setDetalleError(lote.getDetalleError());
		modelo.setSolicitadoPor(lote.getSolicitadoPor() == null ? null : lote.getSolicitadoPor().getEmail());
		modelo.setVenceEn(lote.getVenceEn());
		modelo.setGenerado(lote.getGenerado());
		modelo.setAlta(lote.getAlta());
		modelo.setDescargable(esDescargable(lote));
		if (items != null) {
			modelo.setItems(items.stream().map(this::aModelo).toList());
		}
		return modelo;
	}

	public ItemExportacionModel aModelo(ItemExportacion item) {
		ItemExportacionModel modelo = new ItemExportacionModel();
		modelo.setId(item.getId());
		modelo.setDocumentoId(item.getDocumento() == null ? null : item.getDocumento().getId());
		modelo.setTipoObjeto(item.getTipoObjeto());
		modelo.setIdObjeto(item.getIdObjeto());
		modelo.setCodigoPlantilla(item.getCodigoPlantilla());
		modelo.setNumeroVersionPlantilla(item.getNumeroVersionPlantilla());
		modelo.setEstadoDocumento(item.getEstadoDocumento() == null ? null : item.getEstadoDocumento().name());
		modelo.setNombreEnArchivo(item.getNombreEnArchivo());
		modelo.setTamanoBytes(item.getTamanoBytes());
		modelo.setSha256(item.getSha256());
		modelo.setHallazgos(item.getHallazgos());
		modelo.setMotivoOmision(item.getMotivoOmision());
		modelo.setRecibido(item.getRecibido());
		modelo.setCerrado(item.getCerrado());
		return modelo;
	}

	public static boolean esDescargable(LoteExportacion lote) {
		return lote.getEstado() == EstadoLoteExportacion.DISPONIBLE && lote.getClaveObjeto() != null
				&& (lote.getVenceEn() == null || lote.getVenceEn().isAfter(Instant.now()));
	}
}
