package com.entidadfinanciera.backend.infrastructure.adapter.in.rest.comun;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.entidadfinanciera.backend.domain.exception.DatoInvalidoException;
import com.entidadfinanciera.backend.domain.exception.RecursoNoEncontradoException;
import com.entidadfinanciera.backend.domain.exception.ReglaDeNegocioException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Traduce excepciones del núcleo y de la capa web al formato de error del contrato. Nunca
 * expone trazas ni SQL.
 */
@RestControllerAdvice
public class ManejadorDeErrores {

	private static final Logger LOG = LoggerFactory.getLogger(ManejadorDeErrores.class);

	private final Clock reloj;

	public ManejadorDeErrores(Clock reloj) {
		this.reloj = reloj;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex, HttpServletRequest solicitud) {
		List<ErrorResponse.Detalle> detalles = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(e -> new ErrorResponse.Detalle(e.getField(), e.getDefaultMessage()))
			.toList();
		return responder(HttpStatus.BAD_REQUEST, "VALIDACION", "La solicitud tiene datos inválidos.", solicitud,
				detalles);
	}

	@ExceptionHandler(DatoInvalidoException.class)
	ResponseEntity<ErrorResponse> datoInvalido(DatoInvalidoException ex, HttpServletRequest solicitud) {
		return responder(HttpStatus.BAD_REQUEST, "VALIDACION", "La solicitud tiene datos inválidos.", solicitud,
				List.of(new ErrorResponse.Detalle(ex.getCampo(), ex.getMessage())));
	}

	@ExceptionHandler({ IllegalArgumentException.class, MethodArgumentTypeMismatchException.class })
	ResponseEntity<ErrorResponse> parametroInvalido(Exception ex, HttpServletRequest solicitud) {
		return responder(HttpStatus.BAD_REQUEST, "VALIDACION", ex.getMessage(), solicitud, List.of());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ErrorResponse> cuerpoIlegible(HttpMessageNotReadableException ex, HttpServletRequest solicitud) {
		return responder(HttpStatus.BAD_REQUEST, "CUERPO_ILEGIBLE", "El cuerpo de la solicitud no es un JSON válido.",
				solicitud, List.of());
	}

	@ExceptionHandler(RecursoNoEncontradoException.class)
	ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException ex, HttpServletRequest solicitud) {
		return responder(HttpStatus.NOT_FOUND, ex.getCodigo(), ex.getMessage(), solicitud, List.of());
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ErrorResponse> rutaInexistente(NoResourceFoundException ex, HttpServletRequest solicitud) {
		return responder(HttpStatus.NOT_FOUND, "RUTA_NO_ENCONTRADA", "La ruta solicitada no existe.", solicitud,
				List.of());
	}

	@ExceptionHandler(ReglaDeNegocioException.class)
	ResponseEntity<ErrorResponse> reglaDeNegocio(ReglaDeNegocioException ex, HttpServletRequest solicitud) {
		return responder(HttpStatus.CONFLICT, ex.getCodigo(), ex.getMessage(), solicitud, List.of());
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ErrorResponse> inesperado(Exception ex, HttpServletRequest solicitud) {
		LOG.error("Error no controlado en {}", solicitud.getRequestURI(), ex);
		return responder(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", "Ocurrió un error inesperado.", solicitud,
				List.of());
	}

	private ResponseEntity<ErrorResponse> responder(HttpStatus estado, String codigo, String mensaje,
			HttpServletRequest solicitud, List<ErrorResponse.Detalle> detalles) {
		ErrorResponse cuerpo = new ErrorResponse(codigo, mensaje, OffsetDateTime.now(reloj).truncatedTo(ChronoUnit.SECONDS), solicitud.getRequestURI(),
				detalles);
		return ResponseEntity.status(estado).body(cuerpo);
	}

}
