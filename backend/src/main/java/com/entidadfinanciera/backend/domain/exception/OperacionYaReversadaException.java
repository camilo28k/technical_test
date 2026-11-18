package com.entidadfinanciera.backend.domain.exception;

/** P-04: una operación admite como máximo un reverso. */
public class OperacionYaReversadaException extends ReglaDeNegocioException {

	public OperacionYaReversadaException(Long id) {
		super("OPERACION_YA_REVERSADA", "La operación " + id + " ya fue reversada.");
	}

}
