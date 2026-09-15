package com.nextdocs.ai.integracion;

import static com.nextdocs.ai.integracion.FabricaDatosPrueba.campo;
import static com.nextdocs.ai.integracion.FabricaDatosPrueba.pdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.nextdocs.ai.entidades.ArchivoDocumento;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.LoteExportacion;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.entidades.VersionPlantilla;
import com.nextdocs.ai.enumeraciones.EstadoDocumento;
import com.nextdocs.ai.enumeraciones.EstadoLoteExportacion;
import com.nextdocs.ai.enumeraciones.TipoDatoCampo;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.DocumentoModel;
import com.nextdocs.ai.modelos.LoteExportacionModel;
import com.nextdocs.ai.modelos.NuevoDocumentoReqModel;
import com.nextdocs.ai.modelos.NuevoLoteExportacionReqModel;
import com.nextdocs.ai.repositorios.ArchivoDocumentoRepository;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.repositorios.LoteExportacionRepository;
import com.nextdocs.ai.servicios.AlmacenamientoService;
import com.nextdocs.ai.servicios.ExportacionService;
import com.nextdocs.ai.servicios.GeneradorExportacionService;
import com.nextdocs.ai.servicios.IngestaDocumentalService;
import com.nextdocs.ai.servicios.TrabajadorExportacionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

class ExportacionIT extends PruebaIntegracion {

	@Autowired
	private FabricaDatosPrueba fabrica;

	@Autowired
	private ProveedorPruebaService proveedor;

	@Autowired
	private ExportacionService exportacionService;

	@Autowired
	private GeneradorExportacionService generadorExportacionService;

	@Autowired
	private TrabajadorExportacionService trabajadorExportacionService;

	@Autowired
	private IngestaDocumentalService ingestaDocumentalService;

	@Autowired
	private LoteExportacionRepository loteExportacionRepository;

	@Autowired
	private ArchivoDocumentoRepository archivoDocumentoRepository;

	@Autowired
	private DocumentoRepository documentoRepository;

	@Autowired
	private AlmacenamientoService almacenamientoService;

	private Tenant tenant;

	private Usuario administrador;

	private VersionPlantilla version;

	@BeforeEach
	void preparar() {
		proveedor.reiniciar();
		tenant = fabrica.crearTenant("t" + UUID.randomUUID().toString().substring(0, 8));
		administrador = fabrica.administradorDe(tenant);
		version = fabrica.crearPlantillaPublicada(tenant, "EXP_" + UUID.randomUUID().toString().substring(0, 6),
				List.of(campo("numeroRemito", TipoDatoCampo.TEXTO, true)), List.of(), new BigDecimal("0.9000"));
	}

	@Test
	@DisplayName("el ZIP trae los originales, el indice y el manifiesto de hashes")
	void paqueteCompleto() {
		ingresar("uno");
		ingresar("dos");

		LoteExportacion lote = generar(solicitar(new NuevoLoteExportacionReqModel()));

		assertThat(lote.getEstado()).isEqualTo(EstadoLoteExportacion.DISPONIBLE);
		assertThat(lote.getCantidadDocumentos()).isEqualTo(2);
		assertThat(lote.getSha256()).hasSize(64);
		assertThat(lote.getNombreArchivo()).endsWith(".zip");

		Map<String, byte[]> contenido = abrir(lote);
		assertThat(contenido).containsKey(GeneradorExportacionService.NOMBRE_INDICE);
		assertThat(contenido).containsKey(GeneradorExportacionService.NOMBRE_MANIFIESTO);
		assertThat(contenido.keySet().stream()
				.filter(nombre -> nombre.startsWith(GeneradorExportacionService.CARPETA_DOCUMENTOS)).count())
				.isEqualTo(2);
	}

	@Test
	@DisplayName("el indice trae una fila por documento con su hash y su estado")
	void indiceAuditable() {
		ingresar("uno");
		LoteExportacion lote = generar(solicitar(new NuevoLoteExportacionReqModel()));

		String indice = new String(abrir(lote).get(GeneradorExportacionService.NOMBRE_INDICE),
				StandardCharsets.UTF_8);
		String[] lineas = indice.split("\r\n");

		assertThat(lineas[0]).contains("batch_id").contains("sha256").contains("skipped_reason");
		assertThat(lineas).hasSize(2);
		assertThat(lineas[1]).contains(lote.getId()).contains(version.getPlantilla().getCodigo());
	}

	@Test
	@DisplayName("el manifiesto lista el hash de cada archivo empaquetado")
	void manifiestoDeHashes() {
		ingresar("uno");
		LoteExportacion lote = generar(solicitar(new NuevoLoteExportacionReqModel()));

		Map<String, byte[]> contenido = abrir(lote);
		String manifiesto = new String(contenido.get(GeneradorExportacionService.NOMBRE_MANIFIESTO),
				StandardCharsets.UTF_8);
		String nombreDocumento = contenido.keySet().stream()
				.filter(nombre -> nombre.startsWith(GeneradorExportacionService.CARPETA_DOCUMENTOS)).findFirst()
				.orElseThrow();

		assertThat(manifiesto).contains(nombreDocumento);
		assertThat(manifiesto.split("  ")[0]).hasSize(64);
	}

	@Test
	@DisplayName("SEC: un archivo en cuarentena nunca entra al ZIP y queda registrado como omitido")
	void cuarentenaNoSeExporta() {
		DocumentoModel limpio = ingresar("limpio");
		DocumentoModel infectado = ingresar("infectado");
		ArchivoDocumento archivo = archivoDocumentoRepository.buscarOriginalVigente(infectado.getId()).orElseThrow();
		archivo.setEnCuarentena(true);
		archivoDocumentoRepository.save(archivo);

		LoteExportacion lote = generar(solicitar(new NuevoLoteExportacionReqModel()));

		assertThat(lote.getCantidadDocumentos()).isEqualTo(1);
		assertThat(lote.getCantidadOmitidos()).isEqualTo(1);

		Map<String, byte[]> contenido = abrir(lote);
		List<String> documentos = contenido.keySet().stream()
				.filter(nombre -> nombre.startsWith(GeneradorExportacionService.CARPETA_DOCUMENTOS)).toList();
		assertThat(documentos).hasSize(1);
		assertThat(documentos.get(0)).contains(limpio.getId().substring(0, 8));
		assertThat(documentos.get(0)).doesNotContain(infectado.getId().substring(0, 8));

		LoteExportacionModel modelo = exportacionService.obtener(tenant.getId(), lote.getId());
		assertThat(modelo.getItems()).anySatisfy(item -> assertThat(item.getMotivoOmision()).contains("cuarentena"));
	}

	@Test
	@DisplayName("los filtros por estado acotan la poblacion exportada")
	void filtraPorEstado() {
		ingresar("uno");
		DocumentoModel dos = ingresar("dos");
		Documento entidad = documentoRepository.findById(dos.getId()).orElseThrow();
		entidad.setEstado(EstadoDocumento.APROBADO);
		documentoRepository.save(entidad);

		NuevoLoteExportacionReqModel datos = new NuevoLoteExportacionReqModel();
		datos.setEstados(List.of(EstadoDocumento.APROBADO));
		LoteExportacion lote = generar(solicitar(datos));

		assertThat(lote.getCantidadDocumentos()).isEqualTo(1);
	}

	@Test
	@DisplayName("pedir una exportacion sin resultados se rechaza en vez de generar un ZIP vacio")
	void sinResultados() {
		NuevoLoteExportacionReqModel datos = new NuevoLoteExportacionReqModel();
		datos.setEstados(List.of(EstadoDocumento.CERRADO));

		assertThatThrownBy(() -> exportacionService.solicitar(tenant.getId(), administrador, datos))
				.isInstanceOf(ValidacionException.class)
				.hasMessageContaining("Ningun documento coincide");
	}

	@Test
	@DisplayName("el lote nace con vencimiento a 7 dias")
	void vigenciaPorDefecto() {
		ingresar("uno");
		LoteExportacionModel modelo = solicitar(new NuevoLoteExportacionReqModel());

		long dias = ChronoUnit.DAYS.between(modelo.getAlta(), modelo.getVenceEn());
		assertThat(dias).isEqualTo(7);
	}

	@Test
	@DisplayName("al vencer se borra el archivo y la descarga explica que hay que volver a pedirla")
	void cicloDeVida() {
		ingresar("uno");
		LoteExportacion lote = generar(solicitar(new NuevoLoteExportacionReqModel()));
		assertThat(exportacionService.urlDescarga(tenant.getId(), lote.getId())).contains("http");

		lote.setVenceEn(Instant.now().minus(1, ChronoUnit.HOURS));
		loteExportacionRepository.save(lote);
		int vencidos = trabajadorExportacionService.vencer();

		assertThat(vencidos)
				.as("el ciclo es global: puede vencer lotes de otras corridas, lo que importa es que venza el nuestro")
				.isGreaterThanOrEqualTo(1);
		LoteExportacion recargado = loteExportacionRepository.findById(lote.getId()).orElseThrow();
		assertThat(recargado.getEstado()).isEqualTo(EstadoLoteExportacion.VENCIDO);
		assertThat(recargado.getClaveObjeto()).isNull();
		assertThatThrownBy(() -> exportacionService.urlDescarga(tenant.getId(), lote.getId()))
				.isInstanceOf(ValidacionException.class)
				.hasMessageContaining("Volve a pedir la exportacion");
	}

	@Test
	@DisplayName("el aviso de vencimiento se emite una sola vez")
	void avisoPrevioUnaVez() {
		ingresar("uno");
		trabajadorExportacionService.avisarPorVencer();
		LoteExportacion lote = generar(solicitar(new NuevoLoteExportacionReqModel()));
		lote.setVenceEn(Instant.now().plus(1, ChronoUnit.DAYS));
		loteExportacionRepository.save(lote);

		assertThat(trabajadorExportacionService.avisarPorVencer()).isEqualTo(1);
		assertThat(trabajadorExportacionService.avisarPorVencer()).isZero();
	}

	@Test
	@DisplayName("un lote todavia no generado no se puede descargar")
	void descargaAntesDeTiempo() {
		ingresar("uno");
		LoteExportacionModel modelo = solicitar(new NuevoLoteExportacionReqModel());

		assertThatThrownBy(() -> exportacionService.urlDescarga(tenant.getId(), modelo.getId()))
				.isInstanceOf(ValidacionException.class)
				.hasMessageContaining("todavia no tiene archivo");
	}

	@Test
	@DisplayName("SEC-01: un tenant no ve ni descarga los lotes de otro")
	void aislamientoPorTenant() {
		ingresar("uno");
		LoteExportacionModel modelo = solicitar(new NuevoLoteExportacionReqModel());
		Tenant otro = fabrica.crearTenant("o" + UUID.randomUUID().toString().substring(0, 8));

		assertThatThrownBy(() -> exportacionService.obtener(otro.getId(), modelo.getId()))
				.isInstanceOf(EntidadNoEncontradaException.class);
		assertThatThrownBy(() -> exportacionService.urlDescarga(otro.getId(), modelo.getId()))
				.isInstanceOf(EntidadNoEncontradaException.class);
		assertThat(exportacionService.listar(otro.getId(), null, PageRequest.of(0, 10)).getTotalElements())
				.isZero();
	}

	private LoteExportacionModel solicitar(NuevoLoteExportacionReqModel datos) {
		return exportacionService.solicitar(tenant.getId(), administrador, datos);
	}

	private LoteExportacion generar(LoteExportacionModel modelo) {
		generadorExportacionService.generar(modelo.getId());
		return loteExportacionRepository.findById(modelo.getId()).orElseThrow();
	}

	private Map<String, byte[]> abrir(LoteExportacion lote) {
		Map<String, byte[]> contenido = new LinkedHashMap<>();
		byte[] paquete = almacenamientoService.leerExportacion(lote.getClaveObjeto());
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(paquete))) {
			ZipEntry entrada;
			while ((entrada = zip.getNextEntry()) != null) {
				contenido.put(entrada.getName(), zip.readAllBytes());
			}
		} catch (Exception e) {
			throw new IllegalStateException("No se pudo abrir el ZIP exportado", e);
		}
		return contenido;
	}

	private DocumentoModel ingresar(String clave) {
		NuevoDocumentoReqModel datos = new NuevoDocumentoReqModel();
		datos.setCodigoPlantilla(version.getPlantilla().getCodigo());
		return ingestaDocumentalService.ingresar(tenant, administrador, pdf("remito.pdf", List.of("REMITO " + clave)),
				datos, clave + "-" + UUID.randomUUID());
	}
}
