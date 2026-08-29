CREATE TABLE linea_whatsapp (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	nombre VARCHAR(128) NOT NULL,
	numero_telefono VARCHAR(32) NOT NULL,
	identificador_numero VARCHAR(64) NOT NULL,
	identificador_cuenta VARCHAR(64),
	ruta_webhook VARCHAR(64) NOT NULL,
	referencia_token_acceso VARCHAR(256) NOT NULL,
	referencia_secreto_aplicacion VARCHAR(256) NOT NULL,
	referencia_token_verificacion VARCHAR(256) NOT NULL,
	estado VARCHAR(32) NOT NULL,
	codigo_plantilla_por_defecto VARCHAR(64),
	exigir_contacto_autorizado BOOLEAN NOT NULL DEFAULT TRUE,
	exigir_correlacion BOOLEAN NOT NULL DEFAULT FALSE,
	acusar_recibo BOOLEAN NOT NULL DEFAULT FALSE,
	maximo_media_por_mensaje INTEGER NOT NULL DEFAULT 10,
	minutos_ventana_correlacion INTEGER NOT NULL DEFAULT 1440,
	nombre_plantilla_solicitud VARCHAR(128),
	idioma_plantilla_solicitud VARCHAR(16),
	ultimo_mensaje TIMESTAMP(6) WITH TIME ZONE,
	ultimo_error TEXT,
	fallos_consecutivos INTEGER NOT NULL DEFAULT 0,
	alta TIMESTAMP(6) WITH TIME ZONE,
	baja TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_linea_whatsapp PRIMARY KEY (id),
	CONSTRAINT uq_linea_whatsapp_numero UNIQUE (identificador_numero),
	CONSTRAINT uq_linea_whatsapp_ruta UNIQUE (ruta_webhook),
	CONSTRAINT fk_linea_whatsapp_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);

CREATE INDEX ix_linea_whatsapp_tenant ON linea_whatsapp (tenant_id, alta);

CREATE TABLE contacto_whatsapp_autorizado (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	linea_id VARCHAR(36),
	patron VARCHAR(64) NOT NULL,
	descripcion VARCHAR(256),
	alta TIMESTAMP(6) WITH TIME ZONE,
	baja TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_contacto_whatsapp_autorizado PRIMARY KEY (id),
	CONSTRAINT fk_contacto_whatsapp_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_contacto_whatsapp_linea FOREIGN KEY (linea_id) REFERENCES linea_whatsapp (id)
);

CREATE INDEX ix_contacto_whatsapp_linea ON contacto_whatsapp_autorizado (linea_id, patron);

CREATE TABLE correlacion_whatsapp (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	linea_id VARCHAR(36),
	token VARCHAR(32) NOT NULL,
	sujeto_origen VARCHAR(64),
	sujeto_tipo_objeto VARCHAR(64),
	sujeto_id_objeto VARCHAR(128),
	sujeto_tenant_origen VARCHAR(128),
	codigo_plantilla VARCHAR(64),
	numero_destino VARCHAR(32),
	numero_vinculado VARCHAR(32),
	vinculado_en TIMESTAMP(6) WITH TIME ZONE,
	descripcion VARCHAR(256),
	vence_en TIMESTAMP(6) WITH TIME ZONE,
	documentos_recibidos INTEGER NOT NULL DEFAULT 0,
	ultimo_uso TIMESTAMP(6) WITH TIME ZONE,
	alta TIMESTAMP(6) WITH TIME ZONE,
	baja TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_correlacion_whatsapp PRIMARY KEY (id),
	CONSTRAINT uq_correlacion_whatsapp_token UNIQUE (token),
	CONSTRAINT fk_correlacion_whatsapp_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_correlacion_whatsapp_linea FOREIGN KEY (linea_id) REFERENCES linea_whatsapp (id)
);

CREATE INDEX ix_correlacion_whatsapp_vinculo ON correlacion_whatsapp (linea_id, numero_vinculado);

CREATE TABLE mensaje_whatsapp_entrante (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	linea_id VARCHAR(36),
	correlacion_id VARCHAR(36),
	identificador_mensaje VARCHAR(128) NOT NULL,
	numero_origen VARCHAR(32),
	nombre_perfil VARCHAR(128),
	tipo VARCHAR(32),
	texto VARCHAR(1024),
	token_detectado VARCHAR(32),
	resultado VARCHAR(32) NOT NULL,
	motivo TEXT,
	media INTEGER NOT NULL DEFAULT 0,
	ingestados INTEGER NOT NULL DEFAULT 0,
	rechazados INTEGER NOT NULL DEFAULT 0,
	correlacion_traza VARCHAR(36),
	recibido_en TIMESTAMP(6) WITH TIME ZONE,
	alta TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_mensaje_whatsapp_entrante PRIMARY KEY (id),
	CONSTRAINT uq_mensaje_whatsapp_entrante UNIQUE (linea_id, identificador_mensaje),
	CONSTRAINT fk_mensaje_whatsapp_entrante_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_mensaje_whatsapp_entrante_linea FOREIGN KEY (linea_id) REFERENCES linea_whatsapp (id),
	CONSTRAINT fk_mensaje_whatsapp_entrante_correlacion FOREIGN KEY (correlacion_id)
		REFERENCES correlacion_whatsapp (id)
);

CREATE INDEX ix_mensaje_whatsapp_entrante_tenant ON mensaje_whatsapp_entrante (tenant_id, alta);

CREATE INDEX ix_mensaje_whatsapp_entrante_origen
	ON mensaje_whatsapp_entrante (linea_id, numero_origen, alta);

CREATE TABLE media_whatsapp (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	mensaje_id VARCHAR(36),
	documento_id VARCHAR(36),
	identificador_media VARCHAR(128),
	nombre_archivo VARCHAR(400),
	tipo_mime VARCHAR(128),
	tamano_bytes BIGINT NOT NULL DEFAULT 0,
	sha256 VARCHAR(64),
	resultado VARCHAR(32) NOT NULL,
	codigo_rechazo VARCHAR(64),
	motivo VARCHAR(400),
	alta TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_media_whatsapp PRIMARY KEY (id),
	CONSTRAINT fk_media_whatsapp_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_media_whatsapp_mensaje FOREIGN KEY (mensaje_id) REFERENCES mensaje_whatsapp_entrante (id),
	CONSTRAINT fk_media_whatsapp_documento FOREIGN KEY (documento_id) REFERENCES documento (id)
);

CREATE INDEX ix_media_whatsapp_mensaje ON media_whatsapp (mensaje_id, alta);

CREATE TABLE mensaje_whatsapp_saliente (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	linea_id VARCHAR(36),
	correlacion_id VARCHAR(36),
	plantilla VARCHAR(48) NOT NULL,
	numero_destino VARCHAR(32) NOT NULL,
	identificador_mensaje VARCHAR(128),
	estado VARCHAR(32) NOT NULL,
	dentro_de_ventana BOOLEAN NOT NULL DEFAULT FALSE,
	nombre_plantilla_meta VARCHAR(128),
	cuerpo TEXT,
	detalle_error TEXT,
	enviado TIMESTAMP(6) WITH TIME ZONE,
	alta TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_mensaje_whatsapp_saliente PRIMARY KEY (id),
	CONSTRAINT fk_mensaje_whatsapp_saliente_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_mensaje_whatsapp_saliente_linea FOREIGN KEY (linea_id) REFERENCES linea_whatsapp (id),
	CONSTRAINT fk_mensaje_whatsapp_saliente_correlacion FOREIGN KEY (correlacion_id)
		REFERENCES correlacion_whatsapp (id)
);

CREATE INDEX ix_mensaje_whatsapp_saliente_tenant ON mensaje_whatsapp_saliente (tenant_id, alta);
