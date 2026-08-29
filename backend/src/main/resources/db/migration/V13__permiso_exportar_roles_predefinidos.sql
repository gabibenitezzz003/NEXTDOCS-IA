INSERT INTO rol_permiso (rol_id, permiso)
SELECT r.id, 'documentos.exportar'
FROM rol r
WHERE r.predefinido = TRUE
	AND r.codigo IN ('ADMINISTRADOR', 'AUDITOR')
	AND NOT EXISTS (
		SELECT 1 FROM rol_permiso rp WHERE rp.rol_id = r.id AND rp.permiso = 'documentos.exportar'
	);
