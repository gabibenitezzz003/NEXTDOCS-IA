package com.nextdocs.ai.integracion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.nextdocs.ai.enumeraciones.PresenciaCampo;
import com.nextdocs.ai.enumeraciones.ProveedorDocumentalIa;
import com.nextdocs.ai.exceptions.ProveedorNoDisponibleException;
import com.nextdocs.ai.interfaces.ProveedorDocumentalIaInt;
import com.nextdocs.ai.modelos.CampoEsquemaModel;
import com.nextdocs.ai.modelos.ResultadoClasificacionModel;
import com.nextdocs.ai.modelos.ResultadoExtraccionModel;
import com.nextdocs.ai.modelos.SolicitudClasificacionModel;
import com.nextdocs.ai.modelos.SolicitudExtraccionModel;
import com.nextdocs.ai.modelos.ValorCanonicoModel;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("prueba")
public class ProveedorPruebaService implements ProveedorDocumentalIaInt {

	private final Map<String, ValorCanonicoModel> valoresProgramados = new LinkedHashMap<>();

	private final AtomicInteger llamadas = new AtomicInteger();

	private final List<SolicitudExtraccionModel> solicitudes = new ArrayList<>();

	private final List<SolicitudClasificacionModel> clasificaciones = new ArrayList<>();

	private String tipoProgramado;

	private BigDecimal confianzaClasificacion = new BigDecimal("0.9500");

	private String nombreSugerido;

	private List<com.nextdocs.ai.modelos.CampoSugeridoModel> camposSugeridos = new ArrayList<>();

	private int fallosPendientes;

	private boolean falloReintentable = true;

	private BigDecimal confianzaPorDefecto = new BigDecimal("1.0000");

	private long tokensEntrada = 100;

	private long tokensSalida = 50;

	private BigDecimal costo = BigDecimal.ZERO;

	public void programarClasificacion(String codigo, BigDecimal confianza) {
		this.tipoProgramado = codigo;
		this.confianzaClasificacion = confianza;
		this.nombreSugerido = null;
		this.camposSugeridos = new ArrayList<>();
	}

	public void programarClasificacionDesconocida(String nombreSugerido,
			List<com.nextdocs.ai.modelos.CampoSugeridoModel> campos) {
		this.tipoProgramado = ResultadoClasificacionModel.CODIGO_DESCONOCIDO;
		this.confianzaClasificacion = new BigDecimal("0.2000");
		this.nombreSugerido = nombreSugerido;
		this.camposSugeridos = new ArrayList<>(campos);
	}

	public List<SolicitudClasificacionModel> clasificaciones() {
		return clasificaciones;
	}

	@Override
	public ResultadoClasificacionModel clasificar(SolicitudClasificacionModel solicitud) {
		clasificaciones.add(solicitud);
		ResultadoClasificacionModel resultado = new ResultadoClasificacionModel();
		resultado.setProveedor(tipo());
		resultado.setModelo("prueba");
		resultado.setConfianza(confianzaClasificacion);
		resultado.setCodigoPropuesto(tipoProgramado == null
				? ResultadoClasificacionModel.CODIGO_DESCONOCIDO : tipoProgramado);
		resultado.setMotivo("Clasificacion programada por la prueba");
		resultado.setNombreSugerido(nombreSugerido);
		resultado.getCamposSugeridos().addAll(camposSugeridos);
		return resultado;
	}

	@Override
	public ProveedorDocumentalIa tipo() {
		return ProveedorDocumentalIa.ABBYY;
	}

	@Override
	public boolean estaDisponible() {
		return true;
	}

	@Override
	public synchronized ResultadoExtraccionModel extraer(SolicitudExtraccionModel solicitud) {
		llamadas.incrementAndGet();
		solicitudes.add(solicitud);
		if (fallosPendientes > 0) {
			fallosPendientes--;
			throw new ProveedorNoDisponibleException("CUOTA_EXCEDIDA simulada", falloReintentable);
		}
		ResultadoExtraccionModel resultado = new ResultadoExtraccionModel();
		resultado.setProveedor(tipo());
		resultado.setModelo("prueba-v1");
		resultado.setVersionPrompt(solicitud.getVersionPrompt());
		resultado.setVersionEsquema(solicitud.getVersionEsquema());
		resultado.setTipoDetectado(solicitud.getCodigoPlantilla());
		resultado.setPaginasProcesadas(Math.max(solicitud.getPaginas(), 1));
		resultado.setTokensEntrada(tokensEntrada);
		resultado.setTokensSalida(tokensSalida);
		resultado.setCosto(costo);
		resultado.setMonedaCosto("USD");
		for (CampoEsquemaModel campo : solicitud.getCampos()) {
			resultado.getValores().add(resolver(campo));
		}
		return resultado;
	}

	private ValorCanonicoModel resolver(CampoEsquemaModel campo) {
		ValorCanonicoModel programado = valoresProgramados.get(campo.getClave());
		if (programado != null) {
			return copiar(programado);
		}
		ValorCanonicoModel valor = new ValorCanonicoModel();
		valor.setClaveCampo(campo.getClave());
		valor.setPresencia(PresenciaCampo.PRESENTE);
		valor.setValorCrudo("valor-" + campo.getClave());
		valor.setValorNormalizado("valor-" + campo.getClave());
		valor.setConfianza(confianzaPorDefecto);
		valor.setConfianzaProveedor(confianzaPorDefecto);
		valor.setEvidenciaPagina(1);
		return valor;
	}

	private ValorCanonicoModel copiar(ValorCanonicoModel origen) {
		ValorCanonicoModel copia = new ValorCanonicoModel();
		copia.setClaveCampo(origen.getClaveCampo());
		copia.setPresencia(origen.getPresencia());
		copia.setValorCrudo(origen.getValorCrudo());
		copia.setValorNormalizado(origen.getValorNormalizado());
		copia.setConfianza(origen.getConfianza());
		copia.setConfianzaProveedor(origen.getConfianzaProveedor());
		copia.setEvidenciaPagina(origen.getEvidenciaPagina());
		return copia;
	}

	public synchronized void programar(String clave, PresenciaCampo presencia, String valor, String confianza) {
		ValorCanonicoModel modelo = new ValorCanonicoModel();
		modelo.setClaveCampo(clave);
		modelo.setPresencia(presencia);
		modelo.setValorCrudo(valor);
		modelo.setValorNormalizado(valor);
		modelo.setConfianza(new BigDecimal(confianza));
		modelo.setConfianzaProveedor(new BigDecimal(confianza));
		modelo.setEvidenciaPagina(1);
		valoresProgramados.put(clave, modelo);
	}

	public synchronized void programarFallos(int cantidad, boolean reintentable) {
		this.fallosPendientes = cantidad;
		this.falloReintentable = reintentable;
	}

	public synchronized void programarUso(long tokensEntrada, long tokensSalida, String costo) {
		this.tokensEntrada = tokensEntrada;
		this.tokensSalida = tokensSalida;
		this.costo = new BigDecimal(costo);
	}

	public synchronized void reiniciar() {
		valoresProgramados.clear();
		solicitudes.clear();
		clasificaciones.clear();
		tipoProgramado = null;
		confianzaClasificacion = new BigDecimal("0.9500");
		nombreSugerido = null;
		camposSugeridos = new ArrayList<>();
		llamadas.set(0);
		fallosPendientes = 0;
		falloReintentable = true;
		confianzaPorDefecto = new BigDecimal("1.0000");
		tokensEntrada = 100;
		tokensSalida = 50;
		costo = BigDecimal.ZERO;
	}

	public int cantidadLlamadas() {
		return llamadas.get();
	}

	public synchronized List<SolicitudExtraccionModel> solicitudes() {
		return List.copyOf(solicitudes);
	}
}
