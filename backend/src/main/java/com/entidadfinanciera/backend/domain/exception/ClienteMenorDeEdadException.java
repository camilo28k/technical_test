package com.entidadfinanciera.backend.domain.exception;

/** RN-01 y P-01: el cliente debe tener al menos 18 años cumplidos en Colombia. */
public class ClienteMenorDeEdadException extends ReglaDeNegocioException {

	public ClienteMenorDeEdadException() {
		super("CLIENTE_MENOR_DE_EDAD", "El cliente debe tener al menos 18 años cumplidos.");
	}

}
