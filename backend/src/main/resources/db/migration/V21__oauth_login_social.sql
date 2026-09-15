ALTER TABLE proveedor_identidad
	ADD COLUMN cliente_id VARCHAR(256),
	ADD COLUMN cliente_secreto VARCHAR(512),
	ADD COLUMN url_autorizacion VARCHAR(512),
	ADD COLUMN url_token VARCHAR(512),
	ADD COLUMN alcances VARCHAR(512);
