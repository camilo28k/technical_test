package com.entidadfinanciera.backend.domain.exception;

/** El recurso solicitado no existe. */
public abstract class RecursoNoEncontradoException extends RuntimeException {

	private final String codigo;

	protected RecursoNoEncontradoException(String codigo, String mensaje) {
		super(mensaje);
		this.codigo = codigo;
	}

	public String getCodigo() {
		return codigo;
	}

}
