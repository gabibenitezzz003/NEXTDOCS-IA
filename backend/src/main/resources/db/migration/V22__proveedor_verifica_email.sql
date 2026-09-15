ALTER TABLE proveedor_identidad
	ADD COLUMN verifica_email BOOLEAN NOT NULL DEFAULT false;

UPDATE proveedor_identidad SET verifica_email = true WHERE codigo IN ('GOOGLE', 'MICROSOFT');
