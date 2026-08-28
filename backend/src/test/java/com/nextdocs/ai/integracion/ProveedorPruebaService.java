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
import com.nextdocs.ai.modelos.ResultadoExtraccionModel;
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

	private int fallosPendientes;

	private boolean falloReintentable = true;

	private BigDecimal confianzaPorDefecto = new BigDecimal("1.0000");

	private long tokensEntrada = 100;

	private long tokensSalida = 50;

	private BigDecimal costo = BigDecimal.ZERO;

	@Override
	public ProveedorDocumentalIa tipo() {
		return ProveedorDocumentalIa.DEEPSEEK;
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
