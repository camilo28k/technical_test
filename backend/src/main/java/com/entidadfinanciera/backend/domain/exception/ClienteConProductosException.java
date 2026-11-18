package com.entidadfinanciera.backend.domain.exception;

/** RN-02: no se elimina un cliente que tenga cuentas, incluidas las canceladas. */
public class ClienteConProductosException extends ReglaDeNegocioException {

	public ClienteConProductosException(Long id) {
		super("CLIENTE_CON_PRODUCTOS", "El cliente " + id + " tiene cuentas asociadas y no puede eliminarse.");
	}

}
