package com.entidadfinanciera.backend.application;

/** Solicitud de página: {@code page} desde 0 y {@code size} entre 1 y 100 (contrato REST, sección 1). */
public record Paginacion(int page, int size) {

	public static final int TAMANO_MAXIMO = 100;

	public Paginacion {
		if (page < 0) {
			throw new IllegalArgumentException("page no puede ser negativo");
		}
		if (size < 1 || size > TAMANO_MAXIMO) {
			throw new IllegalArgumentException("size debe estar entre 1 y " + TAMANO_MAXIMO);
		}
	}

}
