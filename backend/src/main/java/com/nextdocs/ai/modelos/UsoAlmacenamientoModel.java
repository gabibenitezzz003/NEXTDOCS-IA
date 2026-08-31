package com.nextdocs.ai.modelos;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Data;

@Data
public class UsoAlmacenamientoModel implements Serializable {

	private static final long serialVersionUID = 5583020117446382290L;

	private long bytesUsados;

	private long cuotaBytes;

	private BigDecimal porcentaje;

	private Integer umbralAlcanzado;

	private String presentacion;

	private Map<String, Long> porClase = new LinkedHashMap<>();
}
