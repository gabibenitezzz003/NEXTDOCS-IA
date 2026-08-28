package com.nextdocs.ai.utiles;

import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;

import com.nextdocs.ai.exceptions.ValidacionException;

public final class ValidadorUrlWebhook {

	private ValidadorUrlWebhook() {
	}

	public static URI validar(String url, boolean permitirLocalhost) {
		if (url == null || url.isBlank()) {
			throw new ValidacionException("La URL del webhook es obligatoria");
		}
		URI uri;
		try {
			uri = URI.create(url.trim());
		} catch (Exception e) {
			throw new ValidacionException("La URL del webhook no es valida");
		}
		if (uri.getHost() == null || uri.getHost().isBlank()) {
			throw new ValidacionException("La URL del webhook no tiene un host valido");
		}
		boolean local = esLocal(uri.getHost());
		if (local && !permitirLocalhost) {
			throw new ValidacionException("La URL del webhook no puede apuntar a una red interna");
		}
		if (!local && esInterno(uri.getHost())) {
			throw new ValidacionException("La URL del webhook no puede apuntar a una red interna");
		}
		String esquema = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
		if ("https".equals(esquema)) {
			return uri;
		}
		if ("http".equals(esquema) && local && permitirLocalhost) {
			return uri;
		}
		throw new ValidacionException("La URL del webhook debe ser HTTPS");
	}

	private static boolean esLocal(String host) {
		String normalizado = host.toLowerCase(Locale.ROOT);
		if ("localhost".equals(normalizado) || normalizado.endsWith(".localhost")) {
			return true;
		}
		try {
			InetAddress direccion = InetAddress.getByName(host);
			return direccion.isLoopbackAddress() || direccion.isAnyLocalAddress();
		} catch (Exception e) {
			return false;
		}
	}

	private static boolean esInterno(String host) {
		String normalizado = host.toLowerCase(Locale.ROOT);
		if ("metadata.google.internal".equals(normalizado) || normalizado.endsWith(".internal")) {
			return true;
		}
		try {
			InetAddress direccion = InetAddress.getByName(host);
			return direccion.isSiteLocalAddress() || direccion.isLinkLocalAddress() || direccion.isMulticastAddress()
					|| direccion.isAnyLocalAddress() || direccion.isLoopbackAddress();
		} catch (Exception e) {
			throw new ValidacionException("No se pudo resolver el host del webhook");
		}
	}
}
