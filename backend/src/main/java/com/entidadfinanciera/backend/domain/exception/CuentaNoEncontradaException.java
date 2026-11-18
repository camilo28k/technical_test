package com.entidadfinanciera.backend.domain.exception;

public class CuentaNoEncontradaException extends RecursoNoEncontradoException {

	public CuentaNoEncontradaException(String descripcion) {
		super("CUENTA_NO_ENCONTRADA", "No existe una cuenta con " + descripcion + ".");
	}

	public static CuentaNoEncontradaException porId(Long id) {
		return new CuentaNoEncontradaException("id " + id);
	}

	public static CuentaNoEncontradaException porNumero(String numero) {
		return new CuentaNoEncontradaException("número " + numero);
	}

}
