ALTER TABLE plantilla_documental
	ADD COLUMN exigir_quality_gate BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE plantilla_documental
	ADD COLUMN umbral_quality_gate NUMERIC(5, 4) NOT NULL DEFAULT 0.8000;

ALTER TABLE version_plantilla
	ADD COLUMN factor_calibracion_confianza NUMERIC(5, 4);

CREATE TABLE conjunto_prueba_plantilla (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	plantilla_id VARCHAR(36) NOT NULL,
	huella VARCHAR(64),
	alta TIMESTAMP(6) WITH TIME ZONE,
	baja TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_conjunto_prueba_plantilla PRIMARY KEY (id),
	CONSTRAINT fk_conjunto_prueba_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_conjunto_prueba_plantilla FOREIGN KEY (plantilla_id) REFERENCES plantilla_documental (id)
);

CREATE UNIQUE INDEX uq_conjunto_prueba_plantilla_activa ON conjunto_prueba_plantilla (plantilla_id)
	WHERE baja IS NULL;

CREATE TABLE caso_prueba_plantilla (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	conjunto_id VARCHAR(36) NOT NULL,
	nombre VARCHAR(128) NOT NULL,
	esperado JSONB NOT NULL,
	clave_objeto VARCHAR(512) NOT NULL,
	nombre_archivo VARCHAR(256) NOT NULL,
	tipo_mime VARCHAR(128) NOT NULL,
	orden INTEGER NOT NULL DEFAULT 0,
	alta TIMESTAMP(6) WITH TIME ZONE,
	baja TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_caso_prueba_plantilla PRIMARY KEY (id),
	CONSTRAINT fk_caso_prueba_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_caso_prueba_conjunto FOREIGN KEY (conjunto_id) REFERENCES conjunto_prueba_plantilla (id)
);

CREATE INDEX ix_caso_prueba_conjunto ON caso_prueba_plantilla (conjunto_id)
	WHERE baja IS NULL;

CREATE TABLE ejecucion_prueba_plantilla (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	version_plantilla_id VARCHAR(36) NOT NULL,
	conjunto_id VARCHAR(36) NOT NULL,
	huella_conjunto VARCHAR(64) NOT NULL,
	exactitud NUMERIC(7, 4) NOT NULL DEFAULT 0,
	exactitud_referencia NUMERIC(7, 4),
	factor_calibracion NUMERIC(5, 4),
	resultado VARCHAR(32) NOT NULL,
	motivo VARCHAR(1024),
	alta TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_ejecucion_prueba_plantilla PRIMARY KEY (id),
	CONSTRAINT fk_ejecucion_prueba_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_ejecucion_prueba_version FOREIGN KEY (version_plantilla_id) REFERENCES version_plantilla (id),
	CONSTRAINT fk_ejecucion_prueba_conjunto FOREIGN KEY (conjunto_id) REFERENCES conjunto_prueba_plantilla (id)
);

CREATE INDEX ix_ejecucion_prueba_version ON ejecucion_prueba_plantilla (version_plantilla_id, alta DESC);

CREATE TABLE resultado_caso_prueba (
	id VARCHAR(36) NOT NULL,
	ejecucion_id VARCHAR(36) NOT NULL,
	caso_id VARCHAR(36) NOT NULL,
	aciertos INTEGER NOT NULL DEFAULT 0,
	total INTEGER NOT NULL DEFAULT 0,
	exactitud NUMERIC(7, 4) NOT NULL DEFAULT 0,
	extraido JSONB,
	CONSTRAINT pk_resultado_caso_prueba PRIMARY KEY (id),
	CONSTRAINT fk_resultado_caso_ejecucion FOREIGN KEY (ejecucion_id) REFERENCES ejecucion_prueba_plantilla (id),
	CONSTRAINT fk_resultado_caso_caso FOREIGN KEY (caso_id) REFERENCES caso_prueba_plantilla (id)
);
