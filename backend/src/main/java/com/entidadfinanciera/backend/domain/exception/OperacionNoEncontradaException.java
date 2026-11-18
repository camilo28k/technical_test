package com.entidadfinanciera.backend.domain.exception;

public class OperacionNoEncontradaException extends RecursoNoEncontradoException {

	public OperacionNoEncontradaException(Long id) {
		super("OPERACION_NO_ENCONTRADA", "No existe una operación con id " + id + ".");
	}

}
