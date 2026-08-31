DROP TABLE IF EXISTS mensaje_whatsapp_saliente;
DROP TABLE IF EXISTS media_whatsapp;
DROP TABLE IF EXISTS mensaje_whatsapp_entrante;
DROP TABLE IF EXISTS correlacion_whatsapp;
DROP TABLE IF EXISTS contacto_whatsapp_autorizado;
DROP TABLE IF EXISTS linea_whatsapp;

DROP TABLE IF EXISTS mensaje_correo_saliente;
DROP TABLE IF EXISTS adjunto_correo;
DROP TABLE IF EXISTS mensaje_correo_entrante;
DROP TABLE IF EXISTS correlacion_correo;
DROP TABLE IF EXISTS remitente_autorizado;
DROP TABLE IF EXISTS buzon_correo;

DELETE FROM rol_permiso WHERE permiso IN ('canales.leer', 'canales.administrar');
