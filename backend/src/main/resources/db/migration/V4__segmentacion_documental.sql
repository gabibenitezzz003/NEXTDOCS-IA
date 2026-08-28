ALTER TABLE version_plantilla
	ADD COLUMN estrategia_segmentacion VARCHAR(32);

ALTER TABLE version_plantilla
	ADD COLUMN paginas_por_documento INTEGER NOT NULL DEFAULT 0;

ALTER TABLE version_plantilla
	ADD COLUMN patron_inicio_documento VARCHAR(512);

UPDATE version_plantilla SET estrategia_segmentacion = 'NINGUNA' WHERE estrategia_segmentacion IS NULL;

CREATE INDEX ix_segmento_documento_hijo ON segmento_documento (documento_hijo_id);
