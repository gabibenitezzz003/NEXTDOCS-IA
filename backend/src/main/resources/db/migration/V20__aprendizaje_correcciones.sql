CREATE TABLE correccion_aprendida (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	plantilla_codigo VARCHAR(64) NOT NULL,
	emisor_clave VARCHAR(128) NOT NULL,
	clave_campo VARCHAR(64) NOT NULL,
	valor_leido VARCHAR(256) NOT NULL,
	valor_corregido VARCHAR(256),
	veces INTEGER NOT NULL DEFAULT 0,
	ultimo_documento_id VARCHAR(36),
	alta TIMESTAMP(6) WITH TIME ZONE,
	actualizado TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_correccion_aprendida PRIMARY KEY (id),
	CONSTRAINT uq_correccion_aprendida UNIQUE (tenant_id, plantilla_codigo, emisor_clave, clave_campo, valor_leido),
	CONSTRAINT fk_correccion_aprendida_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_correccion_aprendida_documento FOREIGN KEY (ultimo_documento_id) REFERENCES documento (id)
);

CREATE INDEX ix_correccion_aprendida_busqueda
	ON correccion_aprendida (tenant_id, plantilla_codigo, emisor_clave);
