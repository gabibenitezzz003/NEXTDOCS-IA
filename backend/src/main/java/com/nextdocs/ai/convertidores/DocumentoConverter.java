package com.nextdocs.ai.convertidores;

import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.entidades.ArchivoDocumento;
import com.nextdocs.ai.entidades.CandidatoAsociacion;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.modelos.ArchivoDocumentoModel;
import com.nextdocs.ai.modelos.CandidatoAsociacionModel;
import com.nextdocs.ai.modelos.DocumentoModel;
import com.nextdocs.ai.utiles.MaquinaEstadoDocumento;

import org.springframework.stereotype.Component;

@Component
public class DocumentoConverter {

	public DocumentoModel aModelo(Documento documento, List<ArchivoDocumento> archivos) {
		DocumentoModel modelo = aModelo(documento);
		if (archivos != null) {
			for (ArchivoDocumento archivo : archivos) {
				modelo.getArchivos().add(aModelo(archivo));
			}
		}
		return modelo;
	}

	public DocumentoModel aModelo(Documento documento) {
		DocumentoModel modelo = new DocumentoModel();
		modelo.setId(documento.getId());
		modelo.setEstado(documento.getEstado());
		modelo.setOrigen(documento.getOrigen());
		modelo.setNombre(documento.getNombre());
		modelo.setHashContenido(documento.getHashContenido());
		modelo.setCorrelacionId(documento.getCorrelacionId());
		modelo.setPaginaDesde(documento.getPaginaDesde());
		modelo.setPaginaHasta(documento.getPaginaHasta());
		modelo.setCantidadSegmentos(documento.getCantidadSegmentos());
		modelo.setRemitente(documento.getRemitente());
		modelo.setObservacion(documento.getObservacion());
		modelo.setRetencionLegal(documento.isRetencionLegal());
		modelo.setRecibido(documento.getRecibido());
		modelo.setProcesado(documento.getProcesado());
		modelo.setCerrado(documento.getCerrado());
		modelo.setAlta(documento.getAlta());
		modelo.setTransicionesPosibles(new ArrayList<>(MaquinaEstadoDocumento.siguientes(documento.getEstado())));
		if (documento.getPlantilla() != null) {
			modelo.setCodigoPlantilla(documento.getPlantilla().getCodigo());
			modelo.setNombrePlantilla(documento.getPlantilla().getNombre());
		}
		if (documento.getVersionPlantilla() != null) {
			modelo.setVersionPlantillaId(documento.getVersionPlantilla().getId());
			modelo.setNumeroVersionPlantilla(documento.getVersionPlantilla().getNumero());
		}
		if (documento.getDocumentoPadre() != null) {
			modelo.setDocumentoPadreId(documento.getDocumentoPadre().getId());
		}
		if (documento.getReferenciaSujeto() != null) {
			modelo.setSujetoOrigen(documento.getReferenciaSujeto().getOrigen());
			modelo.setSujetoTipoObjeto(documento.getReferenciaSujeto().getTipoObjeto());
			modelo.setSujetoIdObjeto(documento.getReferenciaSujeto().getIdObjeto());
		}
		if (documento.getIngresadoPor() != null) {
			modelo.setIngresadoPor(documento.getIngresadoPor().getEmail());
		}
		return modelo;
	}

	public ArchivoDocumentoModel aModelo(ArchivoDocumento archivo) {
		ArchivoDocumentoModel modelo = new ArchivoDocumentoModel();
		modelo.setId(archivo.getId());
		modelo.setNombreArchivo(archivo.getNombreArchivo());
		modelo.setTipoMime(archivo.getTipoMime());
		modelo.setExtension(archivo.getExtension());
		modelo.setTamano(archivo.getTamano());
		modelo.setChecksum(archivo.getChecksum());
		modelo.setPaginas(archivo.getPaginas());
		modelo.setVersion(archivo.getVersion());
		modelo.setOriginal(archivo.isOriginal());
		modelo.setAlta(archivo.getAlta());
		return modelo;
	}

	public CandidatoAsociacionModel aModelo(CandidatoAsociacion candidato) {
		CandidatoAsociacionModel modelo = new CandidatoAsociacionModel();
		modelo.setId(candidato.getId());
		modelo.setConector(candidato.getConector());
		modelo.setDescripcion(candidato.getDescripcion());
		modelo.setPuntaje(candidato.getPuntaje());
		modelo.setRazones(candidato.getRazones());
		modelo.setSeleccionado(candidato.isSeleccionado());
		modelo.setDescartado(candidato.isDescartado());
		modelo.setMotivoSeleccion(candidato.getMotivoSeleccion());
		if (candidato.getSeleccionadoPor() != null) {
			modelo.setSeleccionadoPor(candidato.getSeleccionadoPor().getEmail());
		}
		if (candidato.getReferencia() != null) {
			modelo.setOrigen(candidato.getReferencia().getOrigen());
			modelo.setTipoObjeto(candidato.getReferencia().getTipoObjeto());
			modelo.setIdObjeto(candidato.getReferencia().getIdObjeto());
		}
		return modelo;
	}

	public List<CandidatoAsociacionModel> aModelosCandidatos(List<CandidatoAsociacion> candidatos) {
		List<CandidatoAsociacionModel> modelos = new ArrayList<>();
		for (CandidatoAsociacion candidato : candidatos) {
			modelos.add(aModelo(candidato));
		}
		return modelos;
	}

	public List<DocumentoModel> aModelos(List<Documento> documentos) {
		List<DocumentoModel> modelos = new ArrayList<>();
		for (Documento documento : documentos) {
			modelos.add(aModelo(documento));
		}
		return modelos;
	}
}
