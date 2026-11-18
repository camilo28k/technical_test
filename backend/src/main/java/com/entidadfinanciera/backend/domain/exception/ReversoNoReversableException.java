package com.entidadfinanciera.backend.domain.exception;

/** P-04: un reverso es definitivo y no puede reversarse. */
public class ReversoNoReversableException extends ReglaDeNegocioException {

	public ReversoNoReversableException(Long id) {
		super("REVERSO_NO_REVERSABLE", "La operación " + id + " es un reverso y no puede reversarse.");
	}

}
