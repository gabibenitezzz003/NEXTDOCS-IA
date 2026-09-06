ALTER TABLE plantilla_documental ADD COLUMN clasificable BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE tipo_propuesto (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	codigo_sugerido VARCHAR(64) NOT NULL,
	nombre_sugerido VARCHAR(128),
	motivo VARCHAR(400),
	campos_sugeridos TEXT,
	ultimo_documento_id VARCHAR(36),
	veces INTEGER NOT NULL DEFAULT 0,
	estado VARCHAR(16) NOT NULL,
	codigo_aprobado VARCHAR(64),
	alta TIMESTAMP(6) WITH TIME ZONE,
	resuelto TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_tipo_propuesto PRIMARY KEY (id),
	CONSTRAINT uq_tipo_propuesto UNIQUE (tenant_id, codigo_sugerido),
	CONSTRAINT fk_tipo_propuesto_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_tipo_propuesto_documento FOREIGN KEY (ultimo_documento_id) REFERENCES documento (id)
);

CREATE INDEX ix_tipo_propuesto_tenant ON tipo_propuesto (tenant_id, estado);
