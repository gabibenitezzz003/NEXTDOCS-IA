package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import com.nextdocs.ai.config.PropiedadesCorreo;
import com.nextdocs.ai.entidades.BuzonCorreo;
import com.nextdocs.ai.entidades.CorrelacionCorreo;
import com.nextdocs.ai.entidades.MensajeCorreoSaliente;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.EstadoMensajeSaliente;
import com.nextdocs.ai.enumeraciones.PlantillaCorreo;
import com.nextdocs.ai.repositorios.MensajeCorreoSalienteRepository;
import com.nextdocs.ai.utiles.DireccionCorreo;
import com.nextdocs.ai.utiles.ResolvedorSecreto;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CorreoSalienteService {

	public static final String ENTIDAD = "MensajeCorreoSaliente";

	private static final Logger log = LoggerFactory.getLogger(CorreoSalienteService.class);

	private final MensajeCorreoSalienteRepository mensajeCorreoSalienteRepository;

	private final AuditoriaService auditoriaService;

	private final PropiedadesCorreo propiedades;

	public CorreoSalienteService(MensajeCorreoSalienteRepository mensajeCorreoSalienteRepository,
			AuditoriaService auditoriaService, PropiedadesCorreo propiedades) {
		this.mensajeCorreoSalienteRepository = mensajeCorreoSalienteRepository;
		this.auditoriaService = auditoriaService;
		this.propiedades = propiedades;
	}

	@Transactional
	public MensajeCorreoSaliente enviar(BuzonCorreo buzon, CorrelacionCorreo correlacion, PlantillaCorreo plantilla,
			String destinatario, String asunto, String cuerpo) {
		MensajeCorreoSaliente mensaje = new MensajeCorreoSaliente();
		mensaje.setTenant(buzon.getTenant());
		mensaje.setBuzon(buzon);
		mensaje.setCorrelacion(correlacion);
		mensaje.setPlantilla(plantilla);
		mensaje.setDestinatarios(DireccionCorreo.normalizar(destinatario));
		mensaje.setAsunto(asunto);
		mensaje.setEstado(EstadoMensajeSaliente.PENDIENTE);
		mensaje.setAlta(Instant.now());

		if (mensaje.getDestinatarios() == null) {
			mensaje.setEstado(EstadoMensajeSaliente.FALLIDO);
			mensaje.setDetalleError("El destinatario no es una direccion valida");
			return registrar(buzon, mensaje);
		}
		if (buzon.getHostSalida() == null || buzon.getHostSalida().isBlank()) {
			mensaje.setEstado(EstadoMensajeSaliente.FALLIDO);
			mensaje.setDetalleError("El buzon " + buzon.getDireccion() + " no tiene salida SMTP configurada");
			return registrar(buzon, mensaje);
		}

		String identificador = "<" + UUID.randomUUID() + "@nextdocs-ai>";
		mensaje.setIdentificadorMensaje(identificador);
		try {
			despachar(buzon, mensaje, cuerpo, identificador);
			mensaje.setEstado(EstadoMensajeSaliente.ENVIADO);
			mensaje.setEnviado(Instant.now());
		}
		catch (Exception e) {
			mensaje.setEstado(EstadoMensajeSaliente.FALLIDO);
			mensaje.setDetalleError(e.getMessage());
			log.warn("No se pudo enviar el correo {} a {}: {}", plantilla, mensaje.getDestinatarios(),
					e.getMessage());
		}
		return registrar(buzon, mensaje);
	}

	private MensajeCorreoSaliente registrar(BuzonCorreo buzon, MensajeCorreoSaliente mensaje) {
		mensajeCorreoSalienteRepository.save(mensaje);
		Map<String, Object> detalle = new java.util.LinkedHashMap<>();
		detalle.put("plantilla", mensaje.getPlantilla().name());
		detalle.put("destinatario", mensaje.getDestinatarios());
		detalle.put("identificadorMensaje", mensaje.getIdentificadorMensaje());
		detalle.put("estado", mensaje.getEstado().name());
		detalle.put("buzon", buzon.getDireccion());
		if (mensaje.getEstado() == EstadoMensajeSaliente.FALLIDO) {
			detalle.put("error", mensaje.getDetalleError());
			auditoriaService.registrarFallo(buzon.getTenant().getId(), AccionAuditoria.CORREO_ENVIADO, ENTIDAD,
					mensaje.getId(), detalle);
		}
		else {
			auditoriaService.registrarConDetalle(buzon.getTenant().getId(), AccionAuditoria.CORREO_ENVIADO, ENTIDAD,
					mensaje.getId(), detalle);
		}
		return mensaje;
	}

	private void despachar(BuzonCorreo buzon, MensajeCorreoSaliente mensaje, String cuerpo, String identificador)
			throws MessagingException, java.io.UnsupportedEncodingException {
		Properties configuracion = new Properties();
		configuracion.put("mail.transport.protocol", "smtp");
		configuracion.put("mail.smtp.host", buzon.getHostSalida());
		configuracion.put("mail.smtp.port", String.valueOf(buzon.getPuertoSalida()));
		configuracion.put("mail.smtp.connectiontimeout", String.valueOf(propiedades.getTiempoEsperaMilisegundos()));
		configuracion.put("mail.smtp.timeout", String.valueOf(propiedades.getTiempoEsperaMilisegundos()));
		if (buzon.isSalidaSegura()) {
			configuracion.put("mail.smtp.starttls.enable", "true");
			configuracion.put("mail.smtp.starttls.required", "true");
		}

		String clave = ResolvedorSecreto.resolver(buzon.getReferenciaSecretoSalida()).orElse(null);
		String usuario = buzon.getUsuarioSalida() == null || buzon.getUsuarioSalida().isBlank()
				? buzon.getUsuarioEntrada() : buzon.getUsuarioSalida();
		configuracion.put("mail.smtp.auth", String.valueOf(clave != null));

		Session sesion = Session.getInstance(configuracion);
		MimeMessage mime = new MensajeConIdentificador(sesion, identificador);
		mime.setFrom(new InternetAddress(buzon.getDireccion(), buzon.getNombre()));
		mime.setRecipients(MimeMessage.RecipientType.TO,
				InternetAddress.parse(mensaje.getDestinatarios(), false));
		mime.setSubject(mensaje.getAsunto(), "UTF-8");
		mime.setText(cuerpo, "UTF-8");
		mime.setSentDate(java.util.Date.from(Instant.now()));
		mime.saveChanges();

		try (Transport transporte = sesion.getTransport("smtp")) {
			if (clave != null) {
				transporte.connect(buzon.getHostSalida(), buzon.getPuertoSalida(), usuario, clave);
			}
			else {
				transporte.connect(buzon.getHostSalida(), buzon.getPuertoSalida(), null, null);
			}
			transporte.sendMessage(mime, mime.getAllRecipients());
		}
	}

	private static final class MensajeConIdentificador extends MimeMessage {

		private final String identificador;

		private MensajeConIdentificador(Session sesion, String identificador) {
			super(sesion);
			this.identificador = identificador;
		}

		@Override
		protected void updateMessageID() throws MessagingException {
			setHeader("Message-ID", identificador);
		}
	}
}
