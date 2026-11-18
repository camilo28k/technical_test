package com.entidadfinanciera.backend.domain.exception;

/** DA-05: una transferencia exige cuentas de origen y destino distintas. */
public class MismaCuentaException extends ReglaDeNegocioException {

	public MismaCuentaException(String numero) {
		super("MISMA_CUENTA", "La cuenta de origen y la de destino son la misma (" + numero + ").");
	}

}
