package com.nextdocs.ai.integracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.enumeraciones.AccionAuditoria;
import com.nextdocs.ai.enumeraciones.EstadoUsuario;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.RegistroExistenteException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.CambioClaveReqModel;
import com.nextdocs.ai.modelos.CuentaServicioCreadaModel;
import com.nextdocs.ai.modelos.CuentaServicioReqModel;
import com.nextdocs.ai.modelos.RolModel;
import com.nextdocs.ai.modelos.RolReqModel;
import com.nextdocs.ai.modelos.TenantReqModel;
import com.nextdocs.ai.modelos.UsuarioModel;
import com.nextdocs.ai.modelos.UsuarioReqModel;
import com.nextdocs.ai.repositorios.UsuarioRepository;
import com.nextdocs.ai.servicios.CuentaServicioService;
import com.nextdocs.ai.servicios.GobernanzaService;
import com.nextdocs.ai.servicios.RolService;
import com.nextdocs.ai.servicios.TenantService;
import com.nextdocs.ai.servicios.UsuarioService;
import com.nextdocs.ai.utiles.Permiso;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

class AdministracionIT extends PruebaIntegracion {

	private static final String CLAVE_VALIDA = "clave-de-prueba-larga";

	@Autowired
	private FabricaDatosPrueba fabrica;

	@Autowired
	private UsuarioService usuarioService;

	@Autowired
	private RolService rolService;

	@Autowired
	private CuentaServicioService cuentaServicioService;

	@Autowired
	private TenantService tenantService;

	@Autowired
	private GobernanzaService gobernanzaService;

	@Autowired
	private UsuarioRepository usuarioRepository;

	private Tenant tenant;

	private Usuario administrador;

	@BeforeEach
	void preparar() {
		tenant = fabrica.crearTenant("t" + UUID.randomUUID().toString().substring(0, 8));
		administrador = fabrica.administradorDe(tenant);
	}

	@Test
	@DisplayName("crear un usuario lo deja activo con sus roles y sin exponer la clave")
	void altaDeUsuario() {
		UsuarioModel usuario = crearUsuario("operador@prueba.test", Permiso.CODIGO_ROL_OPERADOR);

		assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
		assertThat(usuario.getRoles()).containsExactly(Permiso.CODIGO_ROL_OPERADOR);
		assertThat(usuario.getPermisos()).contains(Permiso.DOCUMENTOS_LEER).doesNotContain(Permiso.TENANT_ADMINISTRAR);
		assertThat(usuario.isAdministrador()).isFalse();
		assertThat(gobernanzaService.porRecurso(tenant.getId(), "Usuario", usuario.getId()))
				.anyMatch(evento -> evento.getAccion() == AccionAuditoria.USUARIO_CREADO);
	}

	@Test
	@DisplayName("el email de un usuario es unico dentro del tenant")
	void emailDuplicadoSeRechaza() {
		crearUsuario("repetido@prueba.test", Permiso.CODIGO_ROL_OPERADOR);

		assertThatThrownBy(() -> crearUsuario("repetido@prueba.test", Permiso.CODIGO_ROL_REVISOR))
				.isInstanceOf(RegistroExistenteException.class);
	}

	@Test
	@DisplayName("el mismo email puede existir en dos tenants distintos")
	void emailSeRepiteEntreTenants() {
		crearUsuario("compartido@prueba.test", Permiso.CODIGO_ROL_OPERADOR);
		Tenant otro = fabrica.crearTenant("x" + UUID.randomUUID().toString().substring(0, 8));

		UsuarioReqModel datos = usuarioReq("compartido@prueba.test", Permiso.CODIGO_ROL_OPERADOR);

		assertThat(usuarioService.crear(otro, datos).getId()).isNotBlank();
	}

	@Test
	@DisplayName("una clave corta se rechaza")
	void claveCortaSeRechaza() {
		UsuarioReqModel datos = usuarioReq("corta@prueba.test", Permiso.CODIGO_ROL_OPERADOR);
		datos.setClave("corta");

		assertThatThrownBy(() -> usuarioService.crear(tenant, datos)).isInstanceOf(ValidacionException.class)
				.hasMessageContaining("al menos");
	}

	@Test
	@DisplayName("un rol inexistente se rechaza al asignarlo")
	void rolInexistenteSeRechaza() {
		UsuarioReqModel datos = usuarioReq("sinrol@prueba.test", "NO_EXISTE");

		assertThatThrownBy(() -> usuarioService.crear(tenant, datos)).isInstanceOf(ValidacionException.class)
				.hasMessageContaining("no existe en este tenant");
	}

	@Test
	@DisplayName("bloquear al unico administrador se rechaza para no dejar el tenant sin gobierno")
	void noSePuedeDejarElTenantSinAdministradores() {
		UsuarioModel otro = crearUsuario("operador2@prueba.test", Permiso.CODIGO_ROL_OPERADOR);
		Usuario actor = usuarioRepository.findById(otro.getId()).orElseThrow();

		assertThatThrownBy(() -> usuarioService.bloquear(tenant.getId(), administrador.getId(), actor, "prueba"))
				.isInstanceOf(ValidacionException.class).hasMessageContaining("sin ningun administrador activo");
	}

	@Test
	@DisplayName("quitarle el rol de administrador al ultimo administrador se rechaza")
	void noSePuedeDegradarAlUltimoAdministrador() {
		assertThatThrownBy(() -> usuarioService.asignarRoles(tenant.getId(), administrador.getId(),
				List.of(Permiso.CODIGO_ROL_OPERADOR))).isInstanceOf(ValidacionException.class)
						.hasMessageContaining("sin ningun administrador activo");
	}

	@Test
	@DisplayName("con un segundo administrador si se puede bloquear al primero")
	void conDosAdministradoresSePuedeBloquear() {
		UsuarioModel segundo = crearUsuario("admin2@prueba.test", Permiso.CODIGO_ROL_ADMINISTRADOR);
		Usuario actor = usuarioRepository.findById(segundo.getId()).orElseThrow();

		UsuarioModel bloqueado = usuarioService.bloquear(tenant.getId(), administrador.getId(), actor, "rotacion");

		assertThat(bloqueado.getEstado()).isEqualTo(EstadoUsuario.BLOQUEADO);
	}

	@Test
	@DisplayName("un usuario no puede bloquearse a si mismo")
	void nadieSeBloqueaASiMismo() {
		crearUsuario("admin3@prueba.test", Permiso.CODIGO_ROL_ADMINISTRADOR);

		assertThatThrownBy(() -> usuarioService.bloquear(tenant.getId(), administrador.getId(), administrador,
				"prueba")).isInstanceOf(ValidacionException.class).hasMessageContaining("a si mismo");
	}

	@Test
	@DisplayName("un usuario bloqueado se puede desbloquear y vuelve a estar activo")
	void bloquearYDesbloquear() {
		UsuarioModel operador = crearUsuario("operador4@prueba.test", Permiso.CODIGO_ROL_OPERADOR);
		usuarioService.bloquear(tenant.getId(), operador.getId(), administrador, "vacaciones");

		UsuarioModel activo = usuarioService.desbloquear(tenant.getId(), operador.getId());

		assertThat(activo.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
		assertThat(gobernanzaService.porRecurso(tenant.getId(), "Usuario", operador.getId()))
				.anyMatch(evento -> evento.getAccion() == AccionAuditoria.USUARIO_BLOQUEADO)
				.anyMatch(evento -> evento.getAccion() == AccionAuditoria.USUARIO_DESBLOQUEADO);
	}

	@Test
	@DisplayName("un usuario dado de baja deja de aparecer y no se puede obtener")
	void bajaDeUsuario() {
		UsuarioModel operador = crearUsuario("operador5@prueba.test", Permiso.CODIGO_ROL_OPERADOR);

		usuarioService.eliminar(tenant.getId(), operador.getId(), administrador);

		assertThatThrownBy(() -> usuarioService.obtener(tenant.getId(), operador.getId()))
				.isInstanceOf(EntidadNoEncontradaException.class);
		assertThat(usuarioService.listar(tenant.getId(), null, null, PageRequest.of(0, 50)).getContent())
				.noneMatch(modelo -> modelo.getId().equals(operador.getId()));
	}

	@Test
	@DisplayName("cambiar la clave propia exige la actual y rechaza repetirla")
	void cambioDeClavePropia() {
		UsuarioModel operador = crearUsuario("operador6@prueba.test", Permiso.CODIGO_ROL_OPERADOR);
		Usuario actor = usuarioRepository.findById(operador.getId()).orElseThrow();

		CambioClaveReqModel equivocada = new CambioClaveReqModel();
		equivocada.setClaveActual("no-es-la-clave");
		equivocada.setClaveNueva("otra-clave-larga");
		assertThatThrownBy(() -> usuarioService.cambiarClave(tenant.getId(), actor, equivocada))
				.isInstanceOf(ValidacionException.class).hasMessageContaining("no es correcta");

		CambioClaveReqModel repetida = new CambioClaveReqModel();
		repetida.setClaveActual(CLAVE_VALIDA);
		repetida.setClaveNueva(CLAVE_VALIDA);
		assertThatThrownBy(() -> usuarioService.cambiarClave(tenant.getId(), actor, repetida))
				.isInstanceOf(ValidacionException.class).hasMessageContaining("distinta de la actual");

		CambioClaveReqModel correcta = new CambioClaveReqModel();
		correcta.setClaveActual(CLAVE_VALIDA);
		correcta.setClaveNueva("clave-nueva-valida");
		usuarioService.cambiarClave(tenant.getId(), actor, correcta);

		assertThat(gobernanzaService.porRecurso(tenant.getId(), "Usuario", operador.getId()))
				.anyMatch(evento -> evento.getAccion() == AccionAuditoria.CLAVE_MODIFICADA && evento.isExitoso());
	}

	@Test
	@DisplayName("un administrador restablece la clave sin conocer la anterior y queda auditado")
	void restablecerClave() {
		UsuarioModel operador = crearUsuario("operador7@prueba.test", Permiso.CODIGO_ROL_OPERADOR);

		usuarioService.restablecerClave(tenant.getId(), operador.getId(), "clave-restablecida", administrador);

		assertThat(gobernanzaService.porRecurso(tenant.getId(), "Usuario", operador.getId()))
				.anyMatch(evento -> evento.getAccion() == AccionAuditoria.CLAVE_RESTABLECIDA
						&& evento.getDetalle().contains(administrador.getEmail()));
	}

	@Test
	@DisplayName("un rol predefinido no se modifica ni se elimina")
	void rolPredefinidoEsInmutable() {
		RolModel operador = rolService.listar(tenant.getId()).stream()
				.filter(rol -> Permiso.CODIGO_ROL_OPERADOR.equals(rol.getCodigo())).findFirst().orElseThrow();

		assertThat(operador.isPredefinido()).isTrue();
		assertThatThrownBy(() -> rolService.actualizar(tenant.getId(), operador.getId(),
				rolReq("OPERADOR", Permiso.DOCUMENTOS_LEER))).isInstanceOf(ValidacionException.class)
						.hasMessageContaining("no se modifica");
		assertThatThrownBy(() -> rolService.eliminar(tenant.getId(), operador.getId()))
				.isInstanceOf(ValidacionException.class);
	}

	@Test
	@DisplayName("un rol propio se crea, se modifica y se elimina si no tiene usuarios")
	void cicloDeRolPropio() {
		RolModel rol = rolService.crear(tenant, rolReq("SOLO_LECTURA", Permiso.DOCUMENTOS_LEER));

		assertThat(rol.isPredefinido()).isFalse();
		assertThat(rol.getPermisos()).containsExactly(Permiso.DOCUMENTOS_LEER);

		RolModel modificado = rolService.actualizar(tenant.getId(), rol.getId(),
				rolReq("SOLO_LECTURA", Permiso.DOCUMENTOS_LEER, Permiso.PLANTILLAS_LEER));
		assertThat(modificado.getPermisos()).containsExactly(Permiso.DOCUMENTOS_LEER, Permiso.PLANTILLAS_LEER);

		rolService.eliminar(tenant.getId(), rol.getId());
		assertThat(rolService.listar(tenant.getId())).noneMatch(actual -> actual.getId().equals(rol.getId()));
	}

	@Test
	@DisplayName("un rol con usuarios asignados no se elimina")
	void rolEnUsoNoSeElimina() {
		RolModel rol = rolService.crear(tenant, rolReq("EN_USO", Permiso.DOCUMENTOS_LEER));
		crearUsuario("conrol@prueba.test", "EN_USO");

		assertThatThrownBy(() -> rolService.eliminar(tenant.getId(), rol.getId()))
				.isInstanceOf(ValidacionException.class).hasMessageContaining("esta asignado a 1 usuarios");
	}

	@Test
	@DisplayName("un permiso inventado se rechaza al crear un rol")
	void permisoInventadoSeRechaza() {
		assertThatThrownBy(() -> rolService.crear(tenant, rolReq("INVENTADO", "documentos.volar")))
				.isInstanceOf(ValidacionException.class).hasMessageContaining("no existe");
	}

	@Test
	@DisplayName("la clave de una cuenta de servicio se devuelve una sola vez y nunca se persiste en claro")
	void altaDeCuentaDeServicio() {
		CuentaServicioReqModel datos = new CuentaServicioReqModel();
		datos.setNombre("Integracion Follow");
		datos.setAlcances(List.of(Permiso.DOCUMENTOS_LEER, Permiso.DOCUMENTOS_ESCRIBIR));
		datos.setDiasVigencia(90);

		CuentaServicioCreadaModel creada = cuentaServicioService.crear(tenant, datos);

		assertThat(creada.getClave()).startsWith("ndai_");
		assertThat(creada.getCuenta().getPrefijoClave()).isEqualTo(creada.getClave().substring(0, 12));
		assertThat(creada.getCuenta().getExpira()).isNotNull();
		assertThat(cuentaServicioService.autenticar(creada.getClave())).isPresent();
		assertThat(cuentaServicioService.obtener(tenant.getId(), creada.getCuenta().getId()).getAlcances())
				.containsExactly(Permiso.DOCUMENTOS_ESCRIBIR, Permiso.DOCUMENTOS_LEER);
	}

	@Test
	@DisplayName("una cuenta de servicio no puede administrar el tenant")
	void cuentaDeServicioNoAdministraElTenant() {
		CuentaServicioReqModel datos = new CuentaServicioReqModel();
		datos.setNombre("Intento de escalada");
		datos.setAlcances(List.of(Permiso.TENANT_ADMINISTRAR));

		assertThatThrownBy(() -> cuentaServicioService.crear(tenant, datos))
				.isInstanceOf(ValidacionException.class).hasMessageContaining("no puede administrar el tenant");
	}

	@Test
	@DisplayName("una cuenta revocada deja de autenticar y exige motivo")
	void revocacionDeCuentaDeServicio() {
		CuentaServicioReqModel datos = new CuentaServicioReqModel();
		datos.setNombre("A revocar");
		datos.setAlcances(List.of(Permiso.DOCUMENTOS_LEER));
		CuentaServicioCreadaModel creada = cuentaServicioService.crear(tenant, datos);

		assertThatThrownBy(
				() -> cuentaServicioService.revocar(tenant.getId(), creada.getCuenta().getId(), "  "))
				.isInstanceOf(ValidacionException.class).hasMessageContaining("motivo");

		cuentaServicioService.revocar(tenant.getId(), creada.getCuenta().getId(), "Rotacion de credenciales");

		assertThat(cuentaServicioService.autenticar(creada.getClave())).isEmpty();
		assertThatThrownBy(() -> cuentaServicioService.revocar(tenant.getId(), creada.getCuenta().getId(),
				"otra vez")).isInstanceOf(EntidadNoEncontradaException.class);
	}

	@Test
	@DisplayName("SEC-01: la administracion de un tenant no ve usuarios ni cuentas de otro")
	void sec01AdministracionAisladaPorTenant() {
		UsuarioModel propio = crearUsuario("propio@prueba.test", Permiso.CODIGO_ROL_OPERADOR);
		Tenant ajeno = fabrica.crearTenant("x" + UUID.randomUUID().toString().substring(0, 8));

		assertThat(usuarioService.listar(ajeno.getId(), null, null, PageRequest.of(0, 50)).getContent())
				.noneMatch(modelo -> modelo.getId().equals(propio.getId()));
		assertThatThrownBy(() -> usuarioService.obtener(ajeno.getId(), propio.getId()))
				.isInstanceOf(EntidadNoEncontradaException.class);
	}

	@Test
	@DisplayName("el tenant se consulta y se actualiza dejando el antes y el despues auditados")
	void administracionDelTenant() {
		TenantReqModel datos = new TenantReqModel();
		datos.setNombre("Nombre nuevo");
		datos.setPlan("ENTERPRISE");
		datos.setRegion("sa-east-1");
		datos.setDominio("cliente.test");
		datos.setCuotaAlmacenamientoBytes(1024L * 1024 * 1024);

		assertThat(tenantService.obtener(tenant.getId()).getUsuariosActivos()).isEqualTo(1);

		assertThat(tenantService.actualizar(tenant.getId(), datos).getPlan()).isEqualTo("ENTERPRISE");
		assertThat(gobernanzaService.porRecurso(tenant.getId(), "Tenant", tenant.getId()))
				.anyMatch(evento -> evento.getAccion() == AccionAuditoria.TENANT_MODIFICADO
						&& evento.getDetalle().contains("ENTERPRISE"));
	}

	private UsuarioModel crearUsuario(String email, String rol) {
		return usuarioService.crear(tenant, usuarioReq(email, rol));
	}

	private UsuarioReqModel usuarioReq(String email, String rol) {
		UsuarioReqModel datos = new UsuarioReqModel();
		datos.setEmail(email);
		datos.setNombre("Usuario " + email);
		datos.setClave(CLAVE_VALIDA);
		datos.setRoles(List.of(rol));
		return datos;
	}

	private RolReqModel rolReq(String codigo, String... permisos) {
		RolReqModel datos = new RolReqModel();
		datos.setCodigo(codigo);
		datos.setNombre("Rol " + codigo);
		datos.setDescripcion("Creado en la prueba");
		datos.setPermisos(List.of(permisos));
		return datos;
	}
}
