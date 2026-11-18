package com.entidadfinanciera.backend.domain.exception;

/** DA-03: las cuentas inactivas o canceladas no emiten ni reciben movimientos. */
public class CuentaNoActivaException extends ReglaDeNegocioException {

	public CuentaNoActivaException(String numero, String estado) {
		super("CUENTA_NO_ACTIVA", "La cuenta " + numero + " está " + estado.toLowerCase() + " y no admite movimientos.");
	}

}
