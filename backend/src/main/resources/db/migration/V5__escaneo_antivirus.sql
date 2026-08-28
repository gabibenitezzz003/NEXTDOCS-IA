ALTER TABLE archivo_documento
	ADD COLUMN resultado_escaneo VARCHAR(32);

ALTER TABLE archivo_documento
	ADD COLUMN amenaza_detectada VARCHAR(256);

ALTER TABLE archivo_documento
	ADD COLUMN motor_escaneo VARCHAR(64);

ALTER TABLE archivo_documento
	ADD COLUMN escaneado TIMESTAMP(6) WITH TIME ZONE;

ALTER TABLE archivo_documento
	ADD COLUMN en_cuarentena BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX ix_archivo_documento_cuarentena ON archivo_documento (tenant_id, en_cuarentena);
