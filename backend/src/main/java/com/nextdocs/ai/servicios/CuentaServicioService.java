package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import com.nextdocs.ai.entidades.CuentaServicio;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.TipoActor;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.PrincipalNextDocs;
import com.nextdocs.ai.repositorios.CuentaServicioRepository;
import com.nextdocs.ai.utiles.Hash;

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

	private final CuentaServicioRepository cuentaServicioRepository;

	private final AuditoriaService auditoriaService;

	private final SecureRandom aleatorio = new SecureRandom();

	public CuentaServicioService(CuentaServicioRepository cuentaServicioRepository, AuditoriaService auditoriaService) {
		this.cuentaServicioRepository = cuentaServicioRepository;
		this.auditoriaService = auditoriaService;
	}

	@Transactional
	public String crear(Tenant tenant, String nombre, List<String> alcances) {
		if (nombre == null || nombre.isBlank()) {
			throw new ValidacionException("El nombre de la cuenta de servicio es obligatorio");
		}
		byte[] material = new byte[BYTES_CLAVE];
		aleatorio.nextBytes(material);
		String claveEnClaro = PREFIJO + "_" + Base64.getUrlEncoder().withoutPadding().encodeToString(material);
		CuentaServicio cuenta = new CuentaServicio();
		cuenta.setTenant(tenant);
		cuenta.setNombre(nombre.trim());
		cuenta.setPrefijoClave(claveEnClaro.substring(0, 12));
		cuenta.setClaveHash(Hash.sha256(claveEnClaro));
		cuenta.setAlcances(new LinkedHashSet<>(alcances));
		cuenta.setActiva(true);
		cuenta.setAlta(Instant.now());
		cuentaServicioRepository.save(cuenta);
		auditoriaService.registrar(tenant.getId(), AccionAuditoria.CUENTA_SERVICIO_CREADA, ENTIDAD, cuenta.getId());
		return claveEnClaro;
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
	public void revocar(String tenantId, String cuentaId, String motivo) {
		CuentaServicio cuenta = cuentaServicioRepository.buscarPorIdYTenant(cuentaId, tenantId)
				.orElseThrow(() -> EntidadNoEncontradaException.de(ENTIDAD, cuentaId));
		cuenta.setActiva(false);
		cuenta.setMotivoRevocacion(motivo);
		cuenta.setBaja(Instant.now());
		cuentaServicioRepository.save(cuenta);
		auditoriaService.registrar(tenantId, AccionAuditoria.CUENTA_SERVICIO_REVOCADA, ENTIDAD, cuentaId);
	}

	@Transactional(readOnly = true)
	public List<CuentaServicio> listar(String tenantId) {
		return cuentaServicioRepository.listarPorTenant(tenantId);
	}

	private boolean estaExpirada(CuentaServicio cuenta) {
		return cuenta.getExpira() != null && cuenta.getExpira().isBefore(Instant.now());
	}
}
