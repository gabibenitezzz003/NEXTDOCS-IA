DELETE FROM seguimiento_original_fisico a
USING seguimiento_original_fisico b
WHERE a.ctid < b.ctid AND a.documento_id = b.documento_id;

ALTER TABLE seguimiento_original_fisico
	ADD CONSTRAINT uq_seguimiento_original_documento UNIQUE (documento_id);

ALTER TABLE seguimiento_original_fisico
	ADD COLUMN politica VARCHAR(32);

ALTER TABLE seguimiento_original_fisico
	ADD COLUMN extraviado TIMESTAMP(6) WITH TIME ZONE;

ALTER TABLE seguimiento_original_fisico
	ADD COLUMN registrado_por_id VARCHAR(36);

ALTER TABLE seguimiento_original_fisico
	ADD CONSTRAINT fk_seguimiento_registrado_por FOREIGN KEY (registrado_por_id) REFERENCES usuario (id);

CREATE INDEX ix_seguimiento_original_estado ON seguimiento_original_fisico (tenant_id, estado);
