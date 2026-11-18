package com.entidadfinanciera.backend.application.port.out;

import com.entidadfinanciera.backend.domain.model.Operacion;

/** Puerto de salida del historial. Solo inserta: las operaciones son inmutables (DA-01). */
public interface OperacionRepositoryPort {

	/** Inserta la operación y sus movimientos; devuelve ambos con sus identificadores. */
	Operacion registrar(Operacion operacion);

}
