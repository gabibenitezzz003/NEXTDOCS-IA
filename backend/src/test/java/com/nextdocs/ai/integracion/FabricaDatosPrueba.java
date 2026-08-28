package com.nextdocs.ai.integracion;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import com.nextdocs.ai.entidades.CampoPlantilla;
import com.nextdocs.ai.entidades.ConfiguracionConector;
import com.nextdocs.ai.entidades.PlantillaDocumental;
import com.nextdocs.ai.entidades.ReglaPlantilla;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.entidades.VersionPlantilla;
import com.nextdocs.ai.enumeraciones.EstadoPlantilla;
import com.nextdocs.ai.enumeraciones.EstrategiaSegmentacion;
import com.nextdocs.ai.enumeraciones.PoliticaOriginalFisico;
import com.nextdocs.ai.enumeraciones.SeveridadHallazgo;
import com.nextdocs.ai.enumeraciones.TipoAutenticacionConector;
import com.nextdocs.ai.enumeraciones.TipoDatoCampo;
import com.nextdocs.ai.enumeraciones.TipoReglaValidacion;
import com.nextdocs.ai.repositorios.CampoPlantillaRepository;
import com.nextdocs.ai.repositorios.ConfiguracionConectorRepository;
import com.nextdocs.ai.repositorios.PlantillaDocumentalRepository;
import com.nextdocs.ai.repositorios.ReglaPlantillaRepository;
import com.nextdocs.ai.repositorios.UsuarioRepository;
import com.nextdocs.ai.repositorios.VersionPlantillaRepository;
import com.nextdocs.ai.servicios.TenantService;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.context.annotation.Profile;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("prueba")
public class FabricaDatosPrueba {

	public static final String CLAVE_ADMINISTRADOR = "clave-de-prueba";

	private final TenantService tenantService;

	private final UsuarioRepository usuarioRepository;

	private final PlantillaDocumentalRepository plantillaDocumentalRepository;

	private final VersionPlantillaRepository versionPlantillaRepository;

	private final CampoPlantillaRepository campoPlantillaRepository;

	private final ReglaPlantillaRepository reglaPlantillaRepository;

	private final ConfiguracionConectorRepository configuracionConectorRepository;

	public FabricaDatosPrueba(TenantService tenantService, UsuarioRepository usuarioRepository,
			PlantillaDocumentalRepository plantillaDocumentalRepository,
			VersionPlantillaRepository versionPlantillaRepository,
			CampoPlantillaRepository campoPlantillaRepository, ReglaPlantillaRepository reglaPlantillaRepository,
			ConfiguracionConectorRepository configuracionConectorRepository) {
		this.tenantService = tenantService;
		this.usuarioRepository = usuarioRepository;
		this.plantillaDocumentalRepository = plantillaDocumentalRepository;
		this.versionPlantillaRepository = versionPlantillaRepository;
		this.campoPlantillaRepository = campoPlantillaRepository;
		this.reglaPlantillaRepository = reglaPlantillaRepository;
		this.configuracionConectorRepository = configuracionConectorRepository;
	}

	@Transactional
	public Tenant crearTenant(String codigo) {
		return tenantService.crear(codigo, "Tenant " + codigo, "admin@" + codigo + ".test", CLAVE_ADMINISTRADOR);
	}

	@Transactional(readOnly = true)
	public Usuario administradorDe(Tenant tenant) {
		return usuarioRepository.buscarPorEmail(tenant.getId(), "admin@" + tenant.getCodigo() + ".test")
				.orElseThrow();
	}

	@Transactional
	public VersionPlantilla crearPlantillaPublicada(Tenant tenant, String codigo, List<CampoPlantilla> campos,
			List<ReglaPlantilla> reglas, BigDecimal umbralAutoaprobacion) {
		PlantillaDocumental plantilla = new PlantillaDocumental();
		plantilla.setTenant(tenant);
		plantilla.setCodigo(codigo);
		plantilla.setNombre("Plantilla " + codigo);
		plantilla.setFamilia("PRUEBA");
		plantilla.setUmbralQualityGate(new BigDecimal("0.8000"));
		plantilla.setAlta(Instant.now());
		plantillaDocumentalRepository.save(plantilla);

		VersionPlantilla version = new VersionPlantilla();
		version.setTenant(tenant);
		version.setPlantilla(plantilla);
		version.setNumero(1);
		version.setEstado(EstadoPlantilla.PUBLICADA);
		version.setUmbralAutoaprobacion(umbralAutoaprobacion);
		version.setPoliticaOriginalFisico(PoliticaOriginalFisico.NO_REQUIERE);
		version.setEstrategiaSegmentacion(EstrategiaSegmentacion.NINGUNA);
		version.setVersionPrompt("p1");
		version.setVersionEsquema("e1");
		version.setPublicada(Instant.now());
		version.setAlta(Instant.now());
		versionPlantillaRepository.save(version);

		for (CampoPlantilla campo : campos) {
			campo.setVersionPlantilla(version);
			campo.setAlta(Instant.now());
		}
		campoPlantillaRepository.saveAll(campos);

		for (ReglaPlantilla regla : reglas) {
			regla.setVersionPlantilla(version);
			regla.setAlta(Instant.now());
		}
		reglaPlantillaRepository.saveAll(reglas);

		plantilla.setVersionPublicada(version);
		plantillaDocumentalRepository.save(plantilla);
		return version;
	}

	@Transactional
	public ConfiguracionConector registrarConectorPrueba(Tenant tenant, String umbralSeleccion,
			String umbralMinimo) {
		ConfiguracionConector configuracion = new ConfiguracionConector();
		configuracion.setTenant(tenant);
		configuracion.setCodigo(ConectorPruebaService.CODIGO);
		configuracion.setNombre("Conector de prueba");
		configuracion.setUrlBase("http://localhost");
		configuracion.setTipoAutenticacion(TipoAutenticacionConector.NINGUNA);
		configuracion.setTiempoEsperaMilisegundos(1000);
		configuracion.setIntentosMaximos(1);
		configuracion.setUmbralSeleccionAutomatica(new BigDecimal(umbralSeleccion));
		configuracion.setUmbralCandidatoMinimo(new BigDecimal(umbralMinimo));
		configuracion.setUmbralCircuitoAbierto(50);
		configuracion.setDuracionCircuitoAbiertoSegundos(1);
		configuracion.setActivo(true);
		configuracion.setAlta(Instant.now());
		return configuracionConectorRepository.save(configuracion);
	}

	public static CampoPlantilla campo(String clave, TipoDatoCampo tipoDato, boolean requerido) {
		CampoPlantilla campo = new CampoPlantilla();
		campo.setClave(clave);
		campo.setEtiqueta("Etiqueta de " + clave);
		campo.setTipoDato(tipoDato);
		campo.setRequerido(requerido);
		campo.setExtraer(true);
		campo.setValidar(true);
		return campo;
	}

	public static ReglaPlantilla regla(String codigo, TipoReglaValidacion tipo, SeveridadHallazgo severidad,
			String campoObjetivo, String configuracion) {
		ReglaPlantilla regla = new ReglaPlantilla();
		regla.setCodigo(codigo);
		regla.setNombre("Regla " + codigo);
		regla.setTipo(tipo);
		regla.setSeveridad(severidad);
		regla.setCampoObjetivo(campoObjetivo);
		regla.setConfiguracion(configuracion);
		regla.setActiva(true);
		return regla;
	}

	public static MockMultipartFile pdf(String nombre, List<String> textosPorPagina) {
		return new MockMultipartFile("archivo", nombre, "application/pdf", construirPdf(textosPorPagina));
	}

	public static byte[] construirPdf(List<String> textosPorPagina) {
		try (PDDocument documento = new PDDocument()) {
			for (String texto : textosPorPagina) {
				PDPage pagina = new PDPage(PDRectangle.A4);
				documento.addPage(pagina);
				try (PDPageContentStream contenido = new PDPageContentStream(documento, pagina)) {
					contenido.beginText();
					contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 14);
					contenido.newLineAtOffset(50, 780);
					contenido.showText(texto);
					contenido.endText();
				}
			}
			ByteArrayOutputStream salida = new ByteArrayOutputStream();
			documento.save(salida);
			return salida.toByteArray();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public static MockMultipartFile textoComoPdf(String contenido) {
		return new MockMultipartFile("archivo", "falso.pdf", "application/pdf",
				contenido.getBytes(StandardCharsets.UTF_8));
	}
}
