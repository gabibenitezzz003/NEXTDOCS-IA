package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.nextdocs.ai.convertidores.PlantillaConverter;
import com.nextdocs.ai.entidades.CampoPlantilla;
import com.nextdocs.ai.entidades.PlantillaDocumental;
import com.nextdocs.ai.entidades.ReglaPlantilla;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.entidades.VersionPlantilla;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.EstadoPlantilla;
import com.nextdocs.ai.enumeraciones.EstrategiaSegmentacion;
import com.nextdocs.ai.enumeraciones.PoliticaOriginalFisico;
import com.nextdocs.ai.enumeraciones.TipoEventoCanonico;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.RegistroExistenteException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.CampoPlantillaModel;
import com.nextdocs.ai.modelos.CampoPlantillaReqModel;
import com.nextdocs.ai.modelos.PlantillaModel;
import com.nextdocs.ai.modelos.PlantillaReqModel;
import com.nextdocs.ai.modelos.ReglaPlantillaModel;
import com.nextdocs.ai.modelos.ReglaPlantillaReqModel;
import com.nextdocs.ai.modelos.VersionPlantillaModel;
import com.nextdocs.ai.modelos.VersionPlantillaReqModel;
import com.nextdocs.ai.repositorios.CampoPlantillaRepository;
import com.nextdocs.ai.repositorios.PlantillaDocumentalRepository;
import com.nextdocs.ai.repositorios.ReglaPlantillaRepository;
import com.nextdocs.ai.repositorios.VersionPlantillaRepository;
import com.nextdocs.ai.utiles.MaquinaEstadoPlantilla;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlantillaService {

	public static final String ENTIDAD = "PlantillaDocumental";

	public static final String ENTIDAD_VERSION = "VersionPlantilla";

	public static final String ENTIDAD_CAMPO = "CampoPlantilla";

	public static final String ENTIDAD_REGLA = "ReglaPlantilla";

	private final PlantillaDocumentalRepository plantillaDocumentalRepository;

	private final VersionPlantillaRepository versionPlantillaRepository;

	private final CampoPlantillaRepository campoPlantillaRepository;

	private final ReglaPlantillaRepository reglaPlantillaRepository;

	private final ValidadorPlantillaService validadorPlantillaService;

	private final AuditoriaService auditoriaService;

	private final EventoSalidaService eventoSalidaService;

	private final QualityGateService qualityGateService;

	private final PlantillaConverter plantillaConverter;

	public PlantillaService(PlantillaDocumentalRepository plantillaDocumentalRepository,
			VersionPlantillaRepository versionPlantillaRepository,
			CampoPlantillaRepository campoPlantillaRepository, ReglaPlantillaRepository reglaPlantillaRepository,
			ValidadorPlantillaService validadorPlantillaService, AuditoriaService auditoriaService,
			EventoSalidaService eventoSalidaService, QualityGateService qualityGateService,
			PlantillaConverter plantillaConverter) {
		this.plantillaDocumentalRepository = plantillaDocumentalRepository;
		this.versionPlantillaRepository = versionPlantillaRepository;
		this.campoPlantillaRepository = campoPlantillaRepository;
		this.reglaPlantillaRepository = reglaPlantillaRepository;
		this.validadorPlantillaService = validadorPlantillaService;
		this.auditoriaService = auditoriaService;
		this.eventoSalidaService = eventoSalidaService;
		this.qualityGateService = qualityGateService;
		this.plantillaConverter = plantillaConverter;
	}

	@Transactional(readOnly = true)
	public List<PlantillaModel> listar(String tenantId, String familia) {
		return plantillaConverter.aModelos(plantillaDocumentalRepository.listarPorTenant(tenantId, familia));
	}

	@Transactional(readOnly = true)
	public List<String> listarFamilias(String tenantId) {
		return plantillaDocumentalRepository.listarFamilias(tenantId);
	}

	@Transactional(readOnly = true)
	public PlantillaModel obtener(String tenantId, String plantillaId) {
		PlantillaDocumental plantilla = buscarEntidad(tenantId, plantillaId);
		return plantillaConverter.aModelo(plantilla, versionPlantillaRepository.listarPorPlantilla(plantillaId));
	}

	@Transactional(readOnly = true)
	public VersionPlantillaModel obtenerVersion(String tenantId, String versionId) {
		VersionPlantilla version = buscarVersion(tenantId, versionId);
		return plantillaConverter.aModelo(version, campoPlantillaRepository.listarPorVersion(versionId),
				reglaPlantillaRepository.listarPorVersion(versionId));
	}

	@Transactional
	public PlantillaModel crear(Tenant tenant, Usuario usuario, PlantillaReqModel datos) {
		if (plantillaDocumentalRepository.buscarPorCodigo(tenant.getId(), datos.getCodigo()).isPresent()) {
			throw new RegistroExistenteException("Ya existe una plantilla con el codigo " + datos.getCodigo());
		}
		PlantillaDocumental plantilla = new PlantillaDocumental();
		plantilla.setTenant(tenant);
		plantilla.setCodigo(datos.getCodigo());
		plantilla.setNombre(datos.getNombre());
		plantilla.setFamilia(datos.getFamilia());
		plantilla.setDescripcion(datos.getDescripcion());
		plantilla.setUmbralQualityGate(QualityGateService.UMBRAL_POR_DEFECTO);
		plantilla.setCreadoPor(usuario);
		plantilla.setAlta(Instant.now());
		plantillaDocumentalRepository.save(plantilla);

		VersionPlantilla primera = construirVersion(tenant, plantilla, 1);
		primera.setPoliticaOriginalFisico(PoliticaOriginalFisico.NO_REQUIERE);
		primera.setEstrategiaSegmentacion(EstrategiaSegmentacion.NINGUNA);
		versionPlantillaRepository.save(primera);

		auditoriaService.registrar(tenant.getId(), AccionAuditoria.PLANTILLA_CREADA, ENTIDAD, plantilla.getId());
		return plantillaConverter.aModelo(plantilla, List.of(primera));
	}

	@Transactional
	public PlantillaModel actualizar(String tenantId, String plantillaId, PlantillaReqModel datos) {
		PlantillaDocumental plantilla = buscarEntidad(tenantId, plantillaId);
		if (!plantilla.getCodigo().equals(datos.getCodigo())
				&& plantillaDocumentalRepository.buscarPorCodigo(tenantId, datos.getCodigo()).isPresent()) {
			throw new RegistroExistenteException("Ya existe una plantilla con el codigo " + datos.getCodigo());
		}
		plantilla.setCodigo(datos.getCodigo());
		plantilla.setNombre(datos.getNombre());
		plantilla.setFamilia(datos.getFamilia());
		plantilla.setDescripcion(datos.getDescripcion());
		plantillaDocumentalRepository.save(plantilla);
		return plantillaConverter.aModelo(plantilla, versionPlantillaRepository.listarPorPlantilla(plantillaId));
	}

	@Transactional
	public void eliminar(String tenantId, String plantillaId) {
		PlantillaDocumental plantilla = buscarEntidad(tenantId, plantillaId);
		plantilla.setBaja(Instant.now());
		plantillaDocumentalRepository.save(plantilla);
		auditoriaService.registrar(tenantId, AccionAuditoria.PLANTILLA_DEPRECADA, ENTIDAD, plantillaId);
	}

	@Transactional
	public VersionPlantillaModel crearVersion(Tenant tenant, String plantillaId, VersionPlantillaReqModel datos) {
		PlantillaDocumental plantilla = buscarEntidad(tenant.getId(), plantillaId);
		int numero = versionPlantillaRepository.ultimoNumero(plantillaId) + 1;
		VersionPlantilla version = construirVersion(tenant, plantilla, numero);
		aplicarDatos(version, datos);
		versionPlantillaRepository.save(version);

		if (datos.getVersionBaseId() != null && !datos.getVersionBaseId().isBlank()) {
			clonarContenido(tenant, buscarVersion(tenant.getId(), datos.getVersionBaseId()), version);
		}

		auditoriaService.registrarConDetalle(tenant.getId(), AccionAuditoria.PLANTILLA_VERSIONADA, ENTIDAD_VERSION,
				version.getId(), Map.of("plantillaId", plantillaId, "numero", numero, "clonadaDe",
						datos.getVersionBaseId() == null ? "" : datos.getVersionBaseId()));
		return obtenerVersion(tenant.getId(), version.getId());
	}

	@Transactional
	public VersionPlantillaModel actualizarVersion(String tenantId, String versionId,
			VersionPlantillaReqModel datos) {
		VersionPlantilla version = buscarVersion(tenantId, versionId);
		MaquinaEstadoPlantilla.exigirEditable(version.getEstado());
		aplicarDatos(version, datos);
		versionPlantillaRepository.save(version);
		return obtenerVersion(tenantId, versionId);
	}

	@Transactional
	public VersionPlantillaModel cambiarEstadoVersion(String tenantId, String versionId, EstadoPlantilla destino) {
		VersionPlantilla version = buscarVersion(tenantId, versionId);
		MaquinaEstadoPlantilla.validar(version.getEstado(), destino);
		if (destino == EstadoPlantilla.PUBLICADA) {
			throw new ValidacionException("Para publicar una version use el endpoint de publicacion");
		}
		version.setEstado(destino);
		versionPlantillaRepository.save(version);
		return obtenerVersion(tenantId, versionId);
	}

	@Transactional
	public VersionPlantillaModel publicar(String tenantId, String versionId, Usuario usuario) {
		VersionPlantilla version = buscarVersion(tenantId, versionId);
		MaquinaEstadoPlantilla.validar(version.getEstado(), EstadoPlantilla.PUBLICADA);

		List<CampoPlantilla> campos = campoPlantillaRepository.listarPorVersion(versionId);
		List<ReglaPlantilla> reglas = reglaPlantillaRepository.listarPorVersion(versionId);
		List<String> errores = validadorPlantillaService.validar(version, campos, reglas);
		if (!errores.isEmpty()) {
			throw new ValidacionException("La version no se puede publicar: " + String.join("; ", errores));
		}
		qualityGateService.exigirAprobado(tenantId, version);

		PlantillaDocumental plantilla = version.getPlantilla();
		deprecarVersionPublicadaActual(plantilla, versionId);

		version.setEstado(EstadoPlantilla.PUBLICADA);
		version.setPublicadaPor(usuario);
		version.setPublicada(Instant.now());
		versionPlantillaRepository.save(version);

		plantilla.setVersionPublicada(version);
		plantillaDocumentalRepository.save(plantilla);

		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.PLANTILLA_PUBLICADA, ENTIDAD_VERSION,
				versionId, Map.of("plantilla", plantilla.getCodigo(), "numero", version.getNumero(), "campos",
						campos.size(), "reglas", reglas.size()));
		eventoSalidaService.publicar(tenantId, TipoEventoCanonico.PLANTILLA_PUBLICADA, ENTIDAD_VERSION, versionId,
				Map.of("plantillaId", plantilla.getId(), "codigo", plantilla.getCodigo(), "numero",
						version.getNumero()));
		return obtenerVersion(tenantId, versionId);
	}

	@Transactional
	public VersionPlantillaModel revertir(String tenantId, String plantillaId, String versionId, Usuario usuario) {
		PlantillaDocumental plantilla = buscarEntidad(tenantId, plantillaId);
		VersionPlantilla objetivo = buscarVersion(tenantId, versionId);
		if (objetivo.getPlantilla() == null || !plantillaId.equals(objetivo.getPlantilla().getId())) {
			throw new ValidacionException("La version no pertenece a la plantilla indicada");
		}
		if (objetivo.getEstado() != EstadoPlantilla.DEPRECADA) {
			throw new ValidacionException("Solo se puede revertir a una version previamente publicada");
		}
		if (plantilla.getVersionPublicada() != null && versionId.equals(plantilla.getVersionPublicada().getId())) {
			throw new ValidacionException("La version ya es la publicada");
		}

		deprecarVersionPublicadaActual(plantilla, versionId);
		objetivo.setEstado(EstadoPlantilla.PUBLICADA);
		objetivo.setPublicadaPor(usuario);
		objetivo.setPublicada(Instant.now());
		versionPlantillaRepository.save(objetivo);

		plantilla.setVersionPublicada(objetivo);
		plantillaDocumentalRepository.save(plantilla);

		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.PLANTILLA_REVERTIDA, ENTIDAD_VERSION,
				versionId, Map.of("plantilla", plantilla.getCodigo(), "numero", objetivo.getNumero()));
		return obtenerVersion(tenantId, versionId);
	}

	@Transactional
	public CampoPlantillaModel agregarCampo(String tenantId, String versionId, CampoPlantillaReqModel datos) {
		VersionPlantilla version = buscarVersion(tenantId, versionId);
		MaquinaEstadoPlantilla.exigirEditable(version.getEstado());
		if (campoPlantillaRepository.buscarPorClave(versionId, datos.getClave()).isPresent()) {
			throw new RegistroExistenteException("La version ya tiene un campo con la clave " + datos.getClave());
		}
		CampoPlantilla campo = new CampoPlantilla();
		campo.setVersionPlantilla(version);
		campo.setAlta(Instant.now());
		aplicarDatos(campo, datos);
		campoPlantillaRepository.save(campo);
		return plantillaConverter.aModelo(campo);
	}

	@Transactional
	public CampoPlantillaModel actualizarCampo(String tenantId, String versionId, String campoId,
			CampoPlantillaReqModel datos) {
		VersionPlantilla version = buscarVersion(tenantId, versionId);
		MaquinaEstadoPlantilla.exigirEditable(version.getEstado());
		CampoPlantilla campo = buscarCampo(versionId, campoId);
		if (!campo.getClave().equals(datos.getClave())
				&& campoPlantillaRepository.buscarPorClave(versionId, datos.getClave()).isPresent()) {
			throw new RegistroExistenteException("La version ya tiene un campo con la clave " + datos.getClave());
		}
		aplicarDatos(campo, datos);
		campoPlantillaRepository.save(campo);
		return plantillaConverter.aModelo(campo);
	}

	@Transactional
	public void eliminarCampo(String tenantId, String versionId, String campoId) {
		VersionPlantilla version = buscarVersion(tenantId, versionId);
		MaquinaEstadoPlantilla.exigirEditable(version.getEstado());
		campoPlantillaRepository.delete(buscarCampo(versionId, campoId));
	}

	@Transactional
	public ReglaPlantillaModel agregarRegla(String tenantId, String versionId, ReglaPlantillaReqModel datos) {
		VersionPlantilla version = buscarVersion(tenantId, versionId);
		MaquinaEstadoPlantilla.exigirEditable(version.getEstado());
		if (existeRegla(versionId, datos.getCodigo(), null)) {
			throw new RegistroExistenteException("La version ya tiene una regla con el codigo " + datos.getCodigo());
		}
		ReglaPlantilla regla = new ReglaPlantilla();
		regla.setVersionPlantilla(version);
		regla.setAlta(Instant.now());
		aplicarDatos(regla, datos);
		reglaPlantillaRepository.save(regla);
		return plantillaConverter.aModelo(regla);
	}

	@Transactional
	public ReglaPlantillaModel actualizarRegla(String tenantId, String versionId, String reglaId,
			ReglaPlantillaReqModel datos) {
		VersionPlantilla version = buscarVersion(tenantId, versionId);
		MaquinaEstadoPlantilla.exigirEditable(version.getEstado());
		ReglaPlantilla regla = buscarRegla(versionId, reglaId);
		if (existeRegla(versionId, datos.getCodigo(), reglaId)) {
			throw new RegistroExistenteException("La version ya tiene una regla con el codigo " + datos.getCodigo());
		}
		aplicarDatos(regla, datos);
		reglaPlantillaRepository.save(regla);
		return plantillaConverter.aModelo(regla);
	}

	@Transactional
	public void eliminarRegla(String tenantId, String versionId, String reglaId) {
		VersionPlantilla version = buscarVersion(tenantId, versionId);
		MaquinaEstadoPlantilla.exigirEditable(version.getEstado());
		reglaPlantillaRepository.delete(buscarRegla(versionId, reglaId));
	}

	@Transactional(readOnly = true)
	public List<String> validarVersion(String tenantId, String versionId) {
		VersionPlantilla version = buscarVersion(tenantId, versionId);
		return validadorPlantillaService.validar(version, campoPlantillaRepository.listarPorVersion(versionId),
				reglaPlantillaRepository.listarPorVersion(versionId));
	}

	@Transactional(readOnly = true)
	public PlantillaDocumental buscarEntidad(String tenantId, String plantillaId) {
		return plantillaDocumentalRepository.buscarPorIdYTenant(plantillaId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(ENTIDAD, plantillaId));
	}

	@Transactional(readOnly = true)
	public VersionPlantilla buscarVersion(String tenantId, String versionId) {
		return versionPlantillaRepository.buscarPorIdYTenant(versionId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(ENTIDAD_VERSION, versionId));
	}

	private void deprecarVersionPublicadaActual(PlantillaDocumental plantilla, String versionIdEntrante) {
		for (VersionPlantilla candidata : versionPlantillaRepository.listarPorEstado(plantilla.getId(),
				EstadoPlantilla.PUBLICADA)) {
			if (candidata.getId().equals(versionIdEntrante)) {
				continue;
			}
			candidata.setEstado(EstadoPlantilla.DEPRECADA);
			versionPlantillaRepository.save(candidata);
		}
	}

	private VersionPlantilla construirVersion(Tenant tenant, PlantillaDocumental plantilla, int numero) {
		VersionPlantilla version = new VersionPlantilla();
		version.setTenant(tenant);
		version.setPlantilla(plantilla);
		version.setNumero(numero);
		version.setEstado(EstadoPlantilla.BORRADOR);
		version.setEstrategiaSegmentacion(EstrategiaSegmentacion.NINGUNA);
		version.setVersionPrompt("p" + numero);
		version.setVersionEsquema("e" + numero);
		version.setAlta(Instant.now());
		return version;
	}

	private void clonarContenido(Tenant tenant, VersionPlantilla base, VersionPlantilla destino) {
		if (base.getPlantilla() == null || destino.getPlantilla() == null
				|| !base.getPlantilla().getId().equals(destino.getPlantilla().getId())) {
			throw new ValidacionException("La version base debe pertenecer a la misma plantilla");
		}
		List<CampoPlantilla> campos = new ArrayList<>();
		for (CampoPlantilla origen : campoPlantillaRepository.listarPorVersion(base.getId())) {
			CampoPlantilla copia = new CampoPlantilla();
			copia.setVersionPlantilla(destino);
			copia.setClave(origen.getClave());
			copia.setEtiqueta(origen.getEtiqueta());
			copia.setTipoDato(origen.getTipoDato());
			copia.setAlias(origen.getAlias());
			copia.setDescripcion(origen.getDescripcion());
			copia.setRequerido(origen.isRequerido());
			copia.setExtraer(origen.isExtraer());
			copia.setValidar(origen.isValidar());
			copia.setComparar(origen.isComparar());
			copia.setUnico(origen.isUnico());
			copia.setExpresionRegular(origen.getExpresionRegular());
			copia.setFormatoFecha(origen.getFormatoFecha());
			copia.setCatalogoReferencia(origen.getCatalogoReferencia());
			copia.setUmbralConfianza(origen.getUmbralConfianza());
			copia.setSensibilidad(origen.getSensibilidad());
			copia.setOrden(origen.getOrden());
			copia.setAlta(Instant.now());
			campos.add(copia);
		}
		campoPlantillaRepository.saveAll(campos);

		List<ReglaPlantilla> reglas = new ArrayList<>();
		for (ReglaPlantilla origen : reglaPlantillaRepository.listarPorVersion(base.getId())) {
			ReglaPlantilla copia = new ReglaPlantilla();
			copia.setVersionPlantilla(destino);
			copia.setCodigo(origen.getCodigo());
			copia.setNombre(origen.getNombre());
			copia.setTipo(origen.getTipo());
			copia.setSeveridad(origen.getSeveridad());
			copia.setCampoObjetivo(origen.getCampoObjetivo());
			copia.setConfiguracion(origen.getConfiguracion());
			copia.setMensaje(origen.getMensaje());
			copia.setActiva(origen.isActiva());
			copia.setOrden(origen.getOrden());
			copia.setAlta(Instant.now());
			reglas.add(copia);
		}
		reglaPlantillaRepository.saveAll(reglas);

		if (destino.getUmbralAutoaprobacion() == null) {
			destino.setUmbralAutoaprobacion(base.getUmbralAutoaprobacion());
		}
		if (destino.getPoliticaOriginalFisico() == null) {
			destino.setPoliticaOriginalFisico(base.getPoliticaOriginalFisico());
		}
		if (destino.getInstruccionExtraccion() == null) {
			destino.setInstruccionExtraccion(base.getInstruccionExtraccion());
		}
		if (destino.getEstrategiaSegmentacion() == null
				|| destino.getEstrategiaSegmentacion() == EstrategiaSegmentacion.NINGUNA) {
			destino.setEstrategiaSegmentacion(base.getEstrategiaSegmentacion());
			destino.setPaginasPorDocumento(base.getPaginasPorDocumento());
			destino.setPatronInicioDocumento(base.getPatronInicioDocumento());
		}
		versionPlantillaRepository.save(destino);
	}

	private void aplicarDatos(VersionPlantilla version, VersionPlantillaReqModel datos) {
		if (datos.getUmbralAutoaprobacion() != null) {
			version.setUmbralAutoaprobacion(datos.getUmbralAutoaprobacion());
		}
		if (datos.getPoliticaOriginalFisico() != null) {
			version.setPoliticaOriginalFisico(datos.getPoliticaOriginalFisico());
		}
		if (datos.getVersionPrompt() != null) {
			version.setVersionPrompt(datos.getVersionPrompt());
		}
		if (datos.getVersionEsquema() != null) {
			version.setVersionEsquema(datos.getVersionEsquema());
		}
		if (datos.getEstrategiaSegmentacion() != null) {
			version.setEstrategiaSegmentacion(datos.getEstrategiaSegmentacion());
		}
		if (datos.getPaginasPorDocumento() != null) {
			version.setPaginasPorDocumento(datos.getPaginasPorDocumento());
		}
		if (datos.getPatronInicioDocumento() != null) {
			version.setPatronInicioDocumento(datos.getPatronInicioDocumento());
		}
		if (datos.getInstruccionExtraccion() != null) {
			version.setInstruccionExtraccion(datos.getInstruccionExtraccion());
		}
		if (datos.getNotasCambio() != null) {
			version.setNotasCambio(datos.getNotasCambio());
		}
	}

	private void aplicarDatos(CampoPlantilla campo, CampoPlantillaReqModel datos) {
		campo.setClave(datos.getClave());
		campo.setEtiqueta(datos.getEtiqueta());
		campo.setTipoDato(datos.getTipoDato());
		campo.setAlias(datos.getAlias());
		campo.setDescripcion(datos.getDescripcion());
		campo.setRequerido(datos.isRequerido());
		campo.setExtraer(datos.isExtraer());
		campo.setValidar(datos.isValidar());
		campo.setComparar(datos.isComparar());
		campo.setUnico(datos.isUnico());
		campo.setExpresionRegular(datos.getExpresionRegular());
		campo.setFormatoFecha(datos.getFormatoFecha());
		campo.setCatalogoReferencia(datos.getCatalogoReferencia());
		campo.setUmbralConfianza(datos.getUmbralConfianza());
		campo.setSensibilidad(datos.getSensibilidad());
		campo.setOrden(datos.getOrden());
	}

	private void aplicarDatos(ReglaPlantilla regla, ReglaPlantillaReqModel datos) {
		regla.setCodigo(datos.getCodigo());
		regla.setNombre(datos.getNombre());
		regla.setTipo(datos.getTipo());
		regla.setSeveridad(datos.getSeveridad());
		regla.setCampoObjetivo(datos.getCampoObjetivo());
		regla.setConfiguracion(datos.getConfiguracion());
		regla.setMensaje(datos.getMensaje());
		regla.setActiva(datos.isActiva());
		regla.setOrden(datos.getOrden());
	}

	private CampoPlantilla buscarCampo(String versionId, String campoId) {
		CampoPlantilla campo = campoPlantillaRepository.findById(campoId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(ENTIDAD_CAMPO, campoId));
		if (campo.getVersionPlantilla() == null || !versionId.equals(campo.getVersionPlantilla().getId())) {
			throw EntidadNoEncontradaException.de(ENTIDAD_CAMPO, campoId);
		}
		return campo;
	}

	private ReglaPlantilla buscarRegla(String versionId, String reglaId) {
		ReglaPlantilla regla = reglaPlantillaRepository.findById(reglaId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(ENTIDAD_REGLA, reglaId));
		if (regla.getVersionPlantilla() == null || !versionId.equals(regla.getVersionPlantilla().getId())) {
			throw EntidadNoEncontradaException.de(ENTIDAD_REGLA, reglaId);
		}
		return regla;
	}

	private boolean existeRegla(String versionId, String codigo, String reglaIdExcluida) {
		for (ReglaPlantilla regla : reglaPlantillaRepository.listarPorVersion(versionId)) {
			if (regla.getCodigo().equals(codigo) && !regla.getId().equals(reglaIdExcluida)) {
				return true;
			}
		}
		return false;
	}
}
