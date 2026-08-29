package com.nextdocs.ai.servicios;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.nextdocs.ai.config.PropiedadesCorreo;
import com.nextdocs.ai.entidades.BuzonCorreo;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.CorreoCrudoModel;
import com.nextdocs.ai.utiles.Hash;
import com.nextdocs.ai.utiles.ResolvedorSecreto;

import jakarta.mail.Address;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.search.FlagTerm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LectorCorreoService {

	private static final Logger log = LoggerFactory.getLogger(LectorCorreoService.class);

	private final PropiedadesCorreo propiedades;

	public LectorCorreoService(PropiedadesCorreo propiedades) {
		this.propiedades = propiedades;
	}

	public List<CorreoCrudoModel> leerNoLeidos(BuzonCorreo buzon, int tope) {
		String clave = ResolvedorSecreto.resolver(buzon.getReferenciaSecretoEntrada())
				.orElseThrow(() -> new ValidacionException("No se pudo resolver el secreto de entrada del buzon "
						+ buzon.getDireccion() + ". Revisa la referencia " + buzon.getReferenciaSecretoEntrada()));
		List<CorreoCrudoModel> correos = new ArrayList<>();
		Store almacen = null;
		Folder carpeta = null;
		try {
			Session sesion = Session.getInstance(propiedadesDe(buzon));
			almacen = sesion.getStore(buzon.isEntradaSegura() ? "imaps" : "imap");
			almacen.connect(buzon.getHostEntrada(), buzon.getPuertoEntrada(), buzon.getUsuarioEntrada(), clave);
			carpeta = almacen.getFolder(buzon.getCarpeta() == null ? "INBOX" : buzon.getCarpeta());
			carpeta.open(Folder.READ_WRITE);
			Message[] mensajes = carpeta.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
			for (Message mensaje : mensajes) {
				if (correos.size() >= tope) {
					break;
				}
				correos.add(convertir(mensaje));
				mensaje.setFlag(Flags.Flag.SEEN, true);
			}
		}
		catch (Exception e) {
			throw new ValidacionException(
					"No se pudo leer el buzon " + buzon.getDireccion() + ": " + e.getMessage());
		}
		finally {
			cerrar(carpeta, almacen);
		}
		return correos;
	}

	public void verificar(BuzonCorreo buzon) {
		String clave = ResolvedorSecreto.resolver(buzon.getReferenciaSecretoEntrada())
				.orElseThrow(() -> new ValidacionException("No se pudo resolver el secreto de entrada del buzon "
						+ buzon.getDireccion() + ". Revisa la referencia " + buzon.getReferenciaSecretoEntrada()));
		Store almacen = null;
		Folder carpeta = null;
		try {
			Session sesion = Session.getInstance(propiedadesDe(buzon));
			almacen = sesion.getStore(buzon.isEntradaSegura() ? "imaps" : "imap");
			almacen.connect(buzon.getHostEntrada(), buzon.getPuertoEntrada(), buzon.getUsuarioEntrada(), clave);
			carpeta = almacen.getFolder(buzon.getCarpeta() == null ? "INBOX" : buzon.getCarpeta());
			carpeta.open(Folder.READ_ONLY);
		}
		catch (Exception e) {
			throw new ValidacionException(
					"No se pudo conectar al buzon " + buzon.getDireccion() + ": " + e.getMessage());
		}
		finally {
			cerrar(carpeta, almacen);
		}
	}

	private CorreoCrudoModel convertir(Message mensaje) throws Exception {
		CorreoCrudoModel correo = new CorreoCrudoModel();
		correo.setAsunto(decodificar(mensaje.getSubject()));
		correo.setRemitente(primeraDireccion(mensaje.getFrom()));
		correo.setDestinatarios(direcciones(mensaje.getAllRecipients()));
		correo.setEnviadoEn(mensaje.getSentDate() == null ? Instant.now() : mensaje.getSentDate().toInstant());
		recolectarAdjuntos(mensaje, correo.getAdjuntos());
		correo.setIdentificadorMensaje(identificadorDe(mensaje, correo));
		return correo;
	}

	private String identificadorDe(Message mensaje, CorreoCrudoModel correo) throws Exception {
		String[] cabecera = mensaje.getHeader("Message-ID");
		if (cabecera != null && cabecera.length > 0 && cabecera[0] != null && !cabecera[0].isBlank()) {
			return cabecera[0].trim();
		}
		StringBuilder huella = new StringBuilder();
		huella.append(correo.getRemitente()).append('|').append(correo.getAsunto()).append('|')
				.append(correo.getEnviadoEn());
		for (CorreoCrudoModel.AdjuntoCrudoModel adjunto : correo.getAdjuntos()) {
			huella.append('|').append(adjunto.getNombre()).append(':')
					.append(adjunto.getContenido() == null ? "vacio" : Hash.sha256(adjunto.getContenido()));
		}
		return "<sin-id-" + Hash.sha256(huella.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))
				+ "@nextdocs-ai>";
	}

	private void recolectarAdjuntos(Part parte, List<CorreoCrudoModel.AdjuntoCrudoModel> destino) throws Exception {
		Object contenido = parte.getContent();
		if (contenido instanceof Multipart multiparte) {
			for (int i = 0; i < multiparte.getCount(); i++) {
				recolectarAdjuntos(multiparte.getBodyPart(i), destino);
			}
			return;
		}
		String nombre = decodificar(parte.getFileName());
		if (nombre == null || nombre.isBlank()) {
			return;
		}
		CorreoCrudoModel.AdjuntoCrudoModel adjunto = new CorreoCrudoModel.AdjuntoCrudoModel();
		adjunto.setNombre(nombre);
		adjunto.setTipoMimeDeclarado(parte.getContentType());
		adjunto.setContenido(leer(parte.getInputStream(), nombre));
		destino.add(adjunto);
	}

	private byte[] leer(InputStream entrada, String nombre) throws Exception {
		try (InputStream flujo = entrada; ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
			byte[] bloque = new byte[8192];
			int leidos;
			long total = 0;
			while ((leidos = flujo.read(bloque)) > 0) {
				total += leidos;
				if (total > propiedades.getTamanoMaximoAdjuntoBytes()) {
					log.warn("El adjunto {} supera el tope de lectura del canal y no se descarga", nombre);
					return null;
				}
				salida.write(bloque, 0, leidos);
			}
			return salida.toByteArray();
		}
	}

	private Properties propiedadesDe(BuzonCorreo buzon) {
		String protocolo = buzon.isEntradaSegura() ? "imaps" : "imap";
		Properties configuracion = new Properties();
		configuracion.put("mail.store.protocol", protocolo);
		configuracion.put("mail." + protocolo + ".host", buzon.getHostEntrada());
		configuracion.put("mail." + protocolo + ".port", String.valueOf(buzon.getPuertoEntrada()));
		configuracion.put("mail." + protocolo + ".connectiontimeout",
				String.valueOf(propiedades.getTiempoEsperaMilisegundos()));
		configuracion.put("mail." + protocolo + ".timeout",
				String.valueOf(propiedades.getTiempoEsperaMilisegundos()));
		configuracion.put("mail." + protocolo + ".partialfetch", "false");
		if (buzon.isEntradaSegura()) {
			configuracion.put("mail.imaps.ssl.enable", "true");
		}
		return configuracion;
	}

	private List<String> direcciones(Address[] direcciones) {
		List<String> resultado = new ArrayList<>();
		if (direcciones == null) {
			return resultado;
		}
		for (Address direccion : direcciones) {
			if (direccion instanceof InternetAddress internet && internet.getAddress() != null) {
				resultado.add(internet.getAddress());
			}
		}
		return resultado;
	}

	private String primeraDireccion(Address[] direcciones) {
		List<String> lista = direcciones(direcciones);
		return lista.isEmpty() ? null : lista.get(0);
	}

	private String decodificar(String valor) {
		if (valor == null) {
			return null;
		}
		try {
			return MimeUtility.decodeText(valor);
		}
		catch (Exception e) {
			return valor;
		}
	}

	private void cerrar(Folder carpeta, Store almacen) {
		try {
			if (carpeta != null && carpeta.isOpen()) {
				carpeta.close(false);
			}
		}
		catch (Exception e) {
			log.debug("No se pudo cerrar la carpeta del buzon: {}", e.getMessage());
		}
		try {
			if (almacen != null && almacen.isConnected()) {
				almacen.close();
			}
		}
		catch (Exception e) {
			log.debug("No se pudo cerrar la conexion del buzon: {}", e.getMessage());
		}
	}
}
