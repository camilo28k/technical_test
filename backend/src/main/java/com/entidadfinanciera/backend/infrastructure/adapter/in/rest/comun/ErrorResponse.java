package com.entidadfinanciera.backend.infrastructure.adapter.in.rest.comun;

import java.time.OffsetDateTime;
import java.util.List;

/** Cuerpo común de todas las respuestas de error (contrato REST, sección 6). */
public record ErrorResponse(String codigo, String mensaje, OffsetDateTime fecha, String ruta, List<Detalle> detalles) {

	public record Detalle(String campo, String mensaje) {
	}

}
