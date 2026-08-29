package com.nextdocs.ai.interfaces;

import com.nextdocs.ai.enumeraciones.ProveedorDocumentalIa;
import com.nextdocs.ai.modelos.ResultadoClasificacionModel;
import com.nextdocs.ai.modelos.ResultadoExtraccionModel;
import com.nextdocs.ai.modelos.SolicitudClasificacionModel;
import com.nextdocs.ai.modelos.SolicitudExtraccionModel;

public interface ProveedorDocumentalIaInt {

	ProveedorDocumentalIa tipo();

	boolean estaDisponible();

	ResultadoExtraccionModel extraer(SolicitudExtraccionModel solicitud);

	ResultadoClasificacionModel clasificar(SolicitudClasificacionModel solicitud);
}
