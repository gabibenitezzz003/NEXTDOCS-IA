CREATE INDEX ix_auditoria_tenant_accion_fecha ON evento_auditoria (tenant_id, accion, fecha DESC);

CREATE INDEX ix_auditoria_tenant_actor_fecha ON evento_auditoria (tenant_id, id_actor, fecha DESC);

CREATE INDEX ix_auditoria_tenant_correlacion ON evento_auditoria (tenant_id, correlacion_id);
