package com.nextdocs.ai.errores;

import java.util.LinkedHashMap;
import java.util.Map;

import com.nextdocs.ai.exceptions.ArchivoRechazadoException;
import com.nextdocs.ai.exceptions.EntidadNoEncontradaException;
import com.nextdocs.ai.exceptions.NoAutorizadoException;
import com.nextdocs.ai.exceptions.ProhibidoException;
import com.nextdocs.ai.exceptions.ProveedorNoDisponibleException;
import com.nextdocs.ai.exceptions.RegistroExistenteException;
import com.nextdocs.ai.exceptions.TransicionInvalidaException;
import com.nextdocs.ai.exceptions.ValidacionException;
import com.nextdocs.ai.utiles.ContextoCorrelacion;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ErrorHandler {

	private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);

	@ExceptionHandler(EntidadNoEncontradaException.class)
	public ResponseEntity<WebErrorModel> entidadNoEncontrada(HttpServletRequest peticion,
			EntidadNoEncontradaException e) {
		return respuesta(peticion, e.getMessage(), HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(ValidacionException.class)
	public ResponseEntity<WebErrorModel> validacion(HttpServletRequest peticion, ValidacionException e) {
		return respuesta(peticion, e.getMessage(), HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(TransicionInvalidaException.class)
	public ResponseEntity<WebErrorModel> transicionInvalida(HttpServletRequest peticion,
			TransicionInvalidaException e) {
		return respuesta(peticion, e.getMessage(), HttpStatus.CONFLICT);
	}

	@ExceptionHandler(RegistroExistenteException.class)
	public ResponseEntity<WebErrorModel> registroExistente(HttpServletRequest peticion, RegistroExistenteException e) {
		return respuesta(peticion, e.getMessage(), HttpStatus.CONFLICT);
	}

	@ExceptionHandler(NoAutorizadoException.class)
	public ResponseEntity<WebErrorModel> noAutorizado(HttpServletRequest peticion, NoAutorizadoException e) {
		return respuesta(peticion, e.getMessage(), HttpStatus.UNAUTHORIZED);
	}

	@ExceptionHandler({ ProhibidoException.class, AccessDeniedException.class })
	public ResponseEntity<WebErrorModel> prohibido(HttpServletRequest peticion, Exception e) {
		log.warn("Acceso denegado en {}: {}", peticion.getRequestURI(), e.getMessage());
		return respuesta(peticion, e.getMessage(), HttpStatus.FORBIDDEN);
	}

	@ExceptionHandler(ArchivoRechazadoException.class)
	public ResponseEntity<WebErrorModel> archivoRechazado(HttpServletRequest peticion, ArchivoRechazadoException e) {
		return respuesta(peticion, e.getCodigo() + ": " + e.getMessage(), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<WebErrorModel> tamanoExcedido(HttpServletRequest peticion,
			MaxUploadSizeExceededException e) {
		return respuesta(peticion, "El archivo supera el tamano maximo permitido", HttpStatus.PAYLOAD_TOO_LARGE);
	}

	@ExceptionHandler(ProveedorNoDisponibleException.class)
	public ResponseEntity<WebErrorModel> proveedorNoDisponible(HttpServletRequest peticion,
			ProveedorNoDisponibleException e) {
		log.error("Proveedor no disponible: {}", e.getMessage());
		return respuesta(peticion, e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> argumentoInvalido(HttpServletRequest peticion,
			MethodArgumentNotValidException e) {
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		Map<String, String> campos = new LinkedHashMap<>();
		for (FieldError error : e.getBindingResult().getFieldErrors()) {
			campos.put(error.getField(), error.getDefaultMessage());
		}
		cuerpo.put("mensaje", "Los datos enviados no son validos");
		cuerpo.put("campos", campos);
		cuerpo.put("ruta", peticion.getRequestURI());
		cuerpo.put("codigo", HttpStatus.BAD_REQUEST.value());
		cuerpo.put("correlacionId", ContextoCorrelacion.obtener());
		return new ResponseEntity<>(cuerpo, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<WebErrorModel> cuerpoIlegible(HttpServletRequest peticion,
			HttpMessageNotReadableException e) {
		return respuesta(peticion, "El cuerpo de la peticion no se pudo interpretar", HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<WebErrorModel> metodoNoSoportado(HttpServletRequest peticion,
			HttpRequestMethodNotSupportedException e) {
		return respuesta(peticion, e.getMessage(), HttpStatus.METHOD_NOT_ALLOWED);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<WebErrorModel> integridadDatos(HttpServletRequest peticion,
			DataIntegrityViolationException e) {
		log.warn("Violacion de integridad en {}: {}", peticion.getRequestURI(), e.getMostSpecificCause().getMessage());
		return respuesta(peticion, "La operacion viola una restriccion de integridad", HttpStatus.CONFLICT);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<WebErrorModel> errorNoControlado(HttpServletRequest peticion, Exception e) {
		log.error("Error no controlado en {}", peticion.getRequestURI(), e);
		return respuesta(peticion, "Error interno del servidor", HttpStatus.INTERNAL_SERVER_ERROR);
	}

	private ResponseEntity<WebErrorModel> respuesta(HttpServletRequest peticion, String mensaje, HttpStatus estado) {
		WebErrorModel error = new WebErrorModel(mensaje, peticion.getRequestURI(), estado,
				ContextoCorrelacion.obtener());
		return new ResponseEntity<>(error, estado);
	}
}
