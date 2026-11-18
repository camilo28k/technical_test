package com.entidadfinanciera.backend.application.port.out;

import java.util.Optional;

import com.entidadfinanciera.backend.domain.model.Operacion;

/** Puerto de salida del historial. Solo inserta: las operaciones son inmutables (DA-01). */
public interface OperacionRepositoryPort {

	/** Inserta la operación y sus movimientos; devuelve ambos con sus identificadores. */
	Operacion registrar(Operacion operacion);

	/** Operación con sus movimientos. {@code saldoResultante} no se almacena y llega vacío. */
	Optional<Operacion> buscarPorId(Long id);

	boolean existeReversoDe(Long originalId);

}
