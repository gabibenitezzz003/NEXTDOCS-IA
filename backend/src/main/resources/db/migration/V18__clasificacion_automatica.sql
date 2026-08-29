ALTER TABLE documento ADD COLUMN origen_tipo VARCHAR(16);
ALTER TABLE documento ADD COLUMN confianza_tipo NUMERIC(5, 4);
ALTER TABLE documento ADD COLUMN motivo_tipo VARCHAR(400);

UPDATE documento SET origen_tipo = 'DECLARADO' WHERE plantilla_id IS NOT NULL;
