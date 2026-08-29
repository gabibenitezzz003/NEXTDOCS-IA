package com.nextdocs.ai.utiles;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.nextdocs.ai.exceptions.ValidacionException;

public final class Hash {

	private static final String SHA_256 = "SHA-256";

	private static final String HMAC_SHA_256 = "HmacSHA256";

	private static final int TAMANO_BUFFER = 8192;

	private Hash() {
	}

	public static String sha256(byte[] contenido) {
		try {
			return hexadecimal(MessageDigest.getInstance(SHA_256).digest(contenido));
		} catch (NoSuchAlgorithmException e) {
			throw new ValidacionException("Algoritmo de hash no disponible");
		}
	}

	public static String sha256(String texto) {
		return sha256(texto.getBytes(StandardCharsets.UTF_8));
	}

	public static String sha256(InputStream flujo) {
		try {
			MessageDigest resumen = MessageDigest.getInstance(SHA_256);
			byte[] buffer = new byte[TAMANO_BUFFER];
			int leidos;
			while ((leidos = flujo.read(buffer)) != -1) {
				resumen.update(buffer, 0, leidos);
			}
			return hexadecimal(resumen.digest());
		} catch (Exception e) {
			throw new ValidacionException("No se pudo calcular el hash del archivo");
		}
	}

	public static String hmacSha256(String secreto, String contenido) {
		try {
			Mac mac = Mac.getInstance(HMAC_SHA_256);
			mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));
			return hexadecimal(mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception e) {
			throw new ValidacionException("No se pudo firmar el contenido");
		}
	}

	public static boolean sonIguales(String primero, String segundo) {
		if (primero == null || segundo == null) {
			return false;
		}
		return MessageDigest.isEqual(primero.getBytes(StandardCharsets.UTF_8),
				segundo.getBytes(StandardCharsets.UTF_8));
	}

	private static String hexadecimal(byte[] datos) {
		StringBuilder constructor = new StringBuilder(datos.length * 2);
		for (byte dato : datos) {
			constructor.append(Character.forDigit((dato >> 4) & 0xF, 16));
			constructor.append(Character.forDigit(dato & 0xF, 16));
		}
		return constructor.toString();
	}
}
