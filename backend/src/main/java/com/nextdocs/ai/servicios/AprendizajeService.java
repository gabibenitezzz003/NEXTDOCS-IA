package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.nextdocs.ai.entidades.CorreccionAprendida;
import com.nextdocs.ai.entidades.Documento;
import com.nextdocs.ai.entidades.ValorExtraido;
import com.nextdocs.ai.repositorios.CorreccionAprendidaRepository;
import com.nextdocs.ai.repositorios.ValorExtraidoRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AprendizajeService {

	public static final String EMISOR_GENERICO = "*";

	public static final int VECES_PARA_APLICAR = 2;

	public static final String CODIGO_APLICADA = "CORREGIDO_POR_APRENDIZAJE";

	private static final int MAXIMO_PISTAS = 12;

	private static final int LARGO_VALOR = 256;

	private static final Logger log = LoggerFactory.getLogger(AprendizajeService.class);

	private final CorreccionAprendidaRepository correccionAprendidaRepository;

	private final ValorExtraidoRepository valorExtraidoRepository;

	public AprendizajeService(CorreccionAprendidaRepository correccionAprendidaRepository,
			ValorExtraidoRepository valorExtraidoRepository) {
		this.correccionAprendidaRepository = correccionAprendidaRepository;
		this.valorExtraidoRepository = valorExtraidoRepository;
	}

	@Transactional
	public void registrar(Documento documento, String claveCampo, String valorLeido, String valorCorregido) {
		if (documento.getPlantilla() == null) {
			return;
		}
		String leido = recortar(valorLeido);
		String corregido = recortar(valorCorregido);
		if (leido == null || leido.equals(corregido)) {
			return;
		}

		String plantilla = documento.getPlantilla().getCodigo();
		String emisor = claveDeEmisorDe(documento);
		CorreccionAprendida correccion = correccionAprendidaRepository
				.buscar(documento.getTenant().getId(), plantilla, emisor, claveCampo, leido)
				.orElseGet(() -> nueva(documento, plantilla, emisor, claveCampo, leido));
		correccion.setValorCorregido(corregido);
		correccion.setVeces(correccion.getVeces() + 1);
		correccion.setUltimoDocumento(documento);
		correccion.setActualizado(Instant.now());
		correccionAprendidaRepository.save(correccion);
		log.info("Correccion aprendida en {}.{} para el emisor {}: \"{}\" pasa a \"{}\" ({} veces)",
				plantilla, claveCampo, emisor, leido, corregido, correccion.getVeces());
	}

	@Transactional(readOnly = true)
	public List<CorreccionAprendida> correccionesDe(String tenantId, String plantillaCodigo, String emisorClave) {
		if (plantillaCodigo == null) {
			return List.of();
		}
		return correccionAprendidaRepository.listarPara(tenantId, plantillaCodigo,
				emisorClave == null ? EMISOR_GENERICO : emisorClave, EMISOR_GENERICO);
	}

	public List<String> pistas(List<CorreccionAprendida> correcciones) {
		List<String> pistas = new ArrayList<>();
		for (CorreccionAprendida correccion : correcciones) {
			if (pistas.size() >= MAXIMO_PISTAS) {
				break;
			}
			pistas.add("En " + correccion.getClaveCampo() + ", cuando el documento parece decir \""
					+ correccion.getValorLeido() + "\" el valor correcto suele ser \""
					+ correccion.getValorCorregido() + "\".");
		}
		return pistas;
	}

	public int aplicar(List<ValorExtraido> valores, List<CorreccionAprendida> correcciones,
			List<String> advertencias) {
		if (correcciones.isEmpty()) {
			return 0;
		}
		Map<String, ValorExtraido> porClave = new java.util.LinkedHashMap<>();
		for (ValorExtraido valor : valores) {
			porClave.put(valor.getClaveCampo(), valor);
		}

		int aplicadas = 0;
		for (CorreccionAprendida correccion : correcciones) {
			if (correccion.getVeces() < VECES_PARA_APLICAR) {
				continue;
			}
			ValorExtraido valor = porClave.get(correccion.getClaveCampo());
			if (valor == null || valor.isCorregidoManualmente()) {
				continue;
			}
			String actual = comparable(valor.getValorNormalizado());
			if (actual == null || !actual.equals(comparable(correccion.getValorLeido()))) {
				continue;
			}
			valor.setValorAnterior(valor.getValorNormalizado());
			valor.setValorNormalizado(correccion.getValorCorregido());
			advertencias.add(CODIGO_APLICADA + ": en " + correccion.getClaveCampo() + " se leyo \""
					+ correccion.getValorLeido() + "\" y se aplico \"" + correccion.getValorCorregido()
					+ "\", que es como se venia corrigiendo " + correccion.getVeces() + " veces");
			aplicadas++;
		}
		return aplicadas;
	}

	public String claveDeEmisor(List<ValorExtraido> valores) {
		String cuit = null;
		String razon = null;
		for (ValorExtraido valor : valores) {
			if ("cuitEmisor".equals(valor.getClaveCampo())) {
				cuit = valor.getValorNormalizado();
			}
			if ("razonSocialEmisor".equals(valor.getClaveCampo())) {
				razon = valor.getValorNormalizado();
			}
		}
		if (cuit != null && !cuit.isBlank()) {
			return cuit.replaceAll("[^0-9]", "");
		}
		String normalizada = comparable(razon);
		return normalizada == null ? EMISOR_GENERICO : recortarClave(normalizada);
	}

	@Transactional(readOnly = true)
	public String claveDeEmisorDe(Documento documento) {
		return claveDeEmisor(valorExtraidoRepository.listarUltimosPorDocumento(documento.getId()));
	}

	private CorreccionAprendida nueva(Documento documento, String plantilla, String emisor, String campo,
			String leido) {
		CorreccionAprendida correccion = new CorreccionAprendida();
		correccion.setTenant(documento.getTenant());
		correccion.setPlantillaCodigo(plantilla);
		correccion.setEmisorClave(emisor);
		correccion.setClaveCampo(campo);
		correccion.setValorLeido(leido);
		correccion.setAlta(Instant.now());
		return correccion;
	}

	private String comparable(String valor) {
		if (valor == null || valor.isBlank()) {
			return null;
		}
		return java.text.Normalizer.normalize(valor.trim(), java.text.Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
	}

	private String recortarClave(String valor) {
		return valor.length() <= 128 ? valor : valor.substring(0, 128);
	}

	private String recortar(String valor) {
		if (valor == null || valor.isBlank()) {
			return null;
		}
		String limpio = valor.trim();
		return limpio.length() <= LARGO_VALOR ? limpio : limpio.substring(0, LARGO_VALOR);
	}
}
