ALTER TABLE documento
	ADD COLUMN retencion_aplicada TIMESTAMP(6) WITH TIME ZONE;

ALTER TABLE documento
	ADD COLUMN accion_retencion_aplicada VARCHAR(32);

ALTER TABLE valor_extraido
	ADD COLUMN anonimizado BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX ix_documento_retencion_vencida ON documento (retener_hasta)
	WHERE retener_hasta IS NOT NULL AND retencion_aplicada IS NULL;

CREATE INDEX ix_documento_retencion_legal ON documento (tenant_id, retencion_legal)
	WHERE retencion_legal = TRUE;
