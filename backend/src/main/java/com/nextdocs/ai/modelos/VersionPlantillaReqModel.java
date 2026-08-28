package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.math.BigDecimal;

import com.nextdocs.ai.enumeraciones.EstrategiaSegmentacion;
import com.nextdocs.ai.enumeraciones.PoliticaOriginalFisico;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class VersionPlantillaReqModel implements Serializable {

	private static final long serialVersionUID = 2214960377258033519L;

	@DecimalMin("0.0")
	@DecimalMax("1.0")
	private BigDecimal umbralAutoaprobacion;

	private PoliticaOriginalFisico politicaOriginalFisico;

	@Size(max = 64)
	private String versionPrompt;

	@Size(max = 64)
	private String versionEsquema;

	private EstrategiaSegmentacion estrategiaSegmentacion;

	@Min(0)
	@Max(500)
	private Integer paginasPorDocumento;

	@Size(max = 512)
	private String patronInicioDocumento;

	private String instruccionExtraccion;

	@Size(max = 1024)
	private String notasCambio;

	@Size(max = 36)
	private String versionBaseId;
}
