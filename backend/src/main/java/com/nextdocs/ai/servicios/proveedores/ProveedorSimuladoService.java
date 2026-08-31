package com.nextdocs.ai.servicios.proveedores;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Random;

import com.nextdocs.ai.enumeraciones.PresenciaCampo;
import com.nextdocs.ai.enumeraciones.ProveedorDocumentalIa;
import com.nextdocs.ai.interfaces.ProveedorDocumentalIaInt;
import com.nextdocs.ai.modelos.CampoEsquemaModel;
import com.nextdocs.ai.modelos.ResultadoClasificacionModel;
import com.nextdocs.ai.modelos.SolicitudClasificacionModel;
import com.nextdocs.ai.modelos.TipoCandidatoModel;
import com.nextdocs.ai.modelos.ResultadoExtraccionModel;
import com.nextdocs.ai.modelos.SolicitudExtraccionModel;
import com.nextdocs.ai.modelos.ValorCanonicoModel;

import org.springframework.stereotype.Service;

@Service
public class ProveedorSimuladoService implements ProveedorDocumentalIaInt {

	private static final String MODELO = "simulado-v1";

	private static final BigDecimal CONFIANZA_BASE = new BigDecimal("0.9200");

	@Override
	public ProveedorDocumentalIa tipo() {
		return ProveedorDocumentalIa.SIMULADO;
	}

	@Override
	public boolean estaDisponible() {
		return true;
	}

	@Override
	public ResultadoExtraccionModel extraer(SolicitudExtraccionModel solicitud) {
		long inicio = System.currentTimeMillis();
		Random aleatorio = new Random(solicitud.getDocumentoId().hashCode());
		ResultadoExtraccionModel resultado = new ResultadoExtraccionModel();
		resultado.setProveedor(tipo());
		resultado.setModelo(MODELO);
		resultado.setVersionPrompt(solicitud.getVersionPrompt());
		resultado.setVersionEsquema(solicitud.getVersionEsquema());
		resultado.setTipoDetectado(solicitud.getCodigoPlantilla());
		resultado.setConfianzaClasificacion(CONFIANZA_BASE);
		resultado.setPaginasProcesadas(Math.max(solicitud.getPaginas(), 1));
		resultado.setTokensEntrada(solicitud.getContenido() == null ? 0 : solicitud.getContenido().length / 4L);
		resultado.setTokensSalida(solicitud.getCampos().size() * 12L);
		resultado.setCosto(BigDecimal.ZERO);
		resultado.setMonedaCosto("USD");
		for (CampoEsquemaModel campo : solicitud.getCampos()) {
			resultado.getValores().add(simularValor(campo, aleatorio));
		}
		resultado.setDuracionMilisegundos(System.currentTimeMillis() - inicio);
		return resultado;
	}

	@Override
	public ResultadoClasificacionModel clasificar(SolicitudClasificacionModel solicitud) {
		long inicio = System.currentTimeMillis();
		ResultadoClasificacionModel resultado = new ResultadoClasificacionModel();
		resultado.setProveedor(tipo());
		resultado.setModelo(MODELO);
		resultado.setConfianza(CONFIANZA_BASE);
		resultado.setCodigoPropuesto(elegirPorNombre(solicitud));
		resultado.setMotivo("Clasificacion simulada a partir del nombre del archivo");
		if (resultado.esDesconocido()) {
			resultado.setConfianza(new BigDecimal("0.3000"));
			resultado.setNombreSugerido("Tipo sin catalogar");
		}
		resultado.setDuracionMilisegundos(System.currentTimeMillis() - inicio);
		return resultado;
	}

	private String elegirPorNombre(SolicitudClasificacionModel solicitud) {
		String nombre = solicitud.getNombreArchivo() == null ? ""
				: solicitud.getNombreArchivo().toUpperCase(Locale.ROOT);
		for (TipoCandidatoModel candidato : solicitud.getCandidatos()) {
			if (candidato.getCodigo() != null && nombre.contains(candidato.getCodigo())) {
				return candidato.getCodigo();
			}
		}
		return ResultadoClasificacionModel.CODIGO_DESCONOCIDO;
	}

	private ValorCanonicoModel simularValor(CampoEsquemaModel campo, Random aleatorio) {
		ValorCanonicoModel valor = new ValorCanonicoModel();
		valor.setClaveCampo(campo.getClave());
		valor.setEvidenciaPagina(1);
		int sorteo = aleatorio.nextInt(100);
		if (!campo.isRequerido() && sorteo < 10) {
			valor.setPresencia(PresenciaCampo.NO_FIGURA);
			valor.setConfianza(BigDecimal.ONE);
			valor.setConfianzaProveedor(BigDecimal.ONE);
			return valor;
		}
		if (sorteo >= 95) {
			valor.setPresencia(PresenciaCampo.ILEGIBLE);
			valor.setConfianza(new BigDecimal("0.3000"));
			valor.setConfianzaProveedor(new BigDecimal("0.3000"));
			return valor;
		}
		String simulado = valorSimulado(campo, aleatorio);
		BigDecimal confianza = CONFIANZA_BASE.add(new BigDecimal(aleatorio.nextInt(80)).movePointLeft(4))
				.setScale(4, RoundingMode.HALF_UP);
		valor.setPresencia(PresenciaCampo.PRESENTE);
		valor.setValorCrudo(simulado);
		valor.setValorNormalizado(NormalizadorValor.normalizar(simulado, campo.getTipoDato()));
		valor.setConfianza(confianza);
		valor.setConfianzaProveedor(confianza);
		valor.setEvidenciaRecuadro("0.12,0.20,0.48,0.06");
		return valor;
	}

	private String valorSimulado(CampoEsquemaModel campo, Random aleatorio) {
		if (campo.getTipoDato() == null) {
			return campo.getClave().toUpperCase(Locale.ROOT) + "-" + aleatorio.nextInt(10000);
		}
		return switch (campo.getTipoDato()) {
			case NUMERO -> String.valueOf(aleatorio.nextInt(1000) + 1);
			case DECIMAL, MONEDA -> aleatorio.nextInt(100000) / 100.0 + "";
			case FECHA, FECHA_HORA -> "2026-0" + (aleatorio.nextInt(9) + 1) + "-1" + aleatorio.nextInt(9);
			case BOOLEANO -> aleatorio.nextBoolean() ? "SI" : "NO";
			case CUIT -> "30" + (10000000 + aleatorio.nextInt(89999999)) + aleatorio.nextInt(10);
			case EMAIL -> "contacto" + aleatorio.nextInt(999) + "@ejemplo.com";
			case TELEFONO -> "+549" + (1000000000L + aleatorio.nextInt(999999999));
			default -> campo.getEtiqueta() + " " + (aleatorio.nextInt(9000) + 1000);
		};
	}
}
