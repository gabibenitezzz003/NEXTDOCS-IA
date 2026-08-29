package com.nextdocs.ai.servicios;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.nextdocs.ai.config.PropiedadesCorreo;
import com.nextdocs.ai.entidades.BuzonCorreo;
import com.nextdocs.ai.enumeraciones.EstadoBuzonCorreo;
import com.nextdocs.ai.modelos.CorreoCrudoModel;
import com.nextdocs.ai.modelos.LecturaBuzonModel;
import com.nextdocs.ai.modelos.MensajeCorreoModel;
import com.nextdocs.ai.repositorios.BuzonCorreoRepository;
import com.nextdocs.ai.utiles.ContextoCorrelacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TrabajadorCorreoService {

	private static final Logger log = LoggerFactory.getLogger(TrabajadorCorreoService.class);

	private final BuzonCorreoRepository buzonCorreoRepository;

	private final LectorCorreoService lectorCorreoService;

	private final IngestaCorreoService ingestaCorreoService;

	private final PropiedadesCorreo propiedades;

	public TrabajadorCorreoService(BuzonCorreoRepository buzonCorreoRepository,
			LectorCorreoService lectorCorreoService, IngestaCorreoService ingestaCorreoService,
			PropiedadesCorreo propiedades) {
		this.buzonCorreoRepository = buzonCorreoRepository;
		this.lectorCorreoService = lectorCorreoService;
		this.ingestaCorreoService = ingestaCorreoService;
		this.propiedades = propiedades;
	}

	@Scheduled(fixedDelayString = "${nextdocs.correo.intervaloMilisegundos:30000}",
			initialDelayString = "${nextdocs.correo.retrasoInicialMilisegundos:40000}")
	public void revisarBuzones() {
		if (!propiedades.isActivo()) {
			return;
		}
		for (BuzonCorreo buzon : buzonCorreoRepository.listarActivos()) {
			revisar(buzon.getId());
		}
	}

	public LecturaBuzonModel revisar(String buzonId) {
		BuzonCorreo buzon = buzonCorreoRepository.buscarConTenant(buzonId).orElse(null);
		if (buzon == null) {
			return null;
		}
		LecturaBuzonModel lectura = new LecturaBuzonModel();
		lectura.setBuzonId(buzon.getId());
		lectura.setDireccion(buzon.getDireccion());

		List<CorreoCrudoModel> correos;
		try {
			correos = lectorCorreoService.leerNoLeidos(buzon, propiedades.getMensajesPorCiclo());
		}
		catch (Exception e) {
			registrarFallo(buzon, e.getMessage());
			lectura.setError(e.getMessage());
			return lectura;
		}

		lectura.setMensajesLeidos(correos.size());
		List<MensajeCorreoModel> procesados = new ArrayList<>();
		for (CorreoCrudoModel correo : correos) {
			try {
				MensajeCorreoModel mensaje = ingestaCorreoService.procesar(buzon, correo);
				if (mensaje != null) {
					procesados.add(mensaje);
					lectura.setDocumentosIngestados(lectura.getDocumentosIngestados() + mensaje.getIngestados());
				}
			}
			catch (Exception e) {
				log.error("Fallo el procesamiento del mensaje {} del buzon {}", correo.getIdentificadorMensaje(),
						buzon.getDireccion(), e);
			}
			finally {
				ContextoCorrelacion.limpiar();
			}
		}
		lectura.setMensajesProcesados(procesados.size());
		lectura.setMensajes(procesados);
		registrarExito(buzon);
		return lectura;
	}

	public void registrarExito(BuzonCorreo buzon) {
		buzon.setUltimaLectura(Instant.now());
		buzon.setFallosConsecutivos(0);
		buzon.setUltimoError(null);
		buzonCorreoRepository.save(buzon);
	}

	public void registrarFallo(BuzonCorreo buzon, String error) {
		buzon.setFallosConsecutivos(buzon.getFallosConsecutivos() + 1);
		buzon.setUltimoError(error);
		if (buzon.getFallosConsecutivos() >= propiedades.getFallosParaPausar()) {
			buzon.setEstado(EstadoBuzonCorreo.ERROR);
			log.error("El buzon {} quedo en ERROR tras {} fallos consecutivos: {}", buzon.getDireccion(),
					buzon.getFallosConsecutivos(), error);
		}
		buzonCorreoRepository.save(buzon);
	}
}
