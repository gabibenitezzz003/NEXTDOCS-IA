package com.nextdocs.ai.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdocs.ai.config.PropiedadesFederacion;
import com.nextdocs.ai.config.PropiedadesSeguridad;
import com.nextdocs.ai.entidades.ProveedorIdentidad;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.enumeraciones.EstadoTenant;
import com.nextdocs.ai.enumeraciones.OrigenIdentidad;
import com.nextdocs.ai.exceptions.NoAutorizadoException;
import com.nextdocs.ai.exceptions.ProhibidoException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.CodigoEmbedModel;
import com.nextdocs.ai.modelos.IntercambioFederadoReqModel;
import com.nextdocs.ai.modelos.ProveedorOauthPublicoModel;
import com.nextdocs.ai.repositorios.ProveedorIdentidadRepository;
import com.nextdocs.ai.repositorios.TenantRepository;
import com.sun.net.httpserver.HttpServer;

import io.jsonwebtoken.Claims;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OauthLoginServiceTest {

	private static final String TENANT = "demo";

	private static final String PROVEEDOR = "google";

	private static final ObjectMapper MAPEADOR = new ObjectMapper();

	private ProveedorIdentidadRepository proveedorIdentidadRepository;

	private TenantRepository tenantRepository;

	private FederacionIdentidadService federacionIdentidadService;

	private VerificadorTokenIdpService verificadorTokenIdpService;

	private PropiedadesFederacion propiedades;

	private OauthLoginService servicio;

	private HttpServer servidorToken;

	private String urlToken;

	private volatile String cuerpoRecibido;

	@BeforeEach
	void preparar() throws Exception {
		proveedorIdentidadRepository = mock(ProveedorIdentidadRepository.class);
		tenantRepository = mock(TenantRepository.class);
		federacionIdentidadService = mock(FederacionIdentidadService.class);
		verificadorTokenIdpService = mock(VerificadorTokenIdpService.class);
		propiedades = new PropiedadesFederacion();
		propiedades.setTiempoEsperaMilisegundos(3000);
		propiedades.setUrlBaseApi("http://localhost:8090");
		propiedades.setUrlBasePortal("http://localhost:5175");
		PropiedadesSeguridad seguridad = new PropiedadesSeguridad();
		seguridad.setJwtSecreto("secreto-de-prueba-con-mas-de-32-bytes-seguro");
		servicio = new OauthLoginService(proveedorIdentidadRepository, tenantRepository,
				federacionIdentidadService, verificadorTokenIdpService, propiedades, seguridad, MAPEADOR,
				new MensajesService(MAPEADOR));
		servidorToken = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		servidorToken.createContext("/token", intercambio -> {
			cuerpoRecibido = new String(intercambio.getRequestBody().readAllBytes(),
					StandardCharsets.UTF_8);
			byte[] respuesta = "{\"id_token\":\"token-federado\"}".getBytes(StandardCharsets.UTF_8);
			intercambio.getResponseHeaders().add("Content-Type", "application/json");
			intercambio.sendResponseHeaders(200, respuesta.length);
			try (OutputStream salida = intercambio.getResponseBody()) {
				salida.write(respuesta);
			}
		});
		servidorToken.start();
		urlToken = "http://127.0.0.1:" + servidorToken.getAddress().getPort() + "/token";
	}

	@AfterEach
	void cerrar() {
		servidorToken.stop(0);
	}

	private ProveedorIdentidad proveedor() {
		ProveedorIdentidad proveedor = new ProveedorIdentidad();
		proveedor.setCodigo(PROVEEDOR);
		proveedor.setNombre("Google");
		proveedor.setOrigen(OrigenIdentidad.OIDC);
		proveedor.setEmisor("https://accounts.google.com");
		proveedor.setUrlJwks("https://www.googleapis.com/oauth2/v3/certs");
		proveedor.setClienteId("cliente-google");
		proveedor.setClienteSecreto("secreto-google");
		proveedor.setUrlAutorizacion("https://accounts.google.com/o/oauth2/v2/auth");
		proveedor.setUrlToken(urlToken);
		proveedor.setAlcances("openid email profile");
		proveedor.setOrigenesEmbedPermitidos("http://localhost:5175");
		proveedor.setActivo(true);
		return proveedor;
	}

	private void registrarProveedor(ProveedorIdentidad proveedor) {
		when(proveedorIdentidadRepository.buscarPorCodigoTenantYCodigo(TENANT, PROVEEDOR))
				.thenReturn(Optional.of(proveedor));
	}

	private JsonNode estadoDecodificado(String urlAutorizacion) throws Exception {
		String estado = parametro(urlAutorizacion, "state");
		String cuerpo = estado.substring(0, estado.lastIndexOf('.'));
		return MAPEADOR.readTree(Base64.getUrlDecoder().decode(cuerpo));
	}

	private String parametro(String url, String nombre) throws Exception {
		for (String parte : url.split("[?&]")) {
			if (parte.startsWith(nombre + "=")) {
				return URLDecoder.decode(parte.substring(nombre.length() + 1), StandardCharsets.UTF_8);
			}
		}
		throw new IllegalStateException("Falta el parametro " + nombre);
	}

	@Test
	@DisplayName("lista solo proveedores activos con OAuth completo")
	void listaProveedoresPublicos() {
		Tenant tenant = new Tenant();
		tenant.setEstado(EstadoTenant.ACTIVO);
		when(tenantRepository.findByCodigoAndBajaIsNull(TENANT)).thenReturn(Optional.of(tenant));
		ProveedorIdentidad completo = proveedor();
		ProveedorIdentidad sinOauth = new ProveedorIdentidad();
		sinOauth.setCodigo("embed");
		sinOauth.setNombre("Embed");
		sinOauth.setActivo(true);
		ProveedorIdentidad inactivo = proveedor();
		inactivo.setCodigo("inactivo");
		inactivo.setActivo(false);
		when(proveedorIdentidadRepository.listarPorTenant(tenant.getId()))
				.thenReturn(List.of(completo, sinOauth, inactivo));

		List<ProveedorOauthPublicoModel> modelos = servicio.listarPublicos(TENANT);

		assertThat(modelos).hasSize(1);
		assertThat(modelos.get(0).getCodigo()).isEqualTo(PROVEEDOR);
		assertThat(modelos.get(0).getNombre()).isEqualTo("Google");
	}

	@Test
	@DisplayName("lista vacia para tenant inexistente")
	void listaVaciaTenantInexistente() {
		when(tenantRepository.findByCodigoAndBajaIsNull("fantasma")).thenReturn(Optional.empty());

		assertThat(servicio.listarPublicos("fantasma")).isEmpty();
	}

	@Test
	@DisplayName("iniciar arma la URL de autorizacion con state firmado y nonce")
	void iniciarArmaUrl() throws Exception {
		registrarProveedor(proveedor());

		String url = servicio.iniciar(TENANT, PROVEEDOR, "http://localhost:5175");

		assertThat(url).startsWith("https://accounts.google.com/o/oauth2/v2/auth?");
		assertThat(parametro(url, "client_id")).isEqualTo("cliente-google");
		assertThat(parametro(url, "redirect_uri"))
				.isEqualTo("http://localhost:8090/api/v1/federacion/oauth/callback");
		assertThat(parametro(url, "scope")).isEqualTo("openid email profile");
		assertThat(parametro(url, "response_type")).isEqualTo("code");
		JsonNode estado = estadoDecodificado(url);
		assertThat(estado.get("t").asText()).isEqualTo(TENANT);
		assertThat(estado.get("p").asText()).isEqualTo(PROVEEDOR);
		assertThat(estado.get("n").asText()).isEqualTo(parametro(url, "nonce"));
		assertThat(estado.get("r").asText()).isEqualTo("http://localhost:5175");
	}

	@Test
	@DisplayName("iniciar rechaza un retorno que no esta en la lista permitida")
	void iniciarRechazaRetornoExtrano() {
		registrarProveedor(proveedor());

		assertThatThrownBy(() -> servicio.iniciar(TENANT, PROVEEDOR, "https://evil.example.com"))
				.isInstanceOf(ProhibidoException.class);
	}

	@Test
	@DisplayName("iniciar rechaza un proveedor sin configuracion OAuth")
	void iniciarRechazaSinOauth() {
		ProveedorIdentidad proveedor = proveedor();
		proveedor.setClienteId(null);
		registrarProveedor(proveedor);

		assertThatThrownBy(() -> servicio.iniciar(TENANT, PROVEEDOR, null))
				.isInstanceOf(ProhibidoException.class);
	}

	@Test
	@DisplayName("iniciar rechaza un proveedor desconocido")
	void iniciarRechazaProveedorDesconocido() {
		when(proveedorIdentidadRepository.buscarPorCodigoTenantYCodigo(TENANT, "fantasma"))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.iniciar(TENANT, "fantasma", null))
				.isInstanceOf(NoAutorizadoException.class);
	}

	@Test
	@DisplayName("iniciarSinTenant resuelve el proveedor unico activo sin pedir organizacion")
	void iniciarSinTenantResuelveUnico() throws Exception {
		Tenant tenant = new Tenant();
		tenant.setCodigo(TENANT);
		tenant.setEstado(EstadoTenant.ACTIVO);
		ProveedorIdentidad proveedor = proveedor();
		proveedor.setTenant(tenant);
		when(proveedorIdentidadRepository.listarPorCodigoGlobal(PROVEEDOR))
				.thenReturn(List.of(proveedor));

		String url = servicio.iniciarSinTenant(PROVEEDOR, "http://localhost:5175");

		assertThat(url).startsWith("https://accounts.google.com/o/oauth2/v2/auth?");
		assertThat(estadoDecodificado(url).get("t").asText()).isEqualTo(TENANT);
	}

	@Test
	@DisplayName("iniciarSinTenant rechaza cuando no hay proveedor configurado")
	void iniciarSinTenantSinProveedor() {
		when(proveedorIdentidadRepository.listarPorCodigoGlobal("fantasma"))
				.thenReturn(List.of());

		assertThatThrownBy(() -> servicio.iniciarSinTenant("fantasma", null))
				.isInstanceOf(NoAutorizadoException.class);
	}

	@Test
	@DisplayName("iniciarSinTenant pide la organizacion cuando el acceso es ambiguo")
	void iniciarSinTenantAmbiguo() {
		Tenant tenant = new Tenant();
		tenant.setCodigo(TENANT);
		tenant.setEstado(EstadoTenant.ACTIVO);
		ProveedorIdentidad primero = proveedor();
		primero.setTenant(tenant);
		Tenant otro = new Tenant();
		otro.setCodigo("otro");
		otro.setEstado(EstadoTenant.ACTIVO);
		ProveedorIdentidad segundo = proveedor();
		segundo.setTenant(otro);
		when(proveedorIdentidadRepository.listarPorCodigoGlobal(PROVEEDOR))
				.thenReturn(List.of(primero, segundo));

		assertThatThrownBy(() -> servicio.iniciarSinTenant(PROVEEDOR, null))
				.isInstanceOf(ValidacionException.class);
	}

	@Test
	@DisplayName("callback con state alterado redirige al login con error")
	void callbackRechazaEstadoAlterado() throws Exception {
		registrarProveedor(proveedor());
		String url = servicio.iniciar(TENANT, PROVEEDOR, null);
		String estado = parametro(url, "state");
		String alterado = estado.substring(0, estado.length() - 2) + "XX";

		String destino = servicio.resolverCallback("codigo-cualquiera", alterado, null);

		assertThat(destino).startsWith("http://localhost:5175/ingresar?errorFederado=");
	}

	@Test
	@DisplayName("callback con error del proveedor redirige al login con el mensaje")
	void callbackPropagaErrorIdp() throws Exception {
		registrarProveedor(proveedor());
		String url = servicio.iniciar(TENANT, PROVEEDOR, null);
		String estado = parametro(url, "state");

		String destino = servicio.resolverCallback(null, estado, "access_denied");

		assertThat(destino).startsWith("http://localhost:5175/ingresar?errorFederado=");
		assertThat(URLDecoder.decode(destino, StandardCharsets.UTF_8)).contains("access_denied");
	}

	@Test
	@DisplayName("callback feliz canjea el codigo, verifica el token y redirige al portal")
	void callbackFeliz() throws Exception {
		ProveedorIdentidad proveedor = proveedor();
		registrarProveedor(proveedor);
		String url = servicio.iniciar(TENANT, PROVEEDOR, "http://localhost:5175");
		String estado = parametro(url, "state");
		String nonce = estadoDecodificado(url).get("n").asText();
		Claims claims = mock(Claims.class);
		when(claims.get("nonce", String.class)).thenReturn(nonce);
		when(verificadorTokenIdpService.verificar(proveedor, "token-federado")).thenReturn(claims);
		CodigoEmbedModel codigo = new CodigoEmbedModel();
		codigo.setCodigo("codigo-de-un-uso");
		when(federacionIdentidadService.intercambiar(any())).thenReturn(codigo);

		String destino = servicio.resolverCallback("codigo-google", estado, null);

		assertThat(destino)
				.isEqualTo("http://localhost:5175/ingresar?codigo=" + URLEncoder.encode("codigo-de-un-uso", StandardCharsets.UTF_8));
		assertThat(cuerpoRecibido).contains("grant_type=authorization_code");
		assertThat(cuerpoRecibido).contains("code=codigo-google");
		assertThat(cuerpoRecibido).contains("client_secret=secreto-google");
		ArgumentCaptor<IntercambioFederadoReqModel> captor = ArgumentCaptor
				.forClass(IntercambioFederadoReqModel.class);
		org.mockito.Mockito.verify(federacionIdentidadService).intercambiar(captor.capture());
		assertThat(captor.getValue().getCodigoTenant()).isEqualTo(TENANT);
		assertThat(captor.getValue().getProveedor()).isEqualTo(PROVEEDOR);
		assertThat(captor.getValue().getToken()).isEqualTo("token-federado");
	}

	@Test
	@DisplayName("callback rechaza un id_token con nonce que no coincide")
	void callbackRechazaNonceDistinto() throws Exception {
		ProveedorIdentidad proveedor = proveedor();
		registrarProveedor(proveedor);
		String url = servicio.iniciar(TENANT, PROVEEDOR, null);
		String estado = parametro(url, "state");
		Claims claims = mock(Claims.class);
		when(claims.get("nonce", String.class)).thenReturn("nonce-inyectado");
		when(verificadorTokenIdpService.verificar(proveedor, "token-federado")).thenReturn(claims);

		String destino = servicio.resolverCallback("codigo-google", estado, null);

		assertThat(destino).startsWith("http://localhost:5175/ingresar?errorFederado=");
		org.mockito.Mockito.verify(federacionIdentidadService, org.mockito.Mockito.never())
				.intercambiar(any());
	}
}
