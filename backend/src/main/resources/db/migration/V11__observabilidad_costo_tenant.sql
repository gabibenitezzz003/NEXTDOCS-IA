CREATE INDEX ix_ejecucion_extraccion_tenant_alta ON ejecucion_extraccion (tenant_id, alta);

CREATE TABLE politica_costo_tenant (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36) NOT NULL,
	activo BOOLEAN NOT NULL DEFAULT FALSE,
	presupuesto_mensual NUMERIC(14, 6),
	umbral_alerta NUMERIC(5, 4) NOT NULL DEFAULT 0.8000,
	accion_al_exceder VARCHAR(32) NOT NULL DEFAULT 'ALERTA',
	moneda VARCHAR(8) NOT NULL DEFAULT 'USD',
	alta TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_politica_costo_tenant PRIMARY KEY (id),
	CONSTRAINT fk_politica_costo_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT uq_politica_costo_tenant UNIQUE (tenant_id)
);
