CREATE TABLE buzon_correo (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	direccion VARCHAR(256) NOT NULL,
	nombre VARCHAR(128) NOT NULL,
	estado VARCHAR(32) NOT NULL,
	host_entrada VARCHAR(256) NOT NULL,
	puerto_entrada INTEGER NOT NULL DEFAULT 0,
	usuario_entrada VARCHAR(256) NOT NULL,
	referencia_secreto_entrada VARCHAR(256) NOT NULL,
	carpeta VARCHAR(128) NOT NULL,
	entrada_segura BOOLEAN NOT NULL DEFAULT FALSE,
	host_salida VARCHAR(256),
	puerto_salida INTEGER NOT NULL DEFAULT 0,
	usuario_salida VARCHAR(256),
	referencia_secreto_salida VARCHAR(256),
	salida_segura BOOLEAN NOT NULL DEFAULT FALSE,
	codigo_plantilla_por_defecto VARCHAR(64),
	exigir_remitente_autorizado BOOLEAN NOT NULL DEFAULT TRUE,
	exigir_correlacion BOOLEAN NOT NULL DEFAULT FALSE,
	acusar_recibo BOOLEAN NOT NULL DEFAULT FALSE,
	maximo_adjuntos_por_mensaje INTEGER NOT NULL DEFAULT 10,
	ultima_lectura TIMESTAMP(6) WITH TIME ZONE,
	ultimo_error TEXT,
	fallos_consecutivos INTEGER NOT NULL DEFAULT 0,
	alta TIMESTAMP(6) WITH TIME ZONE,
	baja TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_buzon_correo PRIMARY KEY (id),
	CONSTRAINT uq_buzon_correo_direccion UNIQUE (direccion),
	CONSTRAINT fk_buzon_correo_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);

CREATE INDEX ix_buzon_correo_tenant ON buzon_correo (tenant_id, alta);

CREATE TABLE remitente_autorizado (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	buzon_id VARCHAR(36),
	patron VARCHAR(256) NOT NULL,
	descripcion VARCHAR(256),
	alta TIMESTAMP(6) WITH TIME ZONE,
	baja TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_remitente_autorizado PRIMARY KEY (id),
	CONSTRAINT fk_remitente_autorizado_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_remitente_autorizado_buzon FOREIGN KEY (buzon_id) REFERENCES buzon_correo (id)
);

CREATE INDEX ix_remitente_autorizado_buzon ON remitente_autorizado (buzon_id, patron);

CREATE TABLE correlacion_correo (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	buzon_id VARCHAR(36),
	token VARCHAR(32) NOT NULL,
	sujeto_origen VARCHAR(64),
	sujeto_tipo_objeto VARCHAR(64),
	sujeto_id_objeto VARCHAR(128),
	sujeto_tenant_origen VARCHAR(128),
	codigo_plantilla VARCHAR(64),
	destinatario VARCHAR(256),
	descripcion VARCHAR(256),
	vence_en TIMESTAMP(6) WITH TIME ZONE,
	documentos_recibidos INTEGER NOT NULL DEFAULT 0,
	ultimo_uso TIMESTAMP(6) WITH TIME ZONE,
	alta TIMESTAMP(6) WITH TIME ZONE,
	baja TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_correlacion_correo PRIMARY KEY (id),
	CONSTRAINT uq_correlacion_correo_token UNIQUE (token),
	CONSTRAINT fk_correlacion_correo_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_correlacion_correo_buzon FOREIGN KEY (buzon_id) REFERENCES buzon_correo (id)
);

CREATE INDEX ix_correlacion_correo_tenant ON correlacion_correo (tenant_id, alta);

CREATE TABLE mensaje_correo_entrante (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	buzon_id VARCHAR(36),
	correlacion_id VARCHAR(36),
	identificador_mensaje VARCHAR(400) NOT NULL,
	remitente VARCHAR(256),
	destinatarios VARCHAR(400),
	asunto VARCHAR(400),
	token_detectado VARCHAR(32),
	resultado VARCHAR(32) NOT NULL,
	motivo TEXT,
	adjuntos INTEGER NOT NULL DEFAULT 0,
	ingestados INTEGER NOT NULL DEFAULT 0,
	rechazados INTEGER NOT NULL DEFAULT 0,
	correlacion_traza VARCHAR(36),
	enviado_en TIMESTAMP(6) WITH TIME ZONE,
	alta TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_mensaje_correo_entrante PRIMARY KEY (id),
	CONSTRAINT uq_mensaje_correo_entrante UNIQUE (buzon_id, identificador_mensaje),
	CONSTRAINT fk_mensaje_correo_entrante_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_mensaje_correo_entrante_buzon FOREIGN KEY (buzon_id) REFERENCES buzon_correo (id),
	CONSTRAINT fk_mensaje_correo_entrante_correlacion FOREIGN KEY (correlacion_id)
		REFERENCES correlacion_correo (id)
);

CREATE INDEX ix_mensaje_correo_entrante_tenant ON mensaje_correo_entrante (tenant_id, alta);

CREATE TABLE adjunto_correo (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	mensaje_id VARCHAR(36),
	documento_id VARCHAR(36),
	nombre_archivo VARCHAR(400),
	tipo_mime VARCHAR(128),
	tamano_bytes BIGINT NOT NULL DEFAULT 0,
	sha256 VARCHAR(64),
	resultado VARCHAR(32) NOT NULL,
	codigo_rechazo VARCHAR(64),
	motivo VARCHAR(400),
	alta TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_adjunto_correo PRIMARY KEY (id),
	CONSTRAINT fk_adjunto_correo_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_adjunto_correo_mensaje FOREIGN KEY (mensaje_id) REFERENCES mensaje_correo_entrante (id),
	CONSTRAINT fk_adjunto_correo_documento FOREIGN KEY (documento_id) REFERENCES documento (id)
);

CREATE INDEX ix_adjunto_correo_mensaje ON adjunto_correo (mensaje_id, alta);

CREATE TABLE mensaje_correo_saliente (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	buzon_id VARCHAR(36),
	correlacion_id VARCHAR(36),
	plantilla VARCHAR(48) NOT NULL,
	destinatarios VARCHAR(400) NOT NULL,
	asunto VARCHAR(400),
	identificador_mensaje VARCHAR(400),
	estado VARCHAR(32) NOT NULL,
	detalle_error TEXT,
	enviado TIMESTAMP(6) WITH TIME ZONE,
	alta TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_mensaje_correo_saliente PRIMARY KEY (id),
	CONSTRAINT fk_mensaje_correo_saliente_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_mensaje_correo_saliente_buzon FOREIGN KEY (buzon_id) REFERENCES buzon_correo (id),
	CONSTRAINT fk_mensaje_correo_saliente_correlacion FOREIGN KEY (correlacion_id)
		REFERENCES correlacion_correo (id)
);

CREATE INDEX ix_mensaje_correo_saliente_tenant ON mensaje_correo_saliente (tenant_id, alta);

INSERT INTO rol_permiso (rol_id, permiso)
SELECT r.id, 'canales.administrar'
FROM rol r
WHERE r.predefinido = TRUE
	AND r.codigo = 'ADMINISTRADOR'
	AND NOT EXISTS (
		SELECT 1 FROM rol_permiso rp WHERE rp.rol_id = r.id AND rp.permiso = 'canales.administrar'
	);

INSERT INTO rol_permiso (rol_id, permiso)
SELECT r.id, 'canales.leer'
FROM rol r
WHERE r.predefinido = TRUE
	AND r.codigo IN ('ADMINISTRADOR', 'OPERADOR', 'REVISOR', 'AUDITOR')
	AND NOT EXISTS (
		SELECT 1 FROM rol_permiso rp WHERE rp.rol_id = r.id AND rp.permiso = 'canales.leer'
	);
