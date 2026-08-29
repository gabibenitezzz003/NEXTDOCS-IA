package com.nextdocs.ai.utiles;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.web.multipart.MultipartFile;

public class ArchivoEnMemoria implements MultipartFile {

	private final String nombre;

	private final String tipoMimeDeclarado;

	private final byte[] contenido;

	public ArchivoEnMemoria(String nombre, String tipoMimeDeclarado, byte[] contenido) {
		this.nombre = nombre;
		this.tipoMimeDeclarado = tipoMimeDeclarado;
		this.contenido = contenido == null ? new byte[0] : contenido;
	}

	@Override
	public String getName() {
		return "archivo";
	}

	@Override
	public String getOriginalFilename() {
		return nombre;
	}

	@Override
	public String getContentType() {
		return tipoMimeDeclarado;
	}

	@Override
	public boolean isEmpty() {
		return contenido.length == 0;
	}

	@Override
	public long getSize() {
		return contenido.length;
	}

	@Override
	public byte[] getBytes() {
		return contenido;
	}

	@Override
	public InputStream getInputStream() {
		return new ByteArrayInputStream(contenido);
	}

	@Override
	public void transferTo(File destino) throws IOException {
		Files.write(Path.of(destino.getAbsolutePath()), contenido);
	}
}
