package com.nextdocs.ai.integracion;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class ServidorGraphFalso {

	public static final String TOKEN_ESPERADO = "token-de-acceso-de-prueba";

	private final HttpServer servidor;

	private final Map<String, MediaFalsa> medias = new LinkedHashMap<>();

	private final List<String> enviados = new java.util.ArrayList<>();

	private final AtomicInteger descargas = new AtomicInteger();

	private final AtomicInteger secuencia = new AtomicInteger();

	public ServidorGraphFalso() throws IOException {
		servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		servidor.createContext("/", this::atender);
		servidor.start();
	}

	public String url() {
		return "http://127.0.0.1:" + servidor.getAddress().getPort();
	}

	public void detener() {
		servidor.stop(0);
	}

	public String registrarMedia(String tipoMime, byte[] contenido) {
		return registrarMedia(tipoMime, contenido, contenido.length);
	}

	public String registrarMedia(String tipoMime, byte[] contenido, long tamanoDeclarado) {
		String identificador = "media-" + secuencia.incrementAndGet();
		medias.put(identificador, new MediaFalsa(tipoMime, contenido, tamanoDeclarado));
		return identificador;
	}

	public int descargas() {
		return descargas.get();
	}

	public List<String> enviados() {
		return List.copyOf(enviados);
	}

	private void atender(HttpExchange intercambio) throws IOException {
		String ruta = intercambio.getRequestURI().getPath();
		String autorizacion = intercambio.getRequestHeaders().getFirst("Authorization");
		if (!("Bearer " + TOKEN_ESPERADO).equals(autorizacion)) {
			responder(intercambio, 401, "application/json", "{\"error\":{\"message\":\"token invalido\"}}"
					.getBytes(StandardCharsets.UTF_8));
			return;
		}

		if (ruta.endsWith("/messages")) {
			byte[] cuerpo = intercambio.getRequestBody().readAllBytes();
			enviados.add(new String(cuerpo, StandardCharsets.UTF_8));
			responder(intercambio, 200, "application/json",
					("{\"messaging_product\":\"whatsapp\",\"messages\":[{\"id\":\"wamid.salida."
							+ secuencia.incrementAndGet() + "\"}]}").getBytes(StandardCharsets.UTF_8));
			return;
		}

		if (ruta.startsWith("/descarga/")) {
			MediaFalsa media = medias.get(ruta.substring("/descarga/".length()));
			if (media == null) {
				responder(intercambio, 404, "application/json", "{}".getBytes(StandardCharsets.UTF_8));
				return;
			}
			descargas.incrementAndGet();
			responder(intercambio, 200, media.tipoMime(), media.contenido());
			return;
		}

		String identificador = ruta.substring(ruta.lastIndexOf('/') + 1);
		MediaFalsa media = medias.get(identificador);
		if (media == null) {
			responder(intercambio, 200, "application/json",
					("{\"display_phone_number\":\"+541150000000\",\"id\":\"" + identificador + "\"}")
							.getBytes(StandardCharsets.UTF_8));
			return;
		}
		String metadatos = "{\"url\":\"" + url() + "/descarga/" + identificador + "\",\"mime_type\":\""
				+ media.tipoMime() + "\",\"file_size\":" + media.tamanoDeclarado() + ",\"id\":\"" + identificador
				+ "\"}";
		responder(intercambio, 200, "application/json", metadatos.getBytes(StandardCharsets.UTF_8));
	}

	private void responder(HttpExchange intercambio, int codigo, String tipoContenido, byte[] cuerpo)
			throws IOException {
		intercambio.getResponseHeaders().add("Content-Type", tipoContenido);
		intercambio.sendResponseHeaders(codigo, cuerpo.length);
		try (OutputStream salida = intercambio.getResponseBody()) {
			salida.write(cuerpo);
		}
	}

	private record MediaFalsa(String tipoMime, byte[] contenido, long tamanoDeclarado) {
	}
}
