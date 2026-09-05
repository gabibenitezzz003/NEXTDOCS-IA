package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.nextdocs.ai.config.PropiedadesCatalogo;
import com.nextdocs.ai.entidades.CampoPlantilla;
import com.nextdocs.ai.entidades.PlantillaDocumental;
import com.nextdocs.ai.entidades.ReglaPlantilla;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.VersionPlantilla;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.EstadoPlantilla;
import com.nextdocs.ai.enumeraciones.EstrategiaSegmentacion;
import com.nextdocs.ai.enumeraciones.SensibilidadCampo;
import com.nextdocs.ai.repositorios.CampoPlantillaRepository;
import com.nextdocs.ai.repositorios.PlantillaDocumentalRepository;
import com.nextdocs.ai.repositorios.ReglaPlantillaRepository;
import com.nextdocs.ai.repositorios.VersionPlantillaRepository;
import com.nextdocs.ai.servicios.catalogo.CatalogoDocumentalBase;
import com.nextdocs.ai.servicios.catalogo.CatalogoDocumentalBase.CampoBase;
import com.nextdocs.ai.servicios.catalogo.CatalogoDocumentalBase.ReglaBase;
import com.nextdocs.ai.servicios.catalogo.CatalogoDocumentalBase.TipoBase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SembradorCatalogoService {

	private static final Logger log = LoggerFactory.getLogger(SembradorCatalogoService.class);

	private final PlantillaDocumentalRepository plantillaDocumentalRepository;

	private final VersionPlantillaRepository versionPlantillaRepository;

	private final CampoPlantillaRepository campoPlantillaRepository;

	private final ReglaPlantillaRepository reglaPlantillaRepository;

	private final AuditoriaService auditoriaService;

	private final PropiedadesCatalogo propiedades;

	public SembradorCatalogoService(PlantillaDocumentalRepository plantillaDocumentalRepository,
			VersionPlantillaRepository versionPlantillaRepository,
			CampoPlantillaRepository campoPlantillaRepository, ReglaPlantillaRepository reglaPlantillaRepository,
			AuditoriaService auditoriaService, PropiedadesCatalogo propiedades) {
		this.plantillaDocumentalRepository = plantillaDocumentalRepository;
		this.versionPlantillaRepository = versionPlantillaRepository;
		this.campoPlantillaRepository = campoPlantillaRepository;
		this.reglaPlantillaRepository = reglaPlantillaRepository;
		this.auditoriaService = auditoriaService;
		this.propiedades = propiedades;
	}

	@Transactional
	public List<String> sembrarSiCorresponde(Tenant tenant) {
		return propiedades.isSembrarTenantsNuevos() ? sembrar(tenant) : List.of();
	}

	@Transactional
	public List<String> sembrar(Tenant tenant) {
		List<String> creados = new ArrayList<>();
		for (TipoBase tipo : CatalogoDocumentalBase.TIPOS) {
			if (plantillaDocumentalRepository.buscarPorCodigo(tenant.getId(), tipo.codigo()).isPresent()) {
				continue;
			}
			crear(tenant, tipo);
			creados.add(tipo.codigo());
		}
		if (creados.isEmpty()) {
			return creados;
		}
		log.info("Catalogo base {} sembrado en el tenant {}: {}", CatalogoDocumentalBase.VERSION,
				tenant.getCodigo(), creados);
		auditoriaService.registrarConDetalle(tenant.getId(), AccionAuditoria.PLANTILLA_PUBLICADA,
				PlantillaService.ENTIDAD, tenant.getId(),
				Map.of("catalogo", CatalogoDocumentalBase.VERSION, "tipos", creados));
		return creados;
	}

	private void crear(Tenant tenant, TipoBase tipo) {
		Instant ahora = Instant.now();

		PlantillaDocumental plantilla = new PlantillaDocumental();
		plantilla.setTenant(tenant);
		plantilla.setCodigo(tipo.codigo());
		plantilla.setNombre(tipo.nombre());
		plantilla.setFamilia(tipo.familia());
		plantilla.setDescripcion(tipo.descripcion());
		plantilla.setAlta(ahora);
		plantillaDocumentalRepository.save(plantilla);

		VersionPlantilla version = new VersionPlantilla();
		version.setTenant(tenant);
		version.setPlantilla(plantilla);
		version.setNumero(1);
		version.setEstado(EstadoPlantilla.PUBLICADA);
		version.setUmbralAutoaprobacion(tipo.umbralAutoaprobacion());
		version.setPoliticaOriginalFisico(tipo.politicaFisica());
		version.setEstrategiaSegmentacion(EstrategiaSegmentacion.NINGUNA);
		version.setVersionPrompt(CatalogoDocumentalBase.VERSION);
		version.setVersionEsquema(CatalogoDocumentalBase.VERSION);
		version.setNotasCambio("Version del catalogo base de NEXT DOC AI. Cloná la plantilla para adaptarla.");
		version.setPublicada(ahora);
		version.setAlta(ahora);
		versionPlantillaRepository.save(version);

		int orden = 0;
		List<CampoPlantilla> campos = new ArrayList<>();
		for (CampoBase base : tipo.campos()) {
			campos.add(aCampo(version, base, orden++, ahora));
		}
		campoPlantillaRepository.saveAll(campos);

		orden = 0;
		List<ReglaPlantilla> reglas = new ArrayList<>();
		for (ReglaBase base : tipo.reglas()) {
			reglas.add(aRegla(version, base, orden++, ahora));
		}
		reglaPlantillaRepository.saveAll(reglas);

		plantilla.setVersionPublicada(version);
		plantillaDocumentalRepository.save(plantilla);
	}

	private CampoPlantilla aCampo(VersionPlantilla version, CampoBase base, int orden, Instant ahora) {
		CampoPlantilla campo = new CampoPlantilla();
		campo.setVersionPlantilla(version);
		campo.setClave(base.clave());
		campo.setEtiqueta(base.etiqueta());
		campo.setTipoDato(base.tipoDato());
		campo.setAlias(base.alias());
		campo.setRequerido(base.requerido());
		campo.setExtraer(true);
		campo.setValidar(true);
		campo.setComparar(base.requerido());
		campo.setUmbralConfianza(base.umbral());
		campo.setSensibilidad(SensibilidadCampo.INTERNA);
		campo.setOrden(orden);
		campo.setAlta(ahora);
		return campo;
	}

	private ReglaPlantilla aRegla(VersionPlantilla version, ReglaBase base, int orden, Instant ahora) {
		ReglaPlantilla regla = new ReglaPlantilla();
		regla.setVersionPlantilla(version);
		regla.setCodigo(base.codigo());
		regla.setNombre(base.nombre());
		regla.setTipo(base.tipo());
		regla.setSeveridad(base.severidad());
		regla.setCampoObjetivo(base.campoObjetivo());
		regla.setMensaje(base.mensaje());
		regla.setActiva(true);
		regla.setOrden(orden);
		regla.setAlta(ahora);
		return regla;
	}
}
