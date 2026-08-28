package com.nextdocs.ai.convertidores;

import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.entidades.CampoPlantilla;
import com.nextdocs.ai.entidades.PlantillaDocumental;
import com.nextdocs.ai.entidades.ReglaPlantilla;
import com.nextdocs.ai.entidades.VersionPlantilla;
import com.nextdocs.ai.modelos.CampoPlantillaModel;
import com.nextdocs.ai.modelos.PlantillaModel;
import com.nextdocs.ai.modelos.ReglaPlantillaModel;
import com.nextdocs.ai.modelos.VersionPlantillaModel;
import com.nextdocs.ai.utiles.MaquinaEstadoPlantilla;

import org.springframework.stereotype.Component;

@Component
public class PlantillaConverter {

	public PlantillaModel aModelo(PlantillaDocumental plantilla) {
		PlantillaModel modelo = new PlantillaModel();
		modelo.setId(plantilla.getId());
		modelo.setCodigo(plantilla.getCodigo());
		modelo.setNombre(plantilla.getNombre());
		modelo.setFamilia(plantilla.getFamilia());
		modelo.setDescripcion(plantilla.getDescripcion());
		modelo.setAlta(plantilla.getAlta());
		modelo.setBloqueoOptimista(plantilla.getBloqueoOptimista());
		if (plantilla.getVersionPublicada() != null) {
			modelo.setVersionPublicadaId(plantilla.getVersionPublicada().getId());
			modelo.setNumeroVersionPublicada(plantilla.getVersionPublicada().getNumero());
		}
		if (plantilla.getCreadoPor() != null) {
			modelo.setCreadoPor(plantilla.getCreadoPor().getEmail());
		}
		return modelo;
	}

	public PlantillaModel aModelo(PlantillaDocumental plantilla, List<VersionPlantilla> versiones) {
		PlantillaModel modelo = aModelo(plantilla);
		if (versiones != null) {
			modelo.setCantidadVersiones(versiones.size());
			for (VersionPlantilla version : versiones) {
				modelo.getVersiones().add(aModelo(version, null, null));
			}
		}
		return modelo;
	}

	public VersionPlantillaModel aModelo(VersionPlantilla version, List<CampoPlantilla> campos,
			List<ReglaPlantilla> reglas) {
		VersionPlantillaModel modelo = new VersionPlantillaModel();
		modelo.setId(version.getId());
		modelo.setNumero(version.getNumero());
		modelo.setEstado(version.getEstado());
		modelo.setEditable(MaquinaEstadoPlantilla.esEditable(version.getEstado()));
		modelo.setUmbralAutoaprobacion(version.getUmbralAutoaprobacion());
		modelo.setPoliticaOriginalFisico(version.getPoliticaOriginalFisico());
		modelo.setVersionPrompt(version.getVersionPrompt());
		modelo.setVersionEsquema(version.getVersionEsquema());
		modelo.setInstruccionExtraccion(version.getInstruccionExtraccion());
		modelo.setNotasCambio(version.getNotasCambio());
		modelo.setPublicada(version.getPublicada());
		modelo.setAlta(version.getAlta());
		modelo.setBloqueoOptimista(version.getBloqueoOptimista());
		modelo.setTransicionesPosibles(new ArrayList<>(MaquinaEstadoPlantilla.siguientes(version.getEstado())));
		if (version.getPlantilla() != null) {
			modelo.setPlantillaId(version.getPlantilla().getId());
			modelo.setCodigoPlantilla(version.getPlantilla().getCodigo());
			modelo.setEsVersionPublicada(version.getPlantilla().getVersionPublicada() != null
					&& version.getId().equals(version.getPlantilla().getVersionPublicada().getId()));
		}
		if (version.getPublicadaPor() != null) {
			modelo.setPublicadaPor(version.getPublicadaPor().getEmail());
		}
		if (campos != null) {
			for (CampoPlantilla campo : campos) {
				modelo.getCampos().add(aModelo(campo));
			}
		}
		if (reglas != null) {
			for (ReglaPlantilla regla : reglas) {
				modelo.getReglas().add(aModelo(regla));
			}
		}
		return modelo;
	}

	public CampoPlantillaModel aModelo(CampoPlantilla campo) {
		CampoPlantillaModel modelo = new CampoPlantillaModel();
		modelo.setId(campo.getId());
		modelo.setClave(campo.getClave());
		modelo.setEtiqueta(campo.getEtiqueta());
		modelo.setTipoDato(campo.getTipoDato());
		modelo.setAlias(campo.getAlias());
		modelo.setDescripcion(campo.getDescripcion());
		modelo.setRequerido(campo.isRequerido());
		modelo.setExtraer(campo.isExtraer());
		modelo.setValidar(campo.isValidar());
		modelo.setComparar(campo.isComparar());
		modelo.setUnico(campo.isUnico());
		modelo.setExpresionRegular(campo.getExpresionRegular());
		modelo.setFormatoFecha(campo.getFormatoFecha());
		modelo.setCatalogoReferencia(campo.getCatalogoReferencia());
		modelo.setUmbralConfianza(campo.getUmbralConfianza());
		modelo.setSensibilidad(campo.getSensibilidad());
		modelo.setOrden(campo.getOrden());
		return modelo;
	}

	public ReglaPlantillaModel aModelo(ReglaPlantilla regla) {
		ReglaPlantillaModel modelo = new ReglaPlantillaModel();
		modelo.setId(regla.getId());
		modelo.setCodigo(regla.getCodigo());
		modelo.setNombre(regla.getNombre());
		modelo.setTipo(regla.getTipo());
		modelo.setSeveridad(regla.getSeveridad());
		modelo.setCampoObjetivo(regla.getCampoObjetivo());
		modelo.setConfiguracion(regla.getConfiguracion());
		modelo.setMensaje(regla.getMensaje());
		modelo.setActiva(regla.isActiva());
		modelo.setOrden(regla.getOrden());
		return modelo;
	}

	public List<PlantillaModel> aModelos(List<PlantillaDocumental> plantillas) {
		List<PlantillaModel> modelos = new ArrayList<>();
		for (PlantillaDocumental plantilla : plantillas) {
			modelos.add(aModelo(plantilla));
		}
		return modelos;
	}

	public List<VersionPlantillaModel> aModelosVersion(List<VersionPlantilla> versiones) {
		List<VersionPlantillaModel> modelos = new ArrayList<>();
		for (VersionPlantilla version : versiones) {
			modelos.add(aModelo(version, null, null));
		}
		return modelos;
	}
}
