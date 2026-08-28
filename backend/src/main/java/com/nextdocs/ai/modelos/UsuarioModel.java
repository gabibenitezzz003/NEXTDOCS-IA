package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.enumeraciones.EstadoUsuario;
import com.nextdocs.ai.enumeraciones.OrigenIdentidad;

import lombok.Data;

@Data
public class UsuarioModel implements Serializable {

	private static final long serialVersionUID = 6423557740290156301L;

	private String id;

	private String email;

	private String nombre;

	private EstadoUsuario estado;

	private OrigenIdentidad origenIdentidad;

	private String idUsuarioExterno;

	private String idioma;

	private String zonaHoraria;

	private boolean administrador;

	private Instant ultimoAcceso;

	private Instant alta;

	private List<String> roles = new ArrayList<>();

	private List<String> permisos = new ArrayList<>();
}
