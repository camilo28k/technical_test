package com.entidadfinanciera.backend.domain.exception;

/**
 * Violación de una regla de negocio o del estado actual de un recurso. Cada subclase tiene
 * un código estable que el adaptador REST publica tal cual (contrato REST, sección 6).
 */
public abstract class ReglaDeNegocioException extends RuntimeException {

	private final String codigo;

	protected ReglaDeNegocioException(String codigo, String mensaje) {
		super(mensaje);
		this.codigo = codigo;
	}

	public String getCodigo() {
		return codigo;
	}

}
