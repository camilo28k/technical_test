package com.entidadfinanciera.backend.application.port.out;

import java.time.OffsetDateTime;
import java.util.Optional;

import com.entidadfinanciera.backend.application.Pagina;
import com.entidadfinanciera.backend.application.Paginacion;
import com.entidadfinanciera.backend.application.consulta.LineaEstadoDeCuenta;

/** Puerto de salida de lectura del historial, del más reciente al más antiguo. */
public interface HistorialQueryPort {

	/** Movimientos de la cuenta con {@code desde <= fecha < hastaExclusivo}; ambos límites opcionales. */
	Pagina<LineaEstadoDeCuenta> movimientos(Long cuentaId, Optional<OffsetDateTime> desde,
			Optional<OffsetDateTime> hastaExclusivo, Paginacion paginacion);

}
