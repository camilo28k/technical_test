package com.entidadfinanciera.backend.infrastructure.adapter.in.rest.comun;

import java.util.List;

import com.entidadfinanciera.backend.application.Pagina;

/** Formato de los listados paginados (contrato REST, sección 1). */
public record PaginaResponse<T>(List<T> contenido, int page, int size, long totalElementos, int totalPaginas) {

	public static <T> PaginaResponse<T> desde(Pagina<T> pagina) {
		return new PaginaResponse<>(pagina.contenido(), pagina.page(), pagina.size(), pagina.totalElementos(),
				pagina.totalPaginas());
	}

}
