package com.nextdocs.ai.integracion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.exceptions.ConectorNoDisponibleException;
import com.nextdocs.ai.interfaces.ConectorAsociacionInt;
import com.nextdocs.ai.modelos.CandidatoAsociacionModel;
import com.nextdocs.ai.modelos.ContextoAsociacionModel;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("prueba")
public class ConectorPruebaService implements ConectorAsociacionInt {

	public static final String CODIGO = "PRUEBA";

	private final List<CandidatoAsociacionModel> candidatos = new ArrayList<>();

	private boolean fallar;

	private String mensajeFallo = "El conector de prueba simula un timeout";

	@Override
	public String codigo() {
		return CODIGO;
	}

	@Override
	public boolean estaHabilitado(String tenantId) {
		return true;
	}

	@Override
	public synchronized List<CandidatoAsociacionModel> buscarCandidatos(ContextoAsociacionModel contexto) {
		if (fallar) {
			throw new ConectorNoDisponibleException(CODIGO, mensajeFallo, false);
		}
		return List.copyOf(candidatos);
	}

	public synchronized void agregarCandidato(String idObjeto, String descripcion, String puntaje) {
		CandidatoAsociacionModel candidato = new CandidatoAsociacionModel();
		candidato.setConector(CODIGO);
		candidato.setOrigen(CODIGO);
		candidato.setTipoObjeto("PEDIDO");
		candidato.setIdObjeto(idObjeto);
		candidato.setDescripcion(descripcion);
		candidato.setPuntaje(new BigDecimal(puntaje));
		candidato.setRazones("prueba");
		candidatos.add(candidato);
	}

	public synchronized void programarFallo(boolean fallar) {
		this.fallar = fallar;
	}

	public synchronized void reiniciar() {
		candidatos.clear();
		fallar = false;
	}
}
