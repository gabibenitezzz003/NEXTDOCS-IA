package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.nextdocs.ai.convertidores.AdministracionConverter;
import com.nextdocs.ai.entidades.CuentaServicio;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.TipoActor;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.CuentaServicioCreadaModel;
import com.nextdocs.ai.modelos.CuentaServicioModel;
import com.nextdocs.ai.modelos.CuentaServicioReqModel;
import com.nextdocs.ai.modelos.PrincipalNextDocs;
import com.nextdocs.ai.repositorios.CuentaServicioRepository;
import com.nextdocs.ai.utiles.Hash;
import com.nextdocs.ai.utiles.Permiso;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CuentaServicioService {

	public static final String ENTIDAD = "CuentaServicio";

	public static final String CABECERA_CLAVE = "X-Clave-Servicio";

	private static final String PREFIJO = "ndai";

	private static final int BYTES_CLAVE = 32;

	private static final Logger log = LoggerFactory.getLogger(CuentaServicioService.class);

	private final CuentaServicioRepository cuentaServicioRepository;

	private final AuditoriaService auditoriaService;

	private final AdministracionConverter administracionConverter;

	private final SecureRandom aleatorio = new SecureRandom();

	public CuentaServicioService(CuentaServicioRepository cuentaServicioRepository,
			AuditoriaService auditoriaService, AdministracionConverter administracionConverter) {
		this.cuentaServicioRepository = cuentaServicioRepository;
		this.auditoriaService = auditoriaService;
		this.administracionConverter = administracionConverter;
	}

	@Transactional
	public CuentaServicioCreadaModel crear(Tenant tenant, CuentaServicioReqModel datos) {
		byte[] material = new byte[BYTES_CLAVE];
		aleatorio.nextBytes(material);
		String claveEnClaro = PREFIJO + "_" + Base64.getUrlEncoder().withoutPadding().encodeToString(material);
		CuentaServicio cuenta = new CuentaServicio();
		cuenta.setTenant(tenant);
		cuenta.setNombre(datos.getNombre().trim());
		cuenta.setPrefijoClave(claveEnClaro.substring(0, 12));
		cuenta.setClaveHash(Hash.sha256(claveEnClaro));
		cuenta.setAlcances(validarAlcances(datos.getAlcances()));
		cuenta.setActiva(true);
		if (datos.getDiasVigencia() > 0) {
			cuenta.setExpira(Instant.now().plus(datos.getDiasVigencia(), ChronoUnit.DAYS));
		}
		cuenta.setAlta(Instant.now());
		cuentaServicioRepository.save(cuenta);
		auditoriaService.registrarConDetalle(tenant.getId(), AccionAuditoria.CUENTA_SERVICIO_CREADA, ENTIDAD,
				cuenta.getId(), Map.of("nombre", cuenta.getNombre(), "alcances",
						new ArrayList<>(cuenta.getAlcances()), "expira", String.valueOf(cuenta.getExpira())));
		log.info("Alta de la cuenta de servicio {} en el tenant {}", cuenta.getNombre(), tenant.getCodigo());
		CuentaServicioCreadaModel creada = new CuentaServicioCreadaModel();
		creada.setCuenta(administracionConverter.aModelo(cuenta));
		creada.setClave(claveEnClaro);
		creada.setAdvertencia("Esta clave se muestra una sola vez. En base solo queda su hash");
		return creada;
	}

	@Transactional(readOnly = true)
	public List<CuentaServicioModel> listarModelos(String tenantId) {
		return administracionConverter.aModelosCuenta(cuentaServicioRepository.listarPorTenant(tenantId));
	}

	@Transactional(readOnly = true)
	public CuentaServicioModel obtener(String tenantId, String cuentaId) {
		return administracionConverter.aModelo(cuentaServicioRepository.buscarPorIdYTenant(cuentaId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(ENTIDAD, cuentaId)));
	}

	private Set<String> validarAlcances(List<String> alcances) {
		Set<String> validos = Permiso.todos();
		Set<String> resultado = new LinkedHashSet<>();
		for (String alcance : alcances) {
			String limpio = alcance == null ? "" : alcance.trim();
			if (!validos.contains(limpio)) {
				throw new ValidacionException("El alcance " + alcance + " no es un permiso valido");
			}
			resultado.add(limpio);
		}
		if (resultado.contains(Permiso.TENANT_ADMINISTRAR)) {
			throw new ValidacionException(
					"Una cuenta de servicio no puede administrar el tenant. Ese alcance es solo para usuarios");
		}
		return resultado;
	}

	@Transactional(readOnly = true)
	public Optional<PrincipalNextDocs> autenticar(String claveEnClaro) {
		if (claveEnClaro == null || claveEnClaro.isBlank()) {
			return Optional.empty();
		}
		Optional<CuentaServicio> encontrada = cuentaServicioRepository
				.findByClaveHashAndBajaIsNull(Hash.sha256(claveEnClaro.trim()));
		if (encontrada.isEmpty()) {
			return Optional.empty();
		}
		CuentaServicio cuenta = encontrada.get();
		if (!cuenta.isActiva() || estaExpirada(cuenta)) {
			return Optional.empty();
		}
		PrincipalNextDocs principal = new PrincipalNextDocs();
		principal.setIdActor(cuenta.getId());
		principal.setTipoActor(TipoActor.CUENTA_SERVICIO);
		principal.setTenantId(cuenta.getTenant().getId());
		principal.setCodigoTenant(cuenta.getTenant().getCodigo());
		principal.setDescripcion(cuenta.getNombre());
		principal.setPermisos(new LinkedHashSet<>(cuenta.getAlcances()));
		return Optional.of(principal);
	}

	@Transactional
	public void registrarUso(String cuentaId) {
		cuentaServicioRepository.findById(cuentaId).ifPresent(cuenta -> {
			cuenta.setUltimoUso(Instant.now());
			cuentaServicioRepository.save(cuenta);
		});
	}

	@Transactional
	public CuentaServicioModel revocar(String tenantId, String cuentaId, String motivo) {
		if (motivo == null || motivo.isBlank()) {
			throw new ValidacionException("Revocar una cuenta de servicio exige un motivo");
		}
		CuentaServicio cuenta = cuentaServicioRepository.buscarPorIdYTenant(cuentaId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(ENTIDAD, cuentaId));
		if (!cuenta.isActiva()) {
			throw new ValidacionException("La cuenta de servicio ya estaba revocada");
		}
		cuenta.setActiva(false);
		cuenta.setMotivoRevocacion(motivo);
		cuenta.setBaja(Instant.now());
		cuentaServicioRepository.save(cuenta);
		auditoriaService.registrarConDetalle(tenantId, AccionAuditoria.CUENTA_SERVICIO_REVOCADA, ENTIDAD, cuentaId,
				Map.of("nombre", cuenta.getNombre(), "motivo", motivo));
		log.info("La cuenta de servicio {} queda revocada: {}", cuenta.getNombre(), motivo);
		return administracionConverter.aModelo(cuenta);
	}

	@Transactional(readOnly = true)
	public List<CuentaServicio> listar(String tenantId) {
		return cuentaServicioRepository.listarPorTenant(tenantId);
	}

	private boolean estaExpirada(CuentaServicio cuenta) {
		return cuenta.getExpira() != null && cuenta.getExpira().isBefore(Instant.now());
	}
}
