package com.nextdocs.ai.integracion;

import static com.nextdocs.ai.integracion.FabricaDatosPrueba.campo;
import static com.nextdocs.ai.integracion.FabricaDatosPrueba.construirPdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import com.nextdocs.ai.entidades.AdjuntoCorreo;
import com.nextdocs.ai.entidades.CorrelacionCorreo;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.enumeraciones.EstadoMensajeSaliente;
import com.nextdocs.ai.enumeraciones.OrigenDocumento;
import com.nextdocs.ai.enumeraciones.ResultadoAdjuntoCorreo;
import com.nextdocs.ai.enumeraciones.ResultadoMensajeCorreo;
import com.nextdocs.ai.enumeraciones.TipoDatoCampo;
import com.nextdocs.ai.enumeraciones.TipoExcepcion;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.BuzonCorreoModel;
import com.nextdocs.ai.modelos.CorrelacionCorreoModel;
import com.nextdocs.ai.modelos.LecturaBuzonModel;
import com.nextdocs.ai.modelos.MensajeCorreoModel;
import com.nextdocs.ai.modelos.MensajeSalienteModel;
import com.nextdocs.ai.modelos.NuevaCorrelacionCorreoReqModel;
import com.nextdocs.ai.modelos.NuevoBuzonCorreoReqModel;
import com.nextdocs.ai.modelos.RemitenteAutorizadoModel;
import com.nextdocs.ai.repositorios.AdjuntoCorreoRepository;
import com.nextdocs.ai.repositorios.CorrelacionCorreoRepository;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.repositorios.ExcepcionDocumentalRepository;
import com.nextdocs.ai.servicios.BuzonCorreoService;
import com.nextdocs.ai.servicios.IngestaCorreoService;
import com.nextdocs.ai.servicios.TrabajadorCorreoService;
import com.nextdocs.ai.utiles.TokenCorrelacion;

import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

class CanalCorreoIT extends PruebaIntegracion {

	private static final String HOST = variableDePrueba("NEXTDOCS_PRUEBA_CORREO_HOST", "localhost");

	private static final int PUERTO_SMTP = Integer
			.parseInt(variableDePrueba("NEXTDOCS_PRUEBA_CORREO_SMTP", "3027"));

	private static final int PUERTO_IMAP = Integer
			.parseInt(variableDePrueba("NEXTDOCS_PRUEBA_CORREO_IMAP", "3145"));

	@Autowired
	private FabricaDatosPrueba fabrica;

	@Autowired
	private BuzonCorreoService buzonCorreoService;

	@Autowired
	private TrabajadorCorreoService trabajadorCorreoService;

	@Autowired
	private DocumentoRepository documentoRepository;

	@Autowired
	private com.nextdocs.ai.servicios.DocumentoService documentoService;

	@Autowired
	private AdjuntoCorreoRepository adjuntoCorreoRepository;

	@Autowired
	private CorrelacionCorreoRepository correlacionCorreoRepository;

	@Autowired
	private ExcepcionDocumentalRepository excepcionDocumentalRepository;

	private Tenant tenant;

	private BuzonCorreoModel buzon;

	private String direccion;

	@BeforeEach
	void prepararEscenario() {
		exigirServidorDeCorreo();
		String sufijo = UUID.randomUUID().toString().substring(0, 8);
		tenant = fabrica.crearTenant("correo-" + sufijo);
		fabrica.crearPlantillaPublicada(tenant, "REMITO",
				List.of(campo("numero", TipoDatoCampo.TEXTO, true)), List.of(), new BigDecimal("0.9500"));
		direccion = "casos-" + sufijo + "@nextdocs.local";
		buzon = crearBuzon(direccion, true, false);
	}

	@Test
	@DisplayName("un correo con token vigente ingesta el adjunto y lo asocia al sujeto de la solicitud")
	void elTokenVigenteAsocia() throws Exception {
		CorrelacionCorreoModel correlacion = solicitar("FOLLOW", "Caso", "CASO-4477");
		autorizar("proveedor@ejemplo.com");

		enviar("proveedor@ejemplo.com", direccion,
				"Re: Documentacion requerida " + TokenCorrelacion.etiquetar(correlacion.getToken()),
				"remito.pdf", construirPdf(List.of("REMITO 4477")));

		MensajeCorreoModel mensaje = procesarUnico();
		assertThat(mensaje.getResultado()).isEqualTo(ResultadoMensajeCorreo.INGESTADO.name());
		assertThat(mensaje.getTokenDetectado()).isEqualTo(correlacion.getToken());
		assertThat(mensaje.getIngestados()).isEqualTo(1);

		Documento documento = documentoDe(mensaje);
		assertThat(documento.getOrigen()).isEqualTo(OrigenDocumento.EMAIL);
		assertThat(documento.getRemitente()).isEqualTo("proveedor@ejemplo.com");
		assertThat(documento.getReferenciaSujeto().getIdObjeto()).isEqualTo("CASO-4477");
		assertThat(documento.getReferenciaSujeto().getTipoObjeto()).isEqualTo("Caso");
		assertThat(documentoService.obtener(tenant.getId(), documento.getId()).getCodigoPlantilla())
				.isEqualTo("REMITO");

		CorrelacionCorreo actualizada = correlacionCorreoRepository.findById(correlacion.getId()).orElseThrow();
		assertThat(actualizada.getDocumentosRecibidos()).isEqualTo(1);
		assertThat(actualizada.getUltimoUso()).isNotNull();
	}

	@Test
	@DisplayName("el token de otro tenant no resuelve: el documento entra sin asociar y abre excepcion")
	void elTokenDeOtroTenantNoResuelve() throws Exception {
		Tenant ajeno = fabrica.crearTenant("ajeno-" + UUID.randomUUID().toString().substring(0, 8));
		String direccionAjena = "casos-ajeno-" + UUID.randomUUID().toString().substring(0, 8) + "@nextdocs.local";
		BuzonCorreoModel buzonAjeno = crearBuzonPara(ajeno, direccionAjena, false, false);

		NuevaCorrelacionCorreoReqModel pedido = new NuevaCorrelacionCorreoReqModel();
		pedido.setBuzonId(buzonAjeno.getId());
		pedido.setSujetoOrigen("FOLLOW");
		pedido.setSujetoTipoObjeto("Caso");
		pedido.setSujetoIdObjeto("CASO-DEL-OTRO");
		pedido.setDiasVigencia(30);
		CorrelacionCorreoModel delOtroTenant = buzonCorreoService.crearCorrelacion(ajeno, pedido);

		autorizar("proveedor@ejemplo.com");
		enviar("proveedor@ejemplo.com", direccion,
				"Re: adjunto " + TokenCorrelacion.etiquetar(delOtroTenant.getToken()), "remito.pdf",
				construirPdf(List.of("REMITO 1")));

		MensajeCorreoModel mensaje = procesarUnico();
		assertThat(mensaje.getResultado()).isEqualTo(ResultadoMensajeCorreo.SIN_CORRELACION.name());
		assertThat(mensaje.getCorrelacionId()).isNull();
		assertThat(mensaje.getIngestados()).isEqualTo(1);

		Documento documento = documentoDe(mensaje);
		assertThat(documento.getTenant().getId()).isEqualTo(tenant.getId());
		assertThat(sujetoDe(documento)).isNull();
		assertThat(excepcionDocumentalRepository.listarPorDocumento(documento.getId()))
				.anyMatch(excepcion -> excepcion.getTipo() == TipoExcepcion.ASOCIACION
						&& IngestaCorreoService.CODIGO_SIN_CORRELACION.equals(excepcion.getCodigo()));

		CorrelacionCorreo intacta = correlacionCorreoRepository.findById(delOtroTenant.getId()).orElseThrow();
		assertThat(intacta.getDocumentosRecibidos()).isZero();
	}

	@Test
	@DisplayName("un remitente fuera de la lista no ingesta ningun adjunto")
	void elRemitenteNoAutorizadoNoIngesta() throws Exception {
		autorizar("@proveedores.com");
		enviar("cualquiera@internet.com", direccion, "Te mando esto", "remito.pdf",
				construirPdf(List.of("REMITO")));

		MensajeCorreoModel mensaje = procesarUnico();
		assertThat(mensaje.getResultado()).isEqualTo(ResultadoMensajeCorreo.REMITENTE_NO_AUTORIZADO.name());
		assertThat(mensaje.getIngestados()).isZero();
		assertThat(mensaje.getAdjuntos()).isZero();
		assertThat(adjuntoCorreoRepository.listarPorMensaje(mensaje.getId())).isEmpty();
		assertThat(documentosDelTenant()).isEmpty();
	}

	@Test
	@DisplayName("la lista blanca acepta un dominio completo")
	void elDominioAutorizadoAlcanza() throws Exception {
		autorizar("@proveedores.com");
		enviar("quien.sea@proveedores.com", direccion, "Documentacion", "remito.pdf",
				construirPdf(List.of("REMITO")));

		MensajeCorreoModel mensaje = procesarUnico();
		assertThat(mensaje.getResultado()).isEqualTo(ResultadoMensajeCorreo.INGESTADO.name());
		assertThat(mensaje.getIngestados()).isEqualTo(1);
	}

	@Test
	@DisplayName("un adjunto con tipo no permitido queda auditado y no crea documento")
	void elTipoNoPermitidoQuedaAuditado() throws Exception {
		autorizar("proveedor@ejemplo.com");
		enviar("proveedor@ejemplo.com", direccion, "Planilla", "listado.xlsx",
				"no soy un pdf".getBytes(StandardCharsets.UTF_8));

		MensajeCorreoModel mensaje = procesarUnico();
		assertThat(mensaje.getResultado()).isEqualTo(ResultadoMensajeCorreo.RECHAZADO.name());
		assertThat(mensaje.getRechazados()).isEqualTo(1);

		List<AdjuntoCorreo> adjuntos = adjuntoCorreoRepository.listarPorMensaje(mensaje.getId());
		assertThat(adjuntos).hasSize(1);
		assertThat(adjuntos.get(0).getResultado()).isEqualTo(ResultadoAdjuntoCorreo.RECHAZADO);
		assertThat(adjuntos.get(0).getCodigoRechazo()).isEqualTo("EXTENSION_NO_PERMITIDA");
		assertThat(adjuntos.get(0).getDocumento()).isNull();
		assertThat(adjuntos.get(0).getSha256()).isNotBlank();
		assertThat(documentosDelTenant()).isEmpty();
	}

	@Test
	@DisplayName("un mensaje con dos adjuntos, uno valido y uno rechazado, queda PARCIAL")
	void elMensajeMixtoQuedaParcial() throws Exception {
		CorrelacionCorreoModel correlacion = solicitar("FOLLOW", "Caso", "CASO-MIXTO");
		autorizar("proveedor@ejemplo.com");
		enviarVarios("proveedor@ejemplo.com", direccion,
				"Re: envio " + TokenCorrelacion.etiquetar(correlacion.getToken()),
				List.of(adjunto("remito.pdf", construirPdf(List.of("REMITO"))),
						adjunto("nota.xlsx", "texto".getBytes(StandardCharsets.UTF_8))));

		MensajeCorreoModel mensaje = procesarUnico();
		assertThat(mensaje.getResultado()).isEqualTo(ResultadoMensajeCorreo.PARCIAL.name());
		assertThat(mensaje.getIngestados()).isEqualTo(1);
		assertThat(mensaje.getRechazados()).isEqualTo(1);
	}

	@Test
	@DisplayName("un token vencido no asocia y deja el documento para revision humana")
	void elTokenVencidoNoAsocia() throws Exception {
		CorrelacionCorreoModel correlacion = solicitar("FOLLOW", "Caso", "CASO-VIEJO");
		CorrelacionCorreo entidad = correlacionCorreoRepository.findById(correlacion.getId()).orElseThrow();
		entidad.setVenceEn(Instant.now().minus(1, ChronoUnit.DAYS));
		correlacionCorreoRepository.save(entidad);

		autorizar("proveedor@ejemplo.com");
		enviar("proveedor@ejemplo.com", direccion,
				"Re: tarde " + TokenCorrelacion.etiquetar(correlacion.getToken()), "remito.pdf",
				construirPdf(List.of("REMITO")));

		MensajeCorreoModel mensaje = procesarUnico();
		assertThat(mensaje.getResultado()).isEqualTo(ResultadoMensajeCorreo.SIN_CORRELACION.name());
		assertThat(mensaje.getMotivo()).contains("no corresponde a una solicitud vigente");
		assertThat(sujetoDe(documentoDe(mensaje))).isNull();
	}

	@Test
	@DisplayName("el mismo mensaje leido dos veces no duplica documentos")
	void elMensajeDuplicadoNoSeProcesaDosVeces() throws Exception {
		autorizar("proveedor@ejemplo.com");
		enviar("proveedor@ejemplo.com", direccion, "Envio unico", "remito.pdf",
				construirPdf(List.of("REMITO")));

		LecturaBuzonModel primera = trabajadorCorreoService.revisar(buzon.getId());
		assertThat(primera.getMensajesProcesados()).isEqualTo(1);

		LecturaBuzonModel segunda = trabajadorCorreoService.revisar(buzon.getId());
		assertThat(segunda.getMensajesLeidos()).isZero();
		assertThat(documentosDelTenant()).hasSize(1);
	}

	@Test
	@DisplayName("un correo sin adjuntos se registra y no ingesta nada")
	void sinAdjuntosNoIngesta() throws Exception {
		autorizar("proveedor@ejemplo.com");
		enviarVarios("proveedor@ejemplo.com", direccion, "Consulta sin adjuntos", List.of());

		MensajeCorreoModel mensaje = procesarUnico();
		assertThat(mensaje.getResultado()).isEqualTo(ResultadoMensajeCorreo.SIN_ADJUNTOS.name());
		assertThat(documentosDelTenant()).isEmpty();
	}

	@Test
	@DisplayName("el buzon que no exige correlacion ingesta sin asociar y sin abrir excepcion")
	void sinCorrelacionExigidaIngestaLibre() throws Exception {
		String abierta = "abierto-" + UUID.randomUUID().toString().substring(0, 8) + "@nextdocs.local";
		BuzonCorreoModel permisivo = crearBuzon(abierta, false, false);
		enviar("cualquiera@internet.com", abierta, "Sin token", "remito.pdf",
				construirPdf(List.of("REMITO")));

		MensajeCorreoModel mensaje = trabajadorCorreoService.revisar(permisivo.getId()).getMensajes().get(0);
		assertThat(mensaje.getResultado()).isEqualTo(ResultadoMensajeCorreo.INGESTADO.name());
		Documento documento = documentoDe(mensaje);
		assertThat(sujetoDe(documento)).isNull();
		assertThat(excepcionDocumentalRepository.listarPorDocumento(documento.getId()))
				.noneMatch(excepcion -> IngestaCorreoService.CODIGO_SIN_CORRELACION.equals(excepcion.getCodigo()));
	}

	@Test
	@DisplayName("el buzon que exige correlacion abre excepcion cuando el correo no trae token")
	void conCorrelacionExigidaAbreExcepcion() throws Exception {
		String otraDireccion = "estricto-" + UUID.randomUUID().toString().substring(0, 8) + "@nextdocs.local";
		BuzonCorreoModel estricto = crearBuzon(otraDireccion, false, true);

		enviar("cualquiera@internet.com", otraDireccion, "Sin token", "remito.pdf",
				construirPdf(List.of("REMITO")));

		LecturaBuzonModel lectura = trabajadorCorreoService.revisar(estricto.getId());
		MensajeCorreoModel mensaje = lectura.getMensajes().get(0);
		assertThat(mensaje.getResultado()).isEqualTo(ResultadoMensajeCorreo.SIN_CORRELACION.name());
		assertThat(mensaje.getMotivo()).contains("no trae token de correlacion");
		assertThat(excepcionDocumentalRepository.listarPorDocumento(documentoDe(mensaje).getId()))
				.anyMatch(excepcion -> IngestaCorreoService.CODIGO_SIN_CORRELACION.equals(excepcion.getCodigo()));
	}

	@Test
	@DisplayName("el token viaja tambien en la direccion con etiqueta")
	void elTokenViajaEnLaDireccion() throws Exception {
		CorrelacionCorreoModel correlacion = solicitar("FOLLOW", "Caso", "CASO-ETIQUETA");
		autorizar("proveedor@ejemplo.com");
		enviarConSubdireccion("proveedor@ejemplo.com", correlacion.getDireccionConEtiqueta(), direccion,
				"Sin codigo en el asunto", "remito.pdf", construirPdf(List.of("REMITO")));

		MensajeCorreoModel mensaje = procesarUnico();
		assertThat(mensaje.getTokenDetectado()).isEqualTo(correlacion.getToken());
		assertThat(mensaje.getResultado()).isEqualTo(ResultadoMensajeCorreo.INGESTADO.name());
		assertThat(sujetoDe(documentoDe(mensaje))).isEqualTo("CASO-ETIQUETA");
	}

	@Test
	@DisplayName("la solicitud saliente deja message id, plantilla y estado en la auditoria")
	void laSalidaQuedaAuditada() {
		NuevaCorrelacionCorreoReqModel pedido = new NuevaCorrelacionCorreoReqModel();
		pedido.setBuzonId(buzon.getId());
		pedido.setSujetoOrigen("FOLLOW");
		pedido.setSujetoTipoObjeto("Caso");
		pedido.setSujetoIdObjeto("CASO-SALIDA");
		pedido.setCodigoPlantilla("REMITO");
		pedido.setDestinatario("proveedor@ejemplo.com");
		pedido.setDescripcion("Necesitamos el remito");
		pedido.setDiasVigencia(15);
		pedido.setEnviarSolicitud(true);

		CorrelacionCorreoModel correlacion = buzonCorreoService.crearCorrelacion(tenant, pedido);
		assertThat(correlacion.getMensajeSalienteId()).isNotNull();

		List<MensajeSalienteModel> salientes = buzonCorreoService
				.listarSalientes(tenant.getId(), PageRequest.of(0, 10)).getContent();
		assertThat(salientes).hasSize(1);
		MensajeSalienteModel saliente = salientes.get(0);
		assertThat(saliente.getEstado()).isEqualTo(EstadoMensajeSaliente.ENVIADO.name());
		assertThat(saliente.getIdentificadorMensaje()).startsWith("<").endsWith("@nextdocs-ai>");
		assertThat(saliente.getPlantilla()).isEqualTo("SOLICITUD_DOCUMENTACION");
		assertThat(saliente.getAsunto()).contains(TokenCorrelacion.etiquetar(correlacion.getToken()));
		assertThat(saliente.getEnviado()).isNotNull();
	}

	@Test
	@DisplayName("dos tenants no pueden reclamar la misma direccion de buzon")
	void laDireccionDelBuzonEsUnica() {
		Tenant otro = fabrica.crearTenant("otro-" + UUID.randomUUID().toString().substring(0, 8));
		assertThatThrownBy(() -> crearBuzonPara(otro, direccion, false, false))
				.isInstanceOf(ValidacionException.class)
				.hasMessageContaining("ya esta asignada a un buzon");
	}

	private void exigirServidorDeCorreo() {
		try (java.net.Socket sonda = new java.net.Socket()) {
			sonda.connect(new java.net.InetSocketAddress(HOST, PUERTO_IMAP), 2000);
		}
		catch (Exception e) {
			String mensaje = "No hay servidor de correo de pruebas en " + HOST + ":" + PUERTO_IMAP
					+ ". Levantalo con: docker compose up -d greenmail";
			if (Boolean.parseBoolean(variableDePrueba("NEXTDOCS_PRUEBA_OBLIGATORIA", "false"))) {
				throw new IllegalStateException(mensaje, e);
			}
			Assumptions.abort(mensaje);
		}
	}

	private BuzonCorreoModel crearBuzon(String direccionBuzon, boolean exigirRemitente, boolean exigirCorrelacion) {
		return crearBuzonPara(tenant, direccionBuzon, exigirRemitente, exigirCorrelacion);
	}

	private BuzonCorreoModel crearBuzonPara(Tenant destino, String direccionBuzon, boolean exigirRemitente,
			boolean exigirCorrelacion) {
		NuevoBuzonCorreoReqModel datos = new NuevoBuzonCorreoReqModel();
		datos.setDireccion(direccionBuzon);
		datos.setNombre("Documentacion");
		datos.setHostEntrada(HOST);
		datos.setPuertoEntrada(PUERTO_IMAP);
		datos.setUsuarioEntrada(direccionBuzon);
		datos.setReferenciaSecretoEntrada("literal:clave-de-prueba");
		datos.setCarpeta("INBOX");
		datos.setHostSalida(HOST);
		datos.setPuertoSalida(PUERTO_SMTP);
		datos.setUsuarioSalida(direccionBuzon);
		datos.setReferenciaSecretoSalida("literal:clave-de-prueba");
		datos.setExigirRemitenteAutorizado(exigirRemitente);
		datos.setExigirCorrelacion(exigirCorrelacion);
		datos.setMaximoAdjuntosPorMensaje(10);
		return buzonCorreoService.crear(destino, datos);
	}

	private void autorizar(String patron) {
		RemitenteAutorizadoModel datos = new RemitenteAutorizadoModel();
		datos.setPatron(patron);
		datos.setDescripcion("Alta de prueba");
		buzonCorreoService.autorizar(tenant, buzon.getId(), datos);
	}

	private CorrelacionCorreoModel solicitar(String origen, String tipoObjeto, String idObjeto) {
		NuevaCorrelacionCorreoReqModel pedido = new NuevaCorrelacionCorreoReqModel();
		pedido.setBuzonId(buzon.getId());
		pedido.setSujetoOrigen(origen);
		pedido.setSujetoTipoObjeto(tipoObjeto);
		pedido.setSujetoIdObjeto(idObjeto);
		pedido.setCodigoPlantilla("REMITO");
		pedido.setDiasVigencia(30);
		return buzonCorreoService.crearCorrelacion(tenant, pedido);
	}

	private MensajeCorreoModel procesarUnico() {
		LecturaBuzonModel lectura = trabajadorCorreoService.revisar(buzon.getId());
		assertThat(lectura.getError()).isNull();
		assertThat(lectura.getMensajes()).hasSize(1);
		return lectura.getMensajes().get(0);
	}

	private List<Documento> documentosDelTenant() {
		return documentoRepository.findAll().stream()
				.filter(documento -> documento.getTenant().getId().equals(tenant.getId())).toList();
	}

	private Documento documentoDe(MensajeCorreoModel mensaje) {
		List<AdjuntoCorreo> adjuntos = adjuntoCorreoRepository.listarPorMensaje(mensaje.getId());
		AdjuntoCorreo conDocumento = adjuntos.stream().filter(adjunto -> adjunto.getDocumento() != null).findFirst()
				.orElseThrow(() -> new AssertionError("El mensaje no genero ningun documento"));
		return documentoRepository.findById(conDocumento.getDocumento().getId()).orElseThrow();
	}

	private String sujetoDe(Documento documento) {
		return documento.getReferenciaSujeto() == null ? null : documento.getReferenciaSujeto().getIdObjeto();
	}

	private void enviar(String de, String para, String asunto, String nombreAdjunto, byte[] contenido)
			throws Exception {
		enviarVarios(de, para, asunto, List.of(adjunto(nombreAdjunto, contenido)));
	}

	private void enviarConSubdireccion(String de, String cabeceraPara, String buzonReal, String asunto,
			String nombreAdjunto, byte[] contenido) throws Exception {
		MimeMessage mensaje = componer(de, cabeceraPara, asunto, List.of(adjunto(nombreAdjunto, contenido)));
		Transport.send(mensaje, new InternetAddress[] { new InternetAddress(buzonReal) });
	}

	private void enviarVarios(String de, String para, String asunto, List<MimeBodyPart> adjuntos) throws Exception {
		Transport.send(componer(de, para, asunto, adjuntos));
	}

	private MimeMessage componer(String de, String para, String asunto, List<MimeBodyPart> adjuntos)
			throws Exception {
		Properties configuracion = new Properties();
		configuracion.put("mail.smtp.host", HOST);
		configuracion.put("mail.smtp.port", String.valueOf(PUERTO_SMTP));
		Session sesion = Session.getInstance(configuracion);

		MimeMessage mensaje = new MimeMessage(sesion);
		mensaje.setFrom(new InternetAddress(de));
		mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(para, false));
		mensaje.setSubject(asunto, "UTF-8");

		MimeMultipart cuerpo = new MimeMultipart();
		MimeBodyPart texto = new MimeBodyPart();
		texto.setText("Adjunto lo pedido.", "UTF-8");
		cuerpo.addBodyPart(texto);
		for (MimeBodyPart adjunto : adjuntos) {
			cuerpo.addBodyPart(adjunto);
		}
		mensaje.setContent(cuerpo);
		return mensaje;
	}

	private MimeBodyPart adjunto(String nombre, byte[] contenido) throws Exception {
		MimeBodyPart parte = new MimeBodyPart();
		parte.setDataHandler(new DataHandler(new ByteArrayDataSource(contenido, "application/octet-stream")));
		parte.setFileName(nombre);
		parte.setDisposition(MimeBodyPart.ATTACHMENT);
		return parte;
	}

	private static String variableDePrueba(String nombre, String porDefecto) {
		String valor = System.getenv(nombre);
		if (valor == null || valor.isBlank()) {
			valor = System.getProperty(nombre);
		}
		return valor == null || valor.isBlank() ? porDefecto : valor;
	}
}
