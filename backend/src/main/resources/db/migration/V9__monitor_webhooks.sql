CREATE INDEX ix_entrega_webhook_tenant_estado ON entrega_webhook (tenant_id, estado, alta DESC);

CREATE INDEX ix_entrega_webhook_suscripcion ON entrega_webhook (suscripcion_id, alta DESC);
