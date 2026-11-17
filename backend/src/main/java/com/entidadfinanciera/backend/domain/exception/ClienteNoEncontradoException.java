package com.entidadfinanciera.backend.domain.exception;

public class ClienteNoEncontradoException extends RecursoNoEncontradoException {

	public ClienteNoEncontradoException(Long id) {
		super("CLIENTE_NO_ENCONTRADO", "No existe un cliente con id " + id + ".");
	}

}
