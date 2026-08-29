package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.nextdocs.ai.config.PropiedadesWhatsapp;
import com.nextdocs.ai.entidades.CorrelacionWhatsapp;
import com.nextdocs.ai.entidades.LineaWhatsapp;
import com.nextdocs.ai.entidades.MensajeWhatsappSaliente;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.EstadoMensajeSaliente;
import com.nextdocs.ai.enumeraciones.PlantillaWhatsapp;
import com.nextdocs.ai.repositorios.MensajeWhatsappEntranteRepository;
import com.nextdocs.ai.repositorios.MensajeWhatsappSalienteRepository;
import com.nextdocs.ai.utiles.NumeroTelefono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WhatsappSalienteService {

	public static final String ENTIDAD = "MensajeWhatsappSaliente";

	private static final Logger log = LoggerFactory.getLogger(WhatsappSalienteService.class);

	private final MensajeWhatsappSalienteRepository mensajeWhatsappSalienteRepository;

	private final MensajeWhatsappEntranteRepository mensajeWhatsappEntranteRepository;

	private final ClienteWhatsappService clienteWhatsappService;

	private final AuditoriaService auditoriaService;

	private final PropiedadesWhatsapp propiedades;

	public WhatsappSalienteService(MensajeWhatsappSalienteRepository mensajeWhatsappSalienteRepository,
			MensajeWhatsappEntranteRepository mensajeWhatsappEntranteRepository,
			ClienteWhatsappService clienteWhatsappService, AuditoriaService auditoriaService,
			PropiedadesWhatsapp propiedades) {
		this.mensajeWhatsappSalienteRepository = mensajeWhatsappSalienteRepository;
		this.mensajeWhatsappEntranteRepository = mensajeWhatsappEntranteRepository;
		this.clienteWhatsappService = clienteWhatsappService;
		this.auditoriaService = auditoriaService;
		this.propiedades = propiedades;
	}

	@Transactional
	public MensajeWhatsappSaliente enviar(LineaWhatsapp linea, CorrelacionWhatsapp correlacion,
			PlantillaWhatsapp plantilla, String numeroDestino, String cuerpo, List<String> parametrosPlantilla) {
		MensajeWhatsappSaliente mensaje = new MensajeWhatsappSaliente();
		mensaje.setTenant(linea.getTenant());
		mensaje.setLinea(linea);
		mensaje.setCorrelacion(correlacion);
		mensaje.setPlantilla(plantilla);
		mensaje.setNumeroDestino(NumeroTelefono.normalizar(numeroDestino));
		mensaje.setCuerpo(cuerpo);
		mensaje.setEstado(EstadoMensajeSaliente.PENDIENTE);
		mensaje.setAlta(Instant.now());

		if (mensaje.getNumeroDestino() == null) {
			return fallar(linea, mensaje, "El numero " + numeroDestino + " no tiene formato E.164 valido");
		}

		boolean dentroDeVentana = estaDentroDeLaVentana(linea, mensaje.getNumeroDestino());
		mensaje.setDentroDeVentana(dentroDeVentana);
		if (!dentroDeVentana) {
			if (linea.getNombrePlantillaSolicitud() == null || linea.getNombrePlantillaSolicitud().isBlank()) {
				return fallar(linea, mensaje, "Pasaron mas de " + propiedades.getMinutosVentanaRespuesta()
						+ " minutos desde el ultimo mensaje del contacto y la linea no tiene una plantilla"
						+ " aprobada por Meta configurada: fuera de esa ventana WhatsApp solo acepta plantillas");
			}
			mensaje.setNombrePlantillaMeta(linea.getNombrePlantillaSolicitud());
		}

		try {
			String identificador = dentroDeVentana
					? clienteWhatsappService.enviarTexto(linea, mensaje.getNumeroDestino(), cuerpo)
					: clienteWhatsappService.enviarPlantilla(linea, mensaje.getNumeroDestino(),
							linea.getNombrePlantillaSolicitud(), linea.getIdiomaPlantillaSolicitud(),
							parametrosPlantilla);
			mensaje.setIdentificadorMensaje(identificador);
			mensaje.setEstado(EstadoMensajeSaliente.ENVIADO);
			mensaje.setEnviado(Instant.now());
		}
		catch (Exception e) {
			return fallar(linea, mensaje, e.getMessage());
		}
		return registrar(linea, mensaje);
	}

	private boolean estaDentroDeLaVentana(LineaWhatsapp linea, String numero) {
		Optional<Instant> ultimo = mensajeWhatsappEntranteRepository.ultimoContactoDelNumero(linea.getId(), numero);
		return ultimo.isPresent() && ultimo.get()
				.isAfter(Instant.now().minus(propiedades.getMinutosVentanaRespuesta(), ChronoUnit.MINUTES));
	}

	private MensajeWhatsappSaliente fallar(LineaWhatsapp linea, MensajeWhatsappSaliente mensaje, String motivo) {
		mensaje.setEstado(EstadoMensajeSaliente.FALLIDO);
		mensaje.setDetalleError(motivo);
		log.warn("No se pudo enviar el whatsapp {} de la linea {}: {}", mensaje.getPlantilla(),
				linea.getNumeroTelefono(), motivo);
		return registrar(linea, mensaje);
	}

	private MensajeWhatsappSaliente registrar(LineaWhatsapp linea, MensajeWhatsappSaliente mensaje) {
		mensajeWhatsappSalienteRepository.save(mensaje);
		Map<String, Object> detalle = new LinkedHashMap<>();
		detalle.put("plantilla", mensaje.getPlantilla().name());
		detalle.put("destino", NumeroTelefono.enmascarar(mensaje.getNumeroDestino()));
		detalle.put("linea", linea.getNumeroTelefono());
		detalle.put("estado", mensaje.getEstado().name());
		detalle.put("dentroDeVentana", mensaje.isDentroDeVentana());
		if (mensaje.getNombrePlantillaMeta() != null) {
			detalle.put("plantillaMeta", mensaje.getNombrePlantillaMeta());
		}
		if (mensaje.getIdentificadorMensaje() != null) {
			detalle.put("identificadorMensaje", mensaje.getIdentificadorMensaje());
		}
		if (mensaje.getEstado() == EstadoMensajeSaliente.FALLIDO) {
			detalle.put("error", mensaje.getDetalleError());
			auditoriaService.registrarFallo(linea.getTenant().getId(), AccionAuditoria.WHATSAPP_ENVIADO, ENTIDAD,
					mensaje.getId(), detalle);
		}
		else {
			auditoriaService.registrarConDetalle(linea.getTenant().getId(), AccionAuditoria.WHATSAPP_ENVIADO,
					ENTIDAD, mensaje.getId(), detalle);
		}
		return mensaje;
	}
}
