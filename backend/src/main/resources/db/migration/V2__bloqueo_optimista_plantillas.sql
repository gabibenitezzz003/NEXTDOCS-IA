ALTER TABLE plantilla_documental
	ADD COLUMN bloqueo_optimista BIGINT NOT NULL DEFAULT 0;

ALTER TABLE version_plantilla
	ADD COLUMN bloqueo_optimista BIGINT NOT NULL DEFAULT 0;

CREATE INDEX ix_version_plantilla_plantilla_estado ON version_plantilla (plantilla_id, estado);
CREATE INDEX ix_campo_plantilla_version ON campo_plantilla (version_plantilla_id);
CREATE INDEX ix_regla_plantilla_version ON regla_plantilla (version_plantilla_id);
