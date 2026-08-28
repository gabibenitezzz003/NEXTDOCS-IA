package com.nextdocs.ai.convertidores;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.nextdocs.ai.entidades.CampoPlantilla;
import com.nextdocs.ai.entidades.EjecucionExtraccion;
import com.nextdocs.ai.entidades.EjecucionValidacion;
import com.nextdocs.ai.entidades.HallazgoValidacion;
import com.nextdocs.ai.entidades.ValorExtraido;
import com.nextdocs.ai.modelos.EjecucionExtraccionModel;
import com.nextdocs.ai.modelos.EjecucionValidacionModel;
import com.nextdocs.ai.modelos.HallazgoValidacionModel;
import com.nextdocs.ai.modelos.ValorExtraidoModel;

import org.springframework.stereotype.Component;

@Component
public class ExtraccionConverter {

	public EjecucionExtraccionModel aModelo(EjecucionExtraccion ejecucion, List<ValorExtraido> valores,
			Map<String, CampoPlantilla> camposPorClave) {
		EjecucionExtraccionModel modelo = new EjecucionExtraccionModel();
		modelo.setId(ejecucion.getId());
		modelo.setProveedor(ejecucion.getProveedor());
		modelo.setModelo(ejecucion.getModelo());
		modelo.setVersionPrompt(ejecucion.getVersionPrompt());
		modelo.setVersionEsquema(ejecucion.getVersionEsquema());
		modelo.setEstado(ejecucion.getEstado());
		modelo.setIntento(ejecucion.getIntento());
		modelo.setTokensEntrada(ejecucion.getTokensEntrada());
		modelo.setTokensSalida(ejecucion.getTokensSalida());
		modelo.setPaginasProcesadas(ejecucion.getPaginasProcesadas());
		modelo.setCosto(ejecucion.getCosto());
		modelo.setDuracionMilisegundos(ejecucion.getDuracionMilisegundos());
		modelo.setCodigoError(ejecucion.getCodigoError());
		modelo.setMensajeError(ejecucion.getMensajeError());
		modelo.setInicio(ejecucion.getInicio());
		modelo.setFin(ejecucion.getFin());
		if (valores != null) {
			for (ValorExtraido valor : valores) {
				modelo.getValores().add(aModelo(valor, camposPorClave));
			}
		}
		return modelo;
	}

	public ValorExtraidoModel aModelo(ValorExtraido valor, Map<String, CampoPlantilla> camposPorClave) {
		ValorExtraidoModel modelo = new ValorExtraidoModel();
		modelo.setId(valor.getId());
		modelo.setClaveCampo(valor.getClaveCampo());
		modelo.setValorCrudo(valor.getValorCrudo());
		modelo.setValorNormalizado(valor.getValorNormalizado());
		modelo.setPresencia(valor.getPresencia());
		modelo.setConfianza(valor.getConfianza());
		modelo.setConfianzaProveedor(valor.getConfianzaProveedor());
		modelo.setEvidenciaPagina(valor.getEvidenciaPagina());
		modelo.setEvidenciaRecuadro(valor.getEvidenciaRecuadro());
		modelo.setCorregidoManualmente(valor.isCorregidoManualmente());
		modelo.setValorAnterior(valor.getValorAnterior());
		modelo.setAnonimizado(valor.isAnonimizado());
		if (camposPorClave != null && camposPorClave.containsKey(valor.getClaveCampo())) {
			modelo.setEtiqueta(camposPorClave.get(valor.getClaveCampo()).getEtiqueta());
		}
		return modelo;
	}

	public EjecucionValidacionModel aModelo(EjecucionValidacion ejecucion, List<HallazgoValidacion> hallazgos) {
		EjecucionValidacionModel modelo = new EjecucionValidacionModel();
		modelo.setId(ejecucion.getId());
		modelo.setEstado(ejecucion.getEstado());
		modelo.setResultado(ejecucion.getResultado());
		modelo.setCantidadHallazgos(ejecucion.getCantidadHallazgos());
		modelo.setCantidadBloqueantes(ejecucion.getCantidadBloqueantes());
		modelo.setAutoaprobado(ejecucion.isAutoaprobado());
		modelo.setMotivoResultado(ejecucion.getMotivoResultado());
		modelo.setInicio(ejecucion.getInicio());
		modelo.setFin(ejecucion.getFin());
		if (hallazgos != null) {
			for (HallazgoValidacion hallazgo : hallazgos) {
				modelo.getHallazgos().add(aModelo(hallazgo));
			}
		}
		return modelo;
	}

	public HallazgoValidacionModel aModelo(HallazgoValidacion hallazgo) {
		HallazgoValidacionModel modelo = new HallazgoValidacionModel();
		modelo.setId(hallazgo.getId());
		modelo.setCodigoRegla(hallazgo.getCodigoRegla());
		modelo.setClaveCampo(hallazgo.getClaveCampo());
		modelo.setSeveridad(hallazgo.getSeveridad());
		modelo.setMensaje(hallazgo.getMensaje());
		modelo.setEvidencia(hallazgo.getEvidencia());
		modelo.setSobreescrito(hallazgo.isSobreescrito());
		modelo.setMotivoSobreescritura(hallazgo.getMotivoSobreescritura());
		modelo.setAlta(hallazgo.getAlta());
		if (hallazgo.getSobreescritoPor() != null) {
			modelo.setSobreescritoPor(hallazgo.getSobreescritoPor().getEmail());
		}
		return modelo;
	}

	public List<EjecucionExtraccionModel> aModelos(List<EjecucionExtraccion> ejecuciones) {
		List<EjecucionExtraccionModel> modelos = new ArrayList<>();
		for (EjecucionExtraccion ejecucion : ejecuciones) {
			modelos.add(aModelo(ejecucion, null, null));
		}
		return modelos;
	}
}
