CREATE TABLE lote_exportacion (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	solicitado_por_id VARCHAR(36),
	estado VARCHAR(32) NOT NULL,
	nombre VARCHAR(180),
	filtros TEXT,
	orden VARCHAR(40),
	incluir_originales BOOLEAN NOT NULL DEFAULT TRUE,
	cantidad_documentos INTEGER NOT NULL DEFAULT 0,
	cantidad_omitidos INTEGER NOT NULL DEFAULT 0,
	bucket VARCHAR(120),
	clave_objeto VARCHAR(400),
	nombre_archivo VARCHAR(200),
	tamano_bytes BIGINT NOT NULL DEFAULT 0,
	sha256 VARCHAR(64),
	detalle_error TEXT,
	vence_en TIMESTAMP(6) WITH TIME ZONE,
	aviso_vencimiento TIMESTAMP(6) WITH TIME ZONE,
	generado TIMESTAMP(6) WITH TIME ZONE,
	alta TIMESTAMP(6) WITH TIME ZONE,
	baja TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_lote_exportacion PRIMARY KEY (id),
	CONSTRAINT fk_lote_exportacion_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_lote_exportacion_usuario FOREIGN KEY (solicitado_por_id) REFERENCES usuario (id)
);

CREATE INDEX ix_lote_exportacion_tenant ON lote_exportacion (tenant_id, alta DESC);

CREATE INDEX ix_lote_exportacion_estado ON lote_exportacion (estado, alta);

CREATE INDEX ix_lote_exportacion_vencimiento ON lote_exportacion (estado, vence_en);

CREATE TABLE item_exportacion (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	lote_id VARCHAR(36),
	documento_id VARCHAR(36),
	tipo_objeto VARCHAR(80),
	id_objeto VARCHAR(120),
	codigo_plantilla VARCHAR(80),
	numero_version_plantilla INTEGER NOT NULL DEFAULT 0,
	estado_documento VARCHAR(32),
	nombre_en_archivo VARCHAR(400),
	tamano_bytes BIGINT NOT NULL DEFAULT 0,
	sha256 VARCHAR(64),
	hallazgos INTEGER NOT NULL DEFAULT 0,
	motivo_omision VARCHAR(200),
	recibido TIMESTAMP(6) WITH TIME ZONE,
	cerrado TIMESTAMP(6) WITH TIME ZONE,
	alta TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_item_exportacion PRIMARY KEY (id),
	CONSTRAINT fk_item_exportacion_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_item_exportacion_lote FOREIGN KEY (lote_id) REFERENCES lote_exportacion (id),
	CONSTRAINT fk_item_exportacion_documento FOREIGN KEY (documento_id) REFERENCES documento (id)
);

CREATE INDEX ix_item_exportacion_lote ON item_exportacion (lote_id, alta);

CREATE TABLE aviso_almacenamiento (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	umbral INTEGER NOT NULL,
	bytes_usados BIGINT NOT NULL DEFAULT 0,
	cuota_bytes BIGINT NOT NULL DEFAULT 0,
	alta TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_aviso_almacenamiento PRIMARY KEY (id),
	CONSTRAINT uq_aviso_almacenamiento UNIQUE (tenant_id, umbral),
	CONSTRAINT fk_aviso_almacenamiento_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);
