package com.nextdocs.ai.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdocs.ai.entidades.CampoPlantilla;
import com.nextdocs.ai.entidades.CasoPruebaPlantilla;
import com.nextdocs.ai.entidades.ConjuntoPruebaPlantilla;
import com.nextdocs.ai.entidades.EjecucionPruebaPlantilla;
import com.nextdocs.ai.entidades.PlantillaDocumental;
import com.nextdocs.ai.entidades.ResultadoCasoPrueba;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.VersionPlantilla;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.EstadoPlantilla;
import com.nextdocs.ai.enumeraciones.PresenciaCampo;
import com.nextdocs.ai.enumeraciones.ResultadoQualityGate;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.interfaces.ProveedorDocumentalIaInt;
import com.nextdocs.ai.modelos.CampoEsquemaModel;
import com.nextdocs.ai.modelos.CasoPruebaModel;
import com.nextdocs.ai.modelos.ConjuntoPruebaModel;
import com.nextdocs.ai.modelos.EjecucionPruebaModel;
import com.nextdocs.ai.modelos.PoliticaQualityGateReqModel;
import com.nextdocs.ai.modelos.ResultadoCasoPruebaModel;
import com.nextdocs.ai.modelos.ResultadoExtraccionModel;
import com.nextdocs.ai.modelos.SolicitudExtraccionModel;
import com.nextdocs.ai.modelos.ValorCanonicoModel;
import com.nextdocs.ai.repositorios.CampoPlantillaRepository;
import com.nextdocs.ai.repositorios.CasoPruebaPlantillaRepository;
import com.nextdocs.ai.repositorios.ConjuntoPruebaPlantillaRepository;
import com.nextdocs.ai.repositorios.EjecucionPruebaPlantillaRepository;
import com.nextdocs.ai.repositorios.PlantillaDocumentalRepository;
import com.nextdocs.ai.repositorios.ResultadoCasoPruebaRepository;
import com.nextdocs.ai.repositorios.VersionPlantillaRepository;
import com.nextdocs.ai.servicios.proveedores.NormalizadorValor;
import com.nextdocs.ai.servicios.proveedores.RuteadorProveedorService;
import com.nextdocs.ai.utiles.Hash;
import com.nextdocs.ai.utiles.MaquinaEstadoPlantilla;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class QualityGateService {

	public static final String ENTIDAD_CONJUNTO = "ConjuntoPruebaPlantilla";

	public static final String ENTIDAD_CASO = "CasoPruebaPlantilla";

	public static final String ENTIDAD_EJECUCION = "EjecucionPruebaPlantilla";

	public static final BigDecimal UMBRAL_POR_DEFECTO = new BigDecimal("0.8000");

	private static final BigDecimal FACTOR_MINIMO = new BigDecimal("0.1000");

	private static final BigDecimal FACTOR_MAXIMO = BigDecimal.ONE;

	private static final long TAMANO_MAXIMO = 20L * 1024 * 1024;

	private final PlantillaDocumentalRepository plantillaDocumentalRepository;

	private final VersionPlantillaRepository versionPlantillaRepository;

	private final CampoPlantillaRepository campoPlantillaRepository;

	private final ConjuntoPruebaPlantillaRepository conjuntoPruebaPlantillaRepository;

	private final CasoPruebaPlantillaRepository casoPruebaPlantillaRepository;

	private final EjecucionPruebaPlantillaRepository ejecucionPruebaPlantillaRepository;

	private final ResultadoCasoPruebaRepository resultadoCasoPruebaRepository;

	private final AlmacenamientoService almacenamientoService;

	private final RuteadorProveedorService ruteadorProveedorService;

	private final AuditoriaService auditoriaService;

	private final ObjectMapper objectMapper;

	public QualityGateService(PlantillaDocumentalRepository plantillaDocumentalRepository,
			VersionPlantillaRepository versionPlantillaRepository, CampoPlantillaRepository campoPlantillaRepository,
			ConjuntoPruebaPlantillaRepository conjuntoPruebaPlantillaRepository,
			CasoPruebaPlantillaRepository casoPruebaPlantillaRepository,
			EjecucionPruebaPlantillaRepository ejecucionPruebaPlantillaRepository,
			ResultadoCasoPruebaRepository resultadoCasoPruebaRepository, AlmacenamientoService almacenamientoService,
			RuteadorProveedorService ruteadorProveedorService, AuditoriaService auditoriaService,
			ObjectMapper objectMapper) {
		this.plantillaDocumentalRepository = plantillaDocumentalRepository;
		this.versionPlantillaRepository = versionPlantillaRepository;
		this.campoPlantillaRepository = campoPlantillaRepository;
		this.conjuntoPruebaPlantillaRepository = conjuntoPruebaPlantillaRepository;
		this.casoPruebaPlantillaRepository = casoPruebaPlantillaRepository;
		this.ejecucionPruebaPlantillaRepository = ejecucionPruebaPlantillaRepository;
		this.resultadoCasoPruebaRepository = resultadoCasoPruebaRepository;
		this.almacenamientoService = almacenamientoService;
		this.ruteadorProveedorService = ruteadorProveedorService;
		this.auditoriaService = auditoriaService;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public ConjuntoPruebaModel obtenerConjunto(String tenantId, String plantillaId) {
		PlantillaDocumental plantilla = exigirPlantilla(tenantId, plantillaId);
		return aModelo(plantilla, conjuntoPruebaPlantillaRepository.buscarActivo(tenantId, plantillaId).orElse(null));
	}

	@Transactional
	public PlantillaDocumental actualizarPolitica(String tenantId, String plantillaId,
			PoliticaQualityGateReqModel datos) {
		PlantillaDocumental plantilla = exigirPlantilla(tenantId, plantillaId);
		plantilla.setExigirQualityGate(datos.isExigirQualityGate());
		plantilla.setUmbralQualityGate(umbralDe(datos.getUmbralMinimo()));
		plantillaDocumentalRepository.save(plantilla);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.POLITICA_MODIFICADA, "PlantillaDocumental",
				plantillaId, Map.of("exigirQualityGate", plantilla.isExigirQualityGate(), "umbralQualityGate",
						plantilla.getUmbralQualityGate().toPlainString()));
		return plantilla;
	}

	@Transactional
	public CasoPruebaModel agregarCaso(String tenantId, String plantillaId, String nombre, String esperado,
			MultipartFile archivo) {
		PlantillaDocumental plantilla = exigirPlantilla(tenantId, plantillaId);
		validarEsperado(esperado);
		validarArchivo(archivo);
		ConjuntoPruebaPlantilla conjunto = obtenerOCrear(plantilla);
		CasoPruebaPlantilla caso = new CasoPruebaPlantilla();
		caso.setTenant(plantilla.getTenant());
		caso.setConjunto(conjunto);
		caso.setNombre(nombre == null || nombre.isBlank() ? archivo.getOriginalFilename() : nombre.trim());
		caso.setEsperado(esperado);
		caso.setNombreArchivo(archivo.getOriginalFilename());
		caso.setTipoMime(archivo.getContentType() == null ? "application/octet-stream" : archivo.getContentType());
		caso.setClaveObjeto(claveObjeto(tenantId, conjunto.getId(), UUID.randomUUID().toString()));
		caso.setOrden((int) casoPruebaPlantillaRepository.contarActivos(conjunto.getId()) + 1);
		caso.setAlta(Instant.now());
		try {
			almacenamientoService.guardarDocumento(caso.getClaveObjeto(), archivo.getBytes(), caso.getTipoMime());
		} catch (Exception e) {
			throw new ValidacionException("No se pudo guardar el archivo del caso gold");
		}
		casoPruebaPlantillaRepository.save(caso);
		recalcularHuella(conjunto);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.CONJUNTO_PRUEBA_MODIFICADO, ENTIDAD_CASO,
				caso.getId(), Map.of("plantillaId", plantillaId, "nombre", caso.getNombre()));
		return aModelo(caso);
	}

	@Transactional
	public void eliminarCaso(String tenantId, String casoId) {
		CasoPruebaPlantilla caso = casoPruebaPlantillaRepository.buscarPorIdYTenant(casoId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(ENTIDAD_CASO, casoId));
		caso.setBaja(Instant.now());
		casoPruebaPlantillaRepository.save(caso);
		recalcularHuella(caso.getConjunto());
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.CONJUNTO_PRUEBA_MODIFICADO, ENTIDAD_CASO,
				casoId, Map.of("baja", true));
	}

	@Transactional
	public EjecucionPruebaModel ejecutar(String tenantId, String versionId) {
		VersionPlantilla version = versionPlantillaRepository.buscarPorIdYTenant(versionId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de("VersionPlantilla", versionId));
		MaquinaEstadoPlantilla.exigirEditable(version.getEstado());
		PlantillaDocumental plantilla = version.getPlantilla();
		ConjuntoPruebaPlantilla conjunto = conjuntoPruebaPlantillaRepository
				.buscarActivo(tenantId, plantilla.getId())
				.orElseThrow(() -> new ValidacionException("La plantilla no tiene un conjunto gold"));
		List<CasoPruebaPlantilla> casos = casoPruebaPlantillaRepository.listarActivos(conjunto.getId());
		if (casos.isEmpty()) {
			throw new ValidacionException("El conjunto gold no tiene casos");
		}
		recalcularHuella(conjunto);

		List<CampoPlantilla> campos = campoPlantillaRepository.listarPorVersion(versionId);
		ProveedorDocumentalIaInt proveedor = ruteadorProveedorService.resolverPrincipal(tenantId);

		int aciertosTotales = 0;
		int evaluadosTotales = 0;
		BigDecimal sumaConfianza = BigDecimal.ZERO;
		int conConfianza = 0;
		List<ResultadoCasoPrueba> resultados = new ArrayList<>();

		for (CasoPruebaPlantilla caso : casos) {
			ResultadoExtraccionModel extraccion = extraer(proveedor, version, plantilla, campos, caso);
			Puntaje puntaje = puntuar(caso.getEsperado(), extraccion, campos);
			aciertosTotales += puntaje.aciertos;
			evaluadosTotales += puntaje.total;
			sumaConfianza = sumaConfianza.add(puntaje.sumaConfianza);
			conConfianza += puntaje.conConfianza;
			ResultadoCasoPrueba resultado = new ResultadoCasoPrueba();
			resultado.setCaso(caso);
			resultado.setAciertos(puntaje.aciertos);
			resultado.setTotal(puntaje.total);
			resultado.setExactitud(razon(puntaje.aciertos, puntaje.total));
			resultado.setExtraido(puntaje.extraido);
			resultados.add(resultado);
		}

		BigDecimal exactitud = razon(aciertosTotales, evaluadosTotales);
		BigDecimal referencia = exactitudPublicada(plantilla, conjunto.getHuella());
		BigDecimal umbral = umbralDe(plantilla.getUmbralQualityGate());
		BigDecimal factor = calibrar(exactitud, sumaConfianza, conConfianza);
		String motivo = evaluar(exactitud, umbral, referencia);
		ResultadoQualityGate resultado = motivo == null ? ResultadoQualityGate.APROBADO
				: ResultadoQualityGate.RECHAZADO;

		EjecucionPruebaPlantilla ejecucion = new EjecucionPruebaPlantilla();
		ejecucion.setTenant(version.getTenant());
		ejecucion.setVersionPlantilla(version);
		ejecucion.setConjunto(conjunto);
		ejecucion.setHuellaConjunto(conjunto.getHuella());
		ejecucion.setExactitud(exactitud);
		ejecucion.setExactitudReferencia(referencia);
		ejecucion.setFactorCalibracion(factor);
		ejecucion.setResultado(resultado);
		ejecucion.setMotivo(motivo);
		ejecucion.setAlta(Instant.now());
		ejecucionPruebaPlantillaRepository.save(ejecucion);
		for (ResultadoCasoPrueba fila : resultados) {
			fila.setEjecucion(ejecucion);
		}
		resultadoCasoPruebaRepository.saveAll(resultados);

		if (resultado == ResultadoQualityGate.APROBADO) {
			version.setFactorCalibracionConfianza(factor);
		}
		if (version.getEstado() == EstadoPlantilla.BORRADOR) {
			version.setEstado(EstadoPlantilla.EN_PRUEBA);
		}
		versionPlantillaRepository.save(version);

		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.QUALITY_GATE_EJECUTADO, ENTIDAD_EJECUCION,
				ejecucion.getId(), Map.of("versionId", versionId, "resultado", resultado.name(), "exactitud",
						exactitud.toPlainString(), "huella", conjunto.getHuella()));
		return aModelo(ejecucion, resultados);
	}

	@Transactional(readOnly = true)
	public List<EjecucionPruebaModel> listarEjecuciones(String tenantId, String versionId) {
		versionPlantillaRepository.buscarPorIdYTenant(versionId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de("VersionPlantilla", versionId));
		List<EjecucionPruebaModel> modelos = new ArrayList<>();
		for (EjecucionPruebaPlantilla ejecucion : ejecucionPruebaPlantillaRepository.listarPorVersion(versionId)) {
			modelos.add(aModelo(ejecucion, resultadoCasoPruebaRepository.listarPorEjecucion(ejecucion.getId())));
		}
		return modelos;
	}

	@Transactional(readOnly = true)
	public void exigirAprobado(String tenantId, VersionPlantilla version) {
		PlantillaDocumental plantilla = version.getPlantilla();
		if (plantilla == null || !plantilla.isExigirQualityGate()) {
			return;
		}
		ConjuntoPruebaPlantilla conjunto = conjuntoPruebaPlantillaRepository
				.buscarActivo(tenantId, plantilla.getId()).orElse(null);
		if (conjunto == null || casoPruebaPlantillaRepository.contarActivos(conjunto.getId()) == 0) {
			throw new ValidacionException(
					"GOV-04: no se puede publicar una version sin casos gold cuando el quality gate esta exigido");
		}
		String huella = huellaDe(casoPruebaPlantillaRepository.listarActivos(conjunto.getId()));
		List<EjecucionPruebaPlantilla> aprobadas = ejecucionPruebaPlantillaRepository.listarAprobadas(version.getId(),
				huella, ResultadoQualityGate.APROBADO);
		if (aprobadas.isEmpty()) {
			throw new ValidacionException(
					"GOV-04: la version no tiene una corrida gold aprobada para el conjunto vigente");
		}
	}

	private ResultadoExtraccionModel extraer(ProveedorDocumentalIaInt proveedor, VersionPlantilla version,
			PlantillaDocumental plantilla, List<CampoPlantilla> campos, CasoPruebaPlantilla caso) {
		SolicitudExtraccionModel solicitud = new SolicitudExtraccionModel();
		solicitud.setTenantId(plantilla.getTenant().getId());
		solicitud.setDocumentoId("gold-" + caso.getId());
		solicitud.setNombreArchivo(caso.getNombreArchivo());
		solicitud.setTipoMime(caso.getTipoMime());
		solicitud.setContenido(almacenamientoService.leerDocumento(caso.getClaveObjeto()));
		solicitud.setPaginas(1);
		solicitud.setCodigoPlantilla(plantilla.getCodigo());
		solicitud.setInstruccionExtraccion(version.getInstruccionExtraccion());
		solicitud.setVersionPrompt(version.getVersionPrompt());
		solicitud.setVersionEsquema(version.getVersionEsquema());
		solicitud.setCampos(esquema(campos));
		return proveedor.extraer(solicitud);
	}

	private Puntaje puntuar(String esperadoJson, ResultadoExtraccionModel extraccion, List<CampoPlantilla> campos) {
		Puntaje puntaje = new Puntaje();
		Map<String, ValorCanonicoModel> extraidos = new LinkedHashMap<>();
		if (extraccion != null) {
			for (ValorCanonicoModel valor : extraccion.getValores()) {
				extraidos.put(valor.getClaveCampo(), valor);
			}
		}
		try {
			JsonNode esperado = objectMapper.readTree(esperadoJson);
			Iterator<String> claves = esperado.fieldNames();
			Map<String, Object> volcado = new LinkedHashMap<>();
			while (claves.hasNext()) {
				String clave = claves.next();
				JsonNode nodo = esperado.get(clave);
				PresenciaCampo presenciaEsperada = PresenciaCampo.desde(
						nodo.hasNonNull("presencia") ? nodo.get("presencia").asText() : PresenciaCampo.PRESENTE.name());
				String valorEsperado = nodo.hasNonNull("valor") ? nodo.get("valor").asText() : null;
				ValorCanonicoModel obtenido = extraidos.get(clave);
				puntaje.total++;
				boolean acierto = coincide(presenciaEsperada, valorEsperado, obtenido, tipoDe(campos, clave));
				if (acierto) {
					puntaje.aciertos++;
				}
				if (obtenido != null && obtenido.getConfianzaProveedor() != null) {
					puntaje.sumaConfianza = puntaje.sumaConfianza.add(obtenido.getConfianzaProveedor());
					puntaje.conConfianza++;
				}
				Map<String, Object> fila = new LinkedHashMap<>();
				fila.put("esperado", valorEsperado);
				fila.put("presenciaEsperada", presenciaEsperada);
				fila.put("acierto", acierto);
				if (obtenido != null) {
					fila.put("obtenido", obtenido.getValorNormalizado());
					fila.put("presencia", obtenido.getPresencia());
					fila.put("confianzaProveedor", obtenido.getConfianzaProveedor());
				}
				volcado.put(clave, fila);
			}
			puntaje.extraido = objectMapper.writeValueAsString(volcado);
		} catch (ValidacionException e) {
			throw e;
		} catch (Exception e) {
			throw new ValidacionException("No se pudo comparar el caso gold con la extraccion");
		}
		return puntaje;
	}

	private boolean coincide(PresenciaCampo esperada, String valorEsperado, ValorCanonicoModel obtenido,
			com.nextdocs.ai.enumeraciones.TipoDatoCampo tipo) {
		if (obtenido == null) {
			return false;
		}
		if (esperada != obtenido.getPresencia()) {
			return false;
		}
		if (esperada != PresenciaCampo.PRESENTE) {
			return true;
		}
		String esperado = NormalizadorValor.normalizar(valorEsperado, tipo);
		String actual = obtenido.getValorNormalizado() == null
				? NormalizadorValor.normalizar(obtenido.getValorCrudo(), tipo)
				: obtenido.getValorNormalizado();
		if (esperado == null && actual == null) {
			return true;
		}
		if (esperado == null || actual == null) {
			return false;
		}
		return esperado.equalsIgnoreCase(actual.trim());
	}

	private String evaluar(BigDecimal exactitud, BigDecimal umbral, BigDecimal referencia) {
		if (referencia != null && exactitud.compareTo(referencia) < 0) {
			return "La version empeora el gold: " + exactitud.toPlainString() + " < " + referencia.toPlainString();
		}
		if (exactitud.compareTo(umbral) < 0) {
			return "La exactitud " + exactitud.toPlainString() + " no alcanza el umbral " + umbral.toPlainString();
		}
		return null;
	}

	private BigDecimal exactitudPublicada(PlantillaDocumental plantilla, String huella) {
		if (plantilla.getVersionPublicada() == null) {
			return null;
		}
		List<EjecucionPruebaPlantilla> aprobadas = ejecucionPruebaPlantillaRepository.listarAprobadas(
				plantilla.getVersionPublicada().getId(), huella, ResultadoQualityGate.APROBADO);
		if (aprobadas.isEmpty()) {
			return null;
		}
		return aprobadas.get(0).getExactitud();
	}

	private BigDecimal calibrar(BigDecimal exactitud, BigDecimal sumaConfianza, int conConfianza) {
		if (conConfianza == 0) {
			return BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
		}
		BigDecimal media = sumaConfianza.divide(BigDecimal.valueOf(conConfianza), 6, RoundingMode.HALF_UP);
		if (media.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
		}
		BigDecimal factor = exactitud.divide(media, 4, RoundingMode.HALF_UP);
		return factor.max(FACTOR_MINIMO).min(FACTOR_MAXIMO);
	}

	private ConjuntoPruebaPlantilla obtenerOCrear(PlantillaDocumental plantilla) {
		return conjuntoPruebaPlantillaRepository.buscarActivo(plantilla.getTenant().getId(), plantilla.getId())
				.orElseGet(() -> {
					ConjuntoPruebaPlantilla conjunto = new ConjuntoPruebaPlantilla();
					conjunto.setTenant(plantilla.getTenant());
					conjunto.setPlantilla(plantilla);
					conjunto.setAlta(Instant.now());
					return conjuntoPruebaPlantillaRepository.save(conjunto);
				});
	}

	private void recalcularHuella(ConjuntoPruebaPlantilla conjunto) {
		conjunto.setHuella(huellaDe(casoPruebaPlantillaRepository.listarActivos(conjunto.getId())));
		conjuntoPruebaPlantillaRepository.save(conjunto);
	}

	private String huellaDe(List<CasoPruebaPlantilla> casos) {
		TreeMap<String, String> ordenado = new TreeMap<>();
		for (CasoPruebaPlantilla caso : casos) {
			ordenado.put(caso.getId(), caso.getEsperado() == null ? "" : caso.getEsperado());
		}
		StringBuilder material = new StringBuilder();
		for (Map.Entry<String, String> entrada : ordenado.entrySet()) {
			material.append(entrada.getKey()).append('=').append(entrada.getValue()).append('\n');
		}
		return Hash.sha256(material.toString());
	}

	private void validarEsperado(String esperado) {
		if (esperado == null || esperado.isBlank()) {
			throw new ValidacionException("El esperado del caso gold es obligatorio");
		}
		try {
			JsonNode nodo = objectMapper.readTree(esperado);
			if (!nodo.isObject() || nodo.isEmpty()) {
				throw new ValidacionException("El esperado debe ser un objeto JSON con al menos un campo");
			}
		} catch (ValidacionException e) {
			throw e;
		} catch (Exception e) {
			throw new ValidacionException("El esperado del caso gold no es un JSON valido");
		}
	}

	private void validarArchivo(MultipartFile archivo) {
		if (archivo == null || archivo.isEmpty()) {
			throw new ValidacionException("Hay que adjuntar el archivo del caso gold");
		}
		if (archivo.getSize() > TAMANO_MAXIMO) {
			throw new ValidacionException("El archivo del caso gold supera los 20 MB");
		}
	}

	private PlantillaDocumental exigirPlantilla(String tenantId, String plantillaId) {
		return plantillaDocumentalRepository.buscarPorIdYTenant(plantillaId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de("PlantillaDocumental", plantillaId));
	}

	private List<CampoEsquemaModel> esquema(List<CampoPlantilla> campos) {
		List<CampoEsquemaModel> esquema = new ArrayList<>();
		for (CampoPlantilla campo : campos) {
			if (!campo.isExtraer()) {
				continue;
			}
			CampoEsquemaModel modelo = new CampoEsquemaModel();
			modelo.setClave(campo.getClave());
			modelo.setEtiqueta(campo.getEtiqueta());
			modelo.setTipoDato(campo.getTipoDato());
			modelo.setDescripcion(campo.getDescripcion());
			modelo.setAlias(campo.getAlias());
			modelo.setRequerido(campo.isRequerido());
			modelo.setFormatoFecha(campo.getFormatoFecha());
			modelo.setExpresionRegular(campo.getExpresionRegular());
			modelo.setUmbralConfianza(campo.getUmbralConfianza());
			esquema.add(modelo);
		}
		return esquema;
	}

	private com.nextdocs.ai.enumeraciones.TipoDatoCampo tipoDe(List<CampoPlantilla> campos, String clave) {
		for (CampoPlantilla campo : campos) {
			if (clave.equals(campo.getClave())) {
				return campo.getTipoDato();
			}
		}
		return null;
	}

	private BigDecimal umbralDe(BigDecimal umbral) {
		if (umbral == null) {
			return UMBRAL_POR_DEFECTO;
		}
		if (umbral.compareTo(BigDecimal.ZERO) < 0 || umbral.compareTo(BigDecimal.ONE) > 0) {
			throw new ValidacionException("El umbral del quality gate debe estar entre 0 y 1");
		}
		return umbral.setScale(4, RoundingMode.HALF_UP);
	}

	private BigDecimal razon(int aciertos, int total) {
		if (total <= 0) {
			return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
		}
		return BigDecimal.valueOf(aciertos).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
	}

	private String claveObjeto(String tenantId, String conjuntoId, String casoId) {
		return "gold/" + tenantId + "/" + conjuntoId + "/" + casoId;
	}

	private ConjuntoPruebaModel aModelo(PlantillaDocumental plantilla, ConjuntoPruebaPlantilla conjunto) {
		ConjuntoPruebaModel modelo = new ConjuntoPruebaModel();
		modelo.setPlantillaId(plantilla.getId());
		modelo.setExigirQualityGate(plantilla.isExigirQualityGate());
		modelo.setUmbralMinimo(umbralDe(plantilla.getUmbralQualityGate()));
		if (conjunto == null) {
			return modelo;
		}
		modelo.setId(conjunto.getId());
		modelo.setHuella(conjunto.getHuella());
		modelo.setAlta(conjunto.getAlta());
		List<CasoPruebaPlantilla> casos = casoPruebaPlantillaRepository.listarActivos(conjunto.getId());
		modelo.setCantidadCasos(casos.size());
		for (CasoPruebaPlantilla caso : casos) {
			modelo.getCasos().add(aModelo(caso));
		}
		return modelo;
	}

	private CasoPruebaModel aModelo(CasoPruebaPlantilla caso) {
		CasoPruebaModel modelo = new CasoPruebaModel();
		modelo.setId(caso.getId());
		modelo.setNombre(caso.getNombre());
		modelo.setNombreArchivo(caso.getNombreArchivo());
		modelo.setTipoMime(caso.getTipoMime());
		modelo.setEsperado(caso.getEsperado());
		modelo.setOrden(caso.getOrden());
		modelo.setAlta(caso.getAlta());
		return modelo;
	}

	private EjecucionPruebaModel aModelo(EjecucionPruebaPlantilla ejecucion, List<ResultadoCasoPrueba> resultados) {
		EjecucionPruebaModel modelo = new EjecucionPruebaModel();
		modelo.setId(ejecucion.getId());
		if (ejecucion.getVersionPlantilla() != null) {
			modelo.setVersionId(ejecucion.getVersionPlantilla().getId());
			modelo.setNumeroVersion(ejecucion.getVersionPlantilla().getNumero());
		}
		modelo.setHuellaConjunto(ejecucion.getHuellaConjunto());
		modelo.setExactitud(ejecucion.getExactitud());
		modelo.setExactitudReferencia(ejecucion.getExactitudReferencia());
		modelo.setFactorCalibracion(ejecucion.getFactorCalibracion());
		modelo.setResultado(ejecucion.getResultado());
		modelo.setMotivo(ejecucion.getMotivo());
		modelo.setAlta(ejecucion.getAlta());
		if (resultados != null) {
			for (ResultadoCasoPrueba resultado : resultados) {
				ResultadoCasoPruebaModel fila = new ResultadoCasoPruebaModel();
				if (resultado.getCaso() != null) {
					fila.setCasoId(resultado.getCaso().getId());
					fila.setNombre(resultado.getCaso().getNombre());
				}
				fila.setAciertos(resultado.getAciertos());
				fila.setTotal(resultado.getTotal());
				fila.setExactitud(resultado.getExactitud());
				fila.setExtraido(resultado.getExtraido());
				modelo.getCasos().add(fila);
			}
		}
		return modelo;
	}

	private static final class Puntaje {
		private int aciertos;
		private int total;
		private BigDecimal sumaConfianza = BigDecimal.ZERO;
		private int conConfianza;
		private String extraido;
	}
}
