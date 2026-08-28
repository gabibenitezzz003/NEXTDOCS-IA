CREATE TABLE configuracion_conector (
	id VARCHAR(36) NOT NULL,
	tenant_id VARCHAR(36),
	codigo VARCHAR(64) NOT NULL,
	nombre VARCHAR(128) NOT NULL,
	url_base VARCHAR(512),
	referencia_secreto VARCHAR(256),
	tipo_autenticacion VARCHAR(32) NOT NULL,
	nombre_cabecera_clave VARCHAR(64),
	tiempo_espera_milisegundos INTEGER NOT NULL DEFAULT 10000,
	intentos_maximos INTEGER NOT NULL DEFAULT 3,
	umbral_seleccion_automatica NUMERIC(5, 4),
	umbral_candidato_minimo NUMERIC(5, 4),
	umbral_circuito_abierto INTEGER NOT NULL DEFAULT 5,
	duracion_circuito_abierto_segundos INTEGER NOT NULL DEFAULT 60,
	parametros JSONB,
	activo BOOLEAN NOT NULL DEFAULT TRUE,
	alta TIMESTAMP(6) WITH TIME ZONE,
	baja TIMESTAMP(6) WITH TIME ZONE,
	CONSTRAINT pk_configuracion_conector PRIMARY KEY (id),
	CONSTRAINT uq_configuracion_conector UNIQUE (tenant_id, codigo),
	CONSTRAINT fk_configuracion_conector_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);

ALTER TABLE candidato_asociacion
	ADD COLUMN descartado BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE candidato_asociacion
	ADD COLUMN motivo_seleccion VARCHAR(256);
