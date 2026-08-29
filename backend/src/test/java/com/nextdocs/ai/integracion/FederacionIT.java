package com.nextdocs.ai.integracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdocs.ai.entidades.CodigoEmbed;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.enumeraciones.EstadoUsuario;
import com.nextdocs.ai.enumeraciones.OrigenIdentidad;
import com.nextdocs.ai.exceptions.NoAutorizadoException;
import com.nextdocs.ai.exceptions.ProhibidoException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.CodigoEmbedModel;
import com.nextdocs.ai.modelos.IntercambioFederadoReqModel;
import com.nextdocs.ai.modelos.NuevoProveedorIdentidadReqModel;
import com.nextdocs.ai.modelos.ProveedorIdentidadModel;
import com.nextdocs.ai.modelos.SesionResModel;
import com.nextdocs.ai.repositorios.CodigoEmbedRepository;
import com.nextdocs.ai.repositorios.UsuarioRepository;
import com.nextdocs.ai.servicios.FederacionIdentidadService;
import com.nextdocs.ai.servicios.ProveedorIdentidadService;
import com.nextdocs.ai.utiles.Hash;
import com.nextdocs.ai.utiles.Permiso;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class FederacionIT extends PruebaIntegracion {

	private static final String HOST = variableDePrueba("NEXTDOCS_PRUEBA_KEYCLOAK_HOST", "localhost");

	private static final String PUERTO = variableDePrueba("NEXTDOCS_PRUEBA_KEYCLOAK_PUERTO", "8089");

	private static final String REALM = "nextdocs-prueba";

	private static final String BASE = "http://" + HOST + ":" + PUERTO + "/realms/" + REALM;

	private static final String EMISOR = BASE;

	private static final String JWKS = BASE + "/protocol/openid-connect/certs";

	private static final ObjectMapper MAPEADOR = new ObjectMapper();

	@Autowired
	private FabricaDatosPrueba fabrica;

	@Autowired
	private ProveedorIdentidadService proveedorIdentidadService;

	@Autowired
	private FederacionIdentidadService federacionIdentidadService;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private CodigoEmbedRepository codigoEmbedRepository;

	private Tenant tenant;

	private ProveedorIdentidadModel proveedor;

	@BeforeEach
	void prepararEscenario() {
		exigirIdp();
		tenant = fabrica.crearTenant("federado-" + UUID.randomUUID().toString().substring(0, 8));
		proveedor = crearProveedor("FOLLOW", datos -> {
			datos.setPermitirJit(true);
			datos.setCodigoRolPorDefecto(Permiso.CODIGO_ROL_OPERADOR);
			datos.setDominiosPermitidos("@proveedores.com");
			datos.setOrigenesEmbedPermitidos("https://follow.ejemplo.com");
		});
	}

	@Test
	@DisplayName("un usuario del host sin cuenta previa se aprovisiona por JIT con el rol configurado")
	void elUsuarioNuevoSeAprovisiona() throws Exception {
		CodigoEmbedModel codigo = intercambiar(tokenDe("operador.host"), null);
		assertThat(codigo.isAprovisionado()).isTrue();
		assertThat(codigo.getCodigo()).isNotBlank();
		assertThat(codigo.getVenceEn()).isAfter(Instant.now());

		Usuario creado = usuarioRepository.buscarPorEmail(tenant.getId(), "operador.host@proveedores.com")
				.orElseThrow();
		assertThat(creado.getOrigenIdentidad()).isEqualTo(OrigenIdentidad.FOLLOW);
		assertThat(creado.getIdUsuarioExterno()).isNotBlank();
		assertThat(creado.getClaveHash()).isNull();
		assertThat(creado.getRoles()).extracting("codigo").containsExactly(Permiso.CODIGO_ROL_OPERADOR);

		SesionResModel sesion = federacionIdentidadService.canjear(codigo.getCodigo());
		assertThat(sesion.getTokenAcceso()).isNotBlank();
		assertThat(sesion.getCodigoTenant()).isEqualTo(tenant.getCodigo());
		assertThat(sesion.getPermisos()).containsExactlyInAnyOrderElementsOf(Permiso.deOperador());
	}

	@Test
	@DisplayName("el segundo ingreso reutiliza el mismo usuario, no crea otro")
	void elSegundoIngresoNoDuplica() throws Exception {
		intercambiar(tokenDe("operador.host"), null);
		CodigoEmbedModel segundo = intercambiar(tokenDe("operador.host"), null);
		assertThat(segundo.isAprovisionado()).isFalse();
		assertThat(usuariosDelTenant()).hasSize(2);
	}

	@Test
	@DisplayName("el codigo de embed es de un solo uso")
	void elCodigoNoSeReutiliza() throws Exception {
		CodigoEmbedModel codigo = intercambiar(tokenDe("operador.host"), null);
		federacionIdentidadService.canjear(codigo.getCodigo());
		assertThatThrownBy(() -> federacionIdentidadService.canjear(codigo.getCodigo()))
				.isInstanceOf(NoAutorizadoException.class)
				.hasMessageContaining("ya fue usado");
	}

	@Test
	@DisplayName("un codigo vencido se rechaza")
	void elCodigoVencidoSeRechaza() throws Exception {
		CodigoEmbedModel codigo = intercambiar(tokenDe("operador.host"), null);
		CodigoEmbed registro = codigoEmbedRepository.buscarPorHash(Hash.sha256(codigo.getCodigo())).orElseThrow();
		registro.setVenceEn(Instant.now().minusSeconds(5));
		codigoEmbedRepository.save(registro);

		assertThatThrownBy(() -> federacionIdentidadService.canjear(codigo.getCodigo()))
				.isInstanceOf(NoAutorizadoException.class)
				.hasMessageContaining("vencio");
	}

	@Test
	@DisplayName("un codigo inventado se rechaza sin filtrar si existe")
	void elCodigoInventadoSeRechaza() {
		assertThatThrownBy(() -> federacionIdentidadService.canjear("codigo-que-no-existe"))
				.isInstanceOf(NoAutorizadoException.class);
	}

	@Test
	@DisplayName("el usuario deshabilitado no entra aunque el host lo haya autenticado")
	void elUsuarioDeshabilitadoNoEntra() throws Exception {
		intercambiar(tokenDe("operador.host"), null);
		Usuario creado = usuarioRepository.buscarPorEmail(tenant.getId(), "operador.host@proveedores.com")
				.orElseThrow();
		creado.setEstado(EstadoUsuario.BLOQUEADO);
		usuarioRepository.save(creado);

		String token = tokenDe("operador.host");
		assertThatThrownBy(() -> intercambiar(token, null)).isInstanceOf(ProhibidoException.class)
				.hasMessageContaining("deshabilitado");
	}

	@Test
	@DisplayName("si el usuario se bloquea despues de emitir el codigo, el canje tampoco entra")
	void elBloqueoPosteriorCortaElCanje() throws Exception {
		CodigoEmbedModel codigo = intercambiar(tokenDe("operador.host"), null);
		Usuario creado = usuarioRepository.buscarPorEmail(tenant.getId(), "operador.host@proveedores.com")
				.orElseThrow();
		creado.setEstado(EstadoUsuario.BLOQUEADO);
		usuarioRepository.save(creado);

		assertThatThrownBy(() -> federacionIdentidadService.canjear(codigo.getCodigo()))
				.isInstanceOf(ProhibidoException.class)
				.hasMessageContaining("no esta habilitado");
	}

	@Test
	@DisplayName("un token valido de otro tenant no abre sesion en este")
	void elTokenNoCruzaDeTenant() throws Exception {
		Tenant ajeno = fabrica.crearTenant("ajeno-" + UUID.randomUUID().toString().substring(0, 8));
		String token = tokenDe("operador.host");

		IntercambioFederadoReqModel datos = new IntercambioFederadoReqModel();
		datos.setCodigoTenant(ajeno.getCodigo());
		datos.setProveedor("FOLLOW");
		datos.setToken(token);

		assertThatThrownBy(() -> federacionIdentidadService.intercambiar(datos))
				.isInstanceOf(NoAutorizadoException.class)
				.hasMessageContaining("proveedor de identidad activo");
		assertThat(usuarioRepository.buscarPorEmail(ajeno.getId(), "operador.host@proveedores.com")).isEmpty();
	}

	@Test
	@DisplayName("un token emitido por otro cliente del mismo realm se rechaza por audiencia")
	void laAudienciaSeVerifica() throws Exception {
		String token = tokenDe("cima-host", "operador.host");
		assertThatThrownBy(() -> intercambiar(token, null)).isInstanceOf(NoAutorizadoException.class)
				.hasMessageContaining("no fue emitido para");
	}

	@Test
	@DisplayName("un token con la firma alterada se rechaza")
	void laFirmaSeVerifica() throws Exception {
		String token = tokenDe("operador.host");
		String alterado = token.substring(0, token.lastIndexOf('.') + 1) + "ZmlybWEtaW52ZW50YWRh";
		assertThatThrownBy(() -> intercambiar(alterado, null)).isInstanceOf(NoAutorizadoException.class)
				.hasMessageContaining("no es valido");
	}

	@Test
	@DisplayName("un dominio fuera de la lista no se aprovisiona")
	void elDominioAjenoNoSeAprovisiona() throws Exception {
		String token = tokenDe("ajeno.host");
		assertThatThrownBy(() -> intercambiar(token, null)).isInstanceOf(ProhibidoException.class)
				.hasMessageContaining("no esta habilitado en el proveedor");
	}

	@Test
	@DisplayName("un redirect fuera de los origenes declarados se rechaza")
	void elRedirectAjenoSeRechaza() throws Exception {
		String token = tokenDe("operador.host");
		assertThatThrownBy(() -> intercambiar(token, "https://atacante.com/robar"))
				.isInstanceOf(ProhibidoException.class)
				.hasMessageContaining("no esta permitido");
	}

	@Test
	@DisplayName("un redirect del origen declarado se acepta y viaja en el codigo")
	void elRedirectDeclaradoSeAcepta() throws Exception {
		CodigoEmbedModel codigo = intercambiar(tokenDe("operador.host"),
				"https://follow.ejemplo.com/casos/4477");
		assertThat(codigo.getUrlRetorno()).isEqualTo("https://follow.ejemplo.com/casos/4477");
		assertThat(codigo.getAplicacionOrigen()).isEqualTo("FOLLOW");
	}

	@Test
	@DisplayName("sin JIT habilitado no se crea el usuario, se pide alta previa")
	void sinJitNoSeCrea() throws Exception {
		Tenant estricto = fabrica.crearTenant("estricto-" + UUID.randomUUID().toString().substring(0, 8));
		NuevoProveedorIdentidadReqModel datos = base("FOLLOW");
		datos.setPermitirJit(false);
		datos.setDominiosPermitidos("@proveedores.com");
		proveedorIdentidadService.crear(estricto, datos);

		IntercambioFederadoReqModel peticion = new IntercambioFederadoReqModel();
		peticion.setCodigoTenant(estricto.getCodigo());
		peticion.setProveedor("FOLLOW");
		peticion.setToken(tokenDe("operador.host"));

		assertThatThrownBy(() -> federacionIdentidadService.intercambiar(peticion))
				.isInstanceOf(ProhibidoException.class)
				.hasMessageContaining("no permite crear usuarios automaticamente");
	}

	@Test
	@DisplayName("un usuario local con el mismo email no se secuestra en silencio")
	void noSeVinculaPorEmailSinPermiso() throws Exception {
		Tenant otro = fabrica.crearTenant("local-" + UUID.randomUUID().toString().substring(0, 8));
		NuevoProveedorIdentidadReqModel datos = base("FOLLOW");
		datos.setPermitirJit(true);
		datos.setCodigoRolPorDefecto(Permiso.CODIGO_ROL_OPERADOR);
		datos.setPermitirVinculoPorEmail(false);
		proveedorIdentidadService.crear(otro, datos);

		Usuario local = new Usuario();
		local.setTenant(otro);
		local.setEmail("operador.host@proveedores.com");
		local.setNombre("Alguien local");
		local.setEstado(EstadoUsuario.ACTIVO);
		local.setOrigenIdentidad(OrigenIdentidad.LOCAL);
		local.setClaveHash("hash-de-prueba");
		local.setAlta(Instant.now());
		usuarioRepository.save(local);

		IntercambioFederadoReqModel peticion = new IntercambioFederadoReqModel();
		peticion.setCodigoTenant(otro.getCodigo());
		peticion.setProveedor("FOLLOW");
		peticion.setToken(tokenDe("operador.host"));

		assertThatThrownBy(() -> federacionIdentidadService.intercambiar(peticion))
				.isInstanceOf(ProhibidoException.class)
				.hasMessageContaining("sin identidad federada");
		assertThat(usuarioRepository.findById(local.getId()).orElseThrow().getOrigenIdentidad())
				.isEqualTo(OrigenIdentidad.LOCAL);
	}

	@Test
	@DisplayName("un proveedor con emisor http remoto no se puede dar de alta")
	void elEmisorInseguroSeRechaza() {
		NuevoProveedorIdentidadReqModel datos = base("OIDC");
		datos.setEmisor("http://idp.inseguro.com/realms/x");
		assertThatThrownBy(() -> proveedorIdentidadService.crear(tenant, datos))
				.isInstanceOf(ValidacionException.class)
				.hasMessageContaining("debe ser https");
	}

	private List<Usuario> usuariosDelTenant() {
		return usuarioRepository.findAll().stream()
				.filter(usuario -> usuario.getTenant().getId().equals(tenant.getId())).toList();
	}

	private CodigoEmbedModel intercambiar(String token, String urlRetorno) {
		IntercambioFederadoReqModel datos = new IntercambioFederadoReqModel();
		datos.setCodigoTenant(tenant.getCodigo());
		datos.setProveedor(proveedor.getCodigo());
		datos.setToken(token);
		datos.setTipoObjeto("Caso");
		datos.setIdObjeto("CASO-4477");
		datos.setUrlRetorno(urlRetorno);
		return federacionIdentidadService.intercambiar(datos);
	}

	private ProveedorIdentidadModel crearProveedor(String codigo,
			java.util.function.Consumer<NuevoProveedorIdentidadReqModel> ajuste) {
		NuevoProveedorIdentidadReqModel datos = base(codigo);
		ajuste.accept(datos);
		return proveedorIdentidadService.crear(tenant, datos);
	}

	private NuevoProveedorIdentidadReqModel base(String codigo) {
		NuevoProveedorIdentidadReqModel datos = new NuevoProveedorIdentidadReqModel();
		datos.setCodigo(codigo);
		datos.setNombre("Identidad de " + codigo);
		datos.setOrigen(codigo.equals("OIDC") ? "OIDC" : codigo);
		datos.setEmisor(EMISOR);
		datos.setUrlJwks(JWKS);
		datos.setAudiencia("follow-host");
		return datos;
	}

	private String tokenDe(String usuario) throws Exception {
		return tokenDe("follow-host", usuario);
	}

	private String tokenDe(String cliente, String usuario) throws Exception {
		String cuerpo = "grant_type=password&client_id=" + cliente + "&client_secret=secreto-de-prueba"
				+ "&username=" + usuario + "&password=clave-de-prueba";
		HttpRequest peticion = HttpRequest
				.newBuilder(URI.create(BASE + "/protocol/openid-connect/token"))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.timeout(Duration.ofSeconds(15))
				.POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8)).build();
		HttpResponse<String> respuesta = HttpClient.newHttpClient().send(peticion,
				HttpResponse.BodyHandlers.ofString());
		if (respuesta.statusCode() >= 300) {
			throw new IllegalStateException("Keycloak respondio " + respuesta.statusCode() + ": "
					+ respuesta.body());
		}
		return MAPEADOR.readTree(respuesta.body()).get("access_token").asText();
	}

	private void exigirIdp() {
		try (java.net.Socket sonda = new java.net.Socket()) {
			sonda.connect(new java.net.InetSocketAddress(HOST, Integer.parseInt(PUERTO)), 2000);
		}
		catch (Exception e) {
			String mensaje = "No hay un proveedor de identidad de pruebas en " + HOST + ":" + PUERTO
					+ ". Levantalo con: docker compose up -d keycloak";
			if (Boolean.parseBoolean(variableDePrueba("NEXTDOCS_PRUEBA_OBLIGATORIA", "false"))) {
				throw new IllegalStateException(mensaje, e);
			}
			Assumptions.abort(mensaje);
		}
	}

	private static String variableDePrueba(String nombre, String porDefecto) {
		String valor = System.getenv(nombre);
		if (valor == null || valor.isBlank()) {
			valor = System.getProperty(nombre);
		}
		return valor == null || valor.isBlank() ? porDefecto : valor;
	}
}
