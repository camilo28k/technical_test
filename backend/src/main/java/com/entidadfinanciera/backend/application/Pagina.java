package com.entidadfinanciera.backend.application;

import java.util.List;
import java.util.function.Function;

/** Página de resultados independiente de Spring Data. */
public record Pagina<T>(List<T> contenido, int page, int size, long totalElementos, int totalPaginas) {

	public <R> Pagina<R> map(Function<T, R> conversion) {
		return new Pagina<>(contenido.stream().map(conversion).toList(), page, size, totalElementos, totalPaginas);
	}

}
