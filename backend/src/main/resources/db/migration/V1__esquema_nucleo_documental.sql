CREATE TABLE tenant (
	id VARCHAR(36) NOT NULL,
	codigo VARCHAR(64) NOT NULL,
	nombre VARCHAR(256) NOT NULL,
	estado VARCHAR(32) NOT NULL,
	plan VARCHAR(64),
	region VARCHAR(64),
	dominio VARCHAR(256),
	cuota_almacenamiento_bytes BIGINT NOT NULL DEFAULT 0,
	alta TIMESTAMP(6) WITH TIME ZONE,
	baja TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_tenant PRIMARY KEY (id),
	CONSTRAINT uq_tenant_codigo UNIQUE (codigo)
);

CREATE TABLE rol (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	codigo VARCHAR(64) NOT NULL,
	nombre VARCHAR(128) NOT NULL,
	descripcion VARCHAR(512),
	predefinido BOOLEAN NOT NULL DEFAULT FALSE,
	alta TIMESTAMP(6) WITH TIME ZONE,
	baja TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_rol PRIMARY KEY (id),
	CONSTRAINT uq_rol_tenant_codigo UNIQUE (tenant_id, codigo),
	CONSTRAINT fk_rol_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);

CREATE TABLE rol_permiso (
	rol_id VARCHAR(36) NOT NULL,
	permiso VARCHAR(128),
	CONSTRAINT fk_rol_permiso_rol FOREIGN KEY (rol_id) REFERENCES rol (id)
);

CREATE TABLE usuario (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	email VARCHAR(256) NOT NULL,
	nombre VARCHAR(256) NOT NULL,
	clave_hash VARCHAR(256),
	estado VARCHAR(32) NOT NULL,
	origen_identidad VARCHAR(32) NOT NULL,
	id_usuario_externo VARCHAR(128),
	idioma VARCHAR(16),
	zona_horaria VARCHAR(64),
	ultimo_acceso TIMESTAMP(6) WITH TIME ZONE,
	alta TIMESTAMP(6) WITH TIME ZONE,
	baja TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_usuario PRIMARY KEY (id),
	CONSTRAINT uq_usuario_tenant_email UNIQUE (tenant_id, email),
	CONSTRAINT fk_usuario_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);

CREATE TABLE usuario_rol (
	usuario_id VARCHAR(36) NOT NULL,
	rol_id VARCHAR(36) NOT NULL,
	CONSTRAINT pk_usuario_rol PRIMARY KEY (usuario_id, rol_id),
	CONSTRAINT fk_usuario_rol_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
	CONSTRAINT fk_usuario_rol_rol FOREIGN KEY (rol_id) REFERENCES rol (id)
);

CREATE TABLE cuenta_servicio (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	nombre VARCHAR(128) NOT NULL,
	prefijo_clave VARCHAR(32) NOT NULL,
	clave_hash VARCHAR(128) NOT NULL,
	activa BOOLEAN NOT NULL DEFAULT TRUE,
	expira TIMESTAMP(6) WITH TIME ZONE,
	ultimo_uso TIMESTAMP(6) WITH TIME ZONE,
	motivo_revocacion VARCHAR(512),
	alta TIMESTAMP(6) WITH TIME ZONE,
	baja TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_cuenta_servicio PRIMARY KEY (id),
	CONSTRAINT uq_cuenta_servicio_clave_hash UNIQUE (clave_hash),
	CONSTRAINT fk_cuenta_servicio_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);

CREATE TABLE cuenta_servicio_alcance (
	cuenta_servicio_id VARCHAR(36) NOT NULL,
	alcance VARCHAR(128),
	CONSTRAINT fk_cuenta_servicio_alcance FOREIGN KEY (cuenta_servicio_id) REFERENCES cuenta_servicio (id)
);

CREATE TABLE plantilla_documental (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	codigo VARCHAR(64) NOT NULL,
	nombre VARCHAR(128) NOT NULL,
	familia VARCHAR(64),
	descripcion VARCHAR(1024),
	version_publicada_id VARCHAR(36),
	creado_por_id VARCHAR(36),
	alta TIMESTAMP(6) WITH TIME ZONE,
	baja TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_plantilla_documental PRIMARY KEY (id),
	CONSTRAINT uq_plantilla_tenant_codigo UNIQUE (tenant_id, codigo),
	CONSTRAINT fk_plantilla_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_plantilla_creado_por FOREIGN KEY (creado_por_id) REFERENCES usuario (id)
);

CREATE TABLE version_plantilla (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	plantilla_id VARCHAR(36),
	numero INTEGER NOT NULL DEFAULT 0,
	estado VARCHAR(32) NOT NULL,
	umbral_autoaprobacion NUMERIC(5, 4),
	politica_original_fisico VARCHAR(32),
	version_prompt VARCHAR(64),
	version_esquema VARCHAR(64),
	instruccion_extraccion TEXT,
	notas_cambio VARCHAR(1024),
	publicada_por_id VARCHAR(36),
	publicada TIMESTAMP(6) WITH TIME ZONE,
	alta TIMESTAMP(6) WITH TIME ZONE,
	baja TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_version_plantilla PRIMARY KEY (id),
	CONSTRAINT uq_version_plantilla_numero UNIQUE (plantilla_id, numero),
	CONSTRAINT fk_version_plantilla_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_version_plantilla_plantilla FOREIGN KEY (plantilla_id) REFERENCES plantilla_documental (id),
	CONSTRAINT fk_version_plantilla_publicada_por FOREIGN KEY (publicada_por_id) REFERENCES usuario (id)
);

ALTER TABLE plantilla_documental
	ADD CONSTRAINT fk_plantilla_version_publicada FOREIGN KEY (version_publicada_id) REFERENCES version_plantilla (id);

CREATE TABLE campo_plantilla (
	id VARCHAR(36) NOT NULL,
	version_plantilla_id VARCHAR(36),
	clave VARCHAR(128) NOT NULL,
	etiqueta VARCHAR(256) NOT NULL,
	tipo_dato VARCHAR(32) NOT NULL,
	alias VARCHAR(1024),
	descripcion VARCHAR(1024),
	requerido BOOLEAN NOT NULL DEFAULT FALSE,
	extraer BOOLEAN NOT NULL DEFAULT TRUE,
	validar BOOLEAN NOT NULL DEFAULT TRUE,
	comparar BOOLEAN NOT NULL DEFAULT FALSE,
	unico BOOLEAN NOT NULL DEFAULT FALSE,
	expresion_regular VARCHAR(512),
	formato_fecha VARCHAR(256),
	catalogo_referencia VARCHAR(128),
	umbral_confianza NUMERIC(5, 4),
	sensibilidad VARCHAR(32),
	orden INTEGER NOT NULL DEFAULT 0,
	alta TIMESTAMP(6) WITH TIME ZONE,
	baja TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_campo_plantilla PRIMARY KEY (id),
	CONSTRAINT uq_campo_plantilla_clave UNIQUE (version_plantilla_id, clave),
	CONSTRAINT fk_campo_plantilla_version FOREIGN KEY (version_plantilla_id) REFERENCES version_plantilla (id)
);

CREATE TABLE regla_plantilla (
	id VARCHAR(36) NOT NULL,
	version_plantilla_id VARCHAR(36),
	codigo VARCHAR(64) NOT NULL,
	nombre VARCHAR(256) NOT NULL,
	tipo VARCHAR(32) NOT NULL,
	severidad VARCHAR(32) NOT NULL,
	campo_objetivo VARCHAR(128),
	configuracion JSONB,
	mensaje VARCHAR(512),
	activa BOOLEAN NOT NULL DEFAULT TRUE,
	orden INTEGER NOT NULL DEFAULT 0,
	alta TIMESTAMP(6) WITH TIME ZONE,
	baja TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_regla_plantilla PRIMARY KEY (id),
	CONSTRAINT uq_regla_plantilla_codigo UNIQUE (version_plantilla_id, codigo),
	CONSTRAINT fk_regla_plantilla_version FOREIGN KEY (version_plantilla_id) REFERENCES version_plantilla (id)
);

CREATE TABLE documento (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	estado VARCHAR(32) NOT NULL,
	origen VARCHAR(32) NOT NULL,
	nombre VARCHAR(256),
	hash_contenido VARCHAR(128),
	clave_idempotencia VARCHAR(128),
	correlacion_id VARCHAR(64),
	plantilla_id VARCHAR(36),
	version_plantilla_id VARCHAR(36),
	documento_padre_id VARCHAR(36),
	pagina_desde INTEGER NOT NULL DEFAULT 0,
	pagina_hasta INTEGER NOT NULL DEFAULT 0,
	sujeto_origen VARCHAR(64),
	sujeto_tipo_objeto VARCHAR(64),
	sujeto_id_objeto VARCHAR(128),
	sujeto_tenant_origen VARCHAR(128),
	remitente VARCHAR(256),
	observacion VARCHAR(1024),
	cantidad_segmentos INTEGER NOT NULL DEFAULT 0,
	reintentos_extraccion INTEGER NOT NULL DEFAULT 0,
	ingresado_por_id VARCHAR(36),
	ingresado_por_cuenta_servicio_id VARCHAR(36),
	recibido TIMESTAMP(6) WITH TIME ZONE,
	procesado TIMESTAMP(6) WITH TIME ZONE,
	cerrado TIMESTAMP(6) WITH TIME ZONE,
	retener_hasta TIMESTAMP(6) WITH TIME ZONE,
	retencion_legal BOOLEAN NOT NULL DEFAULT FALSE,
	alta TIMESTAMP(6) WITH TIME ZONE,
	baja TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_documento PRIMARY KEY (id),
	CONSTRAINT uq_documento_idempotencia UNIQUE (tenant_id, clave_idempotencia),
	CONSTRAINT fk_documento_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_documento_plantilla FOREIGN KEY (plantilla_id) REFERENCES plantilla_documental (id),
	CONSTRAINT fk_documento_version_plantilla FOREIGN KEY (version_plantilla_id) REFERENCES version_plantilla (id),
	CONSTRAINT fk_documento_padre FOREIGN KEY (documento_padre_id) REFERENCES documento (id),
	CONSTRAINT fk_documento_ingresado_por FOREIGN KEY (ingresado_por_id) REFERENCES usuario (id),
	CONSTRAINT fk_documento_ingresado_cuenta FOREIGN KEY (ingresado_por_cuenta_servicio_id) REFERENCES cuenta_servicio (id)
);

CREATE INDEX ix_documento_tenant_estado ON documento (tenant_id, estado);
CREATE INDEX ix_documento_tenant_alta ON documento (tenant_id, alta);
CREATE INDEX ix_documento_hash ON documento (tenant_id, hash_contenido);

CREATE TABLE archivo_documento (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	documento_id VARCHAR(36),
	clave_objeto VARCHAR(512) NOT NULL,
	bucket VARCHAR(64) NOT NULL,
	nombre_archivo VARCHAR(256),
	tipo_mime VARCHAR(128),
	extension VARCHAR(16),
	tamano BIGINT NOT NULL DEFAULT 0,
	checksum VARCHAR(128),
	paginas INTEGER NOT NULL DEFAULT 0,
	version INTEGER NOT NULL DEFAULT 1,
	original BOOLEAN NOT NULL DEFAULT TRUE,
	algoritmo_cifrado VARCHAR(128),
	alta TIMESTAMP(6) WITH TIME ZONE,
	baja TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_archivo_documento PRIMARY KEY (id),
	CONSTRAINT fk_archivo_documento_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_archivo_documento_documento FOREIGN KEY (documento_id) REFERENCES documento (id)
);

CREATE INDEX ix_archivo_documento_documento ON archivo_documento (documento_id);

CREATE TABLE segmento_documento (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	documento_padre_id VARCHAR(36),
	documento_hijo_id VARCHAR(36),
	pagina_desde INTEGER NOT NULL DEFAULT 0,
	pagina_hasta INTEGER NOT NULL DEFAULT 0,
	orden INTEGER NOT NULL DEFAULT 0,
	motivo_corte VARCHAR(256),
	alta TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_segmento_documento PRIMARY KEY (id),
	CONSTRAINT fk_segmento_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_segmento_padre FOREIGN KEY (documento_padre_id) REFERENCES documento (id),
	CONSTRAINT fk_segmento_hijo FOREIGN KEY (documento_hijo_id) REFERENCES documento (id)
);

CREATE INDEX ix_segmento_documento_padre ON segmento_documento (documento_padre_id);

CREATE TABLE ejecucion_extraccion (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	documento_id VARCHAR(36),
	version_plantilla_id VARCHAR(36),
	proveedor VARCHAR(32) NOT NULL,
	modelo VARCHAR(128),
	version_prompt VARCHAR(64),
	version_esquema VARCHAR(64),
	estado VARCHAR(32) NOT NULL,
	intento INTEGER NOT NULL DEFAULT 0,
	tokens_entrada BIGINT NOT NULL DEFAULT 0,
	tokens_salida BIGINT NOT NULL DEFAULT 0,
	paginas_procesadas INTEGER NOT NULL DEFAULT 0,
	costo NUMERIC(12, 6),
	moneda_costo VARCHAR(8),
	duracion_milisegundos BIGINT NOT NULL DEFAULT 0,
	codigo_error VARCHAR(64),
	mensaje_error TEXT,
	correlacion_id VARCHAR(64),
	inicio TIMESTAMP(6) WITH TIME ZONE,
	fin TIMESTAMP(6) WITH TIME ZONE,
	alta TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_ejecucion_extraccion PRIMARY KEY (id),
	CONSTRAINT fk_ejecucion_extraccion_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_ejecucion_extraccion_documento FOREIGN KEY (documento_id) REFERENCES documento (id),
	CONSTRAINT fk_ejecucion_extraccion_version FOREIGN KEY (version_plantilla_id) REFERENCES version_plantilla (id)
);

CREATE INDEX ix_ejecucion_extraccion_documento ON ejecucion_extraccion (documento_id);

CREATE TABLE valor_extraido (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	documento_id VARCHAR(36),
	ejecucion_id VARCHAR(36),
	clave_campo VARCHAR(128) NOT NULL,
	valor_crudo TEXT,
	valor_normalizado TEXT,
	presencia VARCHAR(32) NOT NULL,
	confianza NUMERIC(5, 4),
	confianza_proveedor NUMERIC(5, 4),
	evidencia_pagina INTEGER NOT NULL DEFAULT 0,
	evidencia_recuadro VARCHAR(128),
	corregido_manualmente BOOLEAN NOT NULL DEFAULT FALSE,
	valor_anterior TEXT,
	alta TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_valor_extraido PRIMARY KEY (id),
	CONSTRAINT fk_valor_extraido_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_valor_extraido_documento FOREIGN KEY (documento_id) REFERENCES documento (id),
	CONSTRAINT fk_valor_extraido_ejecucion FOREIGN KEY (ejecucion_id) REFERENCES ejecucion_extraccion (id)
);

CREATE INDEX ix_valor_extraido_ejecucion ON valor_extraido (ejecucion_id);
CREATE INDEX ix_valor_extraido_documento_clave ON valor_extraido (documento_id, clave_campo);

CREATE TABLE ejecucion_validacion (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	documento_id VARCHAR(36),
	version_plantilla_id VARCHAR(36),
	ejecucion_extraccion_id VARCHAR(36),
	estado VARCHAR(32) NOT NULL,
	resultado VARCHAR(32),
	cantidad_hallazgos INTEGER NOT NULL DEFAULT 0,
	cantidad_bloqueantes INTEGER NOT NULL DEFAULT 0,
	autoaprobado BOOLEAN NOT NULL DEFAULT FALSE,
	motivo_resultado VARCHAR(512),
	correlacion_id VARCHAR(64),
	inicio TIMESTAMP(6) WITH TIME ZONE,
	fin TIMESTAMP(6) WITH TIME ZONE,
	alta TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_ejecucion_validacion PRIMARY KEY (id),
	CONSTRAINT fk_ejecucion_validacion_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_ejecucion_validacion_documento FOREIGN KEY (documento_id) REFERENCES documento (id),
	CONSTRAINT fk_ejecucion_validacion_version FOREIGN KEY (version_plantilla_id) REFERENCES version_plantilla (id),
	CONSTRAINT fk_ejecucion_validacion_extraccion FOREIGN KEY (ejecucion_extraccion_id) REFERENCES ejecucion_extraccion (id)
);

CREATE INDEX ix_ejecucion_validacion_documento ON ejecucion_validacion (documento_id);

CREATE TABLE hallazgo_validacion (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	ejecucion_id VARCHAR(36),
	regla_id VARCHAR(36),
	codigo_regla VARCHAR(64),
	clave_campo VARCHAR(128),
	severidad VARCHAR(32) NOT NULL,
	mensaje VARCHAR(1024),
	evidencia TEXT,
	sobreescrito BOOLEAN NOT NULL DEFAULT FALSE,
	motivo_sobreescritura VARCHAR(512),
	sobreescrito_por_id VARCHAR(36),
	alta TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_hallazgo_validacion PRIMARY KEY (id),
	CONSTRAINT fk_hallazgo_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_hallazgo_ejecucion FOREIGN KEY (ejecucion_id) REFERENCES ejecucion_validacion (id),
	CONSTRAINT fk_hallazgo_regla FOREIGN KEY (regla_id) REFERENCES regla_plantilla (id),
	CONSTRAINT fk_hallazgo_sobreescrito_por FOREIGN KEY (sobreescrito_por_id) REFERENCES usuario (id)
);

CREATE INDEX ix_hallazgo_validacion_ejecucion ON hallazgo_validacion (ejecucion_id);

CREATE TABLE candidato_asociacion (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	documento_id VARCHAR(36),
	conector VARCHAR(64) NOT NULL,
	objeto_origen VARCHAR(64),
	objeto_tipo VARCHAR(64),
	objeto_id VARCHAR(128),
	objeto_tenant_origen VARCHAR(128),
	puntaje NUMERIC(5, 4),
	razones TEXT,
	descripcion VARCHAR(256),
	seleccionado BOOLEAN NOT NULL DEFAULT FALSE,
	seleccionado_por_id VARCHAR(36),
	alta TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_candidato_asociacion PRIMARY KEY (id),
	CONSTRAINT fk_candidato_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_candidato_documento FOREIGN KEY (documento_id) REFERENCES documento (id),
	CONSTRAINT fk_candidato_seleccionado_por FOREIGN KEY (seleccionado_por_id) REFERENCES usuario (id)
);

CREATE INDEX ix_candidato_asociacion_documento ON candidato_asociacion (documento_id);

CREATE TABLE revision_documento (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	documento_id VARCHAR(36),
	actor_id VARCHAR(36),
	decision VARCHAR(32) NOT NULL,
	estado_anterior VARCHAR(32),
	estado_nuevo VARCHAR(32),
	motivo VARCHAR(1024),
	cantidad_correcciones INTEGER NOT NULL DEFAULT 0,
	duracion_revision_milisegundos BIGINT NOT NULL DEFAULT 0,
	correlacion_id VARCHAR(64),
	alta TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_revision_documento PRIMARY KEY (id),
	CONSTRAINT fk_revision_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_revision_documento FOREIGN KEY (documento_id) REFERENCES documento (id),
	CONSTRAINT fk_revision_actor FOREIGN KEY (actor_id) REFERENCES usuario (id)
);

CREATE INDEX ix_revision_documento_documento ON revision_documento (documento_id);

CREATE TABLE cambio_campo_revision (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	revision_id VARCHAR(36),
	clave_campo VARCHAR(128) NOT NULL,
	valor_anterior TEXT,
	valor_nuevo TEXT,
	motivo VARCHAR(512),
	alta TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_cambio_campo_revision PRIMARY KEY (id),
	CONSTRAINT fk_cambio_campo_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_cambio_campo_revision FOREIGN KEY (revision_id) REFERENCES revision_documento (id)
);

CREATE INDEX ix_cambio_campo_revision_revision ON cambio_campo_revision (revision_id);

CREATE TABLE excepcion_documental (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	documento_id VARCHAR(36),
	tipo VARCHAR(32) NOT NULL,
	severidad VARCHAR(32) NOT NULL,
	prioridad VARCHAR(32) NOT NULL,
	estado VARCHAR(32) NOT NULL,
	codigo VARCHAR(64),
	detalle VARCHAR(1024),
	responsable_id VARCHAR(36),
	resuelta_por_id VARCHAR(36),
	resolucion VARCHAR(1024),
	vence_en TIMESTAMP(6) WITH TIME ZONE,
	resuelta TIMESTAMP(6) WITH TIME ZONE,
	clave_deduplicacion VARCHAR(128),
	correlacion_id VARCHAR(64),
	alta TIMESTAMP(6) WITH TIME ZONE,
	baja TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_excepcion_documental PRIMARY KEY (id),
	CONSTRAINT uq_excepcion_deduplicacion UNIQUE (clave_deduplicacion),
	CONSTRAINT fk_excepcion_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_excepcion_documento FOREIGN KEY (documento_id) REFERENCES documento (id),
	CONSTRAINT fk_excepcion_responsable FOREIGN KEY (responsable_id) REFERENCES usuario (id),
	CONSTRAINT fk_excepcion_resuelta_por FOREIGN KEY (resuelta_por_id) REFERENCES usuario (id)
);

CREATE INDEX ix_excepcion_tenant_estado ON excepcion_documental (tenant_id, estado);
CREATE INDEX ix_excepcion_documento ON excepcion_documental (documento_id);

CREATE TABLE seguimiento_original_fisico (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	documento_id VARCHAR(36),
	estado VARCHAR(32) NOT NULL,
	ubicacion VARCHAR(256),
	referencia_fisica VARCHAR(128),
	recibido_por_id VARCHAR(36),
	observacion VARCHAR(512),
	recibido TIMESTAMP(6) WITH TIME ZONE,
	archivado TIMESTAMP(6) WITH TIME ZONE,
	alta TIMESTAMP(6) WITH TIME ZONE,
	baja TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_seguimiento_original_fisico PRIMARY KEY (id),
	CONSTRAINT fk_seguimiento_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_seguimiento_documento FOREIGN KEY (documento_id) REFERENCES documento (id),
	CONSTRAINT fk_seguimiento_recibido_por FOREIGN KEY (recibido_por_id) REFERENCES usuario (id)
);

CREATE INDEX ix_seguimiento_original_documento ON seguimiento_original_fisico (documento_id);

CREATE TABLE evento_auditoria (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	tipo_actor VARCHAR(32) NOT NULL,
	id_actor VARCHAR(36),
	descripcion_actor VARCHAR(256),
	accion VARCHAR(64) NOT NULL,
	tipo_recurso VARCHAR(64),
	id_recurso VARCHAR(36),
	hash_antes VARCHAR(128),
	hash_despues VARCHAR(128),
	detalle JSONB,
	direccion_ip VARCHAR(64),
	agente_usuario VARCHAR(256),
	correlacion_id VARCHAR(64),
	exitoso BOOLEAN NOT NULL DEFAULT TRUE,
	fecha TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_evento_auditoria PRIMARY KEY (id)
);

CREATE INDEX ix_auditoria_tenant_fecha ON evento_auditoria (tenant_id, fecha);
CREATE INDEX ix_auditoria_recurso ON evento_auditoria (tenant_id, tipo_recurso, id_recurso);
CREATE INDEX ix_auditoria_correlacion ON evento_auditoria (correlacion_id);

CREATE TABLE evento_salida (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	tipo_evento VARCHAR(64) NOT NULL,
	tipo_agregado VARCHAR(64),
	id_agregado VARCHAR(36),
	carga JSONB NOT NULL,
	estado VARCHAR(32) NOT NULL,
	intento INTEGER NOT NULL DEFAULT 0,
	ultimo_error TEXT,
	correlacion_id VARCHAR(64),
	disponible_en TIMESTAMP(6) WITH TIME ZONE,
	procesado TIMESTAMP(6) WITH TIME ZONE,
	alta TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_evento_salida PRIMARY KEY (id)
);

CREATE INDEX ix_evento_salida_estado_disponible ON evento_salida (estado, disponible_en);

CREATE TABLE suscripcion_webhook (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	nombre VARCHAR(128) NOT NULL,
	url VARCHAR(1024) NOT NULL,
	secreto VARCHAR(256) NOT NULL,
	activa BOOLEAN NOT NULL DEFAULT TRUE,
	fallos_consecutivos INTEGER NOT NULL DEFAULT 0,
	ultima_entrega TIMESTAMP(6) WITH TIME ZONE,
	alta TIMESTAMP(6) WITH TIME ZONE,
	baja TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_suscripcion_webhook PRIMARY KEY (id),
	CONSTRAINT fk_suscripcion_webhook_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);

CREATE TABLE suscripcion_webhook_evento (
	suscripcion_id VARCHAR(36) NOT NULL,
	tipo_evento VARCHAR(64),
	CONSTRAINT fk_suscripcion_webhook_evento FOREIGN KEY (suscripcion_id) REFERENCES suscripcion_webhook (id)
);

CREATE TABLE entrega_webhook (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	suscripcion_id VARCHAR(36),
	evento_id VARCHAR(36) NOT NULL,
	estado VARCHAR(32) NOT NULL,
	intento INTEGER NOT NULL DEFAULT 0,
	codigo_respuesta INTEGER NOT NULL DEFAULT 0,
	cuerpo_respuesta TEXT,
	duracion_milisegundos BIGINT NOT NULL DEFAULT 0,
	disponible_en TIMESTAMP(6) WITH TIME ZONE,
	entregado TIMESTAMP(6) WITH TIME ZONE,
	alta TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_entrega_webhook PRIMARY KEY (id),
	CONSTRAINT uq_entrega_webhook UNIQUE (suscripcion_id, evento_id),
	CONSTRAINT fk_entrega_webhook_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
	CONSTRAINT fk_entrega_webhook_suscripcion FOREIGN KEY (suscripcion_id) REFERENCES suscripcion_webhook (id)
);

CREATE INDEX ix_entrega_webhook_estado ON entrega_webhook (estado, disponible_en);

CREATE TABLE configuracion_proveedor (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	proveedor VARCHAR(32) NOT NULL,
	modelo VARCHAR(128),
	referencia_secreto VARCHAR(256),
	region VARCHAR(64),
	limite_peticiones_por_minuto INTEGER NOT NULL DEFAULT 0,
	limite_paginas_por_documento INTEGER NOT NULL DEFAULT 0,
	prioridad INTEGER NOT NULL DEFAULT 0,
	parametros JSONB,
	activa BOOLEAN NOT NULL DEFAULT TRUE,
	respaldo BOOLEAN NOT NULL DEFAULT FALSE,
	alta TIMESTAMP(6) WITH TIME ZONE,
	baja TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_configuracion_proveedor PRIMARY KEY (id),
	CONSTRAINT uq_configuracion_proveedor UNIQUE (tenant_id, proveedor),
	CONSTRAINT fk_configuracion_proveedor_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);

CREATE TABLE politica_retencion (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	clase VARCHAR(64) NOT NULL,
	descripcion VARCHAR(256),
	duracion_dias INTEGER NOT NULL DEFAULT 0,
	accion VARCHAR(32) NOT NULL,
	permite_retencion_legal BOOLEAN NOT NULL DEFAULT TRUE,
	activa BOOLEAN NOT NULL DEFAULT TRUE,
	alta TIMESTAMP(6) WITH TIME ZONE,
	baja TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_politica_retencion PRIMARY KEY (id),
	CONSTRAINT uq_politica_retencion UNIQUE (tenant_id, clase),
	CONSTRAINT fk_politica_retencion_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);
