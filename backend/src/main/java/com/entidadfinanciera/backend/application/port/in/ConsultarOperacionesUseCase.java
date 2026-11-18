package com.entidadfinanciera.backend.application.port.in;

import java.time.LocalDate;
import java.util.Optional;

import com.entidadfinanciera.backend.application.Paginacion;
import com.entidadfinanciera.backend.application.consulta.EstadoDeCuenta;
import com.entidadfinanciera.backend.domain.model.Operacion;

/** Puerto de entrada de consultas: operaciones y estados de cuenta (RF-07, CA-11). */
public interface ConsultarOperacionesUseCase {

	Operacion consultarOperacion(Long id);

	/** {@code desde} y {@code hasta} son fechas civiles de Colombia, ambas inclusive. */
	EstadoDeCuenta estadoDeCuenta(Long cuentaId, Optional<LocalDate> desde, Optional<LocalDate> hasta,
			Paginacion paginacion);

}
