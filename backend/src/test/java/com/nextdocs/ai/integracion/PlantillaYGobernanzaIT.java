package com.nextdocs.ai.integracion;

import static com.nextdocs.ai.integracion.FabricaDatosPrueba.pdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.Tenant;
import com.nextdocs.ai.entidades.Usuario;
import com.nextdocs.ai.enumeraciones.DecisionRevision;
import com.nextdocs.ai.enumeraciones.EstadoDocumento;
import com.nextdocs.ai.enumeraciones.EstadoOriginalFisico;
import com.nextdocs.ai.enumeraciones.EstadoPlantilla;
import com.nextdocs.ai.enumeraciones.PoliticaOriginalFisico;
import com.nextdocs.ai.enumeraciones.PresenciaCampo;
import com.nextdocs.ai.enumeraciones.TipoDatoCampo;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.modelos.CampoPlantillaReqModel;
import com.nextdocs.ai.modelos.DocumentoModel;
import com.nextdocs.ai.modelos.NuevoDocumentoReqModel;
import com.nextdocs.ai.modelos.OriginalFisicoReqModel;
import com.nextdocs.ai.modelos.PlantillaModel;
import com.nextdocs.ai.modelos.PlantillaReqModel;
import com.nextdocs.ai.modelos.RevisionDocumentoModel;
import com.nextdocs.ai.modelos.RevisionDocumentoReqModel;
import com.nextdocs.ai.modelos.VersionPlantillaModel;
import com.nextdocs.ai.modelos.VersionPlantillaReqModel;
import com.nextdocs.ai.repositorios.DocumentoRepository;
import com.nextdocs.ai.repositorios.EventoAuditoriaRepository;
import com.nextdocs.ai.repositorios.ValorExtraidoRepository;
import com.nextdocs.ai.servicios.DocumentoService;
import com.nextdocs.ai.servicios.ExtractorDocumentalService;
import com.nextdocs.ai.servicios.IngestaDocumentalService;
import com.nextdocs.ai.servicios.OriginalFisicoService;
import com.nextdocs.ai.servicios.PlantillaService;
import com.nextdocs.ai.servicios.RevisionDocumentalService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PlantillaYGobernanzaIT extends PruebaIntegracion {

	@Autowired
	private FabricaDatosPrueba fabrica;

	@Autowired
	private ProveedorPruebaService proveedor;

	@Autowired
	private PlantillaService plantillaService;

	@Autowired
	private IngestaDocumentalService ingestaDocumentalService;

	@Autowired
	private ExtractorDocumentalService extractorDocumentalService;

	@Autowired
	private DocumentoService documentoService;

	@Autowired
	private RevisionDocumentalService revisionDocumentalService;

	@Autowired
	private OriginalFisicoService originalFisicoService;

	@Autowired
	private DocumentoRepository documentoRepository;

	@Autowired
	private ValorExtraidoRepository valorExtraidoRepository;

	@Autowired
	private EventoAuditoriaRepository eventoAuditoriaRepository;

	private Tenant tenant;

	private Usuario administrador;

	@BeforeEach
	void preparar() {
		proveedor.reiniciar();
		tenant = fabrica.crearTenant("t" + UUID.randomUUID().toString().substring(0, 8));
		administrador = fabrica.administradorDe(tenant);
	}

	@Test
	@DisplayName("publicar una version sin campos se rechaza por el quality gate")
	void publicarSinCamposFalla() {
		PlantillaModel plantilla = crearPlantilla("VACIA");
		String versionId = plantilla.getVersiones().get(0).getId();

		assertThatThrownBy(() -> plantillaService.publicar(tenant.getId(), versionId, administrador))
				.isInstanceOf(ValidacionException.class)
				.hasMessageContaining("no tiene ningun campo definido");
	}

	@Test
	@DisplayName("una regla que apunta a un campo inexistente bloquea la publicacion")
	void reglaHuerfanaBloqueaPublicacion() {
		PlantillaModel plantilla = crearPlantilla("HUERFANA");
		String versionId = plantilla.getVersiones().get(0).getId();
		plantillaService.agregarCampo(tenant.getId(), versionId, campoReq("numeroRemito"));
		plantillaService.agregarRegla(tenant.getId(), versionId,
				reglaReq("IVA_OBLIGATORIO", "iva"));

		List<String> errores = plantillaService.validarVersion(tenant.getId(), versionId);

		assertThat(errores).anyMatch(error -> error.contains("campo inexistente iva"));
	}

	@Test
	@DisplayName("una version publicada es inmutable")
	void versionPublicadaEsInmutable() {
		String versionId = publicarPlantillaSimple("INMUTABLE");

		assertThatThrownBy(
				() -> plantillaService.agregarCampo(tenant.getId(), versionId, campoReq("otroCampo")))
				.isInstanceOf(ValidacionException.class)
				.hasMessageContaining("es inmutable");
	}

	@Test
	@DisplayName("QA1-10: publicar la v2 deprecata la v1 y el rollback invierte los estados")
	void qa110RollbackDePlantilla() {
		PlantillaModel plantilla = crearPlantilla("ROLLBACK");
		String v1 = publicar(plantilla.getVersiones().get(0).getId());

		VersionPlantillaReqModel nueva = new VersionPlantillaReqModel();
		nueva.setVersionBaseId(v1);
		VersionPlantillaModel v2 = plantillaService.crearVersion(tenant, plantilla.getId(), nueva);
		plantillaService.publicar(tenant.getId(), v2.getId(), administrador);

		assertThat(plantillaService.obtenerVersion(tenant.getId(), v1).getEstado())
				.isEqualTo(EstadoPlantilla.DEPRECADA);

		plantillaService.revertir(tenant.getId(), plantilla.getId(), v1, administrador);

		assertThat(plantillaService.obtenerVersion(tenant.getId(), v1).getEstado())
				.isEqualTo(EstadoPlantilla.PUBLICADA);
		assertThat(plantillaService.obtenerVersion(tenant.getId(), v2.getId()).getEstado())
				.isEqualTo(EstadoPlantilla.DEPRECADA);
	}

	@Test
	@DisplayName("QA-WF-03: un documento sigue apuntando a su version aunque se publique una nueva")
	void qaWf03ElDocumentoNoCambiaDeVersion() {
		PlantillaModel plantilla = crearPlantilla("HISTORICO");
		String v1 = publicar(plantilla.getVersiones().get(0).getId());

		DocumentoModel documento = ingresar(plantilla.getCodigo(), "historico");
		assertThat(documento.getVersionPlantillaId()).isEqualTo(v1);

		VersionPlantillaReqModel nueva = new VersionPlantillaReqModel();
		nueva.setVersionBaseId(v1);
		VersionPlantillaModel v2 = plantillaService.crearVersion(tenant, plantilla.getId(), nueva);
		plantillaService.publicar(tenant.getId(), v2.getId(), administrador);

		assertThat(documentoService.obtener(tenant.getId(), documento.getId()).getVersionPlantillaId())
				.isEqualTo(v1);
		assertThat(ingresar(plantilla.getCodigo(), "nuevo").getVersionPlantillaId()).isEqualTo(v2.getId());
	}

	@Test
	@DisplayName("GOV-03: aprobar corrigiendo campos exige motivo y deja el cambio auditado")
	void gov03CorreccionAuditada() {
		String versionId = publicarPlantillaSimple("GOV03");
		proveedor.programar("numeroRemito", PresenciaCampo.PRESENTE, "R-MAL", "0.50");
		DocumentoModel documento = ingresar("GOV03", "gov03");
		extractorDocumentalService.procesar(documento.getId());

		RevisionDocumentoReqModel datos = new RevisionDocumentoReqModel();
		datos.setDecision(DecisionRevision.APROBAR);
		datos.setMotivo("Corregido contra el original fisico");
		datos.setCorrecciones(Map.of("numeroRemito", "R-BIEN"));

		assertThat(documentoRepository.findById(documento.getId()).orElseThrow().getEstado())
				.isEqualTo(EstadoDocumento.VALIDADO);

		RevisionDocumentoModel revision = revisionDocumentalService.registrar(tenant.getId(), documento.getId(),
				administrador, datos);

		assertThat(revision.getEstadoNuevo()).isEqualTo(EstadoDocumento.APROBADO);
		assertThat(revision.getCantidadCorrecciones()).isEqualTo(1);
		assertThat(revision.getCambios()).hasSize(1);
		assertThat(revision.getCambios().get(0).getValorAnterior()).isEqualTo("R-MAL");
		assertThat(revision.getCambios().get(0).getValorNuevo()).isEqualTo("R-BIEN");
		assertThat(valorExtraidoRepository.listarUltimosPorDocumento(documento.getId()))
				.anyMatch(valor -> valor.isCorregidoManualmente()
						&& "R-BIEN".equals(valor.getValorNormalizado()));
		assertThat(eventoAuditoriaRepository.listarPorTenant(tenant.getId(), null, null,
				org.springframework.data.domain.PageRequest.of(0, 100)).getContent()).isNotEmpty();
		assertThat(versionId).isNotBlank();
	}

	@Test
	@DisplayName("GOV-03: rechazar sin motivo se rechaza")
	void gov03RechazoSinMotivo() {
		publicarPlantillaSimple("GOV03B");
		DocumentoModel documento = ingresar("GOV03B", "gov03b");
		extractorDocumentalService.procesar(documento.getId());

		RevisionDocumentoReqModel datos = new RevisionDocumentoReqModel();
		datos.setDecision(DecisionRevision.RECHAZAR);

		assertThatThrownBy(() -> revisionDocumentalService.registrar(tenant.getId(), documento.getId(),
				administrador, datos)).isInstanceOf(ValidacionException.class)
						.hasMessageContaining("motivo explicito");
	}

	@Test
	@DisplayName("QA1-08: con REQUIERE_SEGUIMIENTO el documento cierra y el papel queda pendiente")
	void qa108ElPapelSigueDespuesDelCierre() {
		String versionId = publicarPlantillaConPolitica("QA108", PoliticaOriginalFisico.REQUIERE_SEGUIMIENTO);
		DocumentoModel documento = ingresar("QA108", "qa108");
		extractorDocumentalService.procesar(documento.getId());

		assertThat(originalFisicoService.buscarPorDocumento(tenant.getId(), documento.getId()).orElseThrow()
				.getEstado()).isEqualTo(EstadoOriginalFisico.PENDIENTE);

		asegurarAprobado(documento.getId());
		documentoService.cerrar(tenant.getId(), documento.getId());

		assertThat(documentoRepository.findById(documento.getId()).orElseThrow().getEstado())
				.isEqualTo(EstadoDocumento.CERRADO);
		assertThat(originalFisicoService.buscarPorDocumento(tenant.getId(), documento.getId()).orElseThrow()
				.isPendiente()).isTrue();
		assertThat(versionId).isNotBlank();
	}

	@Test
	@DisplayName("QA1-08: con REQUIERE_PARA_CIERRE el papel bloquea el cierre hasta que se recibe")
	void qa108ElPapelBloqueaElCierre() {
		publicarPlantillaConPolitica("QA108B", PoliticaOriginalFisico.REQUIERE_PARA_CIERRE);
		DocumentoModel documento = ingresar("QA108B", "qa108b");
		extractorDocumentalService.procesar(documento.getId());

		asegurarAprobado(documento.getId());
		assertThatThrownBy(() -> documentoService.cerrar(tenant.getId(), documento.getId()))
				.isInstanceOf(ValidacionException.class)
				.hasMessageContaining("exige el original fisico para cerrar");

		OriginalFisicoReqModel datos = new OriginalFisicoReqModel();
		datos.setUbicacion("Archivo central");
		originalFisicoService.registrarRecepcion(tenant.getId(), documento.getId(), administrador, datos);

		documentoService.cerrar(tenant.getId(), documento.getId());

		assertThat(documentoRepository.findById(documento.getId()).orElseThrow().getEstado())
				.isEqualTo(EstadoDocumento.CERRADO);
	}

	@Test
	@DisplayName("archivar un original fisico que nunca se recibio se rechaza")
	void archivarSinRecibirFalla() {
		publicarPlantillaConPolitica("QA108C", PoliticaOriginalFisico.REQUIERE_SEGUIMIENTO);
		DocumentoModel documento = ingresar("QA108C", "qa108c");

		assertThatThrownBy(() -> originalFisicoService.registrarArchivado(tenant.getId(), documento.getId(),
				administrador, new OriginalFisicoReqModel())).isInstanceOf(ValidacionException.class)
						.hasMessageContaining("de PENDIENTE a ARCHIVADO");
	}

	private PlantillaModel crearPlantilla(String codigo) {
		PlantillaReqModel datos = new PlantillaReqModel();
		datos.setCodigo(codigo);
		datos.setNombre("Plantilla " + codigo);
		datos.setFamilia("PRUEBA");
		return plantillaService.crear(tenant, administrador, datos);
	}

	private String publicarPlantillaSimple(String codigo) {
		PlantillaModel plantilla = crearPlantilla(codigo);
		return publicar(plantilla.getVersiones().get(0).getId());
	}

	private String publicarPlantillaConPolitica(String codigo, PoliticaOriginalFisico politica) {
		PlantillaModel plantilla = crearPlantilla(codigo);
		String versionId = plantilla.getVersiones().get(0).getId();
		VersionPlantillaReqModel datos = new VersionPlantillaReqModel();
		datos.setPoliticaOriginalFisico(politica);
		plantillaService.actualizarVersion(tenant.getId(), versionId, datos);
		return publicar(versionId);
	}

	private void aprobar(String documentoId) {
		RevisionDocumentoReqModel datos = new RevisionDocumentoReqModel();
		datos.setDecision(DecisionRevision.APROBAR);
		datos.setMotivo("Aprobado en la prueba");
		revisionDocumentalService.registrar(tenant.getId(), documentoId, administrador, datos);
	}

	private String publicar(String versionId) {
		plantillaService.agregarCampo(tenant.getId(), versionId, campoReq("numeroRemito"));
		VersionPlantillaReqModel umbral = new VersionPlantillaReqModel();
		umbral.setUmbralAutoaprobacion(new java.math.BigDecimal("0.9000"));
		plantillaService.actualizarVersion(tenant.getId(), versionId, umbral);
		plantillaService.publicar(tenant.getId(), versionId, administrador);
		return versionId;
	}

	private CampoPlantillaReqModel campoReq(String clave) {
		CampoPlantillaReqModel campo = new CampoPlantillaReqModel();
		campo.setClave(clave);
		campo.setEtiqueta("Etiqueta de " + clave);
		campo.setTipoDato(TipoDatoCampo.TEXTO);
		campo.setRequerido(true);
		campo.setExtraer(true);
		campo.setValidar(true);
		return campo;
	}

	private com.nextdocs.ai.modelos.ReglaPlantillaReqModel reglaReq(String codigo, String campoObjetivo) {
		com.nextdocs.ai.modelos.ReglaPlantillaReqModel regla = new com.nextdocs.ai.modelos.ReglaPlantillaReqModel();
		regla.setCodigo(codigo);
		regla.setNombre("Regla " + codigo);
		regla.setTipo(com.nextdocs.ai.enumeraciones.TipoReglaValidacion.OBLIGATORIO);
		regla.setSeveridad(com.nextdocs.ai.enumeraciones.SeveridadHallazgo.BLOQUEANTE);
		regla.setCampoObjetivo(campoObjetivo);
		regla.setActiva(true);
		return regla;
	}

	private void asegurarAprobado(String documentoId) {
		Documento documento = documentoRepository.findById(documentoId).orElseThrow();
		if (documento.getEstado() != EstadoDocumento.APROBADO) {
			aprobar(documentoId);
		}
	}

	private DocumentoModel ingresar(String codigoPlantilla, String clave) {
		NuevoDocumentoReqModel datos = new NuevoDocumentoReqModel();
		datos.setCodigoPlantilla(codigoPlantilla);
		return ingestaDocumentalService.ingresar(tenant, administrador,
				pdf("remito.pdf", List.of("REMITO " + clave)), datos, clave + "-" + UUID.randomUUID());
	}
}
