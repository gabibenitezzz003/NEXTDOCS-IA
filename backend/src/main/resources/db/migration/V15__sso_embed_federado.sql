CREATE TABLE proveedor_identidad (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	codigo VARCHAR(64) NOT NULL,
	nombre VARCHAR(128) NOT NULL,
	origen VARCHAR(32) NOT NULL,
	emisor VARCHAR(512) NOT NULL,
	url_jwks VARCHAR(512) NOT NULL,
	audiencia VARCHAR(256),
	claim_sujeto VARCHAR(64) NOT NULL,
	claim_email VARCHAR(64) NOT NULL,
	claim_nombre VARCHAR(64) NOT NULL,
	permitir_jit BOOLEAN NOT NULL DEFAULT FALSE,
	permitir_vinculo_por_email BOOLEAN NOT NULL DEFAULT FALSE,
	codigo_rol_por_defecto VARCHAR(64),
	dominios_permitidos TEXT,
	origenes_embed_permitidos TEXT,
	segundos_vigencia_codigo INTEGER NOT NULL DEFAULT 0,
	activo BOOLEAN NOT NULL DEFAULT TRUE,
	alta TIMESTAMP(6) WITH TIME ZONE,
	baja TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_proveedor_identidad PRIMARY KEY (id),
	CONSTRAINT uq_proveedor_identidad UNIQUE (tenant_id, codigo),
	CONSTRAINT fk_proveedor_identidad_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);

CREATE TABLE codigo_embed (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	usuario_id VARCHAR(36),
	proveedor_id VARCHAR(36),
	codigo_hash VARCHAR(64) NOT NULL,
	sujeto_externo VARCHAR(128),
	aplicacion_origen VARCHAR(64),
	tipo_objeto VARCHAR(64),
	id_objeto VARCHAR(128),
	url_retorno VARCHAR(512),
	vence_en TIMESTAMP(6) WITH TIME ZONE,
	usado_en TIMESTAMP(6) WITH TIME ZONE,
	correlacion_id VARCHAR(64),
	alta TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_codigo_embed PRIMARY KEY (id),
	CONSTRAINT uq_codigo_embed_hash UNIQUE (codigo_hash),
	CONSTRAINT fk_codigo_embed_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_codigo_embed_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
	CONSTRAINT fk_codigo_embed_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedor_identidad (id)
);

CREATE INDEX ix_codigo_embed_tenant ON codigo_embed (tenant_id, alta);
