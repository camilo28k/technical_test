package com.entidadfinanciera.backend.domain.exception;

/** Un dato no cumple el formato exigido. Se publica como error de validación. */
public class DatoInvalidoException extends RuntimeException {

	private final String campo;

	public DatoInvalidoException(String campo, String mensaje) {
		super(mensaje);
		this.campo = campo;
	}

	public String getCampo() {
		return campo;
	}

}
