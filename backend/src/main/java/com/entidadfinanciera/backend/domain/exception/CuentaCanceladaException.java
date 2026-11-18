package com.entidadfinanciera.backend.domain.exception;

/** DA-03: una cuenta cancelada es definitiva; no admite cambios de estado ni de atributos. */
public class CuentaCanceladaException extends ReglaDeNegocioException {

	public CuentaCanceladaException(String numero) {
		super("CUENTA_CANCELADA", "La cuenta " + numero + " está cancelada y no admite cambios.");
	}

}
